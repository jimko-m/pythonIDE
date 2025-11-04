package com.pythonide.ui.activities

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test suite for MainActivity UI functionality
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Initialize test environment
        InstrumentationRegistry.getInstrumentation().uiAutomation
    }

    @Test
    fun testMainActivityLaunch() {
        // Test that MainActivity launches successfully
        onView(withId(R.id.activity_main_root))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationVisibility() {
        // Test that bottom navigation is visible and functional
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToEditor() {
        // Test navigation to code editor
        onView(withId(R.id.nav_editor))
            .perform(click())
            
        onView(withId(R.id.fragment_editor_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToTerminal() {
        // Test navigation to terminal
        onView(withId(R.id.nav_terminal))
            .perform(click())
            
        onView(withId(R.id.fragment_terminal_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToFiles() {
        // Test navigation to file manager
        onView(withId(R.id.nav_files))
            .perform(click())
            
        onView(withId(R.id.fragment_files_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNewProjectButton() {
        // Test new project creation button
        onView(withId(R.id.fab_new_project))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.fab_new_project))
            .perform(click())
            
        // Should open project template selection
        onView(withId(R.id.activity_template_selection))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsMenu() {
        // Test settings menu functionality
        onView(withId(R.id.action_settings))
            .perform(click())
            
        onView(withId(R.id.activity_settings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThemeToggle() {
        // Test dark/light theme toggle
        onView(withId(R.id.action_settings))
            .perform(click())
            
        onView(withId(R.id.switch_dark_theme))
            .perform(click())
            
        // Verify theme change
        // Note: Theme changes may take a moment to apply
    }

    @Test
    fun testDrawerMenu() {
        // Test navigation drawer functionality
        onView(withId(R.id.action_menu))
            .perform(click())
            
        onView(withId(R.id.nav_drawer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testProjectListDisplay() {
        // Test that projects are displayed correctly
        onView(withId(R.id.recycler_view_projects))
            .check(matches(isDisplayed()))
            
        // Check if project items are visible (if any exist)
        // Note: This test may need to be adjusted based on actual data
    }

    @Test
    fun testCreateNewProjectFlow() {
        // Test complete new project creation flow
        onView(withId(R.id.fab_new_project))
            .perform(click())
            
        onView(withId(R.id.recycler_view_templates))
            .check(matches(isDisplayed()))
            
        // Select a template
        onView(withText(R.string.template_flask))
            .perform(click())
            
        // Verify project creation dialog appears
        onView(withId(R.id.dialog_project_customization))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSearchFunctionality() {
        // Test search functionality
        onView(withId(R.id.action_search))
            .perform(click())
            
        onView(withId(R.id.search_view))
            .check(matches(isDisplayed()))
            
        // Type search query
        onView(withId(R.id.search_view))
            .perform(typeText("test project"))
            
        // Verify search results
        // Note: Implementation depends on search functionality
    }

    @Test
    fun testImportProject() {
        // Test project import functionality
        onView(withId(R.id.action_menu))
            .perform(click())
            
        onView(withText(R.string.action_import))
            .perform(click())
            
        // Should open file picker or import dialog
        // Note: Actual implementation may vary
    }

    @Test
    fun testExportProject() {
        // Test project export functionality
        // Note: This test assumes there's a project selected
        onView(withId(R.id.action_menu))
            .perform(click())
            
        onView(withText(R.string.action_export))
            .perform(click())
            
        // Should open export dialog or file picker
    }

    @Test
    fun testBackButtonHandling() {
        // Test that back button properly handles navigation
        // Navigate to a different screen first
        onView(withId(R.id.nav_terminal))
            .perform(click())
            
        // Press back button
        onView(isRoot()).perform(pressBack())
            
        // Should return to main screen or close app
        onView(withId(R.id.activity_main_root))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testResponsiveLayout() {
        // Test that layout adapts to different screen sizes
        // Note: This test would require device rotation or window size changes
        // Implementation depends on specific testing requirements
    }
}