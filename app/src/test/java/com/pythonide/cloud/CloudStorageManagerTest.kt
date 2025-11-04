package com.pythonide.cloud

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for Cloud Storage Manager functionality
 */
@RunWith(MockitoJUnitRunner::class)
class CloudStorageManagerTest {

    @Mock
    private lateinit var syncEngine: SyncEngine

    private lateinit var cloudManager: CloudStorageManager

    private val testUserId = "test-user-123"
    private val testProjectId = "test-project-456"

    @Before
    fun setUp() {
        cloudManager = CloudStorageManager(syncEngine)
    }

    @Test
    fun `test project synchronization`() {
        // Given
        val projectData = mapOf(
            "name" to "Test Project",
            "files" to listOf("main.py", "utils.py"),
            "lastModified" to System.currentTimeMillis()
        )
        
        `when`(syncEngine.syncProject(testProjectId, projectData))
            .thenReturn(true)

        // When
        val result = cloudManager.syncProject(testProjectId, projectData)

        // Then
        assertTrue(result)
        verify(syncEngine).syncProject(testProjectId, projectData)
    }

    @Test
    fun `test file upload`() {
        // Given
        val fileName = "main.py"
        val fileContent = """
            def main():
                print("Hello, World!")
                
            if __name__ == "__main__":
                main()
        """.trimIndent()
        
        val fileInfo = CloudStorageManager.FileInfo(
            name = fileName,
            content = fileContent,
            size = fileContent.length.toLong(),
            lastModified = System.currentTimeMillis()
        )
        
        `when`(syncEngine.uploadFile(testUserId, fileInfo))
            .thenReturn("cloud-file-id-123")

        // When
        val uploadResult = cloudManager.uploadFile(testUserId, fileInfo)

        // Then
        assertNotNull(uploadResult)
        assertEquals("cloud-file-id-123", uploadResult.fileId)
        assertEquals(fileName, uploadResult.fileName)
        verify(syncEngine).uploadFile(testUserId, fileInfo)
    }

    @Test
    fun `test file download`() {
        // Given
        val fileId = "cloud-file-id-123"
        val expectedContent = """
            def main():
                print("Hello, World!")
        """.trimIndent()
        
        `when`(syncEngine.downloadFile(fileId))
            .thenReturn(expectedContent)

        // When
        val downloadResult = cloudManager.downloadFile(fileId)

        // Then
        assertNotNull(downloadResult)
        assertEquals(expectedContent, downloadResult)
        verify(syncEngine).downloadFile(fileId)
    }

    @Test
    fun `test project backup`() {
        // Given
        val projectFiles = listOf(
            CloudStorageManager.FileInfo("main.py", "print('hello')", 12L, System.currentTimeMillis()),
            CloudStorageManager.FileInfo("config.py", "DEBUG=True", 9L, System.currentTimeMillis())
        )
        
        `when`(syncEngine.createBackup(testProjectId, projectFiles))
            .thenReturn("backup-id-789")

        // When
        val backupId = cloudManager.createBackup(testProjectId, projectFiles)

        // Then
        assertNotNull(backupId)
        assertEquals("backup-id-789", backupId)
        verify(syncEngine).createBackup(testProjectId, projectFiles)
    }

    @Test
    fun `test project restore`() {
        // Given
        val backupId = "backup-id-789"
        val restoredFiles = mapOf(
            "main.py" to "print('restored')",
            "config.py" to "DEBUG=False"
        )
        
        `when`(syncEngine.restoreFromBackup(backupId))
            .thenReturn(restoredFiles)

        // When
        val result = cloudManager.restoreProject(backupId)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("print('restored')", result["main.py"])
        verify(syncEngine).restoreFromBackup(backupId)
    }

    @Test
    fun `test conflict resolution`() {
        // Given
        val localVersion = CloudStorageManager.FileVersion(
            content = "print('local version')",
            timestamp = 1000000L,
            version = 1
        )
        
        val cloudVersion = CloudStorageManager.FileVersion(
            content = "print('cloud version')",
            timestamp = 2000000L,
            version = 2
        )

        // When
        val resolved = cloudManager.resolveConflict(localVersion, cloudVersion)

        // Then
        assertNotNull(resolved)
        assertEquals(cloudVersion.content, resolved.content) // Cloud version should win
        assertEquals(2, resolved.version)
    }

    @Test
    fun `test sync status monitoring`() {
        // Given
        val testProjectId = "test-project-sync"
        
        // When
        val status = cloudManager.getSyncStatus(testProjectId)

        // Then
        assertNotNull(status)
        assertTrue(status is CloudStorageManager.SyncStatus.Synced || 
                  status is CloudStorageManager.SyncStatus.Syncing ||
                  status is CloudStorageManager.SyncStatus.Error)
    }

    @Test
    fun `test offline mode functionality`() {
        // Given
        val offlineFiles = listOf(
            CloudStorageManager.FileInfo("offline.py", "print('offline')", 15L, System.currentTimeMillis())
        )

        // When
        cloudManager.enableOfflineMode()
        val queuedForSync = cloudManager.queueForSync(offlineFiles)

        // Then
        assertTrue(queuedForSync)
        assertTrue(cloudManager.isOfflineModeEnabled())
        
        // Simulate coming back online
        cloudManager.disableOfflineMode()
        // Files should sync automatically
    }
}