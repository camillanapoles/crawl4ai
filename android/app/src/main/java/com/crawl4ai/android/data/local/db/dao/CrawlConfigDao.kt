package com.crawl4ai.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crawl4ai.android.data.local.db.entity.CrawlConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrawlConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrawlConfigEntity): Long

    @Update
    suspend fun update(entity: CrawlConfigEntity)

    @Query("SELECT * FROM crawl_configs WHERE id = :id")
    suspend fun getById(id: Long): CrawlConfigEntity?

    @Query("SELECT * FROM crawl_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): CrawlConfigEntity?

    @Query("SELECT * FROM crawl_configs ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<CrawlConfigEntity>>

    @Query("DELETE FROM crawl_configs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE crawl_configs SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE crawl_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)

    @Query("SELECT COUNT(*) FROM crawl_configs")
    suspend fun count(): Int
}
