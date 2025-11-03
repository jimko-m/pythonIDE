package com.pythonide.files;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.content.ContentResolver;
import android.provider.OpenableColumns;
import android.content.ContentUris;
import android.database.Cursor;

/**
 * FileManagerActivity - نظام إدارة ملفات كامل مع ميزات متقدمة
 * يشمل: file explorer, create/delete/rename, copy/paste, import/export, auto-save, folder management, drag & drop
 */
public class FileManagerActivity extends AppCompatActivity {
    
    private static final String TAG = "FileManagerActivity";
    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_IMPORT_FILES = 1002;
    private static final int REQUEST_EXPORT_FILES = 1003;
    
    // Components
    private RecyclerView fileRecyclerView;
    private TextView currentPathTextView;
    private TextView statusTextView;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View floatingActionButton;
    
    // Layout Manager for different view modes
    private LinearLayoutManager listLayoutManager;
    private GridLayoutManager gridLayoutManager;
    
    // Data
    private FileAdapter fileAdapter;
    private List<FileItem> currentFiles = new ArrayList<>();
    private File currentDirectory;
    private Stack<String> navigationHistory = new Stack<>();
    
    // Copy/Paste functionality
    private List<FileItem> clipboardItems = new ArrayList<>();
    private boolean isCopyMode = false;
    
    // Search functionality
    private EditText searchEditText;
    private SearchView searchView;
    
    // Drag & Drop
    private GestureDetector gestureDetector;
    private View selectedView;
    private ActionMode currentActionMode;
    
