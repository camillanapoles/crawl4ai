package com.crawl4ai.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved extraction template (CSS selector set, XPath, Regex, or LLM prompt).
 * Built-in templates are pre-populated on first run and are read-only.
 */
@Entity(
    tableName = "extraction_templates",
    indices = [Index(value = ["strategyType"]), Index(value = ["isBuiltin"])],
)
data class ExtractionTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val name: String,
    val description: String = "",

    /** "css" | "xpath" | "regex" | "llm" | "none" */
    val strategyType: String = "css",

    /** JSON-encoded ExtractionConfig. */
    val configJson: String = "{}",

    val usageCount: Int = 0,

    /** Built-in templates cannot be deleted. */
    val isBuiltin: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
)
