package com.pythonide.editor

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.pythonide.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Manages code snippets library with auto-insertion functionality
 * Supports custom snippets, template variables, and smart insertion
 */
class SnippetManager(
    private val context: Context,
    private val editText: android.widget.EditText
) {
    
    data class Snippet(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val description: String,
        val trigger: String,
        val content: String,
        val language: String = "python",
        val category: String = "General",
        val isCustom: Boolean = true,
        val variables: List<SnippetVariable> = emptyList(),
        val createdAt: Long = System.currentTimeMillis(),
        val usageCount: Int = 0
    )
    
    data class SnippetVariable(
        val name: String,
        val defaultValue: String = "",
        val description: String = "",
        val isRequired: Boolean = true
    )
    
    data class InsertionContext(
        val cursorPosition: Int,
        val lineStart: Int,
        val lineEnd: Int,
        val surroundingText: String,
        val indentation: String = "",
        val language: String = "python"
    )
    
    data class SnippetMatch(
        val snippet: Snippet,
        val matchQuality: Float, // 0.0 to 1.0
        val triggerPosition: Int
    )
    
    companion object {
        private const val PREFS_NAME = "snippet_manager_prefs"
        private const val KEY_SNIPPETS = "snippets"
        private const val KEY_CUSTOM_ORDER = "custom_order"
        private const val MAX_MATCHES = 10
        private const val MIN_TRIGGER_LENGTH = 2
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var snippets = mutableListOf<Snippet>()
    private val builtInSnippets = mutableListOf<Snippet>()
    
    // Snippet suggestions and completion
    private var currentSuggestions = emptyList<SnippetMatch>()
    private var isShowingSuggestions = false
    private var suggestionPosition = 0
    
    init {
        loadSnippets()
        setupBuiltInSnippets()
    }
    
    /**
     * Insert a snippet at the current cursor position
     */
    fun insertSnippet(snippetId: String): Boolean {
        val snippet = snippets.find { it.id == snippetId } ?: return false
        
        val context = getInsertionContext()
        val processedContent = processSnippetVariables(snippet, context)
        val indentedContent = addIndentation(processedContent, context.indentation)
        
        editText.text.insert(context.cursorPosition, indentedContent)
        
        // Update usage count
        snippet.usageCount++
        saveSnippets()
        
        return true
    }
    
    /**
     * Insert snippet with trigger expansion
     */
    fun expandTrigger(trigger: String): List<SnippetMatch> {
        val matches = findMatchingSnippets(trigger)
        return if (matches.size == 1) {
            // Single match - auto-insert
            val match = matches.first()
            insertSnippet(match.snippet.id)
            emptyList()
        } else {
            // Multiple matches - show suggestions
            currentSuggestions = matches
            isShowingSuggestions = true
            matches
        }
    }
    
    /**
     * Get snippet suggestions based on partial trigger
     */
    fun getSuggestions(partialTrigger: String): List<SnippetMatch> {
        if (partialTrigger.length < MIN_TRIGGER_LENGTH) {
            return emptyList()
        }
        
        val matches = findMatchingSnippets(partialTrigger)
        currentSuggestions = matches
        isShowingSuggestions = true
        
        return matches
    }
    
    /**
     * Select a suggestion from the list
     */
    fun selectSuggestion(index: Int): Boolean {
        if (index in currentSuggestions.indices) {
            val match = currentSuggestions[index]
            insertSnippet(match.snippet.id)
            clearSuggestions()
            return true
        }
        return false
    }
    
    /**
     * Clear current suggestions
     */
    fun clearSuggestions() {
        isShowingSuggestions = false
        currentSuggestions = emptyList()
    }
    
    /**
     * Navigate through suggestions
     */
    fun navigateSuggestions(direction: Int): Boolean {
        if (!isShowingSuggestions || currentSuggestions.isEmpty()) return false
        
        when (direction) {
            -1 -> { // Up
                suggestionPosition = if (suggestionPosition > 0) suggestionPosition - 1 
                else currentSuggestions.size - 1
            }
            1 -> { // Down
                suggestionPosition = if (suggestionPosition < currentSuggestions.size - 1) suggestionPosition + 1 
                else 0
            }
        }
        
        return true
    }
    
    /**
     * Get current suggestion position
     */
    fun getCurrentSuggestionPosition(): Int = suggestionPosition
    
    /**
     * Add a custom snippet
     */
    fun addCustomSnippet(snippet: Snippet): Boolean {
        if (snippets.any { it.name == snippet.name || it.trigger == snippet.trigger }) {
            return false // Duplicate name or trigger
        }
        
        snippets.add(snippet.copy(id = UUID.randomUUID().toString(), isCustom = true))
        saveSnippets()
        return true
    }
    
    /**
     * Update an existing snippet
     */
    fun updateSnippet(snippetId: String, updatedSnippet: Snippet): Boolean {
        val index = snippets.indexOfFirst { it.id == snippetId }
        if (index == -1) return false
        
        snippets[index] = updatedSnippet.copy(id = snippetId)
        saveSnippets()
        return true
    }
    
    /**
     * Remove a custom snippet
     */
    fun removeSnippet(snippetId: String): Boolean {
        val snippet = snippets.find { it.id == snippetId } ?: return false
        if (!snippet.isCustom) return false // Can't remove built-in snippets
        
        snippets.remove(snippet)
        saveSnippets()
        return true
    }
    
    /**
     * Get all snippets
     */
    fun getAllSnippets(): List<Snippet> = snippets.toList()
    
    /**
     * Get snippets by category
     */
    fun getSnippetsByCategory(category: String): List<Snippet> {
        return snippets.filter { it.category == category }
    }
    
    /**
     * Get snippets by language
     */
    fun getSnippetsByLanguage(language: String): List<Snippet> {
        return snippets.filter { it.language == language }
    }
    
    /**
     * Search snippets
     */
    fun searchSnippets(query: String): List<Snippet> {
        val lowerQuery = query.lowercase()
        return snippets.filter { snippet ->
            snippet.name.lowercase().contains(lowerQuery) ||
            snippet.description.lowercase().contains(lowerQuery) ||
            snippet.trigger.lowercase().contains(lowerQuery) ||
            snippet.content.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Get popular snippets (most used)
     */
    fun getPopularSnippets(limit: Int = 10): List<Snippet> {
        return snippets.sortedByDescending { it.usageCount }.take(limit)
    }
    
    /**
     * Get recent snippets
     */
    fun getRecentSnippets(limit: Int = 10): List<Snippet> {
        return snippets.sortedByDescending { it.createdAt }.take(limit)
    }
    
    /**
     * Export snippets to JSON
     */
    fun exportSnippets(): String {
        val json = JSONObject()
        val snippetsArray = JSONArray()
        
        snippets.filter { it.isCustom }.forEach { snippet ->
            snippetsArray.put(snippetToJson(snippet))
        }
        
        json.put("snippets", snippetsArray)
        return json.toString()
    }
    
    /**
     * Import snippets from JSON
     */
    fun importSnippets(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            val snippetsArray = json.getJSONArray("snippets")
            
            val importedSnippets = mutableListOf<Snippet>()
            for (i in 0 until snippetsArray.length()) {
                val snippetJson = snippetsArray.getJSONObject(i)
                val snippet = jsonToSnippet(snippetJson)
                importedSnippets.add(snippet)
            }
            
            // Avoid duplicates
            importedSnippets.forEach { snippet ->
                if (!snippets.any { it.name == snippet.name || it.trigger == snippet.trigger }) {
                    snippets.add(snippet.copy(id = UUID.randomUUID().toString()))
                }
            }
            
            saveSnippets()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create snippet from current selection
     */
    fun createSnippetFromSelection(name: String, trigger: String, description: String = ""): Boolean {
        val selectionStart = editText.selectionStart
        val selectionEnd = editText.selectionEnd
        
        if (selectionStart == selectionEnd) return false // No selection
        
        val selectedText = editText.text.subSequence(selectionStart, selectionEnd).toString()
        val context = getInsertionContext()
        
        val snippet = Snippet(
            name = name,
            description = description,
            trigger = trigger,
            content = selectedText,
            category = "Custom",
            variables = extractVariables(selectedText)
        )
        
        return addCustomSnippet(snippet)
    }
    
    /**
     * Get snippet statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "totalSnippets" to snippets.size,
            "customSnippets" to snippets.count { it.isCustom },
            "builtInSnippets" to snippets.count { !it.isCustom },
            "categories" to snippets.map { it.category }.distinct(),
            "languages" to snippets.map { it.language }.distinct(),
            "totalUsage" to snippets.sumOf { it.usageCount }
        )
    }
    
    private fun findMatchingSnippets(trigger: String): List<SnippetMatch> {
        val lowerTrigger = trigger.lowercase()
        val matches = mutableListOf<SnippetMatch>()
        
        snippets.forEach { snippet ->
            val quality = when {
                snippet.trigger.equals(lowerTrigger, ignoreCase = true) -> 1.0f
                snippet.trigger.startsWith(lowerTrigger, ignoreCase = true) -> 0.8f
                snippet.name.contains(trigger, ignoreCase = true) -> 0.6f
                snippet.trigger.contains(lowerTrigger, ignoreCase = true) -> 0.4f
                else -> 0.0f
            }
            
            if (quality > 0.0f) {
                matches.add(SnippetMatch(snippet, quality, editText.selectionStart))
            }
        }
        
        return matches.sortedByDescending { it.matchQuality }.take(MAX_MATCHES)
    }
    
    private fun getInsertionContext(): InsertionContext {
        val text = editText.text.toString()
        val cursorPos = editText.selectionStart
        
        val lineStart = findLineStart(text, cursorPos)
        val lineEnd = findLineEnd(text, cursorPos)
        
        val lineText = text.substring(lineStart, lineEnd)
        val indentation = Regex("^\\s*").find(lineText)?.value ?: ""
        val surroundingText = text.substring(
            max(0, lineStart - 50),
            min(text.length, lineEnd + 50)
        )
        
        return InsertionContext(
            cursorPosition = cursorPos,
            lineStart = lineStart,
            lineEnd = lineEnd,
            surroundingText = surroundingText,
            indentation = indentation
        )
    }
    
    private fun processSnippetVariables(snippet: Snippet, context: InsertionContext): String {
        var content = snippet.content
        
        snippet.variables.forEach { variable ->
            val replacement = getVariableValue(variable, context)
            content = content.replace("$${variable.name}", replacement)
        }
        
        // Handle common variables
        content = content.replace("\$TM_CURRENT_LINE", context.surroundingText.lines().getOrNull(context.cursorPosition - context.lineStart) ?: "")
        content = content.replace("\$TM_LINE_INDEX", (context.lineStart).toString())
        content = content.replace("\$TM_LINE_NUMBER", (context.lineStart + 1).toString())
        
        return content
    }
    
    private fun getVariableValue(variable: SnippetVariable, context: InsertionContext): String {
        // This could be expanded to show input dialogs for required variables
        // For now, return default values
        return variable.defaultValue
    }
    
    private fun addIndentation(content: String, baseIndentation: String): String {
        val lines = content.split("\n")
        val indentedLines = lines.mapIndexed { index, line ->
            if (index == 0) {
                // First line - replace existing indentation if any
                val currentIndentation = Regex("^\\s*").find(line)?.value ?: ""
                baseIndentation + line.substring(currentIndentation.length)
            } else {
                // Subsequent lines - add base indentation
                baseIndentation + line
            }
        }
        
        return indentedLines.joinToString("\n")
    }
    
    private fun extractVariables(content: String): List<SnippetVariable> {
        val variables = mutableListOf<SnippetVariable>()
        val variablePattern = Regex("\\$(\\w+)")
        
        variablePattern.findAll(content).forEach { match ->
            val varName = match.groupValues[1]
            if (!variables.any { it.name == varName }) {
                variables.add(SnippetVariable(varName))
            }
        }
        
        return variables
    }
    
    private fun findLineStart(text: String, position: Int): Int {
        var start = position
        while (start > 0 && text[start - 1] != '\n') {
            start--
        }
        return start
    }
    
    private fun findLineEnd(text: String, position: Int): Int {
        var end = position
        while (end < text.length && text[end] != '\n') {
            end++
        }
        return end
    }
    
    private fun setupBuiltInSnippets() {
        builtInSnippets.addAll(listOf(
            Snippet(
                name = "Print Statement",
                description = "Simple print statement",
                trigger = "print",
                content = "print(\${1:Hello World})\n\$0",
                category = "Output",
                isCustom = false
            ),
            Snippet(
                name = "Function Definition",
                description = "Define a function",
                trigger = "def",
                content = "def \${1:function_name}(\${2:args}):\n    \"\"\"\n    \${3:Function description}\n    \"\"\"\n    \${4:pass}\n\$0",
                category = "Functions",
                isCustom = false
            ),
            Snippet(
                name = "Class Definition",
                description = "Define a class",
                trigger = "class",
                content = "class \${1:ClassName}:\n    \"\"\"\n    \${2:Class description}\n    \"\"\"\n    \n    def __init__(self\${3:, args}):\n        \${4:pass}\n\$0",
                category = "Classes",
                isCustom = false
            ),
            Snippet(
                name = "If Statement",
                description = "If condition statement",
                trigger = "if",
                content = "if \${1:condition}:\n    \${2:pass}\n\$0",
                category = "Control Flow",
                isCustom = false
            ),
            Snippet(
                name = "For Loop",
                description = "For loop over iterable",
                trigger = "for",
                content = "for \${1:item} in \${2:iterable}:\n    \${3:pass}\n\$0",
                category = "Control Flow",
                isCustom = false
            ),
            Snippet(
                name = "Try/Except",
                description = "Try except block",
                trigger = "try",
                content = "try:\n    \${1:pass}\nexcept \${2:Exception} as \${3:e}:\n    \${4:pass}\n\$0",
                category = "Error Handling",
                isCustom = false
            ),
            Snippet(
                name = "List Comprehension",
                description = "Create list comprehension",
                trigger = "lc",
                content = "[\${1:item} for \${2:item} in \${3:iterable} if \${4:condition}]\n\$0",
                category = "Data Structures",
                isCustom = false
            ),
            Snippet(
                name = "Main Guard",
                description = "Python main guard",
                trigger = "main",
                content = "if __name__ == \"__main__\":\n    \${1:main()}\n\$0",
                category = "Entry Points",
                isCustom = false
            )
        ))
        
        snippets.addAll(builtInSnippets)
    }
    
    private fun loadSnippets() {
        val jsonString = preferences.getString(KEY_SNIPPETS, null)
        if (jsonString != null) {
            try {
                val json = JSONObject(jsonString)
                val snippetsArray = json.getJSONArray(KEY_SNIPPETS)
                
                for (i in 0 until snippetsArray.length()) {
                    val snippetJson = snippetsArray.getJSONObject(i)
                    snippets.add(jsonToSnippet(snippetJson))
                }
            } catch (e: Exception) {
                // Handle corrupted data
            }
        }
    }
    
    private fun saveSnippets() {
        val json = JSONObject()
        val snippetsArray = JSONArray()
        
        snippets.filter { it.isCustom }.forEach { snippet ->
            snippetsArray.put(snippetToJson(snippet))
        }
        
        json.put(KEY_SNIPPETS, snippetsArray)
        
        preferences.edit()
            .putString(KEY_SNIPPETS, json.toString())
            .apply()
    }
    
    private fun snippetToJson(snippet: Snippet): JSONObject {
        return JSONObject().apply {
            put("id", snippet.id)
            put("name", snippet.name)
            put("description", snippet.description)
            put("trigger", snippet.trigger)
            put("content", snippet.content)
            put("language", snippet.language)
            put("category", snippet.category)
            put("isCustom", snippet.isCustom)
            put("usageCount", snippet.usageCount)
            put("createdAt", snippet.createdAt)
            
            val variablesArray = JSONArray()
            snippet.variables.forEach { variable ->
                variablesArray.put(JSONObject().apply {
                    put("name", variable.name)
                    put("defaultValue", variable.defaultValue)
                    put("description", variable.description)
                    put("isRequired", variable.isRequired)
                })
            }
            put("variables", variablesArray)
        }
    }
    
    private fun jsonToSnippet(json: JSONObject): Snippet {
        val variables = mutableListOf<SnippetVariable>()
        val variablesArray = json.optJSONArray("variables")
        if (variablesArray != null) {
            for (i in 0 until variablesArray.length()) {
                val varJson = variablesArray.getJSONObject(i)
                variables.add(
                    SnippetVariable(
                        name = varJson.getString("name"),
                        defaultValue = varJson.optString("defaultValue", ""),
                        description = varJson.optString("description", ""),
                        isRequired = varJson.optBoolean("isRequired", true)
                    )
                )
            }
        }
        
        return Snippet(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description", ""),
            trigger = json.getString("trigger"),
            content = json.getString("content"),
            language = json.optString("language", "python"),
            category = json.optString("category", "General"),
            isCustom = json.optBoolean("isCustom", true),
            variables = variables,
            usageCount = json.optInt("usageCount", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
    
    /**
     * Check if suggestions are currently being shown
     */
    fun isShowingSuggestions(): Boolean = isShowingSuggestions
    
    /**
     * Get current suggestions
     */
    fun getCurrentSuggestions(): List<SnippetMatch> = currentSuggestions.toList()
    
    /**
     * Clean up resources
     */
    fun destroy() {
        clearSuggestions()
        saveSnippets()
    }
}
