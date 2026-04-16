package com.crawl4ai.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crawl4ai.android.data.local.db.entity.CrawlResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrawlResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrawlResultEntity): Long

    @Update
    suspend fun update(entity: CrawlResultEntity)

    @Query("SELECT * FROM crawl_results WHERE id = :id")
    suspend fun getById(id: Long): CrawlResultEntity?

    @Query("SELECT * FROM crawl_results WHERE url = :url ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByUrl(url: String): CrawlResultEntity?

    @Query("SELECT * FROM crawl_results ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CrawlResultEntity>>

    @Query("SELECT * FROM crawl_results ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<CrawlResultEntity>

    @Query("DELETE FROM crawl_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM crawl_results WHERE createdAt < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)

    @Query("DELETE FROM crawl_results")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM crawl_results")
    suspend fun count(): Int

    @Query("SELECT SUM(LENGTH(html) + LENGTH(rawMarkdown)) FROM crawl_results")
    suspend fun totalSizeBytes(): Long?
}
