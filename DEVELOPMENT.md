# Development Setup Guide

## 🚀 Quick Start for Contributors

This guide will help you set up a complete development environment for Python IDE for Android Enhanced Edition.

## 📋 Prerequisites

### Required Software

1. **Android Studio** (Latest version)
   - Download: https://developer.android.com/studio
   - Minimum version: Arctic Fox 2020.3.1
   - Required components:
     - Android SDK
     - Android SDK Platform
     - Android Virtual Device
     - Build Tools

2. **Java Development Kit**
   ```bash
   # Check Java version
   java -version
   # Should be Java 11 or higher
   
   # If not installed, install OpenJDK 11
   sudo apt install openjdk-11-jdk  # Ubuntu/Debian
   brew install openjdk@11         # macOS
   ```

3. **Git**
   ```bash
   # Install Git
   sudo apt install git  # Ubuntu/Debian
   brew install git      # macOS
   
   # Configure Git
   git config --global user.name "Your Name"
   git config --global user.email "your.email@example.com"
   ```

4. **Python 3.7+** (For testing Python features)
   ```bash
   # Install Python 3.7+
   sudo apt install python3 python3-pip python3-venv  # Ubuntu/Debian
   brew install python3  # macOS
   ```

### Hardware Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space for Android Studio and SDK
- **CPU**: Intel i5/AMD Ryzen 5 or equivalent
- **OS**: Windows 10+, macOS 10.15+, or Ubuntu 18.04+

## 🛠️ Project Setup

### 1. Clone the Repository

```bash
# Clone the main repository
git clone https://github.com/pythonide/android.git
cd android

# Add upstream remote for keeping in sync
git remote add upstream https://github.com/pythonide/android.git

# Create a development branch
git checkout -b feature/your-feature-name
```

### 2. Android Studio Setup

#### Import Project

1. **Open Android Studio**
2. **Click "Open an Existing Android Studio Project"**
3. **Navigate to the cloned repository directory**
4. **Select the `android/` folder**
5. **Wait for Gradle sync to complete**

#### Configure SDK

1. **Go to File → Project Structure**
2. **Project Tab:**
   - Project SDK: Android API 33 (or latest available)
   - Project language level: 8+
3. **Modules Tab:**
   - Compile SDK: 33
   - Build Tools: 33.0.0

#### SDK Configuration

```bash
# Via command line (alternative)
sdkmanager "platforms;android-33"
sdkmanager "build-tools;33.0.0"
sdkmanager "system-images;android-33;google_apis;x86_64"
```

### 3. Environment Variables

Create or modify your environment variables:

**Windows:**
```cmd
# User environment variables
ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

**macOS/Linux:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
```

### 4. Build Configuration

#### Local Properties

Create `local.properties` in the project root:

```properties
# Add your paths
sdk.dir=/Users/username/Library/Android/sdk
# Windows example: sdk.dir=C:\\Users\\username\\AppData\\Local\\Android\\Sdk

# Optional: Custom signing configuration
# MYAPP_UPLOAD_STORE_FILE=my-upload-key.keystore
# MYAPP_UPLOAD_KEY_ALIAS=my-key-alias
# MYAPP_UPLOAD_STORE_PASSWORD=******
# MYAPP_UPLOAD_KEY_PASSWORD=******
```

#### Gradle Configuration

The project uses **Gradle 7.4** and **Android Gradle Plugin 7.3.1**.

Key build configurations:
- **Debug builds**: Instant run enabled, debugging enabled
- **Release builds**: ProGuard/R8 enabled, optimization enabled
- **CI builds**: Parallel builds, caching enabled

### 5. Dependency Management

#### Core Dependencies

The project uses several key dependencies:

- **Kotlin**: Primary language
- **AndroidX Libraries**: Modern Android components
- **Material Design**: UI components
- **Room**: Database
- **Retrofit**: HTTP client
- **OkHttp**: Networking
- **Glide**: Image loading

#### Adding New Dependencies

```kotlin
// In app/build.gradle.kts
dependencies {
    // Add dependencies here
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
}
```

## 🧪 Testing Setup

### 1. Unit Testing

```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew testDebugUnitTest --tests="com.pythonide.ai.AICodeAssistantTest"

# Run with coverage
./gradlew testDebugUnitTestCoverage
```

