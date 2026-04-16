package com.crawl4ai.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved/named [com.crawl4ai.android.domain.model.CrawlConfig].
 * Users can define presets and mark one as the default.
 */
@Entity(
    tableName = "crawl_configs",
    indices = [Index(value = ["name"]), Index(value = ["isDefault"])],
)
data class CrawlConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val name: String,
    val description: String = "",

    /** JSON-encoded CrawlConfig (via kotlinx.serialization). */
    val configJson: String = "{}",

    val isDefault: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
