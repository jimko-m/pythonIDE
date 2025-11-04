package com.pythonide.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Screen adapter for responsive UI components
 * Handles dynamic layout adaptation, component resizing, and orientation changes
 */
class ScreenAdapter(private val context: Context) {
    companion object {
        private const val TAG = "ScreenAdapter"
        
        // Screen size constants (in dp)
        private const val PHONE_SMALL = 320
        private const val PHONE_NORMAL = 360
        private const val PHONE_LARGE = 411
        private const val TABLET_SMALL = 600
        private const val TABLET_LARGE = 800
        
        // Animation duration constants
        private const val LAYOUT_CHANGE_ANIMATION_DURATION = 300L
        private const val COMPONENT_RESIZE_ANIMATION_DURATION = 200L
        
        // Component constraints
        private const val MIN_TOUCH_TARGET_SIZE = 48
        private const val MAX_COLUMNS = 4
        private const val MAX_ROWS = 6
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    private val _screenDimensions = MutableStateFlow(getScreenDimensions())
    val screenDimensions: StateFlow<ScreenDimensions> = _screenDimensions.asStateFlow()
    
    private val _orientation = MutableStateFlow(getOrientation())
    val orientation: StateFlow<Orientation> = _orientation.asStateFlow()
    
    private val _layoutMode = MutableStateFlow(getLayoutMode())
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()
    
    private var observers = mutableListOf<ScreenAdapterObserver>()
    
    // Component layout configurations
    private val componentLayouts = mutableMapOf<ComponentType, ComponentLayout>()
    
    // Animation utilities
    private val layoutTransitionAnim = AnimationUtils.loadLayoutAnimation(context, android.R.anim.layout_animation_fade)
    
    /**
     * Screen dimensions data class
     */
    data class ScreenDimensions(
        val width: Int,
        val height: Int,
        val density: Float,
        val widthDp: Int,
        val heightDp: Int,
        val smallestWidthDp: Int,
        val screenSizeCategory: ScreenSizeCategory
    )
    
    /**
     * Orientation enum
     */
    enum class Orientation {
        PORTRAIT, LANDSCAPE, UNKNOWN
    }
    
    /**
     * Screen size categories
     */
    enum class ScreenSizeCategory {
        SMALL_PHONE, NORMAL_PHONE, LARGE_PHONE, SMALL_TABLET, LARGE_TABLET, TV, UNKNOWN
    }
    
    /**
     * Layout modes
     */
    enum class LayoutMode {
        SINGLE_PANE, DUAL_PANE, GRID, TV_LAYOUT, COMPACT, NORMAL, EXPANDED
    }
    
    /**
     * Component types for layout
     */
    enum class ComponentType {
        CODE_EDITOR, FILE_EXPLORER, TERMINAL, TOOLBAR, MENU, STATUS_BAR, TABS, SETTINGS_PANEL
    }
    
    /**
     * Component layout configuration
     */
    data class ComponentLayout(
        val componentType: ComponentType,
        var width: Int = 0,
        var height: Int = 0,
        var weight: Float = 1.0f,
        var alignment: Int = Gravity.CENTER,
        var margins: Margins = Margins(),
        var padding: Margins = Margins(),
        var isVisible: Boolean = true,
        var zIndex: Int = 0
    )
    
    /**
     * Margins data class
     */
    data class Margins(
        val left: Int = 0,
        val top: Int = 0,
        val right: Int = 0,
        val bottom: Int = 0
    ) {
        fun toMarginArray(): IntArray = intArrayOf(left, top, right, bottom)
    }
    
    /**
     * Layout constraints
     */
    data class LayoutConstraints(
        val maxWidth: Int = Int.MAX_VALUE,
        val maxHeight: Int = Int.MAX_VALUE,
        val minWidth: Int = 0,
        val minHeight: Int = 0,
        val aspectRatio: Float? = null,
        val allowWrap: Boolean = false
    )
    
    /**
     * Screen adapter observer interface
     */
    interface ScreenAdapterObserver {
        fun onScreenDimensionsChanged(dimensions: ScreenDimensions)
        fun onOrientationChanged(orientation: Orientation)
        fun onLayoutModeChanged(mode: LayoutMode)
        fun onComponentLayoutChanged(componentType: ComponentType, layout: ComponentLayout)
        fun onLayoutUpdateCompleted(totalComponents: Int)
    }

    /**
     * Initialize screen adapter
     */
    init {
        updateScreenInfo()
        setupDefaultLayouts()
    }

    /**
     * Update screen information
     */
    fun updateScreenInfo() {
        val display = windowManager.defaultDisplay
        display.getMetrics(displayMetrics)
        
        val newDimensions = getScreenDimensions()
        val newOrientation = getOrientation()
        val newLayoutMode = getLayoutMode(newDimensions, newOrientation)
        
        var layoutModeChanged = false
        
        if (_screenDimensions.value != newDimensions) {
            _screenDimensions.value = newDimensions
            notifyScreenDimensionsChanged(newDimensions)
        }
        
        if (_orientation.value != newOrientation) {
            _orientation.value = newOrientation
            notifyOrientationChanged(newOrientation)
        }
        
        if (_layoutMode.value != newLayoutMode) {
            _layoutMode.value = newLayoutMode
            layoutModeChanged = true
            notifyLayoutModeChanged(newLayoutMode)
        }
        
        if (layoutModeChanged) {
            adjustLayoutsForNewMode()
        }
    }

    /**
     * Get responsive layout parameters for a component
     */
    @JvmOverloads
    fun getResponsiveLayoutParams(
        componentType: ComponentType,
        constraints: LayoutConstraints = LayoutConstraints()
    ): LayoutParams {
        val dimensions = _screenDimensions.value
        val layoutMode = _layoutMode.value
        val orientation = _orientation.value
        
        val params = when (componentType) {
            ComponentType.CODE_EDITOR -> getCodeEditorLayoutParams(layoutMode, orientation, constraints)
            ComponentType.FILE_EXPLORER -> getFileExplorerLayoutParams(layoutMode, orientation, constraints)
            ComponentType.TERMINAL -> getTerminalLayoutParams(layoutMode, orientation, constraints)
            ComponentType.TOOLBAR -> getToolbarLayoutParams(layoutMode, orientation, constraints)
            ComponentType.MENU -> getMenuLayoutParams(layoutMode, orientation, constraints)
            ComponentType.STATUS_BAR -> getStatusBarLayoutParams(layoutMode, orientation, constraints)
            ComponentType.TABS -> getTabsLayoutParams(layoutMode, orientation, constraints)
            ComponentType.SETTINGS_PANEL -> getSettingsPanelLayoutParams(layoutMode, orientation, constraints)
        }
        
        // Update component layout configuration
        updateComponentLayout(componentType, params)
        
        return params
    }

    /**
     * Create a view with adaptive layout
     */
    fun createAdaptiveView(
        componentType: ComponentType,
        layout: ViewGroup,
        constraints: LayoutConstraints = LayoutConstraints()
    ): View {
        val dimensions = _screenDimensions.value
        val layoutMode = _layoutMode.value
        
        val view = when (componentType) {
            ComponentType.CODE_EDITOR -> createCodeEditorView(layout, dimensions, layoutMode)
            ComponentType.FILE_EXPLORER -> createFileExplorerView(layout, dimensions, layoutMode)
            ComponentType.TERMINAL -> createTerminalView(layout, dimensions, layoutMode)
            ComponentType.TOOLBAR -> createToolbarView(layout, dimensions, layoutMode)
            ComponentType.MENU -> createMenuView(layout, dimensions, layoutMode)
            else -> createDefaultView(layout, componentType)
        }
        
        val params = getResponsiveLayoutParams(componentType, constraints)
        view.layoutParams = params
        
        return view
    }

    /**
     * Get grid layout configuration
     */
    fun getGridLayoutConfig(componentType: ComponentType): GridLayoutConfig {
        val dimensions = _screenDimensions.value
        val orientation = _orientation.value
        
        return when (componentType) {
            ComponentType.CODE_EDITOR -> GridLayoutConfig(
                rows = if (orientation == Orientation.PORTRAIT) 1 else 1,
                columns = if (orientation == Orientation.PORTRAIT) 1 else 2,
                rowWeight = if (orientation == Orientation.PORTRAIT) 1.0f else 0.7f,
                columnWeight = if (orientation == Orientation.PORTRAIT) 1.0f else 0.3f
            )
            ComponentType.FILE_EXPLORER -> GridLayoutConfig(
                rows = if (orientation == Orientation.PORTRAIT) 2 else 1,
                columns = if (orientation == Orientation.PORTRAIT) 1 else 2,
                rowWeight = if (orientation == Orientation.PORTRAIT) 0.3f else 1.0f,
                columnWeight = if (orientation == Orientation.PORTRAIT) 1.0f else 0.3f
            )
            ComponentType.TERMINAL -> GridLayoutConfig(
                rows = if (orientation == Orientation.PORTRAIT) 2 else 1,
                columns = if (orientation == Orientation.PORTRAIT) 1 else 1,
                rowWeight = if (orientation == Orientation.PORTRAIT) 0.3f else 0.3f,
                columnWeight = 1.0f
            )
            else -> GridLayoutConfig(1, 1, 1.0f, 1.0f)
        }
    }

    /**
     * Get animation for layout changes
     */
    fun getLayoutChangeAnimation(): Animation {
        return AnimationUtils.loadAnimation(context, android.R.anim.fade_in).apply {
            duration = LAYOUT_CHANGE_ANIMATION_DURATION
        }
    }

    /**
     * Check if component should be visible on current screen
     */
    fun shouldShowComponent(componentType: ComponentType): Boolean {
        val dimensions = _screenDimensions.value
        val layoutMode = _layoutMode.value
        
        return when (componentType) {
            ComponentType.CODE_EDITOR -> true // Always visible
            ComponentType.FILE_EXPLORER -> dimensions.widthDp >= 400
            ComponentType.TERMINAL -> dimensions.widthDp >= 350 && layoutMode != LayoutMode.COMPACT
            ComponentType.TOOLBAR -> true // Always visible
            ComponentType.MENU -> layoutMode != LayoutMode.COMPACT || dimensions.widthDp >= 300
            ComponentType.STATUS_BAR -> true // Always visible
            ComponentType.TABS -> dimensions.widthDp >= 250
            ComponentType.SETTINGS_PANEL -> layoutMode != LayoutMode.COMPACT
        }
    }

    /**
     * Get optimal text size for current screen
     */
    fun getOptimalTextSize(baseSize: Float): Float {
        val dimensions = _screenDimensions.value
        
        return when (dimensions.screenSizeCategory) {
            ScreenSizeCategory.SMALL_PHONE -> baseSize * 0.9f
            ScreenSizeCategory.NORMAL_PHONE -> baseSize
            ScreenSizeCategory.LARGE_PHONE -> baseSize * 1.1f
            ScreenSizeCategory.SMALL_TABLET -> baseSize * 1.2f
            ScreenSizeCategory.LARGE_TABLET -> baseSize * 1.3f
            ScreenSizeCategory.TV -> baseSize * 1.5f
            ScreenSizeCategory.UNKNOWN -> baseSize
        }
    }

    /**
     * Get touch target size for current screen
     */
    fun getTouchTargetSize(): Int {
        val dimensions = _screenDimensions.value
        
        return when (dimensions.screenSizeCategory) {
            ScreenSizeCategory.TV -> 80
            ScreenSizeCategory.LARGE_TABLET -> 64
            ScreenSizeCategory.SMALL_TABLET -> 56
            else -> MIN_TOUCH_TARGET_SIZE
        }
    }

    /**
     * Add observer
     */
    fun addObserver(observer: ScreenAdapterObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    /**
     * Remove observer
     */
    fun removeObserver(observer: ScreenAdapterObserver) {
        observers.remove(observer)
    }

    /**
     * Get current screen info summary
     */
    fun getScreenInfoSummary(): String {
        val dimensions = _screenDimensions.value
        val orientation = _orientation.value
        val layoutMode = _layoutMode.value
        
        return """
            Screen Info:
            Size: ${dimensions.width} x ${dimensions.height} (${dimensions.widthDp}x${dimensions.heightDp}dp)
            Category: ${dimensions.screenSizeCategory}
            Orientation: $orientation
            Layout Mode: $layoutMode
            Density: ${dimensions.density}
        """.trimIndent()
    }

    // Private methods
    private fun getScreenDimensions(): ScreenDimensions {
        val display = windowManager.defaultDisplay
        display.getMetrics(displayMetrics)
        
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.density
        val widthDp = (width / density).toInt()
        val heightDp = (height / density).toInt()
        val smallestWidthDp = min(widthDp, heightDp)
        
        val screenSizeCategory = when (smallestWidthDp) {
            in 0..PHONE_SMALL -> ScreenSizeCategory.SMALL_PHONE
            in (PHONE_SMALL + 1)..PHONE_LARGE -> ScreenSizeCategory.LARGE_PHONE
            in (PHONE_LARGE + 1)..TABLET_SMALL -> ScreenSizeCategory.NORMAL_PHONE
            in (TABLET_SMALL + 1)..TABLET_LARGE -> ScreenSizeCategory.SMALL_TABLET
            in (TABLET_LARGE + 1)..Int.MAX_VALUE -> ScreenSizeCategory.LARGE_TABLET
            else -> ScreenSizeCategory.UNKNOWN
        }
        
        // Special check for TV
        val isTV = try {
            context.packageManager.hasSystemFeature("android.software.leanback")
        } catch (e: Exception) {
            false
        }
        
        val finalCategory = if (isTV) ScreenSizeCategory.TV else screenSizeCategory
        
        return ScreenDimensions(
            width = width,
            height = height,
            density = density,
            widthDp = widthDp,
            heightDp = heightDp,
            smallestWidthDp = smallestWidthDp,
            screenSizeCategory = finalCategory
        )
    }

    private fun getOrientation(): Orientation {
        return when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> Orientation.PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE -> Orientation.LANDSCAPE
            else -> Orientation.UNKNOWN
        }
    }

    private fun getLayoutMode(dimensions: ScreenDimensions = _screenDimensions.value, orientation: Orientation = _orientation.value): LayoutMode {
        return when {
            dimensions.screenSizeCategory == ScreenSizeCategory.TV -> {
                if (orientation == Orientation.LANDSCAPE) LayoutMode.TV_LAYOUT else LayoutMode.NORMAL
            }
            dimensions.smallestWidthDp >= TABLET_SMALL -> {
                if (orientation == Orientation.LANDSCAPE) LayoutMode.DUAL_PANE else LayoutMode.GRID
            }
            dimensions.smallestWidthDp >= 400 -> {
                if (orientation == Orientation.LANDSCAPE) LayoutMode.DUAL_PANE else LayoutMode.NORMAL
            }
            dimensions.smallestWidthDp < 320 -> LayoutMode.COMPACT
            else -> LayoutMode.SINGLE_PANE
        }
    }

    private fun setupDefaultLayouts() {
        // Initialize default component layouts
        ComponentType.values().forEach { type ->
            componentLayouts[type] = ComponentLayout(componentType = type)
        }
    }

    private fun adjustLayoutsForNewMode() {
        val layoutMode = _layoutMode.value
        val orientation = _orientation.value
        
        when (layoutMode) {
            LayoutMode.TV_LAYOUT -> setupTVLayout()
            LayoutMode.DUAL_PANE -> setupDualPaneLayout(orientation)
            LayoutMode.GRID -> setupGridLayout(orientation)
            LayoutMode.SINGLE_PANE -> setupSinglePaneLayout(orientation)
            LayoutMode.COMPACT -> setupCompactLayout()
            LayoutMode.NORMAL -> setupNormalLayout()
            LayoutMode.EXPANDED -> setupExpandedLayout()
        }
        
        notifyLayoutUpdateCompleted(componentLayouts.size)
    }

    private fun setupTVLayout() {
        // TV layout - optimized for large screens and remote control
        componentLayouts[ComponentType.CODE_EDITOR]?.apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
            weight = 0.7f
            zIndex = 0
        }
        
        componentLayouts[ComponentType.FILE_EXPLORER]?.apply {
            width = 400
            height = LayoutParams.MATCH_PARENT
            weight = 0.3f
            zIndex = 1
        }
    }

