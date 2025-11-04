package com.pythonide.templates

import android.content.Context
import com.pythonide.models.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the complete project creation workflow from template selection to project initialization.
 * This class coordinates between template management and project creation.
 */
class ProjectCreationManager(private val context: Context) {
    
    private val templateManager = ProjectTemplateManager(context)
    
    /**
     * Create a new project from a template with complete workflow
     */
    suspend fun createProject(
        templateId: String,
        projectName: String,
        projectPath: String,
        customization: TemplateCustomization
    ): Result<ProjectCreationResult> = withContext(Dispatchers.IO) {
        try {
            // Validate project name
            val validation = templateManager.validateProjectName(projectName)
            if (!validation.isValid) {
                return@withContext Result.failure(ProjectCreationException(validation.message))
            }
            
            // Validate template exists
            val template = templateManager.getTemplate(templateId)
                ?: return@withContext Result.failure(ProjectCreationException("Template not found"))
            
            // Create project directory
            val projectDir = File(projectPath, projectName)
            if (projectDir.exists()) {
                return@withContext Result.failure(ProjectCreationException("Project directory already exists"))
            }
            
            // Generate project from template
            val projectResult = templateManager.createProjectFromTemplate(
                templateId = templateId,
                projectName = projectName,
                projectPath = projectPath,
                customization = customization
            )
            
            if (projectResult.isFailure) {
                return@withContext Result.failure(ProjectCreationException(projectResult.exceptionOrNull()?.message ?: "Failed to create project"))
            }
            
            val project = projectResult.getOrThrow()
            
            // Initialize project-specific files
            initializeProjectFiles(projectDir, project, customization)
            
            // Create project metadata
            val creationResult = ProjectCreationResult(
                project = project,
                templateInfo = template.getTemplateInfo(),
                customization = customization,
                creationTime = System.currentTimeMillis()
            )
            
            Result.success(creationResult)
            
        } catch (e: Exception) {
            Result.failure(ProjectCreationException("Project creation failed: ${e.message}", e))
        }
    }
    
    /**
     * Get available project templates with filtering
     */
    fun getAvailableTemplates(category: String? = null, searchQuery: String? = null): List<ProjectTemplate> {
        var templates = templateManager.getAvailableTemplates()
        
        // Filter by category
        category?.let { categoryFilter ->
            if (categoryFilter != "all") {
                templates = templates.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            }
        }
        
        // Filter by search query
        searchQuery?.let { query ->
            val lowercaseQuery = query.lowercase()
            templates = templates.filter { template ->
                template.name.lowercase().contains(lowercaseQuery) ||
                template.description.lowercase().contains(lowercaseQuery) ||
                template.tags.any { it.lowercase().contains(lowercaseQuery) }
            }
        }
        
        return templates.sortedBy { it.name }
    }
    
    /**
     * Get template customization options
     */
    fun getTemplateCustomizationOptions(templateId: String): TemplateCustomizationOptions? {
        return templateManager.getTemplateCustomizationOptions(templateId)
    }
    
    /**
     * Create default customization for a template
     */
    fun createDefaultCustomization(templateId: String): TemplateCustomization {
        val options = templateManager.getTemplateCustomizationOptions(templateId)
            ?: TemplateCustomizationOptions()
        
        return TemplateCustomization(
            options = options,
            customVariables = emptyMap()
        )
    }
    
    /**
     * Validate project path
     */
    fun validateProjectPath(projectPath: String): ValidationResult {
        val pathFile = File(projectPath)
        
        if (projectPath.isBlank()) {
            return ValidationResult(false, "Project path cannot be empty")
        }
        
        if (!pathFile.parentFile?.exists() == true) {
            return ValidationResult(false, "Parent directory does not exist")
        }
        
        if (!pathFile.parentFile?.canWrite() == true) {
            return ValidationResult(false, "Cannot write to parent directory")
        }
        
        return ValidationResult(true)
    }
    
    /**
     * Initialize project-specific files after template generation
     */
    private fun initializeProjectFiles(
        projectDir: File,
        project: Project,
        customization: TemplateCustomization
    ) {
        // Create IDE-specific configuration files
        createIDeConfigurationFiles(projectDir, project, customization)
        
        // Create git repository if needed
        if (customization.options.enableTests || customization.options.enableDocumentation) {
            initializeGitRepository(projectDir)
        }
        
        // Create virtual environment files (for Python projects)
        createVirtualEnvironmentFiles(projectDir, customization)
        
        // Create development configuration files
        createDevelopmentFiles(projectDir, customization)
    }
    
    /**
     * Create IDE-specific configuration files
     */
    private fun createIDeConfigurationFiles(
        projectDir: File,
        project: Project,
        customization: TemplateCustomization
    ) {
        val configDir = File(projectDir, ".pythonide")
        configDir.mkdirs()
        
        // Project configuration
        val projectConfig = mapOf(
            "name" to project.name,
            "type" to project.type,
            "path" to project.path,
            "createdAt" to project.createdAt,
            "templateVersion" to "1.0.0",
            "customization" to mapOf(
                "database" to customization.options.enableDatabase,
                "authentication" to customization.options.enableAuthentication,
                "api" to customization.options.enableApi,
                "tests" to customization.options.enableTests,
                "documentation" to customization.options.enableDocumentation
            )
        )
        
        val configFile = File(configDir, "project.json")
        configFile.writeText(gsonToJson(projectConfig))
        
        // Workspace settings
        val workspaceConfig = mapOf(
            "settings" to mapOf(
                "autoSave" to true,
                "formatOnSave" to true,
                "enableLinting" to customization.options.enableTests,
                "pythonPath" to ".venv/bin/python"
            ),
            "extensions" to listOf(
                "ms-python.python",
                "ms-python.flake8",
                "ms-python.pylint",
                "ms-python.black-formatter"
            )
        )
        
        val workspaceFile = File(configDir, "workspace.json")
        workspaceFile.writeText(gsonToJson(workspaceConfig))
    }
    
