package com.pythonide.libraries;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PackageManager - مدير تثبيت وإلغاء تثبيت المكتبات
 * 
 * المسؤوليات الرئيسية:
 * - تثبيت المكتبات باستخدام pip
 * - إلغاء تثبيت المكتبات
 * - إدارة التبعيات التلقائية
 * - عرض تقدم التثبيت
 * - حفظ حالة التثبيت محلياً
 * - إدارة إصدارات Python المختلفة
 */
public class PackageManager {
    
    private static final String TAG = "PackageManager";
    private static final String PREFS_NAME = "package_manager_prefs";
    private static final String INSTALLED_LIBRARIES_KEY = "installed_libraries";
    private static final String INSTALLATION_LOGS_KEY = "installation_logs";
    
    private Context context;
    private OnInstallationListener listener;
    private ExecutorService executorService;
    private SharedPreferences sharedPreferences;
    private PythonExecutor pythonExecutor;
    
    /**
     * واجهة الاستماع لأحداث التثبيت
     */
    public interface OnInstallationListener {
        void onInstallationStart(String packageName);
        void onInstallationProgress(String packageName, int progress, String currentStep);
        void onInstallationComplete(String packageName, boolean success, String message);
    }
    
    /**
     * إنشاء مدير المكتبات
     */
    public PackageManager(Context context, OnInstallationListener listener) {
        this.context = context;
        this.listener = listener;
        this.executorService = Executors.newFixedThreadPool(2);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.pythonExecutor = new PythonExecutor(context);
    }
    
