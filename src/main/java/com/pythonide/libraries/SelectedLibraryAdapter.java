package com.pythonide.libraries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * SelectedLibraryAdapter - محول لعرض المكتبات المحددة للتثبيت
 */
public class SelectedLibraryAdapter extends RecyclerView.Adapter<SelectedLibraryAdapter.SelectedViewHolder> {
    
    private List<Library> selectedLibraries;
    private OnRemoveListener removeListener;
    
    public interface OnRemoveListener {
        void onRemove(Library library);
    }
    
    public SelectedLibraryAdapter(List<Library> selectedLibraries, OnRemoveListener removeListener) {
        this.selectedLibraries = selectedLibraries;
        this.removeListener = removeListener;
    }
    
    @NonNull
    @Override
    public SelectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_library, parent, false);
        return new SelectedViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SelectedViewHolder holder, int position) {
        Library library = selectedLibraries.get(position);
        holder.bind(library);
    }
    
    @Override
    public int getItemCount() {
        return selectedLibraries.size();
    }
    
    class SelectedViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView versionTextView;
        private ImageButton removeButton;
        
        public SelectedViewHolder(@NonNull View itemView) {
            super(itemView);
            
            nameTextView = itemView.findViewById(R.id.nameTextView);
            versionTextView = itemView.findViewById(R.id.versionTextView);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
        
        public void bind(Library library) {
            nameTextView.setText(library.getName());
            versionTextView.setText(library.getVersion());
            
            removeButton.setOnClickListener(v -> removeListener.onRemove(library));
        }
    }
}