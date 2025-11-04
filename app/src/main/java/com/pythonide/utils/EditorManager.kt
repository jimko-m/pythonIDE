package com.pythonide.utils

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.pythonide.ai.*

/**
 * Manager for Monaco Editor operations with AI-powered enhancements
 * Integrates code completion, error detection, formatting, and analysis features
 */
class EditorManager(private val context: Context, private val webView: WebView) {
    
    private var currentContent = ""
    private var currentLanguage = "python"
    
    // AI Assistant Integration
    private var aiCodeAssistant: AICodeAssistant? = null
    private var isAiEnabled: Boolean = false
    private var lastErrorAnalysis: List<CodeError> = emptyList()
    private var completionSuggestions: List<String> = emptyList()
    private var codeAnalysis: CodeAnalysis? = null
    
    /**
     * Initialize AI Code Assistant
     */
    fun initializeAI(enableAiFeatures: Boolean = true) {
        isAiEnabled = enableAiFeatures
        
        if (enableAiFeatures) {
            aiCodeAssistant = AICodeAssistant(context, this)
            aiCodeAssistant?.setCallback(object : AICodeAssistant.AICallback {
                override fun onSuggestionsReady(suggestions: List<String>) {
                    completionSuggestions = suggestions
                    showSuggestions(suggestions)
                }
                
                override fun onErrorDetected(errors: List<CodeError>) {
                    lastErrorAnalysis = errors
                    displayErrors(errors)
                }
                
                override fun onCodeFormatted(success: Boolean) {
                    if (success) {
                        // Refresh content after formatting
                        getContent { content ->
                            setContent(content)
                        }
                    }
                }
                
                override fun onDocumentationGenerated(docs: String) {
                    // Handle documentation generation
                }
                
                override fun onAnalysisComplete(analysis: CodeAnalysis) {
                    codeAnalysis = analysis
                    displayCodeAnalysis(analysis)
                }
            })
            aiCodeAssistant?.initialize()
        }
    }
    
    /**
     * Enable/disable AI features
     */
    fun setAiEnabled(enabled: Boolean) {
        isAiEnabled = enabled
        if (!enabled) {
            aiCodeAssistant?.cleanup()
            aiCodeAssistant = null
        } else if (aiCodeAssistant == null) {
            initializeAI(true)
        }
    }
    
    /**
     * Get AI-powered code suggestions
     */
    fun getAISuggestions(callback: (List<String>) -> Unit) {
        if (!isAiEnabled || aiCodeAssistant == null) {
            callback(emptyList())
            return
        }
        
        getCursorPosition { line, column ->
            val suggestions = aiCodeAssistant?.getCodeSuggestions(
                currentContent,
                Pair(line, column),
                currentLanguage
            ) ?: emptyList()
            
            callback(suggestions)
        }
    }
    
    /**
     * Analyze code for errors using AI
     */
    fun analyzeCodeForErrors(callback: (List<CodeError>) -> Unit) {
        if (!isAiEnabled || aiCodeAssistant == null) {
            callback(emptyList())
            return
        }
        
        val errors = aiCodeAssistant?.analyzeCodeForErrors(currentContent, currentLanguage) ?: emptyList()
        callback(errors)
        
        // Display errors in the editor
        displayErrors(errors)
    }
    
    /**
     * Format code using AI smart formatter
     */
    fun formatCodeWithAI(style: String = "pep8") {
        if (!isAiEnabled || aiCodeAssistant == null) {
            formatCode() // Fallback to basic formatting
            return
        }
        
        val formattedCode = aiCodeAssistant?.formatCode(currentContent, style, currentLanguage)
        if (formattedCode != null && formattedCode != currentContent) {
            setContent(formattedCode)
        }
    }
    
    /**
     * Generate comprehensive code analysis
     */
    fun generateCodeAnalysis(callback: (CodeAnalysis?) -> Unit) {
        if (!isAiEnabled || aiCodeAssistant == null) {
            callback(null)
            return
        }
        
        val analysis = aiCodeAssistant?.analyzeCode(currentContent, currentLanguage)
        callback(analysis)
        
        if (analysis != null) {
            displayCodeAnalysis(analysis)
        }
    }
    
    /**
     * Generate documentation for current code
     */
    fun generateDocumentation(callback: (String) -> Unit) {
        if (!isAiEnabled || aiCodeAssistant == null) {
            callback("")
            return
        }
        
        val docs = aiCodeAssistant?.generateDocumentation(currentContent, currentLanguage) ?: ""
        callback(docs)
    }
    
    /**
     * Get intelligent code snippets
     */
    fun getCodeSnippet(template: String, context: Map<String, String>, callback: (String) -> Unit) {
        if (!isAiEnabled || aiCodeAssistant == null) {
            callback("")
            return
        }
        
        val snippet = aiCodeAssistant?.getCodeSnippet(template, context) ?: ""
        callback(snippet)
    }
    
    /**
     * Apply code refactoring
     */
    fun applyRefactoring(refactoringType: RefactoringType, callback: (String) -> Unit) {
        if (!isAiEnabled || aiCodeAssistant == null) {
            callback(currentContent)
            return
        }
        
        val refactoredCode = aiCodeAssistant?.applyRefactoring(currentContent, refactoringType, currentLanguage)
        callback(refactoredCode ?: currentContent)
        
        if (refactoredCode != null && refactoredCode != currentContent) {
            setContent(refactoredCode)
        }
    }
    
