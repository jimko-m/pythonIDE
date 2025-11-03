package com.pythonide.advanced;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.net.*;
import java.io.*;
import javax.crypto.*;
import java.util.Base64;

/**
 * Code Sharing Manager for QR codes and links
 * Provides intelligent code sharing with security and accessibility features
 */
public class SharingManager {
    
    public enum ShareType {
        QR_CODE("رمز QR"),
        LINK("رابط مباشر"),
        EMAIL("بريد إلكتروني"),
        SOCIAL_MEDIA("وسائل التواصل"),
        EXPORT_FILE("ملف مُصدَّر");
        
        private final String displayName;
        
        ShareType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum PermissionLevel {
        VIEW_ONLY("عرض فقط", "can_view"),
        COMMENT("تعليق", "can_comment"),
        EDIT("تعديل", "can_edit"),
        ADMIN("إدارة", "can_admin");
        
        private final String displayName;
        private final String permission;
        
        PermissionLevel(String displayName, String permission) {
            this.displayName = displayName;
            this.permission = permission;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getPermission() {
            return permission;
        }
    }
    
    public static class ShareableCode {
        private String id;
        private String code;
        private String title;
        private String description;
        private String filePath;
        private ShareType shareType;
        private PermissionLevel permission;
        private String author;
        private Date createdAt;
        private Date expiresAt;
        private int viewCount;
        private int maxViews;
        private boolean requirePassword;
        private String password;
        private List<String> allowedUsers;
        private Map<String, Object> metadata;
        private String thumbnail;
        private String language;
        private int lineCount;
        private boolean isPublic;
        
        public ShareableCode(String code, ShareType shareType) {
            this.id = generateId();
            this.code = code;
            this.shareType = shareType;
            this.permission = PermissionLevel.VIEW_ONLY;
            this.createdAt = new Date();
            this.viewCount = 0;
            this.maxViews = -1; // unlimited
            this.allowedUsers = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.isPublic = true;
            this.language = detectLanguage();
            this.lineCount = calculateLineCount();
            this.thumbnail = generateThumbnail();
        }
        
        public ShareableCode(String code, String title, String description, ShareType shareType) {
            this(code, shareType);
            this.title = title != null ? title : generateDefaultTitle();
            this.description = description != null ? description : "";
        }
        
        private String generateId() {
            return "share_" + System.currentTimeMillis() + "_" + Math.random();
        }
        
        private String generateDefaultTitle() {
            return "مشاركة الكود - " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        }
        
        private String detectLanguage() {
            // Simple language detection based on code patterns
            if (code.contains("def ") || code.contains("import ") || code.contains("class ")) {
                return "python";
            }
            if (code.contains("function ") || code.contains("var ") || code.contains("const ")) {
                return "javascript";
            }
            if (code.contains("#include") || code.contains("int main")) {
                return "c++";
            }
            return "text";
        }
        
        private int calculateLineCount() {
            return code.split("\r?\n").length;
        }
        
        private String generateThumbnail() {
            // Generate a simple text thumbnail
            String[] lines = code.split("\r?\n");
            StringBuilder thumbnail = new StringBuilder();
            int linesToShow = Math.min(5, lines.length);
            
            for (int i = 0; i < linesToShow; i++) {
                String line = lines[i];
                if (line.length() > 50) {
                    line = line.substring(0, 47) + "...";
                }
                thumbnail.append(line).append("\n");
            }
            
            if (lines.length > linesToShow) {
                thumbnail.append("... (").append(lines.length - linesToShow).append(" أسطر أخرى)");
            }
            
            return thumbnail.toString();
        }
        
        public void addAllowedUser(String userId) {
            if (!allowedUsers.contains(userId)) {
                allowedUsers.add(userId);
            }
        }
        
        public void removeAllowedUser(String userId) {
            allowedUsers.remove(userId);
        }
        
        public void setPassword(String password) {
            this.password = password;
            this.requirePassword = password != null && !password.isEmpty();
        }
        
        public boolean canView(String userId, String password) {
            if (!isPublic && !allowedUsers.contains(userId)) {
                return false;
            }
            
            if (requirePassword && !password.equals(this.password)) {
                return false;
            }
            
            if (maxViews > 0 && viewCount >= maxViews) {
                return false;
            }
            
            if (expiresAt != null && new Date().after(expiresAt)) {
                return false;
            }
            
            return true;
        }
        
        public void incrementViewCount() {
            this.viewCount++;
        }
        
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getCode() { return code; }
        public void setCode(String code) { 
            this.code = code; 
            this.language = detectLanguage();
            this.lineCount = calculateLineCount();
            this.thumbnail = generateThumbnail();
        }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public ShareType getShareType() { return shareType; }
        public void setShareType(ShareType shareType) { this.shareType = shareType; }
        public PermissionLevel getPermission() { return permission; }
        public void setPermission(PermissionLevel permission) { this.permission = permission; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public Date getCreatedAt() { return createdAt; }
        public Date getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
        public int getViewCount() { return viewCount; }
        public int getMaxViews() { return maxViews; }
        public void setMaxViews(int maxViews) { this.maxViews = maxViews; }
        public boolean isRequirePassword() { return requirePassword; }
        public String getPassword() { return password; }
        public List<String> getAllowedUsers() { return allowedUsers; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getThumbnail() { return thumbnail; }
        public String getLanguage() { return language; }
        public int getLineCount() { return lineCount; }
        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
        
        public String getFormattedSize() {
            int charCount = code.length();
            if (charCount < 1024) return charCount + " حرف";
            if (charCount < 1024 * 1024) return String.format("%.1f KB", charCount / 1024.0);
            return String.format("%.1f MB", charCount / (1024.0 * 1024.0));
        }
        
        public boolean isExpired() {
            return expiresAt != null && new Date().after(expiresAt);
        }
        
        public boolean canAccessMore() {
            return maxViews <= 0 || viewCount < maxViews;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("العنوان: ").append(title).append("\n");
            sb.append("النوع: ").append(shareType.getDisplayName()).append("\n");
            sb.append("المؤلف: ").append(author != null ? author : "غير محدد").append("\n");
            sb.append("تاريخ الإنشاء: ").append(createdAt).append("\n");
            sb.append("عدد المشاهدات: ").append(viewCount);
            if (maxViews > 0) {
                sb.append("/").append(maxViews);
            }
            sb.append("\n");
            sb.append("اللغة: ").append(language).append("\n");
            sb.append("عدد الأسطر: ").append(lineCount).append("\n");
            sb.append("الحجم: ").append(getFormattedSize()).append("\n");
            sb.append("المشترك: ").append(isPublic ? "نعم" : "لا");
            if (isExpired()) {
                sb.append(" (منتهي الصلاحية)");
            }
            return sb.toString();
        }
    }
    
    public interface SharingListener {
        void onShareCreated(ShareableCode shareableCode);
        void onShareAccessed(ShareableCode shareableCode, String userId);
        void onShareExpired(ShareableCode shareableCode);
        void onShareDeleted(String shareId);
    }
    
    private Map<String, ShareableCode> shareableCodes;
    private List<SharingListener> listeners;
    private String baseUrl;
    private String storagePath;
    private Cipher encryptor;
    private Cipher decryptor;
    
    public SharingManager(String baseUrl, String storagePath) {
        this.baseUrl = baseUrl;
        this.storagePath = storagePath;
        this.shareableCodes = new HashMap<>();
        this.listeners = new ArrayList<>();
        
        initializeEncryption();
        loadShareableCodes();
    }
    
    private void initializeEncryption() {
        try {
            // Initialize encryption for secure sharing
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            
            encryptor = Cipher.getInstance("AES/GCM/NoPadding");
            decryptor = Cipher.getInstance("AES/GCM/NoPadding");
            
        } catch (Exception e) {
            // Handle encryption initialization error
        }
    }
    
    /**\n     * Create a shareable code entry\n     */\n    public CompletableFuture<ShareableCode> createShareableCode(String code, ShareType shareType) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = new ShareableCode(code, shareType);\n            shareableCode.setAuthor(getCurrentUser());\n            \n            return processShareableCode(shareableCode);\n        });\n    }\n    \n    /**\n     * Create a shareable code entry with metadata\n     */\n    public CompletableFuture<ShareableCode> createShareableCode(String code, String title, String description, ShareType shareType) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = new ShareableCode(code, title, description, shareType);\n            shareableCode.setAuthor(getCurrentUser());\n            \n            return processShareableCode(shareableCode);\n        });\n    }\n    \n    private ShareableCode processShareableCode(ShareableCode shareableCode) {\n        // Encrypt code if necessary\n        if (shareableCode.getShareType() == ShareType.LINK || shareableCode.getShareType() == ShareType.QR_CODE) {\n            try {\n                String encryptedCode = encryptCode(shareableCode.getCode());\n                shareableCode.setCode(encryptedCode);\n            } catch (Exception e) {\n                // Handle encryption error\n            }\n        }\n        \n        shareableCodes.put(shareableCode.getId(), shareableCode);\n        saveShareableCode(shareableCode);\n        \n        notifyShareCreated(shareableCode);\n        return shareableCode;\n    }\n    \n    /**\n     * Generate sharing link for code\n     */\n    public CompletableFuture<String> generateShareLink(String shareId) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = shareableCodes.get(shareId);\n            if (shareableCode == null) {\n                return null;\n            }\n            \n            return baseUrl + "/share/" + shareId;\n        });\n    }\n    \n    /**\n     * Generate QR code data for sharing\n     */\n    public CompletableFuture<String> generateQRCodeData(String shareId) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = shareableCodes.get(shareId);\n            if (shareableCode == null) {\n                return null;\n            }\n            \n            String shareLink = baseUrl + "/share/" + shareId;\n            return shareLink;\n        });\n    }\n    \n    /**\n     * Get shareable code by ID\n     */\n    public ShareableCode getShareableCode(String shareId) {\n        return shareableCodes.get(shareId);\n    }\n    \n    /**\n     * Access shared code\n     */\n    public CompletableFuture<String> accessSharedCode(String shareId, String userId, String password) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = shareableCodes.get(shareId);\n            if (shareableCode == null) {\n                throw new IllegalArgumentException("Shared code not found");\n            }\n            \n            // Check permissions\n            if (!shareableCode.canView(userId, password)) {\n                throw new SecurityException("Access denied to shared code");\n            }\n            \n            // Increment view count\n            shareableCode.incrementViewCount();\n            \n            // Decrypt code if necessary\n            String code = shareableCode.getCode();\n            try {\n                if (isEncrypted(shareableCode)) {\n                    code = decryptCode(code);\n                }\n            } catch (Exception e) {\n                throw new SecurityException("Failed to decrypt shared code");\n            }\n            \n            notifyShareAccessed(shareableCode, userId);\n            \n            // Cleanup if max views reached\n            if (!shareableCode.canAccessMore()) {\n                deleteShareableCode(shareId);\n            }\n            \n            return code;\n        });\n    }\n    \n    /**\n     * Delete shareable code\n     */\n    public CompletableFuture<Boolean> deleteShareableCode(String shareId) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = shareableCodes.remove(shareId);\n            if (shareableCode == null) {\n                return false;\n            }\n            \n            deleteShareableCodeFile(shareId);\n            notifyShareDeleted(shareId);\n            \n            return true;\n        });\n    }\n    \n    /**\n     * Get all shareable codes by user\n     */\n    public List<ShareableCode> getShareableCodesByUser(String userId) {\n        return shareableCodes.values().stream()\n                .filter(code -> userId.equals(code.getAuthor()))\n                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))\n                .collect(java.util.stream.Collectors.toList());\n    }\n    \n    /**\n     * Get popular shareable codes\n     */\n    public List<ShareableCode> getPopularShareableCodes(int limit) {\n        return shareableCodes.values().stream()\n                .filter(code -> !code.isExpired() && code.canAccessMore())\n                .sorted((c1, c2) -> Integer.compare(c2.getViewCount(), c1.getViewCount()))\n                .limit(limit)\n                .collect(java.util.stream.Collectors.toList());\n    }\n    \n    /**\n     * Search shareable codes\n     */\n    public List<ShareableCode> searchShareableCodes(String query) {\n        String searchTerm = query.toLowerCase();\n        return shareableCodes.values().stream()\n                .filter(code -> {\n                    return code.getTitle().toLowerCase().contains(searchTerm) ||\n                           code.getDescription().toLowerCase().contains(searchTerm) ||\n                           code.getLanguage().toLowerCase().contains(searchTerm) ||\n                           (code.getAuthor() != null && code.getAuthor().toLowerCase().contains(searchTerm));\n                })\n                .filter(code -> !code.isExpired() && code.canAccessMore())\n                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))\n                .collect(java.util.stream.Collectors.toList());\n    }\n    \n    /**\n     * Get sharing statistics\n     */\n    public Map<String, Object> getSharingStatistics() {\n        Map<String, Object> stats = new HashMap<>();\n        \n        stats.put("total_shares", shareableCodes.size());\n        stats.put("active_shares", (int) shareableCodes.values().stream()\n                .filter(code -> !code.isExpired() && code.canAccessMore())\n                .count());\n        \n        // Count by share type\n        Map<ShareType, Integer> typeCounts = new HashMap<>();\n        for (ShareType type : ShareType.values()) {\n            typeCounts.put(type, 0);\n        }\n        \n        for (ShareableCode code : shareableCodes.values()) {\n            typeCounts.put(code.getShareType(), \n                typeCounts.getOrDefault(code.getShareType(), 0) + 1);\n        }\n        stats.put("shares_by_type", typeCounts);\n        \n        // Total views\n        long totalViews = shareableCodes.values().stream()\n                .mapToInt(ShareableCode::getViewCount)\n                .sum();\n        stats.put("total_views", totalViews);\n        \n        // Most popular language\n        Map<String, Integer> languageCounts = new HashMap<>();\n        for (ShareableCode code : shareableCodes.values()) {\n            languageCounts.put(code.getLanguage(), \n                languageCounts.getOrDefault(code.getLanguage(), 0) + 1);\n        }\n        stats.put("popular_languages", languageCounts);\n        \n        // Recent shares\n        List<ShareableCode> recentShares = shareableCodes.values().stream()\n                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))\n                .limit(5)\n                .collect(java.util.stream.Collectors.toList());\n        stats.put("recent_shares", recentShares);\n        \n        return stats;\n    }\n    \n    /**\n     * Export shareable code as file\n     */\n    public CompletableFuture<String> exportShareableCode(String shareId, String format) {\n        return CompletableFuture.supplyAsync(() -> {\n            ShareableCode shareableCode = shareableCodes.get(shareId);\n            if (shareableCode == null) {\n                return null;\n            }\n            \n            String fileName = shareableCode.getTitle().replaceAll("[^a-zA-Z0-9_-]", "_") + "." + format;\n            \n            try {\n                String content = shareableCode.getCode();\n                if (isEncrypted(shareableCode)) {\n                    content = decryptCode(content);\n                }\n                \n                File exportFile = new File(storagePath, "exports/" + fileName);\n                exportFile.getParentFile().mkdirs();\n                \n                try (FileWriter writer = new FileWriter(exportFile)) {\n                    writer.write("// مشاركة من Python IDE\\n");\n                    writer.write("// المؤلف: " + (shareableCode.getAuthor() != null ? shareableCode.getAuthor() : "غير محدد") + "\\n");\n                    writer.write("// التاريخ: " + shareableCode.getCreatedAt() + "\\n\\n");\n                    writer.write(content);\n                }\n                \n                return exportFile.getAbsolutePath();\n                \n            } catch (Exception e) {\n                return null;\n            }\n        });\n    }\n    \n    // Helper methods\n    private void loadShareableCodes() {\n        // Load shareable codes from storage\n        File storageDir = new File(storagePath);\n        if (!storageDir.exists()) {\n            storageDir.mkdirs();\n        }\n    }\n    \n    private void saveShareableCode(ShareableCode shareableCode) {\n        try {\n            File codeFile = new File(storagePath, "shares/" + shareableCode.getId() + ".json");\n            codeFile.getParentFile().mkdirs();\n            \n            String json = serializeShareableCode(shareableCode);\n            try (FileWriter writer = new FileWriter(codeFile)) {\n                writer.write(json);\n            }\n        } catch (Exception e) {\n            // Handle save error\n        }\n    }\n    \n    private void deleteShareableCodeFile(String shareId) {\n        File codeFile = new File(storagePath, "shares/" + shareId + ".json");\n        if (codeFile.exists()) {\n            codeFile.delete();\n        }\n    }\n    \n    private String encryptCode(String code) throws Exception {\n        // Simplified encryption (in real implementation, use proper encryption)\n        return Base64.getEncoder().encodeToString(code.getBytes());\n    }\n    \n    private String decryptCode(String encryptedCode) throws Exception {\n        // Simplified decryption (in real implementation, use proper decryption)\n        byte[] decodedBytes = Base64.getDecoder().decode(encryptedCode);\n        return new String(decodedBytes);\n    }\n    \n    private boolean isEncrypted(ShareableCode shareableCode) {\n        return shareableCode.getShareType() == ShareType.LINK || \n               shareableCode.getShareType() == ShareType.QR_CODE;\n    }\n    \n    private String serializeShareableCode(ShareableCode shareableCode) {\n        // Simple JSON serialization\n        StringBuilder json = new StringBuilder();\n        json.append("{");\n        json.append("\"id\": \"").append(shareableCode.getId()).append("\",");\n        json.append("\"title\": \"").append(escapeJson(shareableCode.getTitle())).append("\",");\n        json.append("\"description\": \"").append(escapeJson(shareableCode.getDescription())).append("\",");\n        json.append("\"code\": \"").append(escapeJson(shareableCode.getCode())).append("\",");\n        json.append("\"shareType\": \"").append(shareableCode.getShareType()).append("\",");\n        json.append("\"author\": \"").append(escapeJson(shareableCode.getAuthor())).append("\",");\n        json.append("\"createdAt\": \"").append(shareableCode.getCreatedAt()).append("\",");\n        json.append("\"language\": \"").append(shareableCode.getLanguage()).append("\",");\n        json.append("\"viewCount\": ").append(shareableCode.getViewCount());\n        json.append("}");\n        return json.toString();\n    }\n    \n    private String escapeJson(String str) {\n        if (str == null) return "";\n        return str.replace("\\", "\\\\")\n                  .replace("\"", "\\\"")\n                  .replace("\n", "\\n")\n                  .replace("\r", "\\r")\n                  .replace("\t", "\\t");\n    }\n    \n    private String getCurrentUser() {\n        return System.getProperty("user.name");\n    }\n    \n    // Notification methods\n    private void notifyShareCreated(ShareableCode shareableCode) {\n        for (SharingListener listener : listeners) {\n            listener.onShareCreated(shareableCode);\n        }\n    }\n    \n    private void notifyShareAccessed(ShareableCode shareableCode, String userId) {\n        for (SharingListener listener : listeners) {\n            listener.onShareAccessed(shareableCode, userId);\n        }\n    }\n    \n    private void notifyShareExpired(ShareableCode shareableCode) {\n        for (SharingListener listener : listeners) {\n            listener.onShareExpired(shareableCode);\n        }\n    }\n    \n    private void notifyShareDeleted(String shareId) {\n        for (SharingListener listener : listeners) {\n            listener.onShareDeleted(shareId);\n        }\n    }\n    \n    // Public methods\n    public void addSharingListener(SharingListener listener) {\n        listeners.add(listener);\n    }\n    \n    public void setBaseUrl(String baseUrl) {\n        this.baseUrl = baseUrl;\n    }\n    \n    public String getBaseUrl() {\n        return baseUrl;\n    }\n    \n    public int getTotalShares() {\n        return shareableCodes.size();\n    }\n}