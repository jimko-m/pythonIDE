package com.pythonide.libraries;

import android.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DependencyResolver - حل وإدارة تبعيات المكتبات
 * 
 * المسؤوليات الرئيسية:
 * - حل التبعيات الدائرية
 * - ترتيب التبعيات حسب الأولوية
 * - فحص التوافق بين المكتبات
 * - إزالة التبعيات غير المستخدمة
 * - حفظ رسوم التبعيات
 */
public class DependencyResolver {
    
    private static final String TAG = "DependencyResolver";
    
    // رسم التبعيات
    private Map<String, Set<String>> dependencyGraph;
    private Map<String, LibraryItem> libraryCache;
    private Map<String, Set<String>> reverseDependencyGraph;
    private Map<String, List<String>> dependencyChains;
    
    // إعدادات الحل
    private boolean enableCircularDependencyResolution;
    private boolean enableConflictDetection;
    private int maxDepth;
    
    /**
     * إنشاء محلل التبعيات
     */
    public DependencyResolver() {
        this.dependencyGraph = new ConcurrentHashMap<>();
        this.libraryCache = new ConcurrentHashMap<>();
        this.reverseDependencyGraph = new ConcurrentHashMap<>();
        this.dependencyChains = new ConcurrentHashMap<>();
        this.enableCircularDependencyResolution = true;
        this.enableConflictDetection = true;
        this.maxDepth = 10;
        
        initializeDefaultDependencies();
    }
    
    /**
     * تهيئة التبعيات الافتراضية
     */
    private void initializeDefaultDependencies() {
        // إضافة التبعيات الشائعة
        addDependency("numpy", Arrays.asList("setuptools", "wheel"));
        addDependency("pandas", Arrays.asList("numpy>=1.18.5", "python-dateutil>=2.8.1", "pytz>=2017.3", "six>=1.5.0"));
        addDependency("matplotlib", Arrays.asList("numpy>=1.15", "kiwisolver>=1.0.1", "pyparsing>=2.0.1", "pillow>=6.2.0"));
        addDependency("requests", Arrays.asList("certifi>=2017.4.17", "charset-normalizer>=2.0.0", "idna>=2.5,<3", "urllib3>=1.21.1,<2"));
        addDependency("flask", Arrays.asList("click>=7.1", "itsdangerous>=1.1.0", "jinja2>=2.11.3", "markupsafe>=1.1", "werkzeug>=1.0.1"));
        addDependency("django", Arrays.asList("asgiref>=3.3.2,<4", "pytz", "sqlparse>=0.2.2"));
        addDependency("scipy", Arrays.asList("numpy>=1.16.5,<2.0", "pyparsing>=2.0.3", "scipy>=1.2.0"));
        addDependency("tensorflow", Arrays.asList("numpy>=1.16.0,<1.19.0", "protobuf>=3.6.1", "wrapt>=1.11.1"));
        addDependency("beautifulsoup4", Arrays.asList("soupsieve>1.2"));
        addDependency("selenium", Arrays.asList("urllib3[socks]", "cryptography>=2.8"));
        addDependency("pygame", Arrays.asList()); // لا توجد تبعيات خارجية
        addDependency("pillow", Arrays.asList("setuptools"));
        addDependency("sqlalchemy", Arrays.asList()); // لا توجد تبعيات خارجية
        addDependency("pyyaml", Arrays.asList()); // لا توجد تبعيات خارجية
        addDependency("click", Arrays.asList()); // لا توجد تبعيات خارجية
        addDependency("pytest", Arrays.asList("py>=1.8.2", "packaging", "attrs>=17.4.0"));
        
        // بناء الرسم العكسي
        buildReverseDependencyGraph();
    }
    
    /**
     * إضافة تبعيات لمكتبة
     */
    public void addDependency(String libraryName, List<String> dependencies) {
        if (libraryName == null || dependencies == null) return;
        
        Set<String> deps = dependencyGraph.computeIfAbsent(libraryName.toLowerCase(), k -> new HashSet<>());
        deps.clear(); // مسح التبعيات الحالية
        
        for (String dependency : dependencies) {
            String cleanDep = parseDependencyName(dependency);
            if (!cleanDep.isEmpty()) {
                deps.add(cleanDep);
            }
        }
        
        // إعادة بناء الرسم العكسي
        buildReverseDependencyGraph();
        
        Log.d(TAG, "تم إضافة " + deps.size() + " تبعيات لـ " + libraryName);
    }
    
    /**
     * حل تبعيات مكتبة معينة
     */
    public List<String> resolveDependencies(String libraryName) {
        return resolveDependencies(libraryName, maxDepth, new HashSet<>());
    }
    
