package com.pythonide.libraries;

import android.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CompatibleLibraryChecker - فحص توافق المكتبات
 * 
 * المسؤوليات الرئيسية:
 * - فحص توافق المكتبات مع إصدارات Python المختلفة
 * - فحص توافق المكتبات مع بعضها البعض
 * - تحديد أفضل إصدار من المكتبة للاستخدام
 * - كشف التعارضات بين المكتبات
 * - اقتراح البدائل للمكتبات غير المتوافقة
 */
public class CompatibleLibraryChecker {
    
    private static final String TAG = "CompatibleLibraryChecker";
    
    // قواعد التوافق
    private Map<String, Set<String>> pythonCompatibilityRules;
    private Map<String, Map<String, String>> libraryConflictRules;
    private Map<String, List<String>> libraryRecommendations;
    private Map<String, String> deprecatedLibraries;
    
    // إعدادات التوافق
    private String currentPythonVersion;
    private boolean strictCompatibility;
    private boolean enableConflictDetection;
    
    /**
     * إنشاء فاحص التوافق
     */
    public CompatibleLibraryChecker() {
        this.pythonCompatibilityRules = new ConcurrentHashMap<>();
        this.libraryConflictRules = new ConcurrentHashMap<>();
        this.libraryRecommendations = new ConcurrentHashMap<>();
        this.deprecatedLibraries = new ConcurrentHashMap<>();
        this.strictCompatibility = false;
        this.enableConflictDetection = true;
        
        initializeCompatibilityRules();
        initializeConflicts();
        initializeRecommendations();
        initializeDeprecatedLibraries();
    }
    
