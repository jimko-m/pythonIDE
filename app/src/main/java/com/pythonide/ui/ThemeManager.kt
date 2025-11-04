package com.pythonide.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enhanced theme manager with extensive customization options
 * Supports dark/light themes, custom themes, and user preferences
 */
class ThemeManager(private val context: Context) {
    companion object {
        private const val TAG = "ThemeManager"
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CUSTOM_COLORS = "custom_colors"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_COLOR_BLIND_MODE = "color_blind_mode"
        
        // Default theme colors
        val DEFAULT_LIGHT_COLORS = ThemeColors(
            primary = Color.parseColor("#2196F3"),
            primaryVariant = Color.parseColor("#1976D2"),
            secondary = Color.parseColor("#FF5722"),
            background = Color.parseColor("#FAFAFA"),
            surface = Color.WHITE,
            error = Color.parseColor("#F44336"),
            onPrimary = Color.WHITE,
            onSecondary = Color.WHITE,
            onBackground = Color.parseColor("#212121"),
            onSurface = Color.parseColor("#212121")
        )
        
        val DEFAULT_DARK_COLORS = ThemeColors(
            primary = Color.parseColor("#64B5F6"),
            primaryVariant = Color.parseColor("#1976D2"),
            secondary = Color.parseColor("#FF7043"),
            background = Color.parseColor("#121212"),
            surface = Color.parseColor("#1E1E1E"),
            error = Color.parseColor("#F44336"),
            onPrimary = Color.parseColor("#000000"),
            onSecondary = Color.parseColor("#000000"),
            onBackground = Color.parseColor("#E1E1E1"),
            onSurface = Color.parseColor("#E1E1E1")
        )
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _currentTheme = MutableStateFlow(getCurrentThemeMode())
    val currentTheme: StateFlow<ThemeMode> = _currentTheme.asStateFlow()
    
    private val _customColors = MutableStateFlow(loadCustomColors())
    val customColors: StateFlow<ThemeColors> = _customColors.asStateFlow()
    
    private val _fontSize = MutableStateFlow(loadFontSize())
    val fontSize: StateFlow<FontSize> = _fontSize.asStateFlow()
    
    private val _animationSpeed = MutableStateFlow(loadAnimationSpeed())
    val animationSpeed: StateFlow<AnimationSpeed> = _animationSpeed.asStateFlow()
    
    private val _highContrast = MutableStateFlow(loadHighContrast())
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()
    
    private val _colorBlindMode = MutableStateFlow(loadColorBlindMode())
    val colorBlindMode: StateFlow<ColorBlindMode> = _colorBlindMode.asStateFlow()
    
    private var observers = mutableListOf<ThemeObserver>()

    /**
     * Theme color configuration
     */
    data class ThemeColors(
        val primary: Int,
        val primaryVariant: Int,
        val secondary: Int,
        val background: Int,
        val surface: Int,
        val error: Int,
        val onPrimary: Int,
        val onSecondary: Int,
        val onBackground: Int,
        val onSurface: Int
    )
    
    /**
     * Theme modes
     */
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM, CUSTOM
    }
    
    /**
     * Font size settings
     */
    enum class FontSize(val scale: Float) {
        VERY_SMALL(0.8f), SMALL(0.9f), NORMAL(1.0f), LARGE(1.1f), VERY_LARGE(1.2f), HUGE(1.3f)
    }
    
    /**
     * Animation speed settings
     */
    enum class AnimationSpeed(val multiplier: Float) {
        SLOW(0.5f), NORMAL(1.0f), FAST(1.5f), VERY_FAST(2.0f), DISABLED(0.0f)
    }
    
    /**
     * Color blind mode options
     */
    enum class ColorBlindMode {
        NONE, DEUTERANOPIA, PROTANOPIA, TRITANOPIA, MONOCHROMACY
    }
    
