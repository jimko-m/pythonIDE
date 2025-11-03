package com.pythonide.terminal;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * معالج الأخطاء المتقدم للنهائية المدمجة
 */
public class TerminalErrorHandler {
    
    private static final String TAG = "TerminalErrorHandler";
    private static final String ERROR_LOG_FILE = "terminal_errors.txt";
    
    private Context context;
    private File errorLogFile;
    private List<ErrorEntry> errorHistory;
    
    public TerminalErrorHandler(Context context) {
        this.context = context;
        this.errorLogFile = new File(context.getFilesDir(), ERROR_LOG_FILE);
        this.errorHistory = new ArrayList<>();
        loadErrorHistory();
    }
    
    /**
     * تسجيل خطأ جديد
     */
    public void logError(String category, String message, Throwable throwable) {
        ErrorEntry errorEntry = new ErrorEntry();
        errorEntry.timestamp = System.currentTimeMillis();
        errorEntry.category = category;
        errorEntry.message = message;
        errorEntry.stackTrace = getStackTrace(throwable);
        
        // إضافة للتاريخ
        errorHistory.add(errorEntry);
        
        // حفظ في الملف
        saveErrorToFile(errorEntry);
        
        // طباعة للسجل
        Log.e(TAG, "[" + category + "] " + message, throwable);
        
        // إبقاء فقط آخر 50 خطأ
        if (errorHistory.size() > 50) {
            errorHistory = errorHistory.subList(errorHistory.size() - 50, errorHistory.size());
        }
    }
    
    /**
     * تسجيل خطأ بدون stack trace
     */
    public void logError(String category, String message) {
        logError(category, message, null);
    }
    
    /**
     * تسجيل تحذير
     */
    public void logWarning(String message) {
        Log.w(TAG, message);
        appendToLogFile("WARNING", message, null);
    }
    
    /**
     * تسجيل معلومات
     */
    public void logInfo(String message) {
        Log.i(TAG, message);
        appendToLogFile("INFO", message, null);
    }
    
    /**
     * تسجيل debug
     */
    public void logDebug(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * الحصول على trace للمشكلة
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * حفظ الخطأ في الملف
     */
    private void saveErrorToFile(ErrorEntry errorEntry) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date(errorEntry.timestamp));
            
            FileWriter writer = new FileWriter(errorLogFile, true);
            writer.write("[" + timestamp + "] " + errorEntry.category + ": " + errorEntry.message + "\n");
            
            if (errorEntry.stackTrace != null && !errorEntry.stackTrace.isEmpty()) {
                writer.write("Stack Trace:\n");
                writer.write(errorEntry.stackTrace + "\n");
            }
            