    /**
     * Initialize git repository
     */
    private fun initializeGitRepository(projectDir: File) {
        val gitDir = File(projectDir, ".git")
        if (!gitDir.exists()) {
            try {
                // Initialize git repository
                Runtime.getRuntime().exec(arrayOf("git", "init"), null, projectDir).waitFor()
                
                // Create .gitignore
                val gitignoreContent = """
# IDE files
.pythonide/
.vscode/
.idea/
*.swp
*.swo

# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
build/
develop-eggs/
dist/
downloads/
eggs/
.eggs/
lib/
lib64/
parts/
sdist/
var/
wheels/
*.egg-info/
.installed.cfg
*.egg

# Virtual environments
.venv/
env/
venv/
ENV/
env.bak/
venv.bak/

# Environment variables
.env
.env.local
.env.production

# Database
*.db
*.sqlite
*.sqlite3

# Logs
logs/
*.log

# OS files
.DS_Store
Thumbs.db

# Temporary files
*.tmp
*.temp
                """.trimIndent()
                
                File(projectDir, ".gitignore").writeText(gitignoreContent)
                
            } catch (e: Exception) {
                // Git initialization failed, continue without it
            }
        }
    }
    
    /**
     * Create virtual environment configuration files
     */
    private fun createVirtualEnvironmentFiles(
        projectDir: File,
        customization: TemplateCustomization
    ) {
        // Create requirements.txt with additional dependencies
        val requirementsFile = File(projectDir, "requirements-dev.txt")
        val devRequirements = """
# Development requirements
pytest>=7.0.0
black>=23.0.0
flake8>=6.0.0
pylint>=2.15.0
mypy>=1.0.0
pre-commit>=3.0.0
        """.trimIndent()
        
        requirementsFile.writeText(devRequirements)
        
        // Create activation script for virtual environment
        val activateScript = """
#!/bin/bash
# Activate virtual environment for ${projectDir.name}

if [ ! -d ".venv" ]; then
    python3 -m venv .venv
fi

source .venv/bin/activate
echo "Virtual environment activated for ${projectDir.name}"
        """.trimIndent()
        
        val activateFile = File(projectDir, "activate.sh")
        activateFile.writeText(activateScript)
        activateFile.setExecutable(true)
    }
    
    /**
     * Create development configuration files
     */
    private fun createDevelopmentFiles(
        projectDir: File,
        customization: TemplateCustomization
    ) {
        // Create Makefile for common tasks
        val makefileContent = """
.PHONY: install test lint format clean run

install:
\tpip install -r requirements.txt
\tpip install -r requirements-dev.txt

test:
\tpytest

lint:
\tflake8 .
\tpylint *.py

format:
\tblack .
\tisort .

clean:
\tfind . -type f -name "*.pyc" -delete
\tfind . -type d -name "__pycache__" -delete

run:
\tpython main.py

# Docker commands
docker-build:
\tdocker build -t ${projectDir.name} .

docker-run:
\tdocker run -p 8000:8000 ${projectDir.name}
        """.trimIndent()
        
        File(projectDir, "Makefile").writeText(makefileContent)
        
        // Create pre-commit configuration
        if (customization.options.enableTests) {
            val precommitConfig = """
repos:
-   repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.4.0
    hooks:
    -   id: trailing-whitespace
    -   id: end-of-file-fixer
    -   id: check-yaml
    -   id: check-added-large-files
-   repo: https://github.com/psf/black
    rev: 23.3.0
    hooks:
    -   id: black
-   repo: https://github.com/pycqa/isort
    rev: 5.12.0
    hooks:
    -   id: isort
-   repo: https://github.com/pycqa/flake8
    rev: 6.0.0
    hooks:
    -   id: flake8
            """.trimIndent()
            
            File(projectDir, ".pre-commit-config.yaml").writeText(precommitConfig)
        }
    }
    
    /**
     * Convert Map to JSON string (simplified version)
     */
    private fun gsonToJson(obj: Any): String {
        return when (obj) {
            is Map<*, *> -> {
                val entries = obj.entries.map { (key, value) ->
                    "\"$key\": ${gsonToJson(value!!)}"
                }
                "{\n${entries.joinToString(",\n")}\n}"
            }
            is List<*> -> {
                val items = obj.map { gsonToJson(it!!) }
                "[\n${items.joinToString(",\n")}\n]"
            }
            is String -> "\"$obj\""
            is Number -> obj.toString()
            is Boolean -> obj.toString()
            else -> "\"$obj\""
        }
    }
}

/**
 * Result of project creation
 */
data class ProjectCreationResult(
    val project: Project,
    val templateInfo: ProjectTemplate,
    val customization: TemplateCustomization,
    val creationTime: Long
)

/**
 * Exception for project creation errors
 */
class ProjectCreationException(message: String, cause: Throwable? = null) : Exception(message, cause)