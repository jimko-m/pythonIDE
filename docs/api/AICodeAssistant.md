# AICodeAssistant API Documentation

## Overview

The `AICodeAssistant` class provides AI-powered development assistance including code completion, error detection, formatting, and optimization suggestions.

## Class Definition

```kotlin
class AICodeAssistant(
    private val completionEngine: CodeCompletionEngine,
    private val errorDetector: ErrorDetector,
    private val smartFormatter: SmartFormatter
) {
    // AI assistant implementation
}
```

## Constructor Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `completionEngine` | `CodeCompletionEngine` | Engine responsible for code completion generation |
| `errorDetector` | `ErrorDetector` | Service for detecting syntax and logic errors |
| `smartFormatter` | `SmartFormatter` | Code formatting and style optimization service |

## Public Methods

### Code Completion

#### `suggestCodeCompletion(codeContext: String): String?`

Generates AI-powered code completion suggestions based on the current code context.

**Parameters:**
- `codeContext: String` - The current code context for completion

**Returns:** `String?` - Generated completion suggestion or null if no suggestion available

**Example:**
```kotlin
val aiAssistant = AICodeAssistant(completionEngine, errorDetector, formatter)

val context = "def hello_world():"
val suggestion = aiAssistant.suggestCodeCompletion(context)
// Returns: "    return \"Hello, World!\""
```

#### `getIntelligentSuggestions(partialCode: String, context: String): List<String>`

Provides multiple intelligent suggestions for code completion.

**Parameters:**
- `partialCode: String` - The partial code entered by the user
- `context: String` - Additional context (e.g., "web scraping", "data analysis")

**Returns:** `List<String>` - List of intelligent suggestions

**Example:**
```kotlin
val suggestions = aiAssistant.getIntelligentSuggestions("import requests", "web scraping")
// Returns: ["import requests_oauthlib", "import requests-cache", "from requests import get"]
```

### Error Detection

#### `detectErrors(code: String): List<ErrorInfo>`

Detects syntax and logic errors in Python code.

**Parameters:**
- `code: String` - The Python code to analyze

**Returns:** `List<ErrorInfo>` - List of detected errors

**Example:**
```kotlin
val code = """
def function():
print("Missing indentation")
"""

val errors = aiAssistant.detectErrors(code)
// Returns: [ErrorInfo("Expected indentation", Line 2, "print")]
```

#### `fixError(code: String, errorIndex: Int): String?`

Attempts to automatically fix a detected error.

**Parameters:**
- `code: String` - The original code
- `errorIndex: Int` - Index of the error to fix

**Returns:** `String?` - Fixed code or null if error cannot be auto-fixed

**Example:**
```kotlin
val fixedCode = aiAssistant.fixError(code, 0)
// Returns: "def function():\n    print(\"Fixed indentation\")"
```

### Code Formatting

#### `formatCode(code: String): String`

Formats Python code according to PEP 8 and other style guidelines.

**Parameters:**
- `code: String` - The code to format

**Returns:** `String` - Formatted code

**Example:**
```kotlin
val messyCode = "def  test(   a,b   ):\nprint(a,b)"

val formatted = aiAssistant.formatCode(messyCode)
// Returns: "def test(a, b):\n    print(a, b)"
```

#### `optimizeCode(code: String): OptimizationResult`

Analyzes and optimizes code for better performance.

**Parameters:**
- `code: String` - The code to optimize

**Returns:** `OptimizationResult` - Optimization analysis and suggestions

**Example:**
```kotlin
val inefficientCode = """
result = []
for i in range(1000):
    result.append(i * 2)
"""

val optimization = aiAssistant.optimizeCode(inefficientCode)
// Returns: OptimizationResult with suggestions for list comprehension
```

### Documentation Generation

#### `generateDocumentation(code: String): String`

Generates comprehensive documentation for Python functions and classes.

**Parameters:**
- `code: String` - Python function or class code

