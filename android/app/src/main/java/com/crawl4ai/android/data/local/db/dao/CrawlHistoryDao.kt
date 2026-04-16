package com.crawl4ai.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrawlHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrawlHistoryEntity): Long

    @Query("SELECT * FROM crawl_history WHERE id = :id")
    suspend fun getById(id: Long): CrawlHistoryEntity?

    @Query("SELECT * FROM crawl_history ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CrawlHistoryEntity>>

    @Query("SELECT * FROM crawl_history ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<CrawlHistoryEntity>

    @Query("SELECT * FROM crawl_history WHERE url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFlow(query: String): Flow<List<CrawlHistoryEntity>>

    @Query("DELETE FROM crawl_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM crawl_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM crawl_history")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM crawl_history WHERE success = 1")
    suspend fun successCount(): Int
}
