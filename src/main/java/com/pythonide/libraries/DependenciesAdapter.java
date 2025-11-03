package com.pythonide.libraries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pythonide.libraries.LibraryDetailsActivity.DependencyItem;
import java.util.ArrayList;
import java.util.List;

/**
 * DependenciesAdapter - محول لعرض تبعيات المكتبة
 */
public class DependenciesAdapter extends RecyclerView.Adapter<DependenciesAdapter.DependencyViewHolder> {
    
    private List<DependencyItem> dependencies;
    private List<String> installedLibraries;
    
    public DependenciesAdapter(List<DependencyItem> dependencies, List<String> installedLibraries) {
        this.dependencies = new ArrayList<>(dependencies);
        this.installedLibraries = new ArrayList<>(installedLibraries);
    }
    
    @NonNull
    @Override
    public DependencyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dependency, parent, false);
        return new DependencyViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DependencyViewHolder holder, int position) {
        DependencyItem dependency = dependencies.get(position);
        holder.bind(dependency);
    }
    
    @Override
    public int getItemCount() {
        return dependencies.size();
    }
    
    public void updateDependencies(List<DependencyItem> newDependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(newDependencies);
        notifyDataSetChanged();
    }
    
    class DependencyViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView statusTextView;
        private ImageView statusIconView;
        
        public DependencyViewHolder(@NonNull View itemView) {
            super(itemView);
            
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            statusIconView = itemView.findViewById(R.id.statusIconView);
        }
        
        public void bind(DependencyItem dependency) {
            nameTextView.setText(dependency.getName());
            
            if (dependency.isInstalled()) {
                statusTextView.setText("مثبت");
                statusIconView.setImageResource(R.drawable.ic_check_circle);
                statusTextView.setTextColor(itemView.getContext().getColor(R.color.green_600));
            } else {
                statusTextView.setText("غير مثبت");
                statusIconView.setImageResource(R.drawable.ic_info);
                statusTextView.setTextColor(itemView.getContext().getColor(R.color.orange_600));
            }
        }
    }
}