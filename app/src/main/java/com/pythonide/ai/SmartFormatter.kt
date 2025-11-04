package com.pythonide.ai

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * AI-powered smart code formatter that intelligently formats and refactors code
 * Provides style-aware formatting, refactoring, and code organization features
 */
class SmartFormatter {
    
    private val TAG = "SmartFormatter"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Language-specific formatting rules
    private val formattingRules = mapOf(
        "python" to PythonFormattingRules(),
        "javascript" to JavaScriptFormattingRules(),
        "java" to JavaFormattingRules(),
        "kotlin" to KotlinFormattingRules()
    )
    
    // Cache for formatting results
    private val formatCache = ConcurrentHashMap<String, FormattedCode>()
    
    /**
     * Initialize the smart formatter
     */
    fun initialize() {
        Log.d(TAG, "Initializing Smart Formatter")
        preloadFormattingRules()
    }
    
    /**
     * Format code using AI-powered formatting
     */
    fun formatCode(
        code: String,
        style: String = "pep8",
        language: String = "python",
        options: FormattingOptions = FormattingOptions()
    ): String {
        if (code.isBlank()) return code
        
        val cacheKey = "${language}_${style}_${code.hashCode()}"
        formatCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Returning cached formatted code")
            return cached.formattedCode
        }
        
