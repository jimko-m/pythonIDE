package com.pythonide.files.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * IconProvider - مزود الأيقونات لنظام إدارة الملفات
 * ينشئ أيقونات ديناميكية للملفات والمجلدات
 */
public class IconProvider {
    
    private static final int ICON_SIZE = 64;
    private static final int TEXT_SIZE = 12;
    
    private Context context;
    private Paint textPaint;
    private Paint iconPaint;
    
    public IconProvider(Context context) {
        this.context = context;
        initPaints();
    }
    
    private void initPaints() {
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        
        iconPaint = new Paint();
        iconPaint.setAntiAlias(true);
    }
    
    /**
     * إنشاء أيقونة للمجلد
     */
    public Drawable createFolderIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية المجلد
        iconPaint.setColor(Color.parseColor("#FFC107"));
        canvas.drawRect(8, 16, ICON_SIZE-8, ICON_SIZE-8, iconPaint);
        
        // جزء المجلد العلوي
        iconPaint.setColor(Color.parseColor("#FFD54F"));
        canvas.drawRect(8, 16, 24, 24, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للصور
     */
    public Drawable createImageIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية الصورة
        iconPaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, iconPaint);
        
        // مستطيل الصورة
        iconPaint.setColor(Color.WHITE);
        canvas.drawRect(16, 16, ICON_SIZE-16, ICON_SIZE-24, iconPaint);
        
        // نقطة الصورة
        canvas.drawCircle(ICON_SIZE/2, ICON_SIZE/2, 8, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للفيديوهات
     */
    public Drawable createVideoIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية الفيديو
        iconPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRoundRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, 8, 8, iconPaint);
        
        // مثلث الفيديو
        iconPaint.setColor(Color.WHITE);
        int centerX = ICON_SIZE / 2;
        int centerY = ICON_SIZE / 2;
        canvas.drawCircle(centerX, centerY, 12, iconPaint);
        
        iconPaint.setColor(Color.parseColor("#2196F3"));
        float[] points = {
            centerX - 6, centerY - 8,
            centerX - 6, centerY + 8,
            centerX + 10, centerY
        };
        canvas.drawPolygon(points, 3, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للصوتيات
     */
    public Drawable createAudioIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية الصوت
        iconPaint.setColor(Color.parseColor("#9C27B0"));
        canvas.drawCircle(ICON_SIZE/2, ICON_SIZE/2, 24, iconPaint);
        
        // موجة الصوت
        iconPaint.setColor(Color.WHITE);
        int centerX = ICON_SIZE / 2;
        int centerY = ICON_SIZE / 2;
        
        for (int i = 0; i < 5; i++) {
            float amplitude = 4 + i * 2;
            canvas.drawLine(centerX - 12 + i * 4, centerY - amplitude,
                          centerX - 12 + i * 4, centerY + amplitude, iconPaint);
        }
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للنصوص
     */
    public Drawable createTextIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية النص
        iconPaint.setColor(Color.parseColor("#607D8B"));
        canvas.drawRoundRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, 4, 4, iconPaint);
        
