package com.pythonide.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adaptive UI system that responds to different screen sizes, orientations, and device types
 * Provides dynamic layout adjustments and responsive design features
 */
class AdaptiveUI(private val context: Context) {
    companion object {
        private const val TAG = "AdaptiveUI"
        
        // Screen size categories
        const val SIZE_SMALL = "small"
        const val SIZE_NORMAL = "normal"
        const val SIZE_LARGE = "large"
        const val SIZE_XLARGE = "xlarge"
        
        // Orientation constants
        const val ORIENTATION_PORTRAIT = "portrait"
        const val ORIENTATION_LANDSCAPE = "landscape"
        
        // Device type constants
        const val DEVICE_PHONE = "phone"
        const val DEVICE_TABLET = "tablet"
        const val DEVICE_DESKTOP = "desktop"
        const val DEVICE_TV = "tv"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    private val _screenInfo = MutableStateFlow(getScreenInfo())
    val screenInfo: StateFlow<ScreenInfo> = _screenInfo.asStateFlow()
    
    private val _isFoldable = MutableStateFlow(false)
    val isFoldable: StateFlow<Boolean> = _isFoldable.asStateFlow()
    
    private val _foldState = MutableStateFlow(FoldState.UNKNOWN)
    val foldState: StateFlow<FoldState> = _foldState.asStateFlow()
    
    private var observers = mutableListOf<AdaptiveUIObserver>()
    
    // Layout configurations
    private var currentLayout = LayoutType.UNKNOWN
    private val layoutConfigurations = mutableMapOf<LayoutType, LayoutConfig>()

    /**
     * Screen information data class
     */
    data class ScreenInfo(
        val size: String,
        val orientation: String,
        val deviceType: String,
        val width: Int,
        val height: Int,
        val density: Float,
        val isFoldable: Boolean,
        val foldState: FoldState = FoldState.UNKNOWN,
        val isTablet: Boolean = false,
        val isLandscape: Boolean = false,
        val aspectRatio: Float = 0f
    )
    
    /**
     * Foldable device states
     */
    enum class FoldState {
        UNKNOWN, FLAT, HALF_OPENED, CLOSED, OPENED
    }
    
    /**
     * Layout types for different screen configurations
     */
    enum class LayoutType {
        UNKNOWN,
        PHONE_PORTRAIT,
        PHONE_LANDSCAPE,
        TABLET_PORTRAIT,
        TABLET_LANDSCAPE,
        TV_LANDSCAPE,
        DESKTOP_PORTRAIT,
        DESKTOP_LANDSCAPE,
        FOLDABLE_HALF_OPENED,
        FOLDABLE_FLAT,
        FOLDABLE_CLOSED
    }
    
    /**
     * Layout configuration for different screen types
     */
    data class LayoutConfig(
        val layoutType: LayoutType,
        val columns: Int,
        val rows: Int,
        val sidebarWidth: Int,
        val minTouchTargetSize: Int,
        val maxTextSize: Float,
        val minTextSize: Float,
        val padding: Int,
        val margin: Int
    ) {
        fun copyWithUpdates(
            columns: Int = this.columns,
            rows: Int = this.rows,
            sidebarWidth: Int = this.sidebarWidth
        ): LayoutConfig {
            return copy(columns = columns, rows = rows, sidebarWidth = sidebarWidth)
        }
    }
    
    /**
     * Responsive breakpoints
     */
    data class Breakpoints(
        val phone: Size,
        val tablet: Size,
        val largeTablet: Size
    ) {
        fun getBreakpointForWidth(width: Int): String {
            return when {
                width < phone.width -> "small"
                width < tablet.width -> "normal"
                width < largeTablet.width -> "large"
                else -> "xlarge"
            }
        }
    }
    
    /**
     * Adaptive UI observer interface
     */
    interface AdaptiveUIObserver {
        fun onScreenInfoChanged(info: ScreenInfo)
        fun onLayoutChanged(oldLayout: LayoutType, newLayout: LayoutType, config: LayoutConfig)
        fun onFoldStateChanged(state: FoldState)
        fun onBreakpointReached(breakpoint: String)
    }

    /**
     * Initialize adaptive UI system
     */
    init {
        setupDefaultLayouts()
        updateScreenInfo()
        checkForFoldableDevice()
    }

    /**
     * Get current layout configuration
     */
    fun getCurrentLayoutConfig(): LayoutConfig {
        return layoutConfigurations[currentLayout] ?: LayoutConfig.UNKNOWN.let {
            LayoutConfig(
                layoutType = it,
                columns = 1,
                rows = 1,
                sidebarWidth = 300,
                minTouchTargetSize = 48,
                maxTextSize = 16f,
                minTextSize = 12f,
                padding = 16,
                margin = 8
            )
        }
    }

    /**
     * Get responsive dimensions for components
     */
    fun getResponsiveDimensions(component: ComponentType): ResponsiveDimensions {
        val info = _screenInfo.value
        val layoutConfig = getCurrentLayoutConfig()
        
        return when (component) {
            ComponentType.CODE_EDITOR -> ResponsiveDimensions(
                width = if (info.isLandscape) {
                    info.width - layoutConfig.sidebarWidth - (layoutConfig.padding * 2)
                } else {
                    info.width - (layoutConfig.padding * 2)
                },
                height = if (info.isLandscape) {
                    info.height - (layoutConfig.padding * 2) - 100
                } else {
                    (info.height * 0.6f).toInt() - (layoutConfig.padding * 2)
                },
                minWidth = 300,
                minHeight = 200
            )
            ComponentType.FILE_TREE -> ResponsiveDimensions(
                width = if (info.isTablet && info.isLandscape) {
                    layoutConfig.sidebarWidth
                } else {
                    info.width
                },
                height = when {
                    info.deviceType == DEVICE_TV -> (info.height * 0.4f).toInt()
                    info.isLandscape -> info.height - 100
                    else -> (info.height * 0.3f).toInt()
                },
                minWidth = 250,
                minHeight = 200
            )
            ComponentType.TOOLBAR -> ResponsiveDimensions(
                width = info.width,
                height = if (info.deviceType == DEVICE_TV) 80 else 56,
                minWidth = info.width,
                minHeight = if (info.deviceType == DEVICE_TV) 80 else 56
            )
            ComponentType.TERMINAL -> ResponsiveDimensions(
                width = info.width - (layoutConfig.padding * 2),
                height = if (info.isLandscape) {
                    (info.height * 0.4f).toInt()
                } else {
                    (info.height * 0.3f).toInt()
                },
                minWidth = 300,
                minHeight = 150
            )
            ComponentType.SETTINGS_PANEL -> ResponsiveDimensions(
                width = if (info.isTablet) {
                    (info.width * 0.6f).toInt()
                } else {
                    info.width
                },
                height = if (info.isLandscape) {
                    info.height
                } else {
                    (info.height * 0.8f).toInt()
                },
                minWidth = 320,
                minHeight = 400
            )
        }
    }

    /**
     * Get appropriate text sizes for current screen
     */
    fun getResponsiveTextSize(textType: TextType): Float {
        val info = _screenInfo.value
        val layoutConfig = getCurrentLayoutConfig()
        
        return when (textType) {
            TextType.CODE_EDITOR -> {
                when {
                    info.deviceType == DEVICE_TV -> 18f
                    info.deviceType == DEVICE_TABLET -> 16f
                    info.size == SIZE_LARGE -> 15f
                    info.size == SIZE_XLARGE -> 16f
                    else -> 14f
                }
            }
            TextType.HEADING -> {
                when {
                    info.deviceType == DEVICE_TV -> 28f
                    info.deviceType == DEVICE_TABLET -> 24f
                    info.size == SIZE_LARGE -> 22f
                    info.size == SIZE_XLARGE -> 24f
                    else -> 20f
                }
            }
            TextType.BODY -> {
                when {
                    info.deviceType == DEVICE_TV -> 22f
                    info.deviceType == DEVICE_TABLET -> 18f
                    info.size == SIZE_LARGE -> 16f
                    info.size == SIZE_XLARGE -> 18f
                    else -> 14f
                }
            }
            TextType.BUTTON -> {
                when {
                    info.deviceType == DEVICE_TV -> 24f
                    info.deviceType == DEVICE_TABLET -> 16f
                    info.size == SIZE_LARGE || info.size == SIZE_XLARGE -> 16f
                    else -> 14f
                }
            }
            TextType.MENU -> {
                when {
                    info.deviceType == DEVICE_TV -> 22f
                    info.deviceType == DEVICE_TABLET -> 16f
                    else -> 14f
                }
            }
        }
    }

    /**
     * Get responsive touch target size
     */
    fun getResponsiveTouchTarget(): Int {
        val info = _screenInfo.value
        
        return when {
            info.deviceType == DEVICE_TV -> 80 // Larger targets for TV
            info.size == SIZE_XLARGE -> 60
            info.size == SIZE_LARGE -> 56
            else -> 48 // Minimum touch target size
        }
    }

    /**
     * Check if screen supports multi-pane layout
     */
    fun supportsMultiPane(): Boolean {
        val info = _screenInfo.value
        
        return when (info.deviceType) {
            DEVICE_TABLET -> true
            DEVICE_DESKTOP -> true
            DEVICE_TV -> info.isLandscape
            else -> info.width >= 900 && info.isLandscape
        }
    }

    /**
     * Get optimal column count for grid layouts
     */
    fun getOptimalColumnCount(component: GridComponent): Int {
        val info = _screenInfo.value
        val layoutConfig = getCurrentLayoutConfig()
        
        return when (component) {
            GridComponent.FILE_LIST -> {
                when {
                    info.deviceType == DEVICE_TV -> 3
                    info.deviceType == DEVICE_TABLET && info.isLandscape -> 2
                    info.width > 1200 -> 3
                    info.width > 800 -> 2
                    else -> 1
                }
            }
            GridComponent.TOOL_PANEL -> {
                when {
                    info.deviceType == DEVICE_TABLET && info.isLandscape -> 2
                    info.width > 1000 -> 2
                    else -> 1
                }
            }
            GridComponent.FILE_TABS -> {
                if (info.isLandscape && info.width > 600) {
                    Math.min(layoutConfig.columns, 8)
                } else {
                    Math.min(layoutConfig.columns, 4)
                }
            }
        }
    }

    /**
     * Get animation duration based on device capabilities
     */
    fun getAnimationDuration(animationType: AnimationType): Long {
        val info = _screenInfo.value
        
        return when (animationType) {
            AnimationType.TRANSITION -> if (info.deviceType == DEVICE_TV) 300L else 200L
            AnimationType.HOVER -> 150L
            AnimationType.CLICK -> 100L
            AnimationType.EXPAND_COLLAPSE -> 250L
            AnimationType.SLIDE_IN -> 200L
            AnimationType.FADE_IN -> 150L
        }
    }

    /**
     * Add adaptive UI observer
     */
    fun addObserver(observer: AdaptiveUIObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    /**
     * Remove adaptive UI observer
     */
    fun removeObserver(observer: AdaptiveUIObserver) {
        observers.remove(observer)
    }

    /**
     * Update screen information (call when configuration changes)
     */
    fun updateScreenInfo() {
        val oldLayout = currentLayout
        val newInfo = getScreenInfo()
        val newLayout = determineLayoutType(newInfo)
        
        _screenInfo.value = newInfo
        
        if (oldLayout != newLayout) {
            val oldConfig = getLayoutConfigForType(oldLayout)
            val newConfig = getLayoutConfigForType(newLayout)
            currentLayout = newLayout
            
            notifyLayoutChanged(oldLayout, newLayout, newConfig)
        }
        
        notifyScreenInfoChanged(newInfo)
    }

    /**
     * Get breakpoints for responsive design
     */
    fun getBreakpoints(): Breakpoints {
        val density = context.resources.displayMetrics.density
        
        return Breakpoints(
            phone = Size((400 * density).toInt(), (800 * density).toInt()),
            tablet = Size((800 * density).toInt(), (1280 * density).toInt()),
            largeTablet = Size((1200 * density).toInt(), (1920 * density).toInt())
        )
    }

    // Private methods
    private fun setupDefaultLayouts() {
        // Phone portrait layout
        layoutConfigurations[LayoutType.PHONE_PORTRAIT] = LayoutConfig(
            layoutType = LayoutType.PHONE_PORTRAIT,
            columns = 1,
            rows = 1,
            sidebarWidth = 0,
            minTouchTargetSize = 48,
            maxTextSize = 16f,
            minTextSize = 12f,
            padding = 16,
            margin = 8
        )
        
        // Phone landscape layout
        layoutConfigurations[LayoutType.PHONE_LANDSCAPE] = LayoutConfig(
            layoutType = LayoutType.PHONE_LANDSCAPE,
            columns = 2,
            rows = 1,
            sidebarWidth = 250,
            minTouchTargetSize = 48,
            maxTextSize = 16f,
            minTextSize = 12f,
            padding = 16,
            margin = 8
        )
        
        // Tablet portrait layout
        layoutConfigurations[LayoutType.TABLET_PORTRAIT] = LayoutConfig(
            layoutType = LayoutType.TABLET_PORTRAIT,
            columns = 1,
            rows = 2,
            sidebarWidth = 300,
            minTouchTargetSize = 48,
            maxTextSize = 18f,
            minTextSize = 14f,
            padding = 20,
            margin = 12
        )
        
        // Tablet landscape layout
        layoutConfigurations[LayoutType.TABLET_LANDSCAPE] = LayoutConfig(
            layoutType = LayoutType.TABLET_LANDSCAPE,
            columns = 2,
            rows = 1,
            sidebarWidth = 350,
            minTouchTargetSize = 48,
            maxTextSize = 18f,
            minTextSize = 14f,
            padding = 20,
            margin = 12
        )
        
        // TV layout
        layoutConfigurations[LayoutType.TV_LANDSCAPE] = LayoutConfig(
            layoutType = LayoutType.TV_LANDSCAPE,
            columns = 3,
            rows = 2,
            sidebarWidth = 400,
            minTouchTargetSize = 80,
            maxTextSize = 28f,
            minTextSize = 20f,
            padding = 32,
            margin = 16
        )
    }

    private fun getScreenInfo(): ScreenInfo {
        val display = windowManager.defaultDisplay
        display.getMetrics(displayMetrics)
        
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.density
        val isLandscape = width > height
        
        val size = determineScreenSize(width, density)
        val deviceType = determineDeviceType(width, height, density)
        val isTablet = deviceType == DEVICE_TABLET
        val aspectRatio = width.toFloat() / height.toFloat()
        
        return ScreenInfo(
            size = size,
            orientation = if (isLandscape) ORIENTATION_LANDSCAPE else ORIENTATION_PORTRAIT,
            deviceType = deviceType,
            width = width,
            height = height,
            density = density,
            isFoldable = _isFoldable.value,
            foldState = _foldState.value,
            isTablet = isTablet,
            isLandscape = isLandscape,
            aspectRatio = aspectRatio
        )
    }

    private fun determineScreenSize(width: Int, density: Float): String {
        val dpWidth = (width / density).toInt()
        
        return when {
            dpWidth < 600 -> SIZE_SMALL
            dpWidth < 900 -> SIZE_NORMAL
            dpWidth < 1200 -> SIZE_LARGE
            else -> SIZE_XLARGE
        }
    }

    private fun determineDeviceType(width: Int, height: Int, density: Float): String {
        val smallestWidth = Math.min(width, height) / density
        
        return when {
            isTelevision() -> DEVICE_TV
            smallestWidth >= 900 -> DEVICE_TABLET
            smallestWidth >= 600 -> DEVICE_PHONE
            else -> DEVICE_PHONE
        }
    }

    private fun isTelevision(): Boolean {
        return try {
            // Check if running on Android TV or similar TV devices
            context.packageManager.hasSystemFeature("android.software.leanback")
        } catch (e: Exception) {
            false
        }
    }

    private fun determineLayoutType(info: ScreenInfo): LayoutType {
        return when {
            info.isFoldable -> {
                when (info.foldState) {
                    FoldState.HALF_OPENED -> LayoutType.FOLDABLE_HALF_OPENED
                    FoldState.FLAT -> LayoutType.FOLDABLE_FLAT
                    FoldState.CLOSED -> LayoutType.FOLDABLE_CLOSED
                    else -> if (info.isLandscape) LayoutType.TABLET_LANDSCAPE else LayoutType.TABLET_PORTRAIT
                }
            }
            info.deviceType == DEVICE_TV && info.isLandscape -> LayoutType.TV_LANDSCAPE
            info.deviceType == DEVICE_TABLET && info.isLandscape -> LayoutType.TABLET_LANDSCAPE
            info.deviceType == DEVICE_TABLET -> LayoutType.TABLET_PORTRAIT
            info.deviceType == DEVICE_DESKTOP && info.isLandscape -> LayoutType.DESKTOP_LANDSCAPE
            info.deviceType == DEVICE_DESKTOP -> LayoutType.DESKTOP_PORTRAIT
            info.isLandscape -> LayoutType.PHONE_LANDSCAPE
            else -> LayoutType.PHONE_PORTRAIT
        }
    }

    private fun getLayoutConfigForType(layoutType: LayoutType): LayoutConfig {
        return layoutConfigurations[layoutType] ?: layoutConfigurations[LayoutType.PHONE_PORTRAIT]!!
    }

    private fun checkForFoldableDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Check for foldable device features
                val hasFeature = context.packageManager.hasSystemFeature("android.hardware.type.watch")
                // Additional checks for foldable devices would go here
                _isFoldable.value = false // Simplified for this example
            } catch (e: Exception) {
                Log.w(TAG, "Error checking for foldable device", e)
            }
        }
    }

    private fun notifyScreenInfoChanged(info: ScreenInfo) {
        observers.forEach { observer ->
            try {
                observer.onScreenInfoChanged(info)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying screen info observer", e)
            }
        }
    }

    private fun notifyLayoutChanged(oldLayout: LayoutType, newLayout: LayoutType, config: LayoutConfig) {
        observers.forEach { observer ->
            try {
                observer.onLayoutChanged(oldLayout, newLayout, config)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying layout observer", e)
            }
        }
    }

    private fun notifyFoldStateChanged(state: FoldState) {
        _foldState.value = state
        observers.forEach { observer ->
            try {
                observer.onFoldStateChanged(state)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying fold state observer", e)
            }
        }
    }
}

/**
 * Component types for responsive design
 */
enum class ComponentType {
    CODE_EDITOR, FILE_TREE, TOOLBAR, TERMINAL, SETTINGS_PANEL
}

/**
 * Text types for responsive sizing
 */
enum class TextType {
    CODE_EDITOR, HEADING, BODY, BUTTON, MENU
}

/**
 * Grid component types
 */
enum class GridComponent {
    FILE_LIST, TOOL_PANEL, FILE_TABS
}

/**
 * Animation types
 */
enum class AnimationType {
    TRANSITION, HOVER, CLICK, EXPAND_COLLAPSE, SLIDE_IN, FADE_IN
}

/**
 * Responsive dimensions data class
 */
data class ResponsiveDimensions(
    val width: Int,
    val height: Int,
    val minWidth: Int = 0,
    val minHeight: Int = 0,
    val maxWidth: Int = Int.MAX_VALUE,
    val maxHeight: Int = Int.MAX_VALUE
)