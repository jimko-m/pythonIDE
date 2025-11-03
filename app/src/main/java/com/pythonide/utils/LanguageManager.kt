package com.pythonide.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class LanguageManager(private val context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    
    fun setLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration()
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        context.createConfigurationContext(config)
        
        // Save to preferences
        preferences.edit()
            .putString("selected_language", languageCode)
            .apply()
            
        // Also save to global preferences
        saveGlobalLanguagePreference(context, languageCode)
    }
    
    fun getCurrentLanguage(context: Context): String {
        return preferences.getString("selected_language", getDefaultLanguage(context)) ?: getDefaultLanguage(context)
    }
    
    fun getDefaultLanguage(context: Context): String {
        // Get device default language
        val deviceLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when (deviceLocale.language) {
            "ar" -> "ar"
            "en" -> "en"
            else -> "en" // Default to English
        }
    }
    
    fun applySavedLanguage(context: Context) {
        val language = getCurrentLanguage(context)
        applyLanguage(context, language)
    }
    
    private fun applyLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration()
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    
    private fun saveGlobalLanguagePreference(context: Context, languageCode: String) {
        val globalPrefs = context.getSharedPreferences("python_ide_prefs", Context.MODE_PRIVATE)
        globalPrefs.edit()
            .putString("language", languageCode)
            .apply()
    }
    
    companion object {
        const val DEFAULT_LANGUAGE = "en"
        const val ARABIC_LANGUAGE = "ar"
        const val ENGLISH_LANGUAGE = "en"
    }
}