        return try {
            val rules = formattingRules[language] ?: PythonFormattingRules()
            val formattedCode = when (style) {
                "pep8" -> formatPEP8(code, language, rules, options)
                "google" -> formatGoogleStyle(code, language, rules, options)
                "numpy" -> formatNumPyStyle(code, language, rules, options)
                "black" -> formatBlackStyle(code, language, rules, options)
                "custom" -> formatCustomStyle(code, language, rules, options)
                else -> formatPEP8(code, language, rules, options)
            }
            
            // Cache the result
            formatCache[cacheKey] = FormattedCode(formattedCode, style)
            
            Log.d(TAG, "Successfully formatted $language code using $style style")
            formattedCode
            
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting code", e)
            code // Return original code on error
        }
    }
    
    /**
     * Format code with detailed analysis
     */
    fun formatCodeWithAnalysis(
        code: String,
        style: String = "pep8",
        language: String = "python",
        options: FormattingOptions = FormattingOptions()
    ): FormattedCode {
        val formattedCode = formatCode(code, style, language, options)
        
        val analysis = CodeFormatAnalysis(
            originalLines = code.lines().size,
            formattedLines = formattedCode.lines().size,
            changesMade = countChanges(code, formattedCode),
            style = style,
            language = language,
            formattingScore = calculateFormattingScore(code, formattedCode)
        )
        
        return FormattedCode(formattedCode, style, analysis)
    }
    
    /**
     * Refactor code to improve readability
     */
    fun refactorCode(
        code: String,
        refactoringType: RefactoringType,
        language: String = "python"
    ): String {
        return when (refactoringType) {
            RefactoringType.EXTRACT_METHOD -> extractMethod(code, language)
            RefactoringType.RENAME_VARIABLE -> renameVariable(code, language)
            RefactoringType.SIMPLIFY_CONDITION -> simplifyCondition(code, language)
            RefactoringType.OPTIMIZE_IMPORTS -> optimizeImports(code, language)
            else -> code
        }
    }
    
    /**
     * Organize imports intelligently
     */
    fun organizeImports(
        code: String,
        language: String = "python"
    ): String {
        if (language != "python") return code
        
        val lines = code.lines()
        val importLines = mutableListOf<String>()
        val otherLines = mutableListOf<String>()
        var inDocstring = false
        var docstringQuotes = 0
        
        for (line in lines) {
            val trimmed = line.trim()
            
            // Handle docstrings
            if (trimmed.startsWith("\"\"\"") || trimmed.startsWith("'''")) {
                docstringQuotes++
                inDocstring = docstringQuotes % 2 == 1
                importLines.add(line)
                continue
            }
            
            if (inDocstring) {
                importLines.add(line)
                continue
            }
            
            // Separate imports from other code
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                importLines.add(line)
            } else {
                otherLines.add(line)
            }
        }
        
        // Sort and organize imports
        val sortedImports = sortImports(importLines)
        val organizedImports = organizeImportGroups(sortedImports)
        
        return (organizedImports + otherLines).joinToString("\n")
    }
    
    /**
     * Add missing docstrings
     */
    fun addMissingDocstrings(
        code: String,
        language: String = "python"
    ): String {
        if (language != "python") return code
        
        val lines = code.lines()
        val result = mutableListOf<String>()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            
            // Check for function definitions without docstrings
            if (trimmed.startsWith("def ")) {
                result.add(line)
                i++
                
                // Look ahead for existing docstring
                val hasDocstring = i < lines.size && 
                    (lines[i].trim().startsWith("\"\"\"") || lines[i].trim().startsWith("'''"))
                
                if (!hasDocstring && shouldAddDocstring(trimmed)) {
                    val functionName = extractFunctionName(trimmed)
                    result.add(generateFunctionDocstring(functionName))
                }
                continue
            }
            
            // Check for class definitions without docstrings
            if (trimmed.startsWith("class ")) {
                result.add(line)
                i++
                
                val hasDocstring = i < lines.size && 
                    (lines[i].trim().startsWith("\"\"\"") || lines[i].trim().startsWith("'''"))
                
                if (!hasDocstring && shouldAddDocstring(trimmed)) {
                    val className = extractClassName(trimmed)
                    result.add(generateClassDocstring(className))
                }
                continue
            }
            
            result.add(line)
            i++
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * Optimize variable names
     */
    fun optimizeVariableNames(
        code: String,
        language: String = "python"
    ): String {
        if (language != "python") return code
        
        val lines = code.lines()
        val result = mutableListOf<String>()
        val variableMapping = mutableMapOf<String, String>()
        
        for (line in lines) {
            val optimizedLine = optimizeLineVariableNames(line, variableMapping)
            result.add(optimizedLine)
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * Format code to be more readable
     */
    fun improveReadability(
        code: String,
        language: String = "python"
    ): String {
        var formattedCode = code
        
        // Add blank lines around functions and classes
        formattedCode = addSpacingAroundBlocks(formattedCode)
        
        // Break long lines
        formattedCode = breakLongLines(formattedCode)
        
        // Simplify complex expressions
        formattedCode = simplifyComplexExpressions(formattedCode)
        
        // Organize imports
        formattedCode = organizeImports(formattedCode, language)
        
        return formattedCode
    }
    
    // Private formatting methods
    
    private fun formatPEP8(
        code: String,
        language: String,
        rules: LanguageFormattingRules,
        options: FormattingOptions
    ): String {
        var formatted = code
        
        // Apply basic PEP8 formatting
        formatted = fixIndentation(formatted, 4)
        formatted = fixLineLength(formatted, 79)
        formatted = fixSpacing(formatted)
        formatted = fixBlankLines(formatted)
        
        // Apply language-specific rules
        formatted = rules.applyLanguageSpecificRules(formatted, options)
        
        return formatted
    }
    
    private fun formatGoogleStyle(
        code: String,
        language: String,
        rules: LanguageFormattingRules,
        options: FormattingOptions
    ): String {
        var formatted = code
        
        // Apply Google style formatting
        formatted = fixIndentation(formatted, 2)
        formatted = fixLineLength(formatted, 80)
        formatted = fixGoogleSpacing(formatted)
        
        // Apply language-specific rules
        formatted = rules.applyLanguageSpecificRules(formatted, options)
        
        return formatted
    }
    
    private fun formatNumPyStyle(
        code: String,
        language: String,
        rules: LanguageFormattingRules,
        options: FormattingOptions
    ): String {
        var formatted = code
        
        // Apply NumPy style formatting
        formatted = fixIndentation(formatted, 4)
        formatted = fixLineLength(formatted, 79)
        formatted = fixNumPySpacing(formatted)
        
        // Apply language-specific rules
        formatted = rules.applyLanguageSpecificRules(formatted, options)
        
        return formatted
    }
    
    private fun formatBlackStyle(
        code: String,
        language: String,
        rules: LanguageFormattingRules,
        options: FormattingOptions
    ): String {
        var formatted = code
        
        // Apply Black-style formatting (aggressive but consistent)
        formatted = fixIndentation(formatted, 4)
        formatted = fixLineLength(formatted, 88) // Black's default
        formatted = fixBlackSpacing(formatted)
        formatted = normalizeStringQuotes(formatted)
        
        // Apply language-specific rules
        formatted = rules.applyLanguageSpecificRules(formatted, options)
        
        return formatted
    }
    
    private fun formatCustomStyle(
        code: String,
        language: String,
        rules: LanguageFormattingRules,
        options: FormattingOptions
    ): String {
        var formatted = code
        
        // Apply custom formatting based on options
        formatted = fixIndentation(formatted, options.indentSize)
        formatted = fixLineLength(formatted, options.maxLineLength)
        formatted = applyCustomSpacing(formatted, options)
        
        // Apply language-specific rules
        formatted = rules.applyLanguageSpecificRules(formatted, options)
        
        return formatted
    }
    
    // Helper formatting methods
    
    private fun fixIndentation(code: String, indentSize: Int): String {
        val lines = code.lines()
        val result = mutableListOf<String>()
        val indentStack = mutableListOf<Int>()
        
        for (line in lines) {
            val trimmed = line.trim()
            
            when {
                trimmed.isEmpty() -> {
                    result.add("")
                }
                trimmed.endsWith(":") -> {
                    // Block start
                    val currentIndent = indentStack.getOrNull(0) ?: 0
                    result.add(" ".repeat(currentIndent) + trimmed)
                    indentStack.add(0, currentIndent + indentSize)
                }
                trimmed.startsWith("elif ") || trimmed.startsWith("else") || 
                trimmed.startsWith("except ") || trimmed.startsWith("finally") -> {
                    // Block continuation
                    if (indentStack.size > 1) {
                        indentStack.removeAt(0)
                    }
                    val currentIndent = indentStack.getOrNull(0) ?: 0
                    result.add(" ".repeat(currentIndent) + trimmed)
                }
                else -> {
                    // Regular line
                    val currentIndent = indentStack.getOrNull(0) ?: 0
                    result.add(" ".repeat(currentIndent) + trimmed)
                }
            }
        }
        
        return result.joinToString("\n")
    }
    
    private fun fixLineLength(code: String, maxLength: Int): String {
        val lines = code.lines()
        val result = mutableListOf<String>()
        
        for (line in lines) {
            if (line.length <= maxLength) {
                result.add(line)
            } else {
                // Break long lines intelligently
                result.addAll(breakLineIntelligently(line, maxLength))
            }
        }
        
        return result.joinToString("\n")
    }
    
    private fun breakLineIntelligently(line: String, maxLength: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = line
        
        while (remaining.length > maxLength) {
            // Find the best break point
            val breakPoint = findBreakPoint(remaining, maxLength)
            result.add(remaining.substring(0, breakPoint))
            remaining = "    " + remaining.substring(breakPoint).trim()
        }
        
        result.add(remaining)
        return result
    }
    
    private fun findBreakPoint(line: String, maxLength: Int): Int {
        // Try to break at operators, commas, or spaces
        val candidates = listOf(
            line.lastIndexOf(" ", maxLength),
            line.lastIndexOf(",", maxLength),
            line.lastIndexOf("+", maxLength),
            line.lastIndexOf("-", maxLength),
            line.lastIndexOf("*", maxLength),
            line.lastIndexOf("/", maxLength)
        )
        
        val validBreakPoints = candidates.filter { it > maxLength / 2 }
        
        return if (validBreakPoints.isNotEmpty()) {
            validBreakPoints.maxOrNull()!!
        } else {
            maxLength
        }
    }
    
    private fun fixSpacing(code: String): String {
        return code
            .replace(Regex("\\s+="), " = ")
            .replace(Regex("=\\s+"), " = ")
            .replace(Regex("\\s+=="), " == ")
            .replace(Regex("==\\s+"), " == ")
            .replace(Regex("\\s+!="), " != ")
            .replace(Regex("!=\\s+"), " != ")
            .replace(Regex("\\s+<="), " <= ")
            .replace(Regex("<=\\s+"), " <= ")
            .replace(Regex("\\s+>="), " >= ")
            .replace(Regex(">=\\s+"), " >= ")
            .replace(Regex("\\s+\\+\\s+"), " + ")
            .replace(Regex("\\s+-\\s+"), " - ")
            .replace(Regex("\\s+\\*\\s+"), " * ")
            .replace(Regex("\\s+/\\s+"), " / ")
            .replace(Regex("\\s+%\\s+"), " % ")
            .replace(Regex("\\(\\s+"), "(")
            .replace(Regex("\\s+\\)"), ")")
            .replace(Regex("\\[\\s+"), "[")
            .replace(Regex("\\s+\\]"), "]")
            .replace(Regex("\\{\\s+"), "{")
            .replace(Regex("\\s+\\}"), "}")
    }
    
    private fun fixGoogleSpacing(code: String): String {
        return code
            .replace(Regex("\\s+="), " = ")
            .replace(Regex("\\s+:"), ": ")
            .replace(Regex(",\\s*([^\\s])"), ", $1")
    }
    
    private fun fixNumPySpacing(code: String): String {
        return code
            .replace(Regex("\\s+="), " = ")
            .replace(Regex("\\s*:"), ": ")
            .replace(Regex("([^\\s])\\s*,"), "$1,")
    }
    
    private fun fixBlackSpacing(code: String): String {
        return code
            .replace(Regex("\\s+="), " = ")
            .replace(Regex(":\\s*"), ": ")
            .replace(Regex(",\\s*([^\\s])"), ", $1")
    }
    
    private fun applyCustomSpacing(code: String, options: FormattingOptions): String {
        var formatted = code
        
        if (options.fixSpacing) {
            formatted = fixSpacing(formatted)
        }
        
        if (options.normalizeOperators) {
            formatted = formatted
                .replace("and", " and ")
                .replace("or", " or ")
                .replace("not", " not ")
        }
        
        return formatted
    }
    
    private fun fixBlankLines(code: String): String {
        val lines = code.lines()
        val result = mutableListOf<String>()
        
        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trim()
            
            result.add(line)
            
            // Add blank lines around functions and classes
            if (trimmed.startsWith("def ") || trimmed.startsWith("class ")) {
                if (i > 0 && lines[i - 1].trim().isNotEmpty()) {
                    result.add("")
                }
                if (i < lines.size - 1 && lines[i + 1].trim().isNotEmpty()) {
                    result.add("")
                }
            }
        }
        
        return result.joinToString("\n").replace(Regex("\n{3,}"), "\n\n")
    }
    
    private fun normalizeStringQuotes(code: String): String {
        // Normalize string quotes (simplified)
        return code
            .replace(Regex("\"([^\"]*)\""), "'$1'")
            .replace(Regex("'''([^']*)'''"), "\"\"\"$1\"\"\"")
    }
    
    private fun extractMethod(code: String, language: String): String {
        // Simplified method extraction
        return code // Implementation would extract method logic
    }
    
    private fun renameVariable(code: String, language: String): String {
        // Simplified variable renaming
        return code // Implementation would rename variables intelligently
    }
    
    private fun simplifyCondition(code: String, language: String): String {
        // Simplified condition simplification
        return code // Implementation would simplify complex conditions
    }
    
    private fun optimizeImports(code: String, language: String): String {
        return organizeImports(code, language)
    }
    
    private fun sortImports(importLines: List<String>): List<String> {
        return importLines.sortedWith(compareBy<String> { line ->
            when {
                line.startsWith("from __future__") -> 0
                line.startsWith("import ") -> 1
                line.startsWith("from ") -> 2
                else -> 3
            }
        }.thenBy { line ->
            when {
                line.contains(".") -> line.substringAfter("import ").substringBefore(".").padEnd(20)
                line.contains(".") -> line.substringAfter("from ").substringBefore(".").padEnd(20)
                else -> line
            }
        })
    }
    
    private fun organizeImportGroups(imports: List<String>): List<String> {
        val result = mutableListOf<String>()
        var lastGroup = -1
        
        for (import in imports) {
            val currentGroup = when {
                import.startsWith("from __future__") -> 0
                import.startsWith("import ") -> 1
                import.startsWith("from ") -> 2
                else -> 3
            }
            
            // Add blank line between different groups
            if (currentGroup > lastGroup && result.isNotEmpty()) {
                result.add("")
            }
            
            result.add(import)
            lastGroup = currentGroup
        }
        
        return result
    }
    
    private fun shouldAddDocstring(definition: String): Boolean {
        // Add docstrings for public functions and classes
        val name = if (definition.startsWith("def ")) {
            extractFunctionName(definition)
        } else {
            extractClassName(definition)
        }
        
        return !name.startsWith("_")
    }
    
    private fun extractFunctionName(definition: String): String {
        return definition.substringAfter("def ").substringBefore("(")
    }
    
    private fun extractClassName(definition: String): String {
        return definition.substringAfter("class ").substringBefore("(")
    }
    
    private fun generateFunctionDocstring(functionName: String): String {
        return "    \"\"\"$functionName function.\n    \n    Args:\n        TODO: Add parameters\n        \n    Returns:\n        TODO: Add return description\n    \"\"\""
    }
    
    private fun generateClassDocstring(className: String): String {
        return "    \"\"\"$className class.\n    \n    TODO: Add class description\n    \"\"\""
    }
    
    private fun optimizeLineVariableNames(line: String, mapping: MutableMap<String, String>): String {
        // Simplified variable name optimization
        return line
    }
    
    private fun addSpacingAroundBlocks(code: String): String {
        val lines = code.lines()
        val result = mutableListOf<String>()
        
        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trim()
            
            result.add(line)
            
            // Add spacing around function and class definitions
            if (trimmed.startsWith("def ") || trimmed.startsWith("class ")) {
                if (i > 0 && lines[i - 1].trim().isNotEmpty()) {
                    result.add("")
                }
            }
        }
        
        return result.joinToString("\n").replace(Regex("\n{3,}"), "\n\n")
    }
    
    private fun breakLongLines(code: String): String {
        return fixLineLength(code, 79)
    }
    
    private fun simplifyComplexExpressions(code: String): String {
        // Simplified expression simplification
        return code
    }
    
    private fun countChanges(original: String, formatted: String): Int {
        val originalLines = original.lines()
        val formattedLines = formatted.lines()
        
        var changes = 0
        val maxLines = maxOf(originalLines.size, formattedLines.size)
        
        for (i in 0 until maxLines) {
            val originalLine = originalLines.getOrNull(i) ?: ""
            val formattedLine = formattedLines.getOrNull(i) ?: ""
            
            if (originalLine != formattedLine) {
                changes++
            }
        }
        
        return changes
    }
    
    private fun calculateFormattingScore(original: String, formatted: String): Int {
        // Simple scoring based on improvements made
        var score = 100
        
        // Penalize for:
        score -= original.lines().count { it.length > 79 } * 2
        score -= original.count { it == '\t' } * 1
        score -= original.lines().count { it.trim().isEmpty() && it.isNotEmpty() } * 1
        
        // Reward for:
        score += formatted.lines().count { it.trim().isEmpty() } * 1
        score += if (formatted.contains("\"\"\"") || formatted.contains("'''")) 5 else 0
        
        return score.coerceIn(0, 100)
    }
    
    private fun preloadFormattingRules() {
        Log.d(TAG, "Preloading formatting rules")
        // Pre-load formatting rules for faster processing
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        formatCache.clear()
    }
}

