package com.pythonide.editor

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.pythonide.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.abs

/**
 * Records and plays back editor macros
 * Supports recording text edits, cursor movements, selections, and command execution
 */
class MacroRecorder(
    private val context: Context,
    private val editText: android.widget.EditText
) {
    
    enum class MacroEventType {
        TEXT_INPUT, TEXT_DELETE, TEXT_REPLACE,
        CURSOR_MOVE, SELECTION_CHANGE,
        KEY_DOWN, KEY_UP,
        MOUSE_ACTION, SCROLL,
        COMMAND_EXECUTE, UNDO, REDO
    }
    
    data class MacroEvent(
        val type: MacroEventType,
        val timestamp: Long,
        val data: Map<String, Any> = emptyMap()
    )
    
    data class Macro(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val description: String = "",
        val events: List<MacroEvent>,
        val createdAt: Long = System.currentTimeMillis(),
        val lastUsed: Long = 0,
        val usageCount: Int = 0,
        val isRecording: Boolean = false
    )
    
    data class PlaybackContext(
        val originalText: String = "",
        val originalPosition: Int = 0,
        val originalSelection: Pair<Int, Int> = 0 to 0,
        val speed: Float = 1.0f,
        val onProgress: ((Float) -> Unit)? = null,
        val onEvent: ((MacroEvent, Int) -> Unit)? = null
    )
    
    companion object {
        private const val PREFS_NAME = "macro_recorder_prefs"
        private const val KEY_MACROS = "macros"
        private const val KEY_RECORDING = "current_recording"
        private const val MAX_MACRO_ACTIONS = 10000
        private const val MIN_EVENT_INTERVAL = 10L // ms
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var isRecording = false
    private var isPlaying = false
    private var currentMacro: Macro? = null
    private var recordingEvents = mutableListOf<MacroEvent>()
    private var playbackTimer: Timer? = null
    private var playbackIndex = 0
    private var playbackStartTime = 0L
    private var pauseTime = 0L
    
    // Event compression
    private var lastEventTime = 0L
    private var lastTextState = ""
    
    init {
        loadMacros()
    }
    
    /**
     * Start recording a new macro
     */
    fun startRecording(name: String): Boolean {
        if (isRecording || isPlaying) return false
        
        currentMacro = Macro(
            name = name,
            description = "",
            events = emptyList(),
            isRecording = true
        )
        
        recordingEvents.clear()
        isRecording = true
        lastEventTime = System.currentTimeMillis()
        lastTextState = editText.text.toString()
        
        // Save initial state
        recordEvent(MacroEventType.COMMAND_EXECUTE, mapOf(
            "command" to "save_state",
            "position" to editText.selectionStart,
            "selection" to "${editText.selectionStart}-${editText.selectionEnd}"
        ))
        
        return true
    }
    
    /**
     * Stop recording and save the macro
     */
    fun stopRecording(): Macro? {
        if (!isRecording) return null
        
        isRecording = false
        
        // Save final state
        recordEvent(MacroEventType.COMMAND_EXECUTE, mapOf(
            "command" to "restore_state",
            "position" to editText.selectionStart,
            "selection" to "${editText.selectionStart}-${editText.selectionEnd}"
        ))
        
        val macro = currentMacro?.copy(
            events = recordingEvents.toList(),
            isRecording = false
        )
        
        currentMacro = null
        recordingEvents.clear()
        
        return macro
    }
    
    /**
     * Cancel recording without saving
     */
    fun cancelRecording() {
        if (!isRecording) return
        
        isRecording = false
        currentMacro = null
        recordingEvents.clear()
    }
    
    /**
     * Play back a macro
     */
    fun playMacro(macro: Macro, context: PlaybackContext = PlaybackContext()): Boolean {
        if (isRecording || isPlaying || macro.events.isEmpty()) return false
        
        // Save current state for restoration
        val originalText = editText.text.toString()
        val originalPosition = editText.selectionStart
        val originalSelection = editText.selectionStart to editText.selectionEnd
        
        isPlaying = true
        playbackIndex = 0
        playbackStartTime = System.currentTimeMillis()
        
        // Apply context settings
        editText.setText(context.originalText.ifEmpty { originalText })
        editText.setSelection(context.originalPosition)
        
        // Start playback timer
        playbackTimer = Timer()
        playbackTimer?.schedule(object : TimerTask() {
            override fun run() {
                playNextEvent(macro, context)
            }
        }, 0, (50 / context.speed).toLong()) // Adjust timing based on speed
        
        return true
    }
    
    /**
     * Stop macro playback
     */
    fun stopPlayback() {
        playbackTimer?.cancel()
        playbackTimer = null
        isPlaying = false
        playbackIndex = 0
        
        // Restore original state if needed
        // This would restore the editor to its previous state
    }
    
    /**
     * Pause macro playback
     */
    fun pausePlayback() {
        if (!isPlaying) return
        
        pauseTime = System.currentTimeMillis()
        playbackTimer?.cancel()
        playbackTimer = null
    }
    
    /**
     * Resume macro playback
     */
    fun resumePlayback(macro: Macro, context: PlaybackContext = PlaybackContext()) {
        if (!isPlaying || pauseTime == 0L) return
        
        isPlaying = true
        playbackTimer = Timer()
        playbackTimer?.schedule(object : TimerTask() {
            override fun run() {
                playNextEvent(macro, context)
            }
        }, 0, (50 / context.speed).toLong())
        
        pauseTime = 0L
    }
    
    /**
     * Record a text input event
     */
    fun recordTextInput(text: String, position: Int) {
        if (!isRecording) return
        
        recordEvent(MacroEventType.TEXT_INPUT, mapOf(
            "text" to text,
            "position" to position,
            "length" to text.length
        ))
    }
    
    /**
     * Record a text delete event
     */
    fun recordTextDelete(start: Int, end: Int) {
        if (!isRecording) return
        
        val deletedText = lastTextState.substring(start, end)
        recordEvent(MacroEventType.TEXT_DELETE, mapOf(
            "start" to start,
            "end" to end,
            "deletedText" to deletedText
        ))
        
        lastTextState = editText.text.toString()
    }
    
    /**
     * Record a cursor movement event
     */
    fun recordCursorMove(from: Int, to: Int) {
        if (!isRecording) return
        
        recordEvent(MacroEventType.CURSOR_MOVE, mapOf(
            "from" to from,
            "to" to to
        ))
    }
    
    /**
     * Record a selection change event
     */
    fun recordSelectionChange(start: Int, end: Int) {
        if (!isRecording) return
        
        recordEvent(MacroEventType.SELECTION_CHANGE, mapOf(
            "start" to start,
            "end" to end
        ))
    }
    
    /**
     * Record a key event
     */
    fun recordKeyEvent(event: KeyEvent, action: String) {
        if (!isRecording) return
        
        recordEvent(MacroEventType.KEY_DOWN, mapOf(
            "keyCode" to event.keyCode,
            "action" to action,
            "ctrl" to event.isCtrlPressed,
            "alt" to event.isAltPressed,
            "shift" to event.isShiftPressed
        ))
    }
    
    /**
     * Record a command execution
     */
    fun recordCommand(command: String, parameters: Map<String, Any> = emptyMap()) {
        if (!isRecording) return
        
        recordEvent(MacroEventType.COMMAND_EXECUTE, mapOf(
            "command" to command,
            "parameters" to parameters
        ))
    }
    
    /**
     * Save a macro
     */
    fun saveMacro(macro: Macro): Boolean {
        val existingMacros = getAllMacros().toMutableList()
        val index = existingMacros.indexOfFirst { it.id == macro.id }
        
        if (index >= 0) {
            existingMacros[index] = macro
        } else {
            existingMacros.add(macro)
        }
        
        return saveMacros(existingMacros)
    }
    
    /**
     * Delete a macro
     */
    fun deleteMacro(macroId: String): Boolean {
        val existingMacros = getAllMacros().toMutableList()
        val removed = existingMacros.removeAll { it.id == macroId }
        
        if (removed) {
            saveMacros(existingMacros)
        }
        
        return removed
    }
    
    /**
     * Get all saved macros
     */
    fun getAllMacros(): List<Macro> {
        return preferences.getString(KEY_MACROS, null)?.let { jsonString ->
            try {
                val json = JSONObject(jsonString)
                val macrosArray = json.getJSONArray(KEY_MACROS)
                
                val macros = mutableListOf<Macro>()
                for (i in 0 until macrosArray.length()) {
                    macros.add(jsonToMacro(macrosArray.getJSONObject(i)))
                }
                
                macros.sortedByDescending { it.lastUsed }
            } catch (e: Exception) {
                emptyList<Macro>()
            }
        } ?: emptyList()
    }
    
    /**
     * Get a macro by ID
     */
    fun getMacro(macroId: String): Macro? {
        return getAllMacros().find { it.id == macroId }
    }
    
    /**
     * Update macro usage statistics
     */
    fun updateMacroUsage(macroId: String) {
        val macros = getAllMacros().toMutableList()
        val index = macros.indexOfFirst { it.id == macroId }
        
        if (index >= 0) {
            val macro = macros[index]
            macros[index] = macro.copy(
                lastUsed = System.currentTimeMillis(),
                usageCount = macro.usageCount + 1
            )
            saveMacros(macros)
        }
    }
    
    /**
     * Get recent macros
     */
    fun getRecentMacros(limit: Int = 10): List<Macro> {
        return getAllMacros()
            .sortedByDescending { it.lastUsed }
            .take(limit)
    }
    
    /**
     * Get popular macros
     */
    fun getPopularMacros(limit: Int = 10): List<Macro> {
        return getAllMacros()
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    /**
     * Search macros
     */
    fun searchMacros(query: String): List<Macro> {
        val lowerQuery = query.lowercase()
        return getAllMacros().filter { macro ->
            macro.name.lowercase().contains(lowerQuery) ||
            macro.description.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Export macros to JSON
     */
    fun exportMacros(): String {
        val json = JSONObject()
        val macrosArray = JSONArray()
        
        getAllMacros().forEach { macro ->
            macrosArray.put(macroToJson(macro))
        }
        
        json.put("macros", macrosArray)
        return json.toString()
    }
    
    /**
     * Import macros from JSON
     */
    fun importMacros(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            val macrosArray = json.getJSONArray("macros")
            
            val importedMacros = mutableListOf<Macro>()
            for (i in 0 until macrosArray.length()) {
                val macroJson = macrosArray.getJSONObject(i)
                importedMacros.add(jsonToMacro(macroJson))
            }
            
            val existingMacros = getAllMacros().toMutableList()
            
            // Avoid duplicates by name
            importedMacros.forEach { imported ->
                if (!existingMacros.any { it.name == imported.name }) {
                    existingMacros.add(imported.copy(id = UUID.randomUUID().toString()))
                }
            }
            
            saveMacros(existingMacros)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get macro statistics
     */
    fun getStatistics(): Map<String, Any> {
        val macros = getAllMacros()
        return mapOf(
            "totalMacros" to macros.size,
            "isRecording" to isRecording,
            "isPlaying" to isPlaying,
            "recordingEventCount" to recordingEvents.size,
            "playbackProgress" to if (isPlaying) playbackIndex else 0,
            "totalUsage" to macros.sumOf { it.usageCount },
            "lastUsed" to macros.maxOfOrNull { it.lastUsed } ?: 0L
        )
    }
    
    private fun recordEvent(type: MacroEventType, data: Map<String, Any>) {
        val currentTime = System.currentTimeMillis()
        
        // Apply minimum event interval
        if (currentTime - lastEventTime < MIN_EVENT_INTERVAL) {
            return
        }
        
        if (recordingEvents.size >= MAX_MACRO_ACTIONS) {
            // Stop recording if too many events
            cancelRecording()
            return
        }
        
        val event = MacroEvent(type, currentTime, data)
        recordingEvents.add(event)
        lastEventTime = currentTime
        
        // Compress consecutive text inputs
        if (type == MacroEventType.TEXT_INPUT && recordingEvents.size > 1) {
            val previousEvent = recordingEvents[recordingEvents.size - 2]
            if (previousEvent.type == MacroEventType.TEXT_INPUT) {
                // Combine the text inputs
                val combinedText = previousEvent.data["text"].toString() + data["text"].toString()
                recordingEvents[recordingEvents.size - 2] = previousEvent.copy(
                    data = previousEvent.data.toMutableMap().apply {
                        put("text", combinedText)
                    }
                )
                recordingEvents.removeAt(recordingEvents.size - 1)
            }
        }
    }
    
    private fun playNextEvent(macro: Macro, context: PlaybackContext) {
        if (playbackIndex >= macro.events.size) {
            stopPlayback()
            return
        }
        
        val event = macro.events[playbackIndex]
        val progress = (playbackIndex.toFloat() / macro.events.size.toFloat()) * 100f
        
        context.onProgress?.invoke(progress)
        context.onEvent?.invoke(event, playbackIndex)
        
        executeEvent(event)
        playbackIndex++
    }
    
    private fun executeEvent(event: MacroEvent) {
        when (event.type) {
            MacroEventType.TEXT_INPUT -> {
                val text = event.data["text"] as String
                val position = event.data["position"] as Int
                editText.text.insert(position, text)
            }
            
            MacroEventType.TEXT_DELETE -> {
                val start = event.data["start"] as Int
                val end = event.data["end"] as Int
                editText.text.delete(start, end)
            }
            
            MacroEventType.CURSOR_MOVE -> {
                val to = event.data["to"] as Int
                editText.setSelection(to)
            }
            
            MacroEventType.SELECTION_CHANGE -> {
                val start = event.data["start"] as Int
                val end = event.data["end"] as Int
                editText.setSelection(start, end)
            }
            
            MacroEventType.COMMAND_EXECUTE -> {
                val command = event.data["command"] as String
                executeCommand(command, event.data["parameters"] as? Map<String, Any>)
            }
            
            else -> {
                // Other event types would be handled here
            }
        }
    }
    
    private fun executeCommand(command: String, parameters: Map<String, Any>?) {
        when (command) {
            "save_state" -> {
                // Store current state for restoration
            }
            "restore_state" -> {
                // Restore to saved state
                val position = parameters?.get("position") as? Int ?: 0
                val selection = (parameters?.get("selection") as? String)?.split("-")
                if (selection?.size == 2) {
                    editText.setSelection(selection[0].toInt(), selection[1].toInt())
                } else {
                    editText.setSelection(position)
                }
            }
        }
    }
    
    private fun loadMacros() {
        // Macros are loaded lazily through getAllMacros()
    }
    
    private fun saveMacros(macros: List<Macro>): Boolean {
        return try {
            val json = JSONObject()
            val macrosArray = JSONArray()
            
            macros.forEach { macro ->
                macrosArray.put(macroToJson(macro))
            }
            
            json.put(KEY_MACROS, macrosArray)
            
            preferences.edit()
                .putString(KEY_MACROS, json.toString())
                .apply()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun macroToJson(macro: Macro): JSONObject {
        return JSONObject().apply {
            put("id", macro.id)
            put("name", macro.name)
            put("description", macro.description)
            put("createdAt", macro.createdAt)
            put("lastUsed", macro.lastUsed)
            put("usageCount", macro.usageCount)
            put("isRecording", macro.isRecording)
            
            val eventsArray = JSONArray()
            macro.events.forEach { event ->
                eventsArray.put(eventToJson(event))
            }
            put("events", eventsArray)
        }
    }
    
    private fun jsonToMacro(json: JSONObject): Macro {
        val events = mutableListOf<MacroEvent>()
        val eventsArray = json.optJSONArray("events")
        if (eventsArray != null) {
            for (i in 0 until eventsArray.length()) {
                events.add(jsonToEvent(eventsArray.getJSONObject(i)))
            }
        }
        
        return Macro(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description", ""),
            events = events,
            createdAt = json.getLong("createdAt"),
            lastUsed = json.optLong("lastUsed", 0),
            usageCount = json.optInt("usageCount", 0),
            isRecording = json.optBoolean("isRecording", false)
        )
    }
    
    private fun eventToJson(event: MacroEvent): JSONObject {
        return JSONObject().apply {
            put("type", event.type.name)
            put("timestamp", event.timestamp)
            
            val dataObj = JSONObject()
            event.data.forEach { (key, value) ->
                when (value) {
                    is String -> dataObj.put(key, value)
                    is Int -> dataObj.put(key, value)
                    is Long -> dataObj.put(key, value)
                    is Float -> dataObj.put(key, value)
                    is Double -> dataObj.put(key, value)
                    is Boolean -> dataObj.put(key, value)
                    is Map<*, *> -> dataObj.put(key, JSONObject(value as Map<String, Any>))
                    else -> dataObj.put(key, value.toString())
                }
            }
            put("data", dataObj)
        }
    }
    
    private fun jsonToEvent(json: JSONObject): MacroEvent {
        val type = MacroEventType.valueOf(json.getString("type"))
        val timestamp = json.getLong("timestamp")
        
        val data = mutableMapOf<String, Any>()
        val dataObj = json.optJSONObject("data")
        if (dataObj != null) {
            dataObj.keys().forEach { key ->
                val value = dataObj.get(key)
                when (value) {
                    is String -> data[key] = value
                    is Int -> data[key] = value
                    is Long -> data[key] = value
                    is Float -> data[key] = value
                    is Double -> data[key] = value
                    is Boolean -> data[key] = value
                    is JSONObject -> {
                        // Handle nested objects
                        data[key] = value.toString()
                    }
                    else -> data[key] = value.toString()
                }
            }
        }
        
        return MacroEvent(type, timestamp, data)
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = isPlaying
    
    /**
     * Get current recording events
     */
    fun getCurrentRecording(): List<MacroEvent> = recordingEvents.toList()
    
    /**
     * Clean up resources
     */
    fun destroy() {
        stopPlayback()
        cancelRecording()
    }
}
