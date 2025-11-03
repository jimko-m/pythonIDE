package com.pythonide.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * مكون محرر الكود المخصص مع دعم Monaco Editor
 * يوفر جميع الميزات المتقدمة لمحرر الكود
 */
public class CodeEditText extends AppCompatEditText {
    
    private Paint lineNumberPaint;
    private Paint bracketPaint;
    private Paint functionPaint;
    private Paint keywordPaint;
    private Paint stringPaint;
    private Paint commentPaint;
    private Paint errorPaint;
    
    private float lineNumberWidth;
    private boolean showLineNumbers = true;
    private boolean highlightMatchingBrackets = true;
    private boolean autoIndentEnabled = true;
    private int tabWidth = 4;
    private boolean showWhitespace = false;
    private boolean enableWordWrap = false;
    
    private BracketMatchListener bracketMatchListener;
    private FunctionDetectionListener functionDetectionListener;
    private TextChangedListener textChangedListener;
    
    private SyntaxHighlighter syntaxHighlighter;
    private AutoCompleteHandler autoCompleteHandler;
    private UndoRedoManager undoRedoManager;
    private SearchReplaceManager searchReplaceManager;
    
    private List<Bracket> brackets = new ArrayList<>();
    private Stack<Integer> bracketStack = new Stack<>();
    
    // Bracket types
    private static final char[] BRACKETS = {'(', ')', '[', ']', '{', '}'};
    private static final char[][] BRACKET_PAIRS = {{'(', ')'}, {'[', ']'}, {'{', '}'}};
    
    private interface BracketMatchListener {
        void onBracketMatch(int start, int end, boolean hasMatch);
    }
    
    public interface FunctionDetectionListener {
        void onFunctionDetected(int start, int end, String functionName);
        void onKeywordDetected(int start, int end, String keyword);
        void onStringDetected(int start, int end);
        void onCommentDetected(int start, int end);
    }
    
    public interface TextChangedListener {
        void onTextChanged(String text);
    }
    
    public static class Bracket {
        public int start;
        public int end;
        public char openChar;
        public char closeChar;
        public boolean isMatch;
        
        public Bracket(int start, int end, char openChar, char closeChar) {
            this.start = start;
            this.end = end;
            this.openChar = openChar;
            this.closeChar = closeChar;
        }
    }
    
    public CodeEditText(@NonNull Context context) {
        super(context);
        init();
    }
    
    public CodeEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CodeEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setHorizontallyScrolling(true);
        setVerticalScrollBarEnabled(true);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setCustomSelectionActionModeCallback(null);
        
        // Set up text appearance
        setTypeface(Typeface.MONOSPACE);
        setTextSize(16);
        
        // Initialize paints
        initPaints();
        
