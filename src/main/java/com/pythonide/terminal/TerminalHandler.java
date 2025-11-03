package com.pythonide.terminal;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * معالج الأوامر والفحوصات للنهائية المدمجة
 */
public class TerminalHandler {
    
    private static final String TAG = "TerminalHandler";
    private static final String HISTORY_FILE = "terminal_history.txt";
    private static final String LOG_FILE = "terminal_log.txt";
    
    private Context context;
    private List<String> commandHistory;
    private File historyFile;
    private File logFile;
    
    public TerminalHandler(Context context) {
        this.context = context;
        this.commandHistory = new ArrayList<>();
        this.historyFile = new File(context.getFilesDir(), HISTORY_FILE);
        this.logFile = new File(context.getFilesDir(), LOG_FILE);
        loadHistory();
    }
    
    /**
     * تحميل تاريخ الأوامر من الملف
     */
    private void loadHistory() {
        try {
            if (historyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(historyFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        commandHistory.add(line.trim());
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "لا يمكن تحميل تاريخ الأوامر: " + e.getMessage());
        }
    }
    
    /**
     * حفظ تاريخ الأوامر في الملف
     */
    public void saveHistory() {
        try {
            FileWriter writer = new FileWriter(historyFile);
            for (String command : commandHistory) {
                writer.write(command + "\n");
            }
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "خطأ في حفظ تاريخ الأوامر: " + e.getMessage());
        }
    }
    
    /**
     * إضافة أمر جديد للتاريخ
     */
    public void addToHistory(String command) {
        if (command == null || command.trim().isEmpty()) return;
        
        // تجنب التكرار في آخر أمر
        if (commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(command)) {
            commandHistory.add(command);
            // حفظ فقط آخر 100 أمر
            if (commandHistory.size() > 100) {
                commandHistory = commandHistory.subList(commandHistory.size() - 100, commandHistory.size());
            }
            saveHistory();
        }
    }
    
    /**
     * الحصول على أمر من التاريخ
     */
    public String getHistoryCommand(int index) {
        if (index >= 0 && index < commandHistory.size()) {
            return commandHistory.get(index);
        }
        return null;
    }
    
    /**
     * الحصول على حجم التاريخ
     */
    public int getHistorySize() {
        return commandHistory.size();
    }
    
    /**
     * مسح التاريخ
     */
    public void clearHistory() {
        commandHistory.clear();
        if (historyFile.exists()) {
            historyFile.delete();
        }
    }
    
    /**
     * تسجيل حدث في ملف السجل
     */
    public void log(String message) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = "[" + timestamp + "] " + message;
            
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(logEntry + "\n");
            writer.close();
            
            Log.i(TAG, message);
        } catch (IOException e) {
            Log.e(TAG, "خطأ في تسجيل الحدث: " + e.getMessage());
        }
    }
    
    /**
     * الحصول على آخر سجلات الأخطاء
     */
    public String getRecentLogs(int lines) {
        try {
            if (!logFile.exists()) return "";
            
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            List<String> logLines = new ArrayList<>();
            String line;
            
            // قراءة آخر lines من الملف
            while ((line = reader.readLine()) != null) {
                logLines.add(line);
                if (logLines.size() > lines) {
                    logLines.remove(0);
                }
            }
            reader.close();
            
            StringBuilder result = new StringBuilder();
            for (String logLine : logLines) {
                result.append(logLine).append("\n");
            }
            
            return result.toString();
        } catch (IOException e) {
            return "خطأ في قراءة السجلات: " + e.getMessage();
        }
    }
    
    /**
     * التحقق من وجود Python
     */
    public boolean isPythonAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("which python3");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * الحصول على إصدار Python
     */
    public String getPythonVersion() {
        try {
            Process process = Runtime.getRuntime().exec("python3 --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String version = reader.readLine();
            reader.close();
            return version != null ? version : "غير معروف";
        } catch (Exception e) {
            return "خطأ: " + e.getMessage();
        }
    }
    
    /**
     * تنفيذ أمر نظام والتحقق من النتيجة
     */
    public CommandResult executeSystemCommand(String command) {
        CommandResult result = new CommandResult();
        
        try {
            Process process = Runtime.getRuntime().exec(command);
            result.exitCode = process.waitFor();
            
            // قراءة stdout
            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder stdout = new StringBuilder();
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                stdout.append(line).append("\n");
            }
            result.stdout = stdout.toString();
            stdoutReader.close();
            
            // قراءة stderr
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder stderr = new StringBuilder();
            while ((line = stderrReader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
            result.stderr = stderr.toString();
            stderrReader.close();
            
            log("تنفيذ أمر: " + command + " (exit code: " + result.exitCode + ")");
            
        } catch (Exception e) {
            result.stderr = "خطأ في تنفيذ الأمر: " + e.getMessage();
            log("خطأ في تنفيذ الأمر: " + command + " - " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Result class لأوامر النظام
     */
    public static class CommandResult {
        public int exitCode = 0;
        public String stdout = "";
        public String stderr = "";
        public boolean isSuccessful() {
            return exitCode == 0;
        }
    }
    
    /**
     * الحصول على معلومات النظام
     */
    public String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("معلومات النظام:\n");
        
        // Android version
        info.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        info.append("API Level: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
        
        // Python availability
        if (isPythonAvailable()) {
            info.append("Python: متوفر\n");
            info.append("Python Version: ").append(getPythonVersion()).append("\n");
        } else {
            info.append("Python: غير متوفر\n");
        }
        
        // Available space
        File filesDir = context.getFilesDir();
        long availableBytes = filesDir.getFreeSpace();
        long totalBytes = filesDir.getTotalSpace();
        long usedBytes = totalBytes - availableBytes;
        
        info.append("المساحة المتوفرة: ").append(formatBytes(availableBytes)).append("\n");
        info.append("المساحة المستخدمة: ").append(formatBytes(usedBytes)).append("\n");
        info.append("إجمالي المساحة: ").append(formatBytes(totalBytes)).append("\n");
        
        return info.toString();
    }
    
    /**
     * تنسيق حجم البيانات
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        else return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}