### 2. Instrumentation Testing

```bash
# Connect device or start emulator
adb devices

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest --tests="com.pythonide.ui.activities.MainActivityTest"
```

### 3. Test Configuration

Create test resources in `src/test/res/` and `src/androidTest/res/`:

```kotlin
// Example test setup
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testInitialState() {
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }
}
```

## 🏗️ Build Variants

### Build Types

1. **Debug**
   - Debugging enabled
   - Instant run supported
   - No ProGuard
   - Test coverage enabled

2. **Release**
   - Debugging disabled
   - ProGuard/R8 enabled
   - Minification enabled
   - Test coverage disabled

### Product Flavors

- **Basic**: Core features only
- **Enhanced**: All features except enterprise
- **Enterprise**: Full feature set with enterprise security

#### Building Specific Variants

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build specific flavor
./gradlew assembleEnhancedDebug

# Build for all variants
./gradlew assemble
```

## 🔧 Development Workflow

### 1. Feature Development

```bash
# Create feature branch
git checkout -b feature/amazing-new-feature

# Make changes and commit regularly
git add .
git commit -m "feat: add amazing new feature"

# Push branch
git push origin feature/amazing-new-feature

# Create Pull Request on GitHub
```

### 2. Code Style Guidelines

#### Kotlin Style Guide

- Use **4 spaces** for indentation
- Maximum line length: **120 characters**
- Use **meaningful variable names**
- Follow **Kotlin coding conventions**

```kotlin
// Good example
class CodeEditorActivity : AppCompatActivity() {
    
    private val editorManager by lazy { EditorManager() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_editor)
        initializeEditor()
    }
    
    private fun initializeEditor() {
        editorManager.initialize(this)
    }
}
```

#### XML Style Guide

```xml
<!-- Use meaningful IDs -->
<EditText
    android:id="@+id/et_code_editor"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:imeOptions="actionNext"
    android:inputType="textMultiLine" />

<!-- Organize attributes alphabetically -->
<Button
    android:id="@+id/btn_run_code"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/run_code" />
```

### 3. Git Commit Convention

Follow **Conventional Commits** specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Types:
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Formatting changes
- **refactor**: Code refactoring
- **test**: Adding tests
- **chore**: Maintenance tasks

Examples:
```bash
git commit -m "feat(ai): add intelligent code completion"
git commit -m "fix(editor): resolve cursor position issue"
git commit -m "docs: update API documentation"
```

## 🐛 Debugging & Profiling

### 1. Debugging Setup

#### Enable Debug Mode

```kotlin
// In your code
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Debug information")
}
```

#### Debugging with Android Studio

1. **Set breakpoints** by clicking in the gutter
2. **Debug the app** using Shift+F9
3. **Use debug tools**:
   - Variables view
   - Watches
   - Evaluate expression
   - Call stack

### 2. Performance Profiling

#### Memory Profiling

```bash
# Generate heap dump
adb shell dumpsys meminfo com.pythonide

