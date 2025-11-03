package com.pythonide.data.repository

import android.content.Context
import com.pythonide.data.database.GitDao
import com.pythonide.data.models.*
import com.pythonide.utils.GitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import java.util.*

/**
 * Repository for Git operations
 * Handles business logic for Git version control
 */
class GitRepository(private val gitDao: GitDao) {
    
    /**
     * Get all repositories
     */
    suspend fun getAllRepositories(): List<GitRepository> = withContext(Dispatchers.IO) {
        gitDao.getAllRepositories()
    }
    
    /**
     * Get repository by ID
     */
    suspend fun getRepositoryById(repoId: String): com.pythonide.data.models.GitRepository? = withContext(Dispatchers.IO) {
        gitDao.getRepositoryById(repoId)
    }
    
    /**
     * Initialize new repository
     */
    suspend fun initializeRepository(
        path: String,
        name: String,
        remoteUrl: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            
            // Check if repository already exists
            if (GitHelper.isGitRepository(path)) {
                return@withContext null
            }
            
            // Initialize new repository
            Git.init().setDirectory(directory).call()
            
            val repoId = UUID.randomUUID().toString()
            val repository = com.pythonide.data.models.GitRepository(
                id = repoId,
                name = name,
                path = path,
                remoteUrl = remoteUrl,
                currentBranch = "main",
                lastCommitHash = null,
                lastCommitMessage = null,
                lastCommitDate = null,
                isDirty = false,
                aheadCount = 0,
                behindCount = 0,
                createdDate = Date(),
                lastSyncDate = null,
                autoSync = false,
                credentials = null,
                ignorePatterns = null,
                settings = null
            )
            
            gitDao.insertRepository(repository)
            repoId
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clone repository
     */
    suspend fun cloneRepository(
        remoteUrl: String,
        localPath: String,
        credentials: GitCredentials? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(localPath)
            
            // Clone repository
            val git = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(directory)
                .setCloneAllBranches(false)
                .call()
            
            git.close()
            
            val repoId = UUID.randomUUID().toString()
            val repository = com.pythonide.data.models.GitRepository(
                id = repoId,
                name = directory.name,
                path = localPath,
                remoteUrl = remoteUrl,
                currentBranch = "main",
                lastCommitHash = null,
                lastCommitMessage = null,
                lastCommitDate = null,
                isDirty = false,
                aheadCount = 0,
                behindCount = 0,
                createdDate = Date(),
                lastSyncDate = null,
                autoSync = false,
                credentials = null,
                ignorePatterns = null,
                settings = null
            )
            
            gitDao.insertRepository(repository)
            repoId
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get repository status
     */
    suspend fun getRepositoryStatus(repoId: String): GitStatus? = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext null
            
            val git = Git.open(File(repository.path))
            val status = GitHelper.getGitStatus(git)
            
            git.close()
            status
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Add files to staging
     */
    suspend fun addFiles(repoId: String, filePatterns: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            
            val git = Git.open(File(repository.path))
            val addCommand = git.add()
            
            filePatterns.forEach { pattern ->
                addCommand.addFilepattern(pattern)
            }
            
            addCommand.call()
            git.close()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Commit changes
     */
    suspend fun commitChanges(
        repoId: String,
        message: String,
        authorName: String,
        authorEmail: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext null
            
            val git = Git.open(File(repository.path))
            
            val commitCommand = git.commit()
                .setMessage(message)
                .setAuthor(authorName, authorEmail)
            
            val commit = commitCommand.call()
            
            // Update repository info
            gitDao.updateRepositoryStatus(
                repoId = repoId,
                branchName = repository.currentBranch,
                commitHash = commit.name,
                commitMessage = commit.fullMessage,
                commitDate = Date(commit.commitTime * 1000L),
                isDirty = false
            )
            
            git.close()
            commit.name
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get commit history
     */
    suspend fun getCommitHistory(
        repoId: String,
        limit: Int = 50
    ): List<GitCommit> = withContext(Dispatchers.IO) {
        try {
            gitDao.getCommitsForRepository(repoId, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get branches
     */
    suspend fun getBranches(repoId: String): List<GitBranch> = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext emptyList()
            
            val git = Git.open(File(repository.path))
            val branches = GitHelper.getBranches(git)
            
            git.close()
            
            // Save to database
            gitDao.deleteBranchesForRepository(repoId)
            gitDao.insertBranches(branches)
            
            branches
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Create new branch
     */
    suspend fun createBranch(
        repoId: String,
        branchName: String,
        startPoint: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            
            val git = Git.open(File(repository.path))
            
            val branchCommand = git.branchCreate()
                .setName(branchName)
            
            startPoint?.let {
                branchCommand.setStartPoint(it)
            }
            
            branchCommand.call()
            git.close()
            
            // Refresh branches list
            getBranches(repoId)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Switch branch
     */
    suspend fun switchBranch(
        repoId: String,
        branchName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            
            val git = Git.open(File(repository.path))
            
            git.checkout().setName(branchName).call()
            
            // Update current branch in database
            val branches = gitDao.getBranchesForRepository(repoId)
            val targetBranch = branches.find { it.name == branchName }
            
            gitDao.clearCurrentBranch(repoId)
            targetBranch?.let {
                gitDao.insertBranch(it.copy(isCurrent = true))
            }
            
            git.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Merge branch
     */
    suspend fun mergeBranch(
        repoId: String,
        branchName: String,
        fastForward: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            
            val git = Git.open(File(repository.path))
            
            val mergeCommand = git.merge().include(branchName)
            
            if (fastForward) {
                mergeCommand.setFastForward(MergeCommand.FastForwardOnly.FAST_FORWARD_ONLY)
            }
            
            mergeCommand.call()
            git.close()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Pull changes from remote
     */
    suspend fun pullChanges(
        repoId: String,
        credentials: GitCredentials? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            val git = Git.open(File(repository.path))
            
            // Configure credentials if provided
            credentials?.let {
                GitHelper.configureCredentials(git, it)
            }
            
            val pullCommand = git.pull()
            val result = pullCommand.call()
            
            // Update repository sync info
            gitDao.updateRepository(
                repository = repository.copy(
                    lastSyncDate = Date(),
                    aheadCount = 0,
                    behindCount = 0
                )
            )
            
            git.close()
            result.isSuccessful
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Push changes to remote
     */
    suspend fun pushChanges(
        repoId: String,
        credentials: GitCredentials? = null,
        force: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            val git = Git.open(File(repository.path))
            
            // Configure credentials if provided
            credentials?.let {
                GitHelper.configureCredentials(git, it)
            }
            
            val pushCommand = git.push()
            
            if (force) {
                pushCommand.setForce(FetchResult.FetchRefUpdateResultType.FORCED)
            }
            
            val result = pushCommand.call()
            
            git.close()
            result.all { it.status == PushResult.Status.OK }
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get file differences
     */
    suspend fun getDiff(
        repoId: String,
        oldCommit: String,
        newCommit: String,
        filePath: String? = null
    ): List<GitDiff> = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext emptyList()
            val git = Git.open(File(repository.path))
            
            val diffs = GitHelper.getDiffs(git, oldCommit, newCommit, filePath)
            
            git.close()
            diffs
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Configure Git settings
     */
    suspend fun configureSettings(
        repoId: String,
        settings: GitSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            
            // Convert settings to JSON
            val settingsJson = GitHelper.serializeSettings(settings)
            
            gitDao.updateRepository(
                repository = repository.copy(
                    settings = settingsJson
                )
            )
            
            // Apply settings to repository
            val git = Git.open(File(repository.path))
            GitHelper.applySettings(git, settings)
            git.close()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete repository
     */
    suspend fun deleteRepository(repoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val repository = gitDao.getRepositoryById(repoId) ?: return@withContext false
            
            // Delete from database
            gitDao.deleteRepositoryById(repoId)
            
            // Optionally delete physical repository directory
            // File(repository.path).deleteRecursively()
            
            true
        } catch (e: Exception) {
            false
        }
    }
}