package com.pythonide.editor;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

/**
 * معالج تظليل الكود (Syntax Highlighter) للغة Python
 * يوفر تظليلاً متقدماً للكود مع دعم جميع عناصر Python
 */
public class SyntaxHighlighter {
    
    private CodeEditText codeEditText;
    private String[] keywords;
    private String[] builtins;
    
    // Color constants
    private static final int KEYWORD_COLOR = Color.rgb(159, 68, 245); // Purple
    private static final int BUILTIN_COLOR = Color.rgb(34, 134, 245); // Blue
    private static final int STRING_COLOR = Color.rgb(35, 134, 85);   // Green
    private static final int COMMENT_COLOR = Color.rgb(128, 128, 128); // Gray
    private static final int NUMBER_COLOR = Color.rgb(184, 49, 47);   // Red
    private static final int FUNCTION_COLOR = Color.rgb(0, 123, 255); // Blue
    private static final int CLASS_COLOR = Color.rgb(255, 99, 71);    // Tomato
    private static final int OPERATOR_COLOR = Color.rgb(156, 39, 176); // Purple
    private static final int BRACKET_COLOR = Color.rgb(255, 193, 7);  // Yellow
    private static final int BRACKET_MATCH_COLOR = Color.rgb(255, 152, 0); // Orange
    private static final int ERROR_COLOR = Color.rgb(244, 67, 54);    // Red
    private static final int HOVER_COLOR = Color.rgb(96, 125, 139);   // Blue Gray
    
