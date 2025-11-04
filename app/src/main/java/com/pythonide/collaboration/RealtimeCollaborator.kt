package com.pythonide.collaboration

import android.content.Context
import android.util.Log
import com.pythonide.data.models.FileModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

class RealtimeCollaborator(private val context: Context) {
    
    companion object {
        private const val TAG = "RealtimeCollaborator"
        private const val COLLABORATION_TIMEOUT = 30000L // 30 seconds
        private const val HEARTBEAT_INTERVAL = 5000L // 5 seconds
        private const val MAX_CONCURRENT_EDITORS = 10
        private const val CONFLICT_RESOLUTION_TIMEOUT = 10000L // 10 seconds
        
        // Operation types
        const val OP_INSERT = "insert"
        const val OP_DELETE = "delete"
        const val OP_REPLACE = "replace"
        const val OP_CURSOR = "cursor"
        const val OP_SELECTION = "selection"
        
        // User roles
        const val ROLE_OWNER = "owner"
        const val ROLE_EDITOR = "editor"
        const val ROLE_VIEWER = "viewer"
        
        // Collaboration states
        const val STATE_CONNECTED = "connected"
        const val STATE_DISCONNECTED = "disconnected"
        const val STATE_RECONNECTING = "reconnecting"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore = FirebaseFirestore.getInstance()
    
    // Active collaboration sessions
    private val activeSessions = ConcurrentHashMap<String, CollaborationSession>()
    
    // Operation queue for local changes
    private val operationQueue = Channel<Operation>(Channel.UNLIMITED)
    
    // User presence tracking
    private val presenceManager = PresenceManager(firestore)
    
    // Conflict resolution
    private val conflictResolver = ConflictResolver()
    
    init {
        // Start operation processing
        scope.launch {
            processOperations()
        }
        
        // Start heartbeat
        startHeartbeat()
    }
    
    /**
     * Start a new collaboration session for a file
     */
    suspend fun startCollaboration(
        fileId: String,
        fileName: String,
        userId: String,
        userName: String,
        userRole: String = ROLE_EDITOR
    ): Result<CollaborationSession> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting collaboration session for file: $fileId")
            
            // Create collaboration session
            val session = CollaborationSession(
                fileId = fileId,
                fileName = fileName,
                ownerId = userId,
                collaborators = mutableMapOf(),
                operations = mutableListOf(),
                cursors = mutableMapOf(),
                selections = mutableMapOf()
            )
            
            // Add owner to collaborators
            session.collaborators[userId] = CollaboratorInfo(
                userId = userId,
                userName = userName,
                role = userRole,
                joinTime = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                isOnline = true
            )
            
            // Store session
            activeSessions[fileId] = session
            
            // Set up Firestore listener for real-time updates
            setupFileListener(fileId)
            
            // Broadcast user join
            broadcastUserJoin(fileId, userId, userName)
            
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start collaboration session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Join an existing collaboration session
     */
    suspend fun joinCollaboration(
        fileId: String,
        userId: String,
        userName: String,
        userRole: String = ROLE_EDITOR
    ): Result<CollaborationSession> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Joining collaboration session for file: $fileId")
            
            // Check if session exists
            val session = activeSessions[fileId] ?: loadSessionFromFirestore(fileId)
            
            if (session != null && session.collaborators.size < MAX_CONCURRENT_EDITORS) {
                // Add user to collaborators
                session.collaborators[userId] = CollaboratorInfo(
                    userId = userId,
                    userName = userName,
                    role = userRole,
                    joinTime = System.currentTimeMillis(),
                    lastSeen = System.currentTimeMillis(),
                    isOnline = true
                )
                
                // Update session in Firestore
                updateCollaboratorsInFirestore(fileId, session.collaborators)
                
                // Broadcast user join
                broadcastUserJoin(fileId, userId, userName)
                
                Result.success(session)
            } else {
                throw IllegalStateException("Session full or doesn't exist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join collaboration session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Leave collaboration session
     */
    suspend fun leaveCollaboration(fileId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[fileId] ?: return@withContext false
            
            // Remove user from collaborators
            session.collaborators.remove(userId)
            
            // Update Firestore
            updateCollaboratorsInFirestore(fileId, session.collaborators)
            
            // Broadcast user leave
            broadcastUserLeave(fileId, userId)
            
            // Clean up session if no collaborators left
            if (session.collaborators.isEmpty()) {
                activeSessions.remove(fileId)
                cleanupSession(fileId)
            }
            
            Log.d(TAG, "User $userId left collaboration session $fileId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave collaboration session", e)
            false
        }
    }
    
    /**
     * Send text operation to collaborators
     */
    suspend fun sendOperation(
        fileId: String,
        userId: String,
        operation: Operation
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending operation: ${operation.type} from user: $userId")
            
            // Add metadata to operation
            val enrichedOperation = operation.copy(
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            
            // Send to Firestore
            sendOperationToFirestore(fileId, enrichedOperation)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send operation", e)
            false
        }
    }
    
    /**
     * Update cursor position for real-time cursor sharing
     */
    suspend fun updateCursor(
        fileId: String,
        userId: String,
        line: Int,
        column: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[fileId] ?: return@withContext false
            
            // Update local cursor
            session.cursors[userId] = CursorPosition(line, column, System.currentTimeMillis())
            
            // Send cursor update to Firestore
            sendCursorUpdateToFirestore(fileId, userId, line, column)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update cursor", e)
            false
        }
    }
    
    /**
     * Update text selection for real-time selection sharing
     */
    suspend fun updateSelection(
        fileId: String,
        userId: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[fileId] ?: return@withContext false
            
            // Update local selection
            session.selections[userId] = TextSelection(
                startLine, startColumn, endLine, endColumn, System.currentTimeMillis()
            )
            
            // Send selection update to Firestore
            sendSelectionUpdateToFirestore(fileId, userId, startLine, startColumn, endLine, endColumn)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update selection", e)
            false
        }
    }
    
