package com.pythonide.libraries;

import android.os.Parcelable;
import android.os.Parcel;
import java.util.List;

/**
 * Library - فئة تمثل مكتبة Python
 */
public class Library implements Parcelable {
    private String name;
    private String description;
    private String version;
    private boolean isInstalled;
    private String license;
    private List<String> dependencies;
    private List<String> classifiers;
    private String homePage;
    private String author;
    private String authorEmail;
    private long size;
    private int downloads;
    private String pythonVersion;
    private List<String> keywords;
    private String summary;
    
    public Library(String name, String description, String version, boolean isInstalled,
                   String license, List<String> dependencies, List<String> classifiers, String homePage) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.isInstalled = isInstalled;
        this.license = license;
        this.dependencies = dependencies;
        this.classifiers = classifiers;
        this.homePage = homePage;
    }
    
    public Library(String name, String description, String version, boolean isInstalled,
                   String license, List<String> dependencies, List<String> classifiers, 
                   String homePage, String author, String authorEmail, long size,
                   int downloads, String pythonVersion, List<String> keywords, String summary) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.isInstalled = isInstalled;
        this.license = license;
        this.dependencies = dependencies;
        this.classifiers = classifiers;
        this.homePage = homePage;
        this.author = author;
        this.authorEmail = authorEmail;
        this.size = size;
        this.downloads = downloads;
        this.pythonVersion = pythonVersion;
        this.keywords = keywords;
        this.summary = summary;
    }
    
    protected Library(Parcel in) {
        name = in.readString();
        description = in.readString();
        version = in.readString();
        isInstalled = in.readByte() != 0;
        license = in.readString();
        dependencies = in.createStringArrayList();
        classifiers = in.createStringArrayList();
        homePage = in.readString();
        author = in.readString();
        authorEmail = in.readString();
        size = in.readLong();
        downloads = in.readInt();
        pythonVersion = in.readString();
        keywords = in.createStringArrayList();
        summary = in.readString();
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public boolean isInstalled() { return isInstalled; }
    public String getLicense() { return license; }
    public List<String> getDependencies() { return dependencies; }
    public List<String> getClassifiers() { return classifiers; }
    public String getHomePage() { return homePage; }
    public String getAuthor() { return author; }
    public String getAuthorEmail() { return authorEmail; }
    public long getSize() { return size; }
    public int getDownloads() { return downloads; }
    public String getPythonVersion() { return pythonVersion; }
    public List<String> getKeywords() { return keywords; }
    public String getSummary() { return summary; }
    
    // Setters
    public void setInstalled(boolean installed) { isInstalled = installed; }
    public void setVersion(String version) { this.version = version; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Library library = (Library) obj;
        return name.equals(library.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "Library{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", isInstalled=" + isInstalled +
                '}';
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(version);
        dest.writeByte((byte) (isInstalled ? 1 : 0));
        dest.writeString(license);
        dest.writeStringList(dependencies);
        dest.writeStringList(classifiers);
        dest.writeString(homePage);
        dest.writeString(author);
        dest.writeString(authorEmail);
        dest.writeLong(size);
        dest.writeInt(downloads);
        dest.writeString(pythonVersion);
        dest.writeStringList(keywords);
        dest.writeString(summary);
    }
    
    public static final Creator<Library> CREATOR = new Creator<Library>() {
        @Override
        public Library createFromParcel(Parcel in) {
            return new Library(in);
        }
        
        @Override
        public Library[] newArray(int size) {
            return new Library[size];
        }
    };
}