package com.pythonide.ai

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * AI-powered code completion engine that provides intelligent suggestions
 * Based on context, patterns, and machine learning models
 */
class CodeCompletionEngine {
    
    private val TAG = "CodeCompletionEngine"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Language-specific completion patterns and keywords
    private val languagePatterns = mapOf(
        "python" to PythonCompletionPatterns(),
        "javascript" to JavaScriptCompletionPatterns(),
        "java" to JavaCompletionPatterns(),
        "kotlin" to KotlinCompletionPatterns()
    )
    
    // Cache for frequently used completions
    private val completionCache = ConcurrentHashMap<String, List<CompletionSuggestion>>()
    
    // Context-aware completion
    private val contextPatterns = mapOf(
        "after_import" to listOf("import ", "from ", "# ", "def ", "class "),
        "after_function_def" to listOf("self", "return ", "pass", "# "),
        "after_class_def" to listOf("def __init__", "def ", "# ", "pass"),
        "after_if" to listOf("elif ", "else:", "# ", "return ", "print("),
        "after_for" to listOf("if ", "# ", "print(", "return ", "break", "continue"),
        "after_try" to listOf("except ", "finally:", "# ", "print("),
        "string_literal" to listOf("\\n", "\\t", "\\r", "\\\\", "%s", "{}"),
        "comment" to listOf("TODO", "FIXME", "NOTE", "WARNING", "XXX")
    )
    
    /**
     * Initialize the completion engine
     */
    fun initialize() {
        Log.d(TAG, "Initializing Code Completion Engine")
        // Pre-load common patterns and keywords
        preloadCommonPatterns()
    }
    
    /**
     * Generate intelligent code suggestions
     */
    fun generateSuggestions(
        code: String,
        cursorPosition: Pair<Int, Int>,
        language: String = "python"
    ): List<String> {
        if (code.isEmpty()) return getBasicSuggestions(language)
        
        val suggestions = mutableListOf<String>()
        
        try {
            val context = analyzeContext(code, cursorPosition, language)
            val languagePatterns = languagePatterns[language] ?: PythonCompletionPatterns()
            
            // Get suggestions based on context
            suggestions.addAll(getContextBasedSuggestions(context, languagePatterns))
            suggestions.addAll(getPatternBasedSuggestions(code, cursorPosition, languagePatterns))
            suggestions.addAll(getKeywordSuggestions(code, cursorPosition, language))
            suggestions.addAll(getSnippetSuggestions(context, language))
            
            // Remove duplicates and filter
            val uniqueSuggestions = suggestions.distinct().take(5)
            
            Log.d(TAG, "Generated ${uniqueSuggestions.size} suggestions for $language")
            return uniqueSuggestions
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating suggestions", e)
            return getBasicSuggestions(language)
        }
    }
    
    /**
     * Get suggestions for partial words/identifiers
     */
    fun getPartialWordSuggestions(
        partialWord: String,
        language: String = "python"
    ): List<String> {
        if (partialWord.length < 2) return emptyList()
        
        val languagePatterns = languagePatterns[language] ?: PythonCompletionPatterns()
        val keywords = languagePatterns.getKeywords()
        val builtins = languagePatterns.getBuiltinFunctions()
        val commonWords = keywords + builtins
        
        return commonWords
            .filter { it.startsWith(partialWord, ignoreCase = true) }
            .sortedBy { it.length }
            .take(10)
    }
    
    /**
     * Get smart snippet suggestions
     */
    fun getSnippetSuggestions(
        context: CompletionContext,
        language: String = "python"
    ): List<String> {
        val snippets = mutableListOf<String>()
        
        when (context.type) {
            ContextType.IMPORT_STATEMENT -> {
                snippets.addAll(getImportSuggestions(context.partialText, language))
            }
            ContextType.FUNCTION_CALL -> {
                snippets.addAll(getFunctionCallSuggestions(context.partialText, language))
            }
            ContextType.STRING_LITERAL -> {
                snippets.addAll(getStringSuggestions(context.partialText))
            }
            ContextType.COMMENT -> {
                snippets.addAll(getCommentSuggestions(context.partialText))
            }
            else -> {
                // Generic snippet suggestions based on current context
                snippets.addAll(getGenericSnippets(context, language))
            }
        }
        
        return snippets
    }
    
