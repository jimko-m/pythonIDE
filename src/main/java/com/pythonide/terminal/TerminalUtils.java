package com.pythonide.terminal;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * وسائل مساعدة للنهائية المدمجة
 */
public class TerminalUtils {
    
    private static final String TAG = "TerminalUtils";
    
    // Color codes for terminal
    public static final String COLOR_RESET = "\u001b[0m";
    public static final String COLOR_RED = "\u001b[31m";
    public static final String COLOR_GREEN = "\u001b[32m";
    public static final String COLOR_YELLOW = "\u001b[33m";
    public static final String COLOR_BLUE = "\u001b[34m";
    public static final String COLOR_PURPLE = "\u001b[35m";
    public static final String COLOR_CYAN = "\u001b[36m";
    public static final String COLOR_WHITE = "\u001b[37m";
    
    /**
     * تطبيق الألوان على النص
     */
    public static CharSequence applyColors(String text, Context context) {
        if (text == null) return "";
        
        SpannableString spannable = new SpannableString(text);
        
        // تطبيق ألوان ANSI codes
        spannable = applyAnsiColors(spannable, context);
        
        // تطبيق ألوان الكلمات المفتاحية
        spannable = applySyntaxHighlighting(spannable, context);
        
        return spannable;
    }
    
    /**
     * تطبيق ألوان ANSI codes
     */
    private static SpannableString applyAnsiColors(SpannableString text, Context context) {
        String processed = text.toString();
        Pattern pattern = Pattern.compile("\u001b\\[([0-9;]+)m");
        Matcher matcher = pattern.matcher(processed);
        
        int currentColor = getColor(context, "color_terminal_white");
        int lastIndex = 0;
        
        while (matcher.find()) {
            // إضافة النص العادي قبل color code
            if (matcher.start() > lastIndex) {
                String normalText = processed.substring(lastIndex, matcher.start());
                spannable.setSpan(new ForegroundColorSpan(currentColor), 
                    lastIndex, matcher.start(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            // معالجة color codes
            String codes = matcher.group(1);
            currentColor = parseAnsiColor(codes, currentColor, context);
            
            lastIndex = matcher.end();
        }
        
        // تطبيق color على النص المتبقي
        if (lastIndex < processed.length()) {
            spannable.setSpan(new ForegroundColorSpan(currentColor), 
                lastIndex, processed.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        return spannable;
    }
    
    /**
     * معالجة ألوان ANSI codes
     */
    private static int parseAnsiColor(String codes, int currentColor, Context context) {
        if (codes.equals("0")) {
            return getColor(context, "color_terminal_white"); // reset
        } else if (codes.equals("31")) {
            return getColor(context, "android.R.color.holo_red_light");
        } else if (codes.equals("32")) {
            return getColor(context, "color_terminal_green");
        } else if (codes.equals("33")) {
            return getColor(context, "color_terminal_yellow");
        } else if (codes.equals("34")) {
            return getColor(context, "color_terminal_blue");
        }
        return currentColor;
    }
    
    /**
     * تطبيق syntax highlighting
     */
    private static SpannableString applySyntaxHighlighting(SpannableString text, Context context) {
        String content = text.toString();
        int color = getColor(context, "color_terminal_green");
        
        // Python keywords
        String[] keywords = {"def", "class", "if", "else", "elif", "for", "while", "try", "except", 
                            "import", "from", "as", "return", "yield", "break", "continue", 
                            "pass", "with", "lambda", "and", "or", "not", "in", "is", "None", 
                            "True", "False"};
        
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                text.setSpan(new ForegroundColorSpan(getColor(context, "color_terminal_yellow")), 
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        
        // Strings
        Pattern stringPattern = Pattern.compile("[\"'][^\"']*[\"']");
        Matcher stringMatcher = stringPattern.matcher(content);
        
        while (stringMatcher.find()) {
            text.setSpan(new ForegroundColorSpan(getColor(context, "color_terminal_purple")), 
                stringMatcher.start(), stringMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // Comments
        Pattern commentPattern = Pattern.compile("#.*");
        Matcher commentMatcher = commentPattern.matcher(content);
        
        while (commentMatcher.find()) {
            text.setSpan(new ForegroundColorSpan(getColor(context, "color_terminal_gray")), 
                commentMatcher.start(), commentMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        return text;
    }
    
    /**
     * الحصول على لون من الموارد
     */
    private static int getColor(Context context, String colorName) {
        try {
            // محاولة الحصول على color من الـ context
            if (colorName.startsWith("color_")) {
                int resId = context.getResources().getIdentifier(colorName, "color", context.getPackageName());
                if (resId != 0) {
                    return context.getResources().getColor(resId);
                }
            }
            
            // ألوان افتراضية
            switch (colorName) {
                case "color_terminal_green": return 0xFF00FF00;
                case "color_terminal_white": return 0xFFFFFFFF;
                case "color_terminal_gray": return 0xFF888888;
                case "color_terminal_black": return 0xFF000000;
                case "color_terminal_dark_gray": return 0xFF333333;
                case "color_terminal_blue": return 0xFF007ACC;
                case "color_terminal_yellow": return 0xFFFFFF00;
                case "color_terminal_purple": return 0xFFFF00FF;
                case "android.R.color.holo_red_light": return 0xFFFF4444;
                default: return 0xFF00FF00;
            }
        } catch (Exception e) {
            return 0xFF00FF00; // أخضر افتراضي
        }
    }
    
    /**
     * تقسيم النص الطويل إلى أسطر
     */
    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * حساب عرض النص بالـ monospace font
     */
    public static int calculateMonospaceWidth(String text) {
        if (text == null) return 0;
        
        // تقدير تقريبي لعرض النص بـ monospace font
        // 1 character ≈ 8 pixels في monospace font متوسط الحجم
        return text.length() * 8;
    }
    
    /**
     * إنشاء timestamp منسق
     */
    public static String createTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * إنشاء timestamp مع التاريخ
     */
    public static String createFullTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * التحقق من صحة اسم الملف
     */
    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        
        // فحص الأحرف غير المسموحة
        String invalidChars = "<>:\"/\\|?*";
        for (char c : fileName.toCharArray()) {
            if (invalidChars.contains(String.valueOf(c))) {
                return false;
            }
        }
        
        // فحص الطول
        return fileName.length() <= 255;
    }
    
    /**
     * إنشاء اسم ملف آمن
     */
    public static String createSafeFileName(String originalName) {
        if (originalName == null) return "untitled";
        
        // إزالة الأحرف غير المسموحة
        String safeName = originalName.replaceAll("[<>:\"/\\|?*]", "_");
        
        // استبدال المسافات بـ underscore
        safeName = safeName.replaceAll("\\s+", "_");
        
        // إزالة النقاط المتعددة
        safeName = safeName.replaceAll("\\.+", ".");
        
        // التأكد من عدم كونه فارغ
        if (safeName.isEmpty()) {
            safeName = "untitled";
        }
        
        return safeName;
    }
    
    /**
     * التحقق من كون الأمر Python
     */
    public static boolean isPythonCommand(String command) {
        if (command == null) return false;
        
        String trimmed = command.trim().toLowerCase();
        
        // أوامر Python الشائعة
        if (trimmed.startsWith("python") || 
            trimmed.startsWith("py") ||
            trimmed.contains("print(") ||
            trimmed.contains("import ") ||
            trimmed.contains("from ") ||
            trimmed.startsWith("def ") ||
            trimmed.startsWith("class ") ||
            trimmed.contains("=")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * الحصول على امتداد الملف
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * تحويل أحجام الملفات
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        else return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * التحقق من وجود الملف
     */
    public static boolean fileExists(Context context, String fileName) {
        if (fileName == null) return false;
        
        File file = new File(context.getFilesDir(), fileName);
        return file.exists() && file.isFile();
    }
    
    /**
     * قراءة محتوى الملف
     */
    public static String readFileContent(Context context, String fileName) {
        try {
            if (fileExists(context, fileName)) {
                File file = new File(context.getFilesDir(), fileName);
                java.io.FileReader reader = new java.io.FileReader(file);
                java.io.BufferedReader bufferedReader = new java.io.BufferedReader(reader);
                
                StringBuilder content = new StringBuilder();
                String line;
                
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                bufferedReader.close();
                reader.close();
                
                return content.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "خطأ في قراءة الملف: " + fileName, e);
        }
        
        return null;
    }
    
    /**
     * حفظ محتوى في ملف
     */
    public static boolean writeFileContent(Context context, String fileName, String content) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "خطأ في حفظ الملف: " + fileName, e);
            return false;
        }
    }
    
    /**
     * حذف ملف
     */
    public static boolean deleteFile(Context context, String fileName) {
        try {
            if (fileExists(context, fileName)) {
                File file = new File(context.getFilesDir(), fileName);
                return file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في حذف الملف: " + fileName, e);
        }
        return false;
    }
    
    /**
     * إنشاء مجلد
     */
    public static boolean createDirectory(Context context, String dirName) {
        try {
            File dir = new File(context.getFilesDir(), dirName);
            return dir.exists() || dir.mkdirs();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إنشاء المجلد: " + dirName, e);
            return false;
        }
    }
    
    /**
     * قائمة ملفات المجلد
     */
    public static List<String> listFiles(Context context, String directory) {
        List<String> files = new ArrayList<>();
        
        try {
            File dir = new File(context.getFilesDir(), directory);
            if (dir.exists() && dir.isDirectory()) {
                File[] fileList = dir.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        if (file.isFile()) {
                            files.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في سرد الملفات: " + directory, e);
        }
        
        return files;
    }
    
    /**
     * تنظيف النص للأمان
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        
        // إزالة أو استبدال الأحرف الخطيرة
        String cleaned = input.replaceAll("[<>\"'&]", "");
        
        // تحديد الطول الأقصى
        if (cleaned.length() > 1000) {
            cleaned = cleaned.substring(0, 1000);
        }
        
        return cleaned.trim();
    }
    
    /**
     * طباعة رسالة في اللهجة العربية
     */
    public static void printArabicLog(String tag, String message) {
        Log.i(tag, message + " (مطبوع بواسطة TerminalUtils)");
    }
    
    /**
     * إنشاء معلومات النظام
     */
    public static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== معلومات النظام ===\n");
        info.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        info.append("API Level: ").append(append(android.os.Build.VERSION.SDK_INT)).append("\n");
        info.append("Device: ").append(android.os.Build.MODEL).append("\n");
        info.append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
        info.append("Current Time: ").append(createFullTimestamp()).append("\n");
        return info.toString();
    }
    
    // helper method to append numbers as strings
    private static String append(int value) {
        return String.valueOf(value);
    }
}