            writer.write("---\n");
            writer.close();
            
        } catch (IOException e) {
            Log.e(TAG, "فشل في حفظ الخطأ في الملف: " + e.getMessage());
        }
    }
    
    /**
     * إضافة للملف السجل
     */
    private void appendToLogFile(String level, String message, String stackTrace) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            
            FileWriter writer = new FileWriter(errorLogFile, true);
            writer.write("[" + timestamp + "] " + level + ": " + message + "\n");
            
            if (stackTrace != null && !stackTrace.isEmpty()) {
                writer.write("Stack Trace:\n");
                writer.write(stackTrace + "\n");
            }
            
            writer.write("---\n");
            writer.close();
            
        } catch (IOException e) {
            Log.e(TAG, "فشل في إضافة للسجل: " + e.getMessage());
        }
    }
    
    /**
     * تحميل تاريخ الأخطاء
     */
    private void loadErrorHistory() {
        // سيتم تنفيذ هذا في التحسينات المستقبلية
        // عند الحاجة لقراءة الأخطاء من الملف
    }
    
    /**
     * الحصول على آخر الأخطاء
     */
    public List<ErrorEntry> getRecentErrors(int count) {
        if (count <= 0 || count >= errorHistory.size()) {
            return new ArrayList<>(errorHistory);
        }
        
        return errorHistory.subList(errorHistory.size() - count, errorHistory.size());
    }
    
    /**
     * الحصول على جميع الأخطاء
     */
    public List<ErrorEntry> getAllErrors() {
        return new ArrayList<>(errorHistory);
    }
    
    /**
     * مسح تاريخ الأخطاء
     */
    public void clearErrorHistory() {
        errorHistory.clear();
        if (errorLogFile.exists()) {
            errorLogFile.delete();
        }
    }
    
    /**
     * الحصول على عدد الأخطاء
     */
    public int getErrorCount() {
        return errorHistory.size();
    }
    
    /**
     * الحصول على إحصائيات الأخطاء
     */
    public String getErrorStatistics() {
        if (errorHistory.isEmpty()) {
            return "لا توجد أخطاء مسجلة";
        }
        
        // إحصائيات حسب الفئة
        java.util.Map<String, Integer> categoryStats = new java.util.HashMap<>();
        
        for (ErrorEntry error : errorHistory) {
            categoryStats.put(error.category, 
                categoryStats.getOrDefault(error.category, 0) + 1);
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("إحصائيات الأخطاء:\n");
        stats.append("إجمالي الأخطاء: ").append(errorHistory.size()).append("\n");
        
        for (java.util.Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
            stats.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        // آخر خطأ
        ErrorEntry lastError = errorHistory.get(errorHistory.size() - 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String lastErrorTime = sdf.format(new Date(lastError.timestamp));
        stats.append("آخر خطأ: ").append(lastErrorTime).append("\n");
        
        return stats.toString();
    }
    
    /**
     * فئة دخول الخطأ
     */
    public static class ErrorEntry {
        public long timestamp;
        public String category;
        public String message;
        public String stackTrace;
        
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
    
    /**
     * تحليل الأخطاء ومعالجة متقدمة
     */
    public static class ErrorAnalyzer {
        
        /**
         * تحليل سبب الخطأ من رسالة الخطأ
         */
        public static String analyzeError(String errorMessage) {
            if (errorMessage == null || errorMessage.isEmpty()) {
                return "رسالة خطأ فارغة";
            }
            
            String analysis = "تحليل الخطأ:\n";
            
            // أخطاء Python شائعة
            if (errorMessage.contains("No module named")) {
                analysis += "- الوحدة غير متوفرة، تحقق من تثبيت الحزمة\n";
            } else if (errorMessage.contains("NameError")) {
                analysis += "- متغير غير محدد أو خطأ في اسم المتغير\n";
            } else if (errorMessage.contains("SyntaxError")) {
                analysis += "- خطأ في تركيب الكود، تحقق من الصيغة\n";
            } else if (errorMessage.contains("IndentationError")) {
                analysis += "- خطأ في المسافات البادئة\n";
            } else if (errorMessage.contains("ImportError")) {
                analysis += "- مشكلة في استيراد وحدة\n";
            } else if (errorMessage.contains("FileNotFoundError")) {
                analysis += "- ملف غير موجود\n";
            } else if (errorMessage.contains("PermissionError")) {
                analysis += "- مشكلة في الصلاحيات\n";
            } else if (errorMessage.contains("UnicodeError")) {
                analysis += "- مشكلة في ترميز النصوص\n";
            } else if (errorMessage.contains("MemoryError")) {
                analysis += "- نفاد الذاكرة\n";
            } else if (errorMessage.contains("SystemError")) {
                analysis += "- خطأ في النظام\n";
            }
            
            // أخطاء Android
            if (errorMessage.contains("SecurityException")) {
                analysis += "- مشكلة أمنية، تحقق من الصلاحيات\n";
            } else if (errorMessage.contains("NetworkOnMainThreadException")) {
                analysis += "- استخدم Thread منفصل للشبكة\n";
            }
            
            analysis += "\nاقتراحات:\n";
            analysis += "1. تحقق من صحة الكود\n";
            analysis += "2. تأكد من توفر المتطلبات\n";
            analysis += "3. راجع السجلات المفصلة\n";
            
            return analysis;
        }
        
        /**
         * اقتراح حلول للأخطاء الشائعة
         */
        public static List<String> suggestSolutions(String errorMessage) {
            List<String> solutions = new ArrayList<>();
            
            if (errorMessage.contains("No module named")) {
                solutions.add("تثبيت الوحدة المفقودة باستخدام pip");
                solutions.add("التحقق من مسار Python");
            } else if (errorMessage.contains("Permission denied")) {
                solutions.add("تغيير صلاحيات الملف");
                solutions.add("التأكد من صلاحيات الكتابة في المجلد");
            } else if (errorMessage.contains("Connection refused")) {
                solutions.add("التأكد من تشغيل الخدمة");
                solutions.add("التحقق من رقم المنفذ");
            }
            
            return solutions;
        }
    }
}