/**
 * Data classes for formatting
 */

data class FormattingOptions(
    val indentSize: Int = 4,
    val maxLineLength: Int = 79,
    val fixSpacing: Boolean = true,
    val normalizeOperators: Boolean = true,
    val addDocstrings: Boolean = true,
    val organizeImports: Boolean = true
)

data class FormattedCode(
    val formattedCode: String,
    val style: String,
    val analysis: CodeFormatAnalysis? = null
)

data class CodeFormatAnalysis(
    val originalLines: Int,
    val formattedLines: Int,
    val changesMade: Int,
    val style: String,
    val language: String,
    val formattingScore: Int
)

/**
 * Language-specific formatting rule interfaces and implementations
 */

interface LanguageFormattingRules {
    fun applyLanguageSpecificRules(code: String, options: FormattingOptions): String
}

class PythonFormattingRules : LanguageFormattingRules {
    override fun applyLanguageSpecificRules(code: String, options: FormattingOptions): String {
        var formatted = code
        
        // Python-specific formatting rules
        formatted = fixPythonSpacing(formatted)
        formatted = fixPythonDocstrings(formatted)
        formatted = normalizePythonOperators(formatted)
        
        return formatted
    }
    
    private fun fixPythonSpacing(code: String): String {
        return code
            .replace(Regex("self\\s+,"), "self,")
            .replace(Regex(",\\s+self"), ", self")
            .replace(Regex(":\\s*\\n\\s*def"), ":\n    def")
    }
    
