package com.pythonide.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Comprehensive accessibility management system
 * Provides support for screen readers, high contrast, large text, voice commands, and haptic feedback
 */
class AccessibilityManager(private val context: Context) {
    companion object {
        private const val TAG = "AccessibilityManager"
        
        // Accessibility event types
        const val EVENT_TYPE_UI_CHANGED = 1001
        const val EVENT_TYPE_CONTENT_CHANGED = 1002
        const val EVENT_TYPE_FOCUS_CHANGED = 1003
        const val EVENT_TYPE_CLICK = 1004
        
        // Haptic feedback patterns
        private val HAPTIC_FEEDBACK_LIGHT = longArrayOf(0, 20)
        private val HAPTIC_FEEDBACK_MEDIUM = longArrayOf(0, 40)
        private val HAPTIC_FEEDBACK_HEAVY = longArrayOf(0, 60)
        private val HAPTIC_FEEDBACK_SUCCESS = longArrayOf(0, 20, 40, 20)
        private val HAPTIC_FEEDBACK_ERROR = longArrayOf(0, 100, 50, 100)
    }

    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    private val _accessibilityEnabled = MutableStateFlow(isAccessibilityEnabled())
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()
    
    private val _screenReaderEnabled = MutableStateFlow(isScreenReaderEnabled())
    val screenReaderEnabled: StateFlow<Boolean> = _screenReaderEnabled.asStateFlow()
    
    private val _largeTextEnabled = MutableStateFlow(isLargeTextEnabled())
    val largeTextEnabled: StateFlow<Boolean> = _largeTextEnabled.asStateFlow()
    
    private val _highContrastEnabled = MutableStateFlow(isHighContrastEnabled())
    val highContrastEnabled: StateFlow<Boolean> = _highContrastEnabled.asStateFlow()
    
    private val _voiceControlEnabled = MutableStateFlow(false)
    val voiceControlEnabled: StateFlow<Boolean> = _voiceControlEnabled.asStateFlow()
    
    private val _hapticFeedbackEnabled = MutableStateFlow(isHapticFeedbackEnabled())
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()
    
    private val _reducedMotionEnabled = MutableStateFlow(isReducedMotionEnabled())
    val reducedMotionEnabled: StateFlow<Boolean> = _reducedMotionEnabled.asStateFlow()
    
    private var observers = mutableListOf<AccessibilityObserver>()
    private var speechRecognizer: SpeechRecognizer? = null
    
    // Voice command mappings
    private val voiceCommands = mutableMapOf<String, VoiceCommand>()
    
    /**
     * Accessibility settings
     */
    data class AccessibilitySettings(
        val screenReaderEnabled: Boolean = false,
        val largeTextEnabled: Boolean = false,
        val highContrastEnabled: Boolean = false,
        val voiceControlEnabled: Boolean = false,
        val hapticFeedbackEnabled: Boolean = false,
        val reducedMotionEnabled: Boolean = false,
        val keyboardNavigationEnabled: Boolean = true,
        val soundEffectsEnabled: Boolean = true
    )
    
    /**
     * Voice command data class
     */
    data class VoiceCommand(
        val command: String,
        val description: String,
        val action: () -> Unit,
        val keywords: List<String> = emptyList()
    )
    
    /**
     * Accessibility event
     */
    data class AccessibilityEvent(
        val type: Int,
        val source: String,
        val content: String,
        val metadata: Bundle = Bundle()
    )
    
    /**
     * Accessibility focus info
     */
    data class AccessibilityFocus(
        val nodeId: String,
        val text: String,
        val bounds: android.graphics.Rect,
        val isFocusable: Boolean,
        val isFocused: Boolean
    )
    
    /**
     * Accessibility observer interface
     */
    interface AccessibilityObserver {
        fun onScreenReaderStateChanged(enabled: Boolean)
        fun onTextSizeChanged(largeText: Boolean)
        fun onHighContrastChanged(enabled: Boolean)
        fun onVoiceControlStateChanged(enabled: Boolean)
        fun onHapticFeedbackChanged(enabled: Boolean)
        fun onAccessibilityEvent(event: AccessibilityEvent)
        fun onFocusChanged(focus: AccessibilityFocus?)
        fun onVoiceCommandRecognized(command: String, params: Map<String, String>)
    }

