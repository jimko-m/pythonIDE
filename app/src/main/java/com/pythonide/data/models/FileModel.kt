package com.pythonide.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

/**
 * File model representing a file in the editor
 */
@Entity(tableName = "files")
data class FileModel(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "path")
    val path: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "file_type")
    val fileType: String,
    
    @ColumnInfo(name = "size")
    val size: Long,
    
    @ColumnInfo(name = "last_modified")
    val lastModified: Date,
    
    @ColumnInfo(name = "created_date")
    val createdDate: Date,
    
    @ColumnInfo(name = "is_modified")
    val isModified: Boolean,
    
    @ColumnInfo(name = "parent_folder")
    val parentFolder: String?,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean,
    
    @ColumnInfo(name = "tags")
    val tags: String?, // JSON array of tags
    
    @ColumnInfo(name = "git_status")
    val gitStatus: String? = null, // untracked, modified, added, deleted, etc.
    
    @ColumnInfo(name = "encoding")
    val encoding: String = "UTF-8",
    
    @ColumnInfo(name = "line_ending")
    val lineEnding: String = "LF", // LF, CRLF, CR
    
    @ColumnInfo(name = "auto_save")
    val autoSave: Boolean = true,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null
)

/**
 * Git status for files
 */
enum class GitFileStatus(val displayName: String, val color: String) {
    UNTRACKED("Untracked", "#FF9500"),
    MODIFIED("Modified", "#FF9B00"),
    ADDED("Added", "#0A7F26"),
    DELETED("Deleted", "#D73A49"),
    RENAMED("Renamed", "#9B59B6"),
    COPIED("Copied", "#3498DB"),
    UNMODIFIED("Unmodified", "#95A5A6"),
    IGNORED("Ignored", "#BDC3C7"),
    CONFLICT("Conflict", "#FF6B6B");
    
    companion object {
        fun fromString(status: String?): GitFileStatus {
            return values().find { it.name.equals(status, ignoreCase = true) ?: false } ?: UNMODIFIED
        }
    }
}