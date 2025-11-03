package com.pythonide.files;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pythonide.files.icons.IconProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * FileAdapter - محول البيانات لعرض الملفات في RecyclerView
 * يدعم عرض القائمة والشبكة
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    
    public enum ViewMode {
        LIST, GRID
    }
    
    private Context context;
    private List<FileItem> files;
    private OnFileClickListener clickListener;
    private OnFileLongClickListener longClickListener;
    private ViewMode currentViewMode = ViewMode.LIST;
    private IconProvider iconProvider;
    
    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem, int position);
    }
    
    public interface OnFileLongClickListener {
        void onFileLongClick(FileItem fileItem, int position);
    }
    
    public FileAdapter(Context context, List<FileItem> files, 
                      OnFileClickListener clickListener, 
                      OnFileLongClickListener longClickListener) {
        this.context = context;
        this.files = files;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.iconProvider = new IconProvider(context);
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (currentViewMode == ViewMode.LIST) {
            view = LayoutInflater.from(context).inflate(R.layout.item_file_list, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_file_grid, parent, false);
        }
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = files.get(position);
        holder.bind(fileItem);
    }
    
    @Override
    public int getItemCount() {
        return files.size();
    }
    
    public void setViewMode(ViewMode viewMode) {
        this.currentViewMode = viewMode;
        notifyDataSetChanged();
    }
    
    class FileViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView nameTextView;
        private TextView sizeTextView;
        private TextView dateTextView;
        private View itemView;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            
            if (currentViewMode == ViewMode.LIST) {
                iconImageView = itemView.findViewById(R.id.iconImageView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                sizeTextView = itemView.findViewById(R.id.sizeTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
            } else {
                iconImageView = itemView.findViewById(R.id.gridIconImageView);
                nameTextView = itemView.findViewById(R.id.gridNameTextView);
                sizeTextView = null;
                dateTextView = null;
            }
        }
        
        public void bind(FileItem fileItem) {
            // Set icon
            setFileIcon(iconImageView, fileItem);
            
            // Set name
            nameTextView.setText(fileItem.getName());
            
            // Set size and date for list view
            if (currentViewMode == ViewMode.LIST) {
                if (!fileItem.isDirectory() && !fileItem.isParentLink()) {
                    sizeTextView.setText(formatFileSize(fileItem.getFile().length()));
                    dateTextView.setText(formatDate(fileItem.getFile().lastModified()));
                    sizeTextView.setVisibility(View.VISIBLE);
                    dateTextView.setVisibility(View.VISIBLE);
                } else {
                    sizeTextView.setVisibility(View.GONE);
                    dateTextView.setVisibility(View.GONE);
                }
            }
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFileClick(fileItem, getAdapterPosition());
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onFileLongClick(fileItem, getAdapterPosition());
                    return true;
                }
                return false;
            });
            
            // Set background for selection
            itemView.setSelected(false);
            itemView.setBackgroundColor(Color.TRANSPARENT);
            
            // Set different colors for parent link
            if (fileItem.isParentLink()) {
                nameTextView.setTextColor(Color.GRAY);
                nameTextView.setText(".. "+ context.getString(R.string.parent_directory));
            } else {
                nameTextView.setTextColor(Color.BLACK);
            }
        }
    }
    
    private void setFileIcon(ImageView iconImageView, FileItem fileItem) {
        Drawable icon = iconProvider.getIconForFile(
            fileItem.getName(), 
            fileItem.isDirectory(), 
            fileItem.isParentLink()
        );
        iconImageView.setImageDrawable(icon);
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " بايت";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f كيلوبايت", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f ميغابايت", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f غيغابايت", bytes / (1024.0 * 1024 * 1024));
    }
    
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}