    /**
     * Initialize accessibility manager
     */
    init {
        setupVoiceCommands()
        initializeSpeechRecognizer()
        updateAccessibilityStates()
    }

    /**
     * Enable accessibility features
     */
    fun enableAccessibility(settings: AccessibilitySettings) {
        if (settings.screenReaderEnabled) {
            enableScreenReader()
        }
        if (settings.voiceControlEnabled) {
            enableVoiceControl()
        }
        if (settings.hapticFeedbackEnabled) {
            enableHapticFeedback()
        }
        
        updateAccessibilityStates()
    }

    /**
     * Disable accessibility features
     */
    fun disableAccessibility() {
        disableScreenReader()
        disableVoiceControl()
        disableHapticFeedback()
        
        updateAccessibilityStates()
    }

    /**
     * Announce message to screen reader
     */
    fun announceToScreenReader(message: String, priority: AccessibilityEvent.TYPE_VIEW_FOCUSED = AccessibilityEvent.TYPE_VIEW_FOCUSED) {
        if (_screenReaderEnabled.value) {
            try {
                val event = AccessibilityEvent.obtain(
                    priority,
                    AccessibilityEvent.TYPE_VIEW_FOCUSED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT
                )
                event.text.add(message)
                
                // In a real implementation, you would send this to the accessibility service
                Log.d(TAG, "Announcing to screen reader: $message")
                
                notifyAccessibilityEvent(AccessibilityEvent(
                    type = EVENT_TYPE_CONTENT_CHANGED,
                    source = "screen_reader",
                    content = message
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Error announcing to screen reader", e)
            }
        }
    }

    /**
     * Provide haptic feedback
     */
    fun provideHapticFeedback(feedbackType: HapticFeedbackType) {
        if (!_hapticFeedbackEnabled.value) return
        
        try {
            when (feedbackType) {
                HapticFeedbackType.LIGHT -> vibrator.vibrate(HAPTIC_FEEDBACK_LIGHT, -1)
                HapticFeedbackType.MEDIUM -> vibrator.vibrate(HAPTIC_FEEDBACK_MEDIUM, -1)
                HapticFeedbackType.HEAVY -> vibrator.vibrate(HAPTIC_FEEDBACK_HEAVY, -1)
                HapticFeedbackType.SUCCESS -> vibrator.vibrate(HAPTIC_FEEDBACK_SUCCESS, -1)
                HapticFeedbackType.ERROR -> vibrator.vibrate(HAPTIC_FEEDBACK_ERROR, -1)
            }
            
            Log.d(TAG, "Provided haptic feedback: $feedbackType")
        } catch (e: Exception) {
            Log.e(TAG, "Error providing haptic feedback", e)
        }
    }

    /**
     * Enable screen reader mode
     */
    fun enableScreenReader() {
        try {
            // Enable accessibility service if not already enabled
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            _screenReaderEnabled.value = true
            announceToScreenReader("Screen reader mode enabled")
            
            notifyScreenReaderStateChanged(true)
            Log.d(TAG, "Screen reader enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling screen reader", e)
        }
    }

    /**
     * Disable screen reader mode
     */
    fun disableScreenReader() {
        _screenReaderEnabled.value = false
        notifyScreenReaderStateChanged(false)
        Log.d(TAG, "Screen reader disabled")
    }

    /**
     * Enable voice control
     */
    fun enableVoiceControl() {
        try {
            if (speechRecognizer == null) {
                initializeSpeechRecognizer()
            }
            
            _voiceControlEnabled.value = true
            startListening()
            
            announceToScreenReader("Voice control enabled")
            notifyVoiceControlStateChanged(true)
            Log.d(TAG, "Voice control enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling voice control", e)
        }
    }

    /**
     * Disable voice control
     */
    fun disableVoiceControl() {
        _voiceControlEnabled.value = false
        stopListening()
        
        notifyVoiceControlStateChanged(false)
        Log.d(TAG, "Voice control disabled")
    }

    /**
     * Enable haptic feedback
     */
    fun enableHapticFeedback() {
        _hapticFeedbackEnabled.value = true
        provideHapticFeedback(HapticFeedbackType.SUCCESS)
        notifyHapticFeedbackChanged(true)
        Log.d(TAG, "Haptic feedback enabled")
    }

    /**
     * Disable haptic feedback
     */
    fun disableHapticFeedback() {
        _hapticFeedbackEnabled.value = false
        notifyHapticFeedbackChanged(false)
        Log.d(TAG, "Haptic feedback disabled")
    }

    /**
     * Increase text size
     */
    fun increaseTextSize() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            _largeTextEnabled.value = true
            announceToScreenReader("Text size increased")
            notifyTextSizeChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error increasing text size", e)
        }
    }