# Use Android Studio Memory Profiler
# Tools → Android → Android Device Monitor
```

#### CPU Profiling

```bash
# Use Android Studio CPU Profiler
# Run → Edit Configurations → Profiling → Record CPU activity
```

### 3. Logging Configuration

```kotlin
// Create a custom logger
object Logger {
    private const val TAG = "PythonIDE"
    
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
```

## 📱 Device Testing

### 1. Physical Devices

#### Enable Developer Options

1. **Settings → About Phone**
2. **Tap "Build Number" 7 times**
3. **Go back to Settings → Developer Options**
4. **Enable "USB Debugging"**

#### Testing on Device

```bash
# Connect device via USB
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat
```

### 2. Emulator Setup

#### Create Virtual Device

1. **Android Studio → AVD Manager**
2. **Click "Create Virtual Device"**
3. **Select device** (Pixel 4 recommended)
4. **Choose system image** (API 30+ recommended)
5. **Configure and create**

#### Emulator Optimization

```bash
# Use hardware acceleration (macOS/Linux)
emulator -avd Pixel_4_API_30 -gpu host

# Enable quick boot
# AVD Manager → Edit → Advanced Settings → Enable quick boot
```

## 🔒 Security & Privacy

### 1. Secure Development

#### Sensitive Data Handling

```kotlin
// Use EncryptedSharedPreferences
val sharedPreferences = EncryptedSharedPreferences.create(
    "secret_prefs",
    MasterKeys.AES256_GCM_SPEC,
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

#### Network Security

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.pythonide.com</domain>
    </domain-config>
</network-security-config>
```

### 2. Privacy Compliance

#### GDPR Compliance

```kotlin
// Privacy manager
class PrivacyManager {
    fun showConsentDialog(context: Context) {
        // Show GDPR consent dialog
    }
    
    fun isConsentGiven(): Boolean {
        // Check if user has given consent
        return false
    }
}
```

## 📦 Packaging & Distribution

### 1. Release Build

#### Generate Signed APK

```bash
# Generate signing key
keytool -genkey -v -keystore pythonide-release-key.keystore -alias pythonide -keyalg RSA -keysize 2048 -validity 10000

# Configure signing in build.gradle
android {
    signingConfigs {
        release {
            storeFile file('../pythonide-release-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'pythonide'
            keyPassword 'your_key_password'
        }
    }
}

# Build release APK
./gradlew assembleRelease
```

#### App Bundle (Recommended)

```bash
# Generate App Bundle
./gradlew bundleRelease

# Upload to Play Console
# Play Console → Release → App Releases → Internal testing
```

### 2. Continuous Integration

#### GitHub Actions Setup

See [CI/CD Configuration](.github/workflows/ci-cd.yml) for complete pipeline setup.

#### Local CI Simulation

```bash
# Run full test suite
./gradlew clean test connectedAndroidTest lint

# Build all variants
./gradlew clean assemble

# Generate test reports
./gradlew jacocoTestReport
```

## 📚 Resources & Documentation

### 1. API Documentation

```bash
# Generate KDoc documentation
./gradlew dokka

# Output in: build/dokka/html/
```

### 2. Code References

- **Android Developer Guide**: https://developer.android.com/guide
- **Kotlin Documentation**: https://kotlinlang.org/docs/
- **Material Design**: https://material.io/design
- **Android Architecture Guide**: https://developer.android.com/topic/architecture

### 3. Community Resources

- **Discord Community**: https://discord.gg/pythonide
- **Stack Overflow Tag**: `python-ide-android`
- **GitHub Discussions**: https://github.com/pythonide/android/discussions

## 🐛 Troubleshooting

### Common Issues

#### Gradle Sync Issues

```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies

# Delete Gradle cache
rm -rf ~/.gradle/caches/
```

#### Android Studio Issues

```bash
# Invalidate caches
# File → Invalidate Caches and Restart

# Reset Android Studio settings
rm -rf ~/Library/Preferences/AndroidStudio*
```

#### Build Failures

```bash
# Check specific issues
./gradlew assembleDebug --info --stacktrace

# Update build tools
sdkmanager "build-tools;33.0.0"
```

#### Performance Issues

```bash
# Increase Gradle heap size
# Add to gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

## 🎯 Best Practices

### 1. Performance

- **Use RecyclerView** for large lists
- **Implement lazy loading** for large files
- **Cache computed results** appropriately
- **Use background threads** for heavy operations

### 2. Memory Management

- **Use weak references** where appropriate
- **Close resources** properly (try-with-resources)
- **Avoid memory leaks** with proper Context usage
- **Use Android lifecycle components**

### 3. User Experience

- **Follow Material Design** guidelines
- **Provide loading states** for async operations
- **Handle edge cases** gracefully
- **Test on multiple devices** and screen sizes

### 4. Code Quality

- **Write comprehensive tests** (unit + integration)
- **Follow SOLID principles**
- **Document complex logic**
- **Review code** before committing

---

## 🎉 You're Ready to Contribute!

With this setup complete, you're ready to start developing amazing features for Python IDE for Android. Happy coding! 🚀

### Quick Reference Commands

```bash
# Development workflow
git checkout -b feature/new-feature
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
git add . && git commit -m "feat: add new feature"
git push origin feature/new-feature

# Testing
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint

# Build
./gradlew assembleRelease
./gradlew bundleRelease
```