package com.pythonide.terminal;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * منفذ الأوامر المتقدم للنهائية المدمجة
 */
public class TerminalCommandExecutor {
    
    private static final String TAG = "TerminalCommandExecutor";
    private static final long TIMEOUT_SECONDS = 30;
    
    private Context context;
    private ExecutorService executorService;
    private TerminalHandler terminalHandler;
    private TerminalErrorHandler errorHandler;
    
    // Processes قيد التشغيل
    private Map<Integer, ProcessInfo> activeProcesses;
    private int nextProcessId = 1;
    
    public TerminalCommandExecutor(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.terminalHandler = new TerminalHandler(context);
        this.errorHandler = new TerminalErrorHandler(context);
        this.activeProcesses = new ConcurrentHashMap<>();
    }
    
    /**
     * تنفيذ أمر في thread منفصل
     */
    public void executeCommandAsync(String command, CommandCallback callback) {
        executorService.execute(() -> {
            CommandResult result = executeCommand(command);
            if (callback != null) {
                callback.onComplete(result);
            }
        });
    }
    
    /**
     * تنفيذ أمر وحفظ النتيجة
     */
    public CommandResult executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new CommandResult(false, "", "أمر فارغ");
        }
        
        // إضافة للتاريخ
        terminalHandler.addToHistory(command);
        
        Log.i(TAG, "تنفيذ أمر: " + command);
        terminalHandler.log("تنفيذ أمر: " + command);
        
        // معالجة الأوامر المدمجة
        if (isBuiltInCommand(command)) {
            return executeBuiltInCommand(command);
        }
        
        try {
            // تحليل الأمر وتطبيق shell environment
            ShellCommand shellCommand = parseCommand(command);
            
            // تنفيذ الأمر
            long startTime = System.currentTimeMillis();
            int exitCode = executeShellCommand(shellCommand);
            long endTime = System.currentTimeMillis();
            
            CommandResult result = shellCommand.getResult();
            result.executionTime = endTime - startTime;
            result.command = command;
            
            terminalHandler.log("انتهاء أمر: " + command + " (exit code: " + exitCode + ")");
            
            return result;
            
        } catch (Exception e) {
            errorHandler.logError("EXECUTION", "خطأ في تنفيذ الأمر: " + command, e);
            return new CommandResult(false, "", "خطأ في تنفيذ الأمر: " + e.getMessage());
        }
    }
    
    /**
     * تنفيذ أمر في process منفصل
     */
    public int executeBackgroundCommand(String command) {
        try {
            ShellCommand shellCommand = parseCommand(command);
            
            ProcessBuilder pb = new ProcessBuilder(shellCommand.getCommandArray());
            Process process = pb.start();
            
            int processId = nextProcessId++;
            ProcessInfo processInfo = new ProcessInfo();
            processInfo.id = processId;
            processInfo.command = command;
            processInfo.process = process;
            processInfo.startTime = System.currentTimeMillis();
            processInfo.output = new ArrayList<>();
            processInfo.error = new ArrayList<>();
            
            activeProcesses.put(processId, processInfo);
            
            // قراءة output في threads منفصلة
            startProcessOutputReader(processInfo);
            
            errorHandler.logInfo("بدء تنفيذ أمر في الخلفية: " + command + " (PID: " + processId + ")");
            
            return processId;
            
        } catch (Exception e) {
            errorHandler.logError("BACKGROUND", "فشل في بدء أمر في الخلفية: " + command, e);
            return -1;
        }
    }
    
    /**
     * إيقاف process
     */
    public boolean killProcess(int processId) {
        ProcessInfo processInfo = activeProcesses.get(processId);
        if (processInfo == null) {
            return false;
        }
        
        try {
            processInfo.process.destroy();
            
            // انتظار لفترة قصيرة للتوقف الطبيعي
            boolean finished = processInfo.process.waitFor(3, TimeUnit.SECONDS);
            
            if (!finished) {
                processInfo.process.destroyForcibly();
                finished = processInfo.process.waitFor(1, TimeUnit.SECONDS);
            }
            
            activeProcesses.remove(processId);
            errorHandler.logInfo("تم إيقاف Process: " + processId);
            return finished;
            
        } catch (Exception e) {
            errorHandler.logError("KILL", "خطأ في إيقاف Process " + processId, e);
            return false;
        }
    }
    
    /**
     * الحصول على جميع processes النشطة
     */
    public Map<Integer, ProcessInfo> getActiveProcesses() {
        return new HashMap<>(activeProcesses);
    }
    
    /**
     * التحقق من كون الأمر مدمج
     */
    private boolean isBuiltInCommand(String command) {
        String trimmed = command.trim().toLowerCase();
        return trimmed.equals("clear") || 
               trimmed.equals("exit") || 
               trimmed.equals("quit") || 
               trimmed.equals("history") ||
               trimmed.equals("help") ||
               trimmed.equals("ps") ||
               trimmed.equals("kill") ||
               trimmed.startsWith("kill ");
    }
    
    /**
     * تنفيذ أمر مدمج
     */
    private CommandResult executeBuiltInCommand(String command) {
        String trimmed = command.trim().toLowerCase();
        
        switch (trimmed) {
            case "clear":
                return new CommandResult(true, "تم مسح الشاشة", "");
                
            case "exit":
            case "quit":
                return new CommandResult(true, "تم إنهاء البرنامج", "");
                
            case "history":
                return executeHistoryCommand();
                
            case "help":
                return executeHelpCommand();
                
            case "ps":
                return executePsCommand();
                
            default:
                if (trimmed.startsWith("kill ")) {
                    return executeKillCommand(trimmed);
                }
                return new CommandResult(false, "", "أمر مدمج غير معروف: " + command);
        }
    }
    
    /**
     * تنفيذ أمر التاريخ
     */
    private CommandResult executeHistoryCommand() {
        StringBuilder output = new StringBuilder("تاريخ الأوامر:\n");
        for (int i = 0; i < terminalHandler.getHistorySize(); i++) {
            output.append(i + 1).append(" ").append(terminalHandler.getHistoryCommand(i)).append("\n");
        }
        return new CommandResult(true, output.toString(), "");
    }
    
    /**
     * تنفيذ أمر المساعدة
     */
    private CommandResult executeHelpCommand() {
        String helpText = "أوامر Python IDE Terminal:\n\n" +
                         "clear - مسح الشاشة\n" +
                         "exit/quit - إنهاء البرنامج\n" +
                         "history - عرض تاريخ الأوامر\n" +
                         "ps - عرض العمليات النشطة\n" +
                         "kill <id> - إيقاف عملية\n" +
                         "help - عرض هذه المساعدة\n\n" +
                         "أمثلة Python:\n" +
                         "print('Hello, World!')\n" +
                         "import sys\n" +
                         "sys.version\n" +
                         "2 + 2\n\n" +
                         "الأوامر بالنص العربي مدعومة!";
        
        return new CommandResult(true, helpText, "");
    }
    
    /**
     * تنفيذ أمر العمليات
     */
    private CommandResult executePsCommand() {
        StringBuilder output = new StringBuilder("العمليات النشطة:\n");
        output.append("PID\tالأمر\tالوقت\n");
        output.append("----\t-------\t-------\n");
        
        for (ProcessInfo process : activeProcesses.values()) {
            long runtime = System.currentTimeMillis() - process.startTime;
            String timeStr = String.format("%.1fs", runtime / 1000.0);
            output.append(process.id).append("\t").append(process.command).append("\t").append(timeStr).append("\n");
        }
        
        return new CommandResult(true, output.toString(), "");
    }
    
    /**
     * تنفيذ أمر الإيقاف
     */
    private CommandResult executeKillCommand(String command) {
        try {
            String[] parts = command.split(" ");
            if (parts.length < 2) {
                return new CommandResult(false, "", "الاستخدام: kill <PID>");
            }
            
            int pid = Integer.parseInt(parts[1]);
            boolean success = killProcess(pid);
            
            if (success) {
                return new CommandResult(true, "تم إيقاف العملية " + pid, "");
            } else {
                return new CommandResult(false, "", "فشل في إيقاف العملية " + pid);
            }
            
        } catch (NumberFormatException e) {
            return new CommandResult(false, "", "PID غير صحيح");
        } catch (Exception e) {
            return new CommandResult(false, "", "خطأ: " + e.getMessage());
        }
    }
    
    /**
     * تحليل الأمر
     */
    private ShellCommand parseCommand(String command) {
        // تقسيم بسيط للأمر (يمكن تحسينه)
        String[] parts = command.split(" ");
        List<String> cmdList = new ArrayList<>();
        
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                cmdList.add(part.trim());
            }
        }
        
        if (cmdList.isEmpty()) {
            throw new IllegalArgumentException("أمر فارغ");
        }
        
        return new ShellCommand(cmdList);
    }
    
    /**
     * تنفيذ أمر shell
     */
    private int executeShellCommand(ShellCommand shellCommand) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(shellCommand.getCommandArray());
        pb.directory(context.getFilesDir());
        
        Process process = pb.start();
        
        // قراءة output و error
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        
        Thread stdoutThread = startStreamReader(process.getInputStream(), stdout);
        Thread stderrThread = startStreamReader(process.getErrorStream(), stderr);
        
        // انتظار انتهاء
        int exitCode = process.waitFor();
        
        // انتظار انتهاء threads القراءة
        stdoutThread.join();
        stderrThread.join();
        
        shellCommand.setResult(stdout.toString(), stderr.toString(), exitCode);
        
        return exitCode;
    }
    
    /**
     * بدء thread لقراءة stream
     */
    private Thread startStreamReader(InputStream inputStream, StringBuilder output) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                output.append("خطأ في قراءة Stream: ").append(e.getMessage()).append("\n");
            }
        });
        thread.start();
        return thread;
    }
    
    /**
     * بدء قارئ output للـ process
     */
    private void startProcessOutputReader(ProcessInfo processInfo) {
        // stdout reader
        executorService.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processInfo.process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processInfo.output.add(line);
                    // هنا يمكن إرسال البيانات للواجهة
                }
            } catch (IOException e) {
                processInfo.error.add("خطأ في قراءة stdout: " + e.getMessage());
            }
        });
        
        // stderr reader
        executorService.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processInfo.process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processInfo.error.add(line);
                }
            } catch (IOException e) {
                processInfo.error.add("خطأ في قراءة stderr: " + e.getMessage());
            }
        });
    }
    
    /**
     * callback للأوامر
     */
    public interface CommandCallback {
        void onComplete(CommandResult result);
    }
    
    /**
     * نتيجة الأمر
     */
    public static class CommandResult {
        public boolean success;
        public String stdout;
        public String stderr;
        public String command;
        public long executionTime;
        
        public CommandResult(boolean success, String stdout, String stderr) {
            this.success = success;
            this.stdout = stdout != null ? stdout : "";
            this.stderr = stderr != null ? stderr : "";
            this.executionTime = 0;
        }
        
        public boolean isSuccessful() {
            return success;
        }
        
        public String getOutput() {
            StringBuilder result = new StringBuilder();
            if (!stdout.isEmpty()) {
                result.append(stdout);
            }
            if (!stderr.isEmpty()) {
                result.append("ERROR:\n").append(stderr);
            }
            return result.toString();
        }
    }
    
    /**
     * أمر shell
     */
    private static class ShellCommand {
        private List<String> commandList;
        private CommandResult result;
        
        public ShellCommand(List<String> commandList) {
            this.commandList = commandList;
        }
        
        public String[] getCommandArray() {
            return commandList.toArray(new String[0]);
        }
        
        public void setResult(String stdout, String stderr, int exitCode) {
            this.result = new CommandResult(exitCode == 0, stdout, stderr);
        }
        
        public CommandResult getResult() {
            if (result == null) {
                return new CommandResult(false, "", "لم يتم ضبط النتيجة");
            }
            return result;
        }
    }
    
    /**
     * معلومات العملية
     */
    public static class ProcessInfo {
        public int id;
        public String command;
        public Process process;
        public long startTime;
        public List<String> output;
        public List<String> error;
        
        public boolean isRunning() {
            return process != null && process.isAlive();
        }
        
        public long getRuntime() {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * تنظيف الموارد
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            // إنهاء جميع العمليات
            for (ProcessInfo process : activeProcesses.values()) {
                if (process.isRunning()) {
                    process.process.destroy();
                }
            }
            activeProcesses.clear();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}