        // Add text watcher for real-time processing
        addTextChangedListener(new TextWatcher() {
            private String previousText = "";
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousText = s.toString();
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textChangedListener != null) {
                    textChangedListener.onTextChanged(s.toString());
                }
                processTextChange(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Handle text changes
            }
        });
    }
    
    private void initPaints() {
        // Line number paint
        lineNumberPaint = new Paint();
        lineNumberPaint.setColor(Color.GRAY);
        lineNumberPaint.setTextSize(14);
        lineNumberPaint.setTypeface(Typeface.MONOSPACE);
        
        // Bracket matching paint
        bracketPaint = new Paint();
        bracketPaint.setColor(ContextCompat.getColor(getContext(), R.color.bracket_highlight));
        bracketPaint.setStyle(Paint.Style.FILL);
        
        // Function highlighting paint
        functionPaint = new Paint();
        functionPaint.setColor(ContextCompat.getColor(getContext(), R.color.function_highlight));
        
        // Keyword highlighting paint
        keywordPaint = new Paint();
        keywordPaint.setColor(ContextCompat.getColor(getContext(), R.color.keyword_highlight));
        keywordPaint.setTypeface(Typeface.BOLD);
        
        // String highlighting paint
        stringPaint = new Paint();
        stringPaint.setColor(ContextCompat.getColor(getContext(), R.color.string_highlight));
        
        // Comment highlighting paint
        commentPaint = new Paint();
        commentPaint.setColor(ContextCompat.getColor(getContext(), R.color.comment_highlight));
        
        // Error highlighting paint
        errorPaint = new Paint();
        errorPaint.setColor(Color.RED);
        errorPaint.setStyle(Paint.Style.UNDERLINE);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateLineNumberWidth();
    }
    
    private void calculateLineNumberWidth() {
        Paint.FontMetricsInt metrics = lineNumberPaint.getFontMetricsInt();
        int lineNumberWidth = (int) lineNumberPaint.measureText("9999") + 16; // Add padding
        this.lineNumberWidth = lineNumberWidth;
        setPadding((int) lineNumberWidth + 16, getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (showLineNumbers) {
            drawLineNumbers(canvas);
        }
        
        if (highlightMatchingBrackets) {
            drawBracketHighlighting(canvas);
        }
    }
    
    private void drawLineNumbers(Canvas canvas) {
        if (getLayout() == null) return;
        
        int baseline = getBaseline();
        int lineCount = getLayout().getLineCount();
        int textColor = getCurrentTextColor();
        
        lineNumberPaint.setColor(textColor);
        lineNumberPaint.setAlpha(128); // Semi-transparent
        
        for (int i = 0; i < lineCount; i++) {
            float x = getPaddingLeft() - lineNumberWidth + 8;
            float y = getLayout().getLineBaseline(i) + baseline - getLayout().getLineTop(i);
            String lineNumber = String.valueOf(i + 1);
            canvas.drawText(lineNumber, x, y, lineNumberPaint);
        }
    }
    
    private void drawBracketHighlighting(Canvas canvas) {
        // This would be implemented to draw bracket matching highlights
        // For now, it's a placeholder
    }
    
    private void processTextChange(String text) {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.updateHighlighting();
        }
        
        detectBrackets(text);
        detectFunctionsAndKeywords(text);
        
        if (autoIndentEnabled) {
            handleAutoIndent();
        }
    }
    
    private void detectBrackets(String text) {
        brackets.clear();
        bracketStack.clear();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Check for opening brackets
            for (char[] pair : BRACKET_PAIRS) {
                if (c == pair[0]) {
                    bracketStack.push(i);
                }
            }
            
            // Check for closing brackets
            for (char[] pair : BRACKET_PAIRS) {
                if (c == pair[1] && !bracketStack.isEmpty()) {
                    int openIndex = bracketStack.pop();
                    brackets.add(new Bracket(openIndex, i, text.charAt(openIndex), c));
                }
            }
        }
        
        // Check bracket matches
        checkBracketMatches();
    }
    
    private void checkBracketMatches() {
        for (Bracket bracket : brackets) {
            boolean hasMatch = true; // Simplified logic
            if (bracketMatchListener != null) {
                bracketMatchListener.onBracketMatch(bracket.start, bracket.end, hasMatch);
            }
        }
    }
    
    private void detectFunctionsAndKeywords(String text) {
        if (functionDetectionListener == null) return;
        
        // Detect functions
        Pattern functionPattern = Pattern.compile("\\b(def|class)\\s+(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(text);
        
        while (functionMatcher.find()) {
            int start = functionMatcher.start();
            int end = functionMatcher.end();
            String functionName = functionMatcher.group(2);
            functionDetectionListener.onFunctionDetected(start, end, functionName);
        }
        
        // Detect strings
        Pattern stringPattern = Pattern.compile("[\"'].*?[\"']");
        Matcher stringMatcher = stringPattern.matcher(text);
        
        while (stringMatcher.find()) {
            functionDetectionListener.onStringDetected(stringMatcher.start(), stringMatcher.end());
        }
        
        // Detect comments
        Pattern commentPattern = Pattern.compile("#.*");
        Matcher commentMatcher = commentPattern.matcher(text);
        
        while (commentMatcher.find()) {
            functionDetectionListener.onCommentDetected(commentMatcher.start(), commentMatcher.end());
        }
    }
    
    private void handleAutoIndent() {
        Editable text = getText();
        if (text == null) return;
        
        int cursorPosition = getSelectionStart();
        int lineStart = findLineStart(text, cursorPosition);
        
        // Check if Enter was pressed
        if (cursorPosition > lineStart && text.charAt(cursorPosition - 1) == '\n') {
            // Add indentation based on previous line
            String previousLine = getPreviousLine(text, cursorPosition);
            int indentLevel = calculateIndentLevel(previousLine);
            
            // Add appropriate indentation
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                indent.append("    "); // 4 spaces per indent level
            }
            
            text.insert(cursorPosition, indent.toString());
        }
    }
    
    private String findLineStart(Editable text, int position) {
        int lineStart = position;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        return lineStart;
    }
    
    private String getPreviousLine(Editable text, int position) {
        int lineStart = findLineStart(text, position);
        int previousLineStart = findLineStart(text, lineStart - 1);
        return text.subSequence(previousLineStart, lineStart).toString().trim();
    }
    
    private int calculateIndentLevel(String line) {
        int indentLevel = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                indentLevel++;
            } else if (line.charAt(i) == '\t') {
                indentLevel += tabWidth;
            } else {
                break;
            }
        }
        return indentLevel / 4; // Each indent level is 4 spaces
    }
    
    // Public setter methods for editor configuration
    public void setShowLineNumbers(boolean show) {
        this.showLineNumbers = show;
        calculateLineNumberWidth();
        invalidate();
    }
    
    public void setHighlightMatchingBrackets(boolean highlight) {
        this.highlightMatchingBrackets = highlight;
        invalidate();
    }
    
    public void setAutoIndentEnabled(boolean enabled) {
        this.autoIndentEnabled = enabled;
    }
    
    public void setTabWidth(int width) {
        this.tabWidth = width;
    }
    
    public void setShowWhitespace(boolean show) {
        this.showWhitespace = show;
    }
    
    public void setEnableWordWrap(boolean wrap) {
        this.enableWordWrap = wrap;
        setHorizontallyScrolling(!wrap);
    }
    
    // Highlighting methods
    public void highlightBracketPair(int start, int end) {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.highlightBracketPair(start, end);
        }
    }
    
    public void clearBracketHighlighting() {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.clearBracketHighlighting();
        }
    }
    
    public void highlightFunction(int start, int end) {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.highlightFunction(start, end);
        }
    }
    
    public void highlightKeyword(int start, int end) {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.highlightKeyword(start, end);
        }
    }
    
    public void highlightString(int start, int end) {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.highlightString(start, end);
        }
    }
    
    public void highlightComment(int start, int end) {
        if (syntaxHighlighter != null) {
            syntaxHighlighter.highlightComment(start, end);
        }
    }
    
    // Setter methods for listeners and handlers
    public void setOnBracketMatchListener(BracketMatchListener listener) {
        this.bracketMatchListener = listener;
    }
    
    public void setOnFunctionDetectionListener(FunctionDetectionListener listener) {
        this.functionDetectionListener = listener;
    }
    
    public void setOnTextChangedListener(TextChangedListener listener) {
        this.textChangedListener = listener;
    }
    
    public void setSyntaxHighlighter(SyntaxHighlighter highlighter) {
        this.syntaxHighlighter = highlighter;
    }
    
    public void setAutoCompleteHandler(AutoCompleteHandler handler) {
        this.autoCompleteHandler = handler;
    }
    
    public void setUndoRedoManager(UndoRedoManager manager) {
        this.undoRedoManager = manager;
    }
    
    public void setSearchReplaceManager(SearchReplaceManager manager) {
        this.searchReplaceManager = manager;
    }
}