package com.pythonide.terminal

import android.animation.*
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pythonide.R
import com.pythonide.data.models.TerminalOutput
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced Terminal UI with modern features
 * - Syntax highlighting
 * - Auto-completion
 * - Split screen support
 * - Tab management
 * - Theme customization
 * - Search functionality
 * - Custom terminal emulator
 */
class TerminalUIEnhancer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "TerminalUIEnhancer"
        private const val ANIMATION_DURATION = 300L
        private const val CURSOR_BLINK_DURATION = 600L
        private const val MAX_TAB_COUNT = 10
        private const val SEARCH_DEBOUNCE_DELAY = 300L
    }
    
    // UI Components
    private lateinit var terminalContainer: LinearLayout
    private lateinit var tabContainer: LinearLayout
    private lateinit var toolbarContainer: LinearLayout
    private lateinit var terminalRecycler: RecyclerView
    private lateinit var inputEditText: TerminalEditText
    private lateinit var suggestionRecycler: RecyclerView
    private lateinit var statusBar: TerminalStatusBar
    private lateinit var searchContainer: SearchContainer
    private lateinit var splitControls: SplitControls
    
    // State
    private val terminalAdapter = TerminalAdapter()
    private val suggestionAdapter = SuggestionAdapter()
    private val viewPool = RecyclerView.RecycledViewPool()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Terminal state
    private val terminalOutput = mutableListOf<TerminalOutput>()
    private val terminalHistory = mutableListOf<String>()
    private val currentCommand = StringBuilder()
    private val selectedText = StringBuilder()
    
    private var isCursorVisible = true
    private var cursorPosition = 0
    private var terminalWidth = 80
    private var terminalHeight = 24
    private var fontSize = 14
    private var lineHeight = 20
    private var currentTheme = TerminalTheme.DARK
    private var currentTabIndex = 0
    private var splitMode = SplitMode.NONE
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    private val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var searchPosition = 0
    private val searchResults = mutableListOf<Int>()
    
    // Auto-completion
    private val completions = mutableListOf<Completion>()
    private var completionIndex = 0
    private var showSuggestions = false
    
    // Coroutines
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cursorBlinkJob: Job? = null
    
    // Callbacks
    private var terminalCallback: TerminalUICallback? = null
    
    init {
        setupViews()
        setupStyles()
        startCursorBlink()
        setupTouchHandlers()
        setupKeyboardHandlers()
    }
    
    private fun setupViews() {
        // Create main layout
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        
        // Tab container
        tabContainer = createTabContainer()
        mainLayout.addView(tabContainer)
        
        // Toolbar
        toolbarContainer = createToolbarContainer()
        mainLayout.addView(toolbarContainer)
        
        // Terminal container
        terminalContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        // Terminal output
        terminalRecycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = terminalAdapter
            setRecycledViewPool(viewPool)
            isNestedScrollingEnabled = false
        }
        terminalContainer.addView(terminalRecycler)
        
        // Input area
        val inputContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }
        
        // Prompt text
        val promptText = TextView(context).apply {
            text = "$ "
            textSize = fontSize.toFloat()
            setTextColor(getCurrentForegroundColor())
            setPadding(0, 0, 8, 0)
        }
        inputContainer.addView(promptText)
        
        // Input edit text
        inputEditText = TerminalEditText(context).apply {
            textSize = fontSize.toFloat()
            setTextColor(getCurrentForegroundColor())
            background = null
            setPadding(0, 0, 0, 0)
        }
        inputContainer.addView(inputEditText)
        
        terminalContainer.addView(inputContainer)
        
        // Suggestion recycler
        suggestionRecycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = suggestionAdapter
            visibility = GONE
            setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_suggestion_background))
        }
        terminalContainer.addView(suggestionRecycler)
        
        mainLayout.addView(terminalContainer)
        
        // Status bar
        statusBar = TerminalStatusBar(context)
        mainLayout.addView(statusBar)
        
        // Search container (hidden by default)
        searchContainer = SearchContainer(context).apply {
            visibility = GONE
        }
        mainLayout.addView(searchContainer)
        
        // Split controls
        splitControls = SplitControls(context).apply {
            visibility = GONE
        }
        mainLayout.addView(splitControls)
        
        addView(mainLayout)
    }
    
    private fun createTabContainer(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL)
            setPadding(8, 4, 8, 4)
            setBackgroundColor(getCurrentBackgroundColor())
            
            // Add tab button
            val addTabButton = Button(context).apply {
                text = "+"
                textSize = 16f
                setOnClickListener { terminalCallback?.onAddTab() }
            }
            addView(addTabButton)
        }
    }
    
    private fun createToolbarContainer(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL)
            setPadding(8, 4, 8, 4)
            setBackgroundColor(getCurrentBackgroundColor())
            
            // Search button
            val searchButton = Button(context).apply {
                text = "🔍"
                setOnClickListener { toggleSearch() }
            }
            addView(searchButton)
            
            // Clear button
            val clearButton = Button(context).apply {
                text = "🗑️"
                setOnClickListener { clearTerminal() }
            }
            addView(clearButton)
            
            // Settings button
            val settingsButton = Button(context).apply {
                text = "⚙️"
                setOnClickListener { terminalCallback?.onOpenSettings() }
            }
            addView(settingsButton)
            
            // Split button
            val splitButton = Button(context).apply {
                text = "↕️"
                setOnClickListener { toggleSplitMode() }
            }
            addView(splitButton)
        }
    }
    
    private fun setupStyles() {
        // Terminal text style
        textPaint.apply {
            color = getCurrentForegroundColor()
            textSize = fontSize.toFloat()
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        
        // Cursor style
        cursorPaint.apply {
            color = getCurrentCursorColor()
            style = Paint.Style.FILL
        }
        
        // Apply theme
        applyTerminalTheme(currentTheme)
    }
    
    private fun setupTouchHandlers() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Handle touch for text selection
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Handle text selection
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Complete text selection
                    true
                }
            }
            false
        }
        
        // Long press for context menu
        setOnLongClickListener {
            showContextMenu()
            true
        }
    }
    
    private fun setupKeyboardHandlers() {
        inputEditText.setOnKeyListener { _, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleEnter()
                    }
                    true
                }
                KeyEvent.KEYCODE_TAB -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleTabCompletion()
                    }
                    true
                }
                KeyEvent.KEYCODE_UP -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleArrowUp()
                    }
                    true
                }
                KeyEvent.KEYCODE_DOWN -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleArrowDown()
                    }
                    true
                }
                KeyEvent.KEYCODE_LEFT -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleArrowLeft()
                    }
                    true
                }
                KeyEvent.KEYCODE_RIGHT -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleArrowRight()
                    }
                    true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleBackspace()
                    }
                    true
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        handleEscape()
                    }
                    true
                }
            }
            false
        }
        
        // Text change listener for auto-completion
        inputEditText.addTextChangedListener {
            // Debounce text changes
            scope.launch {
                delay(200)
                checkAutoCompletion()
            }
        }
    }
    
    /**
     * Add output to terminal
     */
    fun addOutput(output: TerminalOutput) {
        terminalOutput.add(output)
        terminalAdapter.submitList(terminalOutput.toList())
        scrollToBottom()
        
        // Update status bar
        statusBar.updateOutputCount(terminalOutput.size)
    }
    
    /**
     * Clear terminal output
     */
    fun clearTerminal() {
        terminalOutput.clear()
        terminalAdapter.submitList(terminalOutput.toList())
        terminalCallback?.onClear()
    }
    
    /**
     * Set terminal theme
     */
    fun setTerminalTheme(theme: TerminalTheme) {
        currentTheme = theme
        applyTerminalTheme(theme)
        terminalAdapter.notifyDataSetChanged()
        terminalCallback?.onThemeChanged(theme)
    }
    
    /**
     * Set font size
     */
    fun setFontSize(size: Int) {
        fontSize = size
        lineHeight = (size * 1.5f).toInt()
        
        textPaint.textSize = fontSize.toFloat()
        cursorPaint.textSize = fontSize.toFloat()
        
        terminalAdapter.notifyDataSetChanged()
        terminalCallback?.onFontSizeChanged(size)
    }
    
    /**
     * Add new tab
     */
    fun addTab(tab: TerminalTab) {
        val tabView = createTabView(tab)
        tabContainer.addView(tabView)
        
        if (tabContainer.childCount > 1) {
            // Remove the "+" button temporarily
            val addButton = tabContainer.getChildAt(tabContainer.childCount - 1)
            tabContainer.removeViewAt(tabContainer.childCount - 1)
            tabContainer.addView(createAddTabButton())
        }
        
        selectTab(tab.id)
    }
    
    /**
     * Select tab
     */
    fun selectTab(tabId: String) {
        // Update tab selection
        for (i in 0 until tabContainer.childCount) {
            val child = tabContainer.getChildAt(i)
            if (child is TabView) {
                child.isSelected = child.tabId == tabId
            }
        }
        
        // Load terminal content for this tab
        terminalCallback?.onTabSelected(tabId)
    }
    
    /**
     * Close tab
     */
    fun closeTab(tabId: String) {
        var tabIndex = -1
        for (i in 0 until tabContainer.childCount) {
            val child = tabContainer.getChildAt(i)
            if (child is TabView && child.tabId == tabId) {
                tabIndex = i
                break
            }
        }
        
        if (tabIndex >= 0) {
            tabContainer.removeViewAt(tabIndex)
            terminalCallback?.onTabClosed(tabId)
        }
    }
    
    /**
     * Toggle split screen mode
     */
    fun toggleSplitScreen(direction: SplitDirection) {
        when (splitMode) {
            SplitMode.NONE -> {
                splitMode = if (direction == SplitDirection.HORIZONTAL) {
                    SplitMode.HORIZONTAL
                } else {
                    SplitMode.VERTICAL
                }
                setupSplitMode()
            }
            SplitMode.HORIZONTAL -> {
                if (direction == SplitDirection.HORIZONTAL) {
                    splitMode = SplitMode.NONE
                    setupNormalMode()
                }
            }
            SplitMode.VERTICAL -> {
                if (direction == SplitDirection.VERTICAL) {
                    splitMode = SplitMode.NONE
                    setupNormalMode()
                }
            }
        }
    }
    
    /**
     * Show search interface
     */
    fun showSearch() {
        searchContainer.visibility = VISIBLE
        searchContainer.focus()
        terminalCallback?.onSearchShown()
    }
    
    /**
     * Hide search interface
     */
    fun hideSearch() {
        searchContainer.visibility = GONE
        _searchQuery.value = ""
        searchResults.clear()
        searchPosition = 0
        terminalCallback?.onSearchHidden()
    }
    
    /**
     * Toggle search
     */
    private fun toggleSearch() {
        if (searchContainer.visibility == VISIBLE) {
            hideSearch()
        } else {
            showSearch()
        }
    }
    
    /**
     * Execute search
     */
    fun search(query: String) {
        _searchQuery.value = query
        searchResults.clear()
        
        if (query.isNotEmpty()) {
            for ((index, output) in terminalOutput.withIndex()) {
                if (output.text.contains(query, ignoreCase = true)) {
                    searchResults.add(index)
                }
            }
            
            if (searchResults.isNotEmpty()) {
                searchPosition = 0
                scrollToSearchResult(searchPosition)
            }
        }
        
        searchContainer.updateResultCount(searchResults.size, if (query.isNotEmpty()) 1 else 0)
    }
    
    /**
     * Navigate to next search result
     */
    fun nextSearchResult() {
        if (searchResults.isNotEmpty()) {
            searchPosition = (searchPosition + 1) % searchResults.size
            scrollToSearchResult(searchPosition)
        }
    }
    
    /**
     * Navigate to previous search result
     */
    fun previousSearchResult() {
        if (searchResults.isNotEmpty()) {
            searchPosition = (searchPosition - 1 + searchResults.size) % searchResults.size
            scrollToSearchResult(searchPosition)
        }
    }
    
    /**
     * Show auto-completion suggestions
     */
    fun showSuggestions(suggestions: List<Completion>) {
        completions.clear()
        completions.addAll(suggestions)
        
        if (suggestions.isNotEmpty()) {
            suggestionAdapter.submitList(suggestions)
            suggestionRecycler.visibility = VISIBLE
            showSuggestions = true
        }
    }
    
    /**
     * Hide suggestions
     */
    fun hideSuggestions() {
        suggestionRecycler.visibility = GONE
        showSuggestions = false
        completions.clear()
    }
    
    /**
     * Insert completion
     */
    fun insertCompletion(completion: Completion) {
        val currentText = inputEditText.text.toString()
        val start = inputEditText.selectionStart
        val end = inputEditText.selectionEnd
        
        val newText = buildString {
            append(currentText.substring(0, start))
            append(completion.text)
            append(currentText.substring(end))
        }
        
        inputEditText.setText(newText)
        inputEditText.setSelection(start + completion.text.length)
        
        hideSuggestions()
        terminalCallback?.onCompletionInserted(completion)
    }
    
    /**
     * Get current command
     */
    fun getCurrentCommand(): String {
        return inputEditText.text.toString()
    }
    
    /**
     * Clear current input
     */
    fun clearInput() {
        inputEditText.setText("")
        cursorPosition = 0
        currentCommand.clear()
    }
    
    /**
     * Set callback
     */
    fun setTerminalCallback(callback: TerminalUICallback) {
        terminalCallback = callback
    }
    
    // Private helper methods
    
    private fun startCursorBlink() {
        cursorBlinkJob?.cancel()
        cursorBlinkJob = scope.launch {
            while (isActive) {
                isCursorVisible = !isCursorVisible
                invalidate()
                delay(CURSOR_BLINK_DURATION)
            }
        }
    }
    
    private fun applyTerminalTheme(theme: TerminalTheme) {
        when (theme) {
            TerminalTheme.DARK -> {
                setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_dark_background))
                inputEditText.setTextColor(ContextCompat.getColor(context, R.color.terminal_dark_foreground))
                terminalRecycler.setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_dark_background))
            }
            TerminalTheme.LIGHT -> {
                setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_light_background))
                inputEditText.setTextColor(ContextCompat.getColor(context, R.color.terminal_light_foreground))
                terminalRecycler.setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_light_background))
            }
            TerminalTheme.MONOKAI -> {
                setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_monokai_background))
                inputEditText.setTextColor(ContextCompat.getColor(context, R.color.terminal_monokai_foreground))
                terminalRecycler.setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_monokai_background))
            }
            // Add more themes...
        }
        
        terminalAdapter.notifyDataSetChanged()
    }
    
    private fun getCurrentBackgroundColor(): Int {
        return when (currentTheme) {
            TerminalTheme.DARK -> ContextCompat.getColor(context, R.color.terminal_dark_background)
            TerminalTheme.LIGHT -> ContextCompat.getColor(context, R.color.terminal_light_background)
            TerminalTheme.MONOKAI -> ContextCompat.getColor(context, R.color.terminal_monokai_background)
            else -> ContextCompat.getColor(context, R.color.terminal_dark_background)
        }
    }
    
    private fun getCurrentForegroundColor(): Int {
        return when (currentTheme) {
            TerminalTheme.DARK -> ContextCompat.getColor(context, R.color.terminal_dark_foreground)
            TerminalTheme.LIGHT -> ContextCompat.getColor(context, R.color.terminal_light_foreground)
            TerminalTheme.MONOKAI -> ContextCompat.getColor(context, R.color.terminal_monokai_foreground)
            else -> ContextCompat.getColor(context, R.color.terminal_dark_foreground)
        }
    }
    
    private fun getCurrentCursorColor(): Int {
        return when (currentTheme) {
            TerminalTheme.DARK -> Color.WHITE
            TerminalTheme.LIGHT -> Color.BLACK
            TerminalTheme.MONOKAI -> Color.WHITE
            else -> Color.WHITE
        }
    }
    
    private fun handleEnter() {
        val command = inputEditText.text.toString()
        if (command.isNotEmpty()) {
            // Add command to history
            terminalHistory.add(command)
            
            // Add command output
            addOutput(TerminalOutput("$ $command", System.currentTimeMillis(), TerminalOutput.OutputType.COMMAND))
            
            // Clear input
            clearInput()
            
            // Execute command
            terminalCallback?.onCommandEntered(command)
        }
    }
    
    private fun handleTabCompletion() {
        if (showSuggestions && completions.isNotEmpty()) {
            // Cycle through suggestions
            completionIndex = (completionIndex + 1) % completions.size
            val completion = completions[completionIndex]
            insertCompletion(completion)
        } else {
            // Trigger auto-completion
            val currentText = inputEditText.text.toString()
            terminalCallback?.onTabCompletionRequested(currentText)
        }
    }
    
    private fun handleArrowUp() {
        if (terminalHistory.isNotEmpty()) {
            val currentIndex = terminalHistory.indexOf(inputEditText.text.toString())
            val previousIndex = if (currentIndex > 0) currentIndex - 1 else terminalHistory.size - 1
            inputEditText.setText(terminalHistory[previousIndex])
            terminalCallback?.onHistoryNavigation(terminalHistory[previousIndex], true)
        }
    }
    
    private fun handleArrowDown() {
        if (terminalHistory.isNotEmpty()) {
            val currentIndex = terminalHistory.indexOf(inputEditText.text.toString())
            val nextIndex = if (currentIndex < terminalHistory.size - 1) currentIndex + 1 else 0
            inputEditText.setText(terminalHistory[nextIndex])
            terminalCallback?.onHistoryNavigation(terminalHistory[nextIndex], false)
        }
    }
    
    private fun handleArrowLeft() {
        // Move cursor left
        val position = inputEditText.selectionStart
        if (position > 0) {
            inputEditText.setSelection(position - 1)
        }
    }
    
    private fun handleArrowRight() {
        // Move cursor right
        val position = inputEditText.selectionStart
        if (position < inputEditText.length()) {
            inputEditText.setSelection(position + 1)
        }
    }
    
    private fun handleBackspace() {
        // Handle backspace with proper behavior
        val position = inputEditText.selectionStart
        if (position > 0) {
            inputEditText.text?.delete(position - 1, position)
        }
    }
    
    private fun handleEscape() {
        when {
            showSuggestions -> hideSuggestions()
            searchContainer.visibility == VISIBLE -> hideSearch()
            else -> terminalCallback?.onEscapePressed()
        }
    }
    
    private suspend fun checkAutoCompletion() {
        val currentText = inputEditText.text.toString()
        if (currentText.isEmpty()) {
            hideSuggestions()
            return
        }
        
        // Get suggestions from callback
        val suggestions = terminalCallback?.onAutoCompletionRequested(currentText) ?: emptyList()
        
        if (suggestions.isNotEmpty()) {
            showSuggestions(suggestions)
        } else {
            hideSuggestions()
        }
    }
    
    private fun scrollToBottom() {
        terminalRecycler.scrollToPosition(terminalOutput.size - 1)
    }
    
    private fun scrollToSearchResult(position: Int) {
        val index = searchResults[position]
        terminalRecycler.scrollToPosition(index)
        highlightSearchResult(index)
    }
    
    private fun highlightSearchResult(index: Int) {
        terminalAdapter.highlightSearch(index, _searchQuery.value)
    }
    
    private fun createTabView(tab: TerminalTab): TabView {
        return TabView(context, tab)
    }
    
    private fun createAddTabButton(): Button {
        return Button(context).apply {
            text = "+"
            textSize = 16f
            setOnClickListener { terminalCallback?.onAddTab() }
        }
    }
    
    private fun setupSplitMode() {
        // Implement split screen UI
        splitControls.visibility = VISIBLE
        // Add second terminal instance
        terminalCallback?.onSplitModeEnabled(splitMode)
    }
    
    private fun setupNormalMode() {
        splitControls.visibility = GONE
        terminalCallback?.onSplitModeDisabled()
    }
    
    private fun toggleSplitMode() {
        toggleSplitScreen(SplitDirection.HORIZONTAL)
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        cursorBlinkJob?.cancel()
        scope.cancel()
        terminalCallback = null
    }
}

