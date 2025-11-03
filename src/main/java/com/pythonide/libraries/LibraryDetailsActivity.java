package com.pythonide.libraries;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * LibraryDetailsActivity - عرض تفاصيل مكتبة Python
 * 
 * يوفر عرض شامل ومفصل لمكتبة Python مع:
 * - معلومات أساسية وإحصائيات
 * - تفاصيل التبعيات
 * - إصدارات Python المدعومة
 * - معلومات الرخصة والمؤلف
 * - روابط خارجية ومراجع
 */
public class LibraryDetailsActivity extends AppCompatActivity {
    
    private static final String TAG = "LibraryDetailsActivity";
    
    // معلومات المكتبة
    private LibraryItem library;
    private Map<String, List<String>> dependencyGraph;
    private List<String> installedLibraries;
    
    // عناصر واجهة المستخدم
    private MaterialTextView textViewName;
    private MaterialTextView textViewVersion;
    private MaterialTextView textViewDescription;
    private MaterialTextView textViewAuthor;
    private MaterialTextView textViewLicense;
    private MaterialTextView textViewDownloads;
    private MaterialTextView textViewLastUpdated;
    private MaterialTextView textViewHomePage;
    private MaterialTextView textViewPyPIUrl;
    private MaterialTextView textViewPythonVersions;
    private MaterialTextView textViewDependencies;
    
    private ImageView imageViewIcon;
    private Chip chipStatus;
    private Chip chipPopularity;
    private Chip chipCompatibility;
    
    private LinearLayout layoutDependencies;
    private LinearLayout layoutPythonVersions;
    private LinearLayout layoutLinks;
    
    private RecyclerView recyclerViewDependencies;
    private DependencyAdapter dependencyAdapter;
    
    private FloatingActionButton fabAction;
    
