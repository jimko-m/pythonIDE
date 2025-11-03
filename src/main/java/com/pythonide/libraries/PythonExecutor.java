package com.pythonide.libraries;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PythonExecutor - مشغل أوامر Python
 * 
 * المسؤوليات الرئيسية:
 * - تنفيذ أوامر pip install/uninstall
 * - تشغيل سكريبت Python
 * - إدارة بيئات Python المختلفة
 * - معالجة الأخطاء والمخرجات
 * - تتبع العمليات الطويلة
 */
public class PythonExecutor {
    
    private static final String TAG = "PythonExecutor";
    
    private Context context;
    private ExecutorService executorService;
    private String pythonPath;
    private String pipPath;
    private boolean isInitialized;
    
    /**
     * إنشاء مشغل Python
     */
    public PythonExecutor(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.isInitialized = false;
        
        initializePaths();
    }
    
    /**
     * تهيئة مسارات Python و pip
     */
    private void initializePaths() {
        try {
            // البحث عن مسار Python
            pythonPath = findPythonPath();
            
            // البحث عن مسار pip
            pipPath = findPipPath();
            
            isInitialized = pythonPath != null && pipPath != null;
            
            if (isInitialized) {
                Log.i(TAG, "تم العثور على Python في: " + pythonPath);
                Log.i(TAG, "تم العثور على pip في: " + pipPath);
            } else {
                Log.w(TAG, "لم يتم العثور على Python أو pip");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تهيئة مسارات Python", e);
        }
    }
    
    /**
     * البحث عن مسار Python
     */
    private String findPythonPath() {
        List<String> possiblePaths = Arrays.asList(
            "python3",
            "python",
            "/usr/bin/python3",
            "/usr/bin/python",
            "/bin/python3",
            "/bin/python",
            "/usr/local/bin/python3",
            "/usr/local/bin/python"
        );
        
        for (String path : possiblePaths) {
            if (isPythonAvailable(path)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * البحث عن مسار pip
     */
    private String findPipPath() {
        List<String> possiblePaths = Arrays.asList(
            "pip3",
            "pip",
            "/usr/bin/pip3",
            "/usr/bin/pip",
            "/bin/pip3",
            "/bin/pip",
            "/usr/local/bin/pip3",
            "/usr/local/bin/pip"
        );
        
        for (String path : possiblePaths) {
            if (isPipAvailable(path)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * فحص توفر Python
     */
    private boolean isPythonAvailable(String pythonPath) {
        try {
            Process process = new ProcessBuilder(pythonPath, "--version").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitCode = process.waitFor();
            
            return exitCode == 0 && line != null && line.startsWith("Python");
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * فحص توفر pip
     */
    private boolean isPipAvailable(String pipPath) {
        try {
            Process process = new ProcessBuilder(pipPath, "--version").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitCode = process.waitFor();
            
            return exitCode == 0 && line != null && line.toLowerCase().contains("pip");
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * تنفيذ أمر مع إرجاع رمز الخروج
     */
    public int executeCommand(List<String> command, String pythonVersion) {
        return executeCommand(command, pythonVersion, null);
    }
    
    /**
     * تنفيذ أمر مع مسار عمل مخصص
     */
    public int executeCommand(List<String> command, String workingDirectory) {
        return executeCommand(command, null, workingDirectory);
    }
    
    /**
     * تنفيذ أمر شامل
     */
    public int executeCommand(List<String> command, String pythonVersion, String workingDirectory) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return -1;
        }
        
        try {
            List<String> fullCommand = new ArrayList<>();
            
            // إضافة مسار Python إذا لزم الأمر
            if (!command.get(0).equals(pythonPath) && !command.get(0).equals("python") && 
                !command.get(0).equals("python3")) {
                fullCommand.add(pythonPath);
            }
            
            fullCommand.addAll(command);
            
            Log.i(TAG, "تنفيذ الأمر: " + String.join(" ", fullCommand));
            
            ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
            
            // تعيين مسار العمل
            if (workingDirectory != null) {
                File workingDir = new File(workingDirectory);
                if (workingDir.exists()) {
                    processBuilder.directory(workingDir);
                }
            }
            
            // إعداد البيئة
            Process process = processBuilder.start();
            
            // قراءة المخرجات في خيط منفصل
            readProcessOutput(process);
            
            // انتظار انتهاء العملية
            int exitCode = process.waitFor();
            
            Log.i(TAG, "انتهى الأمر برمز الخروج: " + exitCode);
            
            return exitCode;
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تنفيذ الأمر", e);
            return -1;
        }
    }
    
    /**
     * تنفيذ أمر مع إرجاع المخرجات
     */
    public String executeCommandWithOutput(List<String> command, String pythonVersion) {
        return executeCommandWithOutput(command, pythonVersion, null);
    }
    
    /**
     * تنفيذ أمر مع المخرجات ومسار العمل
     */
    public String executeCommandWithOutput(List<String> command, String pythonVersion, String workingDirectory) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return null;
        }
        
        try {
            List<String> fullCommand = new ArrayList<>();
            
            // إضافة مسار Python إذا لزم الأمر
            if (!command.get(0).equals(pythonPath) && !command.get(0).equals("python") && 
                !command.get(0).equals("python3")) {
                fullCommand.add(pythonPath);
            }
            
            fullCommand.addAll(command);
            
            Log.i(TAG, "تنفيذ الأمر مع مخرجات: " + String.join(" ", fullCommand));
            
            ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
            
            // تعيين مسار العمل
            if (workingDirectory != null) {
                File workingDir = new File(workingDirectory);
                if (workingDir.exists()) {
                    processBuilder.directory(workingDir);
                }
            }
            
            Process process = processBuilder.start();
            
            // قراءة المخرجات
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // خيط لقراءة المخرجات العادية
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        Log.d(TAG, "[OUTPUT] " + line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "خطأ في قراءة المخرجات", e);
                }
            });
            
            // خيط لقراءة أخطاء
            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        Log.w(TAG, "[ERROR] " + line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "خطأ في قراءة الأخطاء", e);
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            // انتظار انتهاء العملية
            int exitCode = process.waitFor();
            
            // انتظار انتهاء خيوط القراءة
            outputThread.join();
            errorThread.join();
            
            Log.i(TAG, "انتهى الأمر برمز الخروج: " + exitCode);
            
            if (exitCode != 0 && errorOutput.length() > 0) {
                Log.e(TAG, "أخطاء الأمر: " + errorOutput.toString());
            }
            
            return exitCode == 0 ? output.toString() : null;
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تنفيذ الأمر", e);
            return null;
        }
    }
    
    /**
     * تثبيت مكتبة
     */
    public int installPackage(String packageName) {
        return installPackage(packageName, null, null);
    }
    
    /**
     * تثبيت مكتبة بإصدار محدد
     */
    public int installPackage(String packageName, String version) {
        return installPackage(packageName, version, null);
    }
    
    /**
     * تثبيت مكتبة شامل
     */
    public int installPackage(String packageName, String version, String user) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return -1;
        }
        
        List<String> command = new ArrayList<>();
        command.add(pipPath);
        command.add("install");
        
        // إضافة خيارات التثبيت
        if (user != null) {
            command.add("--user");
        }
        
        command.add("--no-warn-script-location");
        
        // إضافة اسم المكتبة والإصدار
        if (version != null && !version.isEmpty()) {
            command.add(packageName + "==" + version);
        } else {
            command.add(packageName);
        }
        
        Log.i(TAG, "تثبيت " + packageName + (version != null ? " v" + version : ""));
        
        return executeCommand(command, null);
    }
    
    /**
     * إلغاء تثبيت مكتبة
     */
    public int uninstallPackage(String packageName) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return -1;
        }
        
        List<String> command = Arrays.asList(pipPath, "uninstall", "-y", packageName);
        
        Log.i(TAG, "إلغاء تثبيت " + packageName);
        
        return executeCommand(command, null);
    }
    