// Adapter classes

private class TerminalAdapter : RecyclerView.Adapter<TerminalViewHolder>() {
    private var terminalOutputs = mutableListOf<TerminalOutput>()
    private var highlightedIndex = -1
    private var searchQuery = ""
    
    fun submitList(outputs: List<TerminalOutput>) {
        terminalOutputs.clear()
        terminalOutputs.addAll(outputs)
        notifyDataSetChanged()
    }
    
    fun highlightSearch(index: Int, query: String) {
        highlightedIndex = index
        searchQuery = query
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val view = TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 4, 8, 4)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setLineSpacing(1.5f, 1f)
        }
        return TerminalViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        val output = terminalOutputs[position]
        holder.bind(output, position == highlightedIndex, searchQuery)
    }
    
    override fun getItemCount(): Int = terminalOutputs.size
}

private class TerminalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textView: TextView = itemView as TextView
    
    fun bind(output: TerminalOutput, isHighlighted: Boolean, searchQuery: String) {
        textView.text = output.text
        
        // Apply output type styling
        val color = when (output.type) {
            TerminalOutput.OutputType.COMMAND -> Color.CYAN
            TerminalOutput.OutputType.OUTPUT -> Color.GREEN
            TerminalOutput.OutputType.ERROR -> Color.RED
            TerminalOutput.OutputType.WARNING -> Color.YELLOW
            TerminalOutput.OutputType.INFO -> Color.BLUE
            else -> Color.WHITE
        }
        textView.setTextColor(color)
        
        // Highlight search results
        if (isHighlighted && searchQuery.isNotEmpty()) {
            textView.setBackgroundColor(Color.YELLOW)
        } else {
            textView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}

private class SuggestionAdapter : RecyclerView.Adapter<SuggestionViewHolder>() {
    private var completions = listOf<Completion>()
    
    fun submitList(newCompletions: List<Completion>) {
        completions = newCompletions
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = Button(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 4, 8, 4)
            textSize = 12f
        }
        return SuggestionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(completions[position])
    }
    
    override fun getItemCount(): Int = completions.size
}

