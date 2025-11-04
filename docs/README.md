# API Documentation

This directory contains comprehensive API documentation for Python IDE for Android Enhanced Edition.

## 📚 Documentation Structure

- **[Core APIs](./api/)** - Core application APIs and services
- **[UI Components](./api/ui/)** - User interface component documentation
- **[Data Layer](./api/data/)** - Database and data management APIs
- **[Cloud Services](./api/cloud/)** - Cloud synchronization and collaboration APIs
- **[AI Services](./api/ai/)** - AI-powered development features
- **[Development Guides](../guides/)** - Development and integration guides

## 🚀 Quick Links

### Core APIs
- [Application](./api/Application.md) - Main application class
- [EditorManager](./api/EditorManager.md) - Code editor management
- [TerminalService](./api/TerminalService.md) - Terminal emulator service
- [PackageManager](./api/PackageManager.md) - Package and dependency management

### UI Components
- [MainActivity](./api/MainActivity.md) - Main application activity
- [CodeEditorActivity](./api/CodeEditorActivity.md) - Code editing interface
- [FileManagerActivity](./api/FileManagerActivity.md) - File management interface

### Data & Services
- [FileRepository](./api/FileRepository.md) - File data access layer
- [GitService](./api/GitService.md) - Git version control service
- [CloudStorageManager](./api/CloudStorageManager.md) - Cloud storage management

### AI Features
- [AICodeAssistant](./api/AICodeAssistant.md) - AI-powered code assistance
- [CodeCompletionEngine](./api/CodeCompletionEngine.md) - Intelligent code completion
- [SmartFormatter](./api/SmartFormatter.md) - Code formatting and style

## 🔧 Usage Examples

### Basic Editor Setup
```kotlin
// Initialize editor with AI assistance
val editorManager = EditorManager(context)
editorManager.initializeWithAI(true)

// Enable real-time syntax checking
editorManager.enableSyntaxCheck(true)

// Set up code completion
editorManager.setCompletionEngine(AICodeAssistant())
```

### Cloud Sync Integration
```kotlin
// Enable cloud synchronization
val cloudManager = CloudStorageManager()
cloudManager.enableSync(userId, projectId)

// Monitor sync status
cloudManager.getSyncStatus(projectId) { status ->
    when (status) {
        SyncStatus.Synced -> println("Project synced")
        SyncStatus.Syncing -> println("Syncing...")
        SyncStatus.Error -> println("Sync error")
    }
}
```

### Package Management
```kotlin
// Install package
packageManager.installPackage("requests") { result ->
    if (result.success) {
        println("Package installed successfully")
    }
}

// Create virtual environment
packageManager.createVirtualEnvironment("my_env") { envPath ->
    println("Environment created at: $envPath")
}
```

## 📖 Getting Started

1. **[Setup Guide](../guides/Getting_Started.md)** - How to integrate with the IDE
2. **[Development Guide](../guides/Development.md)** - Building and extending the IDE
3. **[API Reference](./api/)** - Complete API reference documentation

## 🤝 Contributing

To contribute to the API documentation:

1. Update relevant API documentation files
2. Add new API reference files for new components
3. Update this index file
4. Ensure all code examples are tested and working

## 📞 Support

For API questions and support:

- **GitHub Issues**: [Report bugs and request features](https://github.com/pythonide/android/issues)
- **Discord Community**: [Join our developer community](https://discord.gg/pythonide)
- **Stack Overflow**: [Ask questions with tag `python-ide-android`](https://stackoverflow.com/questions/tagged/python-ide-android)

---

**💡 This API documentation is continuously updated with new features and improvements.**