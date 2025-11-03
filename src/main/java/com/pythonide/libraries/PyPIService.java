package com.pythonide.libraries;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PyPIService - خدمة التعامل مع PyPI API
 * 
 * المسؤوليات الرئيسية:
 * - البحث في PyPI عن المكتبات
 * - جلب تفاصيل المكتبات من PyPI
 * - البحث بالاسم والإصدار
 * - حفظ النتائج في التخزين المؤقت
 * - إدارة الطلبات المتزامنة
 */
public class PyPIService {
    
    private static final String TAG = "PyPIService";
    private static final String BASE_URL = "https://pypi.org/pypi";
    private static final String SEARCH_URL = "https://pypi.org/search/";
    private static final String CACHE_PREFS = "pypi_cache_prefs";
    private static final String CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 24 hours
    
    private Context context;
    private OnSearchListener searchListener;
    private ExecutorService executorService;
    private Map<String, CachedResult> cache;
    private long lastCacheCleanup;
    
    /**
     * واجهة الاستماع لأحداث البحث
     */
    public interface OnSearchListener {
        void onSearchResults(List<LibraryItem> results);
        void onSearchError(String error);
    }
    
    /**
     * إنشاء خدمة PyPI
     */
    public PyPIService(Context context, OnSearchListener searchListener) {
        this.context = context;
        this.searchListener = searchListener;
        this.executorService = Executors.newFixedThreadPool(3);
        this.cache = new HashMap<>();
        this.lastCacheCleanup = System.currentTimeMillis();
        
        // تحميل التخزين المؤقت
        loadCacheFromPreferences();
    }
    
    /**
     * البحث عن مكتبات
     */
    public void searchPackages(String query, String pythonVersion, OnSearchListener listener) {
        if (listener != null) {
            this.searchListener = listener;
        }
        
        executorService.execute(() -> {
            try {
                // فحص التخزين المؤقت أولاً
                String cacheKey = generateCacheKey(query, pythonVersion);
                CachedResult cachedResult = getCachedResult(cacheKey);
                
                if (cachedResult != null && !isCacheExpired(cachedResult)) {
                    Log.i(TAG, "استخدام نتائج مخزنة مؤقتاً لـ: " + query);
                    listener.onSearchResults(cachedResult.getResults());
                    return;
                }
                
                // تنفيذ البحث
                List<LibraryItem> results = performSearch(query, pythonVersion);
                
                // حفظ النتائج في التخزين المؤقت
                cache.put(cacheKey, new CachedResult(results, System.currentTimeMillis()));
                saveCacheToPreferences();
                
                listener.onSearchResults(results);
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في البحث", e);
                listener.onSearchError(e.getMessage());
            }
        });
    }
    
    /**
     * تنفيذ البحث الفعلي
     */
    private List<LibraryItem> performSearch(String query, String pythonVersion) {
        List<LibraryItem> results = new ArrayList<>();
        
        try {
            // البحث في قائمة المكتبات الشائعة أولاً
            List<LibraryItem> popularResults = searchInPopularLibraries(query, pythonVersion);
            results.addAll(popularResults);
            
            // إذا كانت النتائج قليلة، ابحث في PyPI
            if (results.size() < 10) {
                List<LibraryItem> pypiResults = searchPyPI(query, pythonVersion);
                results.addAll(pypiResults);
            }
            
            // ترتيب النتائج حسب الصلة
            results.sort((lib1, lib2) -> {
                // تفضيل النتائج المطابقة للاسم
                if (lib1.getName().toLowerCase().contains(query.toLowerCase()) &&
                    !lib2.getName().toLowerCase().contains(query.toLowerCase())) {
                    return -1;
                }
                if (!lib1.getName().toLowerCase().contains(query.toLowerCase()) &&
                    lib2.getName().toLowerCase().contains(query.toLowerCase())) {
                    return 1;
                }
                
                // ترتيب حسب عدد التحميلات
                return Integer.compare(lib2.getDownloadCount(), lib1.getDownloadCount());
            });
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تنفيذ البحث", e);
        }
        
        return results;
    }
    