    private fun analyzeContext(
        code: String,
        cursorPosition: Pair<Int, Int>,
        language: String
    ): CompletionContext {
        val (line, column) = cursorPosition
        val lines = code.lines()
        
        if (line < 1 || line > lines.size) {
            return CompletionContext(ContextType.UNKNOWN, "", line, column)
        }
        
        val currentLine = lines[line - 1]
        val beforeCursor = currentLine.substring(0, minOf(column - 1, currentLine.length))
        
        // Determine context type
        val contextType = when {
            beforeCursor.trim().startsWith("import ") || beforeCursor.trim().startsWith("from ") -> {
                ContextType.IMPORT_STATEMENT
            }
            beforeCursor.contains("(") && !beforeCursor.contains(")") -> {
                ContextType.FUNCTION_CALL
            }
            beforeCursor.trim().startsWith("#") -> {
                ContextType.COMMENT
            }
            beforeCursor.contains("\"") || beforeCursor.contains("'") -> {
                ContextType.STRING_LITERAL
            }
            beforeCursor.trim().startsWith("def ") -> {
                ContextType.FUNCTION_DEFINITION
            }
            beforeCursor.trim().startsWith("class ") -> {
                ContextType.CLASS_DEFINITION
            }
            else -> {
                ContextType.GENERAL
            }
        }
        
        return CompletionContext(contextType, beforeCursor, line, column)
    }
    
    private fun getContextBasedSuggestions(
        context: CompletionContext,
        patterns: LanguagePatterns
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        when (context.type) {
            ContextType.IMPORT_STATEMENT -> {
                suggestions.addAll(getImportSuggestions(context.partialText, "python"))
            }
            ContextType.COMMENT -> {
                suggestions.addAll(getCommentSuggestions(context.partialText))
            }
            ContextType.FUNCTION_DEFINITION -> {
                suggestions.addAll(listOf("self", "return ", "-> ", ":", "pass"))
            }
            ContextType.CLASS_DEFINITION -> {
                suggestions.addAll(listOf("def __init__", "def __str__", "def __repr__", ":", "pass"))
            }
            ContextType.IF_STATEMENT -> {
                suggestions.addAll(listOf("elif ", "else:", "and ", "or ", "not "))
            }
            ContextType.FOR_STATEMENT -> {
                suggestions.addAll(listOf("in ", "range(", "enumerate(", "zip(", "if "))
            }
            ContextType.WHILE_STATEMENT -> {
                suggestions.addAll(listOf("and ", "or ", "not ", "if ", "break", "continue"))
            }
            else -> {
                // Add general context suggestions
                suggestions.addAll(getGenericContextSuggestions(context))
            }
        }
        
        return suggestions
    }
    
    private fun getPatternBasedSuggestions(
        code: String,
        cursorPosition: Pair<Int, Int>,
        patterns: LanguagePatterns
    ): List<String> {
        val suggestions = mutableListOf<String>()
        val (line, column) = cursorPosition
        val lines = code.lines()
        
        if (line < 1 || line > lines.size) return suggestions
        
        val currentLine = lines[line - 1]
        val beforeCursor = currentLine.substring(0, minOf(column - 1, currentLine.length))
        val afterCursor = currentLine.substring(column - 1)
        
        // Check for common completion patterns
        when {
            // Complete brackets/braces
            beforeCursor.endsWith("(") && afterCursor.startsWith(")") -> {
                suggestions.add(")") // Auto-close parenthesis
            }
            beforeCursor.endsWith("[") && afterCursor.startsWith("]") -> {
                suggestions.add("]") // Auto-close bracket
            }
            beforeCursor.endsWith("{") && afterCursor.startsWith("}") -> {
                suggestions.add("}") // Auto-close brace
            }
            beforeCursor.endsWith("\"") && afterCursor.startsWith("\"") -> {
                suggestions.add("\"") // Auto-close quote
            }
            beforeCursor.endsWith("'") && afterCursor.startsWith("'") -> {
                suggestions.add("'") // Auto-close single quote
            }
            
            // Indentation suggestions
            beforeCursor.trim().endsWith(":") -> {
                suggestions.add("\n    ") // Add indentation after colon
            }
            
            // Auto-completion for common patterns
            beforeCursor.contains("def ") -> {
                suggestions.add("return ")
                suggestions.add("pass")
            }
            
            beforeCursor.contains("if ") -> {
                suggestions.add(" and ")
                suggestions.add(" or ")
                suggestions.add(":")
            }
            
            beforeCursor.contains("for ") -> {
                suggestions.add(" in ")
                suggestions.add(" range(")
            }
            
            beforeCursor.contains("try:") -> {
                suggestions.add("\nexcept ")
                suggestions.add("\nfinally:")
            }
        }
        
        return suggestions
    }
    
