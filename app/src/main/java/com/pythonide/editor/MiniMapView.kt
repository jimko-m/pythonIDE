package com.pythonide.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.pythonide.R
import kotlin.math.max
import kotlin.math.min

/**
 * Minimap view for quick navigation through large code files
 * Shows a scaled-down version of the code with cursor indicators and selection highlights
 */
class MiniMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint()
    private val cursorPaint = Paint()
    private val selectionPaint = Paint()
    private val foldIndicatorPaint = Paint()
    
    private var codeText: String = ""
    private var lines: List<String> = emptyList()
    private var currentLine = 0
    private var visibleLineCount = 0
    private var totalLines = 0
    
    // Navigation
    private var targetLine = 0
    private val navigationRect = RectF()
    private var isNavigating = false
    
    // Minimap configuration
    private val minLineHeight = 2.dpToPx()
    private val maxLineHeight = 8.dpToPx()
    private val lineSpacing = 1.dpToPx()
    private val cursorWidth = 2.dpToPx()
    private val selectionAlpha = 64
    private val foldIndicatorAlpha = 128
    
    // Scroll indicators
    private var scrollIndicatorTop = 0f
    private var scrollIndicatorBottom = 0f
    private var hasScroll = false
    
    // Colors
    private val backgroundColor: Int
    private val textColor: Int
    private val cursorColor: Int
    private val selectionColor: Int
    private val foldIndicatorColor: Int
    
    init {
        setupPaints()
        setupColors()
        setWillNotDraw(false)
    }
    
    private fun setupPaints() {
        // Background paint
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = Color.TRANSPARENT
        
        // Text paint for line numbers and snippets
        textPaint.apply {
            color = ContextCompat.getColor(context, R.color.on_surface)
            textSize = 10.spToPx()
            typeface = Typeface.MONOSPACE
        }
        
        // Cursor paint
        cursorPaint.apply {
            color = ContextCompat.getColor(context, R.color.primary)
            style = Paint.Style.FILL
            strokeWidth = cursorWidth.toFloat()
        }
        
        // Selection paint
        selectionPaint.apply {
            color = ContextCompat.getColor(context, R.color.primary).withAlpha(selectionAlpha)
            style = Paint.Style.FILL
        }
        
        // Fold indicator paint
        foldIndicatorPaint.apply {
            color = ContextCompat.getColor(context, R.color.secondary).withAlpha(foldIndicatorAlpha)
            style = Paint.Style.FILL
        }
    }
    
    private fun setupColors() {
        backgroundColor = ContextCompat.getColor(context, R.color.surface_variant)
        textColor = ContextCompat.getColor(context, R.color.on_surface)
        cursorColor = ContextCompat.getColor(context, R.color.primary)
        selectionColor = ContextCompat.getColor(context, R.color.primary)
        foldIndicatorColor = ContextCompat.getColor(context, R.color.secondary)
    }
    
    /**
     * Set the code text to display
     */
    fun setCodeText(code: String) {
        this.codeText = code
        this.lines = code.split("\n")
        this.totalLines = lines.size
        
        calculateVisibleLineCount()
        updateScrollIndicators()
        invalidate()
    }
    
    /**
     * Update the current cursor line
     */
    fun setCurrentLine(line: Int) {
        this.currentLine = min(line, totalLines - 1)
        updateScrollIndicators()
        invalidate()
    }
    
    /**
     * Set the visible line range in the editor
     */
    fun setVisibleLines(startLine: Int, endLine: Int) {
        this.visibleLineCount = max(1, endLine - startLine + 1)
        updateScrollIndicators()
        invalidate()
    }
    
    /**
     * Navigate to a specific line
     */
    fun navigateToLine(line: Int, onNavigate: ((Int) -> Unit)? = null) {
        val targetLine = min(max(line, 0), totalLines - 1)
        onNavigate?.invoke(targetLine)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 60.dpToPx()
        val desiredHeight = 200.dpToPx()
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }
        
        setMeasuredDimension(width, height)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (lines.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        // Draw background
        drawBackground(canvas)
        
        // Calculate line height and spacing
        val lineHeight = calculateLineHeight()
        val totalHeight = totalLines * (lineHeight + lineSpacing)
        
        // Draw code lines
        drawCodeLines(canvas, lineHeight)
        
        // Draw current cursor indicator
        drawCursorIndicator(canvas, lineHeight)
        
        // Draw fold indicators
        drawFoldIndicators(canvas, lineHeight)
        
        // Draw scroll indicator
        if (hasScroll) {
            drawScrollIndicator(canvas, lineHeight)
        }
        
        // Draw navigation rectangle
        if (isNavigating) {
            drawNavigationIndicator(canvas, lineHeight)
        }
        
        // Draw line numbers for first few characters
        drawLineNumbers(canvas, lineHeight)
    }
    
    private fun drawBackground(canvas: Canvas) {
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
    
    private fun drawCodeLines(canvas: Canvas, lineHeight: Float) {
        val visibleLines = calculateVisibleLines()
        val scaleY = (height - paddingTop - paddingBottom) / (totalLines * (lineHeight + lineSpacing))
        
        lines.forEachIndexed { index, line ->
            val y = paddingTop + index * (lineHeight + lineSpacing) * scaleY
            
            if (y > height || y + lineHeight < 0) return@forEachIndexed
            
            // Draw line background if it's a comment or significant line
            if (isSignificantLine(line)) {
                paint.color = Color.LTGRAY.withAlpha(32)
                canvas.drawRect(
                    paddingLeft.toFloat(),
                    y,
                    (width - paddingRight).toFloat(),
                    y + lineHeight,
                    paint
                )
            }
            
            // Draw simplified text (first few characters)
            val simplifiedText = simplifyLineText(line)
            if (simplifiedText.isNotEmpty()) {
                textPaint.color = textColor.withAlpha(128)
                val textY = y + lineHeight - (lineHeight - textPaint.textSize) / 2
                canvas.drawText(
                    simplifiedText,
                    paddingLeft.toFloat() + 4.dpToPx(),
                    textY,
                    textPaint
                )
            }
        }
    }
    
    private fun drawCursorIndicator(canvas: Canvas, lineHeight: Float) {
        val y = paddingTop + currentLine * (lineHeight + lineSpacing) * 
                ((height - paddingTop - paddingBottom) / (totalLines * (lineHeight + lineSpacing)))
        
        cursorPaint.color = cursorColor
        canvas.drawRect(
            paddingLeft.toFloat(),
            y,
            (width - paddingRight).toFloat(),
            y + lineHeight,
            cursorPaint
        )
    }
    
    private fun drawFoldIndicators(canvas: Canvas, lineHeight: Float) {
        // This would integrate with CodeFoldingManager to get fold regions
        // For now, we'll draw indicators for lines that look like fold points
        
        val significantLines = getFoldableLines()
        significantLines.forEach { lineIndex ->
            val y = paddingTop + lineIndex * (lineHeight + lineSpacing) * 
                    ((height - paddingTop - paddingBottom) / (totalLines * (lineHeight + lineSpacing)))
            
            foldIndicatorPaint.color = foldIndicatorColor
            val indicatorSize = min(lineHeight * 0.6f, 6.dpToPx())
            canvas.drawRect(
                (width - paddingRight - indicatorSize - 2.dpToPx()),
                y + (lineHeight - indicatorSize) / 2,
                (width - paddingRight - 2.dpToPx()).toFloat(),
                y + (lineHeight + indicatorSize) / 2,
                foldIndicatorPaint
            )
        }
    }
    
    private fun drawScrollIndicator(canvas: Canvas, lineHeight: Float) {
        paint.color = cursorColor.withAlpha(128)
        canvas.drawRect(
            paddingLeft.toFloat(),
            scrollIndicatorTop,
            (width - paddingRight).toFloat(),
            scrollIndicatorBottom,
            paint
        )
    }
    
    private fun drawNavigationIndicator(canvas: Canvas, lineHeight: Float) {
        val y = paddingTop + targetLine * (lineHeight + lineSpacing) * 
                ((height - paddingTop - paddingBottom) / (totalLines * (lineHeight + lineSpacing)))
        
        navigationRect.set(
            paddingLeft.toFloat() + 2.dpToPx(),
            y,
            (width - paddingRight - 2.dpToPx()).toFloat(),
            y + lineHeight
        )
        
        paint.color = Color.YELLOW.withAlpha(128)
        canvas.drawRoundRect(navigationRect, 4.dpToPx().toFloat(), 4.dpToPx().toFloat(), paint)
        
        // Draw target indicator
        paint.color = Color.YELLOW
        canvas.drawRect(
            (width - paddingRight - 6.dpToPx()).toFloat(),
            y + lineHeight / 4,
            (width - paddingRight - 2.dpToPx()).toFloat(),
            y + 3 * lineHeight / 4,
            paint
        )
    }
    
    private fun drawLineNumbers(canvas: Canvas, lineHeight: Float) {
        if (totalLines <= 100) return // Only show line numbers for smaller files
        
        textPaint.color = textColor.withAlpha(192)
        val lineNumberTextSize = textPaint.textSize * 0.8f
        
        // Show line numbers for major milestones (every 10th line)
        for (i in 0..totalLines step 10) {
            val y = paddingTop + i * (lineHeight + lineSpacing) * 
                    ((height - paddingTop - paddingBottom) / (totalLines * (lineHeight + lineSpacing)))
            
            val lineNumber = (i + 1).toString()
            canvas.drawText(
                lineNumber,
                2.dpToPx().toFloat(),
                y + lineHeight - (lineHeight - lineNumberTextSize) / 2,
                textPaint
            )
        }
    }
    
    private fun drawEmptyState(canvas: Canvas) {
        textPaint.color = textColor.withAlpha(128)
        val text = "Minimap"
        val textWidth = textPaint.measureText(text)
        val textHeight = textPaint.textSize
        
        canvas.drawText(
            text,
            (width - textWidth) / 2,
            (height + textHeight) / 2,
            textPaint
        )
    }
    
    private fun calculateLineHeight(): Float {
        val availableHeight = height - paddingTop - paddingBottom
        val totalLineHeight = totalLines * (minLineHeight + lineSpacing)
        
        return if (totalLineHeight > availableHeight) {
            (availableHeight / totalLines) - lineSpacing
        } else {
            min(maxLineHeight, (availableHeight / totalLines) - lineSpacing)
        }
    }
    
    private fun calculateVisibleLines(): IntRange {
        val scaleY = (height - paddingTop - paddingBottom) / (totalLines * (minLineHeight + lineSpacing))
        val visibleHeight = height * scaleY
        
        return 0 until (visibleHeight / (minLineHeight + lineSpacing)).toInt()
    }
    
    private fun simplifyLineText(line: String): String {
        // Remove leading/trailing whitespace and show first few meaningful characters
        val trimmed = line.trim()
        return when {
            trimmed.startsWith("#") -> trimmed.take(8) // Comments
            trimmed.startsWith("def ") -> "fn " + trimmed.substringAfter("def ").take(10)
            trimmed.startsWith("class ") -> "cls " + trimmed.substringAfter("class ").take(10)
            trimmed.contains("def ") -> "fn " + trimmed.substringAfter("def ").take(10)
            trimmed.contains("class ") -> "cls " + trimmed.substringAfter("class ").take(10)
            trimmed.length > 15 -> trimmed.take(12) + "..."
            else -> trimmed
        }
    }
    
    private fun isSignificantLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("#") || 
               trimmed.startsWith("def ") || 
               trimmed.startsWith("class ") ||
               trimmed.startsWith("import ") ||
               trimmed.startsWith("from ")
    }
    
    private fun getFoldableLines(): List<Int> {
        val foldableLines = mutableListOf<Int>()
        
        lines.forEachIndexed { index, line ->
            if (line.trim().startsWith("def ") || 
                line.trim().startsWith("class ") ||
                line.trim().startsWith("#region")) {
                foldableLines.add(index)
            }
        }
        
        return foldableLines
    }
    
    private fun calculateVisibleLineCount() {
        // Estimate visible line count based on text size
        val estimatedCharsPerLine = 80
        val estimatedLines = (codeText.length / estimatedCharsPerLine).coerceAtLeast(1)
        this.visibleLineCount = estimatedLines
    }
    
    private fun updateScrollIndicators() {
        if (totalLines > 0 && visibleLineCount < totalLines) {
            hasScroll = true
            val scrollRatio = height.toFloat() / (totalLines * (minLineHeight + lineSpacing))
            val currentRatio = currentLine.toFloat() / totalLines
            
            val indicatorHeight = (visibleLineCount * (minLineHeight + lineSpacing) * scrollRatio).toInt()
            scrollIndicatorTop = (height * currentRatio).coerceIn(0f, height - indicatorHeight.toFloat())
            scrollIndicatorBottom = scrollIndicatorTop + indicatorHeight
        } else {
            hasScroll = false
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val line = calculateLineFromY(event.y)
                if (line in 0 until totalLines) {
                    targetLine = line
                    isNavigating = true
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isNavigating) {
                    val line = calculateLineFromY(event.y)
                    targetLine = line.coerceIn(0, totalLines - 1)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isNavigating) {
                    isNavigating = false
                    // Trigger navigation callback
                    performClick()
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun calculateLineFromY(y: Float): Int {
        val lineHeight = calculateLineHeight()
        val totalHeight = totalLines * (lineHeight + lineSpacing)
        val scaleY = (height - paddingTop - paddingBottom) / totalHeight
        
        val relativeY = (y - paddingTop) / scaleY
        return (relativeY / (lineHeight + lineSpacing)).toInt()
    }
    
    /**
     * Set click listener for navigation
     */
    private var onNavigateListener: ((Int) -> Unit)? = null
    
    fun setOnNavigateListener(listener: (Int) -> Unit) {
        onNavigateListener = listener
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        if (isNavigating) {
            onNavigateListener?.invoke(targetLine)
            isNavigating = false
            invalidate()
            return true
        }
        return false
    }
    
    // Extension functions for unit conversion
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    private fun Int.spToPx(): Float {
        return this * resources.displayMetrics.scaledDensity
    }
    
    private fun Int.withAlpha(alpha: Int): Int {
        return Color.argb(
            alpha,
            Color.red(this),
            Color.green(this),
            Color.blue(this)
        )
    }
    
    /**
     * Get the current statistics for the minimap
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "totalLines" to totalLines,
            "visibleLineCount" to visibleLineCount,
            "currentLine" to currentLine,
            "hasScroll" to hasScroll
        )
    }
}
