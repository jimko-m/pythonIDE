package com.pythonide.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

/**
 * مساعدات محرر الكود (Code Editor Utilities)
 * تحتوي على دوال مفيدة لمحرر الكود
 */
public class CodeEditorUtils {
    
    private static final String PREFS_NAME = "CodeEditorPrefs";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_THEME = "theme";
    private static final String KEY_AUTO_INDENT = "auto_indent";
    private static final String KEY_LINE_NUMBERS = "line_numbers";
    private static final String KEY_WORD_WRAP = "word_wrap";
    private static final String KEY_TAB_WIDTH = "tab_width";
    
    // Default values
    private static final float DEFAULT_FONT_SIZE = 16f;
    private static final boolean DEFAULT_AUTO_INDENT = true;
    private static final boolean DEFAULT_LINE_NUMBERS = true;
    private static final boolean DEFAULT_WORD_WRAP = false;
    private static final int DEFAULT_TAB_WIDTH = 4;
    
    // Theme constants
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_MONOKAI = "monokai";
    public static final String THEME_GITHUB = "github";
    
    /**
     * حفظ الإعدادات في SharedPreferences
     */
    public static void saveSettings(Context context, EditorSettings settings) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putFloat(KEY_FONT_SIZE, settings.fontSize);
        editor.putString(KEY_THEME, settings.theme);
        editor.putBoolean(KEY_AUTO_INDENT, settings.autoIndent);
        editor.putBoolean(KEY_LINE_NUMBERS, settings.lineNumbers);
        editor.putBoolean(KEY_WORD_WRAP, settings.wordWrap);
        editor.putInt(KEY_TAB_WIDTH, settings.tabWidth);
        
        editor.apply();
    }
    
    /**
     * تحميل الإعدادات من SharedPreferences
     */
    public static EditorSettings loadSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        EditorSettings settings = new EditorSettings();
        settings.fontSize = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        settings.theme = prefs.getString(KEY_THEME, THEME_LIGHT);
        settings.autoIndent = prefs.getBoolean(KEY_AUTO_INDENT, DEFAULT_AUTO_INDENT);
        settings.lineNumbers = prefs.getBoolean(KEY_LINE_NUMBERS, DEFAULT_LINE_NUMBERS);
        settings.wordWrap = prefs.getBoolean(KEY_WORD_WRAP, DEFAULT_WORD_WRAP);
        settings.tabWidth = prefs.getInt(KEY_TAB_WIDTH, DEFAULT_TAB_WIDTH);
        
        return settings;
    }
    
    /**
     * تحويل dp إلى px
     */
    public static float dpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }
    
    /**
     * تحويل sp إلى px
     */
    public static float spToPx(Context context, float sp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }
    
    /**
     * إظهار لوحة المفاتيح
     */
    public static void showKeyboard(Context context, android.view.View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    /**
     * إخفاء لوحة المفاتيح
     */
    public static void hideKeyboard(Context context, android.view.View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    /**
     * حساب عدد الأسطر في النص
     */
    public static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        return text.split("\n").length;
    }
    
    /**
     * حساب عدد الأسطر في النص (بما في ذلك الأسطر الفارغة)
     */
    public static int countLinesIncludingEmpty(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        int count = 1; // Start with at least one line
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
    
    /**
     * الحصول على رقم السطر من الموضع
     */
    public static int getLineNumber(String text, int position) {
        if (text == null || text.isEmpty() || position < 0 || position > text.length()) {
            return 1;
        }
        
        int lineNumber = 1;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }
    
    /**
     * الحصول على موضع بداية السطر
     */
    public static int getLineStart(String text, int lineNumber) {
        if (text == null || text.isEmpty() || lineNumber <= 1) {
            return 0;
        }
        
        int currentLine = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                currentLine++;
                if (currentLine == lineNumber) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }
    
    /**
     * الحصول على موضع نهاية السطر
     */
    public static int getLineEnd(String text, int lineNumber) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int currentLine = 1;
        int startPos = getLineStart(text, lineNumber);
        
        for (int i = startPos; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i;
            }
        }
        return text.length();
    }
    
    /**
     * الحصول على نص السطر
     */
    public static String getLineText(String text, int lineNumber) {
        int start = getLineStart(text, lineNumber);
        int end = getLineEnd(text, lineNumber);
        return text.substring(start, end);
    }
    
    /**
     * التحقق من أن النص كود Python صالح
     */
    public static boolean isValidPythonCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - check for balanced brackets and quotes
        return isBalancedBrackets(code) && isBalancedQuotes(code);
    }
    
    /**
     * التحقق من توازن الأقواس
     */
    public static boolean isBalancedBrackets(String text) {
        java.util.Stack<Character> stack = new java.util.Stack<>();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')' || c == ']' || c == '}') {
                if (stack.isEmpty()) {
                    return false;
                }
                
                char open = stack.pop();
                if (!isMatchingBracket(open, c)) {
                    return false;
                }
            }
        }
        
        return stack.isEmpty();
    }
    
    /**
     * التحقق من تطابق الأقواس
     */
    private static boolean isMatchingBracket(char open, char close) {
        return (open == '(' && close == ')') ||
               (open == '[' && close == ']') ||
               (open == '{' && close == '}');
    }
    
    /**
     * التحقق من توازن علامات الاقتباس
     */
    public static boolean isBalancedQuotes(String text) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inTripleQuote = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char nextChar = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            char prevChar = i > 0 ? text.charAt(i - 1) : 0;
            
            // Handle triple quotes first
            if (c == '"' && nextChar == '"' && text.length() > i + 2 && text.charAt(i + 2) == '"') {
                if (!inSingleQuote) {
                    inTripleQuote = !inTripleQuote;
                    i += 2; // Skip the next two quotes
                }
            } else if (c == '\'' && nextChar == '\'' && text.length() > i + 2 && text.charAt(i + 2) == '\'') {
                if (!inDoubleQuote) {
                    inTripleQuote = !inTripleQuote;
                    i += 2; // Skip the next two quotes
                }
            } else if (c == '"' && prevChar != '\\' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '\'' && prevChar != '\\' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            }
        }
        
        return !inSingleQuote && !inDoubleQuote && !inTripleQuote;
    }
    
    /**
     * إنشاء نص مُظلل لعنصر من قائمة الكود
     */
    public static SpannableString createHighlightedText(String text, int color, boolean bold, boolean italic) {
        SpannableString spannable = new SpannableString(text);
        
        if (color != 0) {
            spannable.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        int style = android.graphics.Typeface.NORMAL;
        if (bold && italic) {
            style = android.graphics.Typeface.BOLD_ITALIC;
        } else if (bold) {
            style = android.graphics.Typeface.BOLD;
        } else if (italic) {
            style = android.graphics.Typeface.ITALIC;
        }
        
        spannable.setSpan(new StyleSpan(style), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return spannable;
    }
    
    /**
     * فئة إعدادات المحرر
     */
    public static class EditorSettings {
        public float fontSize = DEFAULT_FONT_SIZE;
        public String theme = THEME_LIGHT;
        public boolean autoIndent = DEFAULT_AUTO_INDENT;
        public boolean lineNumbers = DEFAULT_LINE_NUMBERS;
        public boolean wordWrap = DEFAULT_WORD_WRAP;
        public int tabWidth = DEFAULT_TAB_WIDTH;
    }
    
    /**
     * معلومات الموضع في النص
     */
    public static class TextPosition {
        public int line;
        public int column;
        public int offset;
        
        public TextPosition(int line, int column, int offset) {
            this.line = line;
            this.column = column;
            this.offset = offset;
        }
    }
}