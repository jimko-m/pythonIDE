package com.pythonide.editor;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * معالج الإكمال التلقائي (Autocomplete) للغة Python
 * يوفر suggestions ذكية وعرض ملائم لمحرر الكود
 */
public class AutoCompleteHandler {
    
    private Context context;
    private CodeEditText codeEditText;
    private String[] keywords;
    private String[] builtins;
    private CompletionPopupWindow popupWindow;
    
    private static final int MAX_COMPLETIONS = 10;
    
    // Common Python snippets and templates
    private Map<String, String> codeSnippets = new HashMap<>();
    private List<CompletionItem> allCompletions = new ArrayList<>();
    private List<CompletionItem> filteredCompletions = new ArrayList<>();
    
    public AutoCompleteHandler(Context context, CodeEditText codeEditText, String[] keywords, String[] builtins) {
        this.context = context;
        this.codeEditText = codeEditText;
        this.keywords = keywords;
        this.builtins = builtins;
        
        initializeCodeSnippets();
        initializeCompletions();
        
        // Set up text watcher for autocomplete trigger
        setupTextWatcher();
    }
    
    private void initializeCodeSnippets() {
        // Code templates and snippets
        codeSnippets.put("def", "def function_name():\n    \"\"\"Function description\"\"\"\n    pass");
        codeSnippets.put("class", "class ClassName:\n    \"\"\"Class description\"\"\"\n    def __init__(self):\n        pass");
        codeSnippets.put("if", "if condition:\n    # code here");
        codeSnippets.put("for", "for item in iterable:\n    # code here");
        codeSnippets.put("while", "while condition:\n    # code here");
        codeSnippets.put("try", "try:\n    # code here\nexcept Exception as e:\n    # handle exception");
        codeSnippets.put("import", "import module_name");
        codeSnippets.put("from", "from module_name import *");
        codeSnippets.put("print", "print(\"message\")");
        codeSnippets.put("return", "return value");
        codeSnippets.put("lambda", "lambda x: x");
        codeSnippets.put("with", "with context_manager as variable:\n    # code here");
        codeSnippets.put("if __name__", "if __name__ == \"__main__\":\n    # main code here");
    }
    
    private void initializeCompletions() {
        allCompletions.clear();
        
        // Add keywords
        if (keywords != null) {
            for (String keyword : keywords) {
                allCompletions.add(new CompletionItem(keyword, CompletionItem.TYPE_KEYWORD, "Python keyword"));
            }
        }
        
        // Add builtins
        if (builtins != null) {
            for (String builtin : builtins) {
                allCompletions.add(new CompletionItem(builtin, CompletionItem.TYPE_BUILTIN, "Python builtin function"));
            }
        }
        
        // Add code snippets
        for (Map.Entry<String, String> entry : codeSnippets.entrySet()) {
            allCompletions.add(new CompletionItem(entry.getKey(), CompletionItem.TYPE_SNIPPET, 
                "Code template", entry.getValue()));
        }
        
        // Add some common variable names and patterns
        addCommonCompletions();
    }
    
    private void addCommonCompletions() {
        String[] commonVars = {"data", "result", "value", "item", "items", "list", "dict", "str", "num", "count"};
        for (String var : commonVars) {
            allCompletions.add(new CompletionItem(var, CompletionItem.TYPE_VARIABLE, "Common variable name"));
        }
        
        String[] commonMethods = {"append", "extend", "remove", "pop", "sort", "reverse", "split", "join", "replace", "find"};
        for (String method : commonMethods) {
            allCompletions.add(new CompletionItem(method, CompletionItem.TYPE_METHOD, "Common method"));
        }
    }
    
