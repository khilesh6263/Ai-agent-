package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CallMissedOutgoing
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CallOutcome
import com.example.data.UrgencyLevel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralHangup
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.MonoLabelStyle
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PulsingAgentOrb(
    isActive: Boolean,
    isSpeaking: Boolean,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "agent_orb")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 900 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val ringColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size + 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val maxRadius = this.size.minDimension / 2f
                val baseRadius = (size.toPx() / 2f)
                val currentRadius = baseRadius + (maxRadius - baseRadius) * pulse
                drawCircle(
                    color = ringColor.copy(alpha = (1f - pulse) * 0.45f),
                    radius = currentRadius
                )
            }
        }
        Image(
            painter = painterResource(id = R.drawable.img_agent_avatar),
            contentDescription = "AI Call Agent Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isActive) ringColor else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun AcousticWaveformVisualizer(
    isAnimating: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 28,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val slotWidth = totalWidth / barCount
        val barWidth = (slotWidth * 0.48f).coerceAtLeast(3.dp.toPx())
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalizedPos = i.toFloat() / barCount
            val envelope = sin(normalizedPos * PI).toFloat()
            val amplitude = if (isAnimating) {
                val wave = 0.35f + 0.65f * ((sin(phase + i * 0.55f) + 1f) / 2f)
                (size.height * 0.88f) * wave * (0.35f + 0.65f * envelope)
            } else {
                size.height * (0.16f + 0.14f * envelope)
            }
            val barHeight = amplitude.coerceAtLeast(6.dp.toPx())
            val x = i * slotWidth + (slotWidth - barWidth) / 2f
            val y = centerY - barHeight / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.45f)
                    ),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun OutcomeBadge(
    outcome: CallOutcome,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (outcome) {
        CallOutcome.MESSAGE_TAKEN -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Rounded.MarkChatUnread
        )
        CallOutcome.FAQ_ANSWERED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Rounded.QuestionAnswer
        )
        CallOutcome.REDIRECTED -> Triple(
            AmberAlert.copy(alpha = 0.18f),
            AmberAlert,
            Icons.Rounded.PhoneForwarded
        )
        CallOutcome.USER_AVAILABLE -> Triple(
            EmeraldLive.copy(alpha = 0.18f),
            EmeraldLive,
            Icons.Rounded.CheckCircle
        )
        CallOutcome.GREETED -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primary,
            Icons.Rounded.AutoAwesome
        )
        CallOutcome.MISSED_OR_ENDED -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Rounded.CallMissedOutgoing
        )
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = outcome.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}

@Composable
fun UrgencyChip(
    urgency: UrgencyLevel,
    modifier: Modifier = Modifier
) {
    if (urgency == UrgencyLevel.LOW || urgency == UrgencyLevel.MEDIUM) return
    val color = if (urgency == UrgencyLevel.URGENT) CoralHangup else AmberAlert
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Urgency ${urgency.label}",
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = urgency.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (active) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MonoLabelStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