**Returns:** `String` - Generated documentation

**Example:**
```kotlin
val functionCode = """
def calculate_area(radius, height):
    return 3.14159 * radius ** 2 * height
"""

val documentation = aiAssistant.generateDocumentation(functionCode)
// Returns: Docstring with parameter descriptions and return type
```

#### `generateDocstring(functionSignature: String): String`

Generates a docstring template for a function.

**Parameters:**
- `functionSignature: String` - Function signature line

**Returns:** `String` - Generated docstring template

**Example:**
```kotlin
val signature = "def process_data(data: List[str], filter_func: callable) -> Dict[str, Any]:"

val docstring = aiAssistant.generateDocstring(signature)
// Returns: Complete docstring template with Args and Returns sections
```

### Performance Analysis

#### `analyzePerformance(code: String): PerformanceAnalysis`

Analyzes code performance and provides optimization suggestions.

**Parameters:**
- `code: String` - The code to analyze

**Returns:** `PerformanceAnalysis` - Detailed performance analysis

**Example:**
```kotlin
val analysis = aiAssistant.analyzePerformance(code)
// Returns: PerformanceAnalysis with complexity ratings and suggestions
```

### Code Intelligence

#### `refactorCode(code: String, refactoringType: RefactoringType): RefactoringResult`

Suggests and applies code refactoring improvements.

**Parameters:**
- `code: String` - The code to refactor
- `refactoringType: RefactoringType` - Type of refactoring to apply

**Returns:** `RefactoringResult` - Refactoring suggestions and implementation

**Example:**
```kotlin
val refactoring = aiAssistant.refactorCode(code, RefactoringType.EXTRACT_METHOD)
// Returns: RefactoringResult with extracted method suggestions
```

## Data Classes

### `ErrorInfo`

Represents a detected error in code.

```kotlin
data class ErrorInfo(
    val message: String,
    val line: Int,
    val column: Int,
    val severity: ErrorSeverity,
    val fixSuggestion: String? = null
)
```

### `OptimizationResult`

Result of code optimization analysis.

```kotlin
data class OptimizationResult(
    val suggestions: List<OptimizationSuggestion>,
    val performanceGain: Double,
    val complexityReduction: Double
)
```

### `PerformanceAnalysis`

Comprehensive performance analysis result.

```kotlin
data class PerformanceAnalysis(
    val timeComplexity: String,
    val spaceComplexity: String,
    val bottleneck: String?,
    val optimizations: List<Optimization>
)
```

### `RefactoringResult`

Code refactoring analysis and suggestions.

```kotlin
data class RefactoringResult(
    val originalCode: String,
    val refactoredCode: String?,
    val benefits: List<String>,
    val warnings: List<String>
)
```

## Enums

### `ErrorSeverity`

Represents the severity level of detected errors.

```kotlin
enum class ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}
```

### `RefactoringType`

Types of available refactoring operations.

```kotlin
enum class RefactoringType {
    EXTRACT_METHOD,
    EXTRACT_VARIABLE,
    RENAME,
    SIMPLIFY_CONDITION,
    OPTIMIZE_LOOP,
    REMOVE_DEAD_CODE
}
```

## Usage Examples

### Basic Setup

```kotlin
// Initialize AI assistant with required dependencies
val completionEngine = CodeCompletionEngine()
val errorDetector = ErrorDetector()
val smartFormatter = SmartFormatter()

val aiAssistant = AICodeAssistant(
    completionEngine = completionEngine,
    errorDetector = errorDetector,
    smartFormatter = smartFormatter
)
```

### Real-time Code Assistance