    /**
     * تثبيت مكتبة مع خيارات متقدمة
     */
    public void installPackage(InstallOptions options, OnInstallationListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
        
        executorService.execute(() -> {
            try {
                listener.onInstallationStart(options.getPackageName());
                
                // خطوة 1: فحص التوافق
                updateProgress(options.getPackageName(), 10, "فحص توافق المكتبة");
                
                if (options.isCheckCompatible() && options.getPythonVersion() != null) {
                    if (!checkCompatibility(options)) {
                        listener.onInstallationComplete(options.getPackageName(), false, 
                            "المكتبة غير متوافقة مع Python " + options.getPythonVersion());
                        return;
                    }
                }
                
                // خطوة 2: حل التبعيات
                updateProgress(options.getPackageName(), 30, "حل التبعيات");
                List<String> dependencies = new ArrayList<>();
                
                if (options.isInstallDependencies()) {
                    dependencies = resolveDependencies(options.getPackageName());
                }
                
                // خطوة 3: إنشاء قائمة التثبيت
                updateProgress(options.getPackageName(), 50, "تحضير التثبيت");
                List<String> packagesToInstall = new ArrayList<>();
                packagesToInstall.add(options.getPackageName());
                packagesToInstall.addAll(dependencies);
                
                // خطوة 4: تثبيت كل مكتبة
                List<String> failedPackages = new ArrayList<>();
                
                for (int i = 0; i < packagesToInstall.size(); i++) {
                    String packageName = packagesToInstall.get(i);
                    int progress = 50 + (i * 40 / packagesToInstall.size());
                    updateProgress(options.getPackageName(), progress, 
                        "تثبيت " + packageName + " (" + (i + 1) + "/" + packagesToInstall.size() + ")");
                    
                    boolean success = installSinglePackage(packageName, options);
                    if (!success) {
                        failedPackages.add(packageName);
                    }
                    
                    // حفظ حالة التثبيت
                    saveInstallationStatus(packageName, success);
                }
                
                // خطوة 5: التحقق النهائي
                updateProgress(options.getPackageName(), 95, "التحقق من التثبيت");
                boolean finalSuccess = verifyInstallation(options.getPackageName());
                
                if (finalSuccess && failedPackages.isEmpty()) {
                    listener.onInstallationComplete(options.getPackageName(), true, 
                        "تم التثبيت بنجاح");
                } else {
                    String errorMessage = failedPackages.isEmpty() ? 
                        "فشل في التحقق من التثبيت" : 
                        "فشل في تثبيت: " + String.join(", ", failedPackages);
                    listener.onInstallationComplete(options.getPackageName(), false, errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في التثبيت", e);
                listener.onInstallationComplete(options.getPackageName(), false, 
                    "خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * تثبيت مكتبة واحدة
     */
    private boolean installSinglePackage(String packageName, InstallOptions options) {
        try {
            // بناء أمر pip install
            List<String> command = new ArrayList<>();
            command.add("pip");
            command.add("install");
            
            // إضافة معاملات الإصدار
            if (options.getVersion() != null) {
                command.add(packageName + "==" + options.getVersion());
            } else {
                command.add("latest");
                command.add(packageName);
            }
            
            // إضافة معاملات إضافية
            command.add("--user"); // تثبيت في مجلد المستخدم
            command.add("--no-warn-script-location"); // تجاهل تحذيرات الموقع
            
            // تنفيذ الأمر
            int exitCode = pythonExecutor.executeCommand(command, options.getPythonVersion());
            
            if (exitCode == 0) {
                Log.i(TAG, "تم تثبيت " + packageName + " بنجاح");
                return true;
            } else {
                Log.e(TAG, "فشل في تثبيت " + packageName);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تثبيت " + packageName, e);
            return false;
        }
    }
    
    /**
     * إلغاء تثبيت مكتبة
     */
    public boolean uninstallPackage(String packageName) {
        try {
            Log.i(TAG, "بدء إلغاء تثبيت " + packageName);
            
            // تنفيذ أمر إلغاء التثبيت
            List<String> command = Arrays.asList("pip", "uninstall", "-y", packageName);
            int exitCode = pythonExecutor.executeCommand(command, null);
            
            if (exitCode == 0) {
                // إزالة من التخزين المحلي
                removeFromInstalledLibraries(packageName);
                
                // إزالة التبعيات غير المستخدمة (اختياري)
                removeUnusedDependencies(packageName);
                
                Log.i(TAG, "تم إلغاء تثبيت " + packageName + " بنجاح");
                return true;
            } else {
                Log.e(TAG, "فشل في إلغاء تثبيت " + packageName);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إلغاء تثبيت " + packageName, e);
            return false;
        }
    }
    
    /**
     * إلغاء تثبيت عدة مكتبات
     */
    public List<String> uninstallMultiplePackages(List<String> packageNames) {
        List<String> failedPackages = new ArrayList<>();
        
        for (String packageName : packageNames) {
            if (!uninstallPackage(packageName)) {
                failedPackages.add(packageName);
            }
        }
        
        return failedPackages;
    }
    
    /**
     * الحصول على المكتبات المثبتة
     */
    public List<LibraryItem> getInstalledLibraries() {
        List<LibraryItem> libraries = new ArrayList<>();
        
        try {
            String jsonString = sharedPreferences.getString(INSTALLED_LIBRARIES_KEY, "[]");
            JSONArray jsonArray = new JSONArray(jsonString);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject libraryJson = jsonArray.getJSONObject(i);
                LibraryItem library = parseLibraryFromJson(libraryJson);
                if (library != null) {
                    library.setInstalled(true);
                    libraries.add(library);
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في قراءة المكتبات المثبتة", e);
        }
        
        return libraries;
    }
    
    /**
     * تحديث المكتبات المثبتة
     */
    public void refreshInstalledLibraries() {
        executorService.execute(() -> {
            try {
                // تنفيذ pip list للحصول على قائمة المكتبات المثبتة
                List<String> command = Arrays.asList("pip", "list", "--format=json");
                String output = pythonExecutor.executeCommandWithOutput(command, null);
                
                if (output != null && !output.isEmpty()) {
                    JSONArray packageList = new JSONArray(output);
                    List<LibraryItem> installed = new ArrayList<>();
                    
                    for (int i = 0; i < packageList.length(); i++) {
                        JSONObject packageJson = packageList.getJSONObject(i);
                        String name = packageJson.getString("name");
                        String version = packageJson.getString("version");
                        
                        LibraryItem library = new LibraryItem(
                            name, "مكتبة مثبتة", version, 
                            "غير محدد", "", "غير محدد", "", "", 
                            new ArrayList<>(), new ArrayList<>(), 
                            Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12"), 
                            true, 0, new Date(), false
                        );
                        
                        installed.add(library);
                    }
                    
                    // حفظ القائمة المحدثة
                    saveInstalledLibraries(installed);
                    
                }
                
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحديث المكتبات المثبتة", e);
            }
        });
    }
    
    /**
     * حل التبعيات للمكتبة
     */
    private List<String> resolveDependencies(String packageName) {
        List<String> dependencies = new ArrayList<>();
        
        try {
            // الحصول على معلومات المكتبة من PyPI
            PyPIService pyPIService = new PyPIService(context, null);
            LibraryItem library = pyPIService.getLibraryDetails(packageName);
            
            if (library != null && !library.getDependencies().isEmpty()) {
                for (String dependency : library.getDependencies()) {
                    String cleanDep = parseDependencyName(dependency);
                    if (!cleanDep.isEmpty() && !dependencies.contains(cleanDep)) {
                        dependencies.add(cleanDep);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "خطأ في حل التبعيات لـ " + packageName, e);
        }
        
        return dependencies;
    }
    
    /**
     * فحص توافق المكتبة
     */
    private boolean checkCompatibility(InstallOptions options) {
        try {
            PyPIService pyPIService = new PyPIService(context, null);
            LibraryItem library = pyPIService.getLibraryDetails(options.getPackageName());
            
            if (library == null) {
                return true; // افتراض التوافق إذا لم نتمكن من الحصول على المعلومات
            }
            
            List<String> supportedVersions = library.getPythonVersions();
            return supportedVersions.contains(options.getPythonVersion()) || 
                   options.getPythonVersion().equals("الكل");
                   
        } catch (Exception e) {
            Log.w(TAG, "خطأ في فحص التوافق", e);
            return true; // افتراض التوافق في حالة الخطأ
        }
    }
    
    /**
     * التحقق من التثبيت
     */
    private boolean verifyInstallation(String packageName) {
        try {
            // تنفيذ pip show للتحقق من التثبيت
            List<String> command = Arrays.asList("pip", "show", packageName);
            String output = pythonExecutor.executeCommandWithOutput(command, null);
            
            return output != null && !output.isEmpty() && 
                   output.contains("Name: " + packageName);
                   
        } catch (Exception e) {
            Log.e(TAG, "خطأ في التحقق من التثبيت", e);
            return false;
        }
    }
    
    /**
     * تحديث تقدم التثبيت
     */
    private void updateProgress(String packageName, int progress, String step) {
        if (listener != null) {
            listener.onInstallationProgress(packageName, progress, step);
        }
    }
    
    /**
     * حفظ حالة التثبيت
     */
    private void saveInstallationStatus(String packageName, boolean success) {
        try {
            // الحصول على المكتبات الحالية
            List<LibraryItem> installed = getInstalledLibraries();
            
            if (success) {
                // إضافة أو تحديث المكتبة
                boolean found = false;
                for (LibraryItem library : installed) {
                    if (library.getName().equals(packageName)) {
                        library.setInstalled(true);
                        library.setLastUpdated(new Date());
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    // إنشاء مكتبة جديدة
                    LibraryItem newLibrary = new LibraryItem(
                        packageName, "مكتبة مثبتة", "0.0.0",
                        "غير محدد", "", "غير محدد", "", "",
                        new ArrayList<>(), new ArrayList<>(),
                        Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12"),
                        true, 0, new Date(), false
                    );
                    installed.add(newLibrary);
                }
            } else {
                // إزالة المكتبة في حالة الفشل
                installed.removeIf(library -> library.getName().equals(packageName));
            }
            
            // حفظ القائمة المحدثة
            saveInstalledLibraries(installed);
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في حفظ حالة التثبيت", e);
        }
    }
    
    /**
     * حفظ قائمة المكتبات المثبتة
     */
    private void saveInstalledLibraries(List<LibraryItem> libraries) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (LibraryItem library : libraries) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", library.getName());
                jsonObject.put("description", library.getDescription());
                jsonObject.put("version", library.getVersion());
                jsonObject.put("author", library.getAuthor());
                jsonObject.put("author_email", library.getAuthorEmail());
                jsonObject.put("license", library.getLicense());
                jsonObject.put("home_page", library.getHomePage());
                jsonObject.put("pypi_url", library.getPyPIUrl());
                jsonObject.put("dependencies", new JSONArray(library.getDependencies()));
                jsonObject.put("classifiers", new JSONArray(library.getClassifiers()));
                jsonObject.put("python_versions", new JSONArray(library.getPythonVersions()));
                jsonObject.put("download_count", library.getDownloadCount());
                jsonObject.put("last_updated", library.getLastUpdated().getTime());
                jsonObject.put("is_updated", library.isUpdated());
                
                jsonArray.put(jsonObject);
            }
            
            sharedPreferences.edit()
                .putString(INSTALLED_LIBRARIES_KEY, jsonArray.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في حفظ المكتبات المثبتة", e);
        }
    }
    
    /**
     * إزالة مكتبة من المكتبات المثبتة
     */
    private void removeFromInstalledLibraries(String packageName) {
        List<LibraryItem> installed = getInstalledLibraries();
        installed.removeIf(library -> library.getName().equals(packageName));
        saveInstalledLibraries(installed);
    }
    
    /**
     * إزالة التبعيات غير المستخدمة
     */
    private void removeUnusedDependencies(String packageName) {
        // هذه الميزة تتطلب تحليل التبعيات المعقد
        // يمكن تنفيذها لاحقاً
    }
    
    /**
     * تحليل اسم التبعيات
     */
    private String parseDependencyName(String dependency) {
        // إزالة شروط الإصدار والرموز الخاصة
        String cleanDep = dependency.replaceAll("[<>=!~].*", "").trim();
        cleanDep = cleanDep.replaceAll("[\\[\\]\\(\\)]", "").trim();
        
        // الحصول على الاسم الأساسي فقط
        String[] parts = cleanDep.split("[\\s\\-:]+");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        
        return cleanDep;
    }
    
    /**
     * تحليل JSON إلى LibraryItem
     */
    private LibraryItem parseLibraryFromJson(JSONObject jsonObject) {
        try {
            return new LibraryItem(
                jsonObject.getString("name"),
                jsonObject.optString("description", "وصف غير متوفر"),
                jsonObject.optString("version", "0.0.0"),
                jsonObject.optString("author", "غير محدد"),
                jsonObject.optString("author_email", ""),
                jsonObject.optString("license", "غير محدد"),
                jsonObject.optString("home_page", ""),
                jsonObject.optString("pypi_url", ""),
                parseStringArray(jsonObject.optJSONArray("dependencies")),
                parseStringArray(jsonObject.optJSONArray("classifiers")),
                parseStringArray(jsonObject.optJSONArray("python_versions")),
                true, // installed
                jsonObject.optInt("download_count", 0),
                new Date(jsonObject.optLong("last_updated", System.currentTimeMillis())),
                jsonObject.optBoolean("is_updated", false)
            );
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في تحليل JSON للمكتبة", e);
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
     * حفظ سجل التثبيت
     */
    public void saveInstallationLog(String packageName, String log) {
        try {
            Map<String, String> logs = getInstallationLogs();
            logs.put(packageName + "_" + System.currentTimeMillis(), log);
            
            // الاحتفاظ بآخر 50 سجل فقط
            if (logs.size() > 50) {
                String oldestKey = logs.keySet().iterator().next();
                logs.remove(oldestKey);
            }
            
            JSONObject logsJson = new JSONObject();
            for (Map.Entry<String, String> entry : logs.entrySet()) {
                logsJson.put(entry.getKey(), entry.getValue());
            }
            
            sharedPreferences.edit()
                .putString(INSTALLATION_LOGS_KEY, logsJson.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في حفظ سجل التثبيت", e);
        }
    }
    
    /**
     * الحصول على سجلات التثبيت
     */
    public Map<String, String> getInstallationLogs() {
        Map<String, String> logs = new HashMap<>();
        
        try {
            String jsonString = sharedPreferences.getString(INSTALLATION_LOGS_KEY, "{}");
            JSONObject logsJson = new JSONObject(jsonString);
            
            Iterator<String> keys = logsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                logs.put(key, logsJson.getString(key));
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "خطأ في قراءة سجلات التثبيت", e);
        }
        
        return logs;
    }
    
    /**
     * فحص وجود تحديثات للمكتبات المثبتة
     */
    public List<LibraryItem> checkForUpdates() {
        List<LibraryItem> updatableLibraries = new ArrayList<>();
        
        try {
            List<LibraryItem> installed = getInstalledLibraries();
            PyPIService pyPIService = new PyPIService(context, null);
            
            for (LibraryItem library : installed) {
                try {
                    LibraryItem latestVersion = pyPIService.getLibraryDetails(library.getName());
                    if (latestVersion != null && 
                        !latestVersion.getVersion().equals(library.getVersion())) {
                        
                        library.setUpdated(true);
                        updatableLibraries.add(library);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "خطأ في فحص تحديث " + library.getName(), e);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "خطأ في فحص التحديثات", e);
        }
        
        return updatableLibraries;
    }
    
    /**
     * ترقية مكتبة إلى أحدث إصدار
     */
    public void upgradePackage(String packageName, OnInstallationListener listener) {
        InstallOptions options = new InstallOptions(packageName, null, true, false, true, null);
        installPackage(options, listener);
    }
    
    /**
     * تنظيف الموارد
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}