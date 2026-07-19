package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Workflows ---
    @Query("SELECT * FROM workflows ORDER BY id DESC")
    fun getAllWorkflows(): Flow<List<Workflow>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun getWorkflowById(id: Int): Workflow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: Workflow): Long

    @Delete
    suspend fun deleteWorkflow(workflow: Workflow)

    // --- Logs ---
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE workflowId = :workflowId ORDER BY timestamp DESC")
    fun getLogsForWorkflow(workflowId: Int): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM log_entries")
    suspend fun clearAllLogs()

    // --- Workspace Files ---
    @Query("SELECT * FROM workspace_files ORDER BY path ASC")
    fun getAllFiles(): Flow<List<WorkspaceFile>>

    @Query("SELECT * FROM workspace_files WHERE path = :path")
    suspend fun getFileByPath(path: String): WorkspaceFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: WorkspaceFile)

    @Delete
    suspend fun deleteFile(file: WorkspaceFile)

    @Query("DELETE FROM workspace_files WHERE path LIKE :prefix || '%'")
    suspend fun deleteFilesWithPrefix(prefix: String)

    // --- Library Items ---
    @Query("SELECT * FROM library_items ORDER BY id DESC")
    fun getAllLibraryItems(): Flow<List<LibraryItem>>

    @Query("SELECT * FROM library_items WHERE type = :type ORDER BY id DESC")
    fun getLibraryItemsByType(type: String): Flow<List<LibraryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItem(item: LibraryItem): Long

    @Delete
    suspend fun deleteLibraryItem(item: LibraryItem)

    // --- Secrets ---
    @Query("SELECT * FROM secrets ORDER BY `key` ASC")
    fun getAllSecrets(): Flow<List<SecretKey>>

    @Query("SELECT * FROM secrets WHERE `key` = :key")
    suspend fun getSecret(key: String): SecretKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecret(secret: SecretKey)

    @Query("DELETE FROM secrets WHERE `key` = :key")
    suspend fun deleteSecret(key: String)
}
