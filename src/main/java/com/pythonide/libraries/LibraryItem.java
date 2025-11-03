package com.pythonide.libraries;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import java.util.List;

/**
 * LibraryItem - نموذج بيانات مكتبة Python
 * 
 * يمثل مكتبة Python مع جميع المعلومات ذات الصلة:
 * - المعلومات الأساسية (الاسم، الوصف، الإصدار)
 * - معلومات المؤلف والرخصة
 * - التبعيات والمتطلبات
 * - التوافق مع إصدارات Python
 * - إحصائيات الاستخدام
 * - حالة التثبيت والتحديث
 */
public class LibraryItem implements Parcelable {
    
    // المعلومات الأساسية
    private String name;
    private String description;
    private String version;
    
    // معلومات المؤلف والرخصة
    private String author;
    private String authorEmail;
    private String license;
    private String homePage;
    private String pyPIUrl;
    
    // التبعيات والمتطلبات
    private List<String> dependencies;
    private List<String> classifiers;
    private List<String> pythonVersions;
    
    // إحصائيات الاستخدام
    private int downloadCount;
    private Date lastUpdated;
    
    // حالة المكتبة
    private boolean isInstalled;
    private boolean isUpdated;
    
    // إعدادات إضافية
    private long installationDate;
    private String installationPath;
    private String currentVersion;
    private boolean hasUpdate;
    private String latestVersion;
    private double compatibilityScore;
    
    // ====================== المنشئات ======================
    
