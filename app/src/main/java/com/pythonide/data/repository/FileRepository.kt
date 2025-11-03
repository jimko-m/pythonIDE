package com.pythonide.data.repository

import android.content.Context
import com.pythonide.data.database.FileDao
import com.pythonide.data.models.FileModel
import com.pythonide.data.models.GitFileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Repository for file operations
 * Handles business logic for file management
 */
class FileRepository(private val fileDao: FileDao) {
    
    /**
     * Get all files from database
     */
    suspend fun getAllFiles(): List<FileModel> = withContext(Dispatchers.IO) {
        fileDao.getAllFiles()
    }
    
    /**
     * Get file by ID
     */
    suspend fun getFileById(fileId: String): FileModel? = withContext(Dispatchers.IO) {
        fileDao.getFileById(fileId)
    }
    
    /**
     * Get files in specific folder
     */
    suspend fun getFilesInFolder(folderPath: String): List<FileModel> = withContext(Dispatchers.IO) {
        fileDao.getFilesInFolder(folderPath)
    }
    
    /**
     * Get favorite files
     */
    suspend fun getFavoriteFiles(): List<FileModel> = withContext(Dispatchers.IO) {
        fileDao.getFavoriteFiles()
    }
    
    /**
     * Search files by name or content
     */
    suspend fun searchFiles(query: String): List<FileModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            emptyList()
        } else {
            fileDao.searchFiles("%$query%")
        }
    }
    
    /**
     * Save file content
     */
    suspend fun saveFile(fileId: String, content: String, isAutoSave: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentFile = fileDao.getFileById(fileId) ?: return@withContext false
            
            // Update database record
            val updatedFile = currentFile.copy(
                content = content,
                lastModified = Date(),
                isModified = !isAutoSave
            )
            
            fileDao.updateFileContent(
                fileId = fileId,
                content = content,
                lastModified = updatedFile.lastModified,
                isModified = updatedFile.isModified
            )
            
            // Also save to physical file if not auto-save
            if (!isAutoSave) {
                val file = File(currentFile.path)
                file.parentFile?.mkdirs()
                file.writeText(content)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create new file
     */
    suspend fun createFile(
        name: String,
        path: String,
        content: String = "",
        fileType: String = getFileTypeFromName(name),
        parentFolder: String? = null
    ): FileModel? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            
            val fileModel = FileModel(
                id = UUID.randomUUID().toString(),
                name = name,
                path = path,
                content = content,
                fileType = fileType,
                size = 0,
                lastModified = Date(),
                createdDate = Date(),
                isModified = false,
                parentFolder = parentFolder,
                isFavorite = false,
                tags = null,
                gitStatus = null
            )
            
            // Write to physical file
            file.writeText(content)
            fileModel.size = file.length()
            
            // Save to database
            fileDao.insertFile(fileModel)
            
            fileModel
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete file
     */
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = fileDao.getFileById(fileId) ?: return@withContext false
            
            // Delete physical file
            File(file.path).delete()
            
            // Delete from database
            fileDao.deleteFile(file)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Rename file
     */
    suspend fun renameFile(fileId: String, newName: String): FileModel? = withContext(Dispatchers.IO) {
        try {
            val currentFile = fileDao.getFileById(fileId) ?: return@withContext null
            
            val oldFile = File(currentFile.path)
            val newPath = oldFile.parent + File.separator + newName
            val newFile = File(newPath)
            
            // Rename physical file
            if (oldFile.renameTo(newFile)) {
                val updatedFile = currentFile.copy(
                    name = newName,
                    path = newPath,
                    lastModified = Date(),
                    isModified = false
                )
                
                fileDao.updateFile(updatedFile)
                updatedFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = fileDao.getFileById(fileId) ?: return@withContext false
            
            val newFavoriteStatus = !file.isFavorite
            fileDao.toggleFavorite(fileId, newFavoriteStatus)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Import file from external source
     */
    suspend fun importFile(
        context: Context,
        uri: android.net.Uri,
        targetPath: String
    ): FileModel? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val targetFile = File(targetPath)
                targetFile.parentFile?.mkdirs()
                
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                
                val fileName = getFileNameFromUri(uri)
                val fileType = getFileTypeFromName(fileName)
                
                val fileModel = FileModel(
                    id = UUID.randomUUID().toString(),
                    name = fileName,
                    path = targetPath,
                    content = targetFile.readText(),
                    fileType = fileType,
                    size = targetFile.length(),
                    lastModified = Date(),
                    createdDate = Date(),
                    isModified = false,
                    parentFolder = targetFile.parent,
                    isFavorite = false,
                    tags = null,
                    gitStatus = null
                )
                
                fileDao.insertFile(fileModel)
                fileModel
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Export file to external location
     */
    suspend fun exportFile(
        context: Context,
        fileId: String,
        targetUri: android.net.Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = fileDao.getFileById(fileId) ?: return@withContext false
            
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                File(file.path).inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get files by type
     */
    suspend fun getFilesByType(fileType: String): List<FileModel> = withContext(Dispatchers.IO) {
        fileDao.getFilesByType(fileType)
    }
    
    /**
     * Update git status for file
     */
    suspend fun updateGitStatus(fileId: String, status: GitFileStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            fileDao.updateGitStatus(fileId, status.name)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Scan directory and sync with database
     */
    suspend fun scanDirectory(directoryPath: String): List<FileModel> = withContext(Dispatchers.IO) {
        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext emptyList()
            }
            
            val files = mutableListOf<File>()
            
            // Recursively scan directory
            fun scanRecursive(dir: File) {
                if (dir.isDirectory && dir.canRead()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.canRead()) {
                            files.add(file)
                        } else if (file.isDirectory && file.canRead()) {
                            scanRecursive(file)
                        }
                    }
                }
            }
            
            scanRecursive(directory)
            
            val fileModels = files.map { file ->
                FileModel(
                    id = UUID.randomUUID().toString(),
                    name = file.name,
                    path = file.absolutePath,
                    content = try {
                        file.readText()
                    } catch (e: Exception) {
                        ""
                    },
                    fileType = getFileTypeFromName(file.name),
                    size = file.length(),
                    lastModified = Date(file.lastModified()),
                    createdDate = Date(file.lastModified()),
                    isModified = false,
                    parentFolder = file.parent,
                    isFavorite = false,
                    tags = null,
                    gitStatus = null
                )
            }
            
            // Save to database
            fileDao.insertFiles(fileModels)
            
            fileModels
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    companion object {
        private fun getFileTypeFromName(fileName: String): String {
            return when {
                fileName.endsWith(".py", ignoreCase = true) -> "py"
                fileName.endsWith(".java", ignoreCase = true) -> "java"
                fileName.endsWith(".kt", ignoreCase = true) -> "kt"
                fileName.endsWith(".js", ignoreCase = true) -> "js"
                fileName.endsWith(".ts", ignoreCase = true) -> "ts"
                fileName.endsWith(".html", ignoreCase = true) -> "html"
                fileName.endsWith(".css", ignoreCase = true) -> "css"
                fileName.endsWith(".json", ignoreCase = true) -> "json"
                fileName.endsWith(".xml", ignoreCase = true) -> "xml"
                fileName.endsWith(".md", ignoreCase = true) -> "md"
                fileName.endsWith(".txt", ignoreCase = true) -> "txt"
                else -> "txt"
            }
        }
        
        private fun getFileNameFromUri(uri: android.net.Uri): String {
            // Try to extract filename from URI
            uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
        }
    }
}