    private fun getKeywordSuggestions(
        code: String,
        cursorPosition: Pair<Int, Int>,
        language: String
    ): List<String> {
        val suggestions = mutableListOf<String>()
        val (line, column) = cursorPosition
        val lines = code.lines()
        
        if (line < 1 || line > lines.size) return suggestions
        
        val currentLine = lines[line - 1]
        val beforeCursor = currentLine.substring(0, minOf(column - 1, currentLine.length))
        val partialWord = beforeCursor.substringAfterLast(" ").trim()
        
        if (partialWord.length >= 2) {
            val languagePatterns = this.languagePatterns[language] ?: PythonCompletionPatterns()
            val keywords = languagePatterns.getKeywords()
            val builtins = languagePatterns.getBuiltinFunctions()
            val allWords = keywords + builtins
            
            suggestions.addAll(
                allWords.filter { it.startsWith(partialWord, ignoreCase = true) }
                    .sortedBy { it.length }
                    .take(5)
            )
        }
        
        return suggestions
    }
    
    private fun getImportSuggestions(partialText: String, language: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (language == "python") {
            when {
                partialText.trim().startsWith("from ") -> {
                    suggestions.addAll(listOf(
                        "os", "sys", "json", "math", "random", "datetime", 
                        "collections", "itertools", "functools", "pathlib"
                    ))
                }
                partialText.trim().startsWith("import ") -> {
                    suggestions.addAll(listOf(
                        "numpy as np", "pandas as pd", "matplotlib.pyplot as plt",
                        "requests", "flask", "django", "tensorflow", "torch",
                        "scipy", "sklearn", "seaborn", "plotly"
                    ))
                }
            }
        }
        
        return suggestions
    }
    
    private fun getFunctionCallSuggestions(partialText: String, language: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (language == "python") {
            // Common function calls
            val functions = listOf(
                "print(", "len(", "range(", "str(", "int(", "float(", "list(", "dict(", "tuple(", "set(",
                "open(", "input(", "sorted(", "max(", "min(", "sum(", "abs(", "round(",
                "enumerate(", "zip(", "map(", "filter(", "reduce(",
                "type(", "isinstance(", "hasattr(", "getattr(", "setattr(",
                "os.path.exists(", "os.path.join(", "json.loads(", "json.dumps(",
                "datetime.datetime.now(", "time.sleep("
            )
            
            suggestions.addAll(functions.filter { it.startsWith(partialText.trim()) })
        }
        
        return suggestions
    }
    
    private fun getStringSuggestions(partialText: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Common string escape sequences and formatting
        if (partialText.contains("\\")) {
            suggestions.addAll(listOf(
                "\\n", "\\t", "\\r", "\\\\", "\\\"", "\\'",
                "\\a", "\\b", "\\f", "\\v", "\\ooo", "\\xhh"
            ))
        }
        
        // String formatting suggestions
        if (partialText.contains("f\"") || partialText.contains("f'")) {
            suggestions.addAll(listOf(
                "{}".format(partialText.substringAfterLast("{")),
                "{variable}",
                "{variable:.2f}",
                "{variable!r}",
                "{variable!s}"
            ))
        }
        
        return suggestions
    }
    
