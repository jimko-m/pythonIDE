package com.pythonide.libraries;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LibraryManagerActivity - نظام إدارة مكتبات Python متقدم وكامل
 * 
 * المميزات الرئيسية:
 * - تثبيت وإلغاء تثبيت المكتبات مع إدارة التبعيات
 * - البحث المتقدم في PyPI
 * - عرض تفاصيل المكتبات مع الإصدارات المتعددة
 * - شريط التقدم للتثبيت والإزالة
 * - فحص توافق المكتبات مع إصدارات Python المختلفة
 * - واجهة مستخدم متقدمة وسهلة الاستخدام
 * - إدارة ذكية للذاكرة والتخزين المؤقت
 */
public class LibraryManagerActivity extends AppCompatActivity implements 
        LibraryAdapter.OnLibraryClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        PackageManager.OnInstallationListener,
        PyPIService.OnSearchListener {

    // ====================== المتغيرات الأساسية ======================
    
    // واجهة المستخدم
    private EditText searchEditText;
    private RecyclerView librariesRecyclerView;
    private ProgressBar loadingProgressBar, searchProgressBar;
    private TextView emptyStateText, resultsCountText, statusText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabInstall, fabUninstall;
    private Spinner pythonVersionSpinner;
    private LinearLayout searchInfoLayout;
    private ImageView filterIcon;
    
    // الموجهات والمُكِيفات
    private LibraryAdapter libraryAdapter;
    private PackageManager packageManager;
    private PyPIService pyPIService;
    private DependencyResolver dependencyResolver;
    private CompatibleLibraryChecker compatibilityChecker;
    private PythonExecutor pythonExecutor;
    
    // البيانات والقوائم
    private List<LibraryItem> allLibraries;
    private List<LibraryItem> filteredLibraries;
    private List<LibraryItem> installedLibraries;
    private List<LibraryItem> searchResults;
    private Map<String, List<String>> dependencyGraph;
    private Map<String, LibraryVersion> libraryVersions;
    private Map<String, Boolean> installationStatus;
    
    // إدارة الحالة
    private String currentFilter = "all"; // all, installed, available, compatible, popular
    private String currentPythonVersion = "3.9";
    private boolean isSearching = false;
    private boolean isLoadingInstalled = false;
    private String currentSearchQuery = "";
    private int totalResults = 0;
    
    // Thread Management
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // الثوابت
    private static final String TAG = "LibraryManagerActivity";
    private static final String PYPI_SEARCH_API = "https://pypi.org/search/?q={query}&page={page}";
    private static final String PYPI_PACKAGE_API = "https://pypi.org/pypi/{package}/json";
    private static final int INSTALL_REQUEST_CODE = 1001;
    private static final int SEARCH_DEBOUNCE_DELAY = 500; // milliseconds
    
    // ====================== دورة الحياة ======================
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_manager);
        
        initializeComponents();
        setupToolbar();
        setupUI();
        setupManagers();
        loadInstalledLibraries();
        loadPopularLibraries();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshInstalledLibraries();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }
    
    // ====================== التهيئة والإعداد ======================
    
    /**
     * تهيئة جميع المكونات
     */
    private void initializeComponents() {
        // إعداد إدارة الخيوط
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(6);
        
        // تهيئة البيانات
        allLibraries = new ArrayList<>();
        filteredLibraries = new ArrayList<>();
        installedLibraries = new ArrayList<>();
        searchResults = new ArrayList<>();
        dependencyGraph = new HashMap<>();
        libraryVersions = new HashMap<>();
        installationStatus = new HashMap<>();
        
        // العثور على عناصر واجهة المستخدم
        findUIElements();
    }
    
    /**
     * العثور على عناصر واجهة المستخدم
     */
    private void findUIElements() {
        searchEditText = findViewById(R.id.searchEditText);
        librariesRecyclerView = findViewById(R.id.librariesRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        searchProgressBar = findViewById(R.id.searchProgressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        resultsCountText = findViewById(R.id.resultsCountText);
        statusText = findViewById(R.id.statusText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabInstall = findViewById(R.id.fabInstall);
        fabUninstall = findViewById(R.id.fabUninstall);
        pythonVersionSpinner = findViewById(R.id.pythonVersionSpinner);
        searchInfoLayout = findViewById(R.id.searchInfoLayout);
        filterIcon = findViewById(R.id.filterIcon);
    }
    
    /**
     * إعداد شريط الأدوات
     */
    private void setupToolbar() {
        setTitle("إدارة مكتبات Python");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
    
    /**
     * إعداد واجهة المستخدم
     */
    private void setupUI() {
        setupRecyclerView();
        setupSearch();
        setupBottomNavigation();
        setupFloatingActionButtons();
        setupRefreshLayout();
        setupPythonVersionSpinner();
        setupProgressBars();
    }
    
    /**
     * إعداد قائمة المكتبات
     */
    private void setupRecyclerView() {
        libraryAdapter = new LibraryAdapter(filteredLibraries, this, installedLibraries);
        librariesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        librariesRecyclerView.setAdapter(libraryAdapter);
        
        // إضافة تخصيص للعناصر
        librariesRecyclerView.addItemDecoration(new LibraryItemDecoration(16, 8));
        
        showEmptyState("ابحث عن مكتبات Python أو تصفح المكتبات الشائعة");
    }
    
    /**
     * إعداد البحث
     */
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // تطبيق تأخير للبحث لتجنب الطلبات المتكررة
                if (searchDebounceRunnable != null) {
                    mainHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> performSearch(s.toString().trim());
                mainHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_DELAY);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private Runnable searchDebounceRunnable;
    
    /**
     * إعداد شريط التنقل السفلي
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener(this);
        bottomNavigation.getMenu().findItem(R.id.nav_popular).setChecked(true);
    }
    
    /**
     * إعداد أزرار الإجراء العائم
     */
    private void setupFloatingActionButtons() {
        fabInstall.setOnClickListener(v -> showAdvancedInstallDialog());
        fabUninstall.setOnClickListener(v -> showUninstallManagerDialog());
    }
    
    /**
     * إعداد تحديث السحب
     */
    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentSearchQuery.isEmpty()) {
                if (currentFilter.equals("installed")) {
                    loadInstalledLibraries();
                } else {
                    loadPopularLibraries();
                }
            } else {
                performSearch(currentSearchQuery);
            }
        });
    }
    
    /**
     * إعداد قائمة إصدارات Python
     */
    private void setupPythonVersionSpinner() {
        String[] versions = {"3.8", "3.9", "3.10", "3.11", "3.12", "الكل"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, versions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pythonVersionSpinner.setAdapter(adapter);
        
        pythonVersionSpinner.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, 
                                     View view, int position, long id) {
                String selectedVersion = versions[position];
                if (!selectedVersion.equals(currentPythonVersion)) {
                    currentPythonVersion = selectedVersion;
                    applyPythonVersionFilter();
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    /**
     * إعداد أشرطة التقدم
     */
    private void setupProgressBars() {
        loadingProgressBar.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.GONE);
        searchInfoLayout.setVisibility(View.GONE);
    }
    
    /**
     * إعداد الموجهات
     */
    private void setupManagers() {
        packageManager = new PackageManager(this, this);
        pyPIService = new PyPIService(this, this);
        dependencyResolver = new DependencyResolver();
        compatibilityChecker = new CompatibleLibraryChecker();
        pythonExecutor = new PythonExecutor(this);
        
        // تحميل رسم التبعيات
        loadDependencyGraph();
    }
    
    // ====================== تحميل البيانات ======================
    
    /**
     * تحميل المكتبات المثبتة
     */
    private void loadInstalledLibraries() {
        if (isLoadingInstalled) return;
        
        isLoadingInstalled = true;
        showProgress(true, "جاري تحميل المكتبات المثبتة...");
        
        executorService.execute(() -> {
            try {
                List<LibraryItem> installed = packageManager.getInstalledLibraries();
                
                mainHandler.post(() -> {
                    installedLibraries.clear();
                    installedLibraries.addAll(installed);
                    
                    if (currentFilter.equals("installed")) {
                        applyFilter("installed");
                    }
                    
                    isLoadingInstalled = false;
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحميل المكتبات المثبتة", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "خطأ في تحميل المكتبات المثبتة", Toast.LENGTH_SHORT).show();
                    isLoadingInstalled = false;
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    /**
     * تحديث المكتبات المثبتة
     */
    private void refreshInstalledLibraries() {
        executorService.execute(() -> {
            try {
                packageManager.refreshInstalledLibraries();
                List<LibraryItem> updated = packageManager.getInstalledLibraries();
                
                mainHandler.post(() -> {
                    installedLibraries.clear();
                    installedLibraries.addAll(updated);
                    libraryAdapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحديث المكتبات المثبتة", e);
            }
        });
    }
    
    /**
     * تحميل المكتبات الشائعة
     */
    private void loadPopularLibraries() {
        showProgress(true, "جاري تحميل المكتبات الشائعة...");
        
        executorService.execute(() -> {
            try {
                List<LibraryItem> popularLibraries = fetchPopularLibrariesFromPyPI();
                
                mainHandler.post(() -> {
                    allLibraries.clear();
                    allLibraries.addAll(popularLibraries);
                    
                    if (currentFilter.equals("popular") || currentFilter.equals("all")) {
                        applyFilter("popular");
                    }
                    
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحميل المكتبات الشائعة", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "خطأ في تحميل المكتبات الشائعة", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    /**
     * جلب المكتبات الشائعة من PyPI
     */
    private List<LibraryItem> fetchPopularLibrariesFromPyPI() {
        List<LibraryItem> libraries = new ArrayList<>();
        
        String[] popularPackages = {
            "numpy", "pandas", "matplotlib", "requests", "flask",
            "django", "tensorflow", "scikit-learn", "pillow", "beautifulsoup4",
            "selenium", "pymongo", "sqlalchemy", "pytest", "pygame",
            "opencv-python", "beautifulsoup", "twisted", "pyyaml", "click",
            "jinja2", "werkzeug", "markupsafe", "itsdangerous", "click"
        };
        
        for (String packageName : popularPackages) {
            try {
                LibraryItem library = fetchLibraryFromPyPI(packageName);
                if (library != null) {
                    libraries.add(library);
                }
            } catch (Exception e) {
                Log.w(TAG, "فشل في جلب معلومات " + packageName, e);
            }
        }
        
        return libraries;
    }
    
    /**
     * جلب مكتبة واحدة من PyPI
     */
    private LibraryItem fetchLibraryFromPyPI(String packageName) {
        try {
            URL url = new URL(PYPI_PACKAGE_API.replace("{package}", packageName));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return parseLibraryFromJson(response.toString(), packageName);
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جلب معلومات المكتبة: " + packageName, e);
            return null;
        }
    }
    
    /**
     * تحليل JSON إلى LibraryItem
     */
    private LibraryItem parseLibraryFromJson(String jsonResponse, String packageName) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject info = json.getJSONObject("info");
            
            return new LibraryItem(
                info.optString("name", packageName),
                info.optString("summary", "وصف غير متوفر"),
                info.optString("version", "0.0.0"),
                info.optString("author", "غير محدد"),
                info.optString("author_email", ""),
                info.optString("license", "غير محدد"),
                info.optString("home_page", ""),
                info.optString("pypi_url", ""),
                parseDependencies(info.optString("requires_dist", "")),
                parseClassifiers(info.optJSONArray("classifiers")),
                parsePythonVersions(info.optJSONArray("classifiers")),
                installedLibraries.containsKey(info.optString("name", packageName)),
                getDownloadCount(packageName),
                getLastUpdated(info.optString("upload_time", "")),
                false // isUpdated (سيتم حسابه لاحقاً)
            );
            
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في تحليل JSON للمكتبة: " + packageName, e);
            return null;
        }
    }
    
    // ====================== البحث والفلترة ======================
    
    /**
     * تنفيذ البحث
     */
    private void performSearch(String query) {
        if (query.isEmpty()) {
            resetToCurrentFilter();
            return;
        }
        
        if (query.equals(currentSearchQuery) && isSearching) {
            return;
        }
        
        currentSearchQuery = query;
        isSearching = true;
        
        showSearchProgress(true);
        
        executorService.execute(() -> {
            pyPIService.searchPackages(query, currentPythonVersion, this);
        });
    }
    
    /**
     * تطبيق الفلتر
     */
    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredLibraries.clear();
        
        updateFilterIcon(filter);
        
        switch (filter) {
            case "installed":
                filteredLibraries.addAll(installedLibraries);
                break;
                
            case "available":
                for (LibraryItem lib : allLibraries) {
                    if (!installedLibraries.containsKey(lib.getName())) {
                        filteredLibraries.add(lib);
                    }
                }
                break;
                
            case "compatible":
                for (LibraryItem lib : allLibraries) {
                    if (compatibilityChecker.isCompatible(lib, currentPythonVersion)) {
                        filteredLibraries.add(lib);
                    }
                }
                break;
                
            case "popular":
                filteredLibraries.addAll(allLibraries);
                break;
                
            default: // "all"
                filteredLibraries.addAll(allLibraries);
                break;
        }
        
        libraryAdapter.updateLibraries(filteredLibraries);
        updateResultsInfo(filteredLibraries.size(), filter);
        
        if (filteredLibraries.isEmpty()) {
            showEmptyState("لا توجد مكتبات للفلتر المحدد");
        } else {
            hideEmptyState();
        }
    }
    
    /**
     * تطبيق فلتر إصدار Python
     */
    private void applyPythonVersionFilter() {
        if (currentFilter.equals("compatible")) {
            applyFilter("compatible");
        } else if (currentFilter.equals("installed")) {
            // فلتر المكتبات المثبتة حسب الإصدار
            List<LibraryItem> versionFiltered = new ArrayList<>();
            for (LibraryItem lib : installedLibraries) {
                if (currentPythonVersion.equals("الكل") || 
                    lib.getPythonVersions().contains(currentPythonVersion)) {
                    versionFiltered.add(lib);
                }
            }
            
            filteredLibraries.clear();
            filteredLibraries.addAll(versionFiltered);
            libraryAdapter.updateLibraries(filteredLibraries);
            updateResultsInfo(filteredLibraries.size(), "installed");
        }
    }
    
    /**
     * إعادة تعيين إلى الفلتر الحالي
     */
    private void resetToCurrentFilter() {
        currentSearchQuery = "";
        isSearching = false;
        searchResults.clear();
        showSearchProgress(false);
        applyFilter(currentFilter);
    }
    
    // ====================== التثبيت والإزالة ======================
    
    /**
     * عرض حوار التثبيت المتقدم
     */
    private void showAdvancedInstallDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تثبيت مكتبة جديدة");
        builder.setIcon(R.drawable.ic_install);
        
        // إنشاء تخطيط مخصص
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // حقل اسم المكتبة
        TextInputLayout nameLayout = new TextInputLayout(this);
        TextInputEditText nameEditText = new TextInputEditText(this);
        nameEditText.setHint("اسم المكتبة (مثال: requests, numpy)");
        nameLayout.addView(nameEditText);
        
        // حقل الإصدار
        TextInputLayout versionLayout = new TextInputLayout(this);
        TextInputEditText versionEditText = new TextInputEditText(this);
        versionEditText.setHint("الإصدار المحدد (اختياري)");
        versionLayout.addView(versionEditText);
        
        // خيارات إضافية
        CheckBox checkUpgrade = new CheckBox(this);
        checkUpgrade.setText("ترقية إذا كان مثبت مسبقاً");
        checkUpgrade.setChecked(true);
        
        CheckBox checkDependencies = new CheckBox(this);
        checkDependencies.setText("تثبيت التبعيات تلقائياً");
        checkDependencies.setChecked(true);
        
        CheckBox checkCompatible = new CheckBox(this);
        checkCompatible.setText("فحص التوافق مع إصدار Python");
        checkCompatible.setChecked(true);
        
        layout.addView(nameLayout);
        layout.addView(versionLayout);
        layout.addView(checkUpgrade);
        layout.addView(checkDependencies);
        layout.addView(checkCompatible);
        
        builder.setView(layout);
        
        builder.setPositiveButton("تثبيت", (dialog, which) -> {
            String packageName = nameEditText.getText().toString().trim();
            String version = versionEditText.getText().toString().trim();
            
            if (!packageName.isEmpty()) {
                InstallOptions options = new InstallOptions(
                    packageName,
                    version.isEmpty() ? null : version,
                    checkUpgrade.isChecked(),
                    checkDependencies.isChecked(),
                    checkCompatible.isChecked(),
                    currentPythonVersion.equals("الكل") ? null : currentPythonVersion
                );
                
                installPackageWithOptions(options);
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        
        // إضافة زر للبحث المتقدم
        builder.setNeutralButton("بحث متقدم", (dialog, which) -> {
            showAdvancedSearchDialog();
        });
        
        builder.show();
    }
    
    /**
     * تثبيت مكتبة مع الخيارات المحددة
     */
    private void installPackageWithOptions(InstallOptions options) {
        showInstallationProgress("تحضير التثبيت...");
        
        executorService.execute(() -> {
            try {
                // فحص التوافق أولاً
                if (options.isCheckCompatible() && options.getPythonVersion() != null) {
                    LibraryItem library = pyPIService.getLibraryDetails(options.getPackageName());
                    if (library != null && 
                        !compatibilityChecker.isCompatible(library, options.getPythonVersion())) {
                        mainHandler.post(() -> {
                            hideInstallationProgress();
                            showCompatibilityWarning(library, options);
                        });
                        return;
                    }
                }
                
                // تثبيت المكتبة
                packageManager.installPackage(options, this);
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في التثبيت", e);
                mainHandler.post(() -> {
                    hideInstallationProgress();
                    Toast.makeText(this, "خطأ في التثبيت: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * عرض تحذير التوافق
     */
    private void showCompatibilityWarning(LibraryItem library, InstallOptions options) {
        new AlertDialog.Builder(this)
            .setTitle("تحذير التوافق")
            .setMessage("المكتبة " + library.getName() + " قد لا تكون متوافقة مع Python " + 
                       options.getPythonVersion() + ".\n\nهل تريد المتابعة؟")
            .setPositiveButton("متابعة", (dialog, which) -> {
                options.setCheckCompatible(false);
                installPackageWithOptions(options);
            })
            .setNegativeButton("إلغاء", (dialog, which) -> {
                Toast.makeText(this, "تم إلغاء التثبيت", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    /**
     * عرض مدير الإلغاء
     */
    private void showUninstallManagerDialog() {
        if (installedLibraries.isEmpty()) {
            Toast.makeText(this, "لا توجد مكتبات مثبتة للإلغاء", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إدارة إلغاء التثبيت");
        builder.setIcon(R.drawable.ic_uninstall);
        
        // إنشاء قائمة المكتبات مع خانات الاختيار
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        
        Set<String> selectedLibraries = new HashSet<>();
        
        for (LibraryItem library : installedLibraries) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(library.getName() + " v" + library.getVersion());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedLibraries.add(library.getName());
                } else {
                    selectedLibraries.remove(library.getName());
                }
            });
            listLayout.addView(checkBox);
        }
        
        builder.setView(listLayout);
        
        builder.setPositiveButton("إلغاء التثبيت", (dialog, which) -> {
            if (!selectedLibraries.isEmpty()) {
                uninstallMultipleLibraries(new ArrayList<>(selectedLibraries));
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    /**
     * إلغاء تثبيت عدة مكتبات
     */
    private void uninstallMultipleLibraries(List<String> libraryNames) {
        showInstallationProgress("جاري إلغاء التثبيت...");
        
        executorService.execute(() -> {
            List<String> failed = new ArrayList<>();
            
            for (String libraryName : libraryNames) {
                try {
                    boolean success = packageManager.uninstallPackage(libraryName);
                    if (!success) {
                        failed.add(libraryName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في إلغاء " + libraryName, e);
                    failed.add(libraryName);
                }
            }
            
            mainHandler.post(() -> {
                hideInstallationProgress();
                
                if (failed.isEmpty()) {
                    Toast.makeText(this, "تم إلغاء التثبيت بنجاح", Toast.LENGTH_LONG).show();
                    refreshInstalledLibraries();
                } else {
                    String failedList = String.join(", ", failed);
                    Toast.makeText(this, "فشل في إلغاء: " + failedList, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    
    // ====================== واجهة المستخدم المساعدة ======================
    
    /**
     * إظهار حالة فارغة
     */
    private void showEmptyState(String message) {
        emptyStateText.setText(message);
        emptyStateText.setVisibility(View.VISIBLE);
        librariesRecyclerView.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.GONE);
        searchInfoLayout.setVisibility(View.GONE);
    }
    
    /**
     * إخفاء حالة فارغة
     */
    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
        librariesRecyclerView.setVisibility(View.VISIBLE);
    }
    
    /**
     * إظهار/إخفاء شريط التقدم
     */
    private void showProgress(boolean show, String message) {
        if (show) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            statusText.setText(message);
            statusText.setVisibility(View.VISIBLE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            statusText.setVisibility(View.GONE);
        }
    }
    
    /**
     * إظهار/إخفاء تقدم البحث
     */
    private void showSearchProgress(boolean show) {
        searchProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        searchInfoLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    /**
     * تحديث معلومات النتائج
     */
    private void updateResultsInfo(int count, String filter) {
        resultsCountText.setText(count + " نتيجة");
        searchInfoLayout.setVisibility(View.VISIBLE);
        
        String filterText = getFilterDisplayName(filter);
        statusText.setText(filterText);
    }
    
    /**
     * تحديث أيقونة الفلتر
     */
    private void updateFilterIcon(String filter) {
        int iconRes;
        switch (filter) {
            case "installed": iconRes = R.drawable.ic_check_circle; break;
            case "available": iconRes = R.drawable.ic_download; break;
            case "compatible": iconRes = R.drawable.ic_compatible; break;
            case "popular": iconRes = R.drawable.ic_star; break;
            default: iconRes = R.drawable.ic_library; break;
        }
        filterIcon.setImageResource(iconRes);
    }
    
    /**
     * الحصول على اسم الفلتر للعرض
     */
    private String getFilterDisplayName(String filter) {
        switch (filter) {
            case "installed": return "المكتبات المثبتة";
            case "available": return "المكتبات المتاحة";
            case "compatible": return "المكتبات المتوافقة";
            case "popular": return "المكتبات الشائعة";
            default: return "جميع المكتبات";
        }
    }
    
    /**
     * عرض حوار البحث المتقدم
     */
    private void showAdvancedSearchDialog() {
        // تنفيذ البحث المتقدم
        Toast.makeText(this, "البحث المتقدم قيد التطوير", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * إظهار تقدم التثبيت
     */
    private void showInstallationProgress(String message) {
        // تنفيذ UI مخصص للتقدم
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }
    
    /**
     * إخفاء تقدم التثبيت
     */
    private void hideInstallationProgress() {
        statusText.setVisibility(View.GONE);
    }
    
    /**
     * تحميل رسم التبعيات
     */
    private void loadDependencyGraph() {
        dependencyGraph.put("numpy", Arrays.asList("setuptools"));
        dependencyGraph.put("requests", Arrays.asList("certifi", "charset-normalizer", "idna", "urllib3"));
        dependencyGraph.put("flask", Arrays.asList("click", "itsdangerous", "jinja2", "markupsafe", "werkzeug"));
        dependencyGraph.put("pandas", Arrays.asList("numpy", "python-dateutil", "pytz", "six"));
        dependencyGraph.put("django", Arrays.asList("asgiref", "pytz", "sqlparse"));
        dependencyGraph.put("tensorflow", Arrays.asList("numpy", "protobuf"));
    }
    
    // ====================== معالجات الأحداث ======================
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_popular) {
            currentFilter = "popular";
            applyFilter(currentFilter);
            return true;
        } else if (itemId == R.id.nav_installed) {
            currentFilter = "installed";
            applyFilter(currentFilter);
            return true;
        } else if (itemId == R.id.nav_available) {
            currentFilter = "available";
            applyFilter(currentFilter);
            return true;
        } else if (itemId == R.id.nav_compatible) {
            currentFilter = "compatible";
            applyFilter(currentFilter);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library_manager, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            swipeRefreshLayout.setRefreshing(true);
            if (currentSearchQuery.isEmpty()) {
                if (currentFilter.equals("installed")) {
                    loadInstalledLibraries();
                } else {
                    loadPopularLibraries();
                }
            } else {
                performSearch(currentSearchQuery);
            }
            return true;
        } else if (id == R.id.action_manage_dependencies) {
            showDependencyManager();
            return true;
        } else if (id == R.id.action_clear_cache) {
            clearCache();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_REQUEST_CODE && resultCode == RESULT_OK) {
            refreshInstalledLibraries();
            applyFilter(currentFilter);
        }
    }
    
    // ====================== واجهات الاستدعاء ======================
    
    @Override
    public void onLibraryClick(LibraryItem library) {
        Intent intent = new Intent(this, LibraryDetailsActivity.class);
        intent.putExtra("library", library);
        intent.putExtra("dependency_graph", new HashMap<>(dependencyGraph));
        intent.putStringArrayListExtra("installed_libraries", 
                new ArrayList<>(installedLibraries.keySet()));
        startActivity(intent);
    }
    
    @Override
    public void onInstallClick(LibraryItem library) {
        InstallOptions options = new InstallOptions(
            library.getName(),
            null,
            true, // upgrade
            true, // install dependencies
            true, // check compatibility
            currentPythonVersion.equals("الكل") ? null : currentPythonVersion
        );
        installPackageWithOptions(options);
    }
    
    @Override
    public void onUninstallClick(LibraryItem library) {
        new AlertDialog.Builder(this)
            .setTitle("إلغاء التثبيت")
            .setMessage("هل تريد إلغاء تثبيت " + library.getName() + "؟")
            .setPositiveButton("إلغاء التثبيت", (dialog, which) -> {
                uninstallMultipleLibraries(Arrays.asList(library.getName()));
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }
    
    @Override
    public void onSearchResults(List<LibraryItem> results) {
        mainHandler.post(() -> {
            searchResults.clear();
            searchResults.addAll(results);
            
            // فلترة النتائج حسب التوافق
            List<LibraryItem> compatibleResults = new ArrayList<>();
            for (LibraryItem result : results) {
                if (compatibilityChecker.isCompatible(result, currentPythonVersion)) {
                    compatibleResults.add(result);
                }
            }
            
            libraryAdapter.updateLibraries(compatibleResults);
            updateResultsInfo(compatibleResults.size(), "search");
            
            isSearching = false;
            showSearchProgress(false);
            swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    @Override
    public void onSearchError(String error) {
        mainHandler.post(() -> {
            Toast.makeText(this, "خطأ في البحث: " + error, Toast.LENGTH_LONG).show();
            isSearching = false;
            showSearchProgress(false);
            swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    @Override
    public void onInstallationStart(String packageName) {
        mainHandler.post(() -> {
            showInstallationProgress("بدء تثبيت " + packageName + "...");
        });
    }
    
    @Override
    public void onInstallationProgress(String packageName, int progress, String currentStep) {
        mainHandler.post(() -> {
            statusText.setText("تثبيت " + packageName + ": " + currentStep + " (" + progress + "%)");
        });
    }
    
    @Override
    public void onInstallationComplete(String packageName, boolean success, String message) {
        mainHandler.post(() -> {
            if (success) {
                Toast.makeText(this, "تم تثبيت " + packageName + " بنجاح", Toast.LENGTH_LONG).show();
                refreshInstalledLibraries();
            } else {
                Toast.makeText(this, "فشل تثبيت " + packageName + ": " + message, 
                    Toast.LENGTH_LONG).show();
            }
            hideInstallationProgress();
        });
    }
    
    // ====================== دوال مساعدة إضافية ======================
    
    /**
     * عرض مدير التبعيات
     */
    private void showDependencyManager() {
        Toast.makeText(this, "مدير التبعيات قيد التطوير", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * مسح التخزين المؤقت
     */
    private void clearCache() {
        executorService.execute(() -> {
            pyPIService.clearCache();
            mainHandler.post(() -> {
                Toast.makeText(this, "تم مسح التخزين المؤقت", Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    /**
     * عرض إعدادات النظام
     */
    private void showSettingsDialog() {
        Toast.makeText(this, "الإعدادات قيد التطوير", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * تنظيف الموارد
     */
    private void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (packageManager != null) {
            packageManager.cleanup();
        }
        if (searchDebounceRunnable != null) {
            mainHandler.removeCallbacks(searchDebounceRunnable);
        }
    }
    
    // ====================== دوال التحليل المساعدة ======================
    
    private List<String> parseDependencies(String requiresDist) {
        List<String> deps = new ArrayList<>();
        if (!requiresDist.isEmpty()) {
            String[] depsArray = requiresDist.split(",");
            for (String dep : depsArray) {
                String cleanDep = dep.trim();
                if (!cleanDep.isEmpty()) {
                    deps.add(cleanDep);
                }
            }
        }
        return deps;
    }
    
    private List<String> parseClassifiers(JSONArray classifiers) {
        List<String> classList = new ArrayList<>();
        if (classifiers != null) {
            for (int i = 0; i < classifiers.length(); i++) {
                try {
                    classList.add(classifiers.getString(i));
                } catch (JSONException e) {
                    Log.e(TAG, "خطأ في قراءة مصنف", e);
                }
            }
        }
        return classList;
    }
    
    private List<String> parsePythonVersions(JSONArray classifiers) {
        List<String> versions = new ArrayList<>();
        if (classifiers != null) {
            for (int i = 0; i < classifiers.length(); i++) {
                try {
                    String classifier = classifiers.getString(i);
                    if (classifier.startsWith("Programming Language :: Python ::")) {
                        String version = extractPythonVersion(classifier);
                        if (version != null && !versions.contains(version)) {
                            versions.add(version);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "خطأ في قراءة مصنف إصدار Python", e);
                }
            }
        }
        return versions.isEmpty() ? Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12") : versions;
    }
    
    private String extractPythonVersion(String classifier) {
        // استخراج إصدار Python من المصنف
        String[] parts = classifier.split("::");
        if (parts.length >= 4) {
            return parts[3].trim();
        }
        return null;
    }
    
    private int getDownloadCount(String packageName) {
        // محاكاة عدد التحميلات
        return new Random().nextInt(1000000) + 10000;
    }
    
    private Date getLastUpdated(String uploadTime) {
        try {
            return new Date(); // للتبسيط، نعيد الوقت الحالي
        } catch (Exception e) {
            return new Date();
        }
    }
}
}