    /**
     * Enable high contrast mode
     */
    fun enableHighContrast() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            _highContrastEnabled.value = true
            announceToScreenReader("High contrast mode enabled")
            notifyHighContrastChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling high contrast", e)
        }
    }

    /**
     * Disable high contrast mode
     */
    fun disableHighContrast() {
        _highContrastEnabled.value = false
        notifyHighContrastChanged(false)
        Log.d(TAG, "High contrast disabled")
    }

    /**
     * Get current accessibility settings
     */
    fun getCurrentSettings(): AccessibilitySettings {
        return AccessibilitySettings(
            screenReaderEnabled = _screenReaderEnabled.value,
            largeTextEnabled = _largeTextEnabled.value,
            highContrastEnabled = _highContrastEnabled.value,
            voiceControlEnabled = _voiceControlEnabled.value,
            hapticFeedbackEnabled = _hapticFeedbackEnabled.value,
            reducedMotionEnabled = _reducedMotionEnabled.value
        )
    }

    /**
     * Register voice command
     */
    fun registerVoiceCommand(command: VoiceCommand) {
        voiceCommands[command.command.lowercase()] = command
        Log.d(TAG, "Registered voice command: ${command.command}")
    }

    /**
     * Unregister voice command
     */
    fun unregisterVoiceCommand(command: String) {
        voiceCommands.remove(command.lowercase())
        Log.d(TAG, "Unregistered voice command: $command")
    }

    /**
     * Get accessibility help text
     */
    fun getAccessibilityHelpText(): String {
        return """
            Available accessibility commands:
            
            Screen Reader:
            - Tap and swipe to navigate
            - Double-tap to activate
            - Two-finger scroll to navigate lists
            
            Voice Control:
            - Say "help" to see available commands
            - Say "scroll down/up" to navigate
            - Say "open [file name]" to open files
            - Say "save" to save current work
            
            Gestures:
            - Swipe up with three fingers: Start screen reader
            - Swipe right: Next element
            - Swipe left: Previous element
            - Double-tap: Activate current element
            
            Haptic Feedback:
            - Light tap: Button pressed
            - Medium tap: Menu item selected
            - Heavy tap: Error or warning
        """.trimIndent()
    }

    /**
     * Add accessibility observer
     */
    fun addObserver(observer: AccessibilityObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    /**
     * Remove accessibility observer
     */
    fun removeObserver(observer: AccessibilityObserver) {
        observers.remove(observer)
    }

    /**
     * Process accessibility event
     */
    fun processAccessibilityEvent(event: AccessibilityEvent) {
        when (event.type) {
            EVENT_TYPE_FOCUS_CHANGED -> {
                // Handle focus changes
                Log.d(TAG, "Focus changed: ${event.content}")
            }
            EVENT_TYPE_CLICK -> {
                // Provide haptic feedback for clicks
                provideHapticFeedback(HapticFeedbackType.LIGHT)
            }
            EVENT_TYPE_CONTENT_CHANGED -> {
                // Announce content changes to screen reader
                if (_screenReaderEnabled.value) {
                    announceToScreenReader(event.content)
                }
            }
        }
        
        notifyAccessibilityEvent(event)
    }

    // Private methods
    private fun isAccessibilityEnabled(): Boolean {
        return accessibilityManager.isEnabled
    }

    private fun isScreenReaderEnabled(): Boolean {
        return try {
            val enabledAccessibilityServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            enabledAccessibilityServices?.contains("TalkBack") ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isLargeTextEnabled(): Boolean {
        return try {
            val scale = context.resources.configuration.fontScale
            scale > 1.0
        } catch (e: Exception) {
            false
        }
    }

    private fun isHighContrastEnabled(): Boolean {
        return try {
            // Check if high contrast theme is enabled
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_HIGH_CONTRAST_TEXT,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun isHapticFeedbackEnabled(): Boolean {
        return audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    private fun isReducedMotionEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                ) < 1.0f
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                // Set up recognition listener
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer", e)
        }
    }

    private fun setupVoiceCommands() {
        // Register default voice commands
        registerVoiceCommand(
            VoiceCommand("help", "Show help information") {
                announceToScreenReader(getAccessibilityHelpText())
            }
        )
        
        registerVoiceCommand(
            VoiceCommand("save", "Save current work") {
                announceToScreenReader("Saving current work")
            }
        )
        
        registerVoiceCommand(
            VoiceCommand("open", "Open file", {
                announceToScreenReader("Please specify which file to open")
            }, listOf("file", "document"))
        )
        
        registerVoiceCommand(
            VoiceCommand("scroll down", "Scroll down") {
                announceToScreenReader("Scrolling down")
            }
        )
        
        registerVoiceCommand(
            VoiceCommand("scroll up", "Scroll up") {
                announceToScreenReader("Scrolling up")
            }
        )
    }

    private fun startListening() {
        try {
            // Start voice recognition
            speechRecognizer?.let { recognizer ->
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                recognizer.startListening(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recognition", e)
        }
    }

    private fun updateAccessibilityStates() {
        _accessibilityEnabled.value = isAccessibilityEnabled()
        _screenReaderEnabled.value = isScreenReaderEnabled()
        _largeTextEnabled.value = isLargeTextEnabled()
        _highContrastEnabled.value = isHighContrastEnabled()
        _hapticFeedbackEnabled.value = isHapticFeedbackEnabled()
        _reducedMotionEnabled.value = isReducedMotionEnabled()
    }

    private fun notifyScreenReaderStateChanged(enabled: Boolean) {
        observers.forEach { observer ->
            try {
                observer.onScreenReaderStateChanged(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying screen reader observer", e)
            }
        }
    }

    private fun notifyTextSizeChanged(largeText: Boolean) {
        observers.forEach { observer ->
            try {
                observer.onTextSizeChanged(largeText)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying text size observer", e)
            }
        }
    }

    private fun notifyHighContrastChanged(enabled: Boolean) {
        observers.forEach { observer ->
            try {
                observer.onHighContrastChanged(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying high contrast observer", e)
            }
        }
    }

    private fun notifyVoiceControlStateChanged(enabled: Boolean) {
        observers.forEach { observer ->
            try {
                observer.onVoiceControlStateChanged(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying voice control observer", e)
            }
        }
    }

    private fun notifyHapticFeedbackChanged(enabled: Boolean) {
        observers.forEach { observer ->
            try {
                observer.onHapticFeedbackChanged(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying haptic feedback observer", e)
            }
        }
    }

    private fun notifyAccessibilityEvent(event: AccessibilityEvent) {
        observers.forEach { observer ->
            try {
                observer.onAccessibilityEvent(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying accessibility event observer", e)
            }
        }
    }
}

/**
 * Haptic feedback types
 */
enum class HapticFeedbackType {
    LIGHT, MEDIUM, HEAVY, SUCCESS, ERROR
}