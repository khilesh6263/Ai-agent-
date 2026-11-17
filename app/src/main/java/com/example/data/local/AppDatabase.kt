package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FaqEntity::class, CallRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun faqDao(): FaqDao
    abstract fun callRecordDao(): CallRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "call_agent_db"
                )
                    .build()
                    .also { db ->
                        INSTANCE = db
                        seedDefaultsIfEmpty(db)
                    }
            }
        }

        private fun seedDefaultsIfEmpty(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = db.faqDao()
                if (dao.count() == 0) {
                    dao.insertAll(
                        listOf(
                            FaqEntity(
                                question = "When will you be free or available to talk?",
                                answer = "I am currently wrapping up scheduled work and will call back within an hour.",
                                category = "Availability",
                                isEnabled = true
                            ),
                            FaqEntity(
                                question = "Where should I leave a parcel or delivery?",
                                answer = "Please leave packages with the reception or building security desk and mention my name.",
                                category = "Deliveries",
                                isEnabled = true
                            ),
                            FaqEntity(
                                question = "How can I schedule a meeting or appointment?",
                                answer = "Please share your preferred date, time, and topic, and I will confirm via message.",
                                category = "Scheduling",
                                isEnabled = true
                            ),
                            FaqEntity(
                                question = "What is your office address or hours?",
                                answer = "Office hours are Monday to Friday, 9:30 AM to 6:30 PM. Share your email if you need the location pin.",
                                category = "Work",
                                isEnabled = true
                            )
                        )
                    )
                }
            }
        }
    }
}