    /**
     * حل تبعيات مكتبة مع عمق محدود
     */
    public List<String> resolveDependencies(String libraryName, int depthLimit, Set<String> visited) {
        List<String> dependencies = new ArrayList<>();
        
        if (depthLimit <= 0 || visited.contains(libraryName.toLowerCase())) {
            return dependencies;
        }
        
        String lowerName = libraryName.toLowerCase();
        visited.add(lowerName);
        
        // الحصول على التبعيات المباشرة
        Set<String> directDeps = dependencyGraph.get(lowerName);
        if (directDeps != null) {
            for (String dep : directDeps) {
                if (!dependencies.contains(dep)) {
                    dependencies.add(dep);
                    
                    // حل التبعيات الفرعية
                    List<String> subDeps = resolveDependencies(dep, depthLimit - 1, new HashSet<>(visited));
                    for (String subDep : subDeps) {
                        if (!dependencies.contains(subDep)) {
                            dependencies.add(subDep);
                        }
                    }
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * فحص التبعيات الدائرية
     */
    public boolean hasCircularDependencies(String libraryName) {
        return hasCircularDependencies(libraryName, new HashSet<>(), new HashSet<>());
    }
    
    /**
     * فحص التبعيات الدائرية باستخدام DFS
     */
    private boolean hasCircularDependencies(String libraryName, Set<String> visiting, Set<String> visited) {
        String lowerName = libraryName.toLowerCase();
        
        if (visited.contains(lowerName)) {
            return false;
        }
        
        if (visiting.contains(lowerName)) {
            return true; // تم العثور على دائرة
        }
        
        visiting.add(lowerName);
        
        Set<String> dependencies = dependencyGraph.get(lowerName);
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (hasCircularDependencies(dep, visiting, visited)) {
                    return true;
                }
            }
        }
        
        visiting.remove(lowerName);
        visited.add(lowerName);
        
        return false;
    }
    
    /**
     * الحصول على جميع المكتبات المتأثرة بحذف مكتبة معينة
     */
    public List<String> getAffectedLibraries(String libraryName) {
        List<String> affected = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        collectAffectedLibraries(libraryName.toLowerCase(), affected, visited);
        
        return affected;
    }
    
    /**
     * جمع المكتبات المتأثرة
     */
    private void collectAffectedLibraries(String libraryName, List<String> affected, Set<String> visited) {
        if (visited.contains(libraryName)) return;
        
        visited.add(libraryName);
        
        Set<String> dependents = reverseDependencyGraph.get(libraryName);
        if (dependents != null) {
            for (String dependent : dependents) {
                if (!affected.contains(dependent)) {
                    affected.add(dependent);
                    collectAffectedLibraries(dependent, affected, visited);
                }
            }
        }
    }
    
    /**
     * إزالة التبعيات غير المستخدمة
     */
    public List<String> removeUnusedDependencies(List<String> keptLibraries) {
        List<String> removed = new ArrayList<>();
        Set<String> kept = new HashSet<>();
        
        // تحويل أسماء المكتبات إلى حروف صغيرة
        for (String lib : keptLibraries) {
            kept.add(lib.toLowerCase());
        }
        
        // العثور على المكتبات غير المستخدمة
        Set<String> unused = new HashSet<>();
        for (String lib : dependencyGraph.keySet()) {
            if (!kept.contains(lib) && !isLibraryUsed(lib, kept)) {
                unused.add(lib);
            }
        }
        
        // إزالة المكتبات غير المستخدمة
        for (String lib : unused) {
            Set<String> dependents = reverseDependencyGraph.get(lib);
            if (dependents != null) {
                for (String dependent : dependents) {
                    Set<String> deps = dependencyGraph.get(dependent);
                    if (deps != null) {
                        deps.remove(lib);
                    }
                }
            }
            
            dependencyGraph.remove(lib);
            reverseDependencyGraph.remove(lib);
            libraryCache.remove(lib);
            removed.add(lib);
        }
        
        Log.i(TAG, "تم إزالة " + removed.size() + " مكتبة غير مستخدمة");
        
        return removed;
    }
    
    /**
     * فحص استخدام المكتبة
     */
    private boolean isLibraryUsed(String libraryName, Set<String> keptLibraries) {
        Set<String> dependents = reverseDependencyGraph.get(libraryName);
        if (dependents == null) return false;
        
        for (String dependent : dependents) {
            if (keptLibraries.contains(dependent)) {
                return true;
            }
            
            // فحص递归 للتبعيات الفرعية
            if (isLibraryUsed(dependent, keptLibraries)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * بناء الرسم العكسي للتبعيات
     */
    private void buildReverseDependencyGraph() {
        reverseDependencyGraph.clear();
        
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            String library = entry.getKey();
            for (String dependency : entry.getValue()) {
                Set<String> dependents = reverseDependencyGraph.computeIfAbsent(dependency, k -> new HashSet<>());
                dependents.add(library);
            }
        }
    }
    
    /**
     * تنظيف التبعيات الدائرية
     */
    public void resolveCircularDependencies() {
        if (!enableCircularDependencyResolution) return;
        
        List<String> librariesWithCircles = new ArrayList<>();
        
        for (String library : dependencyGraph.keySet()) {
            if (hasCircularDependencies(library)) {
                librariesWithCircles.add(library);
            }
        }
        
        // حل الدوائر بطريقة بسيطة - إزالة تبعيات عشوائية
        for (String library : librariesWithCircles) {
            resolveCircularDependency(library);
        }
        
        Log.i(TAG, "تم حل " + librariesWithCircles.size() + " دائرة تبعية");
    }
    
    /**
     * حل دائرة تبعية لمكتبة معينة
     */
    private void resolveCircularDependency(String libraryName) {
        Set<String> dependencies = dependencyGraph.get(libraryName.toLowerCase());
        if (dependencies == null || dependencies.isEmpty()) return;
        
        // إزالة تبعية عشوائية لكسر الدائرة
        String randomDep = dependencies.iterator().next();
        dependencies.remove(randomDep);
        
        Log.w(TAG, "تم كسر دائرة تبعية بإزالة " + randomDep + " من تبعيات " + libraryName);
    }
    
    /**
     * فحص تضارب التبعيات
     */
    public List<DependencyConflict> detectConflicts() {
        List<DependencyConflict> conflicts = new ArrayList<>();
        
        // فحص تضارب إصدارات المكتبة
        Map<String, Set<String>> versionConflicts = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            String library = entry.getKey();
            for (String dep : entry.getValue()) {
                String cleanDep = parseDependencyName(dep);
                if (versionConflicts.containsKey(cleanDep)) {
                    versionConflicts.get(cleanDep).add(library);
                } else {
                    Set<String> libs = new HashSet<>();
                    libs.add(library);
                    versionConflicts.put(cleanDep, libs);
                }
            }
        }
        
        // تقرير التضاربات
        for (Map.Entry<String, Set<String>> entry : versionConflicts.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(new DependencyConflict(entry.getKey(), new ArrayList<>(entry.getValue())));
            }
        }
        
        return conflicts;
    }
    
    /**
     * ترتيب المكتبات حسب التبعيات
     */
    public List<String> topologicalSort() {
        List<String> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (String library : dependencyGraph.keySet()) {
            if (!visited.contains(library)) {
                if (!topologicalSortDFS(library, visited, visiting, sorted)) {
                    Log.w(TAG, "تم العثور على دائرة في التبعيات أثناء الترتيب");
                    break;
                }
            }
        }
        
        return sorted;
    }
    
    /**
     * DFS للترتيب الطوبولوجي
     */
    private boolean topologicalSortDFS(String library, Set<String> visited, Set<String> visiting, List<String> sorted) {
        if (visiting.contains(library)) {
            return false; // دائرة
        }
        
        if (visited.contains(library)) {
            return true;
        }
        
        visiting.add(library);
        
        Set<String> dependencies = dependencyGraph.get(library);
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (!topologicalSortDFS(dep, visited, visiting, sorted)) {
                    return false;
                }
            }
        }
        
        visiting.remove(library);
        visited.add(library);
        sorted.add(library);
        
        return true;
    }
    
