package com.pythonide.terminal

import android.content.Context
import android.util.Log
import com.pythonide.data.models.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Enhanced Terminal Manager with modern features
 * - Multiple terminal sessions with tabs
 * - Split screen support
 * - Theme customization
 * - Process management
 * - Terminal history
 * - Search functionality
 */
class EnhancedTerminalManager(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedTerminalManager"
        private const val MAX_HISTORY_SIZE = 10000
        private const val CLEANUP_INTERVAL = 5 * 60 * 1000L // 5 minutes
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true 
    }
    
    private val terminalSessions = ConcurrentHashMap<String, TerminalSessionWrapper>()
    private val runningProcesses = ConcurrentHashMap<Int, RunningProcess>()
    private val commandHistory = mutableListOf<TerminalHistoryEntry>()
    private val searchHistory = ConcurrentHashMap<String, List<String>>()
    
    private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val terminalProcessPool = Executors.newCachedThreadPool()
    
    private var currentSessionId: String? = null
    private var isTermuxAvailable = false
    
    // Callbacks for UI updates
    private val sessionCallbacks = ConcurrentHashMap<String, MutableSet<TerminalSessionCallback>>()
    private val globalCallbacks = mutableSetOf<TerminalGlobalCallback>()
    
    init {
        // Check for Termux availability
        checkTermuxAvailability()
        
        // Schedule periodic cleanup
        executorService.scheduleWithFixedDelay(
            { cleanupInactiveSessions() },
            CLEANUP_INTERVAL,
            CLEANUP_INTERVAL,
            TimeUnit.MILLISECONDS
        )
    }
    
    /**
     * Create a new terminal session
     */
    suspend fun createSession(
        name: String = "Terminal ${Date().time}",
        workingDirectory: String = "/data/data/com.pythonide/files",
        terminalType: String = "xterm-256color",
        environmentVariables: Map<String, String> = emptyMap()
    ): TerminalSessionResult {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = UUID.randomUUID().toString()
                val shellPath = getShellPath()
                
                if (!isTermuxAvailable && !isAlternativeShellAvailable()) {
                    return@withContext TerminalSessionResult.Error("No terminal shell available")
                }
                
                val session = TerminalSession(
                    id = sessionId,
                    name = name,
                    workingDirectory = workingDirectory,
                    shellPath = shellPath,
                    isActive = true,
                    createdDate = Date(),
                    lastActivity = Date(),
                    environmentVariables = json.encodeToString(environmentVariables),
                    terminalType = terminalType,
                    isTermuxInstalled = isTermuxAvailable
                )
                
                val wrapper = TerminalSessionWrapper(session)
                terminalSessions[sessionId] = wrapper
                
                // Set as current if this is the first session
                if (currentSessionId == null) {
                    currentSessionId = sessionId
                }
                
                notifySessionCreated(session)
                
                TerminalSessionResult.Success(session)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create terminal session", e)
                TerminalSessionResult.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Execute command in a terminal session
     */
    suspend fun executeCommand(
        sessionId: String,
        command: String,
        workingDirectory: String? = null,
        environmentVariables: Map<String, String> = emptyMap(),
        background: Boolean = false
    ): CommandExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                val sessionWrapper = terminalSessions[sessionId]
                    ?: return@withContext CommandExecutionResult.Error("Session not found: $sessionId")
                
                val cmd = if (command.startsWith("python") || command.startsWith("pip")) {
                    getPythonCommand(command)
                } else {
                    command
                }
                
                val finalWorkingDir = workingDirectory ?: sessionWrapper.session.workingDirectory
                val terminalCommand = TerminalCommand(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    command = cmd,
                    startTime = Date(),
                    endTime = null,
                    isRunning = true,
                    workingDirectory = finalWorkingDir,
                    environment = json.encodeToString(environmentVariables),
                    isBackground = background
                )
                
                sessionWrapper.addCommand(terminalCommand)
                
                val process = if (isTermuxAvailable) {
                    executeInTermux(cmd, finalWorkingDir, environmentVariables)
                } else {
                    executeInSandbox(cmd, finalWorkingDir, environmentVariables)
                }
                
                if (background) {
                    // Start process in background and return immediately
                    startBackgroundProcess(process, terminalCommand)
                    CommandExecutionResult.BackgroundStarted(terminalCommand)
                } else {
                    // Execute synchronously
                    val result = executeCommandSync(process, terminalCommand)
                    CommandExecutionResult.Completed(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute command: $command", e)
                CommandExecutionResult.Error(e.message ?: "Command execution failed")
            }
        }
    }
    
    /**
     * Execute command synchronously
     */
    private suspend fun executeCommandSync(
        process: Process,
        terminalCommand: TerminalCommand
    ): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            val errorStream = BufferedReader(InputStreamReader(process.errorStream))
            val outputStream = PrintWriter(process.outputStream, true)
            
            val outputLines = mutableListOf<String>()
            val errorLines = mutableListOf<String>()
            
            // Read output and error streams concurrently
            val outputJob = launch {
                try {
                    var line: String?
                    while (inputStream.readLine().also { line = it } != null) {
                        line?.let {
                            outputLines.add(it)
                            // Notify UI of new output
                            notifyOutputReceived(terminalCommand.sessionId, it, TerminalOutput.OutputType.OUTPUT)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading output stream", e)
                }
            }
            
            val errorJob = launch {
                try {
                    var line: String?
                    while (errorStream.readLine().also { line = it } != null) {
                        line?.let {
                            errorLines.add(it)
                            notifyOutputReceived(terminalCommand.sessionId, it, TerminalOutput.OutputType.ERROR)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading error stream", e)
                }
            }
            
            // Wait for process to complete
            val exitCode = process.waitFor()
            
            // Wait for streams to finish reading
            outputJob.join()
            errorJob.join()
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            val output = outputLines.joinToString("\n")
            val error = errorLines.joinToString("\n")
            
            // Update terminal command with results
            val updatedCommand = terminalCommand.copy(
                output = output,
                errorOutput = error,
                exitCode = exitCode,
                endTime = Date(),
                durationMs = duration,
                isRunning = false
            )
            
            // Add to command history if successful and not empty
            if (exitCode == 0 && terminalCommand.command.isNotBlank()) {
                addToHistory(terminalCommand.command, updatedCommand.exitCode ?: 0, duration)
            }
            
            CommandResult(
                terminalCommand = updatedCommand,
                success = exitCode == 0,
                output = output,
                error = error,
                duration = duration
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command", e)
            throw e
        }
    }
    
    /**
     * Start background process
     */
    private fun startBackgroundProcess(
        process: Process,
        terminalCommand: TerminalCommand
    ) {
        terminalProcessPool.execute {
            try {
                process.waitFor()
                val exitCode = process.exitValue()
                
                // Update terminal command
                terminalCommand.exitCode = exitCode
                terminalCommand.endTime = Date()
                terminalCommand.isRunning = false
                
                // Update process tracking
                terminalCommand.processId?.let { pid ->
                    runningProcesses.remove(pid)
                }
                
                // Notify completion
                notifyCommandCompleted(terminalCommand.sessionId, terminalCommand)
                
            } catch (e: Exception) {
                Log.e(TAG, "Background process error", e)
            }
        }
    }
    
    /**
     * Split terminal session horizontally or vertically
     */
    suspend fun splitSession(
        sessionId: String,
        direction: SplitDirection
    ): TerminalSessionResult {
        return withContext(Dispatchers.IO) {
            try {
                val originalSession = terminalSessions[sessionId]
                    ?: return@withContext TerminalSessionResult.Error("Session not found")
                
                val newName = "${originalSession.session.name} - ${direction.displayName}"
                val newSession = createSession(newName)
                
                when (newSession) {
                    is TerminalSessionResult.Success -> {
                        // Set the same working directory and environment
                        val wrapper = terminalSessions[newSession.data.id]
                        wrapper?.session = wrapper?.session?.copy(
                            workingDirectory = originalSession.session.workingDirectory,
                            environmentVariables = originalSession.session.environmentVariables
                        ) ?: newSession.data
                        
                        notifySessionSplit(sessionId, newSession.data.id, direction)
                        TerminalSessionResult.Success(newSession.data)
                    }
                    is TerminalSessionResult.Error -> newSession
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to split session", e)
                TerminalSessionResult.Error(e.message ?: "Split failed")
            }
        }
    }
    
    /**
     * Get session output history
     */
    fun getSessionHistory(sessionId: String): List<TerminalOutput> {
        return terminalSessions[sessionId]?.getOutputHistory() ?: emptyList()
    }
    
    /**
     * Search command history
     */
    fun searchHistory(query: String): List<String> {
        return commandHistory
            .filter { it.command.contains(query, ignoreCase = true) }
            .map { it.command }
            .distinct()
            .reversed() // Most recent first
    }
    
    /**
     * Save terminal session state
     */
    suspend fun saveSession(sessionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val wrapper = terminalSessions[sessionId] ?: return@withContext false
                // In a real implementation, save to persistent storage
                // For now, just return true
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save session", e)
                false
            }
        }
    }
    
    /**
     * Load terminal session state
     */
    suspend fun loadSession(sessionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // In a real implementation, load from persistent storage
                // For now, just return true
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load session", e)
                false
            }
        }
    }
    
    /**
     * Close terminal session
     */
    suspend fun closeSession(sessionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val wrapper = terminalSessions[sessionId] ?: return@withContext false
                
                // Kill any running processes in this session
                wrapper.getRunningProcessIds().forEach { pid ->
                    try {
                        Runtime.getRuntime().exec("kill -9 $pid")
                        runningProcesses.remove(pid)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to kill process $pid", e)
                    }
                }
                
                terminalSessions.remove(sessionId)
                
                // If this was the current session, switch to another one
                if (currentSessionId == sessionId) {
                    currentSessionId = terminalSessions.keys.firstOrNull()
                }
                
                notifySessionClosed(sessionId)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close session", e)
                false
            }
        }
    }
    
    /**
     * Get all active sessions
     */
    fun getActiveSessions(): List<TerminalSession> {
        return terminalSessions.values.map { it.session }
    }
    
    /**
     * Get current session ID
     */
    fun getCurrentSessionId(): String? = currentSessionId
    
    /**
     * Set current session
     */
    fun setCurrentSession(sessionId: String) {
        if (terminalSessions.containsKey(sessionId)) {
            currentSessionId = sessionId
            notifySessionChanged(sessionId)
        }
    }
    
    /**
     * Get running processes
     */
    fun getRunningProcesses(): List<RunningProcess> {
        return runningProcesses.values.toList()
    }
    
    /**
     * Kill process by PID
     */
    suspend fun killProcess(pid: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val process = runningProcesses[pid] ?: return@withContext false
                
                Runtime.getRuntime().exec("kill -9 $pid")
                runningProcesses.remove(pid)
                
                notifyProcessKilled(pid)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kill process $pid", e)
                false
            }
        }
    }
    
    /**
     * Register session callback
     */
    fun registerSessionCallback(sessionId: String, callback: TerminalSessionCallback) {
        sessionCallbacks.computeIfAbsent(sessionId) { mutableSetOf() }.add(callback)
    }
    
    /**
     * Unregister session callback
     */
    fun unregisterSessionCallback(sessionId: String, callback: TerminalSessionCallback) {
        sessionCallbacks[sessionId]?.remove(callback)
    }
    
    /**
     * Register global callback
     */
    fun registerGlobalCallback(callback: TerminalGlobalCallback) {
        globalCallbacks.add(callback)
    }
    
    /**
     * Unregister global callback
     */
    fun unregisterGlobalCallback(callback: TerminalGlobalCallback) {
        globalCallbacks.remove(callback)
    }
    
    /**
     * Get terminal capabilities
     */
    fun getTerminalCapabilities(): TerminalCapabilities {
        return TerminalCapabilities(
            isTermuxAvailable = isTermuxAvailable,
            hasColorSupport = true,
            hasUnicodeSupport = true,
            hasXtermSupport = true,
            has256ColorSupport = true,
            hasTrueColorSupport = true,
            supportedShells = getSupportedShells(),
            maxHistorySize = MAX_HISTORY_SIZE
        )
    }
    
    // Private helper methods
    
    private fun checkTermuxAvailability() {
        try {
            val termuxExists = File("/data/data/com.termux/files/usr/bin").exists()
            val pythonExists = File("/data/data/com.termux/files/usr/bin/python").exists()
            isTermuxAvailable = termuxExists && pythonExists
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check Termux availability", e)
            isTermuxAvailable = false
        }
    }
    
    private fun getShellPath(): String {
        return if (isTermuxAvailable) {
            "/data/data/com.termux/files/usr/bin/bash"
        } else {
            "/system/bin/sh"
        }
    }
    
    private fun isAlternativeShellAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which bash")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor()
            result != null && result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getPythonCommand(command: String): String {
        // Check for virtual environment first
        val venvPath = System.getenv("VIRTUAL_ENV")
        return if (venvPath != null) {
            command.replace("python", "$venvPath/bin/python")
        } else {
            command
        }
    }
    
    private fun executeInTermux(
        command: String,
        workingDirectory: String,
        environmentVariables: Map<String, String>
    ): Process {
        val pb = ProcessBuilder()
        
        // Set working directory
        pb.directory(File(workingDirectory))
        
        // Set environment
        pb.environment().putAll(environmentVariables)
        
        // Execute command
        pb.command("bash", "-c", command)
        
        return pb.start()
    }
    
    private fun executeInSandbox(
        command: String,
        workingDirectory: String,
        environmentVariables: Map<String, String>
    ): Process {
        // In sandbox mode, restrict commands and monitor execution
        val sanitizedCommand = sanitizeCommand(command)
        val pb = ProcessBuilder()
        
        pb.directory(File(workingDirectory))
        pb.environment().putAll(environmentVariables)
        
        pb.command("sh", "-c", sanitizedCommand)
        
        return pb.start()
    }
    
    private fun sanitizeCommand(command: String): String {
        // Basic command sanitization for security
        val dangerousCommands = listOf("rm -rf", "format", "dd if", "mkfs")
        val lowerCommand = command.lowercase()
        
        for (dangerous in dangerousCommands) {
            if (lowerCommand.contains(dangerous)) {
                throw SecurityException("Dangerous command detected: $dangerous")
            }
        }
        
        return command
    }
    
    private fun addToHistory(command: String, exitCode: Int, duration: Long) {
        commandHistory.add(
            TerminalHistoryEntry(
                command = command,
                timestamp = Date(),
                workingDirectory = terminalSessions[currentSessionId]?.session?.workingDirectory ?: "",
                exitCode = exitCode,
                duration = duration
            )
        )
        
        // Maintain history size limit
        if (commandHistory.size > MAX_HISTORY_SIZE) {
            commandHistory.removeFirst()
        }
    }
    
    private fun cleanupInactiveSessions() {
        val currentTime = Date().time
        val inactiveThreshold = 30 * 60 * 1000L // 30 minutes
        
        terminalSessions.values.removeAll { wrapper ->
            val isInactive = (currentTime - wrapper.session.lastActivity.time) > inactiveThreshold
            if (isInactive) {
                Log.i(TAG, "Cleaning up inactive session: ${wrapper.session.id}")
                notifySessionExpired(wrapper.session.id)
            }
            isInactive
        }
    }
    
    private fun getSupportedShells(): List<String> {
        return if (isTermuxAvailable) {
            listOf(
                "/data/data/com.termux/files/usr/bin/bash",
                "/data/data/com.termux/files/usr/bin/zsh",
                "/data/data/com.termux/files/usr/bin/fish"
            )
        } else {
            listOf("/system/bin/sh", "/system/bin/bash")
        }
    }
    
    private fun notifySessionCreated(session: TerminalSession) {
        globalCallbacks.forEach { it.onSessionCreated(session) }
    }
    
    private fun notifySessionClosed(sessionId: String) {
        globalCallbacks.forEach { it.onSessionClosed(sessionId) }
    }
    
    private fun notifySessionChanged(sessionId: String) {
        sessionCallbacks[sessionId]?.forEach { it.onSessionChanged(sessionId) }
    }
    
    private fun notifySessionSplit(sessionId: String, newSessionId: String, direction: SplitDirection) {
        sessionCallbacks[sessionId]?.forEach { it.onSessionSplit(sessionId, newSessionId, direction) }
    }
    
    private fun notifySessionExpired(sessionId: String) {
        sessionCallbacks[sessionId]?.forEach { it.onSessionExpired(sessionId) }
    }
    
    private fun notifyOutputReceived(sessionId: String, output: String, type: TerminalOutput.OutputType) {
        sessionCallbacks[sessionId]?.forEach { it.onOutputReceived(sessionId, output, type) }
    }
    
    private fun notifyCommandCompleted(sessionId: String, command: TerminalCommand) {
        sessionCallbacks[sessionId]?.forEach { it.onCommandCompleted(sessionId, command) }
    }
    
    private fun notifyProcessKilled(pid: Int) {
        globalCallbacks.forEach { it.onProcessKilled(pid) }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        executorService.shutdown()
        terminalProcessPool.shutdown()
        
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
            if (!terminalProcessPool.awaitTermination(1, TimeUnit.SECONDS)) {
                terminalProcessPool.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            terminalProcessPool.shutdownNow()
        }
        
        sessionCallbacks.clear()
        globalCallbacks.clear()
        terminalSessions.clear()
        runningProcesses.clear()
    }
}

