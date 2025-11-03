package com.pythonide.advanced;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Theme Customization Manager for Python IDE
 * Provides comprehensive theme management with syntax highlighting, UI themes, and customization options
 */
public class ThemeManager {
    
    public enum ThemeCategory {
        EDITOR_THEME("محرر الأكواد"),
        UI_THEME("واجهة المستخدم"),
        SYNTAX_THEME("ألوان النحو"),
        DARK_MODE("الوضع الليلي"),
        ACCESSIBILITY("إمكانية الوصول"),
        CUSTOM("مخصص");
        
        private final String displayName;
        
        ThemeCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ThemeVariant {
        LIGHT("فاتح"),
        DARK("داكن"),
        AUTO("تلقائي"),
        HIGH_CONTRAST("تباين عالي"),
        SEPIA("بني داكن");
        
        private final String displayName;
        
        ThemeVariant(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static class ColorScheme {
        private String name;
        private String id;
        private Map<String, String> colors;
        private Map<String, Object> properties;
        private String description;
        private boolean isDark;
        private List<String> supportedLanguages;
        private String author;
        private String version;
        
        public ColorScheme(String id, String name) {
            this.id = id;
            this.name = name;
            this.colors = new HashMap<>();
            this.properties = new HashMap<>();
            this.supportedLanguages = new ArrayList<>();
            this.isDark = false;
            this.version = "1.0";
        }
        
        public void addColor(String key, String hexColor) {
            colors.put(key, hexColor);
        }
        
        public void addProperty(String key, Object value) {
            properties.put(key, value);
        }
        
        public void addSupportedLanguage(String language) {
            if (!supportedLanguages.contains(language)) {
                supportedLanguages.add(language);
            }
        }
        
        public String getColor(String key) {
            return colors.get(key);
        }
        
        public Object getProperty(String key) {
            return properties.get(key);
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Map<String, String> getColors() { return colors; }
        public Map<String, Object> getProperties() { return properties; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isDark() { return isDark; }
        public void setDark(boolean dark) { isDark = dark; }
        public List<String> getSupportedLanguages() { return supportedLanguages; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String toString() {
            return name + " (" + (isDark ? "داكن" : "فاتح") + ")";
        }
    }
    
    public static class EditorTheme extends ColorScheme {
        private Map<String, EditorStyle> editorStyles;
        private String fontFamily;
        private int fontSize;
        private int lineHeight;
        private boolean showLineNumbers;
        private boolean showWhitespace;
        private boolean enableCodeFolding;
        private boolean enableMinimap;
        
        public EditorTheme(String id, String name) {
            super(id, name);
            this.editorStyles = new HashMap<>();
            this.fontFamily = "Monaco, Consolas, 'Courier New', monospace";
            this.fontSize = 14;
            this.lineHeight = 1.5;
            this.showLineNumbers = true;
            this.showWhitespace = false;
            this.enableCodeFolding = true;
            this.enableMinimap = true;
            
            initializeDefaultColors();
        }
        
        private void initializeDefaultColors() {
            // Editor background and text colors
            addColor("editor.background", isDark() ? "#1e1e1e" : "#ffffff");
            addColor("editor.foreground", isDark() ? "#d4d4d4" : "#000000");
            addColor("editor.lineHighlight", isDark() ? "#2a2a2a" : "#f5f5f5");
            addColor("editor.selection", isDark() ? "#264f78" : "#0078d4");
            addColor("editor.inactiveSelection", isDark() ? "#3a3d41" : "#d5e7ff");
            
            // Line number colors
            addColor("editorLineNumber.foreground", isDark() ? "#858585" : "#237893");
            addColor("editorLineNumber.activeForeground", isDark() ? "#c6c6c6" : "#007acc");
            
            // Bracket matching
            addColor("editorBracketMatch.background", isDark() ? "#2d2b30" : "#e9e600");
            addColor("editorBracketMatch.border", isDark() ? "#515151" : "#d7ba7d");
            
            // Indent guides
            addColor("editorIndentGuide.background", isDark() ? "#404040" : "#d0d0d0");
            addColor("editorIndentGuide.activeBackground", isDark() ? "#707070" : "#939393");
            
            // Cursor
            addColor("editorCursor.foreground", isDark() ? "#aeafad" : "#000000");
            
            // Whitespace
            addColor("editorWhitespace.foreground", isDark() ? "#404040" : "#bfbfbf");
            
            // Error squiggles
            addColor("editorError.foreground", "#f44747");
            addColor("editorWarning.foreground", "#ff8c00");
            addColor("editorInfo.foreground", "#4fc1ff");
            
            // Code folding
            addColor("editorFoldBackground", isDark() ? "#2d2b30" : "#f3f3f3");
            
            // Scrollbar
            addColor("scrollbar.shadow", isDark() ? "#097991" : "#a89000");
            addColor("scrollbarSlider.background", isDark() ? "#79797966" : "#88139191");
            addColor("scrollbarSlider.hoverBackground", isDark() ? "#64646466" : "#771e1e1e");
            addColor("scrollbarSlider.activeBackground", isDark() ? "#8f8f8f66" : "#77111111");
            
            // Gutter
            addColor("editorGutter.background", isDark() ? "#1e1e1e" : "#f8f8f8");
            addColor("editorGutter.modifiedBackground", isDark() ? "#0c63e4" : "#0078d4");
            addColor("editorGutter.addedBackground", isDark() ? "#0e8a16" : "#185abd");
            addColor("editorGutter.deletedBackground", isDark() ? "#bd2c00" : "#d73a49");
            addColor("editorGutter.unusedIndexForeground", isDark() ? "#808080" : "#8c8c8c");
            addColor("editorGutter.warningForeground", "#008000");
            addColor("editorGutter.errorForeground", "#ff0000");
        }
        
        public void addEditorStyle(String tokenType, EditorStyle style) {
            editorStyles.put(tokenType, style);
        }
        
        public EditorStyle getEditorStyle(String tokenType) {
            return editorStyles.get(tokenType);
        }
        
        // Getters and setters for editor-specific properties
        public Map<String, EditorStyle> getEditorStyles() { return editorStyles; }
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public int getLineHeight() { return lineHeight; }
        public void setLineHeight(int lineHeight) { this.lineHeight = lineHeight; }
        public boolean isShowLineNumbers() { return showLineNumbers; }
        public void setShowLineNumbers(boolean showLineNumbers) { this.showLineNumbers = showLineNumbers; }
        public boolean isShowWhitespace() { return showWhitespace; }
        public void setShowWhitespace(boolean showWhitespace) { this.showWhitespace = showWhitespace; }
        public boolean isEnableCodeFolding() { return enableCodeFolding; }
        public void setEnableCodeFolding(boolean enableCodeFolding) { this.enableCodeFolding = enableCodeFolding; }
        public boolean isEnableMinimap() { return enableMinimap; }
        public void setEnableMinimap(boolean enableMinimap) { this.enableMinimap = enableMinimap; }
    }
    
    public static class EditorStyle {
        private String foreground;
        private String background;
        private String border;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private int fontSize;
        
        public EditorStyle(String foreground) {
            this.foreground = foreground;
            this.bold = false;
            this.italic = false;
            this.underline = false;
        }
        
        public EditorStyle(String foreground, boolean bold, boolean italic) {
            this.foreground = foreground;
            this.bold = bold;
            this.italic = italic;
        }
        
        // Getters and setters
        public String getForeground() { return foreground; }
        public void setForeground(String foreground) { this.foreground = foreground; }
        public String getBackground() { return background; }
        public void setBackground(String background) { this.background = background; }
        public String getBorder() { return border; }
        public void setBorder(String border) { this.border = border; }
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        public boolean isItalic() { return italic; }
        public void setItalic(boolean italic) { this.italic = italic; }
        public boolean isUnderline() { return underline; }
        public void setUnderline(boolean underline) { this.underline = underline; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    }
    
    public static class SyntaxHighlightingTheme extends ColorScheme {
        private Map<String, SyntaxColor> syntaxColors;
        private String language;
        
        public SyntaxHighlightingTheme(String id, String name, String language) {
            super(id, name);
            this.syntaxColors = new HashMap<>();
            this.language = language;
            this.supportedLanguages.add(language);
            
            initializeSyntaxColors(language);
        }
        
        private void initializeSyntaxColors(String language) {\n            switch (language.toLowerCase()) {\n                case \"python\":\n                    initializePythonSyntaxColors();\n                    break;\n                case \"javascript\":\n                    initializeJavaScriptSyntaxColors();\n                    break;\n                case \"java\":\n                    initializeJavaSyntaxColors();\n                    break;\n                case \"cpp\":\n                case \"c++\":\n                    initializeCppSyntaxColors();\n                    break;\n                case \"html\":\n                    initializeHtmlSyntaxColors();\n                    break;\n                case \"css\":\n                    initializeCssSyntaxColors();\n                    break;\n                default:\n                    initializeGenericSyntaxColors();\n                    break;\n            }\n        }\n        \n        private void initializePythonSyntaxColors() {\n            syntaxColors.put(\"keyword\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", true, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"number\", new SyntaxColor(isDark() ? \"#b5cea8\" : \"#09885a\", false, false));\n            syntaxColors.put(\"function\", new SyntaxColor(isDark() ? \"#dcdcaa\" : \"#795e26\", false, false));\n            syntaxColors.put(\"class\", new SyntaxColor(isDark() ? \"#4ec9b0\" : \"#267f99\", true, false));\n            syntaxColors.put(\"variable\", new SyntaxColor(getColor(\"editor.foreground\"), false, false));\n            syntaxColors.put(\"operator\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n            syntaxColors.put(\"punctuation\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n            syntaxColors.put(\"decorator\", new SyntaxColor(isDark() ? \"#c586c0\" : \"#af00db\", false, false));\n        }\n        \n        private void initializeJavaScriptSyntaxColors() {\n            syntaxColors.put(\"keyword\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", true, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"number\", new SyntaxColor(isDark() ? \"#b5cea8\" : \"#09885a\", false, false));\n            syntaxColors.put(\"function\", new SyntaxColor(isDark() ? \"#dcdcaa\" : \"#795e26\", false, false));\n            syntaxColors.put(\"variable\", new SyntaxColor(getColor(\"editor.foreground\"), false, false));\n            syntaxColors.put(\"operator\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n            syntaxColors.put(\"punctuation\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n        }\n        \n        private void initializeJavaSyntaxColors() {\n            syntaxColors.put(\"keyword\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", true, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"number\", new SyntaxColor(isDark() ? \"#b5cea8\" : \"#09885a\", false, false));\n            syntaxColors.put(\"function\", new SyntaxColor(isDark() ? \"#dcdcaa\" : \"#795e26\", false, false));\n            syntaxColors.put(\"class\", new SyntaxColor(isDark() ? \"#4ec9b0\" : \"#267f99\", true, false));\n            syntaxColors.put(\"variable\", new SyntaxColor(getColor(\"editor.foreground\"), false, false));\n            syntaxColors.put(\"operator\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n            syntaxColors.put(\"type\", new SyntaxColor(isDark() ? \"#4ec9b0\" : \"#267f99\", true, false));\n        }\n        \n        private void initializeCppSyntaxColors() {\n            syntaxColors.put(\"keyword\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", true, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"number\", new SyntaxColor(isDark() ? \"#b5cea8\" : \"#09885a\", false, false));\n            syntaxColors.put(\"function\", new SyntaxColor(isDark() ? \"#dcdcaa\" : \"#795e26\", false, false));\n            syntaxColors.put(\"class\", new SyntaxColor(isDark() ? \"#4ec9b0\" : \"#267f99\", true, false));\n            syntaxColors.put(\"variable\", new SyntaxColor(getColor(\"editor.foreground\"), false, false));\n            syntaxColors.put(\"operator\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n            syntaxColors.put(\"preprocessor\", new SyntaxColor(isDark() ? \"#c586c0\" : \"#af00db\", false, false));\n        }\n        \n        private void initializeHtmlSyntaxColors() {\n            syntaxColors.put(\"tag\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", false, false));\n            syntaxColors.put(\"attribute\", new SyntaxColor(isDark() ? \"#9cdcfe\" : \"#0451a5\", false, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"punctuation\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n        }\n        \n        private void initializeCssSyntaxColors() {\n            syntaxColors.put(\"property\", new SyntaxColor(isDark() ? \"#9cdcfe\" : \"#0451a5\", false, false));\n            syntaxColors.put(\"value\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"keyword\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", true, false));\n            syntaxColors.put(\"function\", new SyntaxColor(isDark() ? \"#dcdcaa\" : \"#795e26\", false, false));\n            syntaxColors.put(\"punctuation\", new SyntaxColor(isDark() ? \"#d4d4d4\" : \"#000000\", false, false));\n        }\n        \n        private void initializeGenericSyntaxColors() {\n            // Default colors for unknown languages\n            syntaxColors.put(\"keyword\", new SyntaxColor(isDark() ? \"#569cd6\" : \"#0000ff\", true, false));\n            syntaxColors.put(\"string\", new SyntaxColor(isDark() ? \"#ce9178\" : \"#a31515\", false, false));\n            syntaxColors.put(\"comment\", new SyntaxColor(isDark() ? \"#6a9955\" : \"#008000\", false, true));\n            syntaxColors.put(\"number\", new SyntaxColor(isDark() ? \"#b5cea8\" : \"#09885a\", false, false));\n            syntaxColors.put(\"function\", new SyntaxColor(isDark() ? \"#dcdcaa\" : \"#795e26\", false, false));\n            syntaxColors.put(\"variable\", new SyntaxColor(getColor(\"editor.foreground\"), false, false));\n        }\n        \n        public void addSyntaxColor(String tokenType, SyntaxColor color) {\n            syntaxColors.put(tokenType, color);\n        }\n        \n        public SyntaxColor getSyntaxColor(String tokenType) {\n            return syntaxColors.get(tokenType);\n        }\n        \n        public String getLanguage() { return language; }\n        public Map<String, SyntaxColor> getSyntaxColors() { return syntaxColors; }\n    }\n    \n    public static class SyntaxColor {\n        private String color;\n        private boolean bold;\n        private boolean italic;\n        \n        public SyntaxColor(String color, boolean bold, boolean italic) {\n            this.color = color;\n            this.bold = bold;\n            this.italic = italic;\n        }\n        \n        // Getters\n        public String getColor() { return color; }\n        public boolean isBold() { return bold; }\n        public boolean isItalic() { return italic; }\n    }\n    \n    public interface ThemeListener {\n        void onThemeChanged(ColorScheme newTheme);\n        void onThemeApplied(String themeId);\n        void onThemeSettingsUpdated();\n    }\n    \n    private Map<String, ColorScheme> colorSchemes;\n    private Map<String, EditorTheme> editorThemes;\n    private Map<String, SyntaxHighlightingTheme> syntaxThemes;\n    private List<ThemeListener> listeners;\n    private ColorScheme currentTheme;\n    private EditorTheme currentEditorTheme;\n    private ThemeVariant currentVariant;\n    private String userSettingsPath;\n    \n    public ThemeManager(String userSettingsPath) {\n        this.userSettingsPath = userSettingsPath;\n        this.colorSchemes = new HashMap<>();\n        this.editorThemes = new HashMap<>();\n        this.syntaxThemes = new HashMap<>();\n        this.listeners = new ArrayList<>();\n        this.currentVariant = ThemeVariant.AUTO;\n        \n        initializeDefaultThemes();\n        loadUserThemes();\n    }\n    \n    private void initializeDefaultThemes() {\n        // Dark theme\n        EditorTheme darkTheme = new EditorTheme(\"dark\", \"الوضع الداكن\");\n        darkTheme.setDark(true);\n        editorThemes.put(darkTheme.getId(), darkTheme);\n        \n        // Light theme\n        EditorTheme lightTheme = new EditorTheme(\"light\", \"الوضع الفاتح\");\n        lightTheme.setDark(false);\n        editorThemes.put(lightTheme.getId(), lightTheme);\n        \n        // High contrast theme\n        EditorTheme highContrastTheme = new EditorTheme(\"high-contrast\", \"التباين العالي\");\n        highContrastTheme.setDark(true);\n        highContrastTheme.addColor(\"editor.background\", \"#000000\");\n        highContrastTheme.addColor(\"editor.foreground\", \"#ffffff\");\n        highContrastTheme.addColor(\"editor.selection\", \"#ffff00\");\n        highContrastTheme.addColor(\"editorLineNumber.foreground\", \"#ffffff\");\n        highContrastTheme.addColor(\"editorCursor.foreground\", \"#ffffff\");\n        highContrastTheme.addColor(\"editorError.foreground\", \"#ff0000\");\n        highContrastTheme.addColor(\"editorWarning.foreground\", \"#ffff00\");\n        highContrastTheme.addColor(\"editorInfo.foreground\", \"#00ffff\");\n        highContrastTheme.setFontSize(16);\n        editorThemes.put(highContrastTheme.getId(), highContrastTheme);\n        \n        // Sepia theme\n        EditorTheme sepiaTheme = new EditorTheme(\"sepia\", \"بني داكن\");\n        sepiaTheme.setDark(false);\n        sepiaTheme.addColor(\"editor.background\", \"#f4f0e8\");\n        sepiaTheme.addColor(\"editor.foreground\", \"#5c4b37\");\n        sepiaTheme.addColor(\"editor.lineHighlight\", \"#e8e0d8\");\n        sepiaTheme.addColor(\"editorLineNumber.foreground\", \"#8b7355\");\n        sepiaTheme.addColor(\"editor.selection\", \"#e8d4b8\");\n        sepiaTheme.addColor(\"editorCursor.foreground\", \"#5c4b37\");\n        sepiaTheme.addColor(\"editorError.foreground\", \"#cc0000\");\n        sepiaTheme.addColor(\"editorWarning.foreground\", \"#cc6600\");\n        sepiaTheme.addColor(\"editorInfo.foreground\", \"#0066cc\");\n        editorThemes.put(sepiaTheme.getId(), sepiaTheme);\n        \n        // Python syntax themes\n        syntaxThemes.put(\"python-dark\", new SyntaxHighlightingTheme(\"python-dark\", \"Python Dark\", \"python\"));\n        syntaxThemes.put(\"python-light\", new SyntaxHighlightingTheme(\"python-light\", \"Python Light\", \"python\"));\n        \n        // JavaScript syntax themes\n        syntaxThemes.put(\"js-dark\", new SyntaxHighlightingTheme(\"js-dark\", \"JavaScript Dark\", \"javascript\"));\n        syntaxThemes.put(\"js-light\", new SyntaxHighlightingTheme(\"js-light\", \"JavaScript Light\", \"javascript\"));\n        \n        // Set default themes\n        currentTheme = colorSchemes.get(\"dark\");\n        currentEditorTheme = darkTheme;\n    }\n    \n    /**\n     * Apply theme to editor\n     */\n    public CompletableFuture<Boolean> applyEditorTheme(String themeId) {\n        return CompletableFuture.supplyAsync(() -> {\n            EditorTheme theme = editorThemes.get(themeId);\n            if (theme != null) {\n                currentEditorTheme = theme;\n                saveThemeSettings();\n                notifyThemeApplied(themeId);\n                return true;\n            }\n            return false;\n        });\n    }\n    \n    /**\n     * Apply syntax highlighting theme\n     */\n    public CompletableFuture<Boolean> applySyntaxTheme(String themeId, String language) {\n        return CompletableFuture.supplyAsync(() -> {\n            String fullThemeId = language.toLowerCase() + \"-\" + themeId;\n            SyntaxHighlightingTheme theme = syntaxThemes.get(fullThemeId);\n            if (theme != null) {\n                // Apply theme to current editor\n                applySyntaxColorsToEditor(theme);\n                notifyThemeSettingsUpdated();\n                return true;\n            }\n            return false;\n        });\n    }\n    \n    /**\n     * Create custom theme\n     */\n    public CompletableFuture<EditorTheme> createCustomTheme(String name, String baseThemeId) {\n        return CompletableFuture.supplyAsync(() -> {\n            EditorTheme baseTheme = editorThemes.get(baseThemeId);\n            if (baseTheme == null) {\n                return null;\n            }\n            \n            EditorTheme customTheme = new EditorTheme(\"custom-\" + System.currentTimeMillis(), name);\n            \n            // Copy colors from base theme\n            customTheme.getColors().putAll(baseTheme.getColors());\n            customTheme.setDark(baseTheme.isDark());\n            customTheme.setFontFamily(baseTheme.getFontFamily());\n            customTheme.setFontSize(baseTheme.getFontSize());\n            customTheme.setLineHeight(baseTheme.getLineHeight());\n            \n            // Save custom theme\n            editorThemes.put(customTheme.getId(), customTheme);\n            saveCustomTheme(customTheme);\n            \n            return customTheme;\n        });\n    }\n    \n    /**\n     * Update theme color\n     */\n    public CompletableFuture<Boolean> updateThemeColor(String themeId, String colorKey, String hexColor) {\n        return CompletableFuture.supplyAsync(() -> {\n            ColorScheme theme = colorSchemes.get(themeId);\n            if (theme == null) {\n                theme = editorThemes.get(themeId);\n            }\n            \n            if (theme != null) {\n                theme.addColor(colorKey, hexColor);\n                saveThemeSettings();\n                notifyThemeSettingsUpdated();\n                return true;\n            }\n            return false;\n        });\n    }\n    \n    /**\n     * Get all available editor themes\n     */\n    public Map<String, EditorTheme> getAllEditorThemes() {\n        return new HashMap<>(editorThemes);\n    }\n    \n    /**\n     * Get syntax themes for specific language\n     */\n    public List<SyntaxHighlightingTheme> getSyntaxThemesForLanguage(String language) {\n        return syntaxThemes.values().stream()\n                .filter(theme -> language.equalsIgnoreCase(theme.getLanguage()))\n                .collect(java.util.stream.Collectors.toList());\n    }\n    \n    /**\n     * Get current editor theme\n     */\n    public EditorTheme getCurrentEditorTheme() {\n        return currentEditorTheme;\n    }\n    \n    /**\n     * Set theme variant\n     */\n    public void setThemeVariant(ThemeVariant variant) {\n        this.currentVariant = variant;\n        \n        switch (variant) {\n            case DARK:\n                currentEditorTheme = editorThemes.get(\"dark\");\n                break;\n            case LIGHT:\n                currentEditorTheme = editorThemes.get(\"light\");\n                break;\n            case HIGH_CONTRAST:\n                currentEditorTheme = editorThemes.get(\"high-contrast\");\n                break;\n            case SEPIA:\n                currentEditorTheme = editorThemes.get(\"sepia\");\n                break;\n            case AUTO:\n                // Auto detect based on system preference\n                boolean isDarkMode = detectSystemDarkMode();\n                currentEditorTheme = editorThemes.get(isDarkMode ? \"dark\" : \"light\");\n                break;\n        }\n        \n        saveThemeSettings();\n        notifyThemeSettingsUpdated();\n    }\n    \n    /**\n     * Export theme\n     */\n    public CompletableFuture<String> exportTheme(String themeId, String format) {\n        return CompletableFuture.supplyAsync(() -> {\n            EditorTheme theme = editorThemes.get(themeId);\n            if (theme == null) {\n                return null;\n            }\n            \n            try {\n                String fileName = theme.getName().replaceAll(\"[^a-zA-Z0-9_-]\", \"_\") + \".\" + format;\n                File exportFile = new File(userSettingsPath, \"themes/\" + fileName);\n                exportFile.getParentFile().mkdirs();\n                \n                String themeData = serializeTheme(theme);\n                try (FileWriter writer = new FileWriter(exportFile)) {\n                    writer.write(themeData);\n                }\n                \n                return exportFile.getAbsolutePath();\n                \n            } catch (Exception e) {\n                return null;\n            }\n        });\n    }\n    \n    /**\n     * Import theme\n     */\n    public CompletableFuture<EditorTheme> importTheme(String filePath) {\n        return CompletableFuture.supplyAsync(() -> {\n            try {\n                StringBuilder json = new StringBuilder();\n                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {\n                    String line;\n                    while ((line = reader.readLine()) != null) {\n                        json.append(line);\n                    }\n                }\n                \n                EditorTheme theme = deserializeTheme(json.toString());\n                if (theme != null) {\n                    editorThemes.put(theme.getId(), theme);\n                    saveCustomTheme(theme);\n                    return theme;\n                }\n                \n            } catch (Exception e) {\n                // Handle import error\n            }\n            \n            return null;\n        });\n    }\n    \n    /**\n     * Get theme statistics\n     */\n    public Map<String, Object> getThemeStatistics() {\n        Map<String, Object> stats = new HashMap<>();\n        \n        stats.put(\"total_themes\", editorThemes.size());\n        stats.put(\"syntax_themes\", syntaxThemes.size());\n        \n        // Count by variant\n        Map<ThemeVariant, Integer> variantCounts = new HashMap<>();\n        for (ThemeVariant variant : ThemeVariant.values()) {\n            variantCounts.put(variant, 0);\n        }\n        \n        for (EditorTheme theme : editorThemes.values()) {\n            ThemeVariant variant = theme.isDark() ? ThemeVariant.DARK : ThemeVariant.LIGHT;\n            if (theme.getId().equals(\"high-contrast\")) {\n                variant = ThemeVariant.HIGH_CONTRAST;\n            } else if (theme.getId().equals(\"sepia\")) {\n                variant = ThemeVariant.SEPIA;\n            }\n            variantCounts.put(variant, variantCounts.get(variant) + 1);\n        }\n        stats.put(\"themes_by_variant\", variantCounts);\n        \n        // Popular languages\n        Map<String, Integer> languageCounts = new HashMap<>();\n        for (SyntaxHighlightingTheme theme : syntaxThemes.values()) {\n            languageCounts.put(theme.getLanguage(), \n                languageCounts.getOrDefault(theme.getLanguage(), 0) + 1);\n        }\n        stats.put(\"supported_languages\", languageCounts);\n        \n        // Custom themes count\n        long customThemesCount = editorThemes.values().stream()\n                .filter(theme -> theme.getId().startsWith(\"custom-\"))\n                .count();\n        stats.put(\"custom_themes\", customThemesCount);\n        \n        return stats;\n    }\n    \n    // Helper methods\n    private boolean detectSystemDarkMode() {\n        // Simplified dark mode detection\n        String os = System.getProperty(\"os.name\").toLowerCase();\n        if (os.contains(\"mac\") || os.contains(\"darwin\")) {\n            // On macOS, you would query the system preferences\n            return false; // Simplified\n        } else if (os.contains(\"win\")) {\n            // On Windows, you would check the registry\n            return false; // Simplified\n        }\n        return false;\n    }\n    \n    private void applySyntaxColorsToEditor(SyntaxHighlightingTheme syntaxTheme) {\n        if (currentEditorTheme == null) {\n            return;\n        }\n        \n        // Apply syntax colors to editor styles\n        for (Map.Entry<String, SyntaxColor> entry : syntaxTheme.getSyntaxColors().entrySet()) {\n            String tokenType = entry.getKey();\n            SyntaxColor syntaxColor = entry.getValue();\n            \n            EditorStyle style = new EditorStyle(syntaxColor.getColor(), \n                                               syntaxColor.isBold(), \n                                               syntaxColor.isItalic());\n            currentEditorTheme.addEditorStyle(tokenType, style);\n        }\n        \n        // Update theme with syntax colors\n        notifyThemeChanged(currentEditorTheme);\n    }\n    \n    private void saveThemeSettings() {\n        try {\n            File settingsFile = new File(userSettingsPath, \"theme_settings.json\");\n            settingsFile.getParentFile().mkdirs();\n            \n            String settings = createSettingsJson();\n            try (FileWriter writer = new FileWriter(settingsFile)) {\n                writer.write(settings);\n            }\n        } catch (Exception e) {\n            // Handle save error\n        }\n    }\n    \n    private void saveCustomTheme(EditorTheme theme) {\n        try {\n            File themeFile = new File(userSettingsPath, \"themes/\" + theme.getId() + \".json\");\n            themeFile.getParentFile().mkdirs();\n            \n            String themeData = serializeTheme(theme);\n            try (FileWriter writer = new FileWriter(themeFile)) {\n                writer.write(themeData);\n            }\n        } catch (Exception e) {\n            // Handle save error\n        }\n    }\n    \n    private void loadUserThemes() {\n        File themesDir = new File(userSettingsPath, \"themes\");\n        if (themesDir.exists() && themesDir.isDirectory()) {\n            File[] themeFiles = themesDir.listFiles((dir, name) -> name.endsWith(\".json\"));\n            if (themeFiles != null) {\n                for (File file : themeFiles) {\n                    try {\n                        StringBuilder json = new StringBuilder();\n                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {\n                            String line;\n                            while ((line = reader.readLine()) != null) {\n                                json.append(line);\n                            }\n                        }\n                        \n                        EditorTheme theme = deserializeTheme(json.toString());\n                        if (theme != null) {\n                            editorThemes.put(theme.getId(), theme);\n                        }\n                    } catch (Exception e) {\n                        // Handle loading error\n                    }\n                }\n            }\n        }\n    }\n    \n    private String createSettingsJson() {\n        StringBuilder json = new StringBuilder();\n        json.append(\"{\");\n        json.append(\"\\\"current_theme_id\\\": \\\"\").append(currentEditorTheme.getId()).append(\"\\\",\");\n        json.append(\"\\\"theme_variant\\\": \\\"\").append(currentVariant.name()).append(\"\\\",\");\n        json.append(\"\\\"font_family\\\": \\\"\").append(currentEditorTheme.getFontFamily()).append(\"\\\",\");\n        json.append(\"\\\"font_size\\\": \").append(currentEditorTheme.getFontSize()).append(\",\");\n        json.append(\"\\\"line_height\\\": \").append(currentEditorTheme.getLineHeight());\n        json.append(\"}\");\n        return json.toString();\n    }\n    \n    private String serializeTheme(EditorTheme theme) {\n        StringBuilder json = new StringBuilder();\n        json.append(\"{\");\n        json.append(\"\\\"id\\\": \\\"\").append(theme.getId()).append(\"\\\",\");\n        json.append(\"\\\"name\\\": \\\"\").append(escapeJson(theme.getName())).append(\"\\\",\");\n        json.append(\"\\\"isDark\\\": \").append(theme.isDark()).append(\",\");\n        json.append(\"\\\"fontFamily\\\": \\\"\").append(escapeJson(theme.getFontFamily())).append(\"\\\",\");\n        json.append(\"\\\"fontSize\\\": \").append(theme.getFontSize()).append(\",\");\n        json.append(\"\\\"lineHeight\\\": \").append(theme.getLineHeight());\n        json.append(\"}\");\n        return json.toString();\n    }\n    \n    private EditorTheme deserializeTheme(String json) {\n        try {\n            // Simple JSON parsing (in real implementation, use proper JSON library)\n            String id = extractJsonField(json, \"id\");\n            String name = extractJsonField(json, \"name\");\n            \n            if (id == null || name == null) {\n                return null;\n            }\n            \n            EditorTheme theme = new EditorTheme(id, name);\n            theme.setDark(Boolean.parseBoolean(extractJsonField(json, \"isDark\") != null ? \"true\" : \"false\"));\n            theme.setFontFamily(extractJsonField(json, \"fontFamily\"));\n            \n            return theme;\n            \n        } catch (Exception e) {\n            return null;\n        }\n    }\n    \n    private String extractJsonField(String json, String fieldName) {\n        String pattern = \"\\\"\" + fieldName + \"\\\":\\\\s*(\\\"?)([^\\\"]*)\\\"?\";\n        Pattern p = Pattern.compile(pattern);\n        Matcher m = p.matcher(json);\n        if (m.find()) {\n            String value = m.group(2);\n            return value.isEmpty() ? null : value;\n        }\n        return null;\n    }\n    \n    private String escapeJson(String str) {\n        if (str == null) return \"\";\n        return str.replace(\"\\\\\", \"\\\\\\\\\")\n                  .replace(\"\\\"\", \"\\\\\\\"\")\n                  .replace(\"\\n\", \"\\\\n\")\n                  .replace(\"\\r\", \"\\\\r\")\n                  .replace(\"\\t\", \"\\\\t\");\n    }\n    \n    // Notification methods\n    private void notifyThemeChanged(ColorScheme newTheme) {\n        for (ThemeListener listener : listeners) {\n            listener.onThemeChanged(newTheme);\n        }\n    }\n    \n    private void notifyThemeApplied(String themeId) {\n        for (ThemeListener listener : listeners) {\n            listener.onThemeApplied(themeId);\n        }\n    }\n    \n    private void notifyThemeSettingsUpdated() {\n        for (ThemeListener listener : listeners) {\n            listener.onThemeSettingsUpdated();\n        }\n    }\n    \n    // Public methods\n    public void addThemeListener(ThemeListener listener) {\n        listeners.add(listener);\n    }\n    \n    public ThemeVariant getCurrentVariant() {\n        return currentVariant;\n    }\n    \n    public Map<String, EditorTheme> getColorSchemes() {\n        return new HashMap<>(colorSchemes);\n    }\n}