    /**
     * Apply remote operation to local file
     */
    suspend fun applyRemoteOperation(
        fileId: String,
        userId: String,
        operation: Operation
    ): FileModel? = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[fileId] ?: return@withContext null
            
            // Get current file content
            val currentFile = session.currentFile ?: return@withContext null
            
            // Check for conflicts
            val conflictResult = conflictResolver.checkForConflict(
                currentFile, operation, session.operations.lastOrNull()
            )
            
            when (conflictResult) {
                is ConflictResolver.ConflictResult.NoConflict -> {
                    // Apply operation
                    val updatedFile = applyOperationToFile(currentFile, operation)
                    
                    // Update session
                    session.currentFile = updatedFile
                    session.operations.add(operation)
                    
                    // Apply to local file
                    updatedFile
                }
                is ConflictResolver.ConflictResult.ConflictDetected -> {
                    // Handle conflict
                    Log.w(TAG, "Conflict detected: ${conflictResult.reason}")
                    handleConflict(fileId, currentFile, operation, conflictResult)
                    null
                }
                is ConflictResolver.ConflictResult.RequiresResolution -> {
                    // Queue for manual resolution
                    Log.i(TAG, "Operation requires manual resolution")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply remote operation", e)
            null
        }
    }
    
    /**
     * Get active collaborators for a file
     */
    fun getActiveCollaborators(fileId: String): List<CollaboratorInfo> {
        val session = activeSessions[fileId]
        return session?.collaborators?.values?.filter { it.isOnline } ?: emptyList()
    }
    
    /**
     * Get collaborator cursors
     */
    fun getCollaboratorCursors(fileId: String): Map<String, CursorPosition> {
        val session = activeSessions[fileId]
        return session?.cursors ?: emptyMap()
    }
    
    /**
     * Get collaborator selections
     */
    fun getCollaboratorSelections(fileId: String): Map<String, TextSelection> {
        val session = activeSessions[fileId]
        return session?.selections ?: emptyMap()
    }
    
    /**
     * Check if user has permission to edit
     */
    fun canEdit(fileId: String, userId: String): Boolean {
        val session = activeSessions[fileId]
        val collaborator = session?.collaborators?.get(userId)
        return collaborator?.role in listOf(ROLE_OWNER, ROLE_EDITOR)
    }
    
