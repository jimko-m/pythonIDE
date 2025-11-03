package com.pythonide.libraries;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * LibraryAdapter - محول عرض المكتبات في RecyclerView
 * 
 * المميزات:
 * - عرض جذاب للمعلومات الأساسية
 * - أزرار إجراءات (تثبيت/إلغاء تثبيت)
 * - مؤشرات حالة (مثبت، تحديث متاح، إلخ)
 * - دعم البحث وتمييز النتائج
 * - تخطيط شبكي متجاوب
 */
public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {
    
    private static final String TAG = "LibraryAdapter";
    
    private Context context;
    private List<LibraryItem> libraries;
    private List<LibraryItem> filteredLibraries;
    private OnLibraryClickListener clickListener;
    private OnPackageActionListener actionListener;
    private String searchQuery;
    private NumberFormat numberFormat;
    private SimpleDateFormat dateFormat;
    
    /**
     * واجهة النقر على المكتبة
     */
    public interface OnLibraryClickListener {
        void onLibraryClick(LibraryItem library);
    }
    
    /**
     * واجهة إجراءات المكتبة
     */
    public interface OnPackageActionListener {
        void onInstallClick(LibraryItem library);
        void onUninstallClick(LibraryItem library);
        void onUpdateClick(LibraryItem library);
        void onDetailsClick(LibraryItem library);
    }
    
    /**
     * إنشاء محول المكتبات
     */
    public LibraryAdapter(List<LibraryItem> libraries, OnLibraryClickListener clickListener, 
                         Map<String, LibraryItem> installedLibraries) {
        this.context = null; // سيتم تعيينه لاحقاً
        this.libraries = libraries != null ? libraries : new ArrayList<>();
        this.filteredLibraries = new ArrayList<>(this.libraries);
        this.clickListener = clickListener;
        this.numberFormat = NumberFormat.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // تحديث حالة التثبيت من الخريطة
        updateInstallationStatus(installedLibraries);
    }
    
    /**
     * إنشاء محول المكتبات مع سياق
     */
    public LibraryAdapter(Context context, List<LibraryItem> libraries, 
                         OnLibraryClickListener clickListener) {
        this.context = context;
        this.libraries = libraries != null ? libraries : new ArrayList<>();
        this.filteredLibraries = new ArrayList<>(this.libraries);
        this.clickListener = clickListener;
        this.numberFormat = NumberFormat.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    /**
     * تعيين مستمع النقر
     */
    public void setOnLibraryClickListener(OnLibraryClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * تعيين مستمع الإجراءات
     */
    public void setOnPackageActionListener(OnPackageActionListener listener) {
        this.actionListener = listener;
    }
    
    /**
     * تحديث البيانات
     */
    public void updateData(List<LibraryItem> newLibraries) {
        this.libraries = newLibraries != null ? newLibraries : new ArrayList<>();
        this.filteredLibraries = new ArrayList<>(this.libraries);
        notifyDataSetChanged();
    }
    
    /**
     * تحديث حالة التثبيت
     */
    public void updateInstallationStatus(Map<String, LibraryItem> installedLibraries) {
        if (installedLibraries != null) {
            for (LibraryItem library : libraries) {
                LibraryItem installed = installedLibraries.get(library.getName());
                if (installed != null) {
                    library.setInstalled(true);
                    library.setCurrentVersion(installed.getVersion());
                }
            }
            notifyDataSetChanged();
        }
    }
    
    /**
     * تطبيق البحث
     */
    public void applyFilter(String query) {
        this.searchQuery = query;
        filteredLibraries.clear();
        
        if (query == null || query.isEmpty()) {
            filteredLibraries.addAll(libraries);
        } else {
            String lowerQuery = query.toLowerCase();
            
            for (LibraryItem library : libraries) {
                if (matchesQuery(library, lowerQuery)) {
                    filteredLibraries.add(library);
                }
            }
        }
        
        // ترتيب النتائج
        sortResults(query);
        notifyDataSetChanged();
    }
    
    /**
     * فحص التطابق مع البحث
     */
    private boolean matchesQuery(LibraryItem library, String lowerQuery) {
        return library.getName().toLowerCase().contains(lowerQuery) ||
               library.getDescription().toLowerCase().contains(lowerQuery) ||
               library.getAuthor().toLowerCase().contains(lowerQuery);
    }
    
    /**
     * ترتيب النتائج
     */
    private void sortResults(String query) {
        if (query == null || query.isEmpty()) {
            return;
        }
        
        filteredLibraries.sort((lib1, lib2) -> {
            // تفضيل النتائج المطابقة للاسم
            boolean lib1Matches = lib1.getName().toLowerCase().contains(query.toLowerCase());
            boolean lib2Matches = lib2.getName().toLowerCase().contains(query.toLowerCase());
            
            if (lib1Matches && !lib2Matches) return -1;
            if (!lib1Matches && lib2Matches) return 1;
            
            // ترتيب حسب الشعبية
            return Integer.compare(lib2.getDownloadCount(), lib1.getDownloadCount());
        });
    }
    
    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        
        View view = LayoutInflater.from(context).inflate(R.layout.item_library, parent, false);
        return new LibraryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        LibraryItem library = filteredLibraries.get(position);
        holder.bind(library);
    }
    
    @Override
    public int getItemCount() {
        return filteredLibraries.size();
    }
    
    /**
     * إنشاء نص مميز للبحث
     */
    private SpannableString createHighlightedText(String fullText, String query) {
        if (query == null || query.isEmpty() || fullText == null) {
            return new SpannableString(fullText != null ? fullText : "");
        }
        
        SpannableString spannableString = new SpannableString(fullText);
        String lowerText = fullText.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        int start = lowerText.indexOf(lowerQuery);
        while (start >= 0) {
            int end = start + lowerQuery.length();
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, 
                                  Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = lowerText.indexOf(lowerQuery, end);
        }
        
        return spannableString;
    }
    
    /**
     * فئة ViewHolder للمكتبة
     */
    class LibraryViewHolder extends RecyclerView.ViewHolder {
        
        private MaterialCardView cardView;
        private ImageView imageViewIcon;
        private MaterialTextView textViewName;
        private MaterialTextView textViewVersion;
        private MaterialTextView textViewDescription;
        private MaterialTextView textViewAuthor;
        private MaterialTextView textViewDownloads;
        private MaterialTextView textViewPythonVersions;
        private Chip chipStatus;
        private Chip chipPopularity;
        private LinearLayout layoutDependencies;
        private Button buttonAction;
        private ImageView imageViewOptions;
        
        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardView);
            imageViewIcon = itemView.findViewById(R.id.iconImageView);
            textViewName = itemView.findViewById(R.id.nameTextView);
            textViewVersion = itemView.findViewById(R.id.versionTextView);
            textViewDescription = itemView.findViewById(R.id.descriptionTextView);
            textViewAuthor = itemView.findViewById(R.id.textView_author);
            textViewDownloads = itemView.findViewById(R.id.downloadsTextView);
            textViewPythonVersions = itemView.findViewById(R.id.textView_python_versions);
            chipStatus = itemView.findViewById(R.id.chip_status);
            chipPopularity = itemView.findViewById(R.id.chip_popularity);
            layoutDependencies = itemView.findViewById(R.id.layout_dependencies);
            buttonAction = itemView.findViewById(R.id.actionButton);
            imageViewOptions = itemView.findViewById(R.id.imageView_options);
            
            setupClickListeners();
        }
        
        private void setupClickListeners() {
            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onLibraryClick(filteredLibraries.get(position));
                }
            });
            
            buttonAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LibraryItem library = filteredLibraries.get(position);
                    handleActionClick(library);
                }
            });
            
            imageViewOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsMenu(filteredLibraries.get(position), v);
                }
            });
        }
        
        public void bind(LibraryItem library) {
            // عرض الاسم مع تمييز البحث
            textViewName.setText(searchQuery != null ? 
                createHighlightedText(library.getName(), searchQuery) : 
                library.getName());
            
            // عرض الإصدار
            textViewVersion.setText("v" + library.getVersion());
            
            // عرض الوصف مع تمييز البحث
            textViewDescription.setText(searchQuery != null ?
                createHighlightedText(library.getShortDescription(), searchQuery) :
                library.getShortDescription());
            
            // عرض المؤلف
            textViewAuthor.setText("بواسطة: " + library.getAuthor());
            
            // عرض عدد التحميلات
            String downloadText = numberFormat.format(library.getDownloadCount()) + " تحميل";
            textViewDownloads.setText(downloadText);
            
            // عرض إصدارات Python
            textViewPythonVersions.setText("Python: " + library.getTopPythonVersions());
            
            // إعداد حالة المكتبة
            setupStatusChips(library);
            
            // إعداد التبعيات
            setupDependencies(library);
            
            // إعداد زر الإجراء
            setupActionButton(library);
            
            // إعداد الأيقونة
            setupIcon(library);
            
            // تخصيص الألوان حسب الحالة
            customizeCardAppearance(library);
        }
        
        private void setupStatusChips(LibraryItem library) {
            // حالة التثبيت
            if (library.isInstalled()) {
                chipStatus.setText("مثبت");
                chipStatus.setChipBackgroundColorResource(R.color.chip_installed);
            } else {
                chipStatus.setText("غير مثبت");
                chipStatus.setChipBackgroundColorResource(R.color.chip_not_installed);
            }
            
            // مستوى الشعبية
            chipPopularity.setText(library.getPopularityLevel());
            
            // لون حسب الشعبية
            if (library.isPopular()) {
                chipPopularity.setChipBackgroundColorResource(R.color.chip_popular);
            } else {
                chipPopularity.setChipBackgroundColorResource(R.color.chip_normal);
            }
            
            // إضافة مؤشر للتحديث إذا كان متاحاً
            if (library.hasUpdate() || library.isUpdated()) {
                chipStatus.setText("تحديث متاح");
                chipStatus.setChipBackgroundColorResource(R.color.chip_update_available);
            }
        }
        
        private void setupDependencies(LibraryItem library) {
            layoutDependencies.removeAllViews();
            
            int dependencyCount = library.getDependenciesCount();
            if (dependencyCount > 0) {
                TextView depText = new TextView(context);
                depText.setText("التبعيات: " + library.getTopDependencies());
                depText.setTextSize(12);
                depText.setTextColor(Color.GRAY);
                layoutDependencies.addView(depText);
                
                // إضافة مؤشر للمزيد إذا كان هناك تبعيات إضافية
                if (dependencyCount > 3) {
                    TextView moreText = new TextView(context);
                    moreText.setText("و" + (dependencyCount - 3) + " تبعيات أخرى...");
                    moreText.setTextSize(12);
                    moreText.setTextColor(Color.GRAY);
                    layoutDependencies.addView(moreText);
                }
            }
        }
        
        private void setupActionButton(LibraryItem library) {
            if (library.isInstalled()) {
                if (library.hasUpdate() || library.isUpdated()) {
                    buttonAction.setText("تحديث");
                    buttonAction.setBackgroundResource(R.drawable.button_update);
                } else {
                    buttonAction.setText("إلغاء التثبيت");
                    buttonAction.setBackgroundResource(R.drawable.button_uninstall);
                }
            } else {
                buttonAction.setText("تثبيت");
                buttonAction.setBackgroundResource(R.drawable.button_install);
            }
        }
        
        private void setupIcon(LibraryItem library) {
            // تحديد نوع المكتبة لاختيار الأيقونة المناسبة
            String iconResource = getIconForLibrary(library.getName());
            try {
                int iconRes = context.getResources().getIdentifier(iconResource, "drawable", context.getPackageName());
                if (iconRes != 0) {
                    imageViewIcon.setImageResource(iconRes);
                } else {
                    imageViewIcon.setImageResource(R.drawable.ic_library_default);
                }
            } catch (Exception e) {
                imageViewIcon.setImageResource(R.drawable.ic_library_default);
            }
            
            // إضافة إطار للمكتبات المثبتة
            if (library.isInstalled()) {
                imageViewIcon.setColorFilter(null);
            } else {
                imageViewIcon.setColorFilter(Color.GRAY);
            }
        }
        
        private void customizeCardAppearance(LibraryItem library) {
            int cardColor;
            
            if (library.hasUpdate() || library.isUpdated()) {
                cardColor = Color.parseColor("#FFF3E0"); // برتقالي فاتح
            } else if (library.isInstalled()) {
                cardColor = Color.parseColor("#E8F5E8"); // أخضر فاتح
            } else {
                cardColor = Color.WHITE; // أبيض
            }
            
            cardView.setCardBackgroundColor(cardColor);
        }
        
        private void handleActionClick(LibraryItem library) {
            if (actionListener != null) {
                if (library.isInstalled()) {
                    if (library.hasUpdate() || library.isUpdated()) {
                        actionListener.onUpdateClick(library);
                    } else {
                        actionListener.onUninstallClick(library);
                    }
                } else {
                    actionListener.onInstallClick(library);
                }
            }
        }
        
        private void showOptionsMenu(LibraryItem library, View anchorView) {
            // إنشاء قائمة خيارات
            PopupMenu popupMenu = new PopupMenu(context, anchorView);
            popupMenu.getMenuInflater().inflate(R.menu.library_options_menu, popupMenu.getMenu());
            
            // إخفاء/إظهار الخيارات حسب حالة المكتبة
            if (!library.isInstalled()) {
                popupMenu.getMenu().findItem(R.id.action_uninstall).setVisible(false);
                popupMenu.getMenu().findItem(R.id.action_update).setVisible(false);
            } else if (!library.hasUpdate() && !library.isUpdated()) {
                popupMenu.getMenu().findItem(R.id.action_update).setVisible(false);
            }
            
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.action_details) {
                    if (clickListener != null) {
                        clickListener.onLibraryClick(library);
                    }
                    return true;
                } else if (itemId == R.id.action_install) {
                    if (actionListener != null) {
                        actionListener.onInstallClick(library);
                    }
                    return true;
                } else if (itemId == R.id.action_uninstall) {
                    if (actionListener != null) {
                        actionListener.onUninstallClick(library);
                    }
                    return true;
                } else if (itemId == R.id.action_update) {
                    if (actionListener != null) {
                        actionListener.onUpdateClick(library);
                    }
                    return true;
                }
                
                return false;
            });
            
            popupMenu.show();
        }
        
        private String getIconForLibrary(String libraryName) {
            String lowerName = libraryName.toLowerCase();
            
            if (lowerName.contains("numpy") || lowerName.contains("scipy") || 
                lowerName.contains("pandas") || lowerName.contains("matplotlib")) {
                return "ic_science";
            } else if (lowerName.contains("flask") || lowerName.contains("django") || 
                      lowerName.contains("fastapi")) {
                return "ic_web";
            } else if (lowerName.contains("requests") || lowerName.contains("httpx")) {
                return "ic_network";
            } else if (lowerName.contains("tensorflow") || lowerName.contains("torch") || 
                      lowerName.contains("sklearn")) {
                return "ic_ai";
            } else if (lowerName.contains("pillow") || lowerName.contains("opencv")) {
                return "ic_image";
            } else if (lowerName.contains("sqlalchemy") || lowerName.contains("pymongo")) {
                return "ic_database";
            }
            
            return "ic_library_default";
        }
    }
}