    private void setupTextWatcher() {
        codeEditText.addTextChangedListener(new android.text.TextWatcher() {
            private String previousText = "";
            private int previousCursorPosition = 0;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousText = s.toString();
                previousCursorPosition = start;
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    char lastChar = s.charAt(start + count - 1);
                    
                    // Trigger autocomplete on dot or after keyword
                    if (lastChar == '.' || lastChar == '(' || shouldTriggerCompletion(s, start + count)) {
                        showCompletions(s, start + count);
                    }
                }
                
                // Hide popup if typing normal characters
                if (count > 0 && isNormalCharacter(lastChar)) {
                    hideCompletions();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Handle after text changed
            }
        });
    }
    
    private boolean shouldTriggerCompletion(CharSequence text, int position) {
        if (position <= 0) return false;
        
        char prevChar = text.charAt(position - 1);
        
        // Trigger completion after certain characters
        return prevChar == ' ' || prevChar == '(' || prevChar == ',';
    }
    
    private boolean isNormalCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }
    
    private void showCompletions(CharSequence text, int position) {
        String prefix = getCurrentWordPrefix(text, position);
        
        if (prefix.length() < 2) {
            hideCompletions();
            return;
        }
        
        filterCompletions(prefix);
        
        if (filteredCompletions.isEmpty()) {
            hideCompletions();
            return;
        }
        
        showCompletionPopup(position);
    }
    
    private String getCurrentWordPrefix(CharSequence text, int position) {
        if (position <= 0) return "";
        
        StringBuilder prefix = new StringBuilder();
        int i = position - 1;
        
        // Get all characters before cursor that are part of a word
        while (i >= 0 && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_' || text.charAt(i) == '.')) {
            prefix.insert(0, text.charAt(i));
            i--;
        }
        
        return prefix.toString().toLowerCase();
    }
    
    private void filterCompletions(String prefix) {
        filteredCompletions.clear();
        
        for (CompletionItem item : allCompletions) {
            if (item.name.toLowerCase().startsWith(prefix)) {
                filteredCompletions.add(item);
                if (filteredCompletions.size() >= MAX_COMPLETIONS) {
                    break;
                }
            }
        }
        
        // Sort completions by type priority and name
        sortCompletions();
    }
    
    private void sortCompletions() {
        filteredCompletions.sort((item1, item2) -> {
            // Sort by type priority (keywords first, then builtins, then others)
            if (item1.type != item2.type) {
                return Integer.compare(item1.type, item2.type);
            }
            return item1.name.compareToIgnoreCase(item2.name);
        });
    }
    
    private void showCompletionPopup(int position) {
        if (popupWindow == null || !popupWindow.isShowing()) {
            popupWindow = new CompletionPopupWindow(context, codeEditText);
        }
        
        popupWindow.setCompletions(filteredCompletions);
        popupWindow.showAtPosition(position);
    }
    
    public void hideCompletions() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
    
    public void applyCompletion(String completion) {
        int cursorPosition = codeEditText.getSelectionStart();
        int wordStart = getCurrentWordStart(cursorPosition);
        
        // Replace the current word with the completion
        Editable text = codeEditText.getText();
        text.replace(wordStart, cursorPosition, completion);
        
        // If it's a snippet, position cursor appropriately
        if (completion.contains("\n")) {
            int newCursorPosition = wordStart + completion.indexOf("(") + 1;
            if (newCursorPosition < wordStart + completion.length()) {
                codeEditText.setSelection(newCursorPosition);
            }
        }
        
        hideCompletions();
    }
    
    private int getCurrentWordStart(int position) {
        CharSequence text = codeEditText.getText();
        int start = position;
        
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || 
                           text.charAt(start - 1) == '_' || text.charAt(start - 1) == '.')) {
            start--;
        }
        
        return start;
    }
    
    // Completion item class
    public static class CompletionItem {
        public static final int TYPE_KEYWORD = 1;
        public static final int TYPE_BUILTIN = 2;
        public static final int TYPE_SNIPPET = 3;
        public static final int TYPE_VARIABLE = 4;
        public static final int TYPE_METHOD = 5;
        
        public String name;
        public String description;
        public String snippet;
        public int type;
        
        public CompletionItem(String name, int type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }
        
        public CompletionItem(String name, int type, String description, String snippet) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.snippet = snippet;
        }
        
        public String getTypeColor() {
            switch (type) {
                case TYPE_KEYWORD: return "#9F44D3"; // Purple
                case TYPE_BUILTIN: return "#2286C3"; // Blue
                case TYPE_SNIPPET: return "#238755"; // Green
                case TYPE_VARIABLE: return "#FF6B47"; // Red
                case TYPE_METHOD: return "#9C27B0"; // Deep Purple
                default: return "#666666";
            }
        }
    }
    
    // Popup window for completions
    private static class CompletionPopupWindow {
        private android.widget.PopupWindow popupWindow;
        private ListViewAdapter adapter;
        private CodeEditText targetEditText;
        private Context context;
        
        public CompletionPopupWindow(Context context, CodeEditText targetEditText) {
            this.context = context;
            this.targetEditText = targetEditText;
            this.adapter = new ListViewAdapter();
            
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
            
            popupWindow = new android.widget.PopupWindow(view, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 
                true);
            
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.drawable.btn_default));
        }
        
        public void setCompletions(List<CompletionItem> completions) {
            adapter.setItems(completions);
        }
        
        public void showAtPosition(int position) {
            // Calculate position and show popup
            popupWindow.showAsDropDown(targetEditText, 0, -targetEditText.getHeight());
        }
        
        public boolean isShowing() {
            return popupWindow != null && popupWindow.isShowing();
        }
        
        public void dismiss() {
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
        }
    }
    
    // List adapter for completion items
    private static class ListViewAdapter extends BaseAdapter implements Filterable {
        private List<CompletionItem> items = new ArrayList<>();
        private List<CompletionItem> originalItems = new ArrayList<>();
        private android.widget.Filter filter;
        
        public void setItems(List<CompletionItem> items) {
            this.items = new ArrayList<>(items);
            this.originalItems = new ArrayList<>(items);
            notifyDataSetChanged();
        }
        
        @Override
        public int getCount() {
            return items.size();
        }
        
        @Override
        public CompletionItem getItem(int position) {
            return items.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            
            CompletionItem item = getItem(position);
            TextView textView = convertView.findViewById(android.R.id.text1);
            
            textView.setText(item.name);
            textView.setTextColor(Color.parseColor(item.getTypeColor()));
            
            return convertView;
        }
        
        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new CompletionFilter();
            }
            return filter;
        }
        
        private class CompletionFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                
                if (constraint == null || constraint.length() == 0) {
                    results.values = originalItems;
                    results.count = originalItems.size();
                } else {
                    List<CompletionItem> filtered = new ArrayList<>();
                    for (CompletionItem item : originalItems) {
                        if (item.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            filtered.add(item);
                        }
                    }
                    results.values = filtered;
                    results.count = filtered.size();
                }
                
                return results;
            }
            
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                items = (List<CompletionItem>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}