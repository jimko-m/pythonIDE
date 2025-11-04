package com.pythonide.ai

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * AI-powered error detection engine that identifies syntax, logic, and style issues
 * Provides intelligent error analysis and suggestions for improvement
 */
class ErrorDetector {
    
    private val TAG = "ErrorDetector"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Language-specific error patterns
    private val errorPatterns = mapOf(
        "python" to PythonErrorPatterns(),
        "javascript" to JavaScriptErrorPatterns(),
        "java" to JavaErrorPatterns(),
        "kotlin" to KotlinErrorPatterns()
    )
    
    // Cache for error analysis results
    private val errorCache = ConcurrentHashMap<String, List<CodeError>>()
    
    /**
     * Initialize the error detector
     */
    fun initialize() {
        Log.d(TAG, "Initializing Error Detector")
        preloadErrorPatterns()
    }
    
    /**
     * Detect errors in code
     */
    fun detectErrors(
        code: String,
        language: String = "python"
    ): List<CodeError> {
        if (code.isBlank()) return emptyList()
        
        val cacheKey = "${language}_${code.hashCode()}"
        errorCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Returning cached error analysis")
            return cached
        }
        
        val errors = mutableListOf<CodeError>()
        
        try {
            val patterns = errorPatterns[language] ?: PythonErrorPatterns()
            
            // Detect syntax errors
            errors.addAll(detectSyntaxErrors(code, language, patterns))
            
            // Detect logic errors
            errors.addAll(detectLogicErrors(code, language, patterns))
            
            // Detect style errors
            errors.addAll(detectStyleErrors(code, language, patterns))
            
            // Detect performance issues
            errors.addAll(detectPerformanceIssues(code, language, patterns))
            
            // Detect security issues
            errors.addAll(detectSecurityIssues(code, language, patterns))
            
            // Detect type errors
            errors.addAll(detectTypeErrors(code, language, patterns))
            
            // Cache results
            errorCache[cacheKey] = errors
            
            Log.d(TAG, "Detected ${errors.size} errors in $language code")
            return errors
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing code", e)
            return emptyList()
        }
    }
    
    /**
     * Detect syntax errors
     */
    private fun detectSyntaxErrors(
        code: String,
        language: String,
        patterns: LanguageErrorPatterns
    ): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Check for basic syntax patterns
            errors.addAll(patterns.detectSyntaxErrors(line, lineNumber))
            
            // Language-specific syntax checks
            when (language) {
                "python" -> {
                    errors.addAll(detectPythonSyntaxErrors(line, lineNumber))
                }
                "javascript" -> {
                    errors.addAll(detectJavaScriptSyntaxErrors(line, lineNumber))
                }
                "java" -> {
                    errors.addAll(detectJavaSyntaxErrors(line, lineNumber))
                }
                "kotlin" -> {
                    errors.addAll(detectKotlinSyntaxErrors(line, lineNumber))
                }
            }
        }
        
        // Check for indentation errors (Python-specific)
        if (language == "python") {
            errors.addAll(checkIndentationErrors(lines))
        }
        
        // Check for bracket matching
        errors.addAll(checkBracketMatching(code, language))
        
        return errors
    }
    
    /**
     * Detect logic errors
     */
    private fun detectLogicErrors(
        code: String,
        language: String,
        patterns: LanguageErrorPatterns
    ): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        // Check for common logic issues
        errors.addAll(checkDeadCode(lines))
        errors.addAll(checkInfiniteLoops(lines))
        errors.addAll(checkUnreachableCode(lines))
        errors.addAll(checkUninitializedVariables(code, language))
        errors.addAll(checkTypeMismatch(code, language))
        errors.addAll(checkNullPointerRisks(code, language))
        
        return errors
    }
    
    /**
     * Detect style errors
     */
    private fun detectStyleErrors(
        code: String,
        language: String,
        patterns: LanguageErrorPatterns
    ): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        // Check line length
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            if (line.length > getMaxLineLength(language)) {
                errors.add(
                    CodeError(
                        type = ErrorType.STYLE,
                        message = "Line too long (${line.length} > ${getMaxLineLength(language)})",
                        line = lineNumber,
                        column = 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Break long lines into multiple lines"
                    )
                )
            }
        }
        
        // Check naming conventions
        errors.addAll(checkNamingConventions(code, language))
        
        // Check code complexity
        errors.addAll(checkCodeComplexity(code, language))
        
        return errors
    }
    
    /**
     * Detect performance issues
     */
    private fun detectPerformanceIssues(
        code: String,
        language: String,
        patterns: LanguageErrorPatterns
    ): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        // Python-specific performance issues
        if (language == "python") {
            errors.addAll(checkPythonPerformanceIssues(code))
        }
        
        // Check for inefficient operations
        errors.addAll(checkInefficientOperations(code, language))
        
        // Check for memory leaks
        errors.addAll(checkMemoryLeaks(code, language))
        
        return errors
    }
    
    /**
     * Detect security issues
     */
    private fun detectSecurityIssues(
        code: String,
        language: String,
        patterns: LanguageErrorPatterns
    ): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Check for common security vulnerabilities
            errors.addAll(checkSecurityVulnerabilities(line, lineNumber, language))
        }
        
        // Check for SQL injection risks
        errors.addAll(checkSQLInjectionRisks(code, language))
        
        // Check for XSS risks
        errors.addAll(checkXSSRisks(code, language))
        
        return errors
    }
    
    /**
     * Detect type errors
     */
    private fun detectTypeErrors(
        code: String,
        language: String,
        patterns: LanguageErrorPatterns
    ): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Static type checking for statically typed languages
        if (language in listOf("java", "kotlin")) {
            errors.addAll(checkStaticTypeErrors(code, language))
        }
        
        // Dynamic type issues for dynamically typed languages
        if (language in listOf("python", "javascript")) {
            errors.addAll(checkDynamicTypeIssues(code, language))
        }
        
        return errors
    }
    
    // Language-specific error detection methods
    
    private fun detectPythonSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for common Python syntax errors
        when {
            line.contains("==") && !line.contains("=") -> {
                // Missing assignment operator check
            }
            line.contains("def ") && !line.contains("(") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Function definition missing parentheses",
                        line = lineNumber,
                        column = line.indexOf("def ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add parentheses: def function_name():"
                    )
                )
            }
            line.contains("if ") && !line.contains(":") && !line.contains(" then ") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Missing colon after if statement",
                        line = lineNumber,
                        column = line.indexOf("if ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add colon: if condition:"
                    )
                )
            }
            line.contains("for ") && !line.contains(" in ") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Missing 'in' in for loop",
                        line = lineNumber,
                        column = line.indexOf("for ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Use: for item in iterable:"
                    )
                )
            }
            line.contains("except ") && !line.contains(":") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Missing colon after except statement",
                        line = lineNumber,
                        column = line.indexOf("except ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add colon: except ExceptionType:"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun detectJavaScriptSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for common JavaScript syntax errors
        when {
            line.contains("function ") && !line.contains("{") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Function missing opening brace",
                        line = lineNumber,
                        column = line.indexOf("function ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add opening brace: function name() {"
                    )
                )
            }
            line.contains("if ") && !line.contains("(") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Missing parentheses in if statement",
                        line = lineNumber,
                        column = line.indexOf("if ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add parentheses: if (condition) {"
                    )
                )
            }
            line.contains("==") && line.contains("=") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.STYLE,
                        message = "Use === instead of == for strict equality",
                        line = lineNumber,
                        column = line.indexOf("==") + 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Use === for strict equality comparison"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun detectJavaSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for common Java syntax errors
        when {
            line.contains("public ") && line.contains("class ") && !line.contains("{") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Class declaration missing opening brace",
                        line = lineNumber,
                        column = line.indexOf("class ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add opening brace: public class ClassName {"
                    )
                )
            }
            line.contains("if ") && !line.contains("(") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Missing parentheses in if statement",
                        line = lineNumber,
                        column = line.indexOf("if ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add parentheses: if (condition) {"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun detectKotlinSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for common Kotlin syntax errors
        when {
            line.contains("fun ") && !line.contains("(") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Function missing parentheses",
                        line = lineNumber,
                        column = line.indexOf("fun ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add parentheses: fun functionName() {"
                    )
                )
            }
            line.contains("if ") && !line.contains("(") -> {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Missing parentheses in if statement",
                        line = lineNumber,
                        column = line.indexOf("if ") + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add parentheses: if (condition) {"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkIndentationErrors(lines: List<String>): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val indentStack = mutableListOf<Int>()
        var expectedIndent = 0
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()
            
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("\"\"\"")) {
                return@forEachIndexed
            }
            
            val currentIndent = line.length - trimmed.length
            
            when {
                indentStack.isEmpty() -> {
                    // Top level
                    if (currentIndent > 0) {
                        errors.add(
                            CodeError(
                                type = ErrorType.SYNTAX,
                                message = "Indentation at top level",
                                line = lineNumber,
                                column = 1,
                                severity = ErrorSeverity.ERROR,
                                suggestion = "Remove extra indentation"
                            )
                        )
                    }
                    expectedIndent = 0
                }
                currentIndent > expectedIndent -> {
                    // Indented block
                    indentStack.add(expectedIndent)
                    expectedIndent = currentIndent
                }
                currentIndent < expectedIndent -> {
                    // Dedented block
                    while (indentStack.isNotEmpty() && currentIndent < expectedIndent) {
                        indentStack.removeAt(0)
                        expectedIndent = indentStack.getOrNull(0) ?: 0
                    }
                    
                    if (currentIndent != expectedIndent) {
                        errors.add(
                            CodeError(
                                type = ErrorType.SYNTAX,
                                message = "Inconsistent indentation",
                                line = lineNumber,
                                column = 1,
                                severity = ErrorSeverity.ERROR,
                                suggestion = "Use consistent indentation (4 spaces)"
                            )
                        )
                    }
                }
            }
        }
        
        return errors
    }
    
    private fun checkBracketMatching(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val stack = mutableListOf<Char>()
        val positions = mutableMapOf<Char, MutableList<Int>>()
        val lines = code.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            line.forEachIndexed { charIndex, char ->
                when (char) {
                    '(', '[', '{' -> {
                        stack.add(char)
                        positions.getOrPut(char) { mutableListOf() }.add(lineIndex + 1)
                    }
                    ')', ']', '}' -> {
                        if (stack.isEmpty()) {
                            errors.add(
                                CodeError(
                                    type = ErrorType.SYNTAX,
                                    message = "Unmatched closing bracket '$char'",
                                    line = lineIndex + 1,
                                    column = charIndex + 1,
                                    severity = ErrorSeverity.ERROR,
                                    suggestion = "Check for missing opening bracket"
                                )
                            )
                        } else {
                            val lastBracket = stack.removeAt(stack.size - 1)
                            if (!isMatchingBracket(lastBracket, char)) {
                                errors.add(
                                    CodeError(
                                        type = ErrorType.SYNTAX,
                                        message = "Mismatched brackets: '$lastBracket' and '$char'",
                                        line = lineIndex + 1,
                                        column = charIndex + 1,
                                        severity = ErrorSeverity.ERROR,
                                        suggestion = "Use matching brackets"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Check for unmatched opening brackets
        stack.forEach { bracket ->
            val posList = positions[bracket] ?: emptyList()
            if (posList.isNotEmpty()) {
                errors.add(
                    CodeError(
                        type = ErrorType.SYNTAX,
                        message = "Unmatched opening bracket '$bracket'",
                        line = posList.last(),
                        column = 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Add matching closing bracket"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun isMatchingBracket(open: Char, close: Char): Boolean {
        return when (open) {
            '(' -> close == ')'
            '[' -> close == ']'
            '{' -> close == '}'
            else -> false
        }
    }
    
    // Other helper methods for different error types
    
    private fun checkDeadCode(lines: List<String>): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        var foundReturn = false
        
        for (i in lines.indices) {
            val line = lines[i].trim()
            
            if (foundReturn && line.isNotEmpty() && !line.startsWith("#")) {
                errors.add(
                    CodeError(
                        type = ErrorType.LOGIC,
                        message = "Unreachable code after return statement",
                        line = i + 1,
                        column = 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Remove unreachable code or reorganize logic"
                    )
                )
            }
            
            if (line.contains("return") || line.contains("break") || line.contains("continue")) {
                foundReturn = true
            }
        }
        
        return errors
    }
    
    private fun checkInfiniteLoops(lines: List<String>): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check for while true without break
            if (trimmed.contains("while True") || trimmed.contains("while true")) {
                val followingLines = lines.subList(index + 1, minOf(index + 20, lines.size))
                val hasBreak = followingLines.any { 
                    it.trim().contains("break", ignoreCase = true) 
                }
                
                if (!hasBreak) {
                    errors.add(
                        CodeError(
                            type = ErrorType.LOGIC,
                            message = "Potential infinite loop",
                            line = index + 1,
                            column = 1,
                            severity = ErrorSeverity.ERROR,
                            suggestion = "Add break condition or break statement"
                        )
                    )
                }
            }
        }
        
        return errors
    }
    
    private fun checkUnreachableCode(lines: List<String>): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        var foundExit = false
        
        for (i in lines.indices) {
            val line = lines[i].trim()
            
            if (foundExit && line.isNotEmpty() && !line.startsWith("#")) {
                errors.add(
                    CodeError(
                        type = ErrorType.LOGIC,
                        message = "Unreachable code after $line",
                        line = i + 1,
                        column = 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Remove unreachable code"
                    )
                )
            }
            
            if (line.contains("sys.exit") || line.contains("exit(") || line.contains("quit(")) {
                foundExit = true
            }
        }
        
        return errors
    }
    
    private fun checkUninitializedVariables(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Simple check for variables used before declaration
        val variablePattern = when (language) {
            "python" -> Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b(?=\\s*=)")
            "javascript" -> Regex("\\b(let|const|var)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b")
            else -> Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b")
        }
        
        val matches = variablePattern.findAll(code)
        val declaredVariables = mutableSetOf<String>()
        val usedVariables = mutableSetOf<String>()
        
        matches.forEach { match ->
            val varName = match.groupValues.getOrNull(1) ?: match.groupValues.getOrNull(2)
            if (varName != null) {
                declaredVariables.add(varName)
            }
        }
        
        // This is a simplified check - a real implementation would need more sophisticated analysis
        if (language == "python") {
            val usagePattern = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b(?![^()]*)")
            usagePattern.findAll(code).forEach { match ->
                val varName = match.groupValues[1]
                if (varName !in declaredVariables && 
                    !isKeyword(varName, language) && 
                    varName.length > 2) {
                    usedVariables.add(varName)
                }
            }
        }
        
        return errors
    }
    
    private fun checkTypeMismatch(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Simplified type checking - look for obvious mismatches
        val typeMismatchPatterns = when (language) {
            "python" -> listOf(
                Regex("\"\\d+\"\\s*\\+\\s*\\d+"),
                Regex("'\\w+'\\s*\\+\\s*\\d+"),
                Regex("\\d+\\s*\\+\\s*\"\\w+\"")
            )
            else -> emptyList()
        }
        
        typeMismatchPatterns.forEach { pattern ->
            pattern.findAll(code).forEach { match ->
                val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
                errors.add(
                    CodeError(
                        type = ErrorType.TYPE,
                        message = "Type mismatch: cannot concatenate string and number",
                        line = lineNumber,
                        column = match.range.first + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Convert types explicitly or use string formatting"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkNullPointerRisks(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for potential null pointer dereferences
        val nullPointerPattern = when (language) {
            "python" -> Regex("\\w+\\s*\\.\\w+\\s*\\([^)]*\\)")
            "javascript" -> Regex("\\w+\\s*\\.\\w+")
            "java" -> Regex("\\w+\\s*\\.\\w+")
            else -> Regex("\\w+\\s*\\.\\w+")
        }
        
        nullPointerPattern.findAll(code).forEach { match ->
            val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
            errors.add(
                CodeError(
                    type = ErrorType.LOGIC,
                    message = "Potential null pointer dereference",
                    line = lineNumber,
                    column = match.range.first + 1,
                    severity = ErrorSeverity.WARNING,
                    suggestion = "Add null checking before dereferencing"
                )
            )
        }
        
        return errors
    }
    
    private fun checkNamingConventions(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check function naming
            if (trimmed.startsWith("def ")) {
                val functionName = trimmed.substringAfter("def ").substringBefore("(")
                if (!isValidFunctionName(functionName, language)) {
                    errors.add(
                        CodeError(
                            type = ErrorType.STYLE,
                            message = "Function name should follow naming conventions",
                            line = index + 1,
                            column = 1,
                            severity = ErrorSeverity.INFO,
                            suggestion = "Use snake_case for functions in Python"
                        )
                    )
                }
            }
            
            // Check class naming
            if (trimmed.startsWith("class ")) {
                val className = trimmed.substringAfter("class ").substringBefore(":")
                if (!isValidClassName(className, language)) {
                    errors.add(
                        CodeError(
                            type = ErrorType.STYLE,
                            message = "Class name should follow naming conventions",
                            line = index + 1,
                            column = 1,
                            severity = ErrorSeverity.INFO,
                            suggestion = "Use PascalCase for classes in Python"
                        )
                    )
                }
            }
            
            // Check variable naming
            val variableMatches = Regex("\\b([a-z][a-zA-Z0-9_]*)\\s*=").findAll(line)
            variableMatches.forEach { match ->
                val varName = match.groupValues[1]
                if (!isValidVariableName(varName, language)) {
                    errors.add(
                        CodeError(
                            type = ErrorType.STYLE,
                            message = "Variable name should follow naming conventions",
                            line = index + 1,
                            column = match.range.first + 1,
                            severity = ErrorSeverity.INFO,
                            suggestion = "Use snake_case for variables in Python"
                        )
                    )
                }
            }
        }
        
        return errors
    }
    
    private fun checkCodeComplexity(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        // Check for too many nested blocks
        var maxNesting = 0
        var currentNesting = 0
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Increase nesting for opening blocks
            if (trimmed.endsWith(":")) {
                currentNesting++
                maxNesting = maxOf(maxNesting, currentNesting)
            }
            // Decrease nesting for closing blocks
            else if (trimmed.startsWith("elif ") || trimmed.startsWith("else") || 
                     trimmed.startsWith("except ") || trimmed.startsWith("finally")) {
                currentNesting--
            }
            
            if (currentNesting > 4) {
                errors.add(
                    CodeError(
                        type = ErrorType.STYLE,
                        message = "Too deeply nested (${currentNesting} levels)",
                        line = index + 1,
                        column = 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Consider refactoring to reduce nesting depth"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkPythonPerformanceIssues(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check for range(len()) usage
            if (trimmed.contains("for i in range(len(")) {
                errors.add(
                    CodeError(
                        type = ErrorType.PERFORMANCE,
                        message = "Use enumerate() instead of range(len())",
                        line = index + 1,
                        column = line.indexOf("range(len(") + 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Replace with: for i, item in enumerate(items):"
                    )
                )
            }
            
            // Check for string concatenation in loop
            if (trimmed.contains("+=") && trimmed.contains("str(") && 
                lines.getOrNull(index - 1)?.contains("for ") == true) {
                errors.add(
                    CodeError(
                        type = ErrorType.PERFORMANCE,
                        message = "String concatenation in loop is inefficient",
                        line = index + 1,
                        column = 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Use list.append() and join() instead"
                    )
                )
            }
            
            // Check for inefficient list operations
            if (trimmed.contains(".append(") && trimmed.contains("+")) {
                errors.add(
                    CodeError(
                        type = ErrorType.PERFORMANCE,
                        message = "Inefficient list operation",
                        line = index + 1,
                        column = 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Use list comprehensions or extend() for better performance"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkInefficientOperations(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for common inefficient patterns
        val inefficientPatterns = listOf(
            Regex("\\.index\\(.*\\)\\s*==\\s*-1") to "Use 'in' operator instead of index()",
            Regex("\\.count\\(.*\\)\\s*>\\s*0") to "Use 'in' operator instead of count()",
            Regex("\\.find\\(.*\\)\\s*!=\\s*-1") to "Use 'in' operator instead of find()"
        )
        
        inefficientPatterns.forEach { (pattern, message) ->
            pattern.findAll(code).forEach { match ->
                val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
                errors.add(
                    CodeError(
                        type = ErrorType.PERFORMANCE,
                        message = message,
                        line = lineNumber,
                        column = match.range.first + 1,
                        severity = ErrorSeverity.WARNING,
                        suggestion = "Use more efficient alternatives"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkMemoryLeaks(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for potential memory leaks
        if (code.contains("global ") && code.count { it == '\n' } > 10) {
            val lineNumber = code.indexOf("global ") + 1
            errors.add(
                CodeError(
                    type = ErrorType.PERFORMANCE,
                    message = "Extensive use of global variables may cause memory issues",
                    line = lineNumber,
                    column = 1,
                    severity = ErrorSeverity.WARNING,
                    suggestion = "Consider using class attributes or function parameters"
                )
            )
        }
        
        return errors
    }
    
    private fun checkSecurityVulnerabilities(line: String, lineNumber: Int, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val trimmed = line.trim()
        
        when (language) {
            "python" -> {
                if (trimmed.contains("eval(") || trimmed.contains("exec(")) {
                    errors.add(
                        CodeError(
                            type = ErrorType.SECURITY,
                            message = "Use of eval() or exec() is dangerous",
                            line = lineNumber,
                            column = line.indexOf(trimmed) + 1,
                            severity = ErrorSeverity.CRITICAL,
                            suggestion = "Avoid dynamic code execution or use safe alternatives"
                        )
                    )
                }
                
                if (trimmed.contains("os.system(") || trimmed.contains("subprocess.call(")) {
                    errors.add(
                        CodeError(
                            type = ErrorType.SECURITY,
                            message = "Command execution detected - potential security risk",
                            line = lineNumber,
                            column = line.indexOf(trimmed) + 1,
                            severity = ErrorSeverity.ERROR,
                            suggestion = "Validate inputs and use secure execution methods"
                        )
                    )
                }
                
                if (trimmed.contains("pickle.load") || trimmed.contains("pickle.loads")) {
                    errors.add(
                        CodeError(
                            type = ErrorType.SECURITY,
                            message = "pickle usage can be unsafe with untrusted data",
                            line = lineNumber,
                            column = line.indexOf(trimmed) + 1,
                            severity = ErrorSeverity.WARNING,
                            suggestion = "Ensure data is trusted or use safer serialization"
                        )
                    )
                }
            }
            
            "javascript" -> {
                if (trimmed.contains("innerHTML")) {
                    errors.add(
                        CodeError(
                            type = ErrorType.SECURITY,
                            message = "innerHTML can lead to XSS vulnerabilities",
                            line = lineNumber,
                            column = line.indexOf(trimmed) + 1,
                            severity = ErrorSeverity.ERROR,
                            suggestion = "Use textContent or sanitize HTML"
                        )
                    )
                }
                
                if (trimmed.contains("eval(")) {
                    errors.add(
                        CodeError(
                            type = ErrorType.SECURITY,
                            message = "eval() is dangerous and can execute malicious code",
                            line = lineNumber,
                            column = line.indexOf(trimmed) + 1,
                            severity = ErrorSeverity.CRITICAL,
                            suggestion = "Avoid eval() or use JSON.parse() for JSON"
                        )
                    )
                }
            }
        }
        
        return errors
    }
    
    private fun checkSQLInjectionRisks(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            if (trimmed.contains("execute(") && (trimmed.contains("+") || trimmed.contains("%"))) {
                errors.add(
                    CodeError(
                        type = ErrorType.SECURITY,
                        message = "Potential SQL injection vulnerability",
                        line = index + 1,
                        column = 1,
                        severity = ErrorSeverity.CRITICAL,
                        suggestion = "Use parameterized queries or prepared statements"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkXSSRisks(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for XSS patterns
        val xssPatterns = listOf(
            Regex("innerHTML\\s*=.*\\+"),
            Regex("document\\.write\\(.*\\+"),
            Regex("\\$\\.html\\(.*\\+")
        )
        
        xssPatterns.forEach { pattern ->
            pattern.findAll(code).forEach { match ->
                val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
                errors.add(
                    CodeError(
                        type = ErrorType.SECURITY,
                        message = "Potential XSS vulnerability",
                        line = lineNumber,
                        column = match.range.first + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Sanitize user input or use safe DOM methods"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkStaticTypeErrors(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Simplified static type checking
        if (language == "java") {
            val typeMismatchPattern = Regex("\\w+\\s+\\w+\\s*=\\s*\"\\d+\"")
            typeMismatchPattern.findAll(code).forEach { match ->
                val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
                errors.add(
                    CodeError(
                        type = ErrorType.TYPE,
                        message = "Type mismatch: assigning string to numeric variable",
                        line = lineNumber,
                        column = match.range.first + 1,
                        severity = ErrorSeverity.ERROR,
                        suggestion = "Use proper type conversion or change variable type"
                    )
                )
            }
        }
        
        return errors
    }
    
    private fun checkDynamicTypeIssues(code: String, language: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        
        // Check for common dynamic type issues
        val undefinedVariablePattern = Regex("\\w+\\s*\\.\\w+\\s*\\([^)]*\\)")
        undefinedVariablePattern.findAll(code).forEach { match ->
            val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
            errors.add(
                CodeError(
                    type = ErrorType.TYPE,
                    message = "Potential undefined method call",
                    line = lineNumber,
                    column = match.range.first + 1,
                    severity = ErrorSeverity.WARNING,
                    suggestion = "Ensure variable is properly initialized"
                )
            )
        }
        
        return errors
    }
    
    // Helper methods
    
    private fun getMaxLineLength(language: String): Int {
        return when (language) {
            "python" -> 79
            "java" -> 80
            "javascript" -> 80
            "kotlin" -> 120
            else -> 80
        }
    }
    
    private fun isValidFunctionName(name: String, language: String): Boolean {
        return when (language) {
            "python" -> Regex("^[a-z_][a-z0-9_]*$").matches(name)
            "javascript" -> Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$").matches(name)
            "java" -> Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$").matches(name) && 
                      !Character.isDigit(name.first())
            "kotlin" -> Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$").matches(name)
            else -> true
        }
    }
    
    private fun isValidClassName(name: String, language: String): Boolean {
        return when (language) {
            "python" -> Regex("^[A-Z][a-zA-Z0-9]*$").matches(name)
            "javascript" -> Regex("^[A-Z][a-zA-Z0-9]*$").matches(name)
            "java" -> Regex("^[A-Z][a-zA-Z0-9]*$").matches(name)
            "kotlin" -> Regex("^[A-Z][a-zA-Z0-9]*$").matches(name)
            else -> true
        }
    }
    
    private fun isValidVariableName(name: String, language: String): Boolean {
        return when (language) {
            "python" -> Regex("^[a-z_][a-z0-9_]*$").matches(name)
            "javascript" -> Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$").matches(name)
            "java" -> Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$").matches(name)
            "kotlin" -> Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$").matches(name)
            else -> true
        }
    }
    
    private fun isKeyword(word: String, language: String): Boolean {
        val keywords = when (language) {
            "python" -> PythonCompletionPatterns().getKeywords()
            "javascript" -> JavaScriptCompletionPatterns().getKeywords()
            "java" -> JavaCompletionPatterns().getKeywords()
            "kotlin" -> KotlinCompletionPatterns().getKeywords()
            else -> emptyList()
        }
        return word in keywords
    }
    
    private fun preloadErrorPatterns() {
        Log.d(TAG, "Preloading error detection patterns")
        // Pre-load error patterns for faster detection
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        errorCache.clear()
    }
}

/**
 * Language-specific error pattern interfaces and implementations
 */

interface LanguageErrorPatterns {
    fun detectSyntaxErrors(line: String, lineNumber: Int): List<CodeError>
}

class PythonErrorPatterns : LanguageErrorPatterns {
    override fun detectSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        // Python-specific syntax error detection
        return emptyList()
    }
}

class JavaScriptErrorPatterns : LanguageErrorPatterns {
    override fun detectSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        // JavaScript-specific syntax error detection
        return emptyList()
    }
}

class JavaErrorPatterns : LanguageErrorPatterns {
    override fun detectSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        // Java-specific syntax error detection
        return emptyList()
    }
}

class KotlinErrorPatterns : LanguageErrorPatterns {
    override fun detectSyntaxErrors(line: String, lineNumber: Int): List<CodeError> {
        // Kotlin-specific syntax error detection
        return emptyList()
    }
}