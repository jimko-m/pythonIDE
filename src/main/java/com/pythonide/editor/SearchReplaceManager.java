package com.pythonide.editor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * معالج البحث والاستبدال (Search & Replace Manager)
 * يوفر إمكانيات البحث والاستبدال المتقدمة مع دعم regex
 */
public class SearchReplaceManager {
    
    private Context context;
    private CodeEditText codeEditText;
    
    private String lastSearchQuery = "";
    private int lastSearchPosition = 0;
    private boolean isSearching = false;
    private boolean searchCaseSensitive = false;
    private boolean searchUseRegex = false;
    private boolean searchWholeWords = false;
    
    private List<SearchResult> searchResults = new ArrayList<>();
    private int currentResultIndex = -1;
    
    public SearchReplaceManager(Context context, CodeEditText codeEditText) {
        this.context = context;
        this.codeEditText = codeEditText;
    }
    
    /**
     * البحث عن نص في الكود
     */
    public List<SearchResult> search(String query, boolean caseSensitive, boolean useRegex, boolean wholeWords) {
        if (query == null || query.isEmpty()) {
            clearSearch();
            return new ArrayList<>();
        }
        
        this.lastSearchQuery = query;
        this.searchCaseSensitive = caseSensitive;
        this.searchUseRegex = useRegex;
        this.searchWholeWords = wholeWords;
        
        searchResults.clear();
        currentResultIndex = -1;
        
        String text = codeEditText.getText().toString();
        
        try {
            Pattern pattern = createSearchPattern(query, caseSensitive, useRegex, wholeWords);
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                SearchResult result = new SearchResult(
                    matcher.start(),
                    matcher.end(),
                    matcher.group(),
                    matcher.group(0) // full match
                );
                searchResults.add(result);
            }
            
            Log.d("SearchReplaceManager", "Found " + searchResults.size() + " matches for: " + query);
            
            // Highlight first result if found
            if (!searchResults.isEmpty()) {
                highlightResult(0);
            }
            
            return searchResults;
            
        } catch (Exception e) {
            Log.e("SearchReplaceManager", "Error during search", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * إنشاء pattern للبحث
     */
    private Pattern createSearchPattern(String query, boolean caseSensitive, boolean useRegex, boolean wholeWords) {
        String patternString;
        
        if (useRegex) {
            patternString = query;
        } else {
            // Escape special regex characters if not using regex
            patternString = Pattern.quote(query);
            
            if (wholeWords) {
                patternString = "\\b" + patternString + "\\b";
            }
        }
        
        int flags = Pattern.MULTILINE;
        if (!caseSensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        
        return Pattern.compile(patternString, flags);
    }
    
    /**
     * الانتقال إلى النتيجة التالية
     */
    public boolean goToNext() {
        if (searchResults.isEmpty()) {
            return false;
        }
        
        currentResultIndex = (currentResultIndex + 1) % searchResults.size();
        highlightResult(currentResultIndex);
        
        return true;
    }
    
    /**
     * الانتقال إلى النتيجة السابقة
     */
    public boolean goToPrevious() {
        if (searchResults.isEmpty()) {
            return false;
        }
        
        currentResultIndex = currentResultIndex <= 0 ? 
            searchResults.size() - 1 : currentResultIndex - 1;
        highlightResult(currentResultIndex);
        
        return true;
    }
    
    /**
     * تظليل نتيجة معينة
     */
    private void highlightResult(int index) {
        if (index < 0 || index >= searchResults.size()) {
            clearSearchHighlights();
            return;
        }
        
        SearchResult result = searchResults.get(index);
        codeEditText.setSelection(result.start, result.end);
        
        // Scroll to the selected result
        codeEditText.requestFocus();
        codeEditText.post(() -> {
            // Scroll to the selection
            int line = codeEditText.getLayout().getLineForOffset(result.start);
            codeEditText.requestFocusFromTouch();
            codeEditText.setSelection(result.start);
        });
        
        Log.d("SearchReplaceManager", "Highlighted result " + (index + 1) + " of " + searchResults.size());
    }
    
    /**
     * الاستبدال في النتيجة الحالية
     */
    public boolean replaceCurrent(String replacement) {
        if (currentResultIndex < 0 || currentResultIndex >= searchResults.size()) {
            return false;
        }
        
        SearchResult result = searchResults.get(currentResultIndex);
        return replaceAt(result.start, result.end, replacement);
    }
    
    /**
     * الاستبدال في جميع النتائج
     */
    public int replaceAll(String query, String replacement, boolean caseSensitive, boolean useRegex) {
        List<SearchResult> results = search(query, caseSensitive, useRegex, false);
        
        if (results.isEmpty()) {
            return 0;
        }
        
        int replaceCount = 0;
        Editable text = codeEditText.getText();
        int offset = 0; // Track position changes due to replacements
        
        // Replace from end to start to maintain positions
        for (int i = results.size() - 1; i >= 0; i--) {
            SearchResult result = results.get(i);
            int adjustedStart = result.start + offset;
            int adjustedEnd = result.end + offset;
            
            if (replaceAt(adjustedStart, adjustedEnd, replacement)) {
                replaceCount++;
                offset += replacement.length() - (result.end - result.start);
            }
        }
        
        Log.d("SearchReplaceManager", "Replaced " + replaceCount + " occurrences");
        
        // Clear search results after replacement
        clearSearch();
        
        return replaceCount;
    }
    
    /**
     * تنفيذ الاستبدال في موقع محدد
     */
    private boolean replaceAt(int start, int end, String replacement) {
        try {
            Editable text = codeEditText.getText();
            text.replace(start, end, replacement);
            return true;
        } catch (Exception e) {
            Log.e("SearchReplaceManager", "Error replacing text", e);
            return false;
        }
    }
    
    /**
     * مسح البحث والتظليل
     */
    public void clearSearch() {
        searchResults.clear();
        currentResultIndex = -1;
        clearSearchHighlights();
        
        Log.d("SearchReplaceManager", "Search cleared");
    }
    
    /**
     * مسح تظليل نتائج البحث
     */
    private void clearSearchHighlights() {
        // In a full implementation, you would remove search highlighting spans
        // For now, we just clear the selection
        codeEditText.clearFocus();
    }
    
    /**
     * الحصول على عدد النتائج
     */
    public int getResultCount() {
        return searchResults.size();
    }
    
    /**
     * الحصول على فهرس النتيجة الحالية
     */
    public int getCurrentResultIndex() {
        return currentResultIndex;
    }
    
    /**
     * الانتقال إلى نتيجة محددة
     */
    public boolean goToResult(int index) {
        if (index < 0 || index >= searchResults.size()) {
            return false;
        }
        
        currentResultIndex = index;
        highlightResult(index);
        return true;
    }
    
    /**
     * فئة نتائج البحث
     */
    public static class SearchResult {
        public final int start;
        public final int end;
        public final String matchedText;
        public final String fullMatch;
        
        public SearchResult(int start, int end, String matchedText, String fullMatch) {
            this.start = start;
            this.end = end;
            this.matchedText = matchedText;
            this.fullMatch = fullMatch;
        }
        
        @Override
        public String toString() {
            return String.format("SearchResult{start=%d, end=%d, text='%s'}", 
                start, end, matchedText);
        }
    }
    
    /**
     * إعداد البحث في SearchView
     */
    public void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query, searchCaseSensitive, searchUseRegex, searchWholeWords);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.isEmpty()) {
                    search(newText, searchCaseSensitive, searchUseRegex, searchWholeWords);
                } else {
                    clearSearch();
                }
                return true;
            }
        });
    }
    
    /**
     * تبديل البحث الحساس لحالة الأحرف
     */
    public void toggleCaseSensitive() {
        searchCaseSensitive = !searchCaseSensitive;
        if (!lastSearchQuery.isEmpty()) {
            search(lastSearchQuery, searchCaseSensitive, searchUseRegex, searchWholeWords);
        }
    }
    
    /**
     * تبديل استخدام regex
     */
    public void toggleUseRegex() {
        searchUseRegex = !searchUseRegex;
        if (!lastSearchQuery.isEmpty()) {
            search(lastSearchQuery, searchCaseSensitive, searchUseRegex, searchWholeWords);
        }
    }
    
    /**
     * تبديل البحث عن الكلمات الكاملة
     */
    public void toggleWholeWords() {
        searchWholeWords = !searchWholeWords;
        if (!lastSearchQuery.isEmpty()) {
            search(lastSearchQuery, searchCaseSensitive, searchUseRegex, searchWholeWords);
        }
    }
}