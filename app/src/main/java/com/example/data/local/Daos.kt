package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FaqDao {
    @Query("SELECT * FROM faqs ORDER BY id ASC")
    fun observeAll(): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun getEnabledOnce(): List<FaqEntity>

    @Query("SELECT COUNT(*) FROM faqs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FaqEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FaqEntity>)

    @Update
    suspend fun update(entity: FaqEntity)

    @Query("DELETE FROM faqs WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallRecordEntity): Long

    @Query("UPDATE call_records SET isResolved = :resolved WHERE id = :id")
    suspend fun setResolved(id: Long, resolved: Boolean)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clearAll()
}
