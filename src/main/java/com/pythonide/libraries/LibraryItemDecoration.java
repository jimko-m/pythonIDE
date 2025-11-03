package com.pythonide.libraries;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/**
 * LibraryItemDecoration - تحسينات بصرية لعناصر المكتبة
 * 
 * يوفر مسافات وتنسيقات مخصصة لعرض المكتبات في RecyclerView
 */
public class LibraryItemDecoration extends RecyclerView.ItemDecoration {
    
    private final int spacing;
    private final int verticalSpacing;
    
    /**
     * إنشاء تزيين بمسافات متساوية
     */
    public LibraryItemDecoration(int spacing) {
        this(spacing, spacing);
    }
    
    /**
     * إنشاء تزيين بمسافات مختلفة أفقية ورأسية
     */
    public LibraryItemDecoration(int horizontalSpacing, int verticalSpacing) {
        this.spacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }
    
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter().getItemCount();
        
        if (itemCount <= 0) return;
        
        // إزالة المسافة الإضافية من آخر صف
        boolean isLastRow = isLastRow(position, itemCount, parent);
        
        // تطبيق المسافات
        outRect.left = spacing / 2;
        outRect.right = spacing / 2;
        outRect.top = isLastRow ? 0 : verticalSpacing / 2;
        outRect.bottom = isLastRow ? 0 : verticalSpacing / 2;
        
        // إضافة مساحة إضافية للعناصر في الصف الأول
        if (position < getColumnsCount(parent)) {
            outRect.top += verticalSpacing / 2;
        }
    }
    
    /**
     * فحص إذا كان العنصر في آخر صف
     */
    private boolean isLastRow(int position, int itemCount, RecyclerView parent) {
        int columns = getColumnsCount(parent);
        int rows = (itemCount + columns - 1) / columns;
        int currentRow = position / columns;
        
        return currentRow == rows - 1;
    }
    
    /**
     * الحصول على عدد الأعمدة
     */
    private int getColumnsCount(RecyclerView parent) {
        return parent.getLayoutManager() instanceof androidx.recyclerview.widget.GridLayoutManager ? 
               ((androidx.recyclerview.widget.GridLayoutManager) parent.getLayoutManager()).getSpanCount() : 1;
    }
}