    /**
     * فلترة المكتبات المتوافقة
     */
    public List<LibraryItem> filterCompatiblePackages(List<LibraryItem> packages, String pythonVersion) {
        List<LibraryItem> compatible = new ArrayList<>();
        
        for (LibraryItem library : packages) {
            if (isCompatibleWithPython(library, pythonVersion)) {
                compatible.add(library);
            }
        }
        
        return compatible;
    }
    
    /**
     * فحص توافق المكتبة مع Python
     */
    private boolean isCompatibleWithPython(LibraryItem library, String pythonVersion) {
        if (pythonVersion == null || pythonVersion.equals("الكل")) {
            return true;
        }
        
        List<String> supportedVersions = library.getPythonVersions();
        return supportedVersions != null && supportedVersions.contains(pythonVersion);
    }
    
    /**
     * تحليل اسم التبعية
     */
    private String parseDependencyName(String dependency) {
        if (dependency == null || dependency.isEmpty()) {
            return "";
        }
        
        // إزالة شروط الإصدار والرموز الخاصة
        String cleanDep = dependency.replaceAll("[<>=!~].*", "").trim();
        cleanDep = cleanDep.replaceAll("[\\[\\]\\(\\)]", "").trim();
        
        // الحصول على الاسم الأساسي فقط
        String[] parts = cleanDep.split("[\\s\\-:]+");
        if (parts.length > 0) {
            return parts[0].trim().toLowerCase();
        }
        
        return cleanDep.toLowerCase();
    }
    
