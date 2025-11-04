package com.pythonide.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

/**
 * Terminal session model
 */
@Entity(tableName = "terminal_sessions")
data class TerminalSession(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "working_directory")
    val workingDirectory: String,
    
    @ColumnInfo(name = "shell_path")
    val shellPath: String = "/data/data/com.termux/files/usr/bin/bash",
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    
    @ColumnInfo(name = "created_date")
    val createdDate: Date,
    
    @ColumnInfo(name = "last_activity")
    val lastActivity: Date,
    
    @ColumnInfo(name = "environment_variables")
    val environmentVariables: String?, // JSON
    
    @ColumnInfo(name = "terminal_type")
    val terminalType: String = "xterm-256color",
    
    @ColumnInfo(name = "columns")
    val columns: Int = 80,
    
    @ColumnInfo(name = "rows")
    val rows: Int = 24,
    
    @ColumnInfo(name = "title")
    val title: String?,
    
    @ColumnInfo(name = "color_scheme")
    val colorScheme: String = "dark",
    
    @ColumnInfo(name = "font_size")
    val fontSize: Int = 12,
    
    @ColumnInfo(name = "history_size")
    val historySize: Int = 1000,
    
    @ColumnInfo(name = "is_termux_installed")
    val isTermuxInstalled: Boolean = false
)

/**
 * Terminal command model
 */
@Entity(tableName = "terminal_commands")
data class TerminalCommand(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "command")
    val command: String,
    
    @ColumnInfo(name = "output")
    val output: String?,
    
    @ColumnInfo(name = "exit_code")
    val exitCode: Int? = null,
    
    @ColumnInfo(name = "start_time")
    val startTime: Date,
    
    @ColumnInfo(name = "end_time")
    val endTime: Date?,
    
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,
    
    @ColumnInfo(name = "is_running")
    val isRunning: Boolean,
    
    @ColumnInfo(name = "process_id")
    val processId: Int? = null,
    
    @ColumnInfo(name = "working_directory")
    val workingDirectory: String,
    
    @ColumnInfo(name = "environment")
    val environment: String?, // JSON
    
    @ColumnInfo(name = "error_output")
    val errorOutput: String? = null,
    
    @ColumnInfo(name = "is_background")
    val isBackground: Boolean = false
)

/**
 * Running process model
 */
data class RunningProcess(
    val processId: Int,
    val command: String,
    val arguments: List<String>,
    val workingDirectory: String,
    val startTime: Date,
    val cpuUsage: Float,
    val memoryUsage: Long,
    val user: String,
    val status: ProcessStatus,
    val parentProcessId: Int? = null,
    val sessionId: String? = null
) {
    enum class ProcessStatus(val displayName: String) {
        RUNNING("Running"),
        SLEEPING("Sleeping"),
        STOPPED("Stopped"),
        ZOMBIE("Zombie"),
        UNKNOWN("Unknown");
        
        companion object {
            fun fromString(status: String): ProcessStatus {
                return values().find { it.name.equals(status, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }
}

/**
 * Termux package model
 */
data class TermuxPackage(
    val name: String,
    val version: String,
    val description: String,
    val installed: Boolean,
    val outdated: Boolean,
    val size: Long,
    val category: String,
    val dependencies: List<String>,
    val repository: String
)

/**
 * Python environment model
 */
data class PythonEnvironment(
    val name: String,
    val path: String,
    val pythonVersion: String,
    val pipVersion: String,
    val packages: List<PythonPackage>,
    val isActive: Boolean,
    val createdDate: Date,
    val lastUsed: Date
)

/**
 * Python package model
 */
data class PythonPackage(
    val name: String,
    val version: String,
    val latestVersion: String?,
    val description: String,
    val installedDate: Date,
    val license: String?,
    val author: String?,
    val homepage: String?,
    val dependencies: List<String>
)

/**
 * Terminal history entry
 */
data class TerminalHistoryEntry(
    val command: String,
    val timestamp: Date,
    val workingDirectory: String,
    val exitCode: Int,
    val duration: Long
)

/**
 * Terminal settings model
 */
data class TerminalSettings(
    val fontSize: Int = 12,
    val fontFamily: String = "Monaco, Consolas, 'Ubuntu Mono', monospace",
    val colorScheme: String = "dark",
    val backgroundColor: String = "#000000",
    val foregroundColor: String = "#FFFFFF",
    val cursorColor: String = "#FFFFFF",
    val selectionColor: String = "#264F78",
    val theme: TerminalTheme = TerminalTheme.DARK,
    val tabSize: Int = 4,
    val bellEnabled: Boolean = true,
    val scrollback: Int = 10000,
    val wordSeparator: String = " ()[]{}'\".,;:!@#$%^&*+=|\\/",
    val confirmExit: Boolean = true,
    val autoSaveHistory: Boolean = true,
    val enableBoldFont: Boolean = true,
    val enableItalicFont: Boolean = true,
    val fastScroll: Boolean = true,
    val alternateScroll: Boolean = true,
    val saveLines: Int = 1000,
    val historyFileEnabled: Boolean = true
) {
    enum class TerminalTheme(val displayName: String) {
        DARK("Dark"),
        LIGHT("Light"),
        MONOKAI("Monokai"),
        SOLARIZED_DARK("Solarized Dark"),
        SOLARIZED_LIGHT("Solarized Light"),
        GRUVBOX("Gruvbox"),
        NORD("Nord")
    }
}

/**
 * Terminal output model
 */
data class TerminalOutput(
    val text: String,
    val timestamp: Date,
    val type: OutputType,
    val foregroundColor: Int? = null,
    val backgroundColor: Int? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val blink: Boolean = false
) {
    enum class OutputType(val colorCode: Int) {
        NORMAL(R.color.terminal_foreground),
        COMMAND(R.color.terminal_blue),
        OUTPUT(R.color.terminal_green),
        ERROR(R.color.terminal_red),
        WARNING(R.color.terminal_yellow),
        INFO(R.color.terminal_cyan),
        PROMPT(R.color.terminal_white),
        COMMENT(R.color.terminal_magenta);
        
        companion object {
            fun fromExitCode(exitCode: Int?): OutputType {
                return when (exitCode) {
                    0 -> OUTPUT
                    null, -1 -> COMMAND
                    else -> ERROR
                }
            }
        }
    }
}