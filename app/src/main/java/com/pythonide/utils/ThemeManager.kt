package com.pythonide.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

/**
 * Manager for theme-related operations
 */
object ThemeManager {
    
    private const val THEME_PREFERENCE_KEY = "theme_preference"
    private const val THEME_AUTO = "auto"
    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"
    
    /**
     * Initialize theme manager
     */
    fun initialize(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val theme = sharedPrefs.getString(THEME_PREFERENCE_KEY, THEME_AUTO) ?: THEME_AUTO
        
        applyThemePreference(theme, context)
    }
    
    /**
     * Apply theme based on preference
     */
    fun applyTheme(activity: AppCompatActivity) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val theme = sharedPrefs.getString(THEME_PREFERENCE_KEY, THEME_AUTO) ?: THEME_AUTO
        
        applyThemePreference(theme, activity)
    }
    
    private fun applyThemePreference(theme: String, context: Context) {
        when (theme) {
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_AUTO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
    
    /**
     * Check if dark mode is active
     */
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        return when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                // Check system preference
                val isSystemDark = android.provider.Settings.Global.getString(
                    context.contentResolver,
                    android.provider.Settings.Global.UI_MODE
                )?.contains(android.content.res.Configuration.UI_MODE_NIGHT_YES) ?: false
                isSystemDark
            }
            else -> false
        }
    }
    
    /**
     * Set theme preference
     */
    fun setThemePreference(context: Context, theme: String) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit()
            .putString(THEME_PREFERENCE_KEY, theme)
            .apply()
        
        applyThemePreference(theme, context)
    }
    
    /**
     * Get current theme preference
     */
    fun getThemePreference(context: Context): String {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPrefs.getString(THEME_PREFERENCE_KEY, THEME_AUTO) ?: THEME_AUTO
    }
    
    /**
     * Toggle between light and dark theme
     */
    fun toggleTheme(context: Context) {
        val currentTheme = getThemePreference(context)
        val newTheme = when (currentTheme) {
            THEME_LIGHT -> THEME_DARK
            THEME_DARK -> THEME_LIGHT
            THEME_AUTO -> if (isDarkMode(context)) THEME_LIGHT else THEME_DARK
            else -> THEME_LIGHT
        }
        
        setThemePreference(context, newTheme)
    }
}