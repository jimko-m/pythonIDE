package com.pythonide.editor

import android.content.Context
import androidx.core.content.ContextCompat
import com.pythonide.R
import java.util.regex.Pattern
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced regex search engine with symbol outline and navigation
 * Provides comprehensive search capabilities, symbol extraction, and code structure analysis
 */
class RegexSearchEngine(
    private val context: Context,
    private val editText: android.widget.EditText
) {
    
    enum class SymbolType {
        FUNCTION, CLASS, VARIABLE, IMPORT, CONSTANT,
        COMMENT, STRING, DECORATOR, KEYWORD, OPERATOR
    }
    
    data class Symbol(
        val name: String,
        val type: SymbolType,
        val start: Int,
        val end: Int,
        val lineNumber: Int,
        val columnNumber: Int,
        val signature: String = "",
        val documentation: String = "",
        val visibility: SymbolVisibility = SymbolVisibility.PUBLIC,
        val parameters: List<SymbolParameter> = emptyList(),
        val decorators: List<String> = emptyList(),
        val isAsync: Boolean = false,
        val returnType: String = ""
    )
    
    data class SymbolParameter(
        val name: String,
        val type: String = "",
        val defaultValue: String = "",
        val isVariadic: Boolean = false,
        val isKeywordOnly: Boolean = false
    )
    
    enum class SymbolVisibility {
        PUBLIC, PRIVATE, PROTECTED, SPECIAL
    }
    
    data class SymbolOutline(
        val symbols: List<Symbol>,
        val totalLines: Int,
        val currentLine: Int,
        val structure: SymbolHierarchy
    )
    
    data class SymbolHierarchy(
        val symbols: List<Symbol>,
        val children: Map<String, List<SymbolHierarchy>> = emptyMap(),
        val parent: String? = null
    )
    
    data class RegexMatch(
        val pattern: Pattern,
        val matcher: Matcher,
        val matchText: String,
        val start: Int,
        val end: Int,
        val lineNumber: Int,
        val columnNumber: Int,
        val groups: List<String>,
        val namedGroups: Map<String, String>
    )
    
    data class SearchOptions(
        val caseSensitive: Boolean = false,
        val multiline: Boolean = false,
        val dotAll: Boolean = false,
        val unicode: Boolean = false,
        val verbose: Boolean = false,
        val findAll: Boolean = true,
        val captureGroups: Boolean = true,
        val namedGroups: Boolean = true
    )
    
    // Python-specific regex patterns
    private val pythonPatterns = mapOf(
        SymbolType.FUNCTION to listOf(
            Regex("^\\s*(?:async\\s+)?def\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)\\s*:"),
            Regex("^\\s*def\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)\\s*->\\s*([^:]+):")
        ),
        SymbolType.CLASS to listOf(
            Regex("^\\s*class\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:\\([^)]*\\))?\\s*:"),
            Regex("^\\s*class\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*:")
        ),
        SymbolType.VARIABLE to listOf(
            Regex("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+?)\\s*$"),
            Regex("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:\\s*([^=]+?)\\s*$")
        ),
        SymbolType.IMPORT to listOf(
            Regex("^\\s*import\\s+([a-zA-Z_.][a-zA-Z0-9_.]*)"),
            Regex("^\\s*from\\s+([a-zA-Z_.][a-zA-Z0-9_.]*)\\s+import\\s+(.+)")
        ),
        SymbolType.CONSTANT to listOf(
            Regex("^\\s*([A-Z_][A-Z0-9_]*)\\s*=\\s*(.+)")
        ),
        SymbolType.DECORATOR to listOf(
            Regex("^\\s*@([a-zA-Z_][a-zA-Z0-9_]*(?:\\([^)]*\\))?)")
        )
    )
    
    private var cachedOutline: SymbolOutline? = null
    private var lastAnalysisTime = 0L
    private val cacheTimeout = 5000L // 5 seconds
    
    init {
        // Pre-compile common patterns for performance
        compileCommonPatterns()
    }
    
    /**
     * Extract symbol outline from the code
     */
    fun extractSymbolOutline(forceRefresh: Boolean = false): SymbolOutline {
        val currentTime = System.currentTimeMillis()
        
        if (!forceRefresh && cachedOutline != null && 
            currentTime - lastAnalysisTime < cacheTimeout) {
            return cachedOutline!!
        }
        
        val code = editText.text.toString()
        val lines = code.split("\n")
        val symbols = mutableListOf<Symbol>()
        
        // Extract symbols using regex patterns
        pythonPatterns.forEach { (type, patterns) ->
            patterns.forEach { pattern ->
                pattern.findAll(code).forEach { match ->
                    val symbol = createSymbolFromMatch(match, type, lines)
                    symbol?.let { symbols.add(it) }
                }
            }
        }
        
        // Sort symbols by position
        symbols.sortBy { it.start }
        
        // Build hierarchy
        val hierarchy = buildSymbolHierarchy(symbols)
        val outline = SymbolOutline(symbols, lines.size, getCurrentLine(), hierarchy)
        
        cachedOutline = outline
        lastAnalysisTime = currentTime
        
        return outline
    }
    
    /**
     * Find all occurrences of a regex pattern
     */
    fun findAllMatches(pattern: String, options: SearchOptions = SearchOptions()): List<RegexMatch> {
        val code = editText.text.toString()
        return findAllMatchesInText(code, pattern, options)
    }
    
    /**
     * Find all occurrences of a regex pattern in text
     */
    fun findAllMatchesInText(text: String, pattern: String, options: SearchOptions = SearchOptions()): List<RegexMatch> {
        val compiledPattern = compilePattern(pattern, options)
        val matches = mutableListOf<RegexMatch>()
        
        val matcher = compiledPattern.matcher(text)
        var matchCount = 0
        
        while (matcher.find() && matchCount < 10000) { // Prevent infinite loops
            val start = matcher.start()
            val end = matcher.end()
            val lineNumber = text.substring(0, start).count { it == '\n' } + 1
            val columnNumber = start - text.lastIndexOf('\n', start - 1)
            
            val groups = mutableListOf<String>()
            if (options.captureGroups) {
                for (i in 1..matcher.groupCount()) {
                    groups.add(matcher.group(i) ?: "")
                }
            }
            
            val namedGroups = mutableMapOf<String, String>()
            if (options.namedGroups) {
                matcher.groupNames().forEach { name ->
                    if (name.isNotEmpty()) {
                        namedGroups[name] = matcher.group(name) ?: ""
                    }
                }
            }
            
            matches.add(
                RegexMatch(
                    pattern = compiledPattern,
                    matcher = matcher,
                    matchText = matcher.group(),
                    start = start,
                    end = end,
                    lineNumber = lineNumber,
                    columnNumber = columnNumber,
                    groups = groups,
                    namedGroups = namedGroups
                )
            )
            
            matchCount++
            
            if (!options.findAll) break
        }
        
        return matches
    }
    
    /**
     * Find symbols by name
     */
    fun findSymbols(name: String, type: SymbolType? = null): List<Symbol> {
        val outline = extractSymbolOutline()
        return outline.symbols.filter { symbol ->
            (type == null || symbol.type == type) &&
            symbol.name.contains(name, ignoreCase = true)
        }
    }
    
    /**
     * Navigate to symbol
     */
    fun navigateToSymbol(symbol: Symbol): Boolean {
        editText.requestFocus()
        editText.setSelection(symbol.start, symbol.end)
        return true
    }
    
    /**
     * Get symbols at a specific position
     */
    fun getSymbolsAtPosition(position: Int): List<Symbol> {
        val outline = extractSymbolOutline()
        return outline.symbols.filter { symbol ->
            position >= symbol.start && position <= symbol.end
        }
    }
    
    /**
     * Get current function/symbol context
     */
    fun getCurrentContext(): Symbol? {
        val position = editText.selectionStart
        val symbols = getSymbolsAtPosition(position)
        
        // Return the innermost (deepest) symbol
        return symbols.maxByOrNull { symbol ->
            symbol.end - symbol.start
        }
    }
    
    /**
     * Find all references to a symbol
     */
    fun findSymbolReferences(symbolName: String): List<RegexMatch> {
        val pattern = Regex.escape(symbolName)
        val searchPattern = "\\b$pattern\\b"
        
        return findAllMatches(searchPattern, SearchOptions(
            caseSensitive = false,
            findAll = true
        )).filter { match ->
            // Filter out the symbol definition itself
            val symbols = getSymbolsAtPosition(match.start)
            symbols.none { it.name == symbolName && it.type in listOf(SymbolType.FUNCTION, SymbolType.CLASS, SymbolType.VARIABLE) }
        }
    }
    
    /**
     * Analyze code complexity
     */
    fun analyzeComplexity(): Map<String, Any> {
        val outline = extractSymbolOutline()
        val functions = outline.symbols.filter { it.type == SymbolType.FUNCTION }
        val classes = outline.symbols.filter { it.type == SymbolType.CLASS }
        
        val totalFunctions = functions.size
        val totalClasses = classes.size
        val totalLines = outline.totalLines
        
        val complexity = mutableMapOf<String, Any>(
            "totalLines" to totalLines,
            "totalFunctions" to totalFunctions,
            "totalClasses" to totalClasses,
            "functionsPerClass" to if (totalClasses > 0) totalFunctions.toFloat() / totalClasses else 0f,
            "linesPerFunction" to if (totalFunctions > 0) totalLines.toFloat() / totalFunctions else 0f,
            "averageFunctionLength" to functions.map { calculateFunctionLength(it) }.average(),
            "nestingLevel" to calculateNestingLevel()
        )
        
        return complexity
    }
    
    /**
     * Find potential issues in code
     */
    fun findCodeIssues(): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val code = editText.text.toString()
        val lines = code.split("\n")
        
        // Find TODO comments
        val todoPattern = Regex("//?\\s*TODO[:\\s].*")
        todoPattern.findAll(code).forEach { match ->
            issues.add(
                CodeIssue(
                    type = CodeIssueType.TODO,
                    message = "TODO comment found",
                    lineNumber = lines.take(match.range.first).count { it == '\n' } + 1,
                    severity = CodeIssueSeverity.INFO,
                    start = match.range.first,
                    end = match.range.last
                )
            )
        }
        
        // Find overly long lines
        lines.forEachIndexed { index, line ->
            if (line.length > 120) {
                val lineStart = code.split("\n").take(index).joinToString("\n").length + index
                issues.add(
                    CodeIssue(
                        type = CodeIssueType.LONG_LINE,
                        message = "Line too long (${line.length} > 120 characters)",
                        lineNumber = index + 1,
                        severity = CodeIssueSeverity.WARNING,
                        start = lineStart,
                        end = lineStart + line.length
                    )
                )
            }
        }
        
        // Find unused imports
        val imports = findSymbols("", SymbolType.IMPORT)
        imports.forEach { import ->
            val importName = import.name.split(".").last()
            val referenceCount = findSymbolReferences(importName).size
            if (referenceCount == 0) {
                issues.add(
                    CodeIssue(
                        type = CodeIssueType.UNUSED_IMPORT,
                        message = "Unused import: $importName",
                        lineNumber = import.lineNumber,
                        severity = CodeIssueSeverity.WARNING,
                        start = import.start,
                        end = import.end
                    )
                )
            }
        }
        
        return issues
    }
    
    /**
     * Generate code documentation
     */
    fun generateDocumentation(): String {
        val outline = extractSymbolOutline()
        val doc = StringBuilder()
        
        doc.append("# Code Documentation\n\n")
        
        outline.symbols.groupBy { it.type }.forEach { (type, symbols) ->
            doc.append("## ${type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}\n\n")
            
            symbols.forEach { symbol ->
                doc.append("### ${symbol.name}\n")
                if (symbol.signature.isNotEmpty()) {
                    doc.append("```python\n${symbol.signature}\n```\n\n")
                }
                if (symbol.documentation.isNotEmpty()) {
                    doc.append("${symbol.documentation}\n\n")
                }
                if (symbol.parameters.isNotEmpty()) {
                    doc.append("**Parameters:**\n")
                    symbol.parameters.forEach { param ->
                        val defaultText = if (param.defaultValue.isNotEmpty()) " = ${param.defaultValue}" else ""
                        doc.append("- `${param.name}`: ${param.type}$defaultText\n")
                    }
                    doc.append("\n")
                }
                if (symbol.returnType.isNotEmpty()) {
                    doc.append("**Returns:** ${symbol.returnType}\n\n")
                }
            }
        }
        
        return doc.toString()
    }
    
    private fun compilePattern(pattern: String, options: SearchOptions): Pattern {
        var flags = 0
        if (!options.caseSensitive) flags = flags or Pattern.CASE_INSENSITIVE
        if (options.multiline) flags = flags or Pattern.MULTILINE
        if (options.dotAll) flags = flags or Pattern.DOTALL
        if (options.unicode) flags = flags or Pattern.UNICODE_CASE
        if (options.verbose) flags = flags or Pattern.COMMENTS
        
        return Pattern.compile(pattern, flags)
    }
    
    private fun compileCommonPatterns() {
        // Pre-compile common patterns for better performance
        pythonPatterns.forEach { (_, patterns) ->
            patterns.forEach { pattern ->
                pattern.find("")
            }
        }
    }
    
    private fun createSymbolFromMatch(match: MatchResult, type: SymbolType, lines: List<String>): Symbol? {
        val matchText = match.value
        val start = match.range.first
        val end = match.range.last + 1
        
        val lineNumber = matchText.split("\n").first().let { lineText ->
            matchText.substring(0, matchText.indexOf(lineText)).count { it == '\n' } + 1
        }
        val columnNumber = start - matchText.substring(0, start).lastIndexOf('\n', start - 1)
        
        return when (type) {
            SymbolType.FUNCTION -> createFunctionSymbol(match, start, end, lineNumber, columnNumber)
            SymbolType.CLASS -> createClassSymbol(match, start, end, lineNumber, columnNumber)
            SymbolType.VARIABLE -> createVariableSymbol(match, start, end, lineNumber, columnNumber)
            SymbolType.IMPORT -> createImportSymbol(match, start, end, lineNumber, columnNumber)
            SymbolType.CONSTANT -> createConstantSymbol(match, start, end, lineNumber, columnNumber)
            SymbolType.DECORATOR -> createDecoratorSymbol(match, start, end, lineNumber, columnNumber)
            else -> null
        }
    }
    
    private fun createFunctionSymbol(match: MatchResult, start: Int, end: Int, lineNumber: Int, columnNumber: Int): Symbol? {
        val groups = match.destructured
        return if (groups.component2().isNotEmpty()) {
            Symbol(
                name = groups.component1(),
                type = SymbolType.FUNCTION,
                start = start,
                end = end,
                lineNumber = lineNumber,
                columnNumber = columnNumber,
                signature = match.value.trim(),
                parameters = parseParameters(groups.component2()),
                isAsync = match.value.contains("async")
            )
        } else null
    }
    
    private fun createClassSymbol(match: MatchResult, start: Int, end: Int, lineNumber: Int, columnNumber: Int): Symbol? {
        val name = match.destructured.component1()
        return Symbol(
            name = name,
            type = SymbolType.CLASS,
            start = start,
            end = end,
            lineNumber = lineNumber,
            columnNumber = columnNumber,
            signature = match.value.trim()
        )
    }
    
    private fun createVariableSymbol(match: MatchResult, start: Int, end: Int, lineNumber: Int, columnNumber: Int): Symbol? {
        val (name, value) = match.destructured
        return Symbol(
            name = name,
            type = SymbolType.VARIABLE,
            start = start,
            end = end,
            lineNumber = lineNumber,
            columnNumber = columnNumber
        )
    }
    
    private fun createImportSymbol(match: MatchResult, start: Int, end: Int, lineNumber: Int, columnNumber: Int): Symbol? {
        val name = match.destructured.component1()
        return Symbol(
            name = name,
            type = SymbolType.IMPORT,
            start = start,
            end = end,
            lineNumber = lineNumber,
            columnNumber = columnNumber
        )
    }
    
    private fun createConstantSymbol(match: MatchResult, start: Int, end: Int, lineNumber: Int, columnNumber: Int): Symbol? {
        val name = match.destructured.component1()
        return Symbol(
            name = name,
            type = SymbolType.CONSTANT,
            start = start,
            end = end,
            lineNumber = lineNumber,
            columnNumber = columnNumber,
            visibility = SymbolVisibility.PUBLIC
        )
    }
    
    private fun createDecoratorSymbol(match: MatchResult, start: Int, end: Int, lineNumber: Int, columnNumber: Int): Symbol? {
        val name = match.destructured.component1().substringAfter("@")
        return Symbol(
            name = name,
            type = SymbolType.DECORATOR,
            start = start,
            end = end,
            lineNumber = lineNumber,
            columnNumber = columnNumber
        )
    }
    
    private fun parseParameters(paramsText: String): List<SymbolParameter> {
        if (paramsText.trim().isEmpty()) return emptyList()
        
        return paramsText.split(",").map { param ->
            val trimmed = param.trim()
            if (trimmed.isEmpty()) return@map SymbolParameter("")
            
            val parts = trimmed.split("=")
            val paramName = parts[0].trim()
            val type = "" // Would need more sophisticated parsing
            val defaultValue = if (parts.size > 1) parts[1].trim() else ""
            
            SymbolParameter(
                name = paramName,
                type = type,
                defaultValue = defaultValue,
                isVariadic = paramName.startsWith("*"),
                isKeywordOnly = paramName.startsWith("*", 1)
            )
        }
    }
    
    private fun buildSymbolHierarchy(symbols: List<Symbol>): SymbolHierarchy {
        // Build a simple flat hierarchy - can be enhanced for nested structures
        return SymbolHierarchy(symbols)
    }
    
    private fun calculateFunctionLength(symbol: Symbol): Int {
        // Estimate function length based on parameter count and complexity
        return symbol.parameters.size * 2 + if (symbol.isAsync) 1 else 0
    }
    
    private fun calculateNestingLevel(): Int {
        val code = editText.text.toString()
        val lines = code.split("\n")
        var maxNesting = 0
        var currentNesting = 0
        
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.contains("def ") || trimmed.contains("class ") || trimmed.contains("if ") || 
                trimmed.contains("for ") || trimmed.contains("while ") || trimmed.contains("try:") -> {
                    currentNesting++
                    maxNesting = max(maxNesting, currentNesting)
                }
                trimmed == "" || trimmed.startsWith("#") -> {
                    // No change in nesting
                }
                else -> {
                    // Decreased nesting level
                    currentNesting = max(0, currentNesting - 1)
                }
            }
        }
        
        return maxNesting
    }
    
    private fun getCurrentLine(): Int {
        return editText.text.substring(0, editText.selectionStart).count { it == '\n' } + 1
    }
    
    // Code issue related classes
    enum class CodeIssueType {
        TODO, FIXME, BUG, WARNING, ERROR, LONG_LINE, UNUSED_IMPORT, COMPLEXITY
    }
    
    enum class CodeIssueSeverity {
        INFO, WARNING, ERROR
    }
    
    data class CodeIssue(
        val type: CodeIssueType,
        val message: String,
        val lineNumber: Int,
        val severity: CodeIssueSeverity,
        val start: Int,
        val end: Int
    )
    
    /**
     * Get cached outline (may be null if not computed)
     */
    fun getCachedOutline(): SymbolOutline? = cachedOutline
    
    /**
     * Force refresh of symbol outline
     */
    fun refreshOutline(): SymbolOutline = extractSymbolOutline(true)
    
    /**
     * Get search statistics
     */
    fun getStatistics(): Map<String, Any> {
        val outline = extractSymbolOutline()
        return mapOf(
            "totalSymbols" to outline.symbols.size,
            "functions" to outline.symbols.count { it.type == SymbolType.FUNCTION },
            "classes" to outline.symbols.count { it.type == SymbolType.CLASS },
            "variables" to outline.symbols.count { it.type == SymbolType.VARIABLE },
            "imports" to outline.symbols.count { it.type == SymbolType.IMPORT },
            "totalLines" to outline.totalLines,
            "currentLine" to outline.currentLine,
            "lastAnalysisTime" to lastAnalysisTime,
            "cacheValid" to (System.currentTimeMillis() - lastAnalysisTime < cacheTimeout)
        )
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        cachedOutline = null
    }
}