```kotlin
class CodeEditorActivity : AppCompatActivity() {
    
    private lateinit var aiAssistant: AICodeAssistant
    private lateinit var codeEditText: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_editor)
        
        initializeAIAssistant()
        setupCodeEditing()
    }
    
    private fun initializeAIAssistant() {
        aiAssistant = AICodeAssistant(
            completionEngine = CodeCompletionEngine(),
            errorDetector = ErrorDetector(),
            smartFormatter = SmartFormatter()
        )
    }
    
    private fun setupCodeEditing() {
        codeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s?.toString() ?: ""
                
                // Detect errors in real-time
                val errors = aiAssistant.detectErrors(currentText)
                displayErrors(errors)
                
                // Generate code completion
                if (currentText.isNotEmpty()) {
                    val completion = aiAssistant.suggestCodeCompletion(currentText)
                    if (completion != null) {
                        showCompletionSuggestion(completion)
                    }
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun formatCode() {
        val code = codeEditText.text.toString()
        val formatted = aiAssistant.formatCode(code)
        codeEditText.setText(formatted)
    }
    
    private fun optimizeCode() {
        val code = codeEditText.text.toString()
        val optimization = aiAssistant.optimizeCode(code)
        
        // Show optimization suggestions
        optimization.suggestions.forEach { suggestion ->
            showOptimizationSuggestion(suggestion)
        }
    }
}
```

### Advanced Usage with Context

```kotlin
class ProjectAnalyzer {
    
    fun analyzeProjectFiles(files: List<File>): AnalysisReport {
        val aiAssistant = createAIAssistant()
        val fileAnalyses = files.mapNotNull { file ->
            if (file.extension == "py") {
                val code = file.readText()
                analyzeFile(file.name, code, aiAssistant)
            } else null
        }
        
        return AnalysisReport(fileAnalyses)
    }
    
    private fun analyzeFile(filename: String, code: String, aiAssistant: AICodeAssistant): FileAnalysis {
        return FileAnalysis(
            filename = filename,
            errors = aiAssistant.detectErrors(code),
            performanceAnalysis = aiAssistant.analyzePerformance(code),
            documentation = aiAssistant.generateDocumentation(code),
            optimizations = aiAssistant.optimizeCode(code)
        )
    }
}
```

## Error Handling

The AI Assistant includes robust error handling for various scenarios:

```kotlin
try {
    val suggestion = aiAssistant.suggestCodeCompletion(code)
    if (suggestion != null) {
        applySuggestion(suggestion)
    }
} catch (e: AIAssistantException) {
    when (e.type) {
        AIAssistantException.Type.NETWORK_ERROR -> {
            // Fallback to local completion
            fallbackCompletion(code)
        }
        AIAssistantException.Type.PROCESSING_ERROR -> {
            // Handle processing errors gracefully
            logError("AI processing failed", e)
        }
        AIAssistantException.Type.INVALID_INPUT -> {
            // Handle invalid input
            showErrorMessage("Invalid code context")
        }
    }
}
```

## Performance Considerations

- **Async Processing**: All AI operations are performed asynchronously to avoid blocking the UI thread
- **Caching**: Intelligent caching of completion suggestions and error detection results
- **Memory Management**: Proper cleanup of AI resources to prevent memory leaks
- **Rate Limiting**: Built-in rate limiting to prevent excessive API calls

## Threading

All AI operations are thread-safe and designed for use on the main UI thread. The class uses Kotlin coroutines for background processing:

```kotlin
// Example of async usage
suspend fun getCompletionAsync(codeContext: String): String? = withContext(Dispatchers.IO) {
    aiAssistant.suggestCodeCompletion(codeContext)
}
```

## Version Compatibility

- **Minimum API Level**: 24 (Android 7.0)
- **Recommended API Level**: 30+ for optimal performance
- **Dependencies**: 
  - Kotlin Coroutines 1.6+
  - AndroidX Core KTX 1.7+
  - ML Kit (for advanced AI features)

## See Also

- [CodeCompletionEngine API](./CodeCompletionEngine.md)
- [ErrorDetector API](./ErrorDetector.md)
- [SmartFormatter API](./SmartFormatter.md)
- [User Guide](../guides/AI_Assistant_Guide.md)