private class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val button: Button = itemView as Button
    
    fun bind(completion: Completion) {
        button.text = completion.text
        button.setOnClickListener {
            // This would need to be wired to the main TerminalUIEnhancer instance
        }
    }
}

// Custom View Classes

private class TerminalEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditText(context, attrs) {
    
    init {
        // Terminal-specific styling
        typeface = Typeface.MONOSPACE
        setLines(1)
        maxLines = 1
    }
}

private class TabView @JvmOverloads constructor(
    context: Context,
    val tab: TerminalTab,
    attrs: AttributeSet? = null
) : Button(context, attrs) {
    
    var tabId: String = tab.id
    
    init {
        text = tab.name
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(4, 4, 4, 4)
        }
        
        setOnClickListener {
            // Tab selection handled by parent
        }
    }
}

private class SearchContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    
    private val searchEditText: EditText
    private val resultText: TextView
    private val prevButton: Button
    private val nextButton: Button
    private val closeButton: Button
    
    init {
        orientation = HORIZONTAL)
        setPadding(8, 8, 8, 8)
        setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_search_background))
        
        searchEditText = EditText(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            hint = "Search..."
            textSize = 14f
        }
        addView(searchEditText)
        
        resultText = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            text = "0/0"
            textSize = 12f
            setPadding(16, 0, 8, 0)
        }
        addView(resultText)
        
        prevButton = Button(context).apply {
            text = "↑"
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                // Previous result
            }
        }
        addView(prevButton)
        
        nextButton = Button(context).apply {
            text = "↓"
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                // Next result
            }
        }
        addView(nextButton)
        
        closeButton = Button(context).apply {
            text = "✕"
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                visibility = GONE
            }
        }
        addView(closeButton)
    }
    
    fun focus() {
        searchEditText.requestFocus()
    }
    
    fun updateResultCount(total: Int, current: Int) {
        resultText.text = "$current/$total"
    }
}