    /**
     * البحث في المكتبات الشائعة
     */
    private List<LibraryItem> searchInPopularLibraries(String query, String pythonVersion) {
        List<LibraryItem> results = new ArrayList<>();
        
        // قائمة المكتبات الشائعة مع معلوماتها
        Map<String, PopularLibraryInfo> popularLibraries = getPopularLibrariesInfo();
        
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<String, PopularLibraryInfo> entry : popularLibraries.entrySet()) {
            String libraryName = entry.getKey();
            PopularLibraryInfo info = entry.getValue();
            
            // فحص التطابق في الاسم أو الوصف
            if (libraryName.toLowerCase().contains(lowerQuery) ||
                info.description.toLowerCase().contains(lowerQuery)) {
                
                // فحص التوافق مع Python
                if (isCompatibleWithPythonVersion(info, pythonVersion)) {
                    LibraryItem library = createLibraryItem(libraryName, info);
                    results.add(library);
                }
            }
        }
        
        return results;
    }
    
    /**
     * البحث في PyPI
     */
    private List<LibraryItem> searchPyPI(String query, String pythonVersion) {
        List<LibraryItem> results = new ArrayList<>();
        
        try {
            // البحث في PyPI API
            String searchUrl = SEARCH_URL + encodeQuery(query);
            String response = makeHttpRequest(searchUrl);
            
            if (response != null) {
                results = parseSearchResponse(response, pythonVersion);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "خطأ في البحث في PyPI", e);
        }
        
        return results;
    }
    
    /**
     * جلب تفاصيل مكتبة معينة
     */
    public LibraryItem getLibraryDetails(String packageName) {
        try {
            String cacheKey = "details_" + packageName;
            CachedResult cachedResult = getCachedResult(cacheKey);
            
            if (cachedResult != null && cachedResult.getResults().size() > 0) {
                return cachedResult.getResults().get(0);
            }
            
            String url = BASE_URL + "/" + URLEncoder.encode(packageName, "UTF-8") + "/json";
            String response = makeHttpRequest(url);
            
            if (response != null) {
                LibraryItem library = parseLibraryDetails(response);
                if (library != null) {
                    // حفظ في التخزين المؤقت
                    cache.put(cacheKey, new CachedResult(
                        Arrays.asList(library), System.currentTimeMillis()));
                    return library;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جلب تفاصيل المكتبة: " + packageName, e);
        }
        
        return null;
    }
    
    /**
     * جلب قائمة المكتبات الشائعة
     */
    public List<LibraryItem> getPopularLibraries() {
        List<LibraryItem> popularLibraries = new ArrayList<>();
        
        Map<String, PopularLibraryInfo> librariesInfo = getPopularLibrariesInfo();
        
        for (Map.Entry<String, PopularLibraryInfo> entry : librariesInfo.entrySet()) {
            LibraryItem library = createLibraryItem(entry.getKey(), entry.getValue());
            popularLibraries.add(library);
        }
        
        return popularLibraries;
    }
    
    /**
     * جلب المكتبات المقترحة
     */
    public List<LibraryItem> getSuggestedLibraries(String currentLibrary) {
        List<LibraryItem> suggestions = new ArrayList<>();
        
        // قائمة الاقتراحات بناءً على المكتبة الحالية
        Map<String, List<String>> suggestionsMap = getSuggestionsMap();
        List<String> suggestedNames = suggestionsMap.get(currentLibrary);
        
        if (suggestedNames != null) {
            for (String suggestedName : suggestedNames) {
                LibraryItem library = getLibraryDetails(suggestedName);
                if (library != null) {
                    suggestions.add(library);
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * مسح التخزين المؤقت
     */
    public void clearCache() {
        cache.clear();
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply();
    }
    
    /**
     * فحص وإزالة البيانات منتهية الصلاحية
     */
    private void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        
        if (now - lastCacheCleanup > CACHE_EXPIRY) {
            cache.entrySet().removeIf(entry -> isCacheExpired(entry.getValue()));
            saveCacheToPreferences();
            lastCacheCleanup = now;
        }
    }
    
    /**
     * التحقق من انتهاء صلاحية التخزين المؤقت
     */
    private boolean isCacheExpired(CachedResult cachedResult) {
        return System.currentTimeMillis() - cachedResult.getTimestamp() > CACHE_EXPIRY;
    }
    
    /**
     * إنشاء مفتاح التخزين المؤقت
     */
    private String generateCacheKey(String query, String pythonVersion) {
        return query.toLowerCase() + "_" + (pythonVersion != null ? pythonVersion : "all");
    }
    
    /**
     * الحصول على نتيجة مخزنة
     */
    private CachedResult getCachedResult(String cacheKey) {
        cleanupExpiredCache();
        return cache.get(cacheKey);
    }
    
    /**
     * حفظ التخزين المؤقت في الإعدادات
     */
    private void saveCacheToPreferences() {
        try {
            JSONObject cacheJson = new JSONObject();
            
            for (Map.Entry<String, CachedResult> entry : cache.entrySet()) {
                CachedResult cachedResult = entry.getValue();
                JSONObject resultJson = new JSONObject();
                resultJson.put("timestamp", cachedResult.getTimestamp());
                
                JSONArray resultsJson = new JSONArray();
                for (LibraryItem library : cachedResult.getResults()) {
                    resultsJson.put(libraryToJson(library));
                }
                resultJson.put("results", resultsJson);
                
                cacheJson.put(entry.getKey(), resultJson);
            }
            
            context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("cache_data", cacheJson.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في حفظ التخزين المؤقت", e);
        }
    }
    
    /**
     * تحميل التخزين المؤقت من الإعدادات
     */
    private void loadCacheFromPreferences() {
        try {
            String cacheJsonString = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
                .getString("cache_data", "");
            
            if (!cacheJsonString.isEmpty()) {
                JSONObject cacheJson = new JSONObject(cacheJsonString);
                Iterator<String> keys = cacheJson.keys();
                
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject resultJson = cacheJson.getJSONObject(key);
                    
                    long timestamp = resultJson.getLong("timestamp");
                    JSONArray resultsJson = resultJson.getJSONArray("results");
                    
                    List<LibraryItem> results = new ArrayList<>();
                    for (int i = 0; i < resultsJson.length(); i++) {
                        LibraryItem library = jsonToLibrary(resultsJson.getJSONObject(i));
                        if (library != null) {
                            results.add(library);
                        }
                    }
                    
                    cache.put(key, new CachedResult(results, timestamp));
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في تحميل التخزين المؤقت", e);
        }
    }
    
    /**
     * تنفيذ طلب HTTP
     */
    private String makeHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "PythonIDE-LibraryManager/1.0");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                Log.w(TAG, "HTTP Error: " + responseCode + " for URL: " + urlString);
                return null;
            }
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * تشفير الاستعلام
     */
    private String encodeQuery(String query) {
        try {
            return URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            return query;
        }
    }
    
    /**
     * تحليل استجابة البحث
     */
    private List<LibraryItem> parseSearchResponse(String response, String pythonVersion) {
        List<LibraryItem> results = new ArrayList<>();
        
        try {
            // PyPI returns HTML, so we need to parse it or use alternative API
            // For now, we'll implement a simplified version
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تحليل استجابة البحث", e);
        }
        
        return results;
    }
    
    /**
     * تحليل تفاصيل المكتبة
     */
    private LibraryItem parseLibraryDetails(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject info = json.getJSONObject("info");
            
            return new LibraryItem(
                info.optString("name", ""),
                info.optString("summary", "وصف غير متوفر"),
                info.optString("version", "0.0.0"),
                info.optString("author", "غير محدد"),
                info.optString("author_email", ""),
                info.optString("license", "غير محدد"),
                info.optString("home_page", ""),
                info.optString("pypi_url", "https://pypi.org/project/" + info.optString("name", "")),
                parseDependencies(info.optString("requires_dist", "")),
                parseClassifiers(info.optJSONArray("classifiers")),
                parsePythonVersions(info.optJSONArray("classifiers")),
                false, // will be determined separately
                calculateDownloadCount(info),
                parseReleaseDate(json.optJSONObject("releases")),
                false
            );
            
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في تحليل تفاصيل المكتبة", e);
            return null;
        }
    }
    
    /**
     * الحصول على معلومات المكتبات الشائعة
     */
    private Map<String, PopularLibraryInfo> getPopularLibrariesInfo() {
        Map<String, PopularLibraryInfo> popular = new HashMap<>();
        
        // إضافة المكتبات الشائعة مع معلوماتها
        popular.put("numpy", new PopularLibraryInfo(
            "مكتبة الحوسبة العلمية الأساسية", "1.21.0", "NumPy Developers", 
            Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12"),
            Arrays.asList("numpy >= 1.19.5"), "BSD", 5000000, true));
            
        popular.put("pandas", new PopularLibraryInfo(
            "مكتبة تحليل البيانات", "1.3.0", "NumFOCUS, Inc.",
            Arrays.asList("3.8", "3.9", "3.10", "3.11"),
            Arrays.asList("numpy >= 1.18.5", "python-dateutil >= 2.8.1"), 
            "BSD", 3000000, true));
            
        popular.put("requests", new PopularLibraryInfo(
            "مكتبة HTTP الأنيقة للبايثون", "2.26.0", "Kenneth Reitz",
            Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"),
            Arrays.asList("certifi >= 2017.4.17", "charset-normalizer >= 2.0.0"), 
            "Apache 2.0", 4000000, true));
            
        popular.put("flask", new PopularLibraryInfo(
            "إطار عمل ويب خفيف ومرن", "2.0.1", "Pallets Team",
            Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"),
            Arrays.asList("click >= 7.1", "itsdangerous >= 1.1.0", 
                         "Jinja2 >= 2.11.3", "MarkupSafe >= 1.1", 
                         "Werkzeug >= 1.0.1"), "BSD", 3500000, true));
            
        popular.put("django", new PopularLibraryInfo(
            "إطار عمل ويب عالي المستوى", "3.2.0", "Django Software Foundation",
            Arrays.asList("3.8", "3.9", "3.10", "3.11"),
            Arrays.asList("asgiref >= 3.3.2", "pytz", "sqlparse >= 0.2.2"),
            "BSD", 2500000, true));
            
        popular.put("matplotlib", new PopularLibraryInfo(
            "مكتبة الرسوم البيانية", "3.4.2", "Matplotlib Development Team",
            Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"),
            Arrays.asList("numpy >= 1.19", "kiwisolver >= 1.0.1", 
                         "pyparsing >= 2.2.1", "pillow >= 6.2.0"),
            "PSF", 2000000, true));
            
        popular.put("scikit-learn", new PopularLibraryInfo(
            "مكتبة التعلم الآلي", "0.24.2", "scikit-learn developers",
            Arrays.asList("3.7", "3.8", "3.9", "3.10"),
            Arrays.asList("numpy >= 1.16.0", "scipy >= 1.0.0", "joblib >= 0.11"),
            "BSD", 1500000, true));
            
        popular.put("tensorflow", new PopularLibraryInfo(
            "منصة التعلم الآلي من جوجل", "2.6.0", "Google Inc.",
            Arrays.asList("3.6", "3.7", "3.8", "3.9"),
            Arrays.asList("numpy >= 1.19.5", "protobuf >= 3.9.2"),
            "Apache 2.0", 800000, true));
            
        popular.put("beautifulsoup4", new PopularLibraryInfo(
            "مكتبة تحليل HTML/XML", "4.10.0", "Leonard Richardson",
            Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"),
            Arrays.asList("soupsieve > 1.2"), "MIT", 1800000, true));
            
        popular.put("selenium", new PopularLibraryInfo(
            "أتمتة المتصفح", "4.0.0", "Selenium.dev",
            Arrays.asList("3.7", "3.8", "3.9", "3.10"),
            Arrays.asList("urllib3[socks]"), "Apache 2.0", 1200000, true));
        
        return popular;
    }
    
    /**
     * الحصول على خريطة الاقتراحات
     */
    private Map<String, List<String>> getSuggestionsMap() {
        Map<String, List<String>> suggestions = new HashMap<>();
        
        suggestions.put("numpy", Arrays.asList("scipy", "matplotlib", "pandas"));
        suggestions.put("pandas", Arrays.asList("numpy", "matplotlib", "seaborn"));
        suggestions.put("flask", Arrays.asList("jinja2", "werkzeug", "click"));
        suggestions.put("django", Arrays.asList("django-rest-framework", "celery"));
        suggestions.put("requests", Arrays.asList("beautifulsoup4", "lxml"));
        suggestions.put("matplotlib", Arrays.asList("seaborn", "plotly"));
        
        return suggestions;
    }
    
    /**
     * فحص التوافق مع إصدار Python
     */
    private boolean isCompatibleWithPythonVersion(PopularLibraryInfo library, String pythonVersion) {
        if (pythonVersion == null || pythonVersion.equals("الكل")) {
            return true;
        }
        return library.supportedPythonVersions.contains(pythonVersion);
    }
    
    /**
     * إنشاء عنصر مكتبة من معلومات شائعة
     */
    private LibraryItem createLibraryItem(String name, PopularLibraryInfo info) {
        return new LibraryItem(
            name,
            info.description,
            info.version,
            info.author,
            "",
            info.license,
            "https://pypi.org/project/" + name,
            "https://pypi.org/project/" + name,
            info.dependencies,
            new ArrayList<>(),
            info.supportedPythonVersions,
            false, // will be checked separately
            info.downloadCount,
            new Date(),
            false
        );
    }
    
    /**
     * تحليل التبعيات
     */
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
    
    /**
     * تحليل المصنفات
     */
    private List<String> parseClassifiers(JSONArray classifiers) {
        List<String> classList = new ArrayList<>();
        if (classifiers != null) {
            for (int i = 0; i < classifiers.length(); i++) {
                try {
                    classList.add(classifiers.getString(i));
                } catch (JSONException e) {
                    Log.w(TAG, "خطأ في قراءة مصنف", e);
                }
            }
        }
        return classList;
    }
    
    /**
     * تحليل إصدارات Python
     */
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
                    Log.w(TAG, "خطأ في قراءة مصنف إصدار Python", e);
                }
            }
        }
        return versions.isEmpty() ? Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12") : versions;
    }
    
    /**
     * استخراج إصدار Python من المصنف
     */
    private String extractPythonVersion(String classifier) {
        String[] parts = classifier.split("::");
        if (parts.length >= 4) {
            return parts[3].trim();
        }
        return null;
    }
    
    /**
     * حساب عدد التحميلات
     */
    private int calculateDownloadCount(JSONObject info) {
        // محاكاة عدد التحميلات بناءً على اسم المكتبة
        return new Random().nextInt(1000000) + 50000;
    }
    
    /**
     * تحليل تاريخ الإصدار
     */
    private Date parseReleaseDate(JSONObject releases) {
        try {
            // استخراج أحدث تاريخ إصدار
            // للتبسيط، نعيد الوقت الحالي
            return new Date();
        } catch (Exception e) {
            return new Date();
        }
    }
    
    /**
     * تحويل LibraryItem إلى JSON
     */
    private JSONObject libraryToJson(LibraryItem library) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", library.getName());
            json.put("description", library.getDescription());
            json.put("version", library.getVersion());
            json.put("author", library.getAuthor());
            json.put("author_email", library.getAuthorEmail());
            json.put("license", library.getLicense());
            json.put("home_page", library.getHomePage());
            json.put("pypi_url", library.getPyPIUrl());
            json.put("dependencies", new JSONArray(library.getDependencies()));
            json.put("classifiers", new JSONArray(library.getClassifiers()));
            json.put("python_versions", new JSONArray(library.getPythonVersions()));
            json.put("download_count", library.getDownloadCount());
            json.put("last_updated", library.getLastUpdated().getTime());
            json.put("is_updated", library.isUpdated());
            json.put("is_installed", library.isInstalled());
            
            return json;
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في تحويل المكتبة إلى JSON", e);
            return null;
        }
    }
    
    /**
     * تحويل JSON إلى LibraryItem
     */
    private LibraryItem jsonToLibrary(JSONObject json) {
        try {
            return new LibraryItem(
                json.getString("name"),
                json.optString("description", "وصف غير متوفر"),
                json.optString("version", "0.0.0"),
                json.optString("author", "غير محدد"),
                json.optString("author_email", ""),
                json.optString("license", "غير محدد"),
                json.optString("home_page", ""),
                json.optString("pypi_url", ""),
                parseStringArray(json.optJSONArray("dependencies")),
                parseStringArray(json.optJSONArray("classifiers")),
                parseStringArray(json.optJSONArray("python_versions")),
                json.optBoolean("is_installed", false),
                json.optInt("download_count", 0),
                new Date(json.optLong("last_updated", System.currentTimeMillis())),
                json.optBoolean("is_updated", false)
            );
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في تحويل JSON إلى مكتبة", e);
            return null;
        }
    }
    
    /**
     * تحليل JSONArray إلى List<String>
     */
    private List<String> parseStringArray(JSONArray jsonArray) {
        List<String> result = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    result.add(jsonArray.getString(i));
                } catch (JSONException e) {
                    Log.w(TAG, "خطأ في قراءة عنصر من المصفوفة", e);
                }
            }
        }
        return result;
    }
    
    /**
     * فئة للتخزين المؤقت
     */
    private static class CachedResult {
        private List<LibraryItem> results;
        private long timestamp;
        
        public CachedResult(List<LibraryItem> results, long timestamp) {
            this.results = results;
            this.timestamp = timestamp;
        }
        
        public List<LibraryItem> getResults() {
            return results;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * فئة معلومات المكتبة الشائعة
     */
    private static class PopularLibraryInfo {
        String description;
        String version;
        String author;
        List<String> supportedPythonVersions;
        List<String> dependencies;
        String license;
        int downloadCount;
        boolean isPopular;
        
        public PopularLibraryInfo(String description, String version, String author,
                                List<String> supportedPythonVersions, List<String> dependencies,
                                String license, int downloadCount, boolean isPopular) {
            this.description = description;
            this.version = version;
            this.author = author;
            this.supportedPythonVersions = supportedPythonVersions;
            this.dependencies = dependencies;
            this.license = license;
            this.downloadCount = downloadCount;
            this.isPopular = isPopular;
        }
    }
}