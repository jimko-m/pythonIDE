package com.pythonide

import android.app.Application
import androidx.room.Room
import com.pythonide.data.database.AppDatabase
import com.pythonide.data.repository.FileRepository
import com.pythonide.data.repository.GitRepository
import com.pythonide.services.TerminalService
import com.pythonide.services.GitService
import com.pythonide.services.FileSyncService
import com.pythonide.utils.CrashHandler
import com.pythonide.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Application class for Python IDE
 * Initializes global components and services
 */
class PythonIDEApplication : Application() {
    
    // Database
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "python_ide_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    // Repositories
    val fileRepository by lazy { FileRepository(database.fileDao()) }
    val gitRepository by lazy { GitRepository(database.gitDao()) }
    
    // Coroutine scope for app-wide operations
    val applicationScope = CoroutineScope(SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize theme manager
        ThemeManager.initialize(this)
        
        // Initialize crash handler
        CrashHandler.initialize(this)
        
        // Initialize services
        initializeServices()
        
        // Setup application-wide configurations
        setupApplication()
        
        // Initialize database
        setupDatabase()
    }
    
    private fun initializeServices() {
        // Initialize terminal service
        TerminalService.initialize(this)
        
        // Initialize git service
        GitService.initialize(this)
        
        // Initialize file sync service
        FileSyncService.initialize(this)
    }
    
    private fun setupApplication() {
        // Enable strict mode for debug builds
        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }
    
    private fun setupDatabase() {
        // Pre-populate database with default settings if needed
        applicationScope.launch {
            // Add any database initialization here
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clean up any caches or temporary data
        clearCaches()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_MODERATE -> {
                // Moderate memory pressure - clean some caches
                clearCaches()
            }
            TRIM_MEMORY_COMPLETE -> {
                // Critical memory pressure - clean all caches
                clearAllCaches()
            }
        }
    }
    
    private fun clearCaches() {
        // Clear any non-critical caches
        // Implementation depends on specific cache usage
    }
    
    private fun clearAllCaches() {
        // Clear all possible caches
        // Implementation depends on specific cache usage
    }
}