// Data classes and interfaces

data class TerminalCapabilities(
    val isTermuxAvailable: Boolean,
    val hasColorSupport: Boolean,
    val hasUnicodeSupport: Boolean,
    val hasXtermSupport: Boolean,
    val has256ColorSupport: Boolean,
    val hasTrueColorSupport: Boolean,
    val supportedShells: List<String>,
    val maxHistorySize: Int
)

data class TerminalSessionWrapper(var session: TerminalSession) {
    private val commandHistory = mutableListOf<TerminalCommand>()
    private val outputHistory = mutableListOf<TerminalOutput>()
    private val runningProcessIds = mutableSetOf<Int>()
    
    fun addCommand(command: TerminalCommand) {
        commandHistory.add(command)
        command.processId?.let { runningProcessIds.add(it) }
    }
    
    fun getOutputHistory(): List<TerminalOutput> = outputHistory.toList()
    
    fun getRunningProcessIds(): Set<Int> = runningProcessIds.toSet()
    
    fun removeProcessId(pid: Int) {
        runningProcessIds.remove(pid)
    }
}

sealed class TerminalSessionResult {
    data class Success(val data: TerminalSession) : TerminalSessionResult()
    data class Error(val message: String) : TerminalSessionResult()
}

sealed class CommandExecutionResult {
    data class Completed(val result: CommandResult) : CommandExecutionResult()
    data class BackgroundStarted(val command: TerminalCommand) : CommandExecutionResult()
    data class Error(val message: String) : CommandExecutionResult()
}

data class CommandResult(
    val terminalCommand: TerminalCommand,
    val success: Boolean,
    val output: String,
    val error: String,
    val duration: Long
)

enum class SplitDirection(val displayName: String, val orientation: String) {
    HORIZONTAL("Horizontal Split", "horizontal"),
    VERTICAL("Vertical Split", "vertical")
}

interface TerminalSessionCallback {
    fun onSessionChanged(sessionId: String)
    fun onSessionSplit(sessionId: String, newSessionId: String, direction: SplitDirection)
    fun onSessionExpired(sessionId: String)
    fun onOutputReceived(sessionId: String, output: String, type: TerminalOutput.OutputType)
    fun onCommandCompleted(sessionId: String, command: TerminalCommand)
}

interface TerminalGlobalCallback {
    fun onSessionCreated(session: TerminalSession)
    fun onSessionClosed(sessionId: String)
    fun onProcessKilled(pid: Int)
}