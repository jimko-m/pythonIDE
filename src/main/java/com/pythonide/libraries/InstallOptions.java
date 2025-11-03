package com.pythonide.libraries;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * InstallOptions - خيارات تثبيت المكتبات
 * 
 * تحدد جميع الخيارات والإعدادات لتثبيت مكتبة Python:
 * - اسم المكتبة والإصدار
 * - إعدادات التثبيت (ترقية، تبعيات، توافق)
 * - إصدار Python المستهدف
 * - مسار التثبيت والإعدادات المتقدمة
 */
public class InstallOptions implements Parcelable {
    
    // المعلومات الأساسية
    private String packageName;
    private String version;
    
    // إعدادات التثبيت
    private boolean upgradeIfInstalled;
    private boolean installDependencies;
    private boolean checkCompatibility;
    private String pythonVersion;
    
    // إعدادات متقدمة
    private boolean installAsUser;
    private boolean noCache;
    private boolean forceReinstall;
    private boolean preRelease;
    private boolean verbose;
    private String targetDirectory;
    private String extraIndexUrl;
    private String trustedHost;
    
    // إعدادات الشبكة
    private boolean useProxy;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;
    
    // إعدادات الأمان
    private boolean verifySSL;
    private boolean allowUnsafePackages;
    private String certPath;
    
    // إعدادات التحديث
    private boolean autoUpdate;
    private int updateCheckIntervalHours;
    private boolean notifyOnUpdate;
    
    // ====================== المنشئات ======================
    
    /**
     * إنشاء خيارات التثبيت الأساسية
     */
    public InstallOptions(String packageName) {
        this.packageName = packageName;
        this.version = null;
        this.upgradeIfInstalled = true;
        this.installDependencies = true;
        this.checkCompatibility = true;
        this.pythonVersion = "3.9";
        
        // الإعدادات الافتراضية
        this.installAsUser = true;
        this.noCache = false;
        this.forceReinstall = false;
        this.preRelease = false;
        this.verbose = false;
        this.verifySSL = true;
        this.allowUnsafePackages = false;
        this.autoUpdate = false;
        this.updateCheckIntervalHours = 24;
        this.notifyOnUpdate = true;
    }
    
    /**
     * إنشاء خيارات التثبيت الكاملة
     */
    public InstallOptions(String packageName, String version, boolean upgradeIfInstalled,
                         boolean installDependencies, boolean checkCompatibility, String pythonVersion) {
        this(packageName);
        this.version = version;
        this.upgradeIfInstalled = upgradeIfInstalled;
        this.installDependencies = installDependencies;
        this.checkCompatibility = checkCompatibility;
        this.pythonVersion = pythonVersion;
    }
    
    // ====================== getters and setters ======================
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public boolean isUpgradeIfInstalled() {
        return upgradeIfInstalled;
    }
    
    public void setUpgradeIfInstalled(boolean upgradeIfInstalled) {
        this.upgradeIfInstalled = upgradeIfInstalled;
    }
    
    public boolean isInstallDependencies() {
        return installDependencies;
    }
    
    public void setInstallDependencies(boolean installDependencies) {
        this.installDependencies = installDependencies;
    }
    
    public boolean isCheckCompatibility() {
        return checkCompatibility;
    }
    
    public void setCheckCompatibility(boolean checkCompatibility) {
        this.checkCompatibility = checkCompatibility;
    }
    
    public String getPythonVersion() {
        return pythonVersion;
    }
    
    public void setPythonVersion(String pythonVersion) {
        this.pythonVersion = pythonVersion;
    }
    
    public boolean isInstallAsUser() {
        return installAsUser;
    }
    
    public void setInstallAsUser(boolean installAsUser) {
        this.installAsUser = installAsUser;
    }
    
