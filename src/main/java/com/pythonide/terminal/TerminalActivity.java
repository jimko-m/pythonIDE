package com.pythonide.terminal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerminalActivity extends Activity {
    
    private TextView outputTextView;
    private EditText inputEditText;
    private Button executeButton;
    private ScrollView scrollView;
    private LinearLayout inputLayout;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    private Process pythonProcess;
    
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private PipedOutputStream commandPipe;
    private PipedInputStream pythonInput;
    
    private static final String PROMPT = "$ python-ide> ";
    private static final String PS1 = ">>> ";
    private static final String PS2 = "... ";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        
        initializeViews();
        setupTerminal();
        setupInputHandlers();
        startPythonInterpreter();
    }
    
    private void initializeViews() {
        outputTextView = findViewById(R.id.output_text);
        inputEditText = findViewById(R.id.input_edit);
        executeButton = findViewById(R.id.execute_button);
        scrollView = findViewById(R.id.scroll_view);
        inputLayout = findViewById(R.id.input_layout);
        
        outputTextView.setMovementMethod(new ScrollingMovementMethod());
        outputTextView.setTextSize(14);
        outputTextView.setTypeface(null, android.graphics.Typeface.MONOSPACE);
        inputEditText.setTypeface(null, android.graphics.Typeface.MONOSPACE);
        inputEditText.setTextSize(14);
        inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }
    
    private void setupTerminal() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // إنشاء مجلد العمل
        File workDir = new File(getFilesDir(), "work");
        if (!workDir.exists()) {
            workDir.mkdirs();
        }
        
        appendOutput("=== Python IDE Terminal ===\n");
        appendOutput("Python IDE Terminal Started\n");
        appendOutput("مجلد العمل: " + workDir.getAbsolutePath() + "\n");
        appendOutput("استخدم الأمر 'exit' أو 'quit' للخروج\n");
        appendOutput("استخدم 'clear' لمسح الشاشة\n\n");
        appendOutput(PROMPT);
        
        inputEditText.requestFocus();
    }
    
    private void setupInputHandlers() {
        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand();
            }
        });
        
        inputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    executeCommand();
                    return true;
                }
                return false;
            }
        });
        
        // معالجة أزرار الأسهم للتنقل في التاريخ
        inputEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_UP:
                            navigateHistory(-1);
                            return true;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            navigateHistory(1);
                            return true;
                    }
                }
                return false;
            }
        });
    }
    
    private void startPythonInterpreter() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // إنشاء pipes للتواصل مع Python
                    commandPipe = new PipedOutputStream();
                    pythonInput = new PipedInputStream(commandPipe);
                    
                    // بدء Python process
                    List<String> command = new ArrayList<>();
                    command.add("/system/bin/python3");
                    command.add("-u");  // Unbuffered output
                    
                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.directory(getFilesDir());
                    
                    pythonProcess = pb.start();
                    
                    // قراءة output من Python في thread منفصل
                    startOutputReader();
                    
                } catch (IOException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("\nخطأ في بدء Python: " + e.getMessage() + "\n");
                            appendOutput(PROMPT);
                        }
                    });
                }
            }
        });
    }
    
    private void startOutputReader() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
                StringBuilder output = new StringBuilder();
                
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String finalLine = line;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                appendOutput(finalLine + "\n");
                                scrollToBottom();
                            }
                        });
                    }
                } catch (IOException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("تم إنهاء Python session\n");
                            appendOutput(PROMPT);
                        }
                    });
                }
            }
        });
    }
    
    private void executeCommand() {
        String command = inputEditText.getText().toString().trim();
        if (command.isEmpty()) {
            appendOutput(PROMPT);
            return;
        }
        
        // إضافة للأمر للتاريخ
        if (!commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(command)) {
            commandHistory.add(command);
        }
        historyIndex = -1;
        
        // عرض الأمر في output
        appendOutput(command + "\n");
        
        // معالجة الأوامر الخاصة
        if (command.equals("clear")) {
            outputTextView.setText("");
            inputEditText.setText("");
            return;
        }
        
        if (command.equals("exit") || command.equals("quit")) {
            appendOutput("تم إنهاء Python IDE Terminal\n");
            finish();
            return;
        }
        
        if (command.equals("history")) {
            StringBuilder history = new StringBuilder("تاريخ الأوامر:\n");
            for (int i = 0; i < commandHistory.size(); i++) {
                history.append(i + 1).append(" ").append(commandHistory.get(i)).append("\n");
            }
            appendOutput(history.toString());
            appendOutput(PROMPT);
            inputEditText.setText("");
            return;
        }
        
        // إرسال الأمر إلى Python
        if (pythonProcess != null && pythonProcess.isAlive()) {
            try {
                commandPipe.write(command.getBytes());
                commandPipe.write('\n');
                commandPipe.flush();
            } catch (IOException e) {
                appendOutput("خطأ في إرسال الأمر: " + e.getMessage() + "\n");
                appendOutput(PROMPT);
            }
        } else {
            appendOutput("خطأ: Python process غير متاح\n");
            appendOutput(PROMPT);
        }
        
        inputEditText.setText("");
    }
    
    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;
        
        historyIndex += direction;
        
        if (historyIndex < 0) {
            historyIndex = 0;
            inputEditText.setText("");
            return;
        } else if (historyIndex >= commandHistory.size()) {
            historyIndex = commandHistory.size() - 1;
            inputEditText.setText(commandHistory.get(historyIndex));
            return;
        }
        
        inputEditText.setText(commandHistory.get(historyIndex));
        inputEditText.setSelection(inputEditText.length());
    }
    
    private void appendOutput(String text) {
        outputTextView.append(text);
        scrollToBottom();
    }
    
    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (pythonProcess != null) {
            pythonProcess.destroy();
        }
        
        try {
            if (commandPipe != null) {
                commandPipe.close();
            }
            if (pythonInput != null) {
                pythonInput.close();
            }
        } catch (IOException e) {
            // تجاهل error أثناء cleanup
        }
    }
    
    @Override
    public void onBackPressed() {
        // منع الخروج بالزر الخلفي إلا إذا كان هناك محتوى في input
        if (!inputEditText.getText().toString().isEmpty()) {
            inputEditText.setText("");
        } else {
            super.onBackPressed();
        }
    }
}