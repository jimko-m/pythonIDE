package com.pythonide.ai

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for AI Code Assistant functionality
 */
@RunWith(MockitoJUnitRunner::class)
class AICodeAssistantTest {

    @Mock
    private lateinit var completionEngine: CodeCompletionEngine

    @Mock
    private lateinit var errorDetector: ErrorDetector

    @Mock
    private lateinit var smartFormatter: SmartFormatter

    private lateinit var aiAssistant: AICodeAssistant

    @Before
    fun setUp() {
        aiAssistant = AICodeAssistant(completionEngine, errorDetector, smartFormatter)
    }

    @Test
    fun `test code completion suggestion`() {
        // Given
        val codeContext = "def hello():"
        val expectedSuggestion = "    return \"Hello, World!\""
        
        `when`(completionEngine.generateCompletion(codeContext))
            .thenReturn(expectedSuggestion)

        // When
        val result = aiAssistant.suggestCodeCompletion(codeContext)

        // Then
        assertNotNull(result)
        assertEquals(expectedSuggestion, result)
        verify(completionEngine).generateCompletion(codeContext)
    }

    @Test
    fun `test error detection`() {
        // Given
        val code = """
            def function():
            print("Missing indentation")
        """.trimIndent()
        
        val errors = listOf(
            ErrorDetector.SyntaxError(1, "Expected indentation", "print")
        )
        
        `when`(errorDetector.detectErrors(code)).thenReturn(errors)

        // When
        val result = aiAssistant.detectErrors(code)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Expected indentation", result[0].message)
    }

    @Test
    fun `test code formatting`() {
        // Given
        val messyCode = """
            def  test(   arg1,arg2   ):
            print(arg1,arg2)
        """.trimIndent()
        
        val expectedFormatted = """
            def test(arg1, arg2):
                print(arg1, arg2)
        """.trimIndent()
        
        `when`(smartFormatter.formatCode(messyCode))
            .thenReturn(expectedFormatted)

        // When
        val result = aiAssistant.formatCode(messyCode)

        // Then
        assertNotNull(result)
        assertEquals(expectedFormatted, result)
    }

    @Test
    fun `test intelligent suggestions`() {
        // Given
        val partialCode = "import requests"
        val context = "web scraping"

        // When
        val suggestions = aiAssistant.getIntelligentSuggestions(partialCode, context)

        // Then
        assertNotNull(suggestions)
        assertTrue(suggestions.isNotEmpty())
        // Verify suggestions are relevant to web scraping
    }

    @Test
    fun `test code optimization`() {
        // Given
        val inefficientCode = """
            result = []
            for i in range(1000):
                result.append(i * 2)
        """.trimIndent()
        
        val expectedOptimization = "result = [i * 2 for i in range(1000)]"

        // When
        val optimized = aiAssistant.optimizeCode(inefficientCode)

        // Then
        assertNotNull(optimized)
        assertTrue(optimized.contains("list comprehension"))
    }

    @Test
    fun `test documentation generation`() {
        // Given
        val functionCode = """
            def calculate_area(radius, height):
                return 3.14159 * radius ** 2 * height
        """.trimIndent()

        // When
        val documented = aiAssistant.generateDocumentation(functionCode)

        // Then
        assertNotNull(documented)
        assertTrue(documented.contains("\"\"\""))
        assertTrue(documented.contains("Calculate area of cylinder"))
    }

    @Test
    fun `test performance analysis`() {
        // Given
        val code = """
            import time
            start = time.time()
            # Some code to analyze
            time.sleep(0.1)
            end = time.time()
        """.trimIndent()

        // When
        val analysis = aiAssistant.analyzePerformance(code)

        // Then
        assertNotNull(analysis)
        assertTrue(analysis.contains("time complexity"))
        assertTrue(analysis.contains("execution time"))
    }
}