    private fun fixPythonDocstrings(code: String): String {
        return code
            .replace(Regex("\"\"\"([^\"]*)\"\"\""), "\"\"\"$1\"\"\"")
            .replace(Regex("'''([^']*)'''"), "'''$1'''")
    }
    
    private fun normalizePythonOperators(code: String): String {
        return code
            .replace(Regex("\\band\\b"), "and")
            .replace(Regex("\\bor\\b"), "or")
            .replace(Regex("\\bnot\\b"), "not")
    }
}

class JavaScriptFormattingRules : LanguageFormattingRules {
    override fun applyLanguageSpecificRules(code: String, options: FormattingOptions): String {
        var formatted = code
        
        // JavaScript-specific formatting rules
        formatted = fixJavaScriptSpacing(formatted)
        formatted = fixJavaScriptSemicolons(formatted)
        
        return formatted
    }
    
    private fun fixJavaScriptSpacing(code: String): String {
        return code
            .replace(Regex("\\s*\\+\\s*"), " + ")
            .replace(Regex("\\s*-\\s*"), " - ")
            .replace(Regex("\\s*\\*\\s*"), " * ")
            .replace(Regex("\\s*/\\s*"), " / ")
    }
    
    private fun fixJavaScriptSemicolons(code: String): String {
        return code
            .replace(Regex("([^{}\\n])\\n"), "$1;\n")
    }
}

