package com.pythonide.utils

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Manager for Monaco Editor operations
 */
class EditorManager(private val context: Context, private val webView: WebView) {
    
    private var currentContent = ""
    private var currentLanguage = "python"
    
    /**
     * Set editor content
     */
    fun setContent(content: String) {
        currentContent = content
        val script = "window.monacoEditor.setValue(`$content`);"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Get editor content
     */
    fun getContent(): String {
        webView.evaluateJavascript("window.monacoEditor.getValue();") { result ->
            currentContent = result?.trim('"')?.replace("\\n", "\n") ?: ""
        }
        return currentContent
    }
    
    /**
     * Set programming language
     */
    fun setLanguage(language: String) {
        currentLanguage = language
        val script = "window.monacoEditor.setModelLanguage(window.monacoEditor.getModel(), '$language');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Find text in editor
     */
    fun find(searchText: String) {
        val script = """
            window.monacoEditor.trigger('keyboard', 'actions.find', {
                searchString: '$searchText',
                matchCase: false,
                wholeWord: false,
                regExp: false
            });
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Replace text
     */
    fun replace(searchText: String, replaceWith: String) {
        val script = """
            const model = window.monacoEditor.getModel();
            const findState = window.monacoEditor.getModel().findState;
            const range = findState?.getCurrentMatch()?.range;
            if (range) {
                model.executeEdits('', [{
                    range: range,
                    text: '$replaceWith'
                }]);
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Replace all occurrences
     */
    fun replaceAll(searchText: String, replaceWith: String) {
        val script = """
            const model = window.monacoEditor.getModel();
            const matches = model.findMatches('$searchText');
            if (matches.length > 0) {
                const edits = matches.map(match => ({
                    range: match.range,
                    text: '$replaceWith'
                }));
                model.executeEdits('', edits);
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Format code
     */
    fun formatCode() {
        val script = "window.monacoEditor.trigger('keyboard', 'editor.action.formatDocument');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Get cursor position
     */
    fun getCursorPosition(callback: (line: Int, column: Int) -> Unit) {
        val script = """
            const position = window.monacoEditor.getPosition();
            callback(position.lineNumber, position.column);
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            // Parse result and call callback
            try {
                val line = result.split(",")[0].trim().toInt()
                val column = result.split(",")[1].trim().toInt()
                callback(line, column)
            } catch (e: Exception) {
                callback(1, 1)
            }
        }
    }
    
    /**
     * Set cursor position
     */
    fun setCursorPosition(line: Int, column: Int) {
        val script = """
            window.monacoEditor.setPosition({
                lineNumber: $line,
                column: $column
            });
            window.monacoEditor.focus();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Insert text at cursor
     */
    fun insertText(text: String) {
        val script = """
            window.monacoEditor.executeEdits('', [{
                range: window.monacoEditor.getSelection(),
                text: '$text'
            }]);
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Select all text
     */
    fun selectAll() {
        val script = "window.monacoEditor.executeEdits('', [{ range: window.monacoEditor.getModel().getFullModelRange(), text: window.monacoEditor.getValue() }]);"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Copy selected text to clipboard
     */
    fun copy() {
        val script = """
            const selection = window.monacoEditor.getSelection();
            if (selection) {
                const text = window.monacoEditor.getModel().getValueInRange(selection);
                window.navigator.clipboard.writeText(text);
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Cut selected text
     */
    fun cut() {
        val script = """
            const selection = window.monacoEditor.getSelection();
            if (selection) {
                const text = window.monacoEditor.getModel().getValueInRange(selection);
                window.navigator.clipboard.writeText(text);
                window.monacoEditor.executeEdits('', [{ range: selection, text: '' }]);
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Paste from clipboard
     */
    fun paste() {
        val script = """
            window.navigator.clipboard.readText().then(text => {
                if (text) {
                    window.monacoEditor.executeEdits('', [{
                        range: window.monacoEditor.getSelection(),
                        text: text
                    }]);
                }
            });
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Set font size
     */
    fun setFontSize(size: Int) {
        val script = "window.monacoEditor.updateOptions({ fontSize: $size });"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Set theme
     */
    fun setTheme(theme: String) {
        val script = "window.monaco.editor.setTheme('$theme');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Toggle word wrap
     */
    fun toggleWordWrap() {
        val script = """
            const current = window.monacoEditor.getOption(window.monaco.editor.EditorOption.wordWrap);
            const newValue = current === 'on' ? 'off' : 'on';
            window.monacoEditor.updateOptions({ wordWrap: newValue });
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Go to line
     */
    fun goToLine(line: Int) {
        val script = """
            window.monacoEditor.revealLineInCenter($line);
            window.monacoEditor.setPosition({ lineNumber: $line, column: 1 });
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Find next
     */
    fun findNext() {
        val script = "window.monacoEditor.trigger('keyboard', 'actions.findNext');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Find previous
     */
    fun findPrevious() {
        val script = "window.monacoEditor.trigger('keyboard', 'actions.findPrevious');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Clear search highlights
     */
    fun clearSearchHighlights() {
        val script = "window.monacoEditor.getModel().disposeFindMatches();"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Fold all
     */
    fun foldAll() {
        val script = "window.monacoEditor.trigger('keyboard', 'editor.foldAll');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Unfold all
     */
    fun unfoldAll() {
        val script = "window.monacoEditor.trigger('keyboard', 'editor.unfoldAll');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Toggle minimap
     */
    fun toggleMinimap() {
        val script = """
            const current = window.monacoEditor.getOption(window.monaco.editor.EditorOption.minimap);
            window.monacoEditor.updateOptions({ minimap: { enabled: !current } });
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Zoom in
     */
    fun zoomIn() {
        val script = "window.monacoEditor.trigger('keyboard', 'editor.action.fontZoomIn');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Zoom out
     */
    fun zoomOut() {
        val script = "window.monacoEditor.trigger('keyboard', 'editor.action.fontZoomOut');"
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * Reset zoom
     */
    fun resetZoom() {
        val script = "window.monacoEditor.trigger('keyboard', 'editor.action.fontZoomReset');"
        webView.evaluateJavascript(script, null)
    }
}