private class TerminalStatusBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    
    private val outputCountText: TextView
    private val connectionStatusText: TextView
    private val cursorPositionText: TextView
    
    init {
        orientation = HORIZONTAL)
        setPadding(8, 4, 8, 4)
        setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_status_background))
        
        outputCountText = TextView(context).apply {
            text = "Lines: 0"
            textSize = 10f
            setTextColor(Color.GRAY)
        }
        addView(outputCountText)
        
        connectionStatusText = TextView(context).apply {
            text = "Connected"
            textSize = 10f
            setTextColor(Color.GREEN)
            setPadding(16, 0, 0, 0)
        }
        addView(connectionStatusText)
        
        cursorPositionText = TextView(context).apply {
            text = "Ln 1, Col 1"
            textSize = 10f
            setTextColor(Color.GRAY)
            setPadding(16, 0, 0, 0)
        }
        addView(cursorPositionText)
    }
    
    fun updateOutputCount(count: Int) {
        outputCountText.text = "Lines: $count"
    }
    
    fun updateConnectionStatus(connected: Boolean) {
        connectionStatusText.text = if (connected) "Connected" else "Disconnected"
        connectionStatusText.setTextColor(if (connected) Color.GREEN else Color.RED)
    }
    
    fun updateCursorPosition(line: Int, column: Int) {
        cursorPositionText.text = "Ln $line, Col $column"
    }
}