    // Pattern definitions
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
        "\\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b"
    );
    
    private static final Pattern BUILTIN_PATTERN = Pattern.compile(
        "\\b(abs|all|any|bin|bool|bytearray|bytes|chr|classmethod|compile|complex|delattr|dict|dir|divmod|enumerate|eval|exec|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|isinstance|issubclass|iter|len|list|locals|map|max|memoryview|min|next|object|oct|open|ord|pow|print|property|range|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|super|tuple|type|vars|zip)\\b"
    );
    
    private static final Pattern STRING_PATTERN = Pattern.compile(
        "('''[\\s\\S]*?'''|\"\"\"[\\s\\S]*?\"\"\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")"
    );
    
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "#[^\\n]*"
    );
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
        "\\b\\d+\\.?\\d*[fF]?\\b"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "\\b(def)\\s+(\\w+)\\s*\\("
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "\\b(class)\\s+(\\w+)\\s*(\\(|:)"
    );
    
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(
        "(\\+|\\-|\\*|/|%|==|!=|<=|>=|<|>|=|\\|\\||&&|\\|\\||\\+=|\\-=|\\*=|/=)"
    );
    
    private static final Pattern BRACKET_PATTERN = Pattern.compile(
        "([\\(\\{\\[\\]\\}])"
    );
    
    private static final Pattern DECORATOR_PATTERN = Pattern.compile(
        "@[\\w\\.]+"
    );
    
    private static final Pattern TRIPLE_QUOTE_PATTERN = Pattern.compile(
        "('''[\\s\\S]*?'''|\"\"\"[\\s\\S]*?\"\"\")"
    );
    
    private Map<String, Integer> keywordSpans = new HashMap<>();
    private Map<String, Integer> functionSpans = new HashMap<>();
    private Map<String, Integer> bracketSpans = new HashMap<>();
    
    public SyntaxHighlighter(CodeEditText codeEditText, String[] keywords, String[] builtins) {
        this.codeEditText = codeEditText;
        this.keywords = keywords != null ? keywords : new String[0];
        this.builtins = builtins != null ? builtins : new String[0];
    }
    
    /**
     * تحديث التظليل للكود
     */
    public void updateHighlighting() {
        if (codeEditText == null) return;
        
        CharSequence text = codeEditText.getText();
        if (text == null || text.length() == 0) return;
        
        try {
            applySyntaxHighlighting(text);
        } catch (Exception e) {
            Log.e("SyntaxHighlighter", "Error updating highlighting", e);
        }
    }
    
    /**
     * تطبيق تظليل الكود
     */
    private void applySyntaxHighlighting(CharSequence text) {
        SpannableString spannableString = new SpannableString(text);
        
        // Reset previous spans
        resetSpans(spannableString);
        
        // Apply highlighting in order of priority
        highlightStrings(spannableString);
        highlightComments(spannableString);
        highlightNumbers(spannableString);
        highlightDecorators(spannableString);
        highlightFunctions(spannableString);
        highlightClasses(spannableString);
        highlightKeywords(spannableString);
        highlightBuiltins(spannableString);
        highlightOperators(spannableString);
        highlightBrackets(spannableString);
        
        // Set the highlighted text
        codeEditText.setText(spannableString);
        
        // Restore cursor position
        codeEditText.setSelection(codeEditText.getSelectionStart());
    }
    
    private void resetSpans(Spannable spannableString) {
        // Remove all existing spans
        Object[] spans = spannableString.getSpans(0, spannableString.length(), Object.class);
        for (Object span : spans) {
            spannableString.removeSpan(span);
        }
        
        keywordSpans.clear();
        functionSpans.clear();
        bracketSpans.clear();
    }
    
    private void highlightStrings(SpannableString spannableString) {
        Matcher matcher = STRING_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            // Check if this string is not already highlighted
            if (!isSpanOverlap(start, end, stringSpans())) {
                spannableString.setSpan(
                    new ForegroundColorSpan(STRING_COLOR),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    private void highlightComments(SpannableString spannableString) {
        Matcher matcher = COMMENT_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            if (!isSpanOverlap(start, end, commentSpans())) {
                spannableString.setSpan(
                    new ForegroundColorSpan(COMMENT_COLOR),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    private void highlightNumbers(SpannableString spannableString) {
        Matcher matcher = NUMBER_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            // Check if not inside a string
            if (!isInString(spannableString, start, end)) {
                spannableString.setSpan(
                    new ForegroundColorSpan(NUMBER_COLOR),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    private void highlightFunctions(SpannableString spannableString) {
        Matcher matcher = FUNCTION_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            spannableString.setSpan(
                new ForegroundColorSpan(FUNCTION_COLOR),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            // Highlight the function name specifically
            int nameStart = matcher.start(2);
            int nameEnd = matcher.end(2);
            spannableString.setSpan(
                new StyleSpan(Typeface.BOLD),
                nameStart, nameEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    private void highlightClasses(SpannableString spannableString) {
        Matcher matcher = CLASS_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            spannableString.setSpan(
                new ForegroundColorSpan(CLASS_COLOR),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            // Highlight the class name specifically
            int nameStart = matcher.start(2);
            int nameEnd = matcher.end(2);
            spannableString.setSpan(
                new StyleSpan(Typeface.BOLD),
                nameStart, nameEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    private void highlightKeywords(SpannableString spannableString) {
        if (keywords == null || keywords.length == 0) return;
        
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
            Matcher matcher = pattern.matcher(spannableString);
            
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                
                // Check if not in a string
                if (!isInString(spannableString, start, end)) {
                    spannableString.setSpan(
                        new ForegroundColorSpan(KEYWORD_COLOR),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    
                    // Make keywords bold
                    spannableString.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }
        }
    }
    
    private void highlightBuiltins(SpannableString spannableString) {
        if (builtins == null || builtins.length == 0) return;
        
        for (String builtin : builtins) {
            Pattern pattern = Pattern.compile("\\b" + builtin + "\\b");
            Matcher matcher = pattern.matcher(spannableString);
            
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                
                if (!isInString(spannableString, start, end)) {
                    spannableString.setSpan(
                        new ForegroundColorSpan(BUILTIN_COLOR),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }
        }
    }
    
    private void highlightOperators(SpannableString spannableString) {
        Matcher matcher = OPERATOR_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            if (!isInString(spannableString, start, end)) {
                spannableString.setSpan(
                    new ForegroundColorSpan(OPERATOR_COLOR),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    private void highlightBrackets(SpannableString spannableString) {
        Matcher matcher = BRACKET_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            spannableString.setSpan(
                new ForegroundColorSpan(BRACKET_COLOR),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    private void highlightDecorators(SpannableString spannableString) {
        Matcher matcher = DECORATOR_PATTERN.matcher(spannableString);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            spannableString.setSpan(
                new ForegroundColorSpan(COMMENT_COLOR),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            spannableString.setSpan(
                new StyleSpan(Typeface.BOLD),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    // Helper methods
    private boolean isSpanOverlap(int start, int end, Object[] spans) {
        for (Object span : spans) {
            int spanStart = spannableGetSpanStart(span);
            int spanEnd = spannableGetSpanEnd(span);
            if (!(end <= spanStart || start >= spanEnd)) {
                return true; // Overlap found
            }
        }
        return false;
    }
    
    private boolean isInString(SpannableString spannableString, int start, int end) {
        Object[] spans = spannableString.getSpans(start, end, ForegroundColorSpan.class);
        for (Object span : spans) {
            if (spannableGetSpanColor(span) == STRING_COLOR) {
                return true;
            }
        }
        return false;
    }
    
    private Object[] stringSpans() {
        return new Object[0]; // Placeholder
    }
    
    private Object[] commentSpans() {
        return new Object[0]; // Placeholder
    }
    
    private int spannableGetSpanStart(Object span) {
        if (span instanceof CharacterStyle) {
            return 0; // Placeholder
        }
        return 0;
    }
    
    private int spannableGetSpanEnd(Object span) {
        if (span instanceof CharacterStyle) {
            return 0; // Placeholder
        }
        return 0;
    }
    
    private int spannableGetSpanColor(Object span) {
        if (span instanceof ForegroundColorSpan) {
            return ((ForegroundColorSpan) span).getForegroundColor();
        }
        return 0;
    }
    
    // Public highlighting methods
    public void highlightBracketPair(int start, int end) {
        CharSequence text = codeEditText.getText();
        if (text == null || text.length() == 0) return;
        
        SpannableString spannableString = new SpannableString(text);
        
        // Highlight the bracket pair
        spannableString.setSpan(
            new BackgroundColorSpan(BRACKET_MATCH_COLOR),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        codeEditText.setText(spannableString);
    }
    
    public void clearBracketHighlighting() {
        updateHighlighting(); // This will clear and reapply highlighting
    }
    
    public void highlightFunction(int start, int end) {
        highlightText(start, end, FUNCTION_COLOR, Typeface.BOLD);
    }
    
    public void highlightKeyword(int start, int end) {
        highlightText(start, end, KEYWORD_COLOR, Typeface.BOLD);
    }
    
    public void highlightString(int start, int end) {
        highlightText(start, end, STRING_COLOR, Typeface.NORMAL);
    }
    
    public void highlightComment(int start, int end) {
        highlightText(start, end, COMMENT_COLOR, Typeface.ITALIC);
    }
    
    private void highlightText(int start, int end, int color, int style) {
        CharSequence text = codeEditText.getText();
        if (text == null || text.length() == 0) return;
        
        SpannableString spannableString = new SpannableString(text);
        
        spannableString.setSpan(
            new ForegroundColorSpan(color),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        if (style != Typeface.NORMAL) {
            spannableString.setSpan(
                new StyleSpan(style),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        codeEditText.setText(spannableString);
    }
    
    /**
     * تنسيق الكود (code formatting)
     */
    public String formatCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "";
        }
        
        // Basic code formatting logic
        StringBuilder formatted = new StringBuilder();
        String[] lines = code.split("\n");
        
        int indentLevel = 0;
        final int INDENT_SIZE = 4;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Decrease indent for closing brackets
            if (trimmedLine.matches(".*[\\]\\}]\\s*:?$")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            // Add indented line
            if (!trimmedLine.isEmpty()) {
                formatted.append(" ".repeat(indentLevel * INDENT_SIZE));
                formatted.append(trimmedLine).append("\n");
            } else {
                formatted.append("\n");
            }
            
            // Increase indent for opening brackets
            if (trimmedLine.matches(".*[\\{\\[\\(]\\s*:?$") || 
                trimmedLine.startsWith("def ") || 
                trimmedLine.startsWith("class ") ||
                trimmedLine.startsWith("if ") ||
                trimmedLine.startsWith("elif ") ||
                trimmedLine.startsWith("else:") ||
                trimmedLine.startsWith("for ") ||
                trimmedLine.startsWith("while ") ||
                trimmedLine.startsWith("try:") ||
                trimmedLine.startsWith("except ") ||
                trimmedLine.startsWith("finally:") ||
                trimmedLine.startsWith("with ")) {
                indentLevel++;
            }
        }
        
        return formatted.toString();
    }
}