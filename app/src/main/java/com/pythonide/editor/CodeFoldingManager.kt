package com.pythonide.editor

import android.graphics.Rect
import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.LeadingMarginSpan
import android.view.View
import com.pythonide.R
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Manages code folding and unfolding functionality
 * Supports custom folding regions, syntax-aware folding, and visual indicators
 */
class CodeFoldingManager(
    private val editText: android.widget.EditText
) {
    
    data class FoldRegion(
        val start: Int,
        val end: Int,
        val title: String,
        val level: Int,
        val id: String = UUID.randomUUID().toString(),
        var isFolded: Boolean = false,
        var collapsedText: String = "..."
    )
    
    data class FoldMarker(
        val position: Int,
        val regionId: String,
        val isStart: Boolean,
        val isFolded: Boolean
    )
    
    companion object {
        private const val FOLD_INDICATOR = "▼"
        private const val FOLD_OUT_INDICATOR = "▶"
        private const val COLLAPSED_PREFIX = "// "
        private const val MAX_NESTED_LEVELS = 10
    }
    
    private val foldRegions = mutableMapOf<String, FoldRegion>()
    private val foldMarkers = mutableMapOf<Int, FoldMarker>()
    private val lineFoldStates = mutableMapOf<Int, Boolean>()
    
    // Python-specific folding patterns
    private val foldingPatterns = mapOf(
        "def" to Regex("^\\s*def\\s+\\w+\\("),
        "class" to Regex("^\\s*class\\s+\\w+"),
        "if" to Regex("^\\s*if\\s+.*:"),
        "elif" to Regex("^\\s*elif\\s+.*:"),
        "else" to Regex("^\\s*else\\s*:"),
        "for" to Regex("^\\s*for\\s+.*\\sin\\s+.*:"),
        "while" to Regex("^\\s*while\\s+.*:"),
        "try" to Regex("^\\s*try\\s*:"),
        "except" to Regex("^\\s*except\\s*.*:"),
        "finally" to Regex("^\\s*finally\\s*:"),
        "with" to Regex("^\\s*with\\s+.*\\s+as\\s+.*:"),
        "lambda" to Regex("^\\s*\\w+\\s*=\\s*lambda\\s+.*:"),
        "#region" to Regex("^\\s*#region\\s*.*"),
        "#end" to Regex("^\\s*#end\\s*")
    )
    
    init {
        setupFoldingDetection()
    }
    
    /**
     * Analyze text and detect foldable regions
     */
    fun analyzeAndDetectFolds() {
        val text = editText.text.toString()
        val lines = text.split("\n")
        val stack = ArrayDeque<Pair<String, Int>>() // Pair of (pattern, lineNumber)
        
        clearAllFolds()
        
        lines.forEachIndexed { lineIndex, line ->
            val lineStart = calculateLineStart(text, lineIndex)
            
            // Check for folding patterns
            foldingPatterns.forEach { (patternName, regex) ->
                if (regex.matches(line.trim())) {
                    handleFoldingPattern(patternName, line, lineIndex, lineStart, stack)
                }
            }
            
            // Check for custom folding comments
            checkCustomFolding(line, lineIndex, lineStart, stack)
        }
        
        // Close any remaining open regions
        while (stack.isNotEmpty()) {
            val (pattern, startLine) = stack.pop()
            val startPos = calculateLineStart(text, startLine)
            val endPos = calculateLineStart(text, minOf(startLine + 1, lines.size - 1))
            createFoldRegion(startPos, endPos, pattern, 1)
        }
        
        updateFoldMarkers()
        applyFolding()
    }
    
    /**
     * Fold a specific region
     */
    fun foldRegion(regionId: String): Boolean {
        val region = foldRegions[regionId] ?: return false
        if (region.isFolded) return false
        
        region.isFolded = true
        collapseRegion(region)
        updateFoldMarkers()
        return true
    }
    
    /**
     * Unfold a specific region
     */
    fun unfoldRegion(regionId: String): Boolean {
        val region = foldRegions[regionId] ?: return false
        if (!region.isFolded) return false
        
        region.isFolded = false
        expandRegion(region)
        updateFoldMarkers()
        return true
    }
    
    /**
     * Toggle folding state of a region
     */
    fun toggleRegion(regionId: String): Boolean {
        return if (foldRegions[regionId]?.isFolded == true) {
            unfoldRegion(regionId)
        } else {
            foldRegion(regionId)
        }
    }
    
    /**
     * Fold all regions at a specific level
     */
    fun foldAllAtLevel(level: Int) {
        foldRegions.values.filter { it.level == level }.forEach { region ->
            foldRegion(region.id)
        }
    }
    
    /**
     * Unfold all regions
     */
    fun unfoldAll() {
        foldRegions.values.filter { it.isFolded }.forEach { region ->
            unfoldRegion(region.id)
        }
    }
    
    /**
     * Fold all regions
     */
    fun foldAll() {
        foldRegions.values.forEach { region ->
            foldRegion(region.id)
        }
    }
    
    /**
     * Create a custom fold region
     */
    fun createCustomFold(start: Int, end: Int, title: String, level: Int = 1): String {
        val region = FoldRegion(start, end, title, level)
        foldRegions[region.id] = region
        updateFoldMarkers()
        return region.id
    }
    
    /**
     * Remove a custom fold region
     */
    fun removeCustomFold(regionId: String) {
        val region = foldRegions[regionId] ?: return
        if (!region.isFolded) {
            unfoldRegion(regionId)
        }
        foldRegions.remove(regionId)
        updateFoldMarkers()
    }
    
    /**
     * Get foldable regions at a position
     */
    fun getFoldableRegionsAt(position: Int): List<FoldRegion> {
        return foldRegions.values.filter { region ->
            position >= region.start && position <= region.end
        }.sortedByDescending { it.level }
    }
    
    /**
     * Check if position is within a folded region
     */
    fun isPositionInFoldedRegion(position: Int): Boolean {
        return foldRegions.values.any { region ->
            region.isFolded && position >= region.start && position <= region.end
        }
    }
    
    /**
     * Navigate to next fold point
     */
    fun navigateToNextFold(): Int? {
        val currentPos = editText.selectionStart
        val nextFold = foldMarkers.values
            .filter { it.position > currentPos }
            .minByOrNull { it.position }
        
        return nextFold?.position
    }
    
    /**
     * Navigate to previous fold point
     */
    fun navigateToPreviousFold(): Int? {
        val currentPos = editText.selectionStart
        val prevFold = foldMarkers.values
            .filter { it.position < currentPos }
            .maxByOrNull { it.position }
        
        return prevFold?.position
    }
    
    private fun handleFoldingPattern(
        patternName: String,
        line: String,
        lineIndex: Int,
        lineStart: Int,
        stack: ArrayDeque<Pair<String, Int>>
    ) {
        when (patternName) {
            "def", "class" -> {
                // Handle function and class definitions
                val title = extractTitle(line, patternName)
                stack.push(Pair("$patternName:$title", lineIndex))
            }
            "if", "elif", "for", "while", "try", "with" -> {
                // Handle block statements
                val title = extractTitle(line, patternName)
                stack.push(Pair("$patternName:$title", lineIndex))
            }
        }
    }
    
    private fun checkCustomFolding(
        line: String,
        lineIndex: Int,
        lineStart: Int,
        stack: ArrayDeque<Pair<String, Int>>
    ) {
        when {
            line.trim().startsWith("#region") -> {
                val title = line.trim().substringAfter("#region").trim()
                stack.push(Pair("region:$title", lineIndex))
            }
            line.trim() == "#end" && stack.isNotEmpty() -> {
                val (pattern, startLine) = stack.pop()
                if (pattern.startsWith("region:")) {
                    val startPos = calculateLineStart(editText.text.toString(), startLine)
                    val endPos = lineStart
                    val title = pattern.substringAfter("region:").trim()
                    createFoldRegion(startPos, endPos, title, 2)
                }
            }
        }
    }
    
    private fun extractTitle(line: String, pattern: String): String {
        return when (pattern) {
            "def" -> {
                Regex("def\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "Function"
            }
            "class" -> {
                Regex("class\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "Class"
            }
            else -> {
                val title = when (pattern) {
                    "if", "elif" -> Regex("$pattern\\s+(.+)").find(line)?.groupValues?.get(1)
                    "for" -> Regex("for\\s+(.+)\\s+in").find(line)?.groupValues?.get(1)
                    "while" -> Regex("while\\s+(.+)").find(line)?.groupValues?.get(1)
                    "try" -> "try"
                    "except" -> Regex("except\\s+(.*?)\\s*:").find(line)?.groupValues?.get(1)
                    "finally" -> "finally"
                    "with" -> Regex("with\\s+(.+?)\\s+as").find(line)?.groupValues?.get(1)
                    else -> pattern
                }
                title?.trim() ?: pattern.capitalize()
            }
        }
    }
    
    private fun createFoldRegion(start: Int, end: Int, title: String, level: Int) {
        if (level > MAX_NESTED_LEVELS) return
        
        val region = FoldRegion(start, end, title, level)
        foldRegions[region.id] = region
    }
    
    private fun collapseRegion(region: FoldRegion) {
        val text = editText.text
        val originalText = text.subSequence(region.start, region.end)
        val lines = originalText.split("\n")
        
        // Create collapsed text with fold indicator
        val collapsedText = "${COLLAPSED_PREFIX}$FOLD_INDICATOR ${region.title} (${lines.size} lines)"
        
        // Replace the region with collapsed text
        text.replace(region.start, region.end, collapsedText)
    }
    
    private fun expandRegion(region: FoldRegion) {
        // This would restore the original text
        // Implementation would need to store original text for each region
        // For now, we just update the markers
    }
    
    private fun setupFoldingDetection() {
        // Set up listeners to re-analyze when text changes
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Re-analyze folds when text changes significantly
                if (before > 0 || count > 0) {
                    editText.post {
                        analyzeAndDetectFolds()
                    }
                }
            }
            override fun afterTextChanged(s: Spannable?) {}
        })
    }
    
    private fun clearAllFolds() {
        foldRegions.clear()
        foldMarkers.clear()
        lineFoldStates.clear()
    }
    
    private fun calculateLineStart(text: String, lineIndex: Int): Int {
        if (lineIndex == 0) return 0
        
        val lines = text.substring(0, text.length).split("\n")
        return lines.take(lineIndex).joinToString("\n").length + lineIndex
    }
    
    private fun updateFoldMarkers() {
        foldMarkers.clear()
        
        foldRegions.values.forEach { region ->
            val marker = FoldMarker(
                position = region.start,
                regionId = region.id,
                isStart = true,
                isFolded = region.isFolded
            )
            foldMarkers[region.start] = marker
        }
        
        applyFoldMarkers()
    }
    
    private fun applyFolding() {
        foldRegions.values.filter { it.isFolded }.forEach { region ->
            collapseRegion(region)
        }
    }
    
    private fun applyFoldMarkers() {
        val text = editText.text
        
        // Remove existing fold marker spans
        val existingSpans = text.getSpans(0, text.length, FoldMarkerSpan::class.java)
        existingSpans.forEach { span ->
            text.removeSpan(span)
        }
        
        // Add new fold markers
        foldMarkers.forEach { (_, marker) ->
            val region = foldRegions[marker.regionId] ?: return@forEach
            
            val span = FoldMarkerSpan(
                marker.isFolded,
                region.title,
                region.level
            ) {
                toggleRegion(marker.regionId)
            }
            
            text.setSpan(
                span,
                marker.position,
                marker.position + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    
    /**
     * Custom clickable span for fold markers
     */
    private class FoldMarkerSpan(
        private val isFolded: Boolean,
        private val title: String,
        private val level: Int,
        private val onClick: () -> Unit
    ) : ClickableSpan() {
        
        override fun onClick(widget: View) {
            onClick()
        }
        
        override fun updateDrawState(ds: android.text.TextPaint) {
            super.updateDrawState(ds)
            ds.color = android.graphics.Color.GRAY
            ds.textSize = ds.textSize * 0.8f
        }
    }
    
    /**
     * Check if current line can start a fold region
     */
    fun canStartFoldAt(position: Int): Boolean {
        val text = editText.text.toString()
        val line = getLineAtPosition(text, position)
        
        return foldingPatterns.values.any { regex ->
            regex.matches(line.trim())
        } || line.trim().startsWith("#region")
    }
    
    private fun getLineAtPosition(text: String, position: Int): String {
        val lineStart = findLineStart(text, position)
        val lineEnd = findLineEnd(text, position)
        return text.substring(lineStart, lineEnd)
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
    
    /**
     * Get all current fold regions
     */
    fun getAllRegions(): List<FoldRegion> = foldRegions.values.toList()
    
    /**
     * Clean up resources
     */
    fun destroy() {
        clearAllFolds()
    }
}
