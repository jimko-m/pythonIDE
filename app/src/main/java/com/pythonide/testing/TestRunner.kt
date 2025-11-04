package com.pythonide.testing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * Manages test execution and coordination with testing framework
 * Handles running individual tests, test suites, and collecting results
 */
class TestRunner(
    private val context: Context,
    private val testFramework: TestFramework
) {
    companion object {
        private const val TAG = "TestRunner"
        private val TEST_PATTERN = Pattern.compile("^test_.*", Pattern.CASE_INSENSITIVE)
    }

    private val _activeTestRuns = ConcurrentHashMap<String, TestRun>()
    private val _testResults = ConcurrentHashMap<String, TestResult>()
    private val _testListeners = mutableListOf<TestListener>()
    private val _runCounter = AtomicInteger(0)
    
    /**
     * Run a single test
     */
    suspend fun runSingleTest(
        testPath: String,
        testName: String,
        parameters: Map<String, Any> = emptyMap()
    ): TestRunResult = withContext(Dispatchers.IO) {
        try {
            val runId = generateRunId()
            val testRun = TestRun(
                id = runId,
                type = TestRunType.SINGLE_TEST,
                testPath = testPath,
                testName = testName,
                parameters = parameters,
                status = TestRunStatus.QUEUED,
                startTime = System.currentTimeMillis()
            )
            
            _activeTestRuns[runId] = testRun
            notifyListeners { onTestRunStarted(testRun) }
            
            // Load test module
            val testModule = testFramework.loadTestModule(testPath)
            if (testModule == null) {
                return@withContext TestRunResult.Error("Failed to load test module: $testPath")
            }
            
            // Find and run the test
            val testResult = testFramework.runSingleTest(testModule, testName, testRun)
            
            testRun.status = TestRunStatus.COMPLETED
            testRun.endTime = System.currentTimeMillis()
            testRun.result = testResult
            
            _testResults[runId] = testResult
            notifyListeners { onTestRunCompleted(testRun) }
            
            TestRunResult.Success(testRun)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running single test", e)
            TestRunResult.Error("Test execution failed: ${e.message}")
        } finally {
            testPath.let { _activeTestRuns.remove(it) }
        }
    }

    /**
     * Run all tests in a file
     */
    suspend fun runTestFile(
        filePath: String,
        pattern: String? = null,
        parameters: Map<String, Any> = emptyMap()
    ): TestRunResult = withContext(Dispatchers.IO) {
        try {
            val runId = generateRunId()
            val testRun = TestRun(
                id = runId,
                type = TestRunType.TEST_FILE,
                testPath = filePath,
                parameters = parameters,
                status = TestRunStatus.QUEUED,
                startTime = System.currentTimeMillis()
            )
            
            _activeTestRuns[runId] = testRun
            notifyListeners { onTestRunStarted(testRun) }
            
            // Load test file and find all test methods
            val testMethods = findTestMethods(filePath, pattern)
            
            if (testMethods.isEmpty()) {
                val errorResult = TestResult(
                    testName = "File: $filePath",
                    status = TestStatus.SKIPPED,
                    error = "No test methods found",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis()
                )
                testRun.status = TestRunStatus.COMPLETED
                testRun.endTime = System.currentTimeMillis()
                testRun.result = errorResult
                return@withContext TestRunResult.Success(testRun)
            }
            
            // Run tests sequentially or in parallel
            val results = mutableListOf<TestResult>()
            
            for (testMethod in testMethods) {
                try {
                    val singleResult = runSingleTest(filePath, testMethod, parameters)
                    when (singleResult) {
                        is TestRunResult.Success -> results.add(singleResult.testRun.result)
                        is TestRunResult.Error -> {
                            results.add(TestResult(
                                testName = testMethod,
                                status = TestStatus.ERROR,
                                error = singleResult.message,
                                startTime = System.currentTimeMillis(),
                                endTime = System.currentTimeMillis()
                            ))
                        }
                    }
                } catch (e: Exception) {
                    results.add(TestResult(
                        testName = testMethod,
                        status = TestStatus.ERROR,
                        error = e.message,
                        startTime = System.currentTimeMillis(),
                        endTime = System.currentTimeMillis()
                    ))
                }
            }
            
            // Aggregate results
            val aggregatedResult = aggregateResults(filePath, results)
            
            testRun.status = TestRunStatus.COMPLETED
            testRun.endTime = System.currentTimeMillis()
            testRun.result = aggregatedResult
            
            notifyListeners { onTestRunCompleted(testRun) }
            
            TestRunResult.Success(testRun)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running test file", e)
            TestRunResult.Error("Test file execution failed: ${e.message}")
        } finally {
            filePath.let { _activeTestRuns.remove(it) }
        }
    }

    /**
     * Run all tests in a directory
     */
    suspend fun runTestDirectory(
        directoryPath: String,
        recursive: Boolean = true,
        pattern: String? = null,
        excludePattern: String? = null
    ): TestRunResult = withContext(Dispatchers.IO) {
        try {
            val runId = generateRunId()
            val testRun = TestRun(
                id = runId,
                type = TestRunType.TEST_DIRECTORY,
                testPath = directoryPath,
                status = TestRunStatus.QUEUED,
                startTime = System.currentTimeMillis()
            )
            
            _activeTestRuns[runId] = testRun
            notifyListeners { onTestRunStarted(testRun) }
            
            // Find all test files in directory
            val testFiles = findTestFiles(directoryPath, recursive, excludePattern)
            
            if (testFiles.isEmpty()) {
                val errorResult = TestResult(
                    testName = "Directory: $directoryPath",
                    status = TestStatus.SKIPPED,
                    error = "No test files found",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis()
                )
                testRun.status = TestRunStatus.COMPLETED
                testRun.endTime = System.currentTimeMillis()
                testRun.result = errorResult
                return@withContext TestRunResult.Success(testRun)
            }
            
            // Run test files
            val results = mutableListOf<TestResult>()
            
            for (testFile in testFiles) {
                try {
                    val fileResult = runTestFile(testFile, pattern)
                    when (fileResult) {
                        is TestRunResult.Success -> results.add(fileResult.testRun.result)
                        is TestRunResult.Error -> {
                            results.add(TestResult(
                                testName = "File: $testFile",
                                status = TestStatus.ERROR,
                                error = fileResult.message,
                                startTime = System.currentTimeMillis(),
                                endTime = System.currentTimeMillis()
                            ))
                        }
                    }
                } catch (e: Exception) {
                    results.add(TestResult(
                        testName = "File: $testFile",
                        status = TestStatus.ERROR,
                        error = e.message,
                        startTime = System.currentTimeMillis(),
                        endTime = System.currentTimeMillis()
                    ))
                }
            }
            
            // Aggregate results
            val aggregatedResult = aggregateResults(directoryPath, results)
            
            testRun.status = TestRunStatus.COMPLETED
            testRun.endTime = System.currentTimeMillis()
            testRun.result = aggregatedResult
            
            notifyListeners { onTestRunCompleted(testRun) }
            
            TestRunResult.Success(testRun)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running test directory", e)
            TestRunResult.Error("Test directory execution failed: ${e.message}")
        } finally {
            directoryPath.let { _activeTestRuns.remove(it) }
        }
    }

    /**
     * Run tests with specific tags/filters
     */
    suspend fun runTestsWithFilter(
        filter: TestFilter,
        searchPath: String
    ): TestRunResult = withContext(Dispatchers.IO) {
        try {
            val runId = generateRunId()
            val testRun = TestRun(
                id = runId,
                type = TestRunType.FILTERED,
                testPath = searchPath,
                status = TestRunStatus.QUEUED,
                startTime = System.currentTimeMillis()
            )
            
            _activeTestRuns[runId] = testRun
            notifyListeners { onTestRunStarted(testRun) }
            
            // Find tests matching the filter
            val matchingTests = findMatchingTests(searchPath, filter)
            
            if (matchingTests.isEmpty()) {
                val errorResult = TestResult(
                    testName = "Filtered tests in: $searchPath",
                    status = TestStatus.SKIPPED,
                    error = "No tests matching filter: $filter",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis()
                )
                testRun.status = TestRunStatus.COMPLETED
                testRun.endTime = System.currentTimeMillis()
                testRun.result = errorResult
                return@withContext TestRunResult.Success(testRun)
            }
            
            // Run matching tests
            val results = mutableListOf<TestResult>()
            
            for (testInfo in matchingTests) {
                try {
                    val testResult = runSingleTest(testInfo.filePath, testInfo.testName)
                    when (testResult) {
                        is TestRunResult.Success -> results.add(testResult.testRun.result)
                        is TestRunResult.Error -> {
                            results.add(TestResult(
                                testName = testInfo.testName,
                                status = TestStatus.ERROR,
                                error = testResult.message,
                                startTime = System.currentTimeMillis(),
                                endTime = System.currentTimeMillis()
                            ))
                        }
                    }
                } catch (e: Exception) {
                    results.add(TestResult(
                        testName = testInfo.testName,
                        status = TestStatus.ERROR,
                        error = e.message,
                        startTime = System.currentTimeMillis(),
                        endTime = System.currentTimeMillis()
                    ))
                }
            }
            
            // Aggregate results
            val aggregatedResult = aggregateResults("Filtered tests", results)
            
            testRun.status = TestRunStatus.COMPLETED
            testRun.endTime = System.currentTimeMillis()
            testRun.result = aggregatedResult
            
            notifyListeners { onTestRunCompleted(testRun) }
            
            TestRunResult.Success(testRun)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running filtered tests", e)
            TestRunResult.Error("Filtered test execution failed: ${e.message}")
        } finally {
            searchPath.let { _activeTestRuns.remove(it) }
        }
    }

    /**
     * Cancel a running test
     */
    suspend fun cancelTestRun(runId: String): Boolean = withContext(Dispatchers.IO) {
        _activeTestRuns[runId]?.let { testRun ->
            testRun.status = TestRunStatus.CANCELLED
            testRun.endTime = System.currentTimeMillis()
            
            notifyListeners { onTestRunCancelled(testRun) }
            _activeTestRuns.remove(runId)
            
            Log.d(TAG, "Cancelled test run: $runId")
            true
        } ?: false
    }

    /**
     * Get test run status
     */
    fun getTestRun(runId: String): TestRun? = _activeTestRuns[runId]

    /**
     * Get all active test runs
     */
    fun getActiveTestRuns(): List<TestRun> = _activeTestRuns.values.toList()

    /**
     * Get test results history
     */
    fun getTestResults(): List<TestResult> = _testResults.values.toList().sortedByDescending { it.startTime }

    /**
     * Find test methods in a file
     */
    private fun findTestMethods(filePath: String, pattern: String?): List<String> {
        return try {
            val content = java.io.File(filePath).readText()
            val lines = content.lines()
            val methods = mutableListOf<String>()
            
            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("def ") && trimmed.contains("(")) {
                    val methodName = trimmed.substringAfter("def ").substringBefore("(")
                    if (isTestMethod(methodName, pattern)) {
                        methods.add(methodName)
                    }
                }
            }
            
            methods
        } catch (e: Exception) {
            Log.e(TAG, "Error finding test methods in $filePath", e)
            emptyList()
        }
    }

    /**
     * Find test files in directory
     */
    private fun findTestFiles(
        directoryPath: String,
        recursive: Boolean,
        excludePattern: String?
    ): List<String> {
        return try {
            val directory = java.io.File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                return emptyList()
            }
            
            val testFiles = mutableListOf<String>()
            val excludeRegex = excludePattern?.let { Regex(it, RegexOption.IGNORE_CASE) }
            
            fun searchDirectory(dir: java.io.File) {
                val files = dir.listFiles() ?: return
                
                for (file in files) {
                    when {
                        file.isDirectory && recursive -> {
                            searchDirectory(file)
                        }
                        file.isFile && file.name.endsWith(".py") -> {
                            val relativePath = file.absolutePath
                            if (excludeRegex == null || !relativePath.contains(excludeRegex)) {
                                testFiles.add(relativePath)
                            }
                        }
                    }
                }
            }
            
            searchDirectory(directory)
            testFiles
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding test files in $directoryPath", e)
            emptyList()
        }
    }

    /**
     * Find tests matching a filter
     */
    private fun findMatchingTests(searchPath: String, filter: TestFilter): List<TestInfo> {
        val matchingTests = mutableListOf<TestInfo>()
        
        val files = when {
            java.io.File(searchPath).isFile -> listOf(searchPath)
            java.io.File(searchPath).isDirectory -> findTestFiles(searchPath, true, null)
            else -> emptyList()
        }
        
        for (filePath in files) {
            val methods = findTestMethods(filePath, filter.testNamePattern)
            
            for (method in methods) {
                if (matchesFilter(method, filePath, filter)) {
                    matchingTests.add(TestInfo(filePath, method))
                }
            }
        }
        
        return matchingTests
    }

    /**
     * Check if a method name is a test method
     */
    private fun isTestMethod(methodName: String, pattern: String?): Boolean {
        return when {
            pattern != null -> methodName.matches(Regex(pattern, RegexOption.IGNORE_CASE))
            TEST_PATTERN.matcher(methodName).matches() -> true
            else -> false
        }
    }

    /**
     * Check if test matches filter criteria
     */
    private fun matchesFilter(testName: String, filePath: String, filter: TestFilter): Boolean {
        // Check test name pattern
        filter.testNamePattern?.let { pattern ->
            if (!testName.matches(Regex(pattern, RegexOption.IGNORE_CASE))) {
                return false
            }
        }
        
        // Check file path pattern
        filter.filePathPattern?.let { pattern ->
            if (!filePath.matches(Regex(pattern, RegexOption.IGNORE_CASE))) {
                return false
            }
        }
        
        // Check tags
        filter.requiredTags?.let { tags ->
            val fileTags = getFileTags(filePath)
            if (!tags.all { fileTags.contains(it) }) {
                return false
            }
        }
        
        return true
    }

    /**
     * Get tags from test file
     */
    private fun getFileTags(filePath: String): Set<String> {
        // Simplified implementation - would parse docstrings or decorators for tags
        return emptySet()
    }

    /**
     * Aggregate test results
     */
    private fun aggregateResults(testName: String, results: List<TestResult>): TestResult {
        val total = results.size
        val passed = results.count { it.status == TestStatus.PASSED }
        val failed = results.count { it.status == TestStatus.FAILED }
        val errors = results.count { it.status == TestStatus.ERROR }
        val skipped = results.count { it.status == TestStatus.SKIPPED }
        
        val overallStatus = when {
            errors > 0 -> TestStatus.ERROR
            failed > 0 -> TestStatus.FAILED
            skipped == total -> TestStatus.SKIPPED
            passed == total -> TestStatus.PASSED
            else -> TestStatus.PASSED // Partial success
        }
        
        val output = buildString {
            appendLine("=== Test Results for $testName ===")
            appendLine("Total: $total, Passed: $passed, Failed: $failed, Errors: $errors, Skipped: $skipped")
            appendLine()
            
            results.forEach { result ->
                appendLine("${result.status}: ${result.testName}")
                result.error?.let { appendLine("  Error: $it") }
                result.output?.let { appendLine("  Output: $it") }
                appendLine()
            }
        }
        
        return TestResult(
            testName = testName,
            status = overallStatus,
            startTime = results.minOfOrNull { it.startTime } ?: System.currentTimeMillis(),
            endTime = results.maxOfOrNull { it.endTime } ?: System.currentTimeMillis(),
            output = output,
            metadata = mapOf(
                "total" to total,
                "passed" to passed,
                "failed" to failed,
                "errors" to errors,
                "skipped" to skipped
            )
        )
    }

    /**
     * Generate unique run ID
     */
    private fun generateRunId(): String = "run_${_runCounter.incrementAndGet()}"

    /**
     * Add test listener
     */
    fun addTestListener(listener: TestListener) {
        _testListeners.add(listener)
    }

    /**
     * Remove test listener
     */
    fun removeTestListener(listener: TestListener) {
        _testListeners.remove(listener)
    }

    /**
     * Notify all listeners
     */
    private fun notifyListeners(block: TestListener.() -> Unit) {
        _testListeners.forEach { it.block() }
    }
}

