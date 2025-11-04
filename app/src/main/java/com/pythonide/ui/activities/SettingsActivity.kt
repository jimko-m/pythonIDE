package com.pythonide.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButton
import com.pythonide.R
import com.pythonide.utils.LanguageManager
import com.pythonide.utils.ThemeManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var languageToggleGroup: MaterialButtonToggleGroup
    private lateinit var themeToggleGroup: MaterialButtonToggleGroup
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnArabic: MaterialButton
    private lateinit var btnEnglish: MaterialButton
    private lateinit var btnThemeAuto: MaterialButton
    private lateinit var btnThemeLight: MaterialButton
    private lateinit var btnThemeDark: MaterialButton
    private lateinit var btnVersionInfo: MaterialButton
    private lateinit var btnRateApp: MaterialButton

    private val languageManager = LanguageManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        setupToolbar()
        setupListeners()
        loadCurrentSettings()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        languageToggleGroup = findViewById(R.id.language_toggle_group)
        themeToggleGroup = findViewById(R.id.theme_toggle_group)
        btnArabic = findViewById(R.id.btn_arabic)
        btnEnglish = findViewById(R.id.btn_english)
        btnThemeAuto = findViewById(R.id.btn_theme_auto)
        btnThemeLight = findViewById(R.id.btn_theme_light)
        btnThemeDark = findViewById(R.id.btn_theme_dark)
        btnVersionInfo = findViewById(R.id.btn_version_info)
        btnRateApp = findViewById(R.id.btn_rate_app)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupListeners() {
        // Language Selection
        languageToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_arabic -> {
                        setLanguage("ar")
                        updateLanguageButtons("ar")
                    }
                    R.id.btn_english -> {
                        setLanguage("en")
                        updateLanguageButtons("en")
                    }
                }
            }
        }

        // Theme Selection
        themeToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_theme_auto -> {
                        setTheme("auto")
                        applyTheme("auto")
                    }
                    R.id.btn_theme_light -> {
                        setTheme("light")
                        applyTheme("light")
                    }
                    R.id.btn_theme_dark -> {
                        setTheme("dark")
                        applyTheme("dark")
                    }
                }
            }
        }

        // Version Info
        btnVersionInfo.setOnClickListener {
            showVersionInfo()
        }

        // Rate App
        btnRateApp.setOnClickListener {
            showRateAppDialog()
        }
    }

    private fun loadCurrentSettings() {
        // Load current language
        val currentLanguage = languageManager.getCurrentLanguage(this)
        updateLanguageButtons(currentLanguage)

        // Load current theme
        val currentTheme = ThemeManager.getThemePreference(this)
        updateThemeButtons(currentTheme)
    }

    private fun setLanguage(languageCode: String) {
        languageManager.setLanguage(this, languageCode)
        
        // Show restart message
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.msg_info))
            .setMessage(getString(R.string.settings_app_restart_message))
            .setPositiveButton(getString(R.string.msg_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun updateLanguageButtons(languageCode: String) {
        when (languageCode) {
            "ar" -> {
                btnArabic.isChecked = true
                btnEnglish.isChecked = false
            }
            "en" -> {
                btnArabic.isChecked = false
                btnEnglish.isChecked = true
            }
        }
    }

    private fun setTheme(themeCode: String) {
        ThemeManager.setThemePreference(this, themeCode)
    }

    private fun updateThemeButtons(themeCode: String) {
        when (themeCode) {
            "auto" -> {
                btnThemeAuto.isChecked = true
                btnThemeLight.isChecked = false
                btnThemeDark.isChecked = false
            }
            "light" -> {
                btnThemeAuto.isChecked = false
                btnThemeLight.isChecked = true
                btnThemeDark.isChecked = false
            }
            "dark" -> {
                btnThemeAuto.isChecked = false
                btnThemeLight.isChecked = false
                btnThemeDark.isChecked = true
            }
        }
    }

    private fun applyTheme(themeCode: String) {
        ThemeManager.setThemePreference(this, themeCode)
        
        // Recreate activity to apply theme changes
        recreate()
    }

    private fun showVersionInfo() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_about_title))
            .setMessage("""
                App: ${getString(R.string.app_name)}
                Version: $versionName
                Build: $versionCode
                
                Integrated Python Development Environment
                
                Developer: MiniMax Agent
            """.trimIndent())
            .setPositiveButton(getString(R.string.msg_ok), null)
            .show()
    }

    private fun showRateAppDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_rate_app_title))
            .setMessage(getString(R.string.settings_rate_app_message))
            .setPositiveButton("⭐⭐⭐⭐⭐") { _, _ ->
                Toast.makeText(this, getString(R.string.settings_thank_you), Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(getString(R.string.msg_later)) { _, _ ->
                // Later action
            }
            .setNegativeButton(getString(R.string.msg_cancel)) { _, _ ->
                // Cancel action
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}