class JavaFormattingRules : LanguageFormattingRules {
    override fun applyLanguageSpecificRules(code: String, options: FormattingOptions): String {
        var formatted = code
        
        // Java-specific formatting rules
        formatted = fixJavaSpacing(formatted)
        formatted = fixJavaBraces(formatted)
        
        return formatted
    }
    
    private fun fixJavaSpacing(code: String): String {
        return code
            .replace(Regex("\\s*\\+\\s*"), " + ")
            .replace(Regex("\\s*-\\s*"), " - ")
            .replace(Regex("\\s*\\*\\s*"), " * ")
            .replace(Regex("\\s*/\\s*"), " / ")
    }
    
    private fun fixJavaBraces(code: String): String {
        return code
            .replace(Regex("{\\s*\\n"), " {\n")
            .replace(Regex("\\n\\s*}"), "\n}")
    }
}

class KotlinFormattingRules : LanguageFormattingRules {
    override fun applyLanguageSpecificRules(code: String, options: FormattingOptions): String {
        var formatted = code
        
        // Kotlin-specific formatting rules
        formatted = fixKotlinSpacing(formatted)
        formatted = fixKotlinSemicolons(formatted)
        
        return formatted
    }
    
    private fun fixKotlinSpacing(code: String): String {
        return code
            .replace(Regex("\\s*\\+\\s*"), " + ")
            .replace(Regex("\\s*-\\s*"), " - ")
            .replace(Regex("\\s*\\*\\s*"), " * ")
            .replace(Regex("\\s*/\\s*"), " / ")
    }
    
    private fun fixKotlinSemicolons(code: String): String {
        // Kotlin doesn't require semicolons
        return code.replace(Regex(";\\s*\\n"), "\n")
    }
}