    // تنسيقات
    private NumberFormat numberFormat;
    private SimpleDateFormat dateFormat;
    private SharedPreferences preferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_details);
        
        initializeFormats();
        loadLibraryData();
        setupUI();
        setupToolbar();
        setupDependenciesRecyclerView();
        populateLibraryInfo();
        setupClickListeners();
    }
    
    /**
     * تهيئة التنسيقات
     */
    private void initializeFormats() {
        numberFormat = NumberFormat.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        preferences = getSharedPreferences("library_preferences", MODE_PRIVATE);
    }
    
    /**
     * تحميل بيانات المكتبة
     */
    private void loadLibraryData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("library")) {
            library = intent.getParcelableExtra("library");
        }
        
        if (intent != null && intent.hasExtra("dependency_graph")) {
            dependencyGraph = (Map<String, List<String>>) intent.getSerializableExtra("dependency_graph");
        }
        
        if (intent != null && intent.hasStringArrayListExtra("installed_libraries")) {
            installedLibraries = intent.getStringArrayListExtra("installed_libraries");
        }
    }
    
    /**
     * إعداد واجهة المستخدم
     */
    private void setupUI() {
        // العثور على العناصر
        imageViewIcon = findViewById(R.id.imageView_icon);
        textViewName = findViewById(R.id.textView_name);
        textViewVersion = findViewById(R.id.textView_version);
        textViewDescription = findViewById(R.id.textView_description);
        textViewAuthor = findViewById(R.id.textView_author);
        textViewLicense = findViewById(R.id.textView_license);
        textViewDownloads = findViewById(R.id.textView_downloads);
        textViewLastUpdated = findViewById(R.id.textView_last_updated);
        textViewHomePage = findViewById(R.id.textView_home_page);
        textViewPyPIUrl = findViewById(R.id.textView_pypi_url);
        textViewPythonVersions = findViewById(R.id.textView_python_versions);
        textViewDependencies = findViewById(R.id.textView_dependencies);
        
        chipStatus = findViewById(R.id.chip_status);
        chipPopularity = findViewById(R.id.chip_popularity);
        chipCompatibility = findViewById(R.id.chip_compatibility);
        
        layoutDependencies = findViewById(R.id.layout_dependencies);
        layoutPythonVersions = findViewById(R.id.layout_python_versions);
        layoutLinks = findViewById(R.id.layout_links);
        
        recyclerViewDependencies = findViewById(R.id.recyclerView_dependencies);
        
        fabAction = findViewById(R.id.fab_action);
    }
    
    /**
     * إعداد شريط الأدوات
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("تفاصيل المكتبة");
        }
    }
    
    /**
     * إعداد قائمة التبعيات
     */
    private void setupDependenciesRecyclerView() {
        dependencyAdapter = new DependencyAdapter(new ArrayList<>());
        recyclerViewDependencies.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDependencies.setAdapter(dependencyAdapter);
    }
    
    /**
     * ملء معلومات المكتبة
     */
    private void populateLibraryInfo() {
        if (library == null) {
            finish();
            return;
        }
        
        // المعلومات الأساسية
        textViewName.setText(library.getName());
        textViewVersion.setText("الإصدار: " + library.getVersion());
        textViewDescription.setText(library.getDescription());
        textViewAuthor.setText("المؤلف: " + library.getAuthor());
        textViewLicense.setText("الرخصة: " + library.getLicense());
        
        // الإحصائيات
        textViewDownloads.setText(numberFormat.format(library.getDownloadCount()) + " تحميل");
        textViewLastUpdated.setText("آخر تحديث: " + formatDate(library.getLastUpdated()));
        
        // الروابط
        setupLinks();
        
        // إصدارات Python
        setupPythonVersions();
        
        // التبعيات
        setupDependencies();
        
        // الأيقونة والحالة
        setupIconAndStatus();
        
        // زر الإجراء
        setupActionButton();
    }
    
    /**
     * إعداد الروابط
     */
    private void setupLinks() {
        layoutLinks.removeAllViews();
        
        if (library.getHomePage() != null && !library.getHomePage().isEmpty()) {
            addLinkItem("الموقع الرسمي", library.getHomePage());
        }
        
        if (library.getPyPIUrl() != null && !library.getPyPIUrl().isEmpty()) {
            addLinkItem("PyPI", library.getPyPIUrl());
        }
        
        if (installedLibraries != null && installedLibraries.contains(library.getName().toLowerCase())) {
            addLinkItem("المسار المحلي", getLibraryPath(library.getName()));
        }
    }
    
    /**
     * إضافة عنصر رابط
     */
    private void addLinkItem(String title, String url) {
        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setCardElevation(2f);
        cardView.setRadius(8f);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setPadding(16, 12, 16, 12);
        
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_link);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        MaterialTextView titleView = new MaterialTextView(this);
        titleView.setText(title);
        titleView.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        ImageView arrowView = new ImageView(this);
        arrowView.setImageResource(R.drawable.ic_arrow_forward);
        arrowView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        contentLayout.addView(iconView);
        contentLayout.addView(titleView);
        contentLayout.addView(arrowView);
        
        cardView.addView(contentLayout);
        
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
        
        layoutLinks.addView(cardView);
    }
    
    /**
     * إعداد إصدارات Python
     */
    private void setupPythonVersions() {
        layoutPythonVersions.removeAllViews();
        
        List<String> versions = library.getPythonVersions();
        if (versions != null && !versions.isEmpty()) {
            for (String version : versions) {
                Chip chip = new Chip(this);
                chip.setText("Python " + version);
                chip.setCheckable(false);
                
                // تحديد لون الشريحة حسب التوافق
                if (isCurrentPythonVersion(version)) {
                    chip.setChipBackgroundColorResource(R.color.chip_compatible);
                    chip.setTextColor(getColor(R.color.white));
                } else {
                    chip.setChipBackgroundColorResource(R.color.chip_normal);
                }
                
                layoutPythonVersions.addView(chip);
            }
        }
    }
    
    /**
     * إعداد التبعيات
     */
    private void setupDependencies() {
        List<String> dependencies = library.getDependencies();
        
        if (dependencies != null && !dependencies.isEmpty()) {
            textViewDependencies.setText(dependencies.size() + " تبعية");
            
            // عرض التبعيات في RecyclerView
            List<DependencyItem> dependencyItems = new ArrayList<>();
            for (String dep : dependencies) {
                DependencyItem item = new DependencyItem(dep, isDependencyInstalled(dep));
                dependencyItems.add(item);
            }
            
            dependencyAdapter.updateDependencies(dependencyItems);
            layoutDependencies.setVisibility(View.VISIBLE);
        } else {
            textViewDependencies.setText("لا توجد تبعيات");
            layoutDependencies.setVisibility(View.GONE);
        }
    }
    
    /**
     * إعداد الأيقونة والحالة
     */
    private void setupIconAndStatus() {
        // إعداد أيقونة
        String iconResource = getIconForLibrary(library.getName());
        try {
            int iconRes = getResources().getIdentifier(iconResource, "drawable", getPackageName());
            if (iconRes != 0) {
                imageViewIcon.setImageResource(iconRes);
            } else {
                imageViewIcon.setImageResource(R.drawable.ic_library_default);
            }
        } catch (Exception e) {
            imageViewIcon.setImageResource(R.drawable.ic_library_default);
        }
        
        // إعداد الشرائح
        setupStatusChips();
    }
    
    /**
     * إعداد شرائح الحالة
     */
    private void setupStatusChips() {
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
        if (library.isPopular()) {
            chipPopularity.setChipBackgroundColorResource(R.color.chip_popular);
        } else {
            chipPopularity.setChipBackgroundColorResource(R.color.chip_normal);
        }
        
        // نقاط التوافق
        double compatibilityScore = library.getCompatibilityScore();
        String compatibilityText = String.format("توافق: %.1f/5.0", compatibilityScore);
        chipCompatibility.setText(compatibilityText);
        
        if (compatibilityScore >= 4.0) {
            chipCompatibility.setChipBackgroundColorResource(R.color.chip_compatible);
        } else if (compatibilityScore >= 3.0) {
            chipCompatibility.setChipBackgroundColorResource(R.color.chip_mostly_compatible);
        } else {
            chipCompatibility.setChipBackgroundColorResource(R.color.chip_partially_compatible);
        }
    }
    
    /**
     * إعداد زر الإجراء
     */
    private void setupActionButton() {
        if (library.isInstalled()) {
            if (library.hasUpdate() || library.isUpdated()) {
                fabAction.setImageResource(R.drawable.ic_update);
                fabAction.setContentDescription("تحديث المكتبة");
            } else {
                fabAction.setImageResource(R.drawable.ic_uninstall);
                fabAction.setContentDescription("إلغاء تثبيت المكتبة");
            }
        } else {
            fabAction.setImageResource(R.drawable.ic_install);
            fabAction.setContentDescription("تثبيت المكتبة");
        }
    }
    
    /**
     * إعداد مستمعي الأحداث
     */
    private void setupClickListeners() {
        fabAction.setOnClickListener(v -> {
            if (library.isInstalled()) {
                if (library.hasUpdate() || library.isUpdated()) {
                    performUpdate();
                } else {
                    performUninstall();
                }
            } else {
                performInstall();
            }
        });
    }
    
    /**
     * تنفيذ التثبيت
     */
    private void performInstall() {
        // بدء النشاط مع خيارات التثبيت
        Intent intent = new Intent(this, InstallLibrariesActivity.class);
        InstallOptions options = new InstallOptions(library.getName(), null, true, true, true, null);
        intent.putExtra("install_options", options);
        startActivityForResult(intent, 1001);
    }
    
    /**
     * تنفيذ الإلغاء
     */
    private void performUninstall() {
        // تنفيذ إلغاء التثبيت
        PackageManager packageManager = new PackageManager(this, null);
        boolean success = packageManager.uninstallPackage(library.getName());
        
        if (success) {
            library.setInstalled(false);
            finish();
        } else {
            Toast.makeText(this, "فشل في إلغاء التثبيت", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تنفيذ التحديث
     */
    private void performUpdate() {
        // تنفيذ التحديث
        PackageManager packageManager = new PackageManager(this, null);
        packageManager.upgradePackage(library.getName(), (packageName, progress, step) -> {
            runOnUiThread(() -> {
                // تحديث واجهة المستخدم مع تقدم التحديث
            });
        });
    }
    
    /**
     * فحص إصدار Python الحالي
     */
    private boolean isCurrentPythonVersion(String version) {
        // هنا يمكن فحص إصدار Python المثبت على النظام
        // للتبسيط، نفترض Python 3.9
        return version.equals("3.9") || version.equals("3.8") || version.equals("3.10");
    }
    
    /**
     * فحص تثبيت التبعية
     */
    private boolean isDependencyInstalled(String dependency) {
        String cleanDep = parseDependencyName(dependency);
        return installedLibraries != null && installedLibraries.contains(cleanDep.toLowerCase());
    }
    
    /**
     * تحليل اسم التبعية
     */
    private String parseDependencyName(String dependency) {
        if (dependency == null || dependency.isEmpty()) return "";
        
        // إزالة شروط الإصدار
        String cleanDep = dependency.replaceAll("[<>=!~].*", "").trim();
        cleanDep = cleanDep.replaceAll("[\\[\\]\\(\\)]", "").trim();
        
        // الحصول على الاسم الأساسي
        String[] parts = cleanDep.split("[\\s\\-:]+");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        
        return cleanDep;
    }
    
    /**
     * الحصول على مسار المكتبة
     */
    private String getLibraryPath(String libraryName) {
        // مسار افتراضي للمكتبات المثبتة
        return "/usr/lib/python3.9/site-packages/" + libraryName;
    }
    
    /**
     * تنسيق التاريخ
     */
    private String formatDate(Date date) {
        if (date == null) return "غير محدد";
        
        try {
            return dateFormat.format(date);
        } catch (Exception e) {
            return "غير محدد";
        }
    }
    
    /**
     * الحصول على أيقونة للمكتبة
     */
    private String getIconForLibrary(String libraryName) {
        String lowerName = libraryName.toLowerCase();
        
        if (lowerName.contains("numpy") || lowerName.contains("scipy") || 
            lowerName.contains("pandas") || lowerName.contains("matplotlib")) {
            return "ic_science";
        } else if (lowerName.contains("flask") || lowerName.contains("django")) {
            return "ic_web";
        } else if (lowerName.contains("requests") || lowerName.contains("httpx")) {
            return "ic_network";
        } else if (lowerName.contains("tensorflow") || lowerName.contains("torch")) {
            return "ic_ai";
        } else if (lowerName.contains("pillow") || lowerName.contains("opencv")) {
            return "ic_image";
        } else if (lowerName.contains("sqlalchemy") || lowerName.contains("pymongo")) {
            return "ic_database";
        }
        
        return "ic_library_default";
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library_details, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_share) {
            shareLibraryInfo();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshLibraryInfo();
            return true;
        } else if (id == R.id.action_add_to_favorites) {
            addToFavorites();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * مشاركة معلومات المكتبة
     */
    private void shareLibraryInfo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
            library.getName() + " v" + library.getVersion() + "\n" + 
            library.getDescription() + "\n" + 
            "PyPI: " + library.getPyPIUrl());
        startActivity(Intent.createChooser(shareIntent, "مشاركة المكتبة"));
    }
    
    /**
     * تحديث معلومات المكتبة
     */
    private void refreshLibraryInfo() {
        // إعادة تحميل المعلومات من PyPI
        PyPIService pyPIService = new PyPIService(this, null);
        LibraryItem updatedLibrary = pyPIService.getLibraryDetails(library.getName());
        
        if (updatedLibrary != null) {
            library = updatedLibrary;
            populateLibraryInfo();
            Toast.makeText(this, "تم تحديث المعلومات", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * إضافة للمفضلة
     */
    private void addToFavorites() {
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> favorites = new HashSet<>(preferences.getStringSet("favorites", new HashSet<>()));
        favorites.add(library.getName());
        editor.putStringSet("favorites", favorites);
        editor.apply();
        
        Toast.makeText(this, "تم الإضافة للمفضلة", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            refreshLibraryInfo();
        }
    }
    
    /**
     * فئة عنصر التبعية
     */
    private static class DependencyItem {
        private String name;
        private boolean isInstalled;
        private String version;
        
        public DependencyItem(String dependency, boolean isInstalled) {
            this.name = parseDependencyName(dependency);
            this.isInstalled = isInstalled;
            this.version = extractVersion(dependency);
        }
        
        private static String parseDependencyName(String dependency) {
            if (dependency == null || dependency.isEmpty()) return "";
            String cleanDep = dependency.replaceAll("[<>=!~].*", "").trim();
            cleanDep = cleanDep.replaceAll("[\\[\\]\\(\\)]", "").trim();
            String[] parts = cleanDep.split("[\\s\\-:]+");
            return parts.length > 0 ? parts[0].trim() : cleanDep;
        }
        
        private static String extractVersion(String dependency) {
            if (dependency == null || dependency.isEmpty()) return "";
            int start = dependency.indexOf("==");
            if (start != -1) {
                return dependency.substring(start + 2).trim();
            }
            return "";
        }
        
        public String getName() { return name; }
        public boolean isInstalled() { return isInstalled; }
        public String getVersion() { return version; }
    }
    
    /**
     * محول التبعيات
     */
    private static class DependencyAdapter extends RecyclerView.Adapter<DependencyAdapter.DependencyViewHolder> {
        private List<DependencyItem> dependencies;
        
        public DependencyAdapter(List<DependencyItem> dependencies) {
            this.dependencies = dependencies;
        }
        
        public void updateDependencies(List<DependencyItem> dependencies) {
            this.dependencies = dependencies;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public DependencyViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                android.R.layout.simple_list_item_2, parent, false);
            return new DependencyViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull DependencyViewHolder holder, int position) {
            DependencyItem dependency = dependencies.get(position);
            holder.text1.setText(dependency.getName());
            if (!dependency.getVersion().isEmpty()) {
                holder.text2.setText("الإصدار: " + dependency.getVersion());
            } else {
                holder.text2.setText(dependency.isInstalled() ? "مثبت" : "غير مثبت");
            }
            holder.text2.setTextColor(dependency.isInstalled() ? 
                holder.itemView.getContext().getColor(R.color.green_500) : 
                holder.itemView.getContext().getColor(R.color.gray_500));
        }
        
        @Override
        public int getItemCount() {
            return dependencies.size();
        }
        
        static class DependencyViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            
            DependencyViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
}