    /**
     * ترقية مكتبة
     */
    public int upgradePackage(String packageName) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return -1;
        }
        
        List<String> command = Arrays.asList(pipPath, "install", "--upgrade", packageName);
        
        Log.i(TAG, "ترقية " + packageName);
        
        return executeCommand(command, null);
    }
    
    /**
     * الحصول على قائمة المكتبات المثبتة
     */
    public String getInstalledPackages() {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return null;
        }
        
        List<String> command = Arrays.asList(pipPath, "list", "--format=json");
        
        Log.i(TAG, "الحصول على قائمة المكتبات المثبتة");
        
        return executeCommandWithOutput(command, null);
    }
    
    /**
     * فحص إصدار المكتبة
     */
    public String getPackageVersion(String packageName) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return null;
        }
        
        List<String> command = Arrays.asList(pipPath, "show", packageName);
        
        Log.i(TAG, "فحص إصدار " + packageName);
        
        String output = executeCommandWithOutput(command, null);
        if (output != null) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("Version: ")) {
                    return line.substring(9).trim();
                }
            }
        }
        
        return null;
    }
    
    /**
     * فحص توفر مكتبة
     */
    public boolean isPackageInstalled(String packageName) {
        String version = getPackageVersion(packageName);
        return version != null && !version.isEmpty();
    }
    
    /**
     * تشغيل سكريبت Python
     */
    public int runPythonScript(String scriptPath, List<String> arguments) {
        return runPythonScript(scriptPath, arguments, null);
    }
    
    /**
     * تشغيل سكريبت Python مع مسار عمل
     */
    public int runPythonScript(String scriptPath, List<String> arguments, String workingDirectory) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return -1;
        }
        
        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(scriptPath);
        
        if (arguments != null) {
            command.addAll(arguments);
        }
        
        Log.i(TAG, "تشغيل سكريبت Python: " + scriptPath);
        
        return executeCommand(command, null, workingDirectory);
    }
    
    /**
     * تنفيذ كود Python مباشرة
     */
    public int executePythonCode(String code) {
        if (!isInitialized) {
            Log.e(TAG, "لم يتم تهيئة PythonExecutor");
            return -1;
        }
        
        try {
            // إنشاء ملف مؤقت للكود
            File tempFile = File.createTempFile("python_code", ".py");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(code);
            }
            
            List<String> command = Arrays.asList(pythonPath, tempFile.getAbsolutePath());
            
            Log.i(TAG, "تنفيذ كود Python");
            
            int exitCode = executeCommand(command, null);
            
            // حذف الملف المؤقت
            tempFile.delete();
            
            return exitCode;
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تنفيذ كود Python", e);
            return -1;
        }
    }
    
    /**
     * الحصول على إصدار Python
     */
    public String getPythonVersion() {
        if (!isInitialized) {
            return null;
        }
        
        try {
            List<String> command = Arrays.asList(pythonPath, "--version");
            String output = executeCommandWithOutput(command, null);
            
            if (output != null) {
                return output.trim();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الحصول على إصدار Python", e);
        }
        
        return "Python غير محدد";
    }
    
    /**
     * الحصول على إصدار pip
     */
    public String getPipVersion() {
        if (!isInitialized) {
            return null;
        }
        
        try {
            List<String> command = Arrays.asList(pipPath, "--version");
            String output = executeCommandWithOutput(command, null);
            
            if (output != null) {
                return output.trim();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الحصول على إصدار pip", e);
        }
        
        return "pip غير محدد";
    }
    
    /**
     * التحقق من صحة التهيئة
     */
    public boolean isReady() {
        return isInitialized && pythonPath != null && pipPath != null;
    }
    
    /**
     * قراءة مخرجات العملية
     */
    private void readProcessOutput(Process process) {
        // خيط لقراءة المخرجات العادية
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "[OUTPUT] " + line);
                }
            } catch (IOException e) {
                Log.e(TAG, "خطأ في قراءة المخرجات", e);
            }
        });
        
        // خيط لقراءة الأخطاء
        Thread errorThread = new Thread(() -> {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    Log.w(TAG, "[ERROR] " + line);
                }
            } catch (IOException e) {
                Log.e(TAG, "خطأ في قراءة الأخطاء", e);
            }
        });
        
        outputThread.start();
        errorThread.start();
    }
    
    /**
     * تنفيذ أمر بطريقة غير متزامنة
     */
    public void executeCommandAsync(List<String> command, CommandCallback callback) {
        executorService.execute(() -> {
            int exitCode = executeCommand(command, null);
            
            if (callback != null) {
                callback.onCommandComplete(exitCode);
            }
        });
    }
    
    /**
     * تثبيت مكتبة بطريقة غير متزامنة
     */
    public void installPackageAsync(String packageName, InstallCallback callback) {
        executorService.execute(() -> {
            try {
                callback.onInstallationStart(packageName);
                
                int exitCode = installPackage(packageName);
                
                boolean success = exitCode == 0;
                callback.onInstallationComplete(packageName, success, success ? "نجح التثبيت" : "فشل التثبيت");
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في التثبيت غير المتزامن", e);
                callback.onInstallationComplete(packageName, false, "خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * تنظيف الموارد
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * واجهة الاستدعاء للأمر
     */
    public interface CommandCallback {
        void onCommandComplete(int exitCode);
    }
    
    /**
     * واجهة الاستدعاء للتثبيت
     */
    public interface InstallCallback {
        void onInstallationStart(String packageName);
        void onInstallationProgress(String packageName, int progress);
        void onInstallationComplete(String packageName, boolean success, String message);
    }
}