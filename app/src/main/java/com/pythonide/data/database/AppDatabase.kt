package com.pythonide.data.database

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.pythonide.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Room Database for Python IDE
 * Manages all data persistence operations
 */
@Database(
    entities = [
        FileModel::class,
        GitRepository::class,
        GitCommit::class,
        GitBranch::class,
        TerminalSession::class,
        TerminalCommand::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun fileDao(): FileDao
    abstract fun gitDao(): GitDao
    abstract fun terminalDao(): TerminalDao
    
    companion object {
        const val DATABASE_NAME = "python_ide_database"
    }
}

/**
 * File DAO for database operations
 */
@Dao
interface FileDao {
    
    @Query("SELECT * FROM files ORDER BY last_modified DESC")
    suspend fun getAllFiles(): List<FileModel>
    
    @Query("SELECT * FROM files WHERE id = :fileId")
    suspend fun getFileById(fileId: String): FileModel?
    
    @Query("SELECT * FROM files WHERE name LIKE :searchQuery OR content LIKE :searchQuery ORDER BY last_modified DESC")
    suspend fun searchFiles(searchQuery: String): List<FileModel>
    
    @Query("SELECT * FROM files WHERE parent_folder = :parentFolder ORDER BY is_favorite DESC, name ASC")
    suspend fun getFilesInFolder(parentFolder: String): List<FileModel>
    
    @Query("SELECT * FROM files WHERE is_favorite = 1 ORDER BY last_modified DESC")
    suspend fun getFavoriteFiles(): List<FileModel>
    
    @Query("SELECT * FROM files WHERE file_type = :fileType ORDER BY last_modified DESC")
    suspend fun getFilesByType(fileType: String): List<FileModel>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileModel)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileModel>)
    
    @Update
    suspend fun updateFile(file: FileModel)
    
    @Delete
    suspend fun deleteFile(file: FileModel)
    
    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: String)
    
    @Query("DELETE FROM files WHERE parent_folder = :parentFolder")
    suspend fun deleteFilesInFolder(parentFolder: String)
    
    @Query("DELETE FROM files WHERE last_modified < :thresholdDate")
    suspend fun deleteOldFiles(thresholdDate: Date)
    
    @Query("UPDATE files SET is_favorite = :isFavorite WHERE id = :fileId")
    suspend fun toggleFavorite(fileId: String, isFavorite: Boolean)
    
    @Query("UPDATE files SET content = :content, last_modified = :lastModified, is_modified = :isModified WHERE id = :fileId")
    suspend fun updateFileContent(fileId: String, content: String, lastModified: Date, isModified: Boolean)
    
    @Query("UPDATE files SET git_status = :gitStatus WHERE id = :fileId")
    suspend fun updateGitStatus(fileId: String, gitStatus: String?)
    
    @Query("SELECT COUNT(*) FROM files")
    suspend fun getFileCount(): Int
    
    @Query("SELECT * FROM files WHERE path = :filePath LIMIT 1")
    suspend fun getFileByPath(filePath: String): FileModel?
}

/**
 * Git DAO for database operations
 */
@Dao
interface GitDao {
    
    // Repository operations
    @Query("SELECT * FROM git_repositories ORDER BY last_sync_date DESC")
    suspend fun getAllRepositories(): List<GitRepository>
    
    @Query("SELECT * FROM git_repositories WHERE id = :repoId")
    suspend fun getRepositoryById(repoId: String): GitRepository?
    