    /**
     * Set editor content
     */
    fun setContent(content: String) {
        currentContent = content
        val script = "window.monacoEditor.setValue(`$content`);"
        webView.evaluateJavascript(script, null)
        
        // Trigger real-time analysis if AI is enabled
        if (isAiEnabled && aiCodeAssistant != null) {
            // Debounced analysis to avoid excessive calls
            debounceAnalysis()
        }
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
    
    // AI-related helper methods
    
    private fun showSuggestions(suggestions: List<String>) {
        if (suggestions.isEmpty()) return
        
        // Convert suggestions to JavaScript array and show in Monaco
        val suggestionsArray = suggestions.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val script = """
            if (window.monacoEditor && window.showAISuggestions) {
                window.showAISuggestions($suggestionsArray);
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    private fun displayErrors(errors: List<CodeError>) {
        if (errors.isEmpty()) return
        
        // Clear existing error markers
        val clearScript = """
            if (window.monacoEditor) {
                window.monacoEditor.getModel().deltaDecorations([], []);
            }
        """.trimIndent()
        webView.evaluateJavascript(clearScript, null)
        
        // Add error markers for each error
        errors.forEach { error ->
            val severity = when (error.severity) {
                ErrorSeverity.CRITICAL -> "window.monaco.MarkerSeverity.Error"
                ErrorSeverity.ERROR -> "window.monaco.MarkerSeverity.Error"
                ErrorSeverity.WARNING -> "window.monaco.MarkerSeverity.Warning"
                ErrorSeverity.INFO -> "window.monaco.MarkerSeverity.Info"
            }
            
            val message = error.message.replace("\"", "\\\"")
            
            val script = """
                if (window.monacoEditor && window.monaco) {
                    const markers = [{
                        severity: $severity,
                        message: "$message",
                        startLineNumber: ${error.line},
                        startColumn: ${error.column},
                        endLineNumber: ${error.line},
                        endColumn: ${error.column + 1}
                    }];
                    window.monaco.editor.setModelMarkers(window.monacoEditor.getModel(), 'ai-error-detector', markers);
                }
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }
    
    private fun displayCodeAnalysis(analysis: CodeAnalysis) {
        // Display analysis results in a custom panel or notification
        val complexityLevel = when {
            analysis.complexity < 5 -> "Low"
            analysis.complexity < 10 -> "Medium"
            analysis.complexity < 20 -> "High"
            else -> "Very High"
        }
        
        val maintainabilityLevel = when {
            analysis.maintainabilityScore >= 80 -> "Good"
            analysis.maintainabilityScore >= 60 -> "Fair"
            analysis.maintainabilityScore >= 40 -> "Poor"
            else -> "Very Poor"
        }
        
        val script = """
            if (window.showCodeAnalysis) {
                window.showCodeAnalysis({
                    complexity: ${analysis.complexity},
                    complexityLevel: "$complexityLevel",
                    maintainabilityScore: ${analysis.maintainabilityScore},
                    maintainabilityLevel: "$maintainabilityLevel",
                    performanceSuggestions: ${analysis.performanceSuggestions.size},
                    codeSmells: ${analysis.codeSmells.size},
                    securityIssues: ${analysis.securityIssues.size},
                    documentationCoverage: ${analysis.documentationCoverage}%
                });
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    private var analysisJob: kotlinx.coroutines.Job? = null
    
    private fun debounceAnalysis() {
        analysisJob?.cancel()
        analysisJob = kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(1000) // Wait 1 second
            
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                analyzeCodeForErrors { errors ->
                    // Update UI with error count or indicators
                    updateErrorIndicators(errors.size)
                }
            }
        }
    }
    
    private fun updateErrorIndicators(errorCount: Int) {
        val script = """
            if (window.updateErrorIndicators) {
                window.updateErrorIndicators($errorCount);
            }
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    // Enhanced getContent method with callback support
    fun getContent(callback: ((String) -> Unit)? = null): String {
        if (callback == null) {
            return getContent()
        }
        
        webView.evaluateJavascript("window.monacoEditor.getValue();") { result ->
            val content = result?.trim('"')?.replace("\\n", "\n") ?: ""
            currentContent = content
            callback(content)
        }
        return currentContent
    }
    
    // Enhanced getCursorPosition with callback
    fun getCursorPosition(callback: (line: Int, column: Int) -> Unit) {
        val script = """
            const position = window.monacoEditor.getPosition();
            callback(position.lineNumber, position.column);
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            try {
                val parts = result?.split(",") ?: listOf("1", "1")
                val line = parts[0].trim().toIntOrNull() ?: 1
                val column = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                callback(line, column)
            } catch (e: Exception) {
                callback(1, 1)
            }
        }
    }
    
    /**
     * Get current AI assistant instance (for external access)
     */
    fun getAICodeAssistant(): AICodeAssistant? = aiCodeAssistant
    
    /**
     * Check if AI features are enabled
     */
    fun isAiEnabled(): Boolean = isAiEnabled
    
    /**
     * Get last error analysis results
     */
    fun getLastErrorAnalysis(): List<CodeError> = lastErrorAnalysis
    
    /**
     * Get current completion suggestions
     */
    fun getCurrentSuggestions(): List<String> = completionSuggestions
    
    /**
     * Get current code analysis
     */
    fun getCurrentCodeAnalysis(): CodeAnalysis? = codeAnalysis
    
    /**
     * Clean up AI resources
     */
    fun cleanupAI() {
        aiCodeAssistant?.cleanup()
        aiCodeAssistant = null
        isAiEnabled = false
        lastErrorAnalysis = emptyList()
        completionSuggestions = emptyList()
        codeAnalysis = null
        analysisJob?.cancel()
        analysisJob = null
    }
    
    /**
     * Complete cleanup including both editor and AI resources
     */
    fun cleanup() {
        cleanupAI()
    }
}