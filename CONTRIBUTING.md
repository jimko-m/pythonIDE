# Contributing to Python IDE for Android

Thank you for your interest in contributing to Python IDE for Android Enhanced Edition! This document provides comprehensive guidelines for contributing to the project.

## 🎯 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Guidelines](#development-guidelines)
- [Code Style Guide](#code-style-guide)
- [Testing Requirements](#testing-requirements)
- [Documentation Standards](#documentation-standards)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Feature Requests](#feature-requests)
- [Community Guidelines](#community-guidelines)

## 🤝 Code of Conduct

### Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Standards

**Encouraged behaviors:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints and experiences
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

**Unacceptable behaviors:**
- Trolling, insulting/derogatory comments, and personal or political attacks
- Public or private harassment
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate

## 🚀 Getting Started

### Prerequisites

Before contributing, ensure you have:

- [Android Studio Arctic Fox 2020.3.1+](https://developer.android.com/studio)
- [Java Development Kit 11+](https://adoptium.net/)
- [Git](https://git-scm.com/)
- [Python 3.7+](https://python.org/) (for testing Python features)

### Fork and Clone

```bash
# 1. Fork the repository on GitHub
# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/android.git
cd android

# 3. Add upstream remote
git remote add upstream https://github.com/pythonide/android.git

# 4. Create a feature branch
git checkout -b feature/your-feature-name
```

### Development Setup

Follow our [Development Setup Guide](./DEVELOPMENT.md) for detailed setup instructions.

## 📋 Development Guidelines

### Architecture Principles

1. **MVVM Architecture**: Follow Model-View-ViewModel pattern
2. **Clean Architecture**: Separate concerns and maintain clear boundaries
3. **SOLID Principles**: Apply single responsibility, open/closed, etc.
4. **Kotlin Best Practices**: Use modern Kotlin features and idioms

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/pythonide/
│   │   │   ├── ai/              # AI-powered features
│   │   │   ├── cloud/           # Cloud synchronization
│   │   │   ├── collaboration/   # Team collaboration
│   │   │   ├── data/            # Data layer (models, repos)
│   │   │   ├── editor/          # Code editor components
│   │   │   ├── packages/        # Package management
│   │   │   ├── services/        # Background services
│   │   │   ├── ui/              # User interface
│   │   │   └── utils/           # Utility classes
│   │   ├── res/                 # Android resources
│   │   └── AndroidManifest.xml
│   ├── test/                    # Unit tests
│   └── androidTest/             # Instrumentation tests
├── build.gradle
└── proguard-rules.pro
```

### Dependency Injection

Use Hilt for dependency injection:

```kotlin
// Example module
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor())
            .build()
    }
}
```

## 📝 Code Style Guide

### Kotlin Style Guide

#### Naming Conventions

```kotlin
// Classes, interfaces, enums: PascalCase
class CodeEditorActivity { }

// Functions, properties, parameters: camelCase
fun formatCode(source: String): String { }
val isValidSyntax: Boolean = true

// Constants: SCREAMING_SNAKE_CASE
companion object {
    private const val MAX_LINE_LENGTH = 120
}
```

#### Formatting Rules

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Maximum 120 characters
- **Import organization**: 
  ```kotlin
  // Standard library
  import java.util.Date
  
  // Android framework
  import android.widget.TextView
  
  // Third-party libraries
  import androidx.lifecycle.ViewModel
  
  // Project imports
  import com.pythonide.ui.activities.MainActivity
  ```

#### Code Structure

```kotlin
// Class definition
class ExampleActivity : AppCompatActivity() {
    
    // Constants
    companion object {
        private const val TAG = "ExampleActivity"
    }
    
    // Properties
    private lateinit var binding: ActivityExampleBinding
    private val viewModel: ExampleViewModel by viewModels()
    
    // Lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeViews()
        observeViewModel()
    }
    
    // Private methods
    private fun initializeViews() {
        // Initialization code
    }
}
```

### XML Style Guide

```xml
<!-- Use meaningful IDs -->
<EditText
    android:id="@+id/et_code_input"
    android:layout_width="match_parent"
    android:layout_height="200dp"
    android:imeOptions="actionNext"
    android:inputType="textMultiLine|textNoSuggestions" />

<!-- Organize attributes alphabetically -->
<Button
    android:id="@+id/btn_run_code"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:text="@string/run_code" />
```

### Database Guidelines

```kotlin
// Entity definitions
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val lastModified: Long
)

// DAO interface
@Dao
interface FileDao {
    
    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    suspend fun getAllFiles(): List<FileEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)
    
    @Delete
    suspend fun deleteFile(file: FileEntity)
}
```

## 🧪 Testing Requirements

### Unit Testing

All new features must include unit tests:

```kotlin
// Example test
@Test
fun `test code formatting`() {
    // Given
    val messyCode = "def test(   a,b   ):\nprint(a)"
    
    // When
    val formatted = formatter.formatCode(messyCode)
    
    // Then
    assertEquals("def test(a, b):\n    print(a)", formatted)
}
```

### Test Coverage

- **Minimum coverage**: 80% for new code
- **Critical components**: 95%+ coverage
- **UI components**: Include integration tests

### Testing Guidelines

1. **Test Naming**: Use descriptive test names that explain the scenario
2. **Test Structure**: Follow Given-When-Then pattern
3. **Mock Appropriately**: Mock external dependencies
4. **Test Data**: Use realistic test data
5. **Assertions**: Use specific assertions, not just generic checks

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Run instrumentation tests
./gradlew connectedAndroidTest
```

## 📚 Documentation Standards

### Code Documentation

All public APIs must be documented:

```kotlin
/**
 * Formats Python code according to PEP 8 guidelines.
 * 
 * This function applies automatic formatting to Python source code,
 * including proper indentation, spacing, and line breaks.
 *
 * @param code The Python source code to format
 * @return Formatted code with proper styling
 * @throws FormattingException if the code contains syntax errors
 * 
 * Example:
 * ```
 * val formatted = formatter.formatCode("def test( a , b ):print(a,b)")
 * // Returns: "def test(a, b):\n    print(a, b)"
 * ```
 */
fun formatCode(code: String): String {
    // Implementation
}
```

### README Updates

When adding new features:

1. Update the main README.md with feature descriptions
2. Add screenshots for UI changes
3. Update the FEATURES.md list
4. Include installation/build instructions

### API Documentation

New APIs must include:

- Class/method documentation
- Parameter descriptions
- Return value documentation
- Usage examples
- Error conditions

## 🔄 Pull Request Process

### Before Submitting

1. **Code Review**: Self-review your code
2. **Tests Pass**: Ensure all tests pass locally
3. **Documentation**: Update relevant documentation
4. **Changelog**: Add entry to CHANGELOG.md
5. **Clean History**: Squash unnecessary commits

### Pull Request Template

```markdown
## 📋 Summary
Brief description of changes

## 🎯 Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## 🧪 Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Test coverage maintained

## 📸 Screenshots
If applicable, add screenshots of UI changes

## 📝 Changelog Entry
Added to CHANGELOG.md with appropriate version
```

### Code Review Process

1. **Automated Checks**: CI/CD pipeline must pass
2. **Review**: At least one maintainer review required
3. **Testing**: Changes tested on multiple devices
4. **Documentation**: Documentation reviewed
5. **Approval**: All comments addressed

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

[optional body]

[optional footer(s)]
```

Examples:
```bash
feat(editor): add multi-cursor editing support
fix(ai): resolve code completion crash on empty files
docs(api): update AICodeAssistant documentation
test(cloud): add unit tests for sync functionality
```

## 🐛 Issue Reporting

### Bug Reports

Use the bug report template:

```markdown
## 🐛 Bug Description
Clear description of the bug

## 🔍 Steps to Reproduce
1. Open app
2. Navigate to...
3. Click on...
4. Scroll to...
5. See error

## ✅ Expected Behavior
What should happen

## 📱 Device Information
- Device: [e.g. Pixel 4]
- OS Version: [e.g. Android 11]
- App Version: [e.g. 2.0.0]

## 📋 Additional Context
Screenshots, logs, other context
```

### Feature Requests

Use the feature request template:

```markdown
## 💡 Feature Description
Clear description of the proposed feature

## 🎯 Use Case
Why is this feature needed?

## 💻 Implementation Ideas
How might this be implemented?

## 🔄 Alternative Solutions
Other ways this could be solved

## 📋 Additional Context
Mockups, references, other context
```

## 💬 Community Guidelines

### Communication

- **Be Respectful**: Treat all community members with respect
- **Stay On Topic**: Keep discussions relevant to the project
- **Constructive Criticism**: Provide helpful, constructive feedback
- **Help Others**: Assist new contributors and users

### Support Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General questions and ideas
- **Discord**: Real-time chat and support
- **Stack Overflow**: Technical questions (tag: `python-ide-android`)

### Getting Help

1. **Search First**: Check existing issues and documentation
2. **Ask Questions**: Use appropriate channels
3. **Provide Context**: Include relevant information
4. **Be Patient**: Allow time for responses

## 🎯 Contribution Types

### Code Contributions

- **Bug Fixes**: Fix reported issues
- **New Features**: Add new functionality
- **Performance**: Optimize existing code
- **Refactoring**: Improve code structure

### Documentation

- **API Documentation**: Document new APIs
- **User Guides**: Write user-facing documentation
- **Tutorials**: Create step-by-step guides
- **Examples**: Provide code examples

### Testing

- **Unit Tests**: Add test coverage
- **Integration Tests**: Test component interactions
- **Performance Tests**: Add performance benchmarks
- **UI Tests**: Test user interfaces

### Community

- **Issue Triage**: Help categorize and prioritize issues
- **Code Review**: Review others' contributions
- **Mentoring**: Help new contributors get started
- **Translation**: Translate documentation

## 🏆 Recognition

Contributors are recognized through:

- **Contributors List**: Added to README.md
- **Release Notes**: Mentioned in release notes
- **GitHub Contributors**: Visible on repository
- **Special Mentions**: For significant contributions

## 📞 Contact

### Maintainers

- **Lead Developer**: [maintainer@pythonide.com](mailto:maintainer@pythonide.com)
- **Project Owner**: [owner@pythonide.com](mailto:owner@pythonide.com)

### Getting Started Help

If you need help getting started:

1. Check the [Development Guide](./DEVELOPMENT.md)
2. Join our [Discord community](https://discord.gg/pythonide)
3. Ask questions in [GitHub Discussions](https://github.com/pythonide/android/discussions)
4. Contact maintainers directly

## 🙏 Thank You

Thank you for contributing to Python IDE for Android! Your contributions help make this project better for everyone. Whether you're fixing bugs, adding features, or improving documentation, every contribution is valuable and appreciated.

---

**Happy Contributing! 🚀**