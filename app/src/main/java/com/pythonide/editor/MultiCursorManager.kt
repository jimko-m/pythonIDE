package com.pythonide.editor

import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.pythonide.R
import java.util.*

/**
 * Manages multi-cursor editing functionality
 * Supports adding, removing, and navigating multiple cursors simultaneously
 */
class MultiCursorManager(
    private val editText: EditText,
    private val onCursorsChanged: (() -> Unit)? = null
) {
    
    data class CursorPosition(
        val offset: Int,
        val color: Int,
        val id: String = UUID.randomUUID().toString()
    )
    
    private val cursors = mutableListOf<CursorPosition>()
    private val cursorColors = listOf(
        ContextCompat.getColor(editText.context, R.color.primary),
        ContextCompat.getColor(editText.context, R.color.secondary),
        ContextCompat.getColor(editText.context, R.color.accent),
        ContextCompat.getColor(editText.context, R.color.success),
        ContextCompat.getColor(editText.context, R.color.warning)
    )
    
    private var isSelecting = false
    private var selectionStart = 0
    private var mainCursorOffset = 0
    
    companion object {
        private const val MAX_CURSORS = 5
    }
    
    init {
        setupTextWatcher()
        setupTouchListener()
    }
    
    private fun setupTextWatcher() {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Store main cursor position before text change
                mainCursorOffset = editText.selectionStart
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Update cursor positions based on text changes
                updateCursorsOnTextChange(start, before, count)
            }
            
            override fun afterTextChanged(s: Spannable?) {
                // Redraw cursors after text change
                redrawCursors()
            }
        })
    }
    
    private fun setupTouchListener() {
        editText.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.isShiftPressed && event.isCtrlPressed) {
                        isSelecting = true
                        selectionStart = editText.selectionStart
                        true
                    } else false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isSelecting) {
                        updateSelection()
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    if (isSelecting) {
                        finalizeMultiCursor()
                        isSelecting = false
                        true
                    } else false
                }
                else -> false
            }
        }
    }
    
    /**
     * Add a new cursor at the specified position
     */
    fun addCursor(position: Int): Boolean {
        if (cursors.size >= MAX_CURSORS) return false
        
        val color = cursorColors[cursors.size % cursorColors.size]
        cursors.add(CursorPosition(position, color))
        redrawCursors()
        onCursorsChanged?.invoke()
        return true
    }
    
    /**
     * Remove a cursor by ID
     */
    fun removeCursor(id: String) {
        cursors.removeAll { it.id == id }
        redrawCursors()
        onCursorsChanged?.invoke()
    }
    
    /**
     * Remove all secondary cursors, keeping only the main cursor
     */
    fun clearSecondaryCursors() {
        cursors.clear()
        redrawCursors()
        onCursorsChanged?.invoke()
    }
    
    /**
     * Get all cursor positions
     */
    fun getAllCursors(): List<CursorPosition> = cursors.toList()
    
    /**
     * Move all cursors up by one line
     */
    fun moveAllCursorsUp() {
        cursors.forEach { cursor ->
            val newPos = findLineStart(editText.text, cursor.offset) - 1
            cursor.offset = if (newPos >= 0) newPos else 0
        }
        redrawCursors()
    }
    
    /**
     * Move all cursors down by one line
     */
    fun moveAllCursorsDown() {
        cursors.forEach { cursor ->
            val newPos = findLineEnd(editText.text, cursor.offset) + 1
            cursor.offset = if (newPos <= editText.text.length) newPos else editText.text.length
        }
        redrawCursors()
    }
    
    /**
     * Add cursors at all occurrences of the selected text
     */
    fun addCursorsForSelection(text: String) {
        val mainText = editText.text.toString()
        var searchPos = 0
        
        while (cursors.size < MAX_CURSORS) {
            val index = mainText.indexOf(text, searchPos)
            if (index == -1) break
            
            addCursor(index)
            searchPos = index + 1
        }
    }
    
    /**
     * Align all cursors to the same column
     */
    fun alignCursorsToColumn(column: Int) {
        cursors.forEach { cursor ->
            val lineStart = findLineStart(editText.text, cursor.offset)
            val newPos = minOf(lineStart + column, findLineEnd(editText.text, cursor.offset))
            cursor.offset = newPos
        }
        redrawCursors()
    }
    
    /**
     * Select text between all cursors and the main cursor
     */
    fun multiSelect(toPosition: Int) {
        editText.setSelection(editText.selectionStart, toPosition)
    }
    
    /**
     * Handle keyboard input for multi-cursor mode
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    insertNewLineAtAllCursors()
                    return true
                }
                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                    deleteAtAllCursors()
                    return true
                }
                KeyEvent.KEYCODE_D -> {
                    if (event.isCtrlPressed) {
                        duplicateLineAtAllCursors()
                        return true
                    }
                }
            }
        }
        return false
    }
    
    private fun insertNewLineAtAllCursors() {
        val text = editText.text
        val newLine = "\n"
        
        cursors.sortedByDescending { it.offset }.forEach { cursor ->
            text.insert(cursor.offset, newLine)
        }
        
        // Also insert at main cursor
        text.insert(editText.selectionStart, newLine)
    }
    
    private fun deleteAtAllCursors() {
        val text = editText.text
        
        cursors.sortedByDescending { it.offset }.forEach { cursor ->
            if (cursor.offset < text.length) {
                text.delete(cursor.offset, cursor.offset + 1)
            }
        }
    }
    
    private fun duplicateLineAtAllCursors() {
        val text = editText.text
        
        cursors.forEach { cursor ->
            val lineStart = findLineStart(text, cursor.offset)
            val lineEnd = findLineEnd(text, cursor.offset)
            val lineText = text.subSequence(lineStart, lineEnd)
            
            text.insert(lineEnd, "\n$lineText")
        }
    }
    
    private fun findLineStart(text: CharSequence, position: Int): Int {
        var start = position
        while (start > 0 && text[start - 1] != '\n') {
            start--
        }
        return start
    }
    
    private fun findLineEnd(text: CharSequence, position: Int): Int {
        var end = position
        while (end < text.length && text[end] != '\n') {
            end++
        }
        return end
    }
    
    private fun updateCursorsOnTextChange(start: Int, before: Int, count: Int) {
        val delta = count - before
        cursors.forEach { cursor ->
            if (cursor.offset > start) {
                cursor.offset += delta
            } else if (cursor.offset == start && before > 0) {
                // Handle deletion that might affect cursor position
                cursor.offset = maxOf(0, cursor.offset + delta)
            }
        }
    }
    
    private fun redrawCursors() {
        // Remove existing cursor spans
        val spans = editText.text.getSpans(0, editText.text.length, ForegroundColorSpan::class.java)
        spans.forEach { span ->
            if (span is CursorMarkerSpan) {
                editText.text.removeSpan(span)
            }
        }
        
        // Add cursor markers for each position
        cursors.forEach { cursor ->
            val span = CursorMarkerSpan(cursor.color)
            editText.text.setSpan(
                span,
                cursor.offset,
                cursor.offset,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    
    private fun updateSelection() {
        // Handle visual selection feedback
        // This would integrate with the visual selection system
    }
    
    private fun finalizeMultiCursor() {
        // Create cursors based on selection range
        val start = minOf(selectionStart, editText.selectionStart)
        val end = maxOf(selectionStart, editText.selectionStart)
        
        // Add cursors at line starts within the selection
        var pos = start
        while (pos < end && cursors.size < MAX_CURSORS) {
            val lineStart = findLineStart(editText.text, pos)
            if (lineStart >= start && lineStart < end) {
                addCursor(lineStart)
            }
            pos = findLineEnd(editText.text, pos) + 1
        }
    }
    
    /**
     * Custom span for marking cursor positions visually
     */
    private class CursorMarkerSpan(private val color: Int) : ForegroundColorSpan(color)
    
    /**
     * Check if multi-cursor mode is active
     */
    fun isMultiCursorMode(): Boolean = cursors.isNotEmpty()
    
    /**
     * Get the number of active cursors (excluding main cursor)
     */
    fun getCursorCount(): Int = cursors.size
    
    /**
     * Destroy the manager and clean up resources
     */
    fun destroy() {
        cursors.clear()
        editText.setOnTouchListener(null)
    }
}