        // خطوط النص
        iconPaint.setColor(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            canvas.drawRoundRect(16, 16 + i * 8, ICON_SIZE-16, 16 + i * 8 + 4, 2, 2, iconPaint);
        }
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة لملفات PDF
     */
    public Drawable createPdfIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية PDF
        iconPaint.setColor(Color.parseColor("#F44336"));
        canvas.drawRoundRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, 4, 4, iconPaint);
        
        // حرف PDF
        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextSize(16);
        canvas.drawText("PDF", ICON_SIZE/2 - 12, ICON_SIZE/2 + 6, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للأرشيف
     */
    public Drawable createArchiveIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية الأرشيف
        iconPaint.setColor(Color.parseColor("#FF9800"));
        canvas.drawRoundRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, 4, 4, iconPaint);
        
        // مثلث الأرشيف
        iconPaint.setColor(Color.WHITE);
        float[] points = {
            ICON_SIZE/2, 12,
            ICON_SIZE/2 - 8, 28,
            ICON_SIZE/2 + 8, 28
        };
        canvas.drawPolygon(points, 3, iconPaint);
        
        // خط أفقي
        canvas.drawRoundRect(ICON_SIZE/2 - 16, 32, ICON_SIZE/2 + 16, 36, 2, 2, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للملفات العامة
     */
    public Drawable createFileIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية الملف
        iconPaint.setColor(Color.parseColor("#9E9E9E"));
        canvas.drawRoundRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, 4, 4, iconPaint);
        
        // طية الملف
        iconPaint.setColor(Color.parseColor("#BDBDBD"));
        canvas.drawRoundRect(ICON_SIZE-20, 8, ICON_SIZE-8, 20, 4, 4, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة لملفات الكود
     */
    public Drawable createCodeIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية الكود
        iconPaint.setColor(Color.parseColor("#3F51B5"));
        canvas.drawRoundRect(8, 8, ICON_SIZE-8, ICON_SIZE-8, 4, 4, iconPaint);
        
        // رموز الكود
        iconPaint.setColor(Color.WHITE);
        canvas.drawText("&lt;/&gt;", ICON_SIZE/2 - 16, ICON_SIZE/2 + 6, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة للمجلد الأب (العودة)
     */
    public Drawable createParentIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية المجلد الأب
        iconPaint.setColor(Color.parseColor("#9E9E9E"));
        canvas.drawCircle(ICON_SIZE/2, ICON_SIZE/2, 24, iconPaint);
        
        // سهم للأعلى
        iconPaint.setColor(Color.WHITE);
        float[] points = {
            ICON_SIZE/2, 16,
            ICON_SIZE/2 - 8, 28,
            ICON_SIZE/2 + 8, 28
        };
        canvas.drawPolygon(points, 3, iconPaint);
        
        // خط عمودي
        canvas.drawLine(ICON_SIZE/2, 28, ICON_SIZE/2, 36, iconPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * إنشاء أيقونة فارغة للمجلدات
     */
    public Drawable createEmptyFolderIcon() {
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية المجلد الفارغ
        iconPaint.setColor(Color.parseColor("#BDBDBD"));
        canvas.drawRect(8, 16, ICON_SIZE-8, ICON_SIZE-8, iconPaint);
        
        // جزء المجلد العلوي
        iconPaint.setColor(Color.parseColor("#E0E0E0"));
        canvas.drawRect(8, 16, 24, 24, iconPaint);
        
        // علامة الفارغ (خطوط أفقية)
        iconPaint.setColor(Color.parseColor("#757575"));
        for (int i = 0; i < 3; i++) {
            canvas.drawLine(16, 32 + i * 6, ICON_SIZE-16, 32 + i * 6, iconPaint);
        }
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * الحصول على أيقونة حسب نوع الملف
     */
    public Drawable getIconForFile(String fileName, boolean isDirectory, boolean isParentLink) {
        if (isParentLink) {
            return createParentIcon();
        }
        
        if (isDirectory) {
            return createFolderIcon();
        }
        
        String extension = getFileExtension(fileName.toLowerCase());
        
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return createImageIcon();
                
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
            case "wmv":
            case "flv":
                return createVideoIcon();
                
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
            case "ogg":
            case "wma":
                return createAudioIcon();
                
            case "txt":
            case "log":
            case "md":
            case "rtf":
            case "csv":
                return createTextIcon();
                
            case "pdf":
                return createPdfIcon();
                
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
            case "bz2":
                return createArchiveIcon();
                
            case "html":
            case "htm":
            case "xml":
            case "json":
            case "css":
            case "js":
            case "java":
            case "py":
            case "cpp":
            case "c":
            case "php":
            case "sql":
                return createCodeIcon();
                
            default:
                return createFileIcon();
        }
    }
    
    /**
     * الحصول على امتداد الملف
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }
}