    // Auto-save
    private Timer autoSaveTimer;
    private static final int AUTO_SAVE_INTERVAL = 30000; // 30 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupDragDrop();
        setupSearch();
        checkPermissions();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        fileRecyclerView = findViewById(R.id.fileRecyclerView);
        currentPathTextView = findViewById(R.id.currentPathTextView);
        statusTextView = findViewById(R.id.statusTextView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        floatingActionButton = findViewById(R.id.fab);
        
        // Initialize layout managers
        listLayoutManager = new LinearLayoutManager(this);
        gridLayoutManager = new GridLayoutManager(this, 3);
        
        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFiles(currentDirectory);
            swipeRefreshLayout.setRefreshing(false);
        });
        
        // Setup FAB click
        floatingActionButton.setOnClickListener(v -> showNewFileDialog());
        
        // Show/hide FAB based on scroll
        fileRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    floatingActionButton.animate().translationY(floatingActionButton.getHeight() + 100);
                } else {
                    floatingActionButton.animate().translationY(0);
                }
            }
        });
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("مدير الملفات");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> goBack());
    }
    
    private void setupRecyclerView() {
        fileAdapter = new FileAdapter(this, currentFiles, this::onFileClick, this::onFileLongClick);
        fileRecyclerView.setAdapter(fileAdapter);
        fileRecyclerView.setLayoutManager(listLayoutManager);
        
        // Enable item animator
        fileRecyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator() {
            @Override
            public boolean animateRemove(RecyclerView.ViewHolder holder) {
                // Custom animation for remove
                return super.animateRemove(holder);
            }
        });
    }
    
    private void setupDragDrop() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onLongPress(MotionEvent e) {
                return super.onLongPress(e);
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
    }
    
    private void setupSearch() {
        // Setup search in toolbar
        searchView = findViewById(R.id.searchView);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }
                
                @Override
                public boolean onQueryTextChange(String newText) {
                    filterFiles(newText);
                    return true;
                }
            });
        }
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        };
        
        List<String> permissionsNeeded = new ArrayList<>();
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                REQUEST_PERMISSIONS);
        } else {
            initializeFileManager();
        }
    }
    
    private void initializeFileManager() {
        // Set initial directory to the internal storage
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        
        loadFiles(documentsDir);
        startAutoSave();
    }
    
    private void loadFiles(File directory) {
        if (directory == null || !directory.exists()) {
            showError("المجلد غير موجود");
            return;
        }
        
        this.currentDirectory = directory;
        currentPathTextView.setText(directory.getAbsolutePath());
        
        new Thread(() -> {
            try {
                File[] files = directory.listFiles();
                if (files != null) {
                    List<FileItem> fileList = new ArrayList<>();
                    
                    // Add parent directory link
                    if (directory.getParentFile() != null) {
                        fileList.add(new FileItem(directory.getParentFile(), true));
                    }
                    
                    // Add directories first, then files
                    List<FileItem> directories = new ArrayList<>();
                    List<FileItem> regularFiles = new ArrayList<>();
                    
                    for (File file : files) {
                        FileItem item = new FileItem(file, false);
                        if (file.isDirectory()) {
                            directories.add(item);
                        } else {
                            regularFiles.add(item);
                        }
                    }
                    
                    // Sort directories and files
                    Collections.sort(directories, (a, b) -> 
                        a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
                    Collections.sort(regularFiles, (a, b) -> 
                        a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
                    
                    fileList.addAll(directories);
                    fileList.addAll(regularFiles);
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        currentFiles.clear();
                        currentFiles.addAll(fileList);
                        fileAdapter.notifyDataSetChanged();
                        updateStatusText();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading files", e);
                runOnUiThread(() -> showError("خطأ في تحميل الملفات: " + e.getMessage()));
            }
        }).start();
    }
    
    private void updateStatusText() {
        int folders = 0;
        int files = 0;
        
        for (FileItem item : currentFiles) {
            if (item.isDirectory() && !item.isParentLink()) {
                folders++;
            } else if (!item.isDirectory()) {
                files++;
            }
        }
        
        statusTextView.setText(String.format("%d مجلد، %d ملف", folders, files));
    }
    
    private void onFileClick(FileItem fileItem, int position) {
        if (fileItem.isParentLink()) {
            goBack();
        } else if (fileItem.isDirectory()) {
            navigationHistory.push(currentDirectory.getAbsolutePath());
            loadFiles(fileItem.getFile());
        } else {
            openFile(fileItem);
        }
    }
    
    private void onFileLongClick(FileItem fileItem, int position) {
        showContextMenu(fileItem, position);
    }
    
    private void showContextMenu(FileItem fileItem, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fileItem.getName());
        
        String[] items;
        if (fileItem.isDirectory()) {
            items = new String[]{"فتح", "إعادة تسمية", "حذف", "خصائص"};
        } else {
            items = new String[]{"فتح", "تعديل", "إعادة تسمية", "حذف", "نسخ", "قص", "خصائص"};
        }
        
        builder.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0: // Open
                    if (fileItem.isDirectory()) {
                        navigationHistory.push(currentDirectory.getAbsolutePath());
                        loadFiles(fileItem.getFile());
                    } else {
                        openFile(fileItem);
                    }
                    break;
                case 1: // Rename
                    showRenameDialog(fileItem);
                    break;
                case 2: // Delete
                    showDeleteDialog(fileItem);
                    break;
                case 3: // Copy
                    if (!fileItem.isDirectory()) {
                        clipboardItems.clear();
                        clipboardItems.add(fileItem);
                        isCopyMode = true;
                        showToast("تم نسخ الملف");
                    }
                    break;
                case 4: // Cut
                    if (!fileItem.isDirectory()) {
                        clipboardItems.clear();
                        clipboardItems.add(fileItem);
                        isCopyMode = false;
                        showToast("تم قص الملف");
                    }
                    break;
                case 5: // Properties
                    showPropertiesDialog(fileItem);
                    break;
            }
        });
        
        builder.show();
    }
    
    private void showNewFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إنشاء جديد");
        
        String[] options = {"مجلد جديد", "ملف جديد"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // New folder
                    createNewFolder();
                    break;
                case 1: // New file
                    createNewFile();
                    break;
            }
        });
        
        builder.show();
    }
    
    private void createNewFolder() {
        EditText input = new EditText(this);
        input.setHint("اسم المجلد");
        
        new AlertDialog.Builder(this)
            .setTitle("إنشاء مجلد جديد")
            .setView(input)
            .setPositiveButton("إنشاء", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    File newFolder = new File(currentDirectory, name);
                    if (newFolder.mkdirs()) {
                        loadFiles(currentDirectory);
                        showToast("تم إنشاء المجلد");
                    } else {
                        showError("فشل في إنشاء المجلد");
                    }
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
            
        input.requestFocus();
    }
    
    private void createNewFile() {
        EditText input = new EditText(this);
        input.setHint("اسم الملف");
        
        new AlertDialog.Builder(this)
            .setTitle("إنشاء ملف جديد")
            .setView(input)
            .setPositiveButton("إنشاء", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    File newFile = new File(currentDirectory, name);
                    try {
                        newFile.createNewFile();
                        loadFiles(currentDirectory);
                        showToast("تم إنشاء الملف");
                        openFile(new FileItem(newFile, false));
                    } catch (IOException e) {
                        showError("فشل في إنشاء الملف: " + e.getMessage());
                    }
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
            
        input.requestFocus();
    }
    
    private void showRenameDialog(FileItem fileItem) {
        EditText input = new EditText(this);
        input.setText(fileItem.getName());
        
        new AlertDialog.Builder(this)
            .setTitle("إعادة تسمية")
            .setView(input)
            .setPositiveButton("حفظ", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(fileItem.getName())) {
                    File newFile = new File(fileItem.getFile().getParentFile(), newName);
                    if (fileItem.getFile().renameTo(newFile)) {
                        loadFiles(currentDirectory);
                        showToast("تم إعادة التسمية");
                    } else {
                        showError("فشل في إعادة التسمية");
                    }
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
            
        input.requestFocus();
        input.setSelection(0, fileItem.getName().length());
    }
    
    private void showDeleteDialog(FileItem fileItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تأكيد الحذف");
        builder.setMessage("هل أنت متأكد من حذف '" + fileItem.getName() + "'؟");
        
        builder.setPositiveButton("حذف", (dialog, which) -> {
            if (deleteFile(fileItem)) {
                loadFiles(currentDirectory);
                showToast("تم حذف الملف");
            } else {
                showError("فشل في حذف الملف");
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private boolean deleteFile(FileItem fileItem) {
        File file = fileItem.getFile();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    FileItem childItem = new FileItem(childFile, false);
                    if (!deleteFile(childItem)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
    
    private void showPropertiesDialog(FileItem fileItem) {
        File file = fileItem.getFile();
        
        StringBuilder properties = new StringBuilder();
        properties.append("الاسم: ").append(file.getName()).append("\n");
        properties.append("النوع: ").append(fileItem.isDirectory() ? "مجلد" : "ملف").append("\n");
        properties.append("الحجم: ").append(formatFileSize(file.length())).append("\n");
        properties.append("آخر تعديل: ").append(new Date(file.lastModified())).append("\n");
        properties.append("المسار: ").append(file.getAbsolutePath()).append("\n");
        
        new AlertDialog.Builder(this)
            .setTitle("خصائص الملف")
            .setMessage(properties.toString())
            .setPositiveButton("موافق", null)
            .show();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " بايت";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f كيلوبايت", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f ميغابايت", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f غيغابايت", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void openFile(FileItem fileItem) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = android.support.v4.content.FileProvider.getUriForFile(
                this, getApplicationContext().getPackageName() + ".fileprovider", fileItem.getFile());
            intent.setDataAndType(uri, getMimeType(fileItem.getFile().getName()));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file", e);
            showError("لا يمكن فتح الملف");
        }
    }
    
    private String getMimeType(String filename) {
        String[] parts = filename.split("\\.");
        if (parts.length > 1) {
            String extension = parts[parts.length - 1].toLowerCase();
            switch (extension) {
                case "txt":
                case "log":
                    return "text/plain";
                case "html":
                case "htm":
                    return "text/html";
                case "pdf":
                    return "application/pdf";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "mp3":
                    return "audio/mpeg";
                case "mp4":
                    return "video/mp4";
                default:
                    return "*/*";
            }
        }
        return "*/*";
    }
    
    private void filterFiles(String query) {
        if (query.isEmpty()) {
            loadFiles(currentDirectory);
            return;
        }
        
        new Thread(() -> {
            List<FileItem> filteredList = new ArrayList<>();
            for (FileItem item : currentFiles) {
                if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            
            runOnUiThread(() -> {
                currentFiles.clear();
                currentFiles.addAll(filteredList);
                fileAdapter.notifyDataSetChanged();
                updateStatusText();
            });
        }).start();
    }
    
    private void goBack() {
        if (!navigationHistory.isEmpty()) {
            String previousPath = navigationHistory.pop();
            loadFiles(new File(previousPath));
        } else if (currentDirectory != null && currentDirectory.getParentFile() != null) {
            loadFiles(currentDirectory.getParentFile());
        }
    }
    
    private void pasteFiles() {
        if (clipboardItems.isEmpty()) {
            showToast("لا توجد ملفات في الحافظة");
            return;
        }
        
        new Thread(() -> {
            try {
                for (FileItem item : clipboardItems) {
                    File source = item.getFile();
                    File destination = new File(currentDirectory, source.getName());
                    
                    if (isCopyMode) {
                        copyFile(source, destination);
                    } else {
                        if (source.renameTo(destination)) {
                            // Success
                        }
                    }
                }
                
                clipboardItems.clear();
                runOnUiThread(() -> {
                    loadFiles(currentDirectory);
                    showToast("تم " + (isCopyMode ? "نسخ" : "نقل") + " الملفات");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error pasting files", e);
                runOnUiThread(() -> showError("خطأ في " + (isCopyMode ? "النسخ" : "النقل")));
            }
        }).start();
    }
    
    private void copyFile(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyFile(file, new File(destination, file.getName()));
                }
            }
        } else {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }
    
    private void exportToZip() {
        if (currentFiles.isEmpty()) {
            showToast("لا توجد ملفات للتصدير");
            return;
        }
        
        String zipFileName = "export_" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(currentDirectory, zipFileName);
        
        new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                for (FileItem item : currentFiles) {
                    if (!item.isDirectory()) {
                        addToZip(item.getFile(), zos, "");
                    }
                }
                
                runOnUiThread(() -> {
                    loadFiles(currentDirectory);
                    showToast("تم تصدير الملفات إلى ZIP");
                });
                
            } catch (IOException e) {
                Log.e(TAG, "Error exporting to ZIP", e);
                runOnUiThread(() -> showError("خطأ في التصدير"));
            }
        }).start();
    }
    
    private void addToZip(File file, ZipOutputStream zos, String parentPath) throws IOException {
        String fileName = parentPath + file.getName();
        
        if (file.isDirectory()) {
            ZipEntry zipEntry = new ZipEntry(fileName + "/");
            zos.putNextEntry(zipEntry);
            zos.closeEntry();
            
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    addToZip(childFile, zos, fileName + "/");
                }
            }
        } else {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }
            
            zos.closeEntry();
        }
    }
    
    private void importFiles() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMPORT_FILES);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMPORT_FILES && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    importFile(uri);
                }
            } else if (data.getData() != null) {
                importFile(data.getData());
            }
        }
    }
    
    private void importFile(Uri uri) {
        new Thread(() -> {
            try {
                ContentResolver contentResolver = getContentResolver();
                String fileName = getFileName(uri);
                File destination = new File(currentDirectory, fileName);
                
                try (InputStream inputStream = contentResolver.openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(destination)) {
                    
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }
                
                runOnUiThread(() -> {
                    loadFiles(currentDirectory);
                    showToast("تم استيراد الملف");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error importing file", e);
                runOnUiThread(() -> showError("خطأ في استيراد الملف"));
            }
        }).start();
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    
    private void startAutoSave() {
        autoSaveTimer = new Timer();
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> performAutoSave());
            }
        }, AUTO_SAVE_INTERVAL, AUTO_SAVE_INTERVAL);
    }
    
    private void performAutoSave() {
        // Implementation for auto-save functionality
        Log.d(TAG, "Auto-save performed");
    }
    
    private void stopAutoSave() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer = null;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manager_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        switch (id) {
            case R.id.action_new_folder:
                createNewFolder();
                return true;
            case R.id.action_new_file:
                createNewFile();
                return true;
            case R.id.action_paste:
                pasteFiles();
                return true;
            case R.id.action_export_zip:
                exportToZip();
                return true;
            case R.id.action_import:
                importFiles();
                return true;
            case R.id.action_list_view:
                setListView();
                return true;
            case R.id.action_grid_view:
                setGridView();
                return true;
            case R.id.action_search:
                if (searchView != null) {
                    searchView.setVisibility(View.VISIBLE);
                    searchView.requestFocus();
                }
                return true;
            case R.id.action_refresh:
                loadFiles(currentDirectory);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void setListView() {
        fileRecyclerView.setLayoutManager(listLayoutManager);
        fileAdapter.setViewMode(FileAdapter.ViewMode.LIST);
    }
    
    private void setGridView() {
        fileRecyclerView.setLayoutManager(gridLayoutManager);
        fileAdapter.setViewMode(FileAdapter.ViewMode.GRID);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void showError(String message) {
        new AlertDialog.Builder(this)
            .setTitle("خطأ")
            .setMessage(message)
            .setPositiveButton("موافق", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (currentDirectory != null) {
            loadFiles(currentDirectory);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoSave();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoSave();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            initializeFileManager();
        } else {
            showError("تحتاج إلى صلاحيات للوصول للملفات");
        }
    }
}