    /**
     * إنشاء مكتبة جديدة
     */
    public LibraryItem(String name, String description, String version, 
                      String author, String authorEmail, String license,
                      String homePage, String pyPIUrl, List<String> dependencies,
                      List<String> classifiers, List<String> pythonVersions,
                      boolean isInstalled, int downloadCount, Date lastUpdated, 
                      boolean isUpdated) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.authorEmail = authorEmail;
        this.license = license;
        this.homePage = homePage;
        this.pyPIUrl = pyPIUrl;
        this.dependencies = dependencies;
        this.classifiers = classifiers;
        this.pythonVersions = pythonVersions;
        this.isInstalled = isInstalled;
        this.downloadCount = downloadCount;
        this.lastUpdated = lastUpdated;
        this.isUpdated = isUpdated;
        this.installationDate = System.currentTimeMillis();
        this.compatibilityScore = calculateCompatibilityScore();
    }
    
    /**
     * إنشاء مكتبة من بيانات PyPI
     */
    public static LibraryItem fromPyPI(String name, String version, String description) {
        return new LibraryItem(
            name,
            description != null ? description : "وصف غير متوفر",
            version != null ? version : "0.0.0",
            "غير محدد", "", "غير محدد", "", "",
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList(),
            java.util.Arrays.asList("3.8", "3.9", "3.10", "3.11", "3.12"),
            false, 0, new Date(), false
        );
    }
    
    // ====================== getters and setters ======================
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getAuthorEmail() {
        return authorEmail;
    }
    
    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }
    
    public String getLicense() {
        return license;
    }
    
    public void setLicense(String license) {
        this.license = license;
    }
    
    public String getHomePage() {
        return homePage;
    }
    
    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }
    
    public String getPyPIUrl() {
        return pyPIUrl;
    }
    
    public void setPyPIUrl(String pyPIUrl) {
        this.pyPIUrl = pyPIUrl;
    }
    
    public List<String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
    
    public List<String> getClassifiers() {
        return classifiers;
    }
    
    public void setClassifiers(List<String> classifiers) {
        this.classifiers = classifiers;
    }
    
    public List<String> getPythonVersions() {
        return pythonVersions;
    }
    
    public void setPythonVersions(List<String> pythonVersions) {
        this.pythonVersions = pythonVersions;
    }
    
    public int getDownloadCount() {
        return downloadCount;
    }
    
    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }
    
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isInstalled() {
        return isInstalled;
    }
    
    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }
    
    public boolean isUpdated() {
        return isUpdated;
    }
    
    public void setUpdated(boolean updated) {
        isUpdated = updated;
    }
    
    public long getInstallationDate() {
        return installationDate;
    }
    
    public void setInstallationDate(long installationDate) {
        this.installationDate = installationDate;
    }
    
    public String getInstallationPath() {
        return installationPath;
    }
    
    public void setInstallationPath(String installationPath) {
        this.installationPath = installationPath;
    }
    
    public String getCurrentVersion() {
        return currentVersion != null ? currentVersion : version;
    }
    
    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }
    
    public boolean hasUpdate() {
        return hasUpdate;
    }
    
    public void setHasUpdate(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }
    
    public String getLatestVersion() {
        return latestVersion != null ? latestVersion : version;
    }
    
    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }
    
    public double getCompatibilityScore() {
        return compatibilityScore;
    }
    
    public void setCompatibilityScore(double compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }
    
    // ====================== طرق مساعدة ======================
    
    /**
     * التحقق من وجود تبعية معينة
     */
    public boolean hasDependency(String dependencyName) {
        if (dependencies == null) return false;
        
        for (String dep : dependencies) {
            if (dep.toLowerCase().contains(dependencyName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * التحقق من التوافق مع إصدار Python معين
     */
    public boolean isCompatibleWithPython(String pythonVersion) {
        if (pythonVersions == null || pythonVersion == null) return true;
        return pythonVersions.contains(pythonVersion);
    }
    
    /**
     * الحصول على وصف قصير للعرض
     */
    public String getShortDescription() {
        if (description == null || description.isEmpty()) {
            return "وصف غير متوفر";
        }
        
        if (description.length() <= 100) {
            return description;
        }
        
        return description.substring(0, 97) + "...";
    }
    
    /**
     * الحصول على حالة المكتبة للعرض
     */
    public String getStatusText() {
        if (!isInstalled) {
            return "غير مثبت";
        }
        
        if (isUpdated || hasUpdate) {
            return "تحديث متاح";
        }
        
        return "مثبت";
    }
    
    /**
     * الحصول على لون حالة المكتبة
     */
    public int getStatusColor() {
        if (!isInstalled) {
            return 0xFF6B7280; // رمادي
        }
        
        if (isUpdated || hasUpdate) {
            return 0xFFEF4444; // أحمر
        }
        
        return 0xFF10B981; // أخضر
    }
    
    /**
     * الحصول على أول 3 إصدارات Python مدعومة
     */
    public String getTopPythonVersions() {
        if (pythonVersions == null || pythonVersions.isEmpty()) {
            return "3.8+";
        }
        
        StringBuilder sb = new StringBuilder();
        int count = Math.min(3, pythonVersions.size());
        
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append(pythonVersions.get(i));
        }
        
        if (pythonVersions.size() > count) {
            sb.append("...");
        }
        
        return sb.toString();
    }
    
    /**
     * التحقق من كون المكتبة شائعة
     */
    public boolean isPopular() {
        return downloadCount > 500000;
    }
    
    /**
     * الحصول على مستوى الشعبية
     */
    public String getPopularityLevel() {
        if (downloadCount > 1000000) {
            return "شائعة جداً";
        } else if (downloadCount > 500000) {
            return "شائعة";
        } else if (downloadCount > 100000) {
            return "متوسطة";
        } else {
            return "نادرة";
        }
    }
    
    /**
     * الحصول على عدد التبعيات
     */
    public int getDependenciesCount() {
        return dependencies != null ? dependencies.size() : 0;
    }
    
    /**
     * الحصول على أول 3 تبعيات
     */
    public String getTopDependencies() {
        if (dependencies == null || dependencies.isEmpty()) {
            return "لا توجد";
        }
        
        StringBuilder sb = new StringBuilder();
        int count = Math.min(3, dependencies.size());
        
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            String dep = dependencies.get(i);
            // إزالة شروط الإصدار
            dep = dep.replaceAll("[<>=!~].*", "").trim();
            sb.append(dep);
        }
        
        if (dependencies.size() > count) {
            sb.append("...");
        }
        
        return sb.toString();
    }
    
    /**
     * حساب نقاط التوافق
     */
    private double calculateCompatibilityScore() {
        double score = 0.0;
        
        if (pythonVersions != null) {
            // نقاط إضافية للإصدارات الحديثة
            if (pythonVersions.contains("3.11")) score += 2.0;
            if (pythonVersions.contains("3.10")) score += 1.5;
            if (pythonVersions.contains("3.9")) score += 1.0;
            if (pythonVersions.contains("3.8")) score += 0.5;
        }
        
        // نقاط للمكتبات الشائعة
        if (isPopular()) {
            score += 1.0;
        }
        
        // نقاط للمكتبات المتوافقة مع أحدث إصدارات Python
        if (pythonVersions != null && pythonVersions.size() >= 4) {
            score += 0.5;
        }
        
        return Math.min(score, 5.0); // حد أقصى 5 نقاط
    }
    
    /**
     * تحديث نقاط التوافق
     */
    public void updateCompatibilityScore() {
        this.compatibilityScore = calculateCompatibilityScore();
    }
    
    /**
     * الحصول على معلومات أساسية للعرض السريع
     */
    public String getSummaryInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("الإصدار: ").append(version);
        
        if (isInstalled) {
            sb.append(" • ").append(getStatusText());
        }
        
        if (pythonVersions != null && !pythonVersions.isEmpty()) {
            sb.append(" • Python: ").append(getTopPythonVersions());
        }
        
        return sb.toString();
    }
    
    /**
     * إنشاء نسخة من المكتبة مع تحديث حالة التثبيت
     */
    public LibraryItem createInstalledCopy() {
        LibraryItem copy = new LibraryItem(
            name, description, version, author, authorEmail, license,
            homePage, pyPIUrl, dependencies, classifiers, pythonVersions,
            true, downloadCount, lastUpdated, isUpdated
        );
        
        copy.setInstallationDate(System.currentTimeMillis());
        copy.setCurrentVersion(version);
        copy.setHasUpdate(false);
        copy.setLatestVersion(version);
        
        return copy;
    }
    
    /**
     * مقارنة مع مكتبة أخرى
     */
    public int compareTo(LibraryItem other) {
        // ترتيب حسب الشعبية أولاً
        int popularityDiff = Integer.compare(other.downloadCount, this.downloadCount);
        if (popularityDiff != 0) {
            return popularityDiff;
        }
        
        // ثم حسب الاسم
        return this.name.compareToIgnoreCase(other.name);
    }
    
    // ====================== Parcelable implementation ======================
    
    protected LibraryItem(Parcel in) {
        name = in.readString();
        description = in.readString();
        version = in.readString();
        author = in.readString();
        authorEmail = in.readString();
        license = in.readString();
        homePage = in.readString();
        pyPIUrl = in.readString();
        
        // قراءة القوائم
        dependencies = in.createStringArrayList();
        classifiers = in.createStringArrayList();
        pythonVersions = in.createStringArrayList();
        
        downloadCount = in.readInt();
        long lastUpdatedTime = in.readLong();
        lastUpdated = new Date(lastUpdatedTime);
        
        isInstalled = in.readByte() != 0;
        isUpdated = in.readByte() != 0;
        
        installationDate = in.readLong();
        installationPath = in.readString();
        currentVersion = in.readString();
        hasUpdate = in.readByte() != 0;
        latestVersion = in.readString();
        compatibilityScore = in.readDouble();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(version);
        dest.writeString(author);
        dest.writeString(authorEmail);
        dest.writeString(license);
        dest.writeString(homePage);
        dest.writeString(pyPIUrl);
        
        dest.writeStringList(dependencies);
        dest.writeStringList(classifiers);
        dest.writeStringList(pythonVersions);
        
        dest.writeInt(downloadCount);
        dest.writeLong(lastUpdated != null ? lastUpdated.getTime() : 0);
        
        dest.writeByte((byte) (isInstalled ? 1 : 0));
        dest.writeByte((byte) (isUpdated ? 1 : 0));
        
        dest.writeLong(installationDate);
        dest.writeString(installationPath);
        dest.writeString(currentVersion);
        dest.writeByte((byte) (hasUpdate ? 1 : 0));
        dest.writeString(latestVersion);
        dest.writeDouble(compatibilityScore);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<LibraryItem> CREATOR = new Creator<LibraryItem>() {
        @Override
        public LibraryItem createFromParcel(Parcel in) {
            return new LibraryItem(in);
        }
        
        @Override
        public LibraryItem[] newArray(int size) {
            return new LibraryItem[size];
        }
    };
    
    // ====================== Object methods ======================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LibraryItem that = (LibraryItem) obj;
        return name != null ? name.equals(that.name) : that.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "LibraryItem{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", isInstalled=" + isInstalled +
                ", downloadCount=" + downloadCount +
                '}';
    }
}