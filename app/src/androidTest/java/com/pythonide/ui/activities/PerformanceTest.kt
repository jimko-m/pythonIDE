package com.pythonide.profiling

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance test suite for measuring app performance and resource usage
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Clear any existing performance data
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("performance_metrics")
    }

    @Test
    fun testStartupTime() {
        // Test app startup performance
        val startupTime = measureTimeMillis {
            // Re-launch activity to measure startup
            activityRule.scenario.recreate()
        }
        
        // Verify startup time is acceptable (< 3 seconds)
        assert(startupTime < 3000) { "Startup time too slow: ${startupTime}ms" }
        
        println("App startup time: ${startupTime}ms")
    }

    @Test
    fun testEditorLargeFilePerformance() {
        // Test editor performance with large files
        
        val largeFileContent = buildString {
            repeat(10000) { line ->
                appendLine("def function_$line():")
                appendLine("    return $line * 2")
            }
        }
        
        val processingTime = measureTimeMillis {
            // Navigate to editor
            onView(withId(R.id.nav_editor)).perform(click())
            
            // Open new file
            onView(withId(R.id.fab_new_file)).perform(click())
            
            // Input large content
            onView(withId(R.id.edit_text_code))
                .perform(typeText(largeFileContent))
        }
        
        // Verify processing time is reasonable (< 5 seconds)
        assert(processingTime < 5000) { "Large file processing too slow: ${processingTime}ms" }
        
        println("Large file processing time: ${processingTime}ms")
    }

    @Test
    fun testMemoryUsage() {
        // Test memory usage during normal operations
        
        // Get initial memory usage
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform memory-intensive operations
        repeat(100) { iteration ->
            onView(withId(R.id.nav_editor)).perform(click())
            onView(withId(R.id.fab_new_file)).perform(click())
            onView(withId(R.id.edit_text_code))
                .perform(typeText("print('Test line $iteration')"))
            onView(withId(R.id.action_save)).perform(click())
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(1000)
        
        // Get final memory usage
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Verify memory increase is acceptable (< 50MB)
        assert(memoryIncrease < 50 * 1024 * 1024) { "Memory usage too high: ${memoryIncrease / (1024 * 1024)}MB" }
        
        println("Memory increase: ${memoryIncrease / (1024 * 1024)}MB")
    }

    @Test
    fun testSearchPerformance() {
        // Test search functionality performance
        
        // First, create some test files with searchable content
        val searchContent = buildString {
            repeat(1000) { i ->
                appendLine("const SEARCHABLE_TEXT_$i = 'This is searchable content number $i'")
            }
        }
        
        onView(withId(R.id.nav_editor)).perform(click())
        onView(withId(R.id.fab_new_file)).perform(click())
        onView(withId(R.id.edit_text_code)).perform(typeText(searchContent))
        onView(withId(R.id.action_save)).perform(click())
        
        // Test search performance
        val searchTime = measureTimeMillis {
            // Open search dialog
            onView(withId(R.id.action_search)).perform(click())
            
            // Perform search
            onView(withId(R.id.search_edit_text))
                .perform(typeText("SEARCHABLE_TEXT_500"))
            
            // Wait for search results
            onView(withId(R.id.search_results_list))
                .check(matches(isDisplayed()))
        }
        
        // Verify search performance (< 1 second)
        assert(searchTime < 1000) { "Search too slow: ${searchTime}ms" }
        
        println("Search performance: ${searchTime}ms")
    }

    @Test
    fun testFileOperationPerformance() {
        // Test file creation, editing, and saving performance
        
        val operations = listOf("create", "edit", "save", "rename", "delete")
        val performanceResults = mutableMapOf<String, Long>()
        
        for (operation in operations) {
            val opTime = measureTimeMillis {
                when (operation) {
                    "create" -> {
                        onView(withId(R.id.fab_new_file)).perform(click())
                        onView(withId(R.id.edit_text_code))
                            .perform(typeText("print('New file content')"))
                    }
                    "edit" -> {
                        onView(withId(R.id.edit_text_code))
                            .perform(clearText(), typeText("print('Edited content')"))
                    }
                    "save" -> {
                        onView(withId(R.id.action_save)).perform(click())
                    }
                    "rename" -> {
                        // Simulate file rename operation
                        onView(withId(R.id.action_menu)).perform(click())
                        onView(withText("Rename")).perform(click())
                    }
                    "delete" -> {
                        // Simulate file delete operation
                        onView(withId(R.id.action_menu)).perform(click())
                        onView(withText("Delete")).perform(click())
                    }
                }
            }
            performanceResults[operation] = opTime
        }
        
        // Verify all operations are fast enough
        performanceResults.forEach { (operation, time) ->
            assert(time < 2000) { "$operation operation too slow: ${time}ms" }
            println("$operation operation time: ${time}ms")
        }
    }

    @Test
    fun testBatteryUsage() {
        // Test battery usage during intensive operations
        
        val operations = listOf(
            { onView(withId(R.id.nav_terminal)).perform(click()) },
            { onView(withId(R.id.nav_files)).perform(click()) },
            { onView(withId(R.id.nav_editor)).perform(click()) }
        )
        
        val startTime = System.currentTimeMillis()
        
        // Perform operations in a loop to simulate usage
        repeat(50) {
            operations.forEach { operation ->
                operation()
                Thread.sleep(100)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Simulate battery level check (in real tests, would use actual battery API)
        val estimatedBatteryUsage = totalTime / (1000 * 60) // Minutes of operation
        
        println("Test duration: ${totalTime / 1000}s")
        println("Estimated battery usage: $estimatedBatteryUsage minutes of moderate use")
        
        // Verify test completed in reasonable time (< 5 minutes)
        assert(totalTime < 5 * 60 * 1000) { "Test took too long: ${totalTime / 1000}s" }
    }

    @Test
    fun testScrollingPerformance() {
        // Test scrolling performance in file lists and editor
        
        // Create a large file to test scrolling
        val largeContent = buildString {
            repeat(5000) { i ->
                appendLine("Line $i: This is a test line with some content for scrolling performance testing.")
            }
        }
        
        onView(withId(R.id.nav_editor)).perform(click())
        onView(withId(R.id.fab_new_file)).perform(click())
        onView(withId(R.id.edit_text_code)).perform(typeText(largeContent))
        
        val scrollTime = measureTimeMillis {
            // Test vertical scrolling in editor
            onView(withId(R.id.edit_text_code))
                .perform(swipeUp())
            Thread.sleep(100)
            onView(withId(R.id.edit_text_code))
                .perform(swipeDown())
        }
        
        // Verify scrolling is smooth (< 500ms)
        assert(scrollTime < 500) { "Scrolling too slow: ${scrollTime}ms" }
        
        println("Scrolling performance: ${scrollTime}ms")
    }

    @Test
    fun testConcurrentOperations() {
        // Test performance under concurrent operations
        
        val concurrentTime = measureTimeMillis {
            // Simulate concurrent operations
            val threads = mutableListOf<Thread>()
            
            repeat(5) { threadIndex ->
                val thread = Thread {
                    repeat(10) { operationIndex ->
                        onView(withId(R.id.nav_editor)).perform(click())
                        onView(withId(R.id.fab_new_file)).perform(click())
                        Thread.sleep(50)
                    }
                }
                threads.add(thread)
                thread.start()
            }
            
            // Wait for all threads to complete
            threads.forEach { it.join() }
        }
        
        // Verify concurrent operations complete in reasonable time
        assert(concurrentTime < 30000) { "Concurrent operations too slow: ${concurrentTime}ms" }
        
        println("Concurrent operations time: ${concurrentTime}ms")
    }
}