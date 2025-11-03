package com.pythonide.editor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * محول فهرس الملفات (File Explorer Adapter)
 * يعرض قائمة الملفات في محرر الكود
 */
public class FileExplorerAdapter extends RecyclerView.Adapter<FileExplorerAdapter.FileViewHolder> {
    
    private List<String> files;
    private Context context;
    private OnFileSelectedListener listener;
    private int selectedPosition = -1;
    
    public interface OnFileSelectedListener {
        void onFileSelected(String fileName);
    }
    
    public FileExplorerAdapter(List<String> files, OnFileSelectedListener listener) {
        this.files = files;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_file_explorer, parent, false);
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        String fileName = files.get(position);
        holder.bind(fileName, position);
    }
    
    @Override
    public int getItemCount() {
        return files.size();
    }
    
    public class FileViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView fileIcon;
        private TextView fileNameText;
        private TextView fileTypeText;
        private View container;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileNameText = itemView.findViewById(R.id.file_name_text);
            fileTypeText = itemView.findViewById(R.id.file_type_text);
            container = itemView.findViewById(R.id.container);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    selectedPosition = position;
                    notifyDataSetChanged();
                    
                    if (listener != null) {
                        listener.onFileSelected(files.get(position));
                    }
                }
            });
        }
        
        public void bind(String fileName, int position) {
            fileNameText.setText(fileName);
            
            // Determine file type and set appropriate icon
            String fileType = getFileType(fileName);
            fileTypeText.setText(fileType);
            
            // Set file icon based on file type
            setFileIcon(fileName);
            
            // Set selected state
            if (position == selectedPosition) {
                container.setSelected(true);
                container.setBackgroundColor(context.getResources().getColor(R.color.selected_file_background));
            } else {
                container.setSelected(false);
                container.setBackgroundColor(android.R.color.transparent);
            }
        }
        
        private String getFileType(String fileName) {
            if (fileName.endsWith(".py")) {
                return "Python";
            } else if (fileName.endsWith(".java")) {
                return "Java";
            } else if (fileName.endsWith(".js")) {
                return "JavaScript";
            } else if (fileName.endsWith(".html")) {
                return "HTML";
            } else if (fileName.endsWith(".css")) {
                return "CSS";
            } else if (fileName.endsWith(".xml")) {
                return "XML";
            } else if (fileName.endsWith(".json")) {
                return "JSON";
            } else if (fileName.endsWith(".txt")) {
                return "Text";
            } else if (fileName.endsWith(".md")) {
                return "Markdown";
            } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                return "YAML";
            } else if (fileName.endsWith(".sql")) {
                return "SQL";
            } else {
                return "File";
            }
        }
        
        private void setFileIcon(String fileName) {
            if (fileName.endsWith(".py")) {
                fileIcon.setImageResource(R.drawable.ic_python_file);
            } else if (fileName.endsWith(".java")) {
                fileIcon.setImageResource(R.drawable.ic_java_file);
            } else if (fileName.endsWith(".js")) {
                fileIcon.setImageResource(R.drawable.ic_js_file);
            } else if (fileName.endsWith(".html")) {
                fileIcon.setImageResource(R.drawable.ic_html_file);
            } else if (fileName.endsWith(".css")) {
                fileIcon.setImageResource(R.drawable.ic_css_file);
            } else if (fileName.endsWith(".xml")) {
                fileIcon.setImageResource(R.drawable.ic_xml_file);
            } else if (fileName.endsWith(".json")) {
                fileIcon.setImageResource(R.drawable.ic_json_file);
            } else if (fileName.endsWith(".txt")) {
                fileIcon.setImageResource(R.drawable.ic_text_file);
            } else if (fileName.endsWith(".md")) {
                fileIcon.setImageResource(R.drawable.ic_markdown_file);
            } else {
                fileIcon.setImageResource(R.drawable.ic_file);
            }
        }
    }
    
    // Public methods
    public void updateFiles(List<String> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }
    
    public void addFile(String fileName) {
        files.add(fileName);
        notifyItemInserted(files.size() - 1);
    }
    
    public void removeFile(int position) {
        if (position >= 0 && position < files.size()) {
            files.remove(position);
            notifyItemRemoved(position);
            
            // Adjust selected position
            if (selectedPosition == position) {
                selectedPosition = -1;
            } else if (selectedPosition > position) {
                selectedPosition--;
            }
        }
    }
    
    public void selectFile(int position) {
        if (position >= 0 && position < files.size()) {
            selectedPosition = position;
            notifyDataSetChanged();
        }
    }
    
    public void clearSelection() {
        selectedPosition = -1;
        notifyDataSetChanged();
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    public String getSelectedFile() {
        if (selectedPosition >= 0 && selectedPosition < files.size()) {
            return files.get(selectedPosition);
        }
        return null;
    }
    
    // Filtering methods
    public void filterFiles(String query) {
        if (query.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        
        // This would implement file filtering logic
        // For now, it's a placeholder
    }
}