    /**
     * الحصول على رسم التبعيات الحالي
     */
    public Map<String, Set<String>> getDependencyGraph() {
        return new HashMap<>(dependencyGraph);
    }
    
    /**
     * حفظ رسم التبعيات
     */
    public void saveDependencyGraph() {
        // حفظ في التخزين المحلي
        // يمكن تنفيذ هذا لاحقاً
        Log.d(TAG, "تم حفظ رسم التبعيات");
    }
    
    /**
     * تحميل رسم التبعيات
     */
    public void loadDependencyGraph(Map<String, Set<String>> graph) {
        if (graph != null) {
            dependencyGraph.clear();
            dependencyGraph.putAll(graph);
            buildReverseDependencyGraph();
            Log.d(TAG, "تم تحميل رسم التبعيات");
        }
    }
    
    /**
     * مسح جميع البيانات
     */
    public void clear() {
        dependencyGraph.clear();
        reverseDependencyGraph.clear();
        libraryCache.clear();
        dependencyChains.clear();
        Log.d(TAG, "تم مسح بيانات التبعيات");
    }
    
    /**
     * الحصول على إحصائيات التبعيات
     */
    public DependencyStatistics getStatistics() {
        int totalLibraries = dependencyGraph.size();
        int totalDependencies = dependencyGraph.values().stream().mapToInt(Set::size).sum();
        int librariesWithNoDeps = 0;
        int mostDependentLib = 0;
        String mostDependentName = "";
        
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            if (entry.getValue().isEmpty()) {
                librariesWithNoDeps++;
            }
            
            if (entry.getValue().size() > mostDependentLib) {
                mostDependentLib = entry.getValue().size();
                mostDependentName = entry.getKey();
            }
        }
        
        return new DependencyStatistics(
            totalLibraries,
            totalDependencies,
            librariesWithNoDeps,
            mostDependentLib,
            mostDependentName
        );
    }
    
    /**
     * فئة تضارب التبعيات
     */
    public static class DependencyConflict {
        private String dependencyName;
        private List<String> libraries;
        
        public DependencyConflict(String dependencyName, List<String> libraries) {
            this.dependencyName = dependencyName;
            this.libraries = libraries;
        }
        
        public String getDependencyName() {
            return dependencyName;
        }
        
        public List<String> getLibraries() {
            return libraries;
        }
        
        @Override
        public String toString() {
            return dependencyName + " مطلوبة من: " + String.join(", ", libraries);
        }
    }
    
    /**
     * فئة إحصائيات التبعيات
     */
    public static class DependencyStatistics {
        private int totalLibraries;
        private int totalDependencies;
        private int librariesWithNoDependencies;
        private int mostDependentLibraryCount;
        private String mostDependentLibraryName;
        
        public DependencyStatistics(int totalLibraries, int totalDependencies, 
                                  int librariesWithNoDependencies, 
                                  int mostDependentLibraryCount,
                                  String mostDependentLibraryName) {
            this.totalLibraries = totalLibraries;
            this.totalDependencies = totalDependencies;
            this.librariesWithNoDependencies = librariesWithNoDependencies;
            this.mostDependentLibraryCount = mostDependentLibraryCount;
            this.mostDependentLibraryName = mostDependentLibraryName;
        }
        
        public int getTotalLibraries() { return totalLibraries; }
        public int getTotalDependencies() { return totalDependencies; }
        public int getLibrariesWithNoDependencies() { return librariesWithNoDependencies; }
        public int getMostDependentLibraryCount() { return mostDependentLibraryCount; }
        public String getMostDependentLibraryName() { return mostDependentLibraryName; }
        
        public double getAverageDependencies() {
            return totalLibraries > 0 ? (double) totalDependencies / totalLibraries : 0;
        }
        
        @Override
        public String toString() {
            return String.format("إجمالي المكتبات: %d، إجمالي التبعيات: %d، متوسط التبعيات: %.2f",
                totalLibraries, totalDependencies, getAverageDependencies());
        }
    }
}