package com.pythonide.files;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FileContentEditor - محرر محتوى الملفات
 * يدعم تحرير النصوص وعرض معلومات الملف
 */
public class FileContentEditor extends AppCompatActivity {
    
    private static final int SAVE_DELAY = 2000; // 2 seconds
    
    private EditText contentEditText;
    private TextView fileNameTextView;
    private TextView fileSizeTextView;
    private TextView lastModifiedTextView;
    private TextView statusTextView;
    private Toolbar toolbar;
    
    private File currentFile;
    private String originalContent = "";
    private boolean hasUnsavedChanges = false;
    private boolean isAutoSave = true;
    private Runnable saveTask;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_content_editor);
        
        initViews();
        setupToolbar();
        loadFile();
        setupTextWatcher();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        contentEditText = findViewById(R.id.contentEditText);
        fileNameTextView = findViewById(R.id.fileNameTextView);
        fileSizeTextView = findViewById(R.id.fileSizeTextView);
        lastModifiedTextView = findViewById(R.id.lastModifiedTextView);
        statusTextView = findViewById(R.id.statusTextView);
        
        // Set up content EditText
        contentEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard();
            }
        });
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void loadFile() {
        Intent intent = getIntent();
        String filePath = intent.getStringExtra("file_path");
        
        if (filePath != null) {
            currentFile = new File(filePath);
            readFileContent();
            updateFileInfo();
        } else {
            showError("لم يتم تحديد ملف");
            finish();
        }
    }
    
    private void readFileContent() {
        new Thread(() -> {
            try {
                StringBuilder content = new StringBuilder();
                
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(currentFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                // Remove last newline if exists
                if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
                    content.deleteCharAt(content.length() - 1);
                }
                
                originalContent = content.toString();
                
                runOnUiThread(() -> {
                    contentEditText.setText(originalContent);
                    updateStatus("تم تحميل الملف");
                });
                
            } catch (IOException e) {
                runOnUiThread(() -> showError("خطأ في قراءة الملف: " + e.getMessage()));
            }
        }).start();
    }
    
    private void setupTextWatcher() {
        contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges = !s.toString().equals(originalContent);
                updateStatus();
                scheduleAutoSave();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void scheduleAutoSave() {
        if (saveTask != null) {
            contentEditText.removeCallbacks(saveTask);
        }
        
        if (isAutoSave && hasUnsavedChanges) {
            saveTask = () -> {
                if (hasUnsavedChanges) {
                    saveFile();
                }
            };
            contentEditText.postDelayed(saveTask, SAVE_DELAY);
        }
    }
    
    private void saveFile() {
        if (currentFile == null) {
            showError("لا يوجد ملف للحفظ");
            return;
        }
        
        new Thread(() -> {
            try {
                String newContent = contentEditText.getText().toString();
                
                try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8))) {
                    writer.write(newContent);
                }
                
                originalContent = newContent;
                hasUnsavedChanges = false;
                
                runOnUiThread(() -> {
                    updateFileInfo();
                    updateStatus("تم حفظ الملف");
                });
                
            } catch (IOException e) {
                runOnUiThread(() -> showError("خطأ في حفظ الملف: " + e.getMessage()));
            }
        }).start();
    }
    
    private void updateFileInfo() {
        if (currentFile != null) {
            fileNameTextView.setText(currentFile.getName());
            
            // Update file size
            long fileSize = currentFile.length();
            fileSizeTextView.setText(formatFileSize(fileSize));
            
            // Update last modified
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            lastModifiedTextView.setText(sdf.format(new Date(currentFile.lastModified())));
        }
    }
    
    private void updateStatus() {
        if (hasUnsavedChanges) {
            statusTextView.setText("تغييرات غير محفوظة");
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            statusTextView.setText("تم الحفظ");
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
    
    private void updateStatus(String message) {
        statusTextView.setText(message);
        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " بايت";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f كيلوبايت", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f ميغابايت", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f غيغابايت", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void hideKeyboard() {
        if (getCurrentFocus() != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_editor_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
                
            case R.id.action_save:
                saveFile();
                return true;
                
            case R.id.action_find_replace:
                showFindReplaceDialog();
                return true;
                
            case R.id.action_auto_save:
                isAutoSave = !isAutoSave;
                item.setChecked(isAutoSave);
                showToast(isAutoSave ? "تم تفعيل الحفظ التلقائي" : "تم إلغاء الحفظ التلقائي");
                return true;
                
            case R.id.action_share:
                shareFile();
                return true;
                
            case R.id.action_properties:
                showFileProperties();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void showFindReplaceDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("البحث والاستبدال");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_find_replace, null);
        builder.setView(view);
        
        EditText findEditText = view.findViewById(R.id.findEditText);
        EditText replaceEditText = view.findViewById(R.id.replaceEditText);
        Button findButton = view.findViewById(R.id.findButton);
        Button replaceButton = view.findViewById(R.id.replaceButton);
        Button replaceAllButton = view.findViewById(R.id.replaceAllButton);
        
        findButton.setOnClickListener(v -> {
            String searchText = findEditText.getText().toString();
            if (!searchText.isEmpty()) {
                findInText(searchText);
            }
        });
        
        replaceButton.setOnClickListener(v -> {
            String searchText = findEditText.getText().toString();
            String replaceText = replaceEditText.getText().toString();
            if (!searchText.isEmpty()) {
                replaceOne(searchText, replaceText);
            }
        });
        
        replaceAllButton.setOnClickListener(v -> {
            String searchText = findEditText.getText().toString();
            String replaceText = replaceEditText.getText().toString();
            if (!searchText.isEmpty()) {
                replaceAll(searchText, replaceText);
            }
        });
        
        builder.setPositiveButton("إغلاق", null);
        builder.show();
    }
    
    private void findInText(String searchText) {
        String content = contentEditText.getText().toString();
        int index = content.indexOf(searchText);
        
        if (index >= 0) {
            contentEditText.setSelection(index, index + searchText.length());
            contentEditText.requestFocus();
        } else {
            showToast("النص غير موجود");
        }
    }
    
    private void replaceOne(String searchText, String replaceText) {
        String content = contentEditText.getText().toString();
        int index = content.indexOf(searchText);
        
        if (index >= 0) {
            String newContent = content.substring(0, index) + 
                              replaceText + 
                              content.substring(index + searchText.length());
            contentEditText.setText(newContent);
            contentEditText.setSelection(index, index + replaceText.length());
        } else {
            showToast("النص غير موجود");
        }
    }
    
    private void replaceAll(String searchText, String replaceText) {
        String content = contentEditText.getText().toString();
        String newContent = content.replace(searchText, replaceText);
        
        if (!newContent.equals(content)) {
            contentEditText.setText(newContent);
            showToast("تم استبدال جميع occurrences");
        } else {
            showToast("النص غير موجود");
        }
    }
    
    private void shareFile() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(currentFile));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentFile.getName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "مشاركة الملف"));
        } catch (Exception e) {
            showError("خطأ في مشاركة الملف");
        }
    }
    
    private void showFileProperties() {
        if (currentFile == null) return;
        
        StringBuilder properties = new StringBuilder();
        properties.append("الاسم: ").append(currentFile.getName()).append("\n");
        properties.append("الحجم: ").append(formatFileSize(currentFile.length())).append("\n");
        properties.append("آخر تعديل: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(new Date(currentFile.lastModified()))).append("\n");
        properties.append("المسار: ").append(currentFile.getAbsolutePath()).append("\n");
        properties.append("قراءة فقط: ").append(currentFile.canRead() ? "نعم" : "لا").append("\n");
        properties.append("قابل للكتابة: ").append(currentFile.canWrite() ? "نعم" : "لا").append("\n");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("خصائص الملف")
              .setMessage(properties.toString())
              .setPositiveButton("موافق", null)
              .show();
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("حفظ التغييرات")
                  .setMessage("لديك تغييرات غير محفوظة. هل تريد الحفظ قبل الخروج؟")
                  .setPositiveButton("حفظ", (dialog, which) -> {
                      saveFile();
                      finish();
                  })
                  .setNegativeButton("عدم الحفظ", (dialog, which) -> finish())
                  .setNeutralButton("إلغاء", null)
                  .show();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (hasUnsavedChanges) {
            updateStatus();
        }
    }
}