    @Query("SELECT * FROM git_repositories WHERE path = :path LIMIT 1")
    suspend fun getRepositoryByPath(path: String): GitRepository?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repository: GitRepository)
    
    @Update
    suspend fun updateRepository(repository: GitRepository)
    
    @Delete
    suspend fun deleteRepository(repository: GitRepository)
    
    @Query("DELETE FROM git_repositories WHERE id = :repoId")
    suspend fun deleteRepositoryById(repoId: String)
    
    // Commit operations
    @Query("SELECT * FROM git_commits WHERE repository_id = :repoId ORDER BY commit_date DESC LIMIT :limit")
    suspend fun getCommitsForRepository(repoId: String, limit: Int = 100): List<GitCommit>
    
    @Query("SELECT * FROM git_commits WHERE hash = :commitHash")
    suspend fun getCommitByHash(commitHash: String): GitCommit?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommit(commit: GitCommit)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommits(commits: List<GitCommit>)
    
    @Query("DELETE FROM git_commits WHERE repository_id = :repoId")
    suspend fun deleteCommitsForRepository(repoId: String)
    
    // Branch operations
    @Query("SELECT * FROM git_branches WHERE repository_id = :repoId ORDER BY is_current DESC, name ASC")
    suspend fun getBranchesForRepository(repoId: String): List<GitBranch>
    
    @Query("SELECT * FROM git_branches WHERE repository_id = :repoId AND is_current = 1")
    suspend fun getCurrentBranch(repoId: String): GitBranch?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: GitBranch)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<GitBranch>)
    
    @Query("UPDATE git_branches SET is_current = 0 WHERE repository_id = :repoId")
    suspend fun clearCurrentBranch(repoId: String)
    
    @Query("DELETE FROM git_branches WHERE repository_id = :repoId")
    suspend fun deleteBranchesForRepository(repoId: String)
    
    @Query("UPDATE git_repositories SET current_branch = :branchName, last_commit_hash = :commitHash, last_commit_message = :commitMessage, last_commit_date = :commitDate, is_dirty = :isDirty WHERE id = :repoId")
    suspend fun updateRepositoryStatus(repoId: String, branchName: String, commitHash: String?, commitMessage: String?, commitDate: Date?, isDirty: Boolean)
}

/**
 * Terminal DAO for database operations
 */
@Dao
interface TerminalDao {
    
    // Session operations
    @Query("SELECT * FROM terminal_sessions ORDER BY is_active DESC, last_activity DESC")
    suspend fun getAllSessions(): List<TerminalSession>
    
    @Query("SELECT * FROM terminal_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): TerminalSession?
    
    @Query("SELECT * FROM terminal_sessions WHERE is_active = 1")
    suspend fun getActiveSessions(): List<TerminalSession>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TerminalSession)
    
    @Update
    suspend fun updateSession(session: TerminalSession)
    
    @Delete
    suspend fun deleteSession(session: TerminalSession)
    
    @Query("UPDATE terminal_sessions SET is_active = 0 WHERE id != :activeSessionId")
    suspend fun deactivateAllSessions(activeSessionId: String)
    
    @Query("UPDATE terminal_sessions SET last_activity = :lastActivity WHERE id = :sessionId")
    suspend fun updateSessionActivity(sessionId: String, lastActivity: Date)
    
    // Command operations
    @Query("SELECT * FROM terminal_commands WHERE session_id = :sessionId ORDER BY start_time DESC LIMIT :limit")
    suspend fun getCommandsForSession(sessionId: String, limit: Int = 100): List<TerminalCommand>
    
    @Query("SELECT * FROM terminal_commands WHERE is_running = 1")
    suspend fun getRunningCommands(): List<TerminalCommand>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: TerminalCommand)
    
    @Update
    suspend fun updateCommand(command: TerminalCommand)
    
    @Query("UPDATE terminal_commands SET output = :output, exit_code = :exitCode, end_time = :endTime, duration_ms = :durationMs, is_running = :isRunning WHERE id = :commandId")
    suspend fun updateCommandResult(commandId: String, output: String?, exitCode: Int?, endTime: Date?, durationMs: Long?, isRunning: Boolean)
    
    @Query("DELETE FROM terminal_commands WHERE start_time < :thresholdDate")
    suspend fun deleteOldCommands(thresholdDate: Date)
    
    @Query("DELETE FROM terminal_commands WHERE session_id = :sessionId")
    suspend fun deleteCommandsForSession(sessionId: String)
    
    // Cleanup operations
    @Query("SELECT COUNT(*) FROM terminal_commands")
    suspend fun getCommandCount(): Int
    
    @Query("DELETE FROM terminal_commands WHERE is_running = 0 AND end_time < :thresholdDate")
    suspend fun cleanupOldCommands(thresholdDate: Date)
}

/**
 * Database converters for complex types
 */
class Converters {
    
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        return data?.split(",")?.filter { it.isNotEmpty() }
    }
    
    @TypeConverter
    fun fromMap(map: Map<String, String>?): String? {
        return map?.entries?.joinToString(";") { "${it.key}:${it.value}" }
    }
    
    @TypeConverter
    fun toMap(data: String?): Map<String, String>? {
        return data?.split(";")?.mapNotNull { entry ->
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }?.toMap()
    }
}

/**
 * Database callback for initial setup
 */
class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Pre-populate database with default data if needed
        scope.launch {
            // Add any initial data setup here
        }
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Database opened, perform any migrations or setup
    }
}

/**
 * Database migrations (for future use)
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add migration logic here when updating database version
    }
}