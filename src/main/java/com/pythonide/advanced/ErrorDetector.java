package com.pythonide.advanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorDetector {
    private List<ErrorDetectionListener> listeners;
    private Map<String, String> errorPatterns;
    private Map<String, String> commonFixes;
    private boolean realTimeDetection;

    public interface ErrorDetectionListener {
        void onErrorDetected(List<ErrorInfo> errors);
        void onErrorFixed(ErrorInfo error);
    }

    public ErrorDetector() {
        this.listeners = new ArrayList<>();
        this.errorPatterns = new HashMap<>();
        this.commonFixes = new HashMap<>();
        this.realTimeDetection = true;

        initializeErrorPatterns();
        initializeCommonFixes();
    }

    // --- ErrorInfo inner class ---
    public static class ErrorInfo {
        private ErrorType type;
        private String message;
        private String suggestion;
        private String codeFix;
        private int lineNumber;
        private int columnNumber;
        private String filePath;
        private String severity;
        private List<String> examples;
        private String documentation;

        public ErrorInfo(ErrorType type, String message, String suggestion, String codeFix, int line) {
            this(type, message, suggestion, codeFix, line, 0, "خطأ");
        }

        public ErrorInfo(ErrorType type, String message, String suggestion, String codeFix,
                         int line, int column, String severity) {
            this.type = type;
            this.message = message != null ? message : "";
            this.suggestion = suggestion != null ? suggestion : "";
            this.codeFix = codeFix != null ? codeFix : "";
            this.lineNumber = Math.max(1, line);
            this.columnNumber = Math.max(0, column);
            this.severity = severity != null ? severity : "خطأ";
            this.examples = new ArrayList<>();
            this.documentation = "";
            this.filePath = "";
        }

        public void addExample(String example) {
            if (example != null && !example.isEmpty()) {
                examples.add(example);
            }
        }

        public void setDocumentation(String docUrl) {
            this.documentation = docUrl != null ? docUrl : "";
        }

        // Getters / Setters
        public ErrorType getType() { return type; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
        public String getCodeFix() { return codeFix; }
        public int getLineNumber() { return lineNumber; }
        public int getColumnNumber() { return columnNumber; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String path) { this.filePath = path != null ? path : ""; }
        public String getSeverity() { return severity; }
        public List<String> getExamples() { return examples; }
        public String getDocumentation() { return documentation; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("السطر ").append(lineNumber).append(": ");
            sb.append(type != null ? type.getDisplayName() : "UNKNOWN").append("\n");
            sb.append(message != null ? message : "");
            if (suggestion != null && !suggestion.isEmpty()) {
                sb.append("\nاقتراح: ").append(suggestion);
            }
            if (codeFix != null && !codeFix.isEmpty()) {
                sb.append("\nالإصلاح:\n").append(codeFix);
            }
            return sb.toString();
        }
    }

    // stub methods to initialize patterns/fixes
    private void initializeErrorPatterns() {
        // ضع هنا initial regex patterns إن رغبت
    }

    private void initializeCommonFixes() {
        commonFixes.put("pandas", "pip install pandas");
        commonFixes.put("numpy", "pip install numpy");
        commonFixes.put("matplotlib", "pip install matplotlib");
        commonFixes.put("requests", "pip install requests");
        commonFixes.put("flask", "pip install flask");
        commonFixes.put("django", "pip install django");
        commonFixes.put("scikit-learn", "pip install scikit-learn");
        commonFixes.put("tensorflow", "pip install tensorflow");
        commonFixes.put("pytorch", "pip install torch");

        commonFixes.put("missing_colon", "أضف نقطتين (:) في نهاية السطر");
        commonFixes.put("missing_quote", "أضف علامات اقتباس");
        commonFixes.put("missing_parenthesis", "أضف قوساً");
        commonFixes.put("missing_bracket", "أضف قوساً مربعاً");
    }
}
