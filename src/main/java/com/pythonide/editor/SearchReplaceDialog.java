package com.pythonide.editor;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

/**
 * حوار البحث والاستبدال (Search & Replace Dialog)
 * واجهة مستخدم متقدمة للبحث والاستبدال
 */
public class SearchReplaceDialog extends Dialog {
    
    private Context context;
    private SearchReplaceManager searchManager;
    
    // UI Components
    private EditText searchEditText;
    private EditText replaceEditText;
    private Button findNextButton;
    private Button findPreviousButton;
    private Button replaceButton;
    private Button replaceAllButton;
    private Button closeButton;
    
    private CheckBox caseSensitiveCheckBox;
    private CheckBox useRegexCheckBox;
    private CheckBox wholeWordsCheckBox;
    
    private TextView resultCountTextView;
    private LinearLayout replaceLayout;
    
    private boolean isReplaceMode = false;
    
    public SearchReplaceDialog(@NonNull Context context, SearchReplaceManager searchManager) {
        super(context);
        this.context = context;
        this.searchManager = searchManager;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_search_replace);
        
        initializeViews();
        setupToolbar();
        setupListeners();
        setupUI();
    }
    
    private void initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        replaceEditText = findViewById(R.id.replace_edit_text);
        findNextButton = findViewById(R.id.find_next_button);
        findPreviousButton = findViewById(R.id.find_previous_button);
        replaceButton = findViewById(R.id.replace_button);
        replaceAllButton = findViewById(R.id.replace_all_button);
        closeButton = findViewById(R.id.close_button);
        
        caseSensitiveCheckBox = findViewById(R.id.case_sensitive_checkbox);
        useRegexCheckBox = findViewById(R.id.use_regex_checkbox);
        wholeWordsCheckBox = findViewById(R.id.whole_words_checkbox);
        
        resultCountTextView = findViewById(R.id.result_count_text);
        replaceLayout = findViewById(R.id.replace_layout);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("البحث والاستبدال");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
        toolbar.setNavigationOnClickListener(v -> dismiss());
    }
    
    private void setupListeners() {
        // Search text listeners
        searchEditText.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    performSearch();
                } else {
                    clearSearch();
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        // Button listeners
        findNextButton.setOnClickListener(v -> {
            if (searchManager.goToNext()) {
                updateResultCount();
            } else {
                showNoResultsMessage();
            }
        });
        
        findPreviousButton.setOnClickListener(v -> {
            if (searchManager.goToPrevious()) {
                updateResultCount();
            } else {
                showNoResultsMessage();
            }
        });
        
        replaceButton.setOnClickListener(v -> {
            if (isReplaceMode) {
                if (searchManager.replaceCurrent(replaceEditText.getText().toString())) {
                    // Move to next result after replacement
                    if (searchManager.goToNext()) {
                        updateResultCount();
                    } else {
                        updateResultCount();
                    }
                } else {
                    Toast.makeText(context, "فشل في الاستبدال", Toast.LENGTH_SHORT).show();
                }
            } else {
                toggleReplaceMode();
            }
        });
        
        replaceAllButton.setOnClickListener(v -> {
            if (isReplaceMode) {
                String query = searchEditText.getText().toString();
                String replacement = replaceEditText.getText().toString();
                
                if (query.isEmpty()) {
                    Toast.makeText(context, "يرجى إدخال نص للبحث", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int count = searchManager.replaceAll(
                    query, 
                    replacement, 
                    caseSensitiveCheckBox.isChecked(), 
                    useRegexCheckBox.isChecked()
                );
                
                if (count > 0) {
                    Toast.makeText(context, "تم استبدال " + count + " من النتائج", Toast.LENGTH_LONG).show();
                    updateResultCount();
                } else {
                    Toast.makeText(context, "لم يتم العثور على نتائج للاستبدال", Toast.LENGTH_SHORT).show();
                }
            } else {
                toggleReplaceMode();
            }
        });
        
        closeButton.setOnClickListener(v -> dismiss());
        
        // Check box listeners
        caseSensitiveCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> performSearch());
        useRegexCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> performSearch());
        wholeWordsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> performSearch());
    }
    
    private void setupUI() {
        // Set up window size
        getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        
        // Initially hide replace layout
        replaceLayout.setVisibility(View.GONE);
        
        // Set default values
        caseSensitiveCheckBox.setChecked(false);
        useRegexCheckBox.setChecked(false);
        wholeWordsCheckBox.setChecked(false);
        
        // Focus on search edit text
        searchEditText.requestFocus();
    }
    
    private void toggleReplaceMode() {
        isReplaceMode = !isReplaceMode;
        
        if (isReplaceMode) {
            replaceLayout.setVisibility(View.VISIBLE);
            replaceButton.setText("استبدال");
            replaceAllButton.setText("استبدال الكل");
        } else {
            replaceLayout.setVisibility(View.GONE);
            replaceButton.setText("الاستبدال");
            replaceAllButton.setText("الاستبدال");
        }
        
        // Update button states
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        String query = searchEditText.getText().toString();
        boolean hasQuery = !query.isEmpty();
        
        findNextButton.setEnabled(hasQuery);
        findPreviousButton.setEnabled(hasQuery);
        replaceButton.setEnabled(hasQuery);
        replaceAllButton.setEnabled(hasQuery);
    }
    
    private void performSearch() {
        String query = searchEditText.getText().toString();
        
        if (query.isEmpty()) {
            clearSearch();
            return;
        }
        
        // Perform search
        SearchReplaceManager.SearchResult[] results = searchManager.search(
            query,
            caseSensitiveCheckBox.isChecked(),
            useRegexCheckBox.isChecked(),
            wholeWordsCheckBox.isChecked()
        ).toArray(new SearchReplaceManager.SearchResult[0]);
        
        updateResultCount();
        updateButtonStates();
        
        if (results.length == 0) {
            showNoResultsMessage();
        } else {
            // Focus back on editor
            searchEditText.clearFocus();
        }
    }
    
    private void updateResultCount() {
        int count = searchManager.getResultCount();
        int currentIndex = searchManager.getCurrentResultIndex();
        
        if (count > 0) {
            resultCountTextView.setText("النتائج: " + (currentIndex + 1) + " من " + count);
            resultCountTextView.setVisibility(View.VISIBLE);
        } else {
            resultCountTextView.setVisibility(View.GONE);
        }
    }
    
    private void clearSearch() {
        searchManager.clearSearch();
        resultCountTextView.setVisibility(View.GONE);
        updateButtonStates();
    }
    
    private void showNoResultsMessage() {
        Toast.makeText(context, "لم يتم العثور على النتائج", Toast.LENGTH_SHORT).show();
    }
    
    // Public methods for external control
    public void setSearchText(String text) {
        if (searchEditText != null) {
            searchEditText.setText(text);
            performSearch();
        }
    }
    
    public void setReplaceText(String text) {
        if (replaceEditText != null) {
            replaceEditText.setText(text);
        }
    }
    
    public void showReplaceMode() {
        if (!isReplaceMode) {
            toggleReplaceMode();
        }
    }
    
    public void hideReplaceMode() {
        if (isReplaceMode) {
            toggleReplaceMode();
        }
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        // Clear search highlights when dialog is closed
        if (searchManager != null) {
            searchManager.clearSearch();
        }
    }
}