    private fun setupFileListener(fileId: String) {
        val session = activeSessions[fileId] ?: return
        
        // Listen to operations
        val operationsListener = firestore
            .collection("collaborations")
            .document(fileId)
            .collection("operations")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e(TAG, "Error listening to operations", it)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            val operation = change.document.toObject(Operation::class.java)
                            if (operation.userId != "local") { // Don't process our own operations
                                scope.launch {
                                    applyRemoteOperation(fileId, operation.userId, operation)
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // Handle operation modification if needed
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Handle operation removal if needed
                        }
                    }
                }
            }
        
        // Listen to cursors
        val cursorsListener = firestore
            .collection("collaborations")
            .document(fileId)
            .collection("cursors")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e(TAG, "Error listening to cursors", it)
                    return@addSnapshotListener
                }
                
                snapshot?.documents?.forEach { document ->
                    val userId = document.id
                    val cursorData = document.data
                    
                    if (cursorData != null) {
                        val line = (cursorData["line"] as? Long)?.toInt() ?: 0
                        val column = (cursorData["column"] as? Long)?.toInt() ?: 0
                        
                        val session = activeSessions[fileId]
                        session?.cursors?.put(userId, CursorPosition(line, column, System.currentTimeMillis()))
                    }
                }
            }
        
        // Store listeners for cleanup
        session.listeners = listOf(operationsListener, cursorsListener)
    }
    
    private suspend fun loadSessionFromFirestore(fileId: String): CollaborationSession? {
        return try {
            val document = firestore.collection("collaborations")
                .document(fileId)
                .get()
                .await()
            
            if (document.exists()) {
                val data = document.data
                val session = CollaborationSession(
                    fileId = fileId,
                    fileName = data?.get("fileName") as? String ?: "",
                    ownerId = data?.get("ownerId") as? String ?: "",
                    collaborators = mutableMapOf(),
                    operations = mutableListOf(),
                    cursors = mutableMapOf(),
                    selections = mutableMapOf()
                )
                
                activeSessions[fileId] = session
                session
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session from Firestore", e)
            null
        }
    }
    
    private suspend fun sendOperationToFirestore(fileId: String, operation: Operation) {
        firestore.collection("collaborations")
            .document(fileId)
            .collection("operations")
            .add(operation)
            .await()
    }
    
    private suspend fun sendCursorUpdateToFirestore(fileId: String, userId: String, line: Int, column: Int) {
        firestore.collection("collaborations")
            .document(fileId)
            .collection("cursors")
            .document(userId)
            .set(hashMapOf(
                "line" to line,
                "column" to column,
                "timestamp" to System.currentTimeMillis()
            ))
            .await()
    }
    
    private suspend fun sendSelectionUpdateToFirestore(
        fileId: String, 
        userId: String, 
        startLine: Int, 
        startColumn: Int, 
        endLine: Int, 
        endColumn: Int
    ) {
        firestore.collection("collaborations")
            .document(fileId)
            .collection("selections")
            .document(userId)
            .set(hashMapOf(
                "startLine" to startLine,
                "startColumn" to startColumn,
                "endLine" to endLine,
                "endColumn" to endColumn,
                "timestamp" to System.currentTimeMillis()
            ))
            .await()
    }
    
    private suspend fun updateCollaboratorsInFirestore(
        fileId: String, 
        collaborators: Map<String, CollaboratorInfo>
    ) {
        val collaboratorMap = collaborators.values.map { collab ->
            hashMapOf(
                "userId" to collab.userId,
                "userName" to collab.userName,
                "role" to collab.role,
                "joinTime" to collab.joinTime,
                "lastSeen" to collab.lastSeen,
                "isOnline" to collab.isOnline
            )
        }
        
        firestore.collection("collaborations")
            .document(fileId)
            .set(hashMapOf(
                "collaborators" to collaboratorMap,
                "lastUpdate" to System.currentTimeMillis()
            ))
            .await()
    }
    
    private suspend fun broadcastUserJoin(fileId: String, userId: String, userName: String) {
        // Update user presence
        presenceManager.updatePresence(userId, fileId, PresenceManager.PresenceType.JOINED)
        
        // Add to activity log
        addActivityLogEntry(fileId, ActivityLogEntry(
            type = "user_join",
            userId = userId,
            userName = userName,
            timestamp = System.currentTimeMillis()
        ))
    }
    
    private suspend fun broadcastUserLeave(fileId: String, userId: String) {
        // Update user presence
        presenceManager.updatePresence(userId, fileId, PresenceManager.PresenceType.LEFT)
        
        // Add to activity log
        addActivityLogEntry(fileId, ActivityLogEntry(
            type = "user_leave",
            userId = userId,
            timestamp = System.currentTimeMillis()
        ))
    }
    
    private suspend fun addActivityLogEntry(fileId: String, entry: ActivityLogEntry) {
        firestore.collection("collaborations")
            .document(fileId)
            .collection("activity")
            .add(entry)
            .await()
    }
    
    private fun applyOperationToFile(file: FileModel, operation: Operation): FileModel {
        val content = file.content ?: ""
        
        val updatedContent = when (operation.type) {
            OP_INSERT -> {
                val lines = content.split("\n").toMutableList()
                if (operation.line in 0..lines.size) {
                    val line = lines.getOrNull(operation.line) ?: ""
                    val before = line.substring(0, minOf(operation.column, line.length))
                    val after = line.substring(minOf(operation.column, line.length))
                    lines[operation.line] = before + operation.text + after
                    lines.joinToString("\n")
                } else {
                    content
                }
            }
            OP_DELETE -> {
                val lines = content.split("\n").toMutableList()
                if (operation.line in 0..lines.size) {
                    val line = lines.getOrNull(operation.line) ?: ""
                    if (operation.column >= 0 && operation.column < line.length) {
                        val before = line.substring(0, operation.column)
                        val after = line.substring(minOf(operation.column + operation.length, line.length))
                        lines[operation.line] = before + after
                        lines.joinToString("\n")
                    } else {
                        content
                    }
                } else {
                    content
                }
            }
            else -> content
        }
        
        return file.copy(
            content = updatedContent,
            lastModified = System.currentTimeMillis()
        )
    }
    
    private fun handleConflict(
        fileId: String, 
        currentFile: FileModel, 
        operation: Operation, 
        conflict: ConflictResolver.ConflictResult.ConflictDetected
    ) {
        // Implement conflict resolution strategy
        Log.w(TAG, "Handling conflict: ${conflict.reason}")
        
        // Could implement various strategies:
        // 1. Last write wins
        // 2. User prompt for resolution
        // 3. Automatic merge based on operation type
        
        // For now, implement last write wins
        scope.launch {
            val resolvedFile = applyOperationToFile(currentFile, operation)
            val session = activeSessions[fileId]
            session?.currentFile = resolvedFile
            session?.operations?.add(operation)
        }
    }
    
    private suspend fun processOperations() {
        for (operation in operationQueue) {
            try {
                // Process queued operations
                delay(10) // Small delay to prevent overwhelming
            } catch (e: Exception) {
                Log.e(TAG, "Error processing operation", e)
            }
        }
    }
    
    private fun startHeartbeat() {
        scope.launch {
            while (true) {
                try {
                    // Update presence for all active users
                    activeSessions.values.forEach { session ->
                        session.collaborators.values.forEach { collaborator ->
                            collaborator.lastSeen = System.currentTimeMillis()
                        }
                    }
                    
                    // Clean up stale connections
                    cleanupStaleConnections()
                    
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in heartbeat", e)
                    delay(HEARTBEAT_INTERVAL)
                }
            }
        }
    }
    
    private fun cleanupStaleConnections() {
        val currentTime = System.currentTimeMillis()
        
        activeSessions.values.forEach { session ->
            val inactiveUsers = session.collaborators.values.filter {
                currentTime - it.lastSeen > COLLABORATION_TIMEOUT
            }
            
            inactiveUsers.forEach { user ->
                session.collaborators.remove(user.userId)
                session.cursors.remove(user.userId)
                session.selections.remove(user.userId)
                
                scope.launch {
                    updateCollaboratorsInFirestore(session.fileId, session.collaborators)
                }
            }
        }
    }
    
    private fun cleanupSession(fileId: String) {
        val session = activeSessions[fileId]
        session?.listeners?.forEach { it.remove() }
        activeSessions.remove(fileId)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        activeSessions.clear()
        operationQueue.close()
    }
}

