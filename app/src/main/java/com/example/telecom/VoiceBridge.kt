package com.example.telecom

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.LanguageMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "VoiceBridge"

class VoiceBridge(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private suspend fun ensureTtsInitialized(): Boolean = suspendCancellableCoroutine { cont ->
        if (ttsReady && tts != null) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        try {
            tts?.shutdown()
            tts = TextToSpeech(context.applicationContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (cont.isActive) {
                    cont.resume(ttsReady)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
            if (cont.isActive) cont.resume(false)
        }
    }

    fun enableCallSpeakerphone(enable: Boolean) {
        try {
            if (enable) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioManager speakerphone adjustment failed: ${e.message}")
        }
    }

    suspend fun speak(text: String, languageMode: LanguageMode): Boolean {
        if (text.isBlank()) return false
        val ready = ensureTtsInitialized()
        if (!ready) {
            Log.w(TAG, "TTS engine unavailable on this device")
            return false
        }

        val engine = tts ?: return false
        val targetLocale = chooseLocale(text, languageMode)
        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(1.02f)

        return suspendCancellableCoroutine { cont ->
            val utteranceId = UUID.randomUUID().toString()
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && cont.isActive) {
                        cont.resume(true)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && cont.isActive) {
                        cont.resume(false)
                    }
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId && cont.isActive) {
                        cont.resume(false)
                    }
                }
            })

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
            }
            val speakResult = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (speakResult != TextToSpeech.SUCCESS && cont.isActive) {
                cont.resume(false)
            }

            cont.invokeOnCancellation {
                runCatching { engine.stop() }
            }
        }
    }

    suspend fun listenOnce(languageMode: LanguageMode): Result<String> = withContext(Dispatchers.Main) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return@withContext Result.failure(
                IllegalStateException("Speech recognition service is not available on this device.")
            )
        }

        suspendCancellableCoroutine { cont ->
            try {
                speechRecognizer?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network error"
                            else -> "Speech recognition error ($error)"
                        }
                        if (cont.isActive) {
                            cont.resume(Result.failure(IllegalStateException(message)))
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim().orEmpty()
                        if (cont.isActive) {
                            if (best.isNotEmpty()) {
                                cont.resume(Result.success(best))
                            } else {
                                cont.resume(Result.failure(IllegalStateException("No speech detected")))
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val localeTag = when (languageMode) {
                    LanguageMode.HINDI -> "hi-IN"
                    LanguageMode.AUTO_BILINGUAL -> "en-IN"
                    LanguageMode.ENGLISH -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                recognizer.startListening(intent)

                cont.invokeOnCancellation {
                    runCatching {
                        recognizer.cancel()
                        recognizer.destroy()
                    }
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
    }

    fun stopSpeaking() {
        runCatching { tts?.stop() }
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    fun release() {
        stopListening()
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = null
            ttsReady = false
        }
        enableCallSpeakerphone(false)
    }

    private fun chooseLocale(text: String, mode: LanguageMode): Locale {
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        return when {
            hasDevanagari || mode == LanguageMode.HINDI -> Locale.forLanguageTag("hi-IN")
            mode == LanguageMode.AUTO_BILINGUAL -> Locale.forLanguageTag("en-IN")
            else -> Locale.US
        }
    }
}
