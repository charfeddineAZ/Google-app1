package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class Workflow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val nodesJson: String,
    val connectionsJson: String,
    val cronExpression: String = "*/5 * * * *",
    val isScheduleActive: Boolean = false,
    val lastRunTime: Long = 0L
)

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workflowId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // "INFO", "WARNING", "ERROR", "SUCCESS"
    val nodeName: String,
    val message: String,
    val duration: Long = 0L
)

@Entity(tableName = "workspace_files")
data class WorkspaceFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String, // Full path, e.g. "/root/test.json"
    val isDirectory: Boolean,
    val content: String = ""
)

@Entity(tableName = "library_items")
data class LibraryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "JS", "PYTHON", "RACCORD", "SELECTOR"
    val name: String,
    val content: String,
    val tags: String = "",
    val version: Int = 1
)

@Entity(tableName = "secrets")
data class SecretKey(
    @PrimaryKey val key: String,
    val value: String
)