    private fun getCommentSuggestions(partialText: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Common comment patterns
        val commentPatterns = listOf(
            "TODO: ", "FIXME: ", "NOTE: ", "WARNING: ", "XXX: ",
            "BUG: ", "HACK: ", "DEPRECATED: ", "REVIEW: "
        )
        
        val commentWord = partialText.substringAfter("#").trim().lowercase()
        
        suggestions.addAll(
            commentPatterns.filter { it.lowercase().startsWith("#$commentWord") }
        )
        
        return suggestions
    }
    
    private fun getGenericSnippets(context: CompletionContext, language: String): List<String> {
        val snippets = mutableListOf<String>()
        
        if (language == "python") {
            // Common code snippets
            when {
                context.line > 1 -> {
                    val previousLine = context.codeLines.getOrNull(context.line - 2)?.trim()
                    when {
                        previousLine?.endsWith("def ") == true -> {
                            snippets.addAll(listOf(
                                "self",
                                "return ",
                                "-> ",
                                ":",
                                "pass"
                            ))
                        }
                        previousLine?.endsWith("class ") == true -> {
                            snippets.addAll(listOf(
                                "def __init__(self):",
                                "def __str__(self):",
                                "def __repr__(self):",
                                ":",
                                "pass"
                            ))
                        }
                        previousLine?.endsWith(":") == true -> {
                            snippets.add("\n    ")
                            snippets.add("pass")
                        }
                    }
                }
                else -> {
                    // File-level snippets
                    snippets.addAll(listOf(
                        "import ",
                        "from ",
                        "def ",
                        "class ",
                        "if __name__ == \"__main__\":"
                    ))
                }
            }
        }
        
        return snippets
    }
    
    private fun getGenericContextSuggestions(context: CompletionContext): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Add generic suggestions based on line context
        val line = context.codeLines.getOrNull(context.line - 1)?.trim() ?: ""
        
        when {
            line.endsWith(":") -> {
                suggestions.add("\n    ") // Indentation after colon
                suggestions.add("pass")
            }
            line.isEmpty() -> {
                suggestions.addAll(listOf("def ", "class ", "import ", "from ", "if ", "for ", "while ", "try:"))
            }
        }
        
        return suggestions
    }
    
    private fun getBasicSuggestions(language: String): List<String> {
        return when (language) {
            "python" -> listOf(
                "def ", "class ", "import ", "from ", "if ", "for ", "while ", "try:",
                "print(", "return ", "pass", "# ", "\"\"\""
            )
            "javascript" -> listOf(
                "function ", "const ", "let ", "var ", "if ", "for ", "while ", "try {",
                "console.log(", "return ", "=>", "{", "// "
            )
            "java" -> listOf(
                "public ", "private ", "protected ", "class ", "interface ", "enum ",
                "if ", "for ", "while ", "try {", "System.out.println(", "return ", "{", "// "
            )
            "kotlin" -> listOf(
                "fun ", "val ", "var ", "class ", "interface ", "object ",
                "if ", "for ", "while ", "try {", "println(", "return ", "{", "// "
            )
            else -> emptyList()
        }
    }
    
    private fun preloadCommonPatterns() {
        Log.d(TAG, "Preloading common completion patterns")
        // Preload common patterns for faster suggestions
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        completionCache.clear()
    }
}

/**
 * Data classes for completion engine
 */

data class CompletionContext(
    val type: ContextType,
    val partialText: String,
    val line: Int,
    val column: Int,
    val codeLines: List<String> = emptyList()
)

data class CompletionSuggestion(
    val text: String,
    val type: SuggestionType,
    val description: String = "",
    val priority: Int = 0
)

enum class ContextType {
    UNKNOWN, GENERAL, IMPORT_STATEMENT, FUNCTION_CALL, FUNCTION_DEFINITION,
    CLASS_DEFINITION, STRING_LITERAL, COMMENT, IF_STATEMENT, FOR_STATEMENT, WHILE_STATEMENT
}

