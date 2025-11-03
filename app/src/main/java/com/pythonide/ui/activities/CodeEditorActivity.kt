package com.pythonide.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pythonide.R
import com.pythonide.databinding.ActivityCodeEditorBinding
import com.pythonide.utils.EditorManager
import com.pythonide.utils.ThemeManager
import java.io.File

/**
 * Code editor activity with Monaco Editor integration
 */
class CodeEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCodeEditorBinding
    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var editorControls: LinearLayout
    private lateinit var findReplacePanel: LinearLayout
    private lateinit var fileInfo: TextView
    private lateinit var cursorPosition: TextView
    private lateinit var findText: EditText
    private lateinit var replaceText: EditText
    private lateinit var statusText: TextView
    private lateinit var fileModified: TextView
    private lateinit var loadingProgress: ProgressBar
    
    private var currentFile: File? = null
    private var isModified = false
    private lateinit var editorManager: EditorManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ThemeManager.applyTheme(this)
        
        binding = ActivityCodeEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeViews()
        setupToolbar()
        setupEditor()
        setupKeyboardListener()
        
        handleIntent(intent)
    }
    
    private fun initializeViews() {
        toolbar = binding.toolbar
        webView = binding.monacoEditor
        editorControls = binding.editorControls
        findReplacePanel = binding.findReplacePanel
        fileInfo = binding.fileInfo
        cursorPosition = binding.cursorPosition
        findText = binding.findText
        replaceText = binding.replaceText
        statusText = binding.statusText
        fileModified = binding.fileModified
        loadingProgress = binding.loadingProgress
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.editor_title)
    }
    
    private fun setupEditor() {
        editorManager = EditorManager(this, webView)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                initializeMonacoEditor()
            }
        }
        
        // Load Monaco Editor
        webView.loadUrl("file:///android_asset/monaco-editor/index.html")
    }
    
    private fun initializeMonacoEditor() {
        val theme = if (ThemeManager.isDarkMode(this)) "vs-dark" else "vs-light"
        
        val initializationScript = """
            window.monacoEditor = monaco.editor.create(document.getElementById('container'), {
                value: '',
                language: 'python',
                theme: '$theme',
                automaticLayout: true,
                fontSize: 14,
                fontFamily: 'Monaco, Consolas, "Ubuntu Mono", monospace',
                lineNumbers: 'on',
                minimap: { enabled: true },
                scrollBeyondLastLine: false,
                wordWrap: 'on',
                tabSize: 4,
                insertSpaces: true,
                folding: true,
                foldingHighlight: true,
                showFoldingControls: 'always',
                bracketMatching: 'always',
                autoClosingBrackets: 'always',
                autoClosingQuotes: 'always',
                suggest: {
                    showKeywords: true,
                    showSnippets: true,
                    showFunctions: true,
                    showConstructors: true,
                    showFields: true,
                    showVariables: true,
                    showClasses: true,
                    showStructs: true,
                    showInterfaces: true,
                    showModules: true,
                    showProperties: true,
                    showEvents: true,
                    showOperators: true,
                    showUnits: true,
                    showValues: true,
                    showConstants: true,
                    showEnums: true,
                    showEnumMembers: true,
                    showTypeParameters: true,
                    showIssues: true,
                    showUsers: true,
                    showColors: true,
                    showFiles: true,
                    showReferences: true,
                    showFolders: true,
                    showWords: true,
                    showColors: true,
                    showFiles: true,
                    showReferences: true,
                    showFolders: true,
                    showTypeParameters: true
                },
                quickSuggestions: true,
                parameterHints: {
                    enabled: true
                },
                hover: {
                    enabled: true
                }
            });
            
            // Editor event listeners
            window.monacoEditor.onDidChangeCursorPosition((e) => {
                window.Android.updateCursorPosition(e.position.lineNumber, e.position.column);
            });
            
            window.monacoEditor.onDidChangeModelContent(() => {
                window.Android.onContentChanged();
            });
            
            window.monacoEditor.onDidChangeModelLanguage((e) => {
                window.Android.onLanguageChanged(e.languageId);
            });
        """.trimIndent()
        
        webView.evaluateJavascript(initializationScript) { }
    }
    
    private fun setupKeyboardListener() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            
            // Show/hide find/replace panel based on keyboard visibility
            if (findReplacePanel.visibility == android.view.View.VISIBLE && imeVisible) {
                hideFindReplacePanel()
            }
            
            insets
        }
    }
    
    private fun handleIntent(intent: android.content.Intent) {
        when (intent.getStringExtra("action")) {
            "new_file" -> {
                val fileType = intent.getStringExtra("file_type") ?: "py"
                createNewFile(fileType)
            }
            "open_file" -> {
                val fileUri = intent.getStringExtra("file_uri")
                if (fileUri != null) {
                    openFile(fileUri)
                }
            }
        }
    }
    
    private fun createNewFile(fileType: String = "py") {
        currentFile = File("/tmp/new_file.$fileType")
        currentFile?.createNewFile()
        
        val initialContent = getInitialContentForFileType(fileType)
        editorManager.setContent(initialContent)
        editorManager.setLanguage(fileType)
        
        updateFileInfo()
        updateTitle()
    }
    
    private fun openFile(fileUri: String) {
        try {
            val uri = android.net.Uri.parse(fileUri)
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.readText() ?: ""
            inputStream?.close()
            
            val fileName = getFileNameFromUri(uri)
            currentFile = File("/tmp/$fileName")
            currentFile?.writeText(content)
            
            editorManager.setContent(content)
            editorManager.setLanguage(getLanguageFromFileName(fileName))
            
            updateFileInfo()
            updateTitle()
            isModified = false
            updateModifiedStatus()
            
        } catch (e: Exception) {
            showError(getString(R.string.error_file_not_found))
        }
    }
    
    private fun saveFile() {
        val content = editorManager.getContent()
        if (currentFile != null) {
            try {
                currentFile?.writeText(content)
                isModified = false
                updateModifiedStatus()
                showSuccess(getString(R.string.msg_file_saved))
            } catch (e: Exception) {
                showError(getString(R.string.msg_file_not_saved))
            }
        } else {
            // Save as new file
            saveFileAs()
        }
    }
    
    private fun saveFileAs() {
        // Implement file save as dialog
        // This would typically open a file picker dialog
        showMessage(getString(R.string.msg_save_as))
    }
    
    private fun showFindReplacePanel() {
        findReplacePanel.visibility = android.view.View.VISIBLE
        findText.requestFocus()
    }
    
    private fun hideFindReplacePanel() {
        findReplacePanel.visibility = android.view.View.GONE
    }
    
    private fun findText() {
        val searchText = findText.text.toString()
        if (searchText.isNotEmpty()) {
            editorManager.find(searchText)
        }
    }
    
    private fun replaceText() {
        val searchText = findText.text.toString()
        val replaceWith = replaceText.text.toString()
        if (searchText.isNotEmpty()) {
            editorManager.replace(searchText, replaceWith)
        }
    }
    
    private fun replaceAllText() {
        val searchText = findText.text.toString()
        val replaceWith = replaceText.text.toString()
        if (searchText.isNotEmpty()) {
            editorManager.replaceAll(searchText, replaceWith)
        }
    }
    
    private fun updateFileInfo() {
        currentFile?.let { file ->
            val fileName = file.name
            val fileSize = file.length()
            val sizeInKB = if (fileSize < 1024) "$fileSize B" else "${fileSize / 1024} KB"
            
            fileInfo.text = "$fileName • $sizeInKB"
        }
    }
    
    private fun updateTitle() {
        val title = currentFile?.name ?: "untitled"
        supportActionBar?.title = if (isModified) "$title *" else title
    }
    
    private fun updateModifiedStatus() {
        fileModified.text = if (isModified) "•" else ""
        updateTitle()
    }
    
    private fun updateCursorPosition(line: Int, column: Int) {
        cursorPosition.text = "Ln $line, Col $column"
    }
    
    private fun showMessage(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(android.R.color.holo_blue_dark))
    }
    
    private fun showSuccess(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(R.color.success))
    }
    
    private fun showError(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(R.color.error))
    }
    
    private fun getFileNameFromUri(uri: android.net.Uri): String {
        // Extract filename from URI
        val cursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor?.moveToFirst()
        val name = nameIndex?.let { cursor.getString(it) } ?: "unknown"
        cursor?.close()
        return name
    }
    
    private fun getLanguageFromFileName(fileName: String): String {
        return when {
            fileName.endsWith(".py") -> "python"
            fileName.endsWith(".java") -> "java"
            fileName.endsWith(".kt") -> "kotlin"
            fileName.endsWith(".js") -> "javascript"
            fileName.endsWith(".ts") -> "typescript"
            fileName.endsWith(".html") -> "html"
            fileName.endsWith(".css") -> "css"
            fileName.endsWith(".json") -> "json"
            fileName.endsWith(".xml") -> "xml"
            else -> "plaintext"
        }
    }
    
    private fun getInitialContentForFileType(fileType: String): String {
        return when (fileType) {
            "py" -> """#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def main():
    print("Hello, World!")

if __name__ == "__main__":
    main()
"""
            "java" -> """public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
"""
            "kt" -> """fun main() {
    println("Hello, World!")
}
"""
            "js" -> """function main() {
    console.log("Hello, World!");
}

main();
"""
            else -> ""
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveFile()
                true
            }
            R.id.action_save_as -> {
                saveFileAs()
                true
            }
            R.id.action_find_replace -> {
                if (findReplacePanel.visibility == android.view.View.VISIBLE) {
                    hideFindReplacePanel()
                } else {
                    showFindReplacePanel()
                }
                true
            }
            R.id.action_format -> {
                editorManager.formatCode()
                true
            }
            R.id.action_run -> {
                runCode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun runCode() {
        // Implement code execution
        showMessage(getString(R.string.status_running))
    }
    
    override fun onBackPressed() {
        if (isModified) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_save_file))
                .setMessage(getString(R.string.msg_unsaved_changes))
                .setPositiveButton(getString(R.string.msg_save)) { _, _ -> saveFile() }
                .setNegativeButton(getString(R.string.msg_discard)) { _, _ -> super.onBackPressed() }
                .setNeutralButton(getString(R.string.msg_cancel), null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
    
    companion object {
        // This interface is called from JavaScript
        @Suppress("unused")
        class EditorJavaScriptInterface(private val activity: CodeEditorActivity) {
            fun updateCursorPosition(line: Int, column: Int) {
                activity.runOnUiThread {
                    activity.updateCursorPosition(line, column)
                }
            }
            
            fun onContentChanged() {
                activity.runOnUiThread {
                    activity.isModified = true
                    activity.updateModifiedStatus()
                }
            }
            
            fun onLanguageChanged(language: String) {
                activity.runOnUiThread {
                    activity.statusText.text = "Language: $language"
                }
            }
        }
    }
}