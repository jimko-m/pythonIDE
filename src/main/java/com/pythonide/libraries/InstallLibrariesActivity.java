package com.pythonide.libraries;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * InstallLibrariesActivity - شاشة التثبيت المتقدم للمكتبات
 */
public class InstallLibrariesActivity extends AppCompatActivity implements 
        LibrarySearchAdapter.OnLibrarySelectListener {
    
    // واجهة المستخدم
    private TextInputEditText searchInput;
    private TextInputLayout searchInputLayout;
    private RecyclerView searchResultsRecyclerView;
    private TextView selectedLibrariesText;
    private RecyclerView selectedLibrariesRecyclerView;
    private TextView dependenciesText;
    private ProgressBar searchProgressBar;
    private Button installButton;
    private Button clearButton;
    private LinearLayout dependenciesContainer;
    private LinearLayout selectedLibrariesContainer;
    
    // البيانات
    private List<Library> searchResults;
    private List<Library> selectedLibraries;
    private Set<String> allDependencies;
    private LibrarySearchAdapter searchAdapter;
    private SelectedLibraryAdapter selectedAdapter;
    private Map<String, List<String>> dependencyGraph;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_libraries);
        
        // إعداد ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("تثبيت المكتبات");
        }
        
        initViews();
        setupRecyclerViews();
        setupListeners();
    }
    
    /**
     * تهيئة عناصر الواجهة
     */
    private void initViews() {
        searchInput = findViewById(R.id.searchInput);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        selectedLibrariesText = findViewById(R.id.selectedLibrariesText);
        selectedLibrariesRecyclerView = findViewById(R.id.selectedLibrariesRecyclerView);
        dependenciesText = findViewById(R.id.dependenciesText);
        searchProgressBar = findViewById(R.id.searchProgressBar);
        installButton = findViewById(R.id.installButton);
        clearButton = findViewById(R.id.clearButton);
        dependenciesContainer = findViewById(R.id.dependenciesContainer);
        selectedLibrariesContainer = findViewById(R.id.selectedLibrariesContainer);
        
        // إعداد البيانات الأولية
        searchResults = new ArrayList<>();
        selectedLibraries = new ArrayList<>();
        allDependencies = new LinkedHashSet<>();
        dependencyGraph = new HashMap<>();
        
        // إعداد ActionBar للعودة
        setupDependencyGraph();
        updateUI();
    }
    
    /**
     * إعداد RecyclerViews
     */
    private void setupRecyclerViews() {
        searchAdapter = new LibrarySearchAdapter(searchResults, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchAdapter);
        
        selectedAdapter = new SelectedLibraryAdapter(selectedLibraries, new SelectedLibraryAdapter.OnRemoveListener() {
            @Override
            public void onRemove(Library library) {
                removeLibrary(library);
            }
        });
        selectedLibrariesRecyclerView.setLayoutManager(new LinearLayoutManager(this, 
                LinearLayoutManager.HORIZONTAL, false));
        selectedLibrariesRecyclerView.setAdapter(selectedAdapter);
    }
    
    /**
     * إعداد المستمعين
     */
    private void setupListeners() {
        // بحث فوري مع تأخير
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    new SearchTask().execute(s.toString());
                } else {
                    clearSearchResults();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        installButton.setOnClickListener(v -> {
            if (!selectedLibraries.isEmpty()) {
                startInstallation();
            }
        });
        
        clearButton.setOnClickListener(v -> clearAll());
    }
    
    /**
     * إعداد رسم التبعيات
     */
    private void setupDependencyGraph() {
        dependencyGraph.put("numpy", Arrays.asList("setuptools", "wheel"));
        dependencyGraph.put("pandas", Arrays.asList("numpy", "python-dateutil", "pytz", "six"));
        dependencyGraph.put("matplotlib", Arrays.asList("numpy", "pyparsing", "kiwisolver", "cycler"));
        dependencyGraph.put("requests", Arrays.asList("certifi", "charset-normalizer", "idna", "urllib3"));
        dependencyGraph.put("flask", Arrays.asList("click", "itsdangerous", "jinja2", "markupsafe", "werkzeug"));
        dependencyGraph.put("django", Arrays.asList("asgiref", "pytz", "sqlparse"));
        dependencyGraph.put("tensorflow", Arrays.asList("protobuf", "numpy"));
        dependencyGraph.put("scikit-learn", Arrays.asList("numpy", "scipy", "joblib", "threadpoolctl"));
        dependencyGraph.put("pillow", Arrays.asList());
        dependencyGraph.put("beautifulsoup4", Arrays.asList("soupsieve", "lxml"));
        dependencyGraph.put("selenium", Arrays.asList("urllib3"));
        dependencyGraph.put("pymongo", Arrays.asList());
        dependencyGraph.put("sqlalchemy", Arrays.asList());
        dependencyGraph.put("pytest", Arrays.asList("attrs", "py", "colorama"));
        dependencyGraph.put("pygame", Arrays.asList("pygame", "pygame"));
    }
    
    /**
     * مسح نتائج البحث
     */
    private void clearSearchResults() {
        searchResults.clear();
        searchAdapter.notifyDataSetChanged();
    }
    
    /**
     * تحديث واجهة المستخدم
     */
    private void updateUI() {
        // تحديث عدد المكتبات المحددة
        if (selectedLibraries.isEmpty()) {
            selectedLibrariesText.setText("لم يتم اختيار أي مكتبات");
            selectedLibrariesContainer.setVisibility(View.GONE);
            installButton.setEnabled(false);
        } else {
            selectedLibrariesText.setText("المكتبات المحددة (" + selectedLibraries.size() + "):");
            selectedLibrariesContainer.setVisibility(View.VISIBLE);
            installButton.setEnabled(true);
        }
        
        // تحديث التبعيات
        allDependencies.clear();
        for (Library library : selectedLibraries) {
            if (dependencyGraph.containsKey(library.getName())) {
                allDependencies.addAll(dependencyGraph.get(library.getName()));
            }
        }
        
        if (allDependencies.isEmpty()) {
            dependenciesText.setText("لا توجد تبعيات إضافية");
            dependenciesContainer.setVisibility(View.GONE);
        } else {
            dependenciesText.setText("التبعيات المطلوبة (" + allDependencies.size() + "):");
            dependenciesContainer.setVisibility(View.VISIBLE);
        }
        
        selectedAdapter.notifyDataSetChanged();
    }
    
    /**
     * إضافة مكتبة للقائمة المحددة
     */
    @Override
    public void onLibrarySelect(Library library) {
        if (!selectedLibraries.contains(library)) {
            selectedLibraries.add(library);
            updateUI();
            
            // إخفاء لوحة المفاتيح
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
            
            // مسح النص
            searchInput.setText("");
            clearSearchResults();
        }
    }
    
    /**
     * إزالة مكتبة من القائمة المحددة
     */
    private void removeLibrary(Library library) {
        selectedLibraries.remove(library);
        updateUI();
    }
    
    /**
     * مسح جميع المكتبات المحددة
     */
    private void clearAll() {
        selectedLibraries.clear();
        updateUI();
        clearSearchResults();
    }
    
    /**
     * بدء عملية التثبيت
     */
    private void startInstallation() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("تثبيت المكتبات");
        progressDialog.setMessage("تحضير التثبيت...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.show();
        
        // تنفيذ التثبيت في مهمة منفصلة
        new InstallTask(progressDialog).execute();
    }
    
    /**
     * فئة البحث عن المكتبات
     */
    private class SearchTask extends AsyncTask<String, Void, List<Library>> {
        private String query;
        
        @Override
        protected void onPreExecute() {
            searchProgressBar.setVisibility(View.VISIBLE);
        }
        
        @Override
        protected List<Library> doInBackground(String... strings) {
            query = strings[0];
            return searchLibraries(query);
        }
        
        @Override
        protected void onPostExecute(List<Library> results) {
            searchProgressBar.setVisibility(View.GONE);
            
            // فلترة النتائج - استبعاد المكتبات المختارة
            List<Library> filteredResults = new ArrayList<>();
            for (Library lib : results) {
                boolean alreadySelected = false;
                for (Library selected : selectedLibraries) {
                    if (selected.getName().equals(lib.getName())) {
                        alreadySelected = true;
                        break;
                    }
                }
                if (!alreadySelected) {
                    filteredResults.add(lib);
                }
            }
            
            searchResults.clear();
            searchResults.addAll(filteredResults);
            searchAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * البحث عن المكتبات (محاكاة للبحث من PyPI)
     */
    private List<Library> searchLibraries(String query) {
        List<Library> results = new ArrayList<>();
        
        // قاعدة بيانات المكتبات الشائعة للبحث
        Map<String, String[]> popularLibraries = new HashMap<>();
        popularLibraries.put("numpy", new String[]{"مكتبة الحوسبة العلمية", "1.21.0"});
        popularLibraries.put("pandas", new String[]{"مكتبة تحليل البيانات", "1.3.0"});
        popularLibraries.put("matplotlib", new String[]{"مكتبة الرسم البياني", "3.4.2"});
        popularLibraries.put("requests", new String[]{"مكتبة HTTP للبايثون", "2.26.0"});
        popularLibraries.put("flask", new String[]{"إطار عمل ويب", "2.0.1"});
        popularLibraries.put("django", new String[]{"إطار عمل ويب كامل", "3.2.5"});
        popularLibraries.put("tensorflow", new String[]{"مكتبة التعلم الآلي", "2.6.0"});
        popularLibraries.put("scikit-learn", new String[]{"مكتبة التعلم الآلي", "0.24.2"});
        popularLibraries.put("pillow", new String[]{"مكتبة معالجة الصور", "8.3.1"});
        popularLibraries.put("beautifulsoup4", new String[]{"مكتبة تحليل HTML", "4.10.0"});
        popularLibraries.put("selenium", new String[]{"مكتبة اختبار الويب", "3.141.0"});
        popularLibraries.put("pymongo", new String[]{"مكتبة MongoDB", "3.12.1"});
        popularLibraries.put("sqlalchemy", new String[]{"مكتبة قاعدة البيانات", "1.4.23"});
        popularLibraries.put("pytest", new String[]{"مكتبة الاختبار", "6.2.5"});
        popularLibraries.put("pygame", new String[]{"مكتبة الألعاب", "1.9.6"});
        popularLibraries.put("openpyxl", new String[]{"مكتبة Excel", "3.0.9"});
        popularLibraries.put("matplotlib", new String[]{"مكتبة الرسم البياني", "3.4.2"});
        popularLibraries.put("plotly", new String[]{"مكتبة التفاعل", "5.1.0"});
        popularLibraries.put("fastapi", new String[]{"إطار عمل API السريع", "0.68.0"});
        
        // البحث في النتائج
        for (Map.Entry<String, String[]> entry : popularLibraries.entrySet()) {
            if (entry.getKey().toLowerCase().contains(query.toLowerCase()) ||
                entry.getValue()[0].toLowerCase().contains(query.toLowerCase())) {
                results.add(createLibrary(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
            }
        }
        
        return results;
    }
    
    /**
     * إنشاء مكتبة من البيانات
     */
    private Library createLibrary(String name, String description, String version) {
        return new Library(name, description, version, false, "MIT", 
                dependencyGraph.getOrDefault(name, new ArrayList<>()), 
                new ArrayList<>(), "");
    }
    
    /**
     * فئة التثبيت
     */
    private class InstallTask extends AsyncTask<Void, Integer, Boolean> {
        private ProgressDialog progressDialog;
        private List<String> librariesToInstall;
        private List<String> dependenciesToInstall;
        
        public InstallTask(ProgressDialog progressDialog) {
            this.progressDialog = progressDialog;
        }
        
        @Override
        protected void onPreExecute() {
            librariesToInstall = new ArrayList<>();
            dependenciesToInstall = new ArrayList<>(allDependencies);
            
            for (Library lib : selectedLibraries) {
                librariesToInstall.add(lib.getName());
            }
            
            progressDialog.setMax(librariesToInstall.size() + dependenciesToInstall.size());
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                int totalProgress = 0;
                
                // تثبيت التبعيات أولاً
                for (String dep : dependenciesToInstall) {
                    publishProgress(totalProgress);
                    totalProgress++;
                    
                    // محاكاة تثبيت المكتبة
                    Thread.sleep(500);
                    
                    if (!installLibrary(dep)) {
                        return false;
                    }
                }
                
                // تثبيت المكتبات الرئيسية
                for (String lib : librariesToInstall) {
                    publishProgress(totalProgress);
                    totalProgress++;
                    
                    Thread.sleep(1000);
                    
                    if (!installLibrary(lib)) {
                        return false;
                    }
                }
                
                return true;
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            progressDialog.dismiss();
            
            if (success) {
                setResult(RESULT_OK);
                finish();
                Toast.makeText(InstallLibrariesActivity.this, 
                        "تم تثبيت المكتبات بنجاح!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(InstallLibrariesActivity.this, 
                        "فشل في تثبيت المكتبات", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * تثبيت مكتبة (محاكاة)
     */
    private boolean installLibrary(String libraryName) {
        try {
            // هنا يتم تنفيذ أمر pip install الفعلي
            // Process process = Runtime.getRuntime().exec("pip install " + libraryName);
            // process.waitFor();
            
            return true; // للاختبار
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}