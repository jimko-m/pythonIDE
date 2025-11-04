package com.pythonide.templates

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

/**
 * Handles the generation of project files from templates.
 * Processes templates and creates the actual project structure and files.
 */
class TemplateGenerator(private val context: Context) {
    
    companion object {
        private const val TAG = "TemplateGenerator"
        private const val TEMPLATE_VAR_START = "{{"
        private const val TEMPLATE_VAR_END = "}}"
    }
    
    /**
     * Generate a project from a template
     */
    suspend fun generateFromTemplate(
        template: ProjectTemplate,
        projectDir: File,
        projectName: String,
        customization: TemplateCustomization?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating project from template: ${template.id}")
            
            // Create base project structure
            template.createProjectStructure(projectDir)
            
            // Process template files
            template.getTemplateFiles().forEach { templateFile ->
                val targetFile = File(projectDir, templateFile.targetPath)
                targetFile.parentFile?.mkdirs()
                
                // Process file content with variables
                val content = processTemplateContent(
                    templateFile.content,
                    projectName,
                    template,
                    customization
                )
                
                // Write file
                FileWriter(targetFile).use { writer ->
                    writer.write(content)
                }
            }
            
            Log.d(TAG, "Project generation completed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate project", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process template content by replacing variables
     */
    private fun processTemplateContent(
        content: String,
        projectName: String,
        template: ProjectTemplate,
        customization: TemplateCustomization?
    ): String {
        var processedContent = content
        
        // Basic project variables
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "PROJECT_NAME" + TEMPLATE_VAR_END, projectName)
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "PROJECT_NAME_LOWER" + TEMPLATE_VAR_END, projectName.lowercase())
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "PROJECT_NAME_UPPER" + TEMPLATE_VAR_END, projectName.uppercase())
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "TEMPLATE_ID" + TEMPLATE_VAR_END, template.id)
        
        // Template metadata
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "TEMPLATE_NAME" + TEMPLATE_VAR_END, template.name)
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "TEMPLATE_VERSION" + TEMPLATE_VAR_END, template.version)
        processedContent = processedContent.replace(TEMPLATE_VAR_START + "AUTHOR" + TEMPLATE_VAR_END, template.author)
        
        // Customization variables
        customization?.let { custom ->
            custom.customVariables.forEach { (key, value) ->
                processedContent = processedContent.replace(
                    TEMPLATE_VAR_START + key + TEMPLATE_VAR_END, 
                    value
                )
            }
            
            // Add conditional sections based on customization
            if (custom.options.enableDatabase) {
                processedContent = enableSection(processedContent, "DATABASE")
            }
            if (custom.options.enableAuthentication) {
                processedContent = enableSection(processedContent, "AUTH")
            }
            if (custom.options.enableApi) {
                processedContent = enableSection(processedContent, "API")
            }
            if (custom.options.enableTests) {
                processedContent = enableSection(processedContent, "TESTS")
            }
            if (custom.options.enableDocumentation) {
                processedContent = enableSection(processedContent, "DOCS")
            }
        }
        
        return processedContent
    }
    
    /**
     * Enable sections marked with conditional comments
     */
    private fun enableSection(content: String, section: String): String {
        // Remove comments that disable sections
        val startMarker = "<!-- $section:DISABLED -->"
        val endMarker = "<!-- $section:END -->"
        
        var processedContent = content
        
        // Remove disabled section comments and enable the content
        processedContent = processedContent.replace(
            Regex("$startMarker[\\s\\S]*?$endMarker"),
            ""
        )
        
        return processedContent
    }
    
    /**
     * Create additional configuration files based on customization
     */
    private fun createAdditionalFiles(
        projectDir: File,
        template: ProjectTemplate,
        customization: TemplateCustomization?
    ) {
        customization?.let { custom ->
            val configDir = File(projectDir, ".pythonide")
            configDir.mkdirs()
            
            // Create project configuration
            val configFile = File(configDir, "project.json")
            val config = buildString {
                appendLine("{")
                appendLine("  \"templateId\": \"${template.id}\",")
                appendLine("  \"templateVersion\": \"${template.version}\",")
                appendLine("  \"customization\": {")
                appendLine("    \"enableDatabase\": ${custom.options.enableDatabase},")
                appendLine("    \"enableAuthentication\": ${custom.options.enableAuthentication},")
                appendLine("    \"enableApi\": ${custom.options.enableApi},")
                appendLine("    \"enableTests\": ${custom.options.enableTests},")
                appendLine("    \"enableDocumentation\": ${custom.options.enableDocumentation},")
                appendLine("    \"databaseType\": \"${custom.options.databaseType}\",")
                appendLine("    \"additionalDependencies\": ${custom.options.additionalDependencies}")
                appendLine("  },")
                appendLine("  \"createdAt\": ${System.currentTimeMillis()}")
                appendLine("}")
            }
            
            FileWriter(configFile).use { it.write(config) }
        }
    }
}

/**
 * Data class representing a file in a template
 */
data class TemplateFile(
    val targetPath: String,
    val content: String,
    val isExecutable: Boolean = false
)

/**
 * Base interface for project templates
 */
interface ProjectTemplate {
    fun getTemplateInfo(): ProjectTemplate
    fun getTemplateFiles(): List<TemplateFile>
    fun createProjectStructure(projectDir: File)
    fun getCustomizationOptions(): TemplateCustomizationOptions
}