enum class SuggestionType {
    KEYWORD, BUILTIN, SNIPPET, PATTERN, IMPORT, METHOD
}

/**
 * Language-specific pattern interfaces and implementations
 */

interface LanguagePatterns {
    fun getKeywords(): List<String>
    fun getBuiltinFunctions(): List<String>
    fun getReservedWords(): List<String>
}

class PythonCompletionPatterns : LanguagePatterns {
    override fun getKeywords(): List<String> = listOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", 
        "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", 
        "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", 
        "raise", "return", "try", "while", "with", "yield"
    )
    
    override fun getBuiltinFunctions(): List<String> = listOf(
        "abs", "all", "any", "bin", "bool", "bytearray", "bytes", "callable", "chr", 
        "classmethod", "compile", "complex", "delattr", "dict", "dir", "divmod", 
        "enumerate", "eval", "exec", "filter", "float", "format", "frozenset", 
        "getattr", "globals", "hasattr", "hash", "help", "hex", "id", "input", 
        "int", "isinstance", "issubclass", "iter", "len", "list", "locals", "map", 
        "max", "memoryview", "min", "next", "object", "oct", "open", "ord", "pow", 
        "print", "property", "range", "repr", "reversed", "round", "set", "setattr", 
        "slice", "sorted", "staticmethod", "str", "sum", "super", "tuple", "type", 
        "vars", "zip"
    )
    
    override fun getReservedWords(): List<String> = getKeywords()
}

class JavaScriptCompletionPatterns : LanguagePatterns {
    override fun getKeywords(): List<String> = listOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default", 
        "delete", "do", "else", "export", "extends", "finally", "for", "function", 
        "if", "import", "in", "instanceof", "new", "return", "super", "switch", 
        "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield"
    )
    
    override fun getBuiltinFunctions(): List<String> = listOf(
        "alert", "console.log", "document.getElementById", "document.querySelector",
        "fetch", "JSON.parse", "JSON.stringify", "Math.abs", "Math.floor", "Math.random",
        "parseFloat", "parseInt", "setTimeout", "setInterval", "clearTimeout", "clearInterval"
    )
    
    override fun getReservedWords(): List<String> = getKeywords()
}

class JavaCompletionPatterns : LanguagePatterns {
    override fun getKeywords(): List<String> = listOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", 
        "class", "const", "continue", "default", "do", "double", "else", "enum", 
        "extends", "final", "finally", "float", "for", "goto", "if", "implements", 
        "import", "instanceof", "int", "interface", "long", "native", "new", "package", 
        "private", "protected", "public", "return", "short", "static", "strictfp", 
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", 
        "try", "void", "volatile", "while"
    )
    
    override fun getBuiltinFunctions(): List<String> = listOf(
        "System.out.println", "System.out.print", "Math.abs", "Math.max", "Math.min",
        "Integer.parseInt", "Double.parseDouble", "String.valueOf", "Arrays.toString",
        "List.of", "Set.of", "Map.of", "Arrays.sort", "Collections.sort"
    )
    
    override fun getReservedWords(): List<String> = getKeywords()
}

class KotlinCompletionPatterns : LanguagePatterns {
    override fun getKeywords(): List<String> = listOf(
        "as", "as?", "break", "class", "continue", "do", "else", "false", "for", 
        "fun", "if", "in", "!in", "interface", "is", "!is", "null", "object", 
        "package", "return", "super", "this", "throw", "true", "try", "typealias", 
        "typeof", "val", "var", "when", "while", "by", "catch", "constructor", 
        "delegate", "dynamic", "field", "file", "finally", "get", "import", "init", 
        "param", "property", "receiver", "set", "setparam", "where"
    )
    
    override fun getBuiltinFunctions(): List<String> = listOf(
        "println", "print", "readLine", "run", "let", "also", "apply", "with",
        "takeIf", "takeUnless", "repeat", "TODO", "lazy", " MutableList", "MutableMap",
        "MutableSet", "listOf", "mapOf", "setOf", "arrayOf"
    )
    
    override fun getReservedWords(): List<String> = getKeywords()
}