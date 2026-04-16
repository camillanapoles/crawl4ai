package com.crawl4ai.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crawl4ai.android.data.local.db.entity.DeepCrawlSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeepCrawlSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeepCrawlSessionEntity): Long

    @Update
    suspend fun update(entity: DeepCrawlSessionEntity)

    @Query("SELECT * FROM deep_crawl_sessions WHERE id = :id")
    suspend fun getById(id: Long): DeepCrawlSessionEntity?

    @Query("SELECT * FROM deep_crawl_sessions WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): DeepCrawlSessionEntity?

    @Query("SELECT * FROM deep_crawl_sessions ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<DeepCrawlSessionEntity>>

    @Query("SELECT * FROM deep_crawl_sessions WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatusFlow(status: String): Flow<List<DeepCrawlSessionEntity>>

    @Query("UPDATE deep_crawl_sessions SET status = :status, pagesVisited = :pages, completedAt = :completedAt, durationMs = :durationMs WHERE sessionId = :sessionId")
    suspend fun updateStatus(
        sessionId: String,
        status: String,
        pages: Int,
        completedAt: Long,
        durationMs: Long,
    )

    @Query("DELETE FROM deep_crawl_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM deep_crawl_sessions")
    suspend fun deleteAll()
}