    private fun setupDualPaneLayout(orientation: Orientation) {
        when (orientation) {
            Orientation.PORTRAIT -> {
                componentLayouts[ComponentType.CODE_EDITOR]?.apply {
                    width = LayoutParams.MATCH_PARENT
                    height = 0
                    weight = 0.6f
                }
                componentLayouts[ComponentType.FILE_EXPLORER]?.apply {
                    width = LayoutParams.MATCH_PARENT
                    height = 0
                    weight = 0.4f
                }
            }
            Orientation.LANDSCAPE -> {
                componentLayouts[ComponentType.CODE_EDITOR]?.apply {
                    width = 0
                    height = LayoutParams.MATCH_PARENT
                    weight = 0.7f
                }
                componentLayouts[ComponentType.FILE_EXPLORER]?.apply {
                    width = 0
                    height = LayoutParams.MATCH_PARENT
                    weight = 0.3f
                }
            }
            else -> { /* Handle unknown orientation */ }
        }
    }

    private fun setupGridLayout(orientation: Orientation) {
        componentLayouts[ComponentType.CODE_EDITOR]?.apply {
            width = 0
            height = 0
            weight = 0.6f
            alignment = Gravity.CENTER
        }
        
        componentLayouts[ComponentType.FILE_EXPLORER]?.apply {
            width = 0
            height = 0
            weight = 0.4f
            alignment = Gravity.CENTER
        }
    }