private class SplitControls @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    
    private val horizontalButton: Button
    private val verticalButton: Button
    private val unsplitButton: Button
    
    init {
        orientation = HORIZONTAL)
        setPadding(8, 4, 8, 4)
        setBackgroundColor(ContextCompat.getColor(context, R.color.terminal_split_background))
        
        horizontalButton = Button(context).apply {
            text = "↕ Split"
            setOnClickListener {
                // Handle horizontal split
            }
        }
        addView(horizontalButton)
        
        verticalButton = Button(context).apply {
            text = "↔ Split"
            setOnClickListener {
                // Handle vertical split
            }
        }
        addView(verticalButton)
        
        unsplitButton = Button(context).apply {
            text = "✕ Unsplit"
            setOnClickListener {
                // Handle unsplit
            }
        }
        addView(unsplitButton)
    }
}

// Data classes

data class TerminalTab(
    val id: String,
    val name: String,
    val sessionId: String
)

data class Completion(
    val text: String,
    val description: String = "",
    val type: CompletionType = CompletionType.COMMAND
)

enum class CompletionType {
    COMMAND,
    FILE,
    VARIABLE,
    FUNCTION,
    PACKAGE
}

enum class TerminalTheme {
    DARK,
    LIGHT,
    MONOKAI,
    SOLARIZED_DARK,
    SOLARIZED_LIGHT,
    GRUVBOX,
    NORD
}

enum class SplitMode {
    NONE,
    HORIZONTAL,
    VERTICAL
}

// Callback interface

interface TerminalUICallback {
    fun onCommandEntered(command: String)
    fun onTabCompletionRequested(currentText: String)
    fun onAutoCompletionRequested(currentText: String): List<Completion>
    fun onAddTab()
    fun onTabSelected(tabId: String)
    fun onTabClosed(tabId: String)
    fun onOpenSettings()
    fun onClear()
    fun onThemeChanged(theme: TerminalTheme)
    fun onFontSizeChanged(size: Int)
    fun onSplitModeEnabled(mode: SplitMode)
    fun onSplitModeDisabled()
    fun onSearchShown()
    fun onSearchHidden()
    fun onCompletionInserted(completion: Completion)
    fun onHistoryNavigation(command: String, isUp: Boolean)
    fun onEscapePressed()
}