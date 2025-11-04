package com.pythonide.templates

import android.content.Context
import com.pythonide.models.Project
import com.pythonide.templates.templates.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages project templates and scaffolding for the Python IDE.
 * Provides functionality to create new projects from pre-built templates.
 */
class ProjectTemplateManager(private val context: Context) {
    
    private val templates = mapOf(
        "flask" to FlaskTemplate(),
        "django" to DjangoTemplate(),
        "fastapi" to FastAPITemplate(),
        "datascience" to DataScienceTemplate()
    )
    
    /**
     * Get list of available project templates
     */
    fun getAvailableTemplates(): List<ProjectTemplate> {
        return templates.values.map { it.getTemplateInfo() }
    }
    
    /**
     * Get template by ID
     */
    fun getTemplate(templateId: String): ProjectTemplate? {
        return templates[templateId]
    }
    
    /**
     * Create a new project from template
     */
    suspend fun createProjectFromTemplate(
        templateId: String,
        projectName: String,
        projectPath: String,
        customizationOptions: TemplateCustomization? = null
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val template = templates[templateId] 
                ?: return@withContext Result.failure(Exception("Template not found: $templateId"))
            
            val projectDir = File(projectPath, projectName)
            if (projectDir.exists()) {
                return@withContext Result.failure(Exception("Project directory already exists"))
            }
            
            projectDir.mkdirs()
            
            // Generate project structure
            val templateGenerator = TemplateGenerator(context)
            val generationResult = templateGenerator.generateFromTemplate(
                template = template,
                projectDir = projectDir,
                projectName = projectName,
                customization = customizationOptions
            )
            
            if (generationResult.isSuccess) {
                // Create project metadata
                val project = Project(
                    name = projectName,
                    path = projectDir.absolutePath,
                    type = templateId,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    files = getProjectFiles(projectDir)
                )
                
                Result.success(project)
            } else {
                Result.failure(Exception("Failed to generate project: ${generationResult.exceptionOrNull()?.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get template customization options
     */
    fun getTemplateCustomizationOptions(templateId: String): TemplateCustomizationOptions? {
        return templates[templateId]?.getCustomizationOptions()
    }
    
    /**
     * Validate project name
     */
    fun validateProjectName(projectName: String): ValidationResult {
        if (projectName.isBlank()) {
            return ValidationResult(false, "Project name cannot be empty")
        }
        
        if (!projectName.matches(Regex("[a-zA-Z0-9_-]+"))) {
            return ValidationResult(false, "Project name can only contain letters, numbers, underscores, and hyphens")
        }
        
        if (projectName.length < 2) {
            return ValidationResult(false, "Project name must be at least 2 characters long")
        }
        
        if (projectName.length > 50) {
            return ValidationResult(false, "Project name cannot exceed 50 characters")
        }
        
        return ValidationResult(true)
    }
    
    private fun getProjectFiles(projectDir: File): List<String> {
        return projectDir.listFiles()?.flatMap { file ->
            if (file.isDirectory) {
                getProjectFiles(file)
            } else {
                listOf(file.relativeTo(projectDir).path)
            }
        } ?: emptyList()
    }
}

/**
 * Data class representing a project template
 */
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val icon: String,
    val version: String,
    val author: String,
    val tags: List<String>
)

/**
 * Template customization options
 */
data class TemplateCustomizationOptions(
    val enableDatabase: Boolean = false,
    val enableAuthentication: Boolean = false,
    val enableApi: Boolean = false,
    val enableTests: Boolean = true,
    val enableDocumentation: Boolean = false,
    val databaseType: String = "sqlite",
    val additionalDependencies: List<String> = emptyList()
)

/**
 * Template customization with actual values
 */
data class TemplateCustomization(
    val options: TemplateCustomizationOptions,
    val customVariables: Map<String, String> = emptyMap()
)

/**
 * Validation result for project creation
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String = ""
)