    public boolean isNoCache() {
        return noCache;
    }
    
    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }
    
    public boolean isForceReinstall() {
        return forceReinstall;
    }
    
    public void setForceReinstall(boolean forceReinstall) {
        this.forceReinstall = forceReinstall;
    }
    
    public boolean isPreRelease() {
        return preRelease;
    }
    
    public void setPreRelease(boolean preRelease) {
        this.preRelease = preRelease;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public String getTargetDirectory() {
        return targetDirectory;
    }
    
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }
    
    public String getExtraIndexUrl() {
        return extraIndexUrl;
    }
    
    public void setExtraIndexUrl(String extraIndexUrl) {
        this.extraIndexUrl = extraIndexUrl;
    }
    
    public String getTrustedHost() {
        return trustedHost;
    }
    
    public void setTrustedHost(String trustedHost) {
        this.trustedHost = trustedHost;
    }
    
    public boolean isUseProxy() {
        return useProxy;
    }
    
    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }
    
    public int getProxyPort() {
        return proxyPort;
    }
    
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    
    public String getProxyUser() {
        return proxyUser;
    }
    
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }
    
    public String getProxyPassword() {
        return proxyPassword;
    }
    
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
    
    public boolean isVerifySSL() {
        return verifySSL;
    }
    
    public void setVerifySSL(boolean verifySSL) {
        this.verifySSL = verifySSL;
    }
    
    public boolean isAllowUnsafePackages() {
        return allowUnsafePackages;
    }
    
    public void setAllowUnsafePackages(boolean allowUnsafePackages) {
        this.allowUnsafePackages = allowUnsafePackages;
    }
    
    public String getCertPath() {
        return certPath;
    }
    
    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
    
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }
    
    public int getUpdateCheckIntervalHours() {
        return updateCheckIntervalHours;
    }
    
    public void setUpdateCheckIntervalHours(int updateCheckIntervalHours) {
        this.updateCheckIntervalHours = updateCheckIntervalHours;
    }
    
    public boolean isNotifyOnUpdate() {
        return notifyOnUpdate;
    }
    
    public void setNotifyOnUpdate(boolean notifyOnUpdate) {
        this.notifyOnUpdate = notifyOnUpdate;
    }
    
    // ====================== طرق مساعدة ======================
    
    /**
     * بناء أمر التثبيت
     */
    public java.util.List<String> buildInstallCommand() {
        java.util.List<String> command = new java.util.ArrayList<>();
        
        // أمر pip install
        command.add("pip");
        command.add("install");
        
        // إضافة المعاملات
        if (upgradeIfInstalled) {
            command.add("--upgrade");
        }
        
        if (forceReinstall) {
            command.add("--force-reinstall");
        }
        
        if (noCache) {
            command.add("--no-cache-dir");
        }
        
        if (preRelease) {
            command.add("--pre");
        }
        
        if (installAsUser) {
            command.add("--user");
        }
        
        if (verbose) {
            command.add("--verbose");
        }
        
        if (!verifySSL) {
            command.add("--trusted-host");
            command.add("pypi.org");
            command.add("--trusted-host");
            command.add("pypi.python.org");
            command.add("--trusted-host");
            command.add("files.pythonhosted.org");
        }
        
        if (allowUnsafePackages) {
            command.add("--allow-unsafe-packages");
        }
        
        if (targetDirectory != null) {
            command.add("--target");
            command.add(targetDirectory);
        }
        
        if (extraIndexUrl != null) {
            command.add("--extra-index-url");
            command.add(extraIndexUrl);
        }
        
        if (trustedHost != null) {
            command.add("--trusted-host");
            command.add(trustedHost);
        }
        
        // إضافة اسم المكتبة والإصدار
        if (version != null && !version.isEmpty()) {
            command.add(packageName + "==" + version);
        } else {
            command.add(packageName);
        }
        
        return command;
    }
    
    /**
     * بناء أمر إلغاء التثبيت
     */
    public java.util.List<String> buildUninstallCommand() {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("pip");
        command.add("uninstall");
        command.add("-y"); // تأكيد تلقائي
        command.add(packageName);
        return command;
    }
    
    /**
     * التحقق من صحة الخيارات
     */
    public boolean isValid() {
        return packageName != null && !packageName.trim().isEmpty();
    }
    
    /**
     * التحقق من وجود خيارات متقدمة
     */
    public boolean hasAdvancedOptions() {
        return targetDirectory != null || extraIndexUrl != null || trustedHost != null ||
               useProxy || !verifySSL || allowUnsafePackages;
    }
    
    /**
     * الحصول على وصف مختصر للخيارات
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("تثبيت ").append(packageName);
        
        if (version != null && !version.isEmpty()) {
            summary.append(" v").append(version);
        }
        
        if (upgradeIfInstalled) {
            summary.append(" مع الترقية");
        }
        
        if (installDependencies) {
            summary.append(" + تبعيات");
        }
        
        if (checkCompatibility) {
            summary.append(" + فحص التوافق");
        }
        
        if (pythonVersion != null && !pythonVersion.isEmpty() && !pythonVersion.equals("الكل")) {
            summary.append(" (Python ").append(pythonVersion).append(")");
        }
        
        return summary.toString();
    }
    
    /**
     * إنشاء نسخة من الخيارات
     */
    public InstallOptions clone() {
        InstallOptions clone = new InstallOptions(packageName, version, upgradeIfInstalled,
                                                 installDependencies, checkCompatibility, pythonVersion);
        
        // نسخ الإعدادات المتقدمة
        clone.installAsUser = this.installAsUser;
        clone.noCache = this.noCache;
        clone.forceReinstall = this.forceReinstall;
        clone.preRelease = this.preRelease;
        clone.verbose = this.verbose;
        clone.targetDirectory = this.targetDirectory;
        clone.extraIndexUrl = this.extraIndexUrl;
        clone.trustedHost = this.trustedHost;
        clone.useProxy = this.useProxy;
        clone.proxyHost = this.proxyHost;
        clone.proxyPort = this.proxyPort;
        clone.proxyUser = this.proxyUser;
        clone.proxyPassword = this.proxyPassword;
        clone.verifySSL = this.verifySSL;
        clone.allowUnsafePackages = this.allowUnsafePackages;
        clone.certPath = this.certPath;
        clone.autoUpdate = this.autoUpdate;
        clone.updateCheckIntervalHours = this.updateCheckIntervalHours;
        clone.notifyOnUpdate = this.notifyOnUpdate;
        
        return clone;
    }
    
    /**
     * مقارنة مع خيارات أخرى
     */
    public boolean equals(InstallOptions other) {
        if (this == other) return true;
        if (other == null) return false;
        
        return packageName.equals(other.packageName) &&
               (version == null ? other.version == null : version.equals(other.version)) &&
               upgradeIfInstalled == other.upgradeIfInstalled &&
               installDependencies == other.installDependencies &&
               checkCompatibility == other.checkCompatibility &&
               (pythonVersion == null ? other.pythonVersion == null : pythonVersion.equals(other.pythonVersion));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InstallOptions that = (InstallOptions) obj;
        return equals(that);
    }
    
    @Override
    public int hashCode() {
        int result = packageName.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (upgradeIfInstalled ? 1 : 0);
        result = 31 * result + (installDependencies ? 1 : 0);
        result = 31 * result + (checkCompatibility ? 1 : 0);
        result = 31 * result + (pythonVersion != null ? pythonVersion.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "InstallOptions{" +
                "packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", upgradeIfInstalled=" + upgradeIfInstalled +
                ", installDependencies=" + installDependencies +
                ", checkCompatibility=" + checkCompatibility +
                ", pythonVersion='" + pythonVersion + '\'' +
                '}';
    }
    
    // ====================== Parcelable implementation ======================
    
    protected InstallOptions(Parcel in) {
        packageName = in.readString();
        version = in.readString();
        upgradeIfInstalled = in.readByte() != 0;
        installDependencies = in.readByte() != 0;
        checkCompatibility = in.readByte() != 0;
        pythonVersion = in.readString();
        installAsUser = in.readByte() != 0;
        noCache = in.readByte() != 0;
        forceReinstall = in.readByte() != 0;
        preRelease = in.readByte() != 0;
        verbose = in.readByte() != 0;
        targetDirectory = in.readString();
        extraIndexUrl = in.readString();
        trustedHost = in.readString();
        useProxy = in.readByte() != 0;
        proxyHost = in.readString();
        proxyPort = in.readInt();
        proxyUser = in.readString();
        proxyPassword = in.readString();
        verifySSL = in.readByte() != 0;
        allowUnsafePackages = in.readByte() != 0;
        certPath = in.readString();
        autoUpdate = in.readByte() != 0;
        updateCheckIntervalHours = in.readInt();
        notifyOnUpdate = in.readByte() != 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(version);
        dest.writeByte((byte) (upgradeIfInstalled ? 1 : 0));
        dest.writeByte((byte) (installDependencies ? 1 : 0));
        dest.writeByte((byte) (checkCompatibility ? 1 : 0));
        dest.writeString(pythonVersion);
        dest.writeByte((byte) (installAsUser ? 1 : 0));
        dest.writeByte((byte) (noCache ? 1 : 0));
        dest.writeByte((byte) (forceReinstall ? 1 : 0));
        dest.writeByte((byte) (preRelease ? 1 : 0));
        dest.writeByte((byte) (verbose ? 1 : 0));
        dest.writeString(targetDirectory);
        dest.writeString(extraIndexUrl);
        dest.writeString(trustedHost);
        dest.writeByte((byte) (useProxy ? 1 : 0));
        dest.writeString(proxyHost);
        dest.writeInt(proxyPort);
        dest.writeString(proxyUser);
        dest.writeString(proxyPassword);
        dest.writeByte((byte) (verifySSL ? 1 : 0));
        dest.writeByte((byte) (allowUnsafePackages ? 1 : 0));
        dest.writeString(certPath);
        dest.writeByte((byte) (autoUpdate ? 1 : 0));
        dest.writeInt(updateCheckIntervalHours);
        dest.writeByte((byte) (notifyOnUpdate ? 1 : 0));
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<InstallOptions> CREATOR = new Creator<InstallOptions>() {
        @Override
        public InstallOptions createFromParcel(Parcel in) {
            return new InstallOptions(in);
        }
        
        @Override
        public InstallOptions[] newArray(int size) {
            return new InstallOptions[size];
        }
    };
}