    private fun setupSinglePaneLayout(orientation: Orientation) {
        componentLayouts[ComponentType.CODE_EDITOR]?.apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
            weight = 1.0f
        }
        
        // Other components in single pane mode
        componentLayouts[ComponentType.FILE_EXPLORER]?.apply {
            isVisible = false
        }
    }

    private fun setupCompactLayout() {
        // Minimize components for very small screens
        ComponentType.values().forEach { type ->
            componentLayouts[type]?.apply {
                when (type) {
                    ComponentType.CODE_EDITOR -> {
                        width = LayoutParams.MATCH_PARENT
                        height = LayoutParams.MATCH_PARENT
                        weight = 1.0f
                    }
                    ComponentType.TOOLBAR -> {
                        width = LayoutParams.MATCH_PARENT
                        height = LayoutParams.WRAP_CONTENT
                        weight = 0f
                    }
                    else -> {
                        isVisible = false
                    }
                }
            }
        }
    }

    private fun setupNormalLayout() {
        ComponentType.values().forEach { type ->
            componentLayouts[type]?.apply {
                isVisible = true
                weight = getDefaultWeight(type)
            }
        }
    }

    private fun setupExpandedLayout() {
        // Expanded layout for large screens
        ComponentType.values().forEach { type ->
            componentLayouts[type]?.apply {
                isVisible = true
                weight = getDefaultWeight(type) * 1.2f
            }
        }
    }

    private fun getDefaultWeight(componentType: ComponentType): Float {
        return when (componentType) {
            ComponentType.CODE_EDITOR -> 0.6f
            ComponentType.FILE_EXPLORER -> 0.3f
            ComponentType.TERMINAL -> 0.2f
            ComponentType.TOOLBAR -> 0f
            ComponentType.MENU -> 0f
            ComponentType.STATUS_BAR -> 0f
            ComponentType.TABS -> 0f
            ComponentType.SETTINGS_PANEL -> 0.4f
        }
    }

    private fun getCodeEditorLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        val width = when (layoutMode) {
            LayoutMode.TV_LAYOUT -> LayoutParams.MATCH_PARENT
            LayoutMode.DUAL_PANE -> if (orientation == Orientation.PORTRAIT) LayoutParams.MATCH_PARENT else 0
            LayoutMode.GRID -> 0
            LayoutMode.SINGLE_PANE -> LayoutParams.MATCH_PARENT
            LayoutMode.COMPACT -> LayoutParams.MATCH_PARENT
            LayoutMode.NORMAL -> LayoutParams.MATCH_PARENT
            LayoutMode.EXPANDED -> LayoutParams.MATCH_PARENT
        }
        
        val height = when (orientation) {
            Orientation.PORTRAIT -> 0
            Orientation.LANDSCAPE -> LayoutParams.MATCH_PARENT
            else -> LayoutParams.MATCH_PARENT
        }
        
        return LayoutParams(width, height).apply {
            weight = getWeightForComponent(ComponentType.CODE_EDITOR, layoutMode)
        }
    }

    private fun getFileExplorerLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        val width = when {
            layoutMode == LayoutMode.TV_LAYOUT -> 400
            layoutMode == LayoutMode.DUAL_PANE && orientation == Orientation.LANDSCAPE -> 0
            layoutMode == LayoutMode.SINGLE_PANE || layoutMode == LayoutMode.COMPACT -> LayoutParams.MATCH_PARENT
            else -> 0
        }
        
        val height = when {
            layoutMode == LayoutMode.SINGLE_PANE && orientation == Orientation.PORTRAIT -> 0
            layoutMode == LayoutMode.COMPACT -> LayoutParams.WRAP_CONTENT
            else -> LayoutParams.MATCH_PARENT
        }
        
        return LayoutParams(width, height).apply {
            weight = getWeightForComponent(ComponentType.FILE_EXPLORER, layoutMode)
        }
    }

    private fun getTerminalLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
            weight = 0.3f
        }
    }

    private fun getToolbarLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun getMenuLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        return if (layoutMode == LayoutMode.COMPACT) {
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        } else {
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
    }

    private fun getStatusBarLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, 100) // Fixed height for status bar
    }

    private fun getTabsLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun getSettingsPanelLayoutParams(layoutMode: LayoutMode, orientation: Orientation, constraints: LayoutConstraints): LayoutParams {
        return if (layoutMode == LayoutMode.COMPACT) {
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        } else {
            LayoutParams(400, LayoutParams.MATCH_PARENT)
        }
    }

    private fun getWeightForComponent(componentType: ComponentType, layoutMode: LayoutMode): Float {
        return componentLayouts[componentType]?.weight ?: 1.0f
    }

    private fun updateComponentLayout(componentType: ComponentType, params: LayoutParams) {
        val existingLayout = componentLayouts[componentType]
        if (existingLayout != null) {
            existingLayout.width = params.width
            existingLayout.height = params.height
            existingLayout.weight = params.weight
            
            notifyComponentLayoutChanged(componentType, existingLayout)
        }
    }

    private fun createCodeEditorView(layout: ViewGroup, dimensions: ScreenDimensions, layoutMode: LayoutMode): View {
        // Create code editor view with adaptive sizing
        val view = View(context)
        view.id = View.generateViewId()
        
        // Apply appropriate styling based on screen size
        view.setBackgroundColor(android.graphics.Color.BLACK)
        
        return view
    }

    private fun createFileExplorerView(layout: ViewGroup, dimensions: ScreenDimensions, layoutMode: LayoutMode): View {
        val view = View(context)
        view.id = View.generateViewId()
        view.setBackgroundColor(android.graphics.Color.DKGRAY)
        return view
    }

    private fun createTerminalView(layout: ViewGroup, dimensions: ScreenDimensions, layoutMode: LayoutMode): View {
        val view = View(context)
        view.id = View.generateViewId()
        view.setBackgroundColor(android.graphics.Color.BLACK)
        return view
    }

    private fun createToolbarView(layout: ViewGroup, dimensions: ScreenDimensions, layoutMode: LayoutMode): View {
        val view = View(context)
        view.id = View.generateViewId()
        view.setBackgroundColor(android.graphics.Color.GRAY)
        return view
    }

    private fun createMenuView(layout: ViewGroup, dimensions: ScreenDimensions, layoutMode: LayoutMode): View {
        val view = View(context)
        view.id = View.generateViewId()
        view.setBackgroundColor(android.graphics.Color.LTGRAY)
        return view
    }

    private fun createDefaultView(layout: ViewGroup, componentType: ComponentType): View {
        val view = View(context)
        view.id = View.generateViewId()
        return view
    }

    private fun notifyScreenDimensionsChanged(dimensions: ScreenDimensions) {
        observers.forEach { observer ->
            try {
                observer.onScreenDimensionsChanged(dimensions)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying screen dimensions observer", e)
            }
        }
    }

    private fun notifyOrientationChanged(orientation: Orientation) {
        observers.forEach { observer ->
            try {
                observer.onOrientationChanged(orientation)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying orientation observer", e)
            }
        }
    }

    private fun notifyLayoutModeChanged(layoutMode: LayoutMode) {
        observers.forEach { observer ->
            try {
                observer.onLayoutModeChanged(layoutMode)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying layout mode observer", e)
            }
        }
    }

    private fun notifyComponentLayoutChanged(componentType: ComponentType, layout: ComponentLayout) {
        observers.forEach { observer ->
            try {
                observer.onComponentLayoutChanged(componentType, layout)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying component layout observer", e)
            }
        }
    }

    private fun notifyLayoutUpdateCompleted(totalComponents: Int) {
        observers.forEach { observer ->
            try {
                observer.onLayoutUpdateCompleted(totalComponents)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying layout update observer", e)
            }
        }
    }
}

/**
 * Grid layout configuration data class
 */
data class GridLayoutConfig(
    val rows: Int,
    val columns: Int,
    val rowWeight: Float,
    val columnWeight: Float
)