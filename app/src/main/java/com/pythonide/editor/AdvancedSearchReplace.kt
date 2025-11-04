package com.pythonide.editor

import android.content.Context
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.pythonide.R
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced search and replace functionality with regex support
 * Features: case sensitivity, whole word matching, regex patterns, multi-line search, replace groups
 */
class AdvancedSearchReplace(
    private val editText: android.widget.EditText,
    private val context: Context
) {
    
    data class SearchResult(
        val start: Int,
        val end: Int,
        val matchedText: String,
        val lineNumber: Int,
        val columnNumber: Int
    )
    
    data class ReplaceResult(
        val originalText: String,
        val replacementText: String,
        val groups: List<String?>
    )
    
    data class SearchOptions(
        val caseSensitive: Boolean = false,
        val wholeWord: Boolean = false,
        val useRegex: Boolean = false,
        val multiline: Boolean = false,
        val dotAll: Boolean = false
    )
    
    enum class SearchDirection {
        FORWARD, BACKWARD
    }
    
    enum class ReplaceMode {
        REPLACE_ONE, REPLACE_ALL, REPLACE_SELECTION
    }
    
    companion object {
        private const val MAX_SEARCH_RESULTS = 1000
        private const val HIGHLIGHT_ALPHA = 128
    }
    
    private var currentSearchTerm = ""
    private var currentReplaceTerm = ""
    private var searchOptions = SearchOptions()
    private var lastSearchResults = emptyList<SearchResult>()
    private var currentResultIndex = -1
    
    // Search history
    private val searchHistory = LinkedList<String>()
    private val replaceHistory = LinkedList<String>()
    private var historyIndex = -1
    
    // Highlight spans
    private var highlightSpans = mutableListOf<Pair<Int, Int>>()
    
    init {
        setupSearchKeyboardShortcuts()
    }
    
    /**
     * Perform search for the given term
     */
    fun search(term: String, options: SearchOptions = SearchOptions()): List<SearchResult> {
        if (term.isEmpty()) {
            clearHighlights()
            return emptyList()
        }
        
        currentSearchTerm = term
        searchOptions = options
        lastSearchResults = performSearch(term, options)
        currentResultIndex = if (lastSearchResults.isNotEmpty()) 0 else -1
        
        addToSearchHistory(term)
        highlightResults()
        
        return lastSearchResults
    }
    
    /**
     * Replace the current match
     */
    fun replace(replacement: String): ReplaceResult? {
        if (currentResultIndex < 0 || currentResultIndex >= lastSearchResults.size) {
            return null
        }
        
        val result = lastSearchResults[currentResultIndex]
        val originalText = result.matchedText
        
        val replacementText = if (searchOptions.useRegex) {
            processRegexGroups(originalText, replacement, result)
        } else {
            replacement
        }
        
        // Perform the replacement
        editText.text.replace(result.start, result.end, replacementText)
        
        // Update search results after replacement
        updateSearchResultsAfterReplacement(result.start, originalText.length, replacementText.length)
        
        currentReplaceTerm = replacement
        addToReplaceHistory(replacement)
        
        return ReplaceResult(originalText, replacementText, extractGroups(originalText, replacement))
    }
    
    /**
     * Replace all occurrences
     */
    fun replaceAll(replacement: String, mode: ReplaceMode = ReplaceMode.REPLACE_ALL): Int {
        if (lastSearchResults.isEmpty()) return 0
        
        var replacementCount = 0
        
        when (mode) {
            ReplaceMode.REPLACE_ALL -> {
                // Replace from end to beginning to maintain positions
                val sortedResults = lastSearchResults.sortedByDescending { it.start }
                sortedResults.forEach { result ->
                    val replacementText = if (searchOptions.useRegex) {
                        processRegexGroups(result.matchedText, replacement, result)
                    } else {
                        replacement
                    }
                    
                    editText.text.replace(result.start, result.end, replacementText)
                    replacementCount++
                }
            }
            ReplaceMode.REPLACE_ONE -> {
                replace(replacement)?.let { replacementCount++ }
            }
            ReplaceMode.REPLACE_SELECTION -> {
                val selection = editText.selectionStart until editText.selectionEnd
                lastSearchResults.filter { 
                    it.start >= selection.start && it.end <= selection.end 
                }.forEach { result ->
                    val replacementText = if (searchOptions.useRegex) {
                        processRegexGroups(result.matchedText, replacement, result)
                    } else {
                        replacement
                    }
                    
                    editText.text.replace(result.start, result.end, replacementText)
                    replacementCount++
                }
            }
        }
        
        // Re-run search to update results
        if (replacementCount > 0) {
            search(currentSearchTerm, searchOptions)
        }
        
        currentReplaceTerm = replacement
        addToReplaceHistory(replacement)
        
        return replacementCount
    }
    
    /**
     * Find next match
     */
    fun findNext(): SearchResult? {
        if (lastSearchResults.isEmpty()) return null
        
        if (currentResultIndex < lastSearchResults.size - 1) {
            currentResultIndex++
        } else {
            currentResultIndex = 0 // Wrap around
        }
        
        highlightCurrentResult()
        return lastSearchResults[currentResultIndex]
    }
    
    /**
     * Find previous match
     */
    fun findPrevious(): SearchResult? {
        if (lastSearchResults.isEmpty()) return null
        
        if (currentResultIndex > 0) {
            currentResultIndex--
        } else {
            currentResultIndex = lastSearchResults.size - 1 // Wrap around
        }
        
        highlightCurrentResult()
        return lastSearchResults[currentResultIndex]
    }
    
    /**
     * Go to specific result index
     */
    fun goToResult(index: Int): SearchResult? {
        if (index in lastSearchResults.indices) {
            currentResultIndex = index
            highlightCurrentResult()
            return lastSearchResults[index]
        }
        return null
    }
    
    /**
     * Search in selection only
     */
    fun searchInSelection(term: String, options: SearchOptions = SearchOptions()): List<SearchResult> {
        val selectionStart = editText.selectionStart
        val selectionEnd = editText.selectionEnd
        
        if (selectionStart == selectionEnd) {
            return search(term, options) // No selection, search entire document
        }
        
        val selectedText = editText.text.subSequence(selectionStart, selectionEnd)
        val results = performSearchInText(selectedText.toString(), options)
        
        // Adjust positions to account for selection offset
        return results.map { result ->
            result.copy(
                start = result.start + selectionStart,
                end = result.end + selectionStart
            )
        }
    }
    
    /**
     * Find all occurrences and return their count
     */
    fun countOccurrences(term: String, options: SearchOptions = SearchOptions()): Int {
        return performSearch(term, options).size
    }
    
    /**
     * Highlight all matches (visual indication)
     */
    fun highlightAll() {
        if (lastSearchResults.isEmpty()) return
        
        val text = editText.text
        val highlightColor = ContextCompat.getColor(context, R.color.accent)
        
        lastSearchResults.forEach { result ->
            val span = BackgroundColorSpan(highlightColor.withAlpha(HIGHLIGHT_ALPHA))
            text.setSpan(span, result.start, result.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            highlightSpans.add(Pair(result.start, result.end))
        }
    }
    
    /**
     * Clear all highlights
     */
    fun clearHighlights() {
        val text = editText.text
        highlightSpans.forEach { (start, end) ->
            val spans = text.getSpans(start, end, BackgroundColorSpan::class.java)
            spans.forEach { span ->
                text.removeSpan(span)
            }
        }
        highlightSpans.clear()
    }
    
    /**
     * Get search statistics
     */
    fun getSearchStatistics(): Map<String, Any> {
        return mapOf(
            "totalMatches" to lastSearchResults.size,
            "currentIndex" to (currentResultIndex + 1),
            "searchTerm" to currentSearchTerm,
            "replaceTerm" to currentReplaceTerm,
            "options" to searchOptions
        )
    }
    
    /**
     * Navigate to current match
     */
    fun navigateToCurrentMatch() {
        if (currentResultIndex in lastSearchResults.indices) {
            val result = lastSearchResults[currentResultIndex]
            editText.requestFocus()
            editText.setSelection(result.start, result.end)
        }
    }
    
    /**
     * Navigate to next match and select it
     */
    fun selectNextMatch() {
        findNext()?.let { result ->
            editText.requestFocus()
            editText.setSelection(result.start, result.end)
        }
    }
    
    /**
     * Select all matches
     */
    fun selectAllMatches() {
        if (lastSearchResults.isEmpty()) return
        
        val selections = lastSearchResults.map { it.start to it.end }
        if (selections.size == 1) {
            editText.setSelection(selections.first().first, selections.first().second)
        } else {
            // For multiple selections, this would integrate with MultiCursorManager
            // For now, just select the first match
            editText.setSelection(selections.first().first, selections.first().second)
        }
    }
    
    /**
     * Find matches in specific file/buffer
     */
    fun searchInText(text: String, term: String, options: SearchOptions = SearchOptions()): List<SearchResult> {
        return performSearchInText(text, options)
    }
    
    /**
     * Get search suggestions based on current cursor position
     */
    fun getSearchSuggestions(): List<String> {
        val text = editText.text.toString()
        val cursorPos = editText.selectionStart
        
        // Get word at cursor
        val wordStart = findWordStart(text, cursorPos)
        val wordEnd = findWordEnd(text, cursorPos)
        
        return if (wordStart < cursorPos && wordEnd > cursorPos) {
            val word = text.substring(wordStart, wordEnd)
            findSimilarWords(text, word)
        } else {
            emptyList()
        }
    }
    
    private fun performSearch(term: String, options: SearchOptions): List<SearchResult> {
        return performSearchInText(editText.text.toString(), options)
    }
    
    private fun performSearchInText(text: String, options: SearchOptions): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        if (options.useRegex) {
            return performRegexSearch(text, term, options)
        }
        
        val searchText = if (options.caseSensitive) text else text.lowercase()
        val searchTerm = if (options.caseSensitive) term else term.lowercase()
        
        var position = 0
        while (position < searchText.length && results.size < MAX_SEARCH_RESULTS) {
            val foundIndex = searchText.indexOf(searchTerm, position)
            
            if (foundIndex == -1) break
            
            // Check whole word condition
            if (options.wholeWord && !isWholeWord(text, foundIndex, foundIndex + searchTerm.length)) {
                position = foundIndex + 1
                continue
            }
            
            val matchedText = text.substring(foundIndex, foundIndex + searchTerm.length)
            val lineNumber = text.substring(0, foundIndex).count { it == '\n' } + 1
            val columnNumber = foundIndex - text.lastIndexOf('\n', foundIndex - 1)
            
            results.add(SearchResult(foundIndex, foundIndex + searchTerm.length, matchedText, lineNumber, columnNumber))
            position = foundIndex + 1
        }
        
        return results
    }
    
    private fun performRegexSearch(text: String, pattern: String, options: SearchOptions): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        try {
            var regex = pattern
            if (options.multiline) {
                regex = "(?m)$regex"
            }
            if (options.dotAll) {
                regex = "(?s)$regex"
            }
            
            val compiledPattern = Pattern.compile(regex, if (options.caseSensitive) 0 else Pattern.CASE_INSENSITIVE)
            val matcher = compiledPattern.matcher(text)
            
            var matchCount = 0
            while (matcher.find() && matchCount < MAX_SEARCH_RESULTS) {
                val start = matcher.start()
                val end = matcher.end()
                val matchedText = matcher.group()
                
                // Check whole word condition
                if (options.wholeWord && !isWholeWord(text, start, end)) {
                    continue
                }
                
                val lineNumber = text.substring(0, start).count { it == '\n' } + 1
                val columnNumber = start - text.lastIndexOf('\n', start - 1)
                
                results.add(SearchResult(start, end, matchedText, lineNumber, columnNumber))
                matchCount++
            }
        } catch (e: Exception) {
            // Invalid regex pattern
        }
        
        return results
    }
    
    private fun processRegexGroups(originalText: String, replacement: String, result: SearchResult): String {
        try {
            // Extract the original pattern and groups
            val pattern = currentSearchTerm
            var regex = pattern
            if (searchOptions.multiline) {
                regex = "(?m)$regex"
            }
            if (searchOptions.dotAll) {
                regex = "(?s)$regex"
            }
            
            val compiledPattern = Pattern.compile(regex, if (searchOptions.caseSensitive) 0 else Pattern.CASE_INSENSITIVE)
            val matcher = compiledPattern.matcher(originalText)
            
            if (matcher.matches()) {
                var processedReplacement = replacement
                
                // Replace $0, $1, $2, etc. with corresponding groups
                var groupIndex = 0
                while (matcher.group(groupIndex + 1) != null) {
                    val groupValue = matcher.group(groupIndex + 1) ?: ""
                    processedReplacement = processedReplacement.replace("$${groupIndex + 1}", groupValue)
                    groupIndex++
                }
                
                // Replace $0 with the entire match
                processedReplacement = processedReplacement.replace("$0", originalText)
                
                // Replace $& with the entire match
                processedReplacement = processedReplacement.replace("$&", originalText)
                
                // Replace $` with the text before the match (relative to the whole text)
                val textBefore = editText.text.subSequence(0, result.start)
                processedReplacement = processedReplacement.replace("$`", textBefore.toString())
                
                // Replace $' with the text after the match
                val textAfter = editText.text.subSequence(result.end, editText.text.length)
                processedReplacement = processedReplacement.replace("$'", textAfter.toString())
                
                return processedReplacement
            }
        } catch (e: Exception) {
            // Return original replacement if regex processing fails
        }
        
        return replacement
    }
    
    private fun extractGroups(originalText: String, replacement: String): List<String?> {
        // This would extract captured groups from regex replacement
        return listOf(originalText) // Simplified for now
    }
    
    private fun updateSearchResultsAfterReplacement(start: Int, originalLength: Int, newLength: Int) {
        val delta = newLength - originalLength
        
        lastSearchResults = lastSearchResults.map { result ->
            when {
                result.end <= start -> result // Before replacement, no change
                result.start >= start -> {
                    // After replacement, shift by delta
                    result.copy(
                        start = result.start + delta,
                        end = result.end + delta
                    )
                }
                else -> {
                    // Overlaps with replacement
                    result.copy(
                        end = result.end + delta
                    )
                }
            }
        }.filter { it.start < it.end } // Remove invalid results
    }
    
    private fun isWholeWord(text: String, start: Int, end: Int): Boolean {
        val before = if (start > 0) text[start - 1] else ' '
        val after = if (end < text.length) text[end] else ' '
        
        return !before.isLetterOrDigit() && !after.isLetterOrDigit()
    }
    
    private fun findWordStart(text: String, position: Int): Int {
        var start = position
        while (start > 0 && text[start - 1].isLetterOrDigit()) {
            start--
        }
        return start
    }
    
    private fun findWordEnd(text: String, position: Int): Int {
        var end = position
        while (end < text.length && text[end].isLetterOrDigit()) {
            end++
        }
        return end
    }
    
    private fun findSimilarWords(text: String, word: String): List<String> {
        val words = text.split(Regex("\\W+"))
            .filter { it.length > 2 && it != word }
            .distinct()
            .sortedBy { it.length }
            .take(10)
        
        return words.filter { it.contains(word, ignoreCase = true) }
    }
    
    private fun highlightResults() {
        clearHighlights()
        highlightAll()
        highlightCurrentResult()
    }
    
    private fun highlightCurrentResult() {
        if (currentResultIndex !in lastSearchResults.indices) return
        
        val result = lastSearchResults[currentResultIndex]
        editText.setSelection(result.start, result.end)
    }
    
    private fun addToSearchHistory(term: String) {
        searchHistory.remove(term)
        searchHistory.addFirst(term)
        historyIndex = -1
    }
    
    private fun addToReplaceHistory(replacement: String) {
        replaceHistory.remove(replacement)
        replaceHistory.addFirst(replacement)
    }
    
    private fun setupSearchKeyboardShortcuts() {
        // This would be called from the activity to set up keyboard shortcuts
        // Integration with the editor's key handling
    }
    
    private fun Int.withAlpha(alpha: Int): Int {
        return android.graphics.Color.argb(
            alpha,
            android.graphics.Color.red(this),
            android.graphics.Color.green(this),
            android.graphics.Color.blue(this)
        )
    }
    
    /**
     * Get current search term
     */
    fun getCurrentSearchTerm(): String = currentSearchTerm
    
    /**
     * Get current replace term
     */
    fun getCurrentReplaceTerm(): String = currentReplaceTerm
    
    /**
     * Get last search results
     */
    fun getLastSearchResults(): List<SearchResult> = lastSearchResults.toList()
    
    /**
     * Get search history
     */
    fun getSearchHistory(): List<String> = searchHistory.toList()
    
    /**
     * Get replace history
     */
    fun getReplaceHistory(): List<String> = replaceHistory.toList()
    
    /**
     * Navigate through search history
     */
    fun navigateHistory(backward: Boolean): String? {
        return if (backward) {
            if (historyIndex < searchHistory.size - 1) {
                historyIndex++
                searchHistory.getOrNull(historyIndex)
            } else null
        } else {
            if (historyIndex > -1) {
                val result = searchHistory.getOrNull(historyIndex)
                historyIndex--
                result
            } else null
        }
    }
}