/**
 * Data classes for test running
 */
data class TestRun(
    val id: String,
    val type: TestRunType,
    val testPath: String,
    val testName: String? = null,
    val parameters: Map<String, Any> = emptyMap(),
    var status: TestRunStatus,
    val startTime: Long,
    var endTime: Long? = null,
    var result: TestResult? = null
)

data class TestResult(
    val testName: String,
    val status: TestStatus,
    val startTime: Long,
    val endTime: Long,
    val error: String? = null,
    val output: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

data class TestInfo(
    val filePath: String,
    val testName: String
)

sealed class TestRunResult {
    data class Success(val testRun: TestRun) : TestRunResult()
    data class Error(val message: String) : TestRunResult()
}

enum class TestRunType {
    SINGLE_TEST,
    TEST_FILE,
    TEST_DIRECTORY,
    FILTERED
}

enum class TestRunStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    CANCELLED,
    ERROR
}

enum class TestStatus {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED
}

data class TestFilter(
    val testNamePattern: String? = null,
    val filePathPattern: String? = null,
    val requiredTags: Set<String>? = null
)

/**
 * Test listener interface
 */
interface TestListener {
    fun onTestRunStarted(testRun: TestRun) {}
    fun onTestRunCompleted(testRun: TestRun) {}
    fun onTestRunCancelled(testRun: TestRun) {}
    fun onTestProgress(testRun: TestRun, progress: TestProgress) {}
    fun onTestOutput(testRun: TestRun, output: String) {}
    fun onTestError(testRun: TestRun, error: String) {}
}

data class TestProgress(
    val completed: Int,
    val total: Int,
    val passed: Int,
    val failed: Int,
    val errors: Int
)