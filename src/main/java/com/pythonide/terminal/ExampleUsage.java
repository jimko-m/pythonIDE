package com.pythonide.terminal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * مثال شامل لاستخدام Terminal المدمجة
 */
public class ExampleUsage extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        demonstrateTerminalFeatures();
    }
    
    /**
     * عرض جميع ميزات Terminal المدمجة
     */
    private void demonstrateTerminalFeatures() {
        
        // 1. بدء Terminal Activity
        startTerminalActivity();
        
        // 2. تنفيذ أوامر من التطبيق
        demonstrateCommandExecution();
        
        // 3. معالجة الأخطاء
        demonstrateErrorHandling();
        
        // 4. إدارة التاريخ
        demonstrateHistoryManagement();
        
        // 5. استخدام الوسائل المساعدة
        demonstrateUtils();
    }
    
    /**
     * مثال: بدء Terminal Activity
     */
    private void startTerminalActivity() {
        // بدء Terminal في Activity منفصل
        Intent terminalIntent = new Intent(this, TerminalActivity.class);
        startActivity(terminalIntent);
        
        // بدء Terminal مع بيانات إضافية
        Intent extendedTerminalIntent = new Intent(this, TerminalActivity.class);
        extendedTerminalIntent.putExtra("initial_command", "print('مرحباً من Terminal!')");
        extendedTerminalIntent.putExtra("working_directory", "/sdcard/python_projects");
        extendedTerminalIntent.putExtra("show_welcome", true);
        startActivity(extendedTerminalIntent);
    }
    
    /**
     * مثال: تنفيذ أوامر
     */
    private void demonstrateCommandExecution() {
        TerminalCommandExecutor executor = new TerminalCommandExecutor(this);
        
        // تنفيذ أمر بسيط
        TerminalCommandExecutor.CommandResult result1 = 
            executor.executeCommand("print('مرحباً بالعالم!')");
        
        if (result1.isSuccessful()) {
            System.out.println("النتيجة: " + result1.getOutput());
        } else {
            System.out.println("خطأ: " + result1.stderr);
        }
        
        // تنفيذ أمر مع timeout
        executor.executeCommandAsync("import time; time.sleep(2); print('منتهي')", 
            new TerminalCommandExecutor.CommandCallback() {
                @Override
                public void onComplete(TerminalCommandExecutor.CommandResult result) {
                    if (result.isSuccessful()) {
                        showToast("تم تنفيذ الأمر بنجاح");
                    } else {
                        showToast("خطأ: " + result.stderr);
                    }
                }
            });
        
        // تنفيذ أمر في الخلفية
        int processId = executor.executeBackgroundCommand(
            "python -c \"import time; print('Process started'); time.sleep(5); print('Process finished')\""
        );
        
        if (processId > 0) {
            showToast("تم بدء عملية في الخلفية برقم: " + processId);
            
            // مراقبة العملية
            monitorProcess(executor, processId);
        }
    }
    
    /**
     * مثال: معالجة الأخطاء
     */
    private void demonstrateErrorHandling() {
        TerminalErrorHandler errorHandler = new TerminalErrorHandler(this);
        
        // تسجيل خطأ عادي
        try {
            // محاكاة خطأ
            throw new RuntimeException("خطأ اختبار في النهائية");
        } catch (Exception e) {
            errorHandler.logError("EXECUTION", "خطأ في تنفيذ أمر Python", e);
        }
        
        // تسجيل تحذير
        errorHandler.logWarning("استهلاك عالي للذاكرة");
        
        // تسجيل معلومات
        errorHandler.logInfo("تم بدء Terminal بنجاح");
        
        // الحصول على آخر الأخطاء
        TerminalErrorHandler.ErrorEntry[] recentErrors = 
            errorHandler.getRecentErrors(5).toArray(new TerminalErrorHandler.ErrorEntry[0]);
        
        // تحليل الخطأ
        String analysis = TerminalErrorHandler.ErrorAnalyzer.analyzeError(
            "NameError: name 'variable' is not defined"
        );
        showToast("تحليل الخطأ: " + analysis);
        
        // اقتراح حلول
        java.util.List<String> solutions = 
            TerminalErrorHandler.ErrorAnalyzer.suggestSolutions("No module named 'requests'");
        
        for (String solution : solutions) {
            System.out.println("حل مقترح: " + solution);
        }
    }
    
    /**
     * مثال: إدارة التاريخ
     */
    private void demonstrateHistoryManagement() {
        TerminalHandler terminalHandler = new TerminalHandler(this);
        
        // إضافة أوامر للتاريخ
        terminalHandler.addToHistory("print('أمر 1')");
        terminalHandler.addToHistory("import sys");
        terminalHandler.addToHistory("sys.version");
        terminalHandler.addToHistory("2 + 2");
        
        // الحصول على أمر من التاريخ
        String command = terminalHandler.getHistoryCommand(1);
        if (command != null) {
            showToast("الأمر رقم 1: " + command);
        }
        
        // عرض جميع التاريخ
        for (int i = 0; i < terminalHandler.getHistorySize(); i++) {
            String histCommand = terminalHandler.getHistoryCommand(i);
            System.out.println((i + 1) + ": " + histCommand);
        }
        
        // مسح التاريخ
        terminalHandler.clearHistory();
        
        // حفظ التاريخ في ملف
        terminalHandler.saveHistory();
    }
    
    /**
     * مثال: استخدام الوسائل المساعدة
     */
    private void demonstrateUtils() {
        Context context = this;
        
        // تطبيق ألوان على النص
        String coloredText = TerminalUtils.applyColors(
            "\033[31mخطأ\033[0m \033[32mنجح\033[0m", context).toString();
        System.out.println("النص الملون: " + coloredText);
        
        // تنسيق أحجام الملفات
        String sizeFormat = TerminalUtils.formatFileSize(1024 * 1024 * 5); // 5MB
        showToast("حجم الملف: " + sizeFormat);
        
        // التحقق من اسم ملف
        boolean validName = TerminalUtils.isValidFileName("my_file.py");
        showToast("اسم الملف صحيح: " + validName);
        
        // إنشاء اسم ملف آمن
        String safeName = TerminalUtils.createSafeFileName("ملف<Python>test.txt");
        showToast("اسم الملف الآمن: " + safeName);
        
        // تحديد لغة الأمر
        boolean isPython = TerminalUtils.isPythonCommand("print('test')");
        showToast("أمر Python: " + isPython);
        
        // تنظيف المدخلات
        String cleanedInput = TerminalUtils.sanitizeInput("<script>alert('test')</script>");
        showToast("مدخل نظيف: " + cleanedInput);
        
        // معلومات النظام
        String systemInfo = TerminalUtils.getSystemInfo();
        System.out.println(systemInfo);
        
        // تقسيم النص
        java.util.List<String> wrappedLines = TerminalUtils.wrapText(
            "هذا نص طويل جداً يحتاج لتقسيم إلى أسطر متعددة بشكل تلقائي", 30);
        for (String line : wrappedLines) {
            System.out.println("-" + line);
        }
    }
    
    /**
     * مراقبة عملية في الخلفية
     */
    private void monitorProcess(TerminalCommandExecutor executor, int processId) {
        // فحص حالة العملية كل ثانيتين
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                java.util.Map<Integer, TerminalCommandExecutor.ProcessInfo> processes = 
                    executor.getActiveProcesses();
                
                TerminalCommandExecutor.ProcessInfo process = processes.get(processId);
                if (process == null) {
                    // العملية انتهت
                    showToast("انتهت العملية " + processId);
                    cancel();
                    return;
                }
                
                // عرض حالة العملية
                System.out.println("العملية " + processId + ": " + 
                    (process.isRunning() ? "قيد التشغيل" : "متوقفة") +
                    " - وقت التشغيل: " + (process.getRuntime() / 1000) + " ثانية");
            }
        }, 0, 2000);
    }
    
    /**
     * عرض toast رسالة
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * مثال متقدم: Terminal مخصص
     */
    public void createCustomTerminal() {
        // إنشاء Terminal مخصص مع إعدادات مختلفة
        Intent customIntent = new Intent(this, TerminalActivity.class);
        customIntent.putExtra("theme", "dark");
        customIntent.putExtra("font_size", 16);
        customIntent.putExtra("auto_scroll", true);
        customIntent.putExtra("command_prompt", ">>> ");
        customIntent.putExtra("enable_history", true);
        customIntent.putExtra("max_history_size", 50);
        customIntent.putExtra("working_directory", getFilesDir().getAbsolutePath());
        startActivity(customIntent);
    }
    
    /**
     * مثال: دمج Terminal مع ميزات أخرى
     */
    public void integrateTerminalWithEditor() {
        // فتح ملف في المحرر وتشغيله في Terminal
        String pythonFile = "test.py";
        
        // كتابة كود Python
        TerminalUtils.writeFileContent(this, pythonFile, 
            "print('Hello from file!')\n" +
            "import datetime\n" +
            "print('Current time:', datetime.datetime.now())");
        
        // تشغيل الملف في Terminal
        Intent runIntent = new Intent(this, TerminalActivity.class);
        runIntent.putExtra("initial_command", "exec(open('" + pythonFile + "').read())");
        startActivity(runIntent);
    }
}