    /**
     * Theme observer interface
     */
    interface ThemeObserver {
        fun onThemeChanged(themeMode: ThemeMode, colors: ThemeColors)
        fun onColorsUpdated(colors: ThemeColors)
        fun onFontSizeChanged(fontSize: FontSize)
        fun onAnimationSpeedChanged(speed: AnimationSpeed)
        fun onAccessibilitySettingsChanged(highContrast: Boolean, colorBlindMode: ColorBlindMode)
    }

    /**
     * Initialize theme manager
     */
    init {
        applyCurrentTheme()
    }

    /**
     * Set theme mode
     */
    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
        _currentTheme.value = mode
        applyCurrentTheme()
        notifyThemeChanged()
        Log.d(TAG, "Theme mode changed to: $mode")
    }

    /**
     * Set custom colors
     */
    fun setCustomColors(colors: ThemeColors) {
        _customColors.value = colors
        saveCustomColors(colors)
        notifyColorsUpdated()
        Log.d(TAG, "Custom colors updated")
    }

    /**
     * Set font size
     */
    fun setFontSize(size: FontSize) {
        preferences.edit().putFloat(KEY_FONT_SIZE, size.scale).apply()
        _fontSize.value = size
        notifyFontSizeChanged()
        Log.d(TAG, "Font size changed to: $size")
    }

    /**
     * Set animation speed
     */
    fun setAnimationSpeed(speed: AnimationSpeed) {
        preferences.edit().putFloat(KEY_ANIMATION_SPEED, speed.multiplier).apply()
        _animationSpeed.value = speed
        notifyAnimationSpeedChanged()
        Log.d(TAG, "Animation speed changed to: $speed")
    }

    /**
     * Set high contrast mode
     */
    fun setHighContrast(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        _highContrast.value = enabled
        notifyAccessibilityChanged()
        Log.d(TAG, "High contrast: $enabled")
    }

    /**
     * Set color blind mode
     */
    fun setColorBlindMode(mode: ColorBlindMode) {
        preferences.edit().putString(KEY_COLOR_BLIND_MODE, mode.name).apply()
        _colorBlindMode.value = mode
        notifyAccessibilityChanged()
        Log.d(TAG, "Color blind mode: $mode")
    }

    /**
     * Get current theme colors based on mode and accessibility settings
     */
    fun getCurrentColors(): ThemeColors {
        val baseColors = when (_currentTheme.value) {
            ThemeMode.LIGHT -> DEFAULT_LIGHT_COLORS
            ThemeMode.DARK -> DEFAULT_DARK_COLORS
            ThemeMode.SYSTEM -> if (isSystemDarkMode()) DEFAULT_DARK_COLORS else DEFAULT_LIGHT_COLORS
            ThemeMode.CUSTOM -> _customColors.value
        }
        
        return if (_highContrast.value) {
            applyHighContrast(baseColors)
        } else if (_colorBlindMode.value != ColorBlindMode.NONE) {
            applyColorBlindCompensation(baseColors, _colorBlindMode.value)
        } else {
            baseColors
        }
    }

    /**
     * Check if system is in dark mode
     */
    fun isSystemDarkMode(): Boolean {
        return when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true -> context.resources.configuration.isNightModeActive
            false -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * Add theme observer
     */
    fun addObserver(observer: ThemeObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    /**
     * Remove theme observer
     */
    fun removeObserver(observer: ThemeObserver) {
        observers.remove(observer)
    }

    /**
     * Create custom theme presets
     */
    fun getThemePresets(): List<ThemePreset> {
        return listOf(
            ThemePreset("Default Light", DEFAULT_LIGHT_COLORS, ThemeMode.LIGHT),
            ThemePreset("Default Dark", DEFAULT_DARK_COLORS, ThemeMode.DARK),
            ThemePreset("High Contrast Dark", applyHighContrast(DEFAULT_DARK_COLORS), ThemeMode.CUSTOM),
            ThemePreset("Ocean Blue", ThemeColors(
                primary = Color.parseColor("#1976D2"),
                primaryVariant = Color.parseColor("#0D47A1"),
                secondary = Color.parseColor("#00ACC1"),
                background = Color.parseColor("#E3F2FD"),
                surface = Color.WHITE,
                error = Color.parseColor("#D32F2F"),
                onPrimary = Color.WHITE,
                onSecondary = Color.WHITE,
                onBackground = Color.parseColor("#0D47A1"),
                onSurface = Color.parseColor("#0D47A1")
            ), ThemeMode.CUSTOM),
            ThemePreset("Forest Green", ThemeColors(
                primary = Color.parseColor("#388E3C"),
                primaryVariant = Color.parseColor("#1B5E20"),
                secondary = Color.parseColor("#FF8F00"),
                background = Color.parseColor("#F1F8E9"),
                surface = Color.WHITE,
                error = Color.parseColor("#F44336"),
                onPrimary = Color.WHITE,
                onSecondary = Color.WHITE,
                onBackground = Color.parseColor("#1B5E20"),
                onSurface = Color.parseColor("#1B5E20")
            ), ThemeMode.CUSTOM)
        )
    }

    /**
     * Apply preset theme
     */
    fun applyThemePreset(preset: ThemePreset) {
        setThemeMode(preset.themeMode)
        if (preset.colors != getCurrentColors()) {
            setCustomColors(preset.colors)
        }
    }

    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        setThemeMode(ThemeMode.SYSTEM)
        setCustomColors(DEFAULT_LIGHT_COLORS)
        setFontSize(FontSize.NORMAL)
        setAnimationSpeed(AnimationSpeed.NORMAL)
        setHighContrast(false)
        setColorBlindMode(ColorBlindMode.NONE)
        Log.d(TAG, "Theme settings reset to defaults")
    }

    // Private methods
    private fun applyCurrentTheme() {
        val mode = _currentTheme.value
        val delegateMode = when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.CUSTOM -> {
                // For custom themes, determine based on current colors
                val colors = _customColors.value
                if (colors.background < Color.parseColor("#808080")) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            }
        }
        
        AppCompatDelegate.setDefaultNightMode(delegateMode)
    }

    private fun getCurrentThemeMode(): ThemeMode {
        val ordinal = preferences.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.values()[ordinal]
    }

    private fun loadCustomColors(): ThemeColors {
        val colorsJson = preferences.getString(KEY_CUSTOM_COLORS, null)
        return if (colorsJson != null) {
            try {
                // Parse JSON (simplified for this example)
                // In production, use proper JSON serialization
                DEFAULT_LIGHT_COLORS
            } catch (e: Exception) {
                Log.e(TAG, "Error loading custom colors", e)
                DEFAULT_LIGHT_COLORS
            }
        } else {
            DEFAULT_LIGHT_COLORS
        }
    }

    private fun saveCustomColors(colors: ThemeColors) {
        // Save to SharedPreferences (simplified for this example)
        // In production, use proper JSON serialization
        preferences.edit().putString(KEY_CUSTOM_COLORS, colors.toString()).apply()
    }

    private fun loadFontSize(): FontSize {
        val scale = preferences.getFloat(KEY_FONT_SIZE, FontSize.NORMAL.scale)
        return FontSize.values().find { it.scale == scale } ?: FontSize.NORMAL
    }

    private fun loadAnimationSpeed(): AnimationSpeed {
        val multiplier = preferences.getFloat(KEY_ANIMATION_SPEED, AnimationSpeed.NORMAL.multiplier)
        return AnimationSpeed.values().find { it.multiplier == multiplier } ?: AnimationSpeed.NORMAL
    }

    private fun loadHighContrast(): Boolean {
        return preferences.getBoolean(KEY_HIGH_CONTRAST, false)
    }

    private fun loadColorBlindMode(): ColorBlindMode {
        val modeName = preferences.getString(KEY_COLOR_BLIND_MODE, ColorBlindMode.NONE.name)
        return ColorBlindMode.valueOf(modeName ?: ColorBlindMode.NONE.name)
    }

    private fun applyHighContrast(colors: ThemeColors): ThemeColors {
        return colors.copy(
            background = Color.BLACK,
            surface = Color.BLACK,
            onBackground = Color.WHITE,
            onSurface = Color.WHITE,
            primary = Color.parseColor("#FFFFFF"),
            onPrimary = Color.BLACK
        )
    }

    private fun applyColorBlindCompensation(colors: ThemeColors, mode: ColorBlindMode): ThemeColors {
        return when (mode) {
            ColorBlindMode.DEUTERANOPIA -> colors.copy(
                primary = adjustColorForDeuteranopia(colors.primary),
                secondary = adjustColorForDeuteranopia(colors.secondary)
            )
            ColorBlindMode.PROTANOPIA -> colors.copy(
                primary = adjustColorForProtanopia(colors.primary),
                secondary = adjustColorForProtanopia(colors.secondary)
            )
            ColorBlindMode.TRITANOPIA -> colors.copy(
                primary = adjustColorForTritanopia(colors.primary),
                secondary = adjustColorForTritanopia(colors.secondary)
            )
            ColorBlindMode.MONOCHROMACY -> applyMonochrome(colors)
            else -> colors
        }
    }

    private fun adjustColorForDeuteranopia(color: Int): Int {
        // Simplified color adjustment for deuteranopia (green deficiency)
        // In production, use more sophisticated color transformation
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        
        // Adjust for deuteranopia
        val newRed = (red * 0.625f + green * 0.375f).toInt().coerceIn(0, 255)
        val newGreen = (red * 0.7f + green * 0.3f).toInt().coerceIn(0, 255)
        val newBlue = blue
        
        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun adjustColorForProtanopia(color: Int): Int {
        // Simplified color adjustment for protanopia (red deficiency)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        
        // Adjust for protanopia
        val newRed = (red * 0.567f + green * 0.433f).toInt().coerceIn(0, 255)
        val newGreen = (red * 0.558f + green * 0.442f).toInt().coerceIn(0, 255)
        val newBlue = blue
        
        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun adjustColorForTritanopia(color: Int): Int {
        // Simplified color adjustment for tritanopia (blue deficiency)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        
        // Adjust for tritanopia
        val newRed = red
        val newGreen = (green * 0.95f + blue * 0.05f).toInt().coerceIn(0, 255)
        val newBlue = (red * 0.433f + green * 0.242f + blue * 0.325f).toInt().coerceIn(0, 255)
        
        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun applyMonochrome(colors: ThemeColors): ThemeColors {
        // Convert to grayscale
        val gray = (Color.red(colors.primary) * 0.299f + 
                   Color.green(colors.primary) * 0.587f + 
                   Color.blue(colors.primary) * 0.114f).toInt()
        
        val mono = Color.rgb(gray, gray, gray)
        return colors.copy(
            primary = mono,
            primaryVariant = mono,
            secondary = mono,
            background = mono,
            surface = mono
        )
    }

    private fun notifyThemeChanged() {
        val colors = getCurrentColors()
        observers.forEach { observer ->
            try {
                observer.onThemeChanged(_currentTheme.value, colors)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying theme observer", e)
            }
        }
    }

    private fun notifyColorsUpdated() {
        observers.forEach { observer ->
            try {
                observer.onColorsUpdated(_customColors.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying colors observer", e)
            }
        }
    }

    private fun notifyFontSizeChanged() {
        observers.forEach { observer ->
            try {
                observer.onFontSizeChanged(_fontSize.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying font size observer", e)
            }
        }
    }

    private fun notifyAnimationSpeedChanged() {
        observers.forEach { observer ->
            try {
                observer.onAnimationSpeedChanged(_animationSpeed.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying animation speed observer", e)
            }
        }
    }

    private fun notifyAccessibilityChanged() {
        observers.forEach { observer ->
            try {
                observer.onAccessibilitySettingsChanged(_highContrast.value, _colorBlindMode.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying accessibility observer", e)
            }
        }
    }
}

/**
 * Theme preset data class
 */
data class ThemePreset(
    val name: String,
    val colors: ThemeManager.ThemeColors,
    val themeMode: ThemeManager.ThemeMode
)