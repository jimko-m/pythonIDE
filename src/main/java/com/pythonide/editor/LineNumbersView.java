package com.pythonide.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * عرض أرقام الأسطر (Line Numbers View)
 * يعرض أرقام الأسطر بجانب محرر الكود
 */
public class LineNumbersView extends View {
    
    private Paint lineNumberPaint;
    private Paint currentLinePaint;
    private String[] lineNumbers = {"1"};
    private int currentLine = 1;
    private int totalLines = 1;
    
    // Colors
    private static final int DEFAULT_LINE_NUMBER_COLOR = 0xFF757575;
    private static final int CURRENT_LINE_COLOR = 0xFF3F51B5;
    private static final int LINE_NUMBER_BG_COLOR = 0xFFF5F5F5;
    
    // Dimensions
    private float textSize = 14f;
    private float lineHeight = 20f;
    private int padding = 8;
    
    public LineNumbersView(Context context) {
        super(context);
        init();
    }
    
    public LineNumbersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public LineNumbersView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        lineNumberPaint = new Paint();
        lineNumberPaint.setColor(DEFAULT_LINE_NUMBER_COLOR);
        lineNumberPaint.setTextSize(textSize);
        lineNumberPaint.setTypeface(Typeface.MONOSPACE);
        lineNumberPaint.setTextAlign(Paint.Align.RIGHT);
        
        currentLinePaint = new Paint();
        currentLinePaint.setColor(CURRENT_LINE_COLOR);
        currentLinePaint.setTextSize(textSize);
        currentLinePaint.setTypeface(Typeface.MONOSPACE);
        currentLinePaint.setTextAlign(Paint.Align.RIGHT);
        
        setBackgroundColor(LINE_NUMBER_BG_COLOR);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Set fixed width for line numbers
        int width = Math.round(getResources().getDisplayMetrics().density * 48);
        setMeasuredDimension(width, heightMeasureSpec);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (lineNumbers == null || lineNumbers.length == 0) {
            return;
        }
        
        float viewHeight = getHeight();
        float viewWidth = getWidth();
        
        Paint.FontMetrics metrics = lineNumberPaint.getFontMetrics();
        float textHeight = Math.abs(metrics.bottom - metrics.top);
        lineHeight = textHeight * 1.2f; // Add some line spacing
        
        int visibleLines = (int) (viewHeight / lineHeight);
        int startLine = Math.max(1, currentLine - visibleLines / 2);
        int endLine = Math.min(totalLines, startLine + visibleLines);
        
        // Adjust start line if we're near the end
        if (endLine - startLine < visibleLines && startLine > 1) {
            startLine = Math.max(1, endLine - visibleLines);
        }
        
        for (int i = startLine; i <= endLine && i <= lineNumbers.length; i++) {
            float y = (i - startLine + 1) * lineHeight - metrics.bottom;
            float x = viewWidth - padding;
            
            // Use different paint for current line
            Paint paint = (i == currentLine) ? currentLinePaint : lineNumberPaint;
            
            // Highlight current line background (optional)
            if (i == currentLine) {
                paintCurrentLineBackground(canvas, i, y, viewWidth);
            }
            
            // Draw line number
            if (i <= lineNumbers.length) {
                canvas.drawText(lineNumbers[i - 1], x, y, paint);
            }
        }
    }
    
    private void paintCurrentLineBackground(Canvas canvas, int lineNumber, float y, float viewWidth) {
        // Optional: draw background highlight for current line
        Paint bgPaint = new Paint();
        bgPaint.setColor(ContextCompat.getColor(getContext(), R.color.selected_file_background));
        bgPaint.setStyle(Paint.Style.FILL);
        
        float top = y - lineNumberPaint.getTextSize();
        float bottom = y + lineNumberPaint.getFontMetrics().top;
        
        canvas.drawRect(0, top, viewWidth, bottom, bgPaint);
    }
    
    /**
     * تحديث أرقام الأسطر
     */
    public void updateLineNumbers(String[] newLineNumbers) {
        this.lineNumbers = newLineNumbers != null ? newLineNumbers : new String[]{"1"};
        this.totalLines = lineNumbers.length;
        invalidate();
    }
    
    /**
     * تحديث رقم السطر الحالي
     */
    public void updateCurrentLine(int newCurrentLine) {
        this.currentLine = Math.max(1, Math.min(newCurrentLine, totalLines));
        invalidate();
    }
    
    /**
     * تحديث العدد الإجمالي للأسطر
     */
    public void updateTotalLines(int totalLines) {
        this.totalLines = Math.max(1, totalLines);
        invalidate();
    }
    
    /**
     * تعيين لون أرقام الأسطر
     */
    public void setLineNumberColor(int color) {
        lineNumberPaint.setColor(color);
        invalidate();
    }
    
    /**
     * تعيين لون السطر الحالي
     */
    public void setCurrentLineColor(int color) {
        currentLinePaint.setColor(color);
        invalidate();
    }
    
    /**
     * تعيين حجم النص
     */
    public void setTextSize(float textSize) {
        this.textSize = textSize;
        lineNumberPaint.setTextSize(textSize);
        currentLinePaint.setTextSize(textSize);
        invalidate();
    }
    
    /**
     * تعيين التباعد بين الأسطر
     */
    public void setLineHeight(float lineHeight) {
        this.lineHeight = lineHeight;
        invalidate();
    }
    
    /**
     * الحصول على عدد الأسطر
     */
    public int getTotalLines() {
        return totalLines;
    }
    
    /**
     * الحصول على رقم السطر الحالي
     */
    public int getCurrentLine() {
        return currentLine;
    }
}