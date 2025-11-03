package com.pythonide.libraries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * LibrarySearchAdapter - محول لعرض نتائج البحث عن المكتبات
 */
public class LibrarySearchAdapter extends RecyclerView.Adapter<LibrarySearchAdapter.SearchViewHolder> {
    
    private List<Library> searchResults;
    private OnLibrarySelectListener listener;
    
    public interface OnLibrarySelectListener {
        void onLibrarySelect(Library library);
    }
    
    public LibrarySearchAdapter(List<Library> searchResults, OnLibrarySelectListener listener) {
        this.searchResults = searchResults;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        Library library = searchResults.get(position);
        holder.bind(library);
    }
    
    @Override
    public int getItemCount() {
        return searchResults.size();
    }
    
    class SearchViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView versionTextView;
        private TextView descriptionTextView;
        private Button selectButton;
        
        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            
            nameTextView = itemView.findViewById(R.id.nameTextView);
            versionTextView = itemView.findViewById(R.id.versionTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            selectButton = itemView.findViewById(R.id.selectButton);
        }
        
        public void bind(Library library) {
            nameTextView.setText(library.getName());
            versionTextView.setText("الإصدار: " + library.getVersion());
            descriptionTextView.setText(library.getDescription());
            
            selectButton.setOnClickListener(v -> listener.onLibrarySelect(library));
        }
    }
}