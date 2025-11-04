package com.pythonide.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

/**
 * Git repository model
 */
@Entity(tableName = "git_repositories")
data class GitRepository(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "path")
    val path: String,
    
    @ColumnInfo(name = "remote_url")
    val remoteUrl: String?,
    
    @ColumnInfo(name = "branch")
    val currentBranch: String,
    
    @ColumnInfo(name = "last_commit_hash")
    val lastCommitHash: String?,
    
    @ColumnInfo(name = "last_commit_message")
    val lastCommitMessage: String?,
    
    @ColumnInfo(name = "last_commit_date")
    val lastCommitDate: Date?,
    
    @ColumnInfo(name = "is_dirty")
    val isDirty: Boolean,
    
    @ColumnInfo(name = "ahead_count")
    val aheadCount: Int,
    
    @ColumnInfo(name = "behind_count")
    val behindCount: Int,
    
    @ColumnInfo(name = "created_date")
    val createdDate: Date,
    
    @ColumnInfo(name = "last_sync_date")
    val lastSyncDate: Date?,
    
    @ColumnInfo(name = "auto_sync")
    val autoSync: Boolean,
    
    @ColumnInfo(name = "credentials")
    val credentials: String?, // Encrypted JSON
    
    @ColumnInfo(name = "ignore_patterns")
    val ignorePatterns: String?, // .gitignore content
    
    @ColumnInfo(name = "settings")
    val settings: String? // JSON settings
)

/**
 * Git commit model
 */
@Entity(tableName = "git_commits")
data class GitCommit(
    @PrimaryKey
    @ColumnInfo(name = "hash")
    val hash: String,
    
    @ColumnInfo(name = "repository_id")
    val repositoryId: String,
    
    @ColumnInfo(name = "message")
    val message: String,
    
    @ColumnInfo(name = "author_name")
    val authorName: String,
    
    @ColumnInfo(name = "author_email")
    val authorEmail: String,
    
    @ColumnInfo(name = "committer_name")
    val committerName: String,
    
    @ColumnInfo(name = "committer_email")
    val committerEmail: String,
    
    @ColumnInfo(name = "commit_date")
    val commitDate: Date,
    
    @ColumnInfo(name = "parent_hashes")
    val parentHashes: String, // JSON array
    
    @ColumnInfo(name = "tree_hash")
    val treeHash: String,
    
    @ColumnInfo(name = "changes")
    val changes: String, // JSON array of file changes
    
    @ColumnInfo(name = "stats")
    val stats: String?, // JSON stats
    
    @ColumnInfo(name = "is_merge_commit")
    val isMergeCommit: Boolean
)

/**
 * Git branch model
 */
@Entity(tableName = "git_branches")
data class GitBranch(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "repository_id")
    val repositoryId: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "full_name")
    val fullName: String, // refs/heads/branch_name
    
    @ColumnInfo(name = "commit_hash")
    val commitHash: String,
    
    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean,
    
    @ColumnInfo(name = "is_local")
    val isLocal: Boolean,
    
    @ColumnInfo(name = "is_remote")
    val isRemote: Boolean,
    
    @ColumnInfo(name = "remote_name")
    val remoteName: String?,
    
    @ColumnInfo(name = "tracking_branch")
    val trackingBranch: String?,
    
    @ColumnInfo(name = "ahead_count")
    val aheadCount: Int,
    
    @ColumnInfo(name = "behind_count")
    val behindCount: Int,
    
    @ColumnInfo(name = "last_commit_date")
    val lastCommitDate: Date,
    
    @ColumnInfo(name = "is_protected")
    val isProtected: Boolean
)

/**
 * Git file change model
 */
data class GitFileChange(
    val path: String,
    val oldPath: String?,
    val changeType: ChangeType,
    val isStaged: Boolean,
    val isUntracked: Boolean,
    val isModified: Boolean,
    val isDeleted: Boolean,
    val renameScore: Int? = null,
    val oldFileMode: Int? = null,
    val newFileMode: Int? = null,
    val oldFileSize: Long? = null,
    val newFileSize: Long? = null
) {
    enum class ChangeType(val displayName: String) {
        ADD("Add"),
        MODIFY("Modify"),
        DELETE("Delete"),
        RENAME("Rename"),
        COPY("Copy");
        
        companion object {
            fun fromString(type: String): ChangeType {
                return values().find { it.name.equals(type, ignoreCase = true) } ?: MODIFY
            }
        }
    }
}

/**
 * Git status summary
 */
data class GitStatus(
    val isInitialized: Boolean,
    val isBare: Boolean,
    val currentBranch: String?,
    val isDetached: Boolean,
    val detachedCommit: String?,
    val isClean: Boolean,
    val stagedFiles: List<GitFileChange>,
    val modifiedFiles: List<GitFileChange>,
    val untrackedFiles: List<GitFileChange>,
    val missingFiles: List<String>,
    val renamedFiles: List<GitFileChange>,
    val copiedFiles: List<GitFileChange>,
    val conflicts: List<String>,
    val aheadCount: Int,
    val behindCount: Int,
    val remoteUrl: String?,
    val lastFetchDate: Date?
)

/**
 * Git diff model
 */
data class GitDiff(
    val oldPath: String,
    val newPath: String,
    val changeType: GitFileChange.ChangeType,
    val oldMode: Int,
    val newMode: Int,
    val oldFileSize: Long,
    val newFileSize: Long,
    val binaryFile: Boolean,
    val patch: String?
)

/**
 * Git settings model
 */
data class GitSettings(
    val userName: String?,
    val userEmail: String?,
    val defaultBranch: String = "main",
    val autoCommit: Boolean = false,
    val autoSync: Boolean = false,
    val syncInterval: Long = 300000L, // 5 minutes
    val autoStash: Boolean = true,
    val signCommits: Boolean = false,
    val gpgKeyId: String?,
    val sshKeyPath: String?,
    val credentialHelper: String?,
    val ignoreWhitespace: Boolean = false,
    val verboseCommits: Boolean = true,
    val lineEnding: String = "LF",
    val characterEncoding: String = "UTF-8"
)