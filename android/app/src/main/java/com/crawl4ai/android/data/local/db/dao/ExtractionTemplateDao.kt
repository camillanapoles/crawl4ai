package com.crawl4ai.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crawl4ai.android.data.local.db.entity.ExtractionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtractionTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExtractionTemplateEntity): Long

    @Update
    suspend fun update(entity: ExtractionTemplateEntity)

    @Query("SELECT * FROM extraction_templates WHERE id = :id")
    suspend fun getById(id: Long): ExtractionTemplateEntity?

    @Query("SELECT * FROM extraction_templates ORDER BY usageCount DESC, name ASC")
    fun getAllFlow(): Flow<List<ExtractionTemplateEntity>>

    @Query("SELECT * FROM extraction_templates WHERE strategyType = :type ORDER BY usageCount DESC")
    fun getByTypeFlow(type: String): Flow<List<ExtractionTemplateEntity>>

    @Query("SELECT * FROM extraction_templates WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchFlow(query: String): Flow<List<ExtractionTemplateEntity>>

    @Query("UPDATE extraction_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("DELETE FROM extraction_templates WHERE id = :id AND isBuiltin = 0")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM extraction_templates")
    suspend fun count(): Int
}
