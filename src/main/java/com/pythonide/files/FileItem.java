package com.pythonide.files;

import java.io.File;
import java.io.Serializable;

/**
 * FileItem - عنصر ملف واحد في مدير الملفات
 */
public class FileItem implements Serializable {
    
    private File file;
    private boolean isParentLink;
    private long size;
    private long lastModified;
    
    public FileItem(File file, boolean isParentLink) {
        this.file = file;
        this.isParentLink = isParentLink;
        
        if (file != null) {
            this.size = file.length();
            this.lastModified = file.lastModified();
        }
    }
    
    public File getFile() {
        return file;
    }
    
    public String getName() {
        if (file == null) return "";
        if (isParentLink) {
            return file.getName();
        }
        return file.getName();
    }
    
    public String getPath() {
        if (file == null) return "";
        return file.getAbsolutePath();
    }
    
    public String getExtension() {
        if (file == null) return "";
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    public boolean isDirectory() {
        return file != null && file.isDirectory();
    }
    
    public boolean isParentLink() {
        return isParentLink;
    }
    
    public boolean isFile() {
        return file != null && file.isFile();
    }
    
    public long getSize() {
        return size;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public String getFormattedSize() {
        return formatFileSize(size);
    }
    
    public String getFormattedDate() {
        return formatDate(lastModified);
    }
    
    public boolean canRead() {
        return file != null && file.canRead();
    }
    
    public boolean canWrite() {
        return file != null && file.canWrite();
    }
    
    public boolean canExecute() {
        return file != null && file.canExecute();
    }
    
    public boolean isHidden() {
        return file != null && file.isHidden();
    }
    
    public boolean isImage() {
        String ext = getExtension();
        return ext.equals("jpg") || ext.equals("jpeg") || 
               ext.equals("png") || ext.equals("gif") || 
               ext.equals("bmp") || ext.equals("webp");
    }
    
    public boolean isVideo() {
        String ext = getExtension();
        return ext.equals("mp4") || ext.equals("avi") || 
               ext.equals("mkv") || ext.equals("mov") || 
               ext.equals("wmv") || ext.equals("flv");
    }
    
    public boolean isAudio() {
        String ext = getExtension();
        return ext.equals("mp3") || ext.equals("wav") || 
               ext.equals("flac") || ext.equals("aac") || 
               ext.equals("ogg") || ext.equals("wma");
    }
    
    public boolean isText() {
        String ext = getExtension();
        return ext.equals("txt") || ext.equals("log") || 
               ext.equals("md") || ext.equals("rtf") || 
               ext.equals("csv") || ext.equals("xml") || 
               ext.equals("json") || ext.equals("html") || 
               ext.equals("htm") || ext.equals("css") || 
               ext.equals("js") || ext.equals("java") || 
               ext.equals("py") || ext.equals("cpp") || 
               ext.equals("c") || ext.equals("php") || 
               ext.equals("sql") || ext.equals("ini") || 
               ext.equals("conf") || ext.equals("config");
    }
    
    public boolean isCompressed() {
        String ext = getExtension();
        return ext.equals("zip") || ext.equals("rar") || 
               ext.equals("7z") || ext.equals("tar") || 
               ext.equals("gz") || ext.equals("bz2") || 
               ext.equals("xz") || ext.equals("lz");
    }
    
    public boolean isPdf() {
        return getExtension().equals("pdf");
    }
    
    public String getMimeType() {
        if (isDirectory()) return "application/vnd.android.package-archive";
        
        String ext = getExtension();
        switch (ext) {
            case "txt":
            case "log":
            case "md":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "csv":
                return "text/csv";
            case "rtf":
                return "application/rtf";
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            case "webp":
                return "image/webp";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "flac":
                return "audio/flac";
            case "aac":
                return "audio/aac";
            case "ogg":
                return "audio/ogg";
            case "wma":
                return "audio/x-ms-wma";
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mkv":
                return "video/x-matroska";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/vnd.rar";
            case "7z":
                return "application/x-7z-compressed";
            case "tar":
                return "application/x-tar";
            case "gz":
                return "application/gzip";
            case "java":
                return "text/x-java-source";
            case "py":
                return "text/x-python";
            case "cpp":
            case "cxx":
                return "text/x-c++src";
            case "c":
                return "text/x-csrc";
            case "php":
                return "text/x-php";
            case "sql":
                return "application/sql";
            case "js":
                return "application/javascript";
            default:
                return "application/octet-stream";
        }
    }
    
    public String getDisplayName() {
        String name = getName();
        if (name.isEmpty()) {
            return file.getAbsolutePath();
        }
        return name;
    }
    
    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        return getName().toLowerCase().contains(lowerQuery) ||
               getPath().toLowerCase().contains(lowerQuery) ||
               getExtension().toLowerCase().contains(lowerQuery);
    }
    
    @Override
    public String toString() {
        return "FileItem{" +
                "name='" + getName() + '\'' +
                ", path='" + getPath() + '\'' +
                ", isDirectory=" + isDirectory() +
                ", isParentLink=" + isParentLink +
                ", size=" + size +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FileItem fileItem = (FileItem) obj;
        return file != null && fileItem.file != null &&
               file.getAbsolutePath().equals(fileItem.file.getAbsolutePath());
    }
    
    @Override
    public int hashCode() {
        return file != null ? file.getAbsolutePath().hashCode() : 0;
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " بايت";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f كيلوبايت", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f ميغابايت", bytes / (1024.0 * 1024));
        return String.format(java.util.Locale.getDefault(), "%.1f غيغابايت", bytes / (1024.0 * 1024 * 1024));
    }
    
    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}