// Data classes
data class CollaborationSession(
    val fileId: String,
    val fileName: String,
    val ownerId: String,
    val collaborators: MutableMap<String, CollaboratorInfo>,
    val operations: MutableList<Operation>,
    val cursors: MutableMap<String, CursorPosition>,
    val selections: MutableMap<String, TextSelection>,
    var currentFile: FileModel? = null,
    var listeners: List<ListenerRegistration>? = null
)

data class CollaboratorInfo(
    val userId: String,
    val userName: String,
    val role: String,
    val joinTime: Long,
    var lastSeen: Long,
    var isOnline: Boolean
)

data class Operation(
    val type: String,
    val line: Int,
    val column: Int,
    val length: Int = 0,
    val text: String = "",
    val userId: String = "",
    val timestamp: Long = 0L
)

data class CursorPosition(
    val line: Int,
    val column: Int,
    val timestamp: Long
)

data class TextSelection(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val timestamp: Long
)

data class ActivityLogEntry(
    val type: String,
    val userId: String,
    val userName: String = "",
    val timestamp: Long,
    val details: Map<String, Any> = emptyMap()
)

// Helper classes
class PresenceManager(private val firestore: FirebaseFirestore) {
    
    enum class PresenceType {
        JOINED,
        ACTIVE,
        LEFT,
        IDLE
    }
    
    suspend fun updatePresence(userId: String, fileId: String, presenceType: PresenceType) {
        firestore.collection("presence")
            .document(fileId)
            .collection("users")
            .document(userId)
            .set(hashMapOf(
                "presenceType" to presenceType.name,
                "lastUpdate" to System.currentTimeMillis()
            ))
            .await()
    }
}

class ConflictResolver {
    
    sealed class ConflictResult {
        object NoConflict : ConflictResult()
        data class ConflictDetected(val reason: String) : ConflictResult()
        data class RequiresResolution(val suggestion: String) : ConflictResult()
    }
    
    fun checkForConflict(
        currentFile: FileModel,
        operation: Operation,
        lastOperation: Operation?
    ): ConflictResult {
        // Simple conflict detection - can be enhanced
        if (lastOperation != null && operation.timestamp < lastOperation.timestamp) {
            return ConflictResult.ConflictDetected("Operation is out of order")
        }
        
        return ConflictResult.NoConflict
    }
}