    /**
     * تهيئة قواعد التوافق
     */
    private void initializeCompatibilityRules() {
        // مكتبات متوافقة مع جميع الإصدارات
        addPythonCompatibility("requests", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11", "3.12"));
        addPythonCompatibility("click", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("pillow", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"));
        addPythonCompatibility("six", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("setuptools", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11", "3.12"));
        
        // مكتبات متوافقة مع Python 3.8+
        addPythonCompatibility("numpy", Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12"));
        addPythonCompatibility("pandas", Arrays.asList("3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("scipy", Arrays.asList("3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("scikit-learn", Arrays.asList("3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("matplotlib", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"));
        addPythonCompatibility("beautifulsoup4", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10"));
        addPythonCompatibility("sqlalchemy", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("jinja2", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("markupsafe", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        
        // مكتبات متوافقة مع Python 3.9+
        addPythonCompatibility("flask", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("werkzeug", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("itsdangerous", Arrays.asList("3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("asgiref", Arrays.asList("3.7", "3.8", "3.9", "3.10", "3.11"));
        
        // مكتبات متوافقة مع Python 3.10+
        addPythonCompatibility("django", Arrays.asList("3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("sqlparse", Arrays.asList("3.7", "3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("tzdata", Arrays.asList("3.8", "3.9", "3.10", "3.11"));
        
        // مكتبات متوافقة مع Python 3.11+
        addPythonCompatibility("tensorflow", Arrays.asList("3.8", "3.9", "3.10", "3.11"));
        addPythonCompatibility("pydantic", Arrays.asList("3.6", "3.7", "3.8", "3.9", "3.10", "3.11"));
        
        // مكتبات متوافقة مع Python 3.12
        addPythonCompatibility("pymongo", Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12"));
    }
    
    /**
     * تهيئة التعارضات
     */
    private void initializeConflicts() {
        // تضارب بين إصدارات TensorFlow
        addLibraryConflict("tensorflow", "2.10.0", "tensorflow-gpu", "2.11.0");
        
        // تضارب بين مكتبات SQLite
        addLibraryConflict("sqlite3", "3.0.0", "pysqlite3", "0.4.0");
        
        // تضارب في مكتبات XML
        addLibraryConflict("lxml", "4.0.0", "xmltodict", "0.12.0", "0.13.0");
        
        // تضارب مكتبات الصور
        addLibraryConflict("Pillow", "8.0.0", "PIL", "1.0.0");
    }
    
    /**
     * تهيئة الاقتراحات
     */
    private void initializeRecommendations() {
        // اقتراحات للمكتبات المجهولة
        addRecommendation("pycurl", Arrays.asList("requests", "httpx"));
        addRecommendation("mysql-python", Arrays.asList("PyMySQL", "mysqlclient", "SQLAlchemy"));
        addRecommendation("urllib2", Arrays.asList("requests", "httpx"));
        addRecommendation("imp", Arrays.asList("importlib"));
        addRecommendation("optparse", Arrays.asList("argparse", "click"));
        addRecommendation("ConfigParser", Arrays.asList("configparser"));
        addRecommendation("cPickle", Arrays.asList("pickle"));
        
        // اقتراحات لمكتبات TensorFlow البديلة
        addRecommendation("tensorflow", Arrays.asList("jax", "pytorch", "scikit-learn"));
        
        // اقتراحات لمكتبات الويب البديلة
        addRecommendation("django", Arrays.asList("flask", "fastapi"));
        addRecommendation("flask", Arrays.asList("fastapi", "starlette"));
    }
    
    /**
     * تهيئة المكتبات المهملة
     */
    private void initializeDeprecatedLibraries() {
        deprecatedLibraries.put("mysql-python", "مهمل - استخدم PyMySQL أو mysqlclient");
        deprecatedLibraries.put("pycurl", "مشاكل في التوافق - استخدم requests");
        deprecatedLibraries.put("urllib2", "مهمل في Python 3 - استخدم urllib.request");
        deprecatedLibraries.put("imp", "مهمل - استخدم importlib");
        deprecatedLibraries.put("optparse", "مهمل - استخدم argparse");
        deprecatedLibraries.put("ConfigParser", "اسم بديل لـ configparser");
        deprecatedLibraries.put("cPickle", "استخدم pickle مباشرة");
        deprecatedLibraries.put("SimpleHTTPServer", "مهمل - استخدم http.server");
    }
    
    /**
     * فحص توافق المكتبة مع Python
     */
    public boolean isCompatible(LibraryItem library, String pythonVersion) {
        if (library == null || pythonVersion == null) return true;
        
        String libraryName = library.getName().toLowerCase();
        
        // فحص القواعد المعرفة
        Set<String> compatibleVersions = pythonCompatibilityRules.get(libraryName);
        if (compatibleVersions != null) {
            return compatibleVersions.contains(pythonVersion);
        }
        
        // فحص إصدارات Python المدعومة في المكتبة
        List<String> libraryVersions = library.getPythonVersions();
        if (libraryVersions != null && !libraryVersions.isEmpty()) {
            return libraryVersions.contains(pythonVersion) || libraryVersions.contains("3.8+");
        }
        
        // افتراض التوافق مع الإصدارات الحديثة
        return pythonVersion.compareTo("3.8") >= 0;
    }
    
    /**
     * فحص توافق مكتبة مع أخرى
     */
    public boolean isCompatible(LibraryItem lib1, LibraryItem lib2) {
        if (lib1 == null || lib2 == null) return true;
        
        String lib1Name = lib1.getName().toLowerCase();
        String lib2Name = lib2.getName().toLowerCase();
        
        // فحص التعارضات المباشرة
        if (hasConflict(lib1Name, lib2Name)) {
            return false;
        }
        
        // فحص التوافق عبر التبعيات
        return checkDependencyCompatibility(lib1, lib2);
    }
    
    /**
     * فحص وجود تضارب بين مكتبتين
     */
    private boolean hasConflict(String lib1Name, String lib2Name) {
        // فحص التعارضات المعرفة
        Map<String, String> conflicts = libraryConflictRules.get(lib1Name);
        if (conflicts != null && conflicts.containsKey(lib2Name)) {
            return true;
        }
        
        conflicts = libraryConflictRules.get(lib2Name);
        if (conflicts != null && conflicts.containsKey(lib1Name)) {
            return true;
        }
        
        // فحص التعارضات العامة
        return hasGeneralConflict(lib1Name, lib2Name);
    }
    
    /**
     * فحص التعارضات العامة
     */
    private boolean hasGeneralConflict(String lib1Name, String lib2Name) {
        // مكتبات متضاربة شائعة
        if ((lib1Name.contains("tensorflow") && lib2Name.contains("pytorch")) ||
            (lib1Name.contains("pytorch") && lib2Name.contains("tensorflow"))) {
            return true;
        }
        
        if ((lib1Name.contains("flask") && lib2Name.contains("django")) ||
            (lib1Name.contains("django") && lib2Name.contains("flask"))) {
            return true; // ليس تضارب حقيقي لكن قد يسبب التباس
        }
        
        return false;
    }
    
    /**
     * فحص توافق التبعيات
     */
    private boolean checkDependencyCompatibility(LibraryItem lib1, LibraryItem lib2) {
        // فحص تبعيات lib1 مع lib2
        if (lib1.getDependencies() != null) {
            for (String dep : lib1.getDependencies()) {
                String cleanDep = parseDependencyName(dep);
                if (cleanDep.equalsIgnoreCase(lib2.getName())) {
                    if (!isDependencyCompatible(dep, lib2)) {
                        return false;
                    }
                }
            }
        }
        
        // فحص تبعيات lib2 مع lib1
        if (lib2.getDependencies() != null) {
            for (String dep : lib2.getDependencies()) {
                String cleanDep = parseDependencyName(dep);
                if (cleanDep.equalsIgnoreCase(lib1.getName())) {
                    if (!isDependencyCompatible(dep, lib1)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * فحص توافق التبعية
     */
    private boolean isDependencyCompatible(String dependency, LibraryItem library) {
        // تحليل شروط الإصدار في التبعية
        String versionConstraint = extractVersionConstraint(dependency);
        if (versionConstraint != null) {
            return isVersionCompatible(versionConstraint, library.getVersion());
        }
        
        return true;
    }
    
    /**
     * فحص توافق الإصدار
     */
    private boolean isVersionCompatible(String constraint, String libraryVersion) {
        try {
            // تنفيذ مقارنة الإصدارات المبسطة
            // في التطبيق الحقيقي، يجب استخدام مكتبة مقارنة الإصدارات
            if (constraint.startsWith(">=")) {
                String minVersion = constraint.substring(2);
                return compareVersions(libraryVersion, minVersion) >= 0;
            } else if (constraint.startsWith(">")) {
                String minVersion = constraint.substring(1);
                return compareVersions(libraryVersion, minVersion) > 0;
            } else if (constraint.startsWith("<=")) {
                String maxVersion = constraint.substring(2);
                return compareVersions(libraryVersion, maxVersion) <= 0;
            } else if (constraint.startsWith("<")) {
                String maxVersion = constraint.substring(1);
                return compareVersions(libraryVersion, maxVersion) < 0;
            } else if (constraint.contains("==")) {
                String exactVersion = constraint.split("==")[1];
                return libraryVersion.equals(exactVersion);
            }
        } catch (Exception e) {
            Log.w(TAG, "خطأ في مقارنة الإصدارات: " + constraint + " vs " + libraryVersion, e);
        }
        
        return true; // افتراض التوافق في حالة الخطأ
    }
    
    /**
     * مقارنة إصدارات
     */
    private int compareVersions(String version1, String version2) {
        try {
            String[] parts1 = version1.split("\\.");
            String[] parts2 = version2.split("\\.");
            
            int maxLength = Math.max(parts1.length, parts2.length);
            
            for (int i = 0; i < maxLength; i++) {
                int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
                
                if (part1 < part2) return -1;
                if (part1 > part2) return 1;
            }
            
            return 0;
        } catch (Exception e) {
            return version1.compareTo(version2);
        }
    }
    
    /**
     * الحصول على مستوى التوافق
     */
    public CompatibilityLevel getCompatibilityLevel(LibraryItem library, String pythonVersion) {
        if (!isCompatible(library, pythonVersion)) {
            return CompatibilityLevel.INCOMPATIBLE;
        }
        
        // حساب نقاط التوافق
        double score = calculateCompatibilityScore(library, pythonVersion);
        
        if (score >= 0.9) {
            return CompatibilityLevel.FULLY_COMPATIBLE;
        } else if (score >= 0.7) {
            return CompatibilityLevel.MOSTLY_COMPATIBLE;
        } else if (score >= 0.5) {
            return CompatibilityLevel.PARTIALLY_COMPATIBLE;
        } else {
            return CompatibilityLevel.MINIMALLY_COMPATIBLE;
        }
    }
    
    /**
     * حساب نقاط التوافق
     */
    private double calculateCompatibilityScore(LibraryItem library, String pythonVersion) {
        double score = 0.5; // نقاط أساسية
        
        // نقاط للتوافق المحدد
        if (isCompatible(library, pythonVersion)) {
            score += 0.3;
        }
        
        // نقاط للمكتبات الشائعة والموثوقة
        if (library.getDownloadCount() > 1000000) {
            score += 0.1;
        } else if (library.getDownloadCount() > 100000) {
            score += 0.05;
        }
        
        // نقاط للتحديثات الحديثة
        if (library.getLastUpdated() != null) {
            long daysSinceUpdate = (System.currentTimeMillis() - library.getLastUpdated().getTime()) / (1000 * 60 * 60 * 24);
            if (daysSinceUpdate < 365) {
                score += 0.05;
            }
        }
        
        // نقاط للمكتبات غير المهملة
        if (!isDeprecated(library.getName())) {
            score += 0.05;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * فحص المكتبات المتوافقة مع Python
     */
    public List<LibraryItem> filterCompatibleLibraries(List<LibraryItem> libraries, String pythonVersion) {
        List<LibraryItem> compatible = new ArrayList<>();
        
        for (LibraryItem library : libraries) {
            if (isCompatible(library, pythonVersion)) {
                compatible.add(library);
            }
        }
        
        // ترتيب حسب مستوى التوافق
        compatible.sort((lib1, lib2) -> {
            CompatibilityLevel level1 = getCompatibilityLevel(lib1, pythonVersion);
            CompatibilityLevel level2 = getCompatibilityLevel(lib2, pythonVersion);
            return level2.ordinal() - level1.ordinal();
        });
        
        return compatible;
    }
    
    /**
     * الحصول على اقتراحات للبدائل
     */
    public List<String> getAlternatives(String libraryName) {
        return libraryRecommendations.getOrDefault(libraryName.toLowerCase(), new ArrayList<>());
    }
    
    /**
     * فحص إذا كانت المكتبة مهملة
     */
    public boolean isDeprecated(String libraryName) {
        return deprecatedLibraries.containsKey(libraryName.toLowerCase());
    }
    
    /**
     * الحصول على رسالة التحذير للمكتبة المهملة
     */
    public String getDeprecationWarning(String libraryName) {
        return deprecatedLibraries.get(libraryName.toLowerCase());
    }
    
    /**
     * فحص مجموعة مكتبات للتوافق
     */
    public CompatibilityReport checkCompatibilitySet(List<LibraryItem> libraries) {
        List<String> conflicts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> deprecatedFound = new ArrayList<>();
        
        for (LibraryItem library : libraries) {
            // فحص المهملة
            if (isDeprecated(library.getName())) {
                deprecatedFound.add(library.getName() + ": " + getDeprecationWarning(library.getName()));
            }
            
            // فحص التوافق بين المكتبات
            for (LibraryItem otherLibrary : libraries) {
                if (!library.equals(otherLibrary) && !isCompatible(library, otherLibrary)) {
                    conflicts.add("تضارب: " + library.getName() + " و " + otherLibrary.getName());
                }
            }
        }
        
        // فحص التبعيات
        for (LibraryItem library : libraries) {
            if (library.getDependencies() != null) {
                for (String dependency : library.getDependencies()) {
                    String depName = parseDependencyName(dependency);
                    boolean found = false;
                    
                    for (LibraryItem lib : libraries) {
                        if (lib.getName().equalsIgnoreCase(depName)) {
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        warnings.add("تبعية مفقودة: " + dependency + " لـ " + library.getName());
                    }
                }
            }
        }
        
        return new CompatibilityReport(conflicts, warnings, deprecatedFound);
    }
    
    /**
     * إضافة قاعدة توافق Python
     */
    private void addPythonCompatibility(String libraryName, List<String> versions) {
        pythonCompatibilityRules.put(libraryName.toLowerCase(), new HashSet<>(versions));
    }
    
    /**
     * إضافة تضارب مكتبات
     */
    private void addLibraryConflict(String lib1Name, String... conflictingVersions) {
        Map<String, String> conflicts = libraryConflictRules.computeIfAbsent(lib1Name.toLowerCase(), k -> new HashMap<>());
        
        for (String conflict : conflictingVersions) {
            String[] parts = conflict.split(",");
            if (parts.length >= 2) {
                conflicts.put(parts[0].toLowerCase(), parts[1]);
            }
        }
    }
    
    /**
     * إضافة اقتراح
     */
    private void addRecommendation(String libraryName, List<String> alternatives) {
        libraryRecommendations.put(libraryName.toLowerCase(), new ArrayList<>(alternatives));
    }
    
    /**
     * تحليل اسم التبعية
     */
    private String parseDependencyName(String dependency) {
        if (dependency == null) return "";
        
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
     * استخراج قيود الإصدار
     */
    private String extractVersionConstraint(String dependency) {
        if (dependency == null) return null;
        
        // البحث عن قيود الإصدار
        if (dependency.contains(">=") || dependency.contains("<=") || 
            dependency.contains("==") || dependency.contains(">") || 
            dependency.contains("<")) {
            
            return dependency.substring(dependency.indexOf(">") != -1 || dependency.indexOf("<") != -1 || 
                                     dependency.indexOf("=") != -1 ? dependency.indexOf(">") : 
                                     dependency.indexOf("<"), dependency.length());
        }
        
        return null;
    }
    
    /**
     * مستويات التوافق
     */
    public enum CompatibilityLevel {
        INCOMPATIBLE("غير متوافق"),
        MINIMALLY_COMPATIBLE("توافق ضئيل"),
        PARTIALLY_COMPATIBLE("توافق جزئي"),
        MOSTLY_COMPATIBLE("توافق معظم"),
        FULLY_COMPATIBLE("توافق كامل");
        
        private final String description;
        
        CompatibilityLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * تقرير التوافق
     */
    public static class CompatibilityReport {
        private List<String> conflicts;
        private List<String> warnings;
        private List<String> deprecatedFound;
        
        public CompatibilityReport(List<String> conflicts, List<String> warnings, List<String> deprecatedFound) {
            this.conflicts = conflicts;
            this.warnings = warnings;
            this.deprecatedFound = deprecatedFound;
        }
        
        public List<String> getConflicts() { return conflicts; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getDeprecatedFound() { return deprecatedFound; }
        
        public boolean hasConflicts() { return !conflicts.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasDeprecated() { return !deprecatedFound.isEmpty(); }
        
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            
            if (hasConflicts()) {
                summary.append("تضارب: ").append(conflicts.size()).append("\n");
            }
            
            if (hasWarnings()) {
                summary.append("تحذيرات: ").append(warnings.size()).append("\n");
            }
            
            if (hasDeprecated()) {
                summary.append("مهمل: ").append(deprecatedFound.size()).append("\n");
            }
            
            if (!hasConflicts() && !hasWarnings() && !hasDeprecated()) {
                summary.append("جميع المكتبات متوافقة");
            }
            
            return summary.toString();
        }
    }
}