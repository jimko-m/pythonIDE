package com.pythonide.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.accompanist.permissions.*

/**
 * Manager for handling runtime permissions
 */
object PermissionManager {
    
    private val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    private val MICROPHONE_PERMISSION = arrayOf(Manifest.permission.RECORD_AUDIO)
    
    /**
     * Check and request required permissions
     */
    fun checkAndRequestPermissions(
        context: Context,
        callback: (allGranted: Boolean, deniedPermissions: Array<String>) -> Unit
    ) {
        val deniedPermissions = mutableListOf<String>()
        
        // Check storage permissions
        STORAGE_PERMISSIONS.forEach { permission ->
            if (!isPermissionGranted(context, permission)) {
                deniedPermissions.add(permission)
            }
        }
        
        // Check for MANAGE_EXTERNAL_STORAGE permission (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!hasManageExternalStoragePermission(context)) {
                deniedPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }
        
        // Check camera permission
        if (!isPermissionGranted(context, Manifest.permission.CAMERA)) {
            deniedPermissions.add(Manifest.permission.CAMERA)
        }
        
        // Check microphone permission
        if (!isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) {
            deniedPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (deniedPermissions.isEmpty()) {
            callback(true, emptyArray())
        } else {
            callback(false, deniedPermissions.toTypedArray())
        }
    }
    
    /**
     * Request permissions
     */
    fun requestPermissions(context: Context, permissions: Array<String>) {
        val activity = context as? Activity
        activity?.requestPermissions(permissions, PERMISSION_REQUEST_CODE)
    }
    
    /**
     * Handle permission result
     */
    fun handlePermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        return false
    }
    
    /**
     * Check if permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check for MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     */
    fun hasManageExternalStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not needed for older versions
        }
    }
    
    /**
     * Request MANAGE_EXTERNAL_STORAGE permission
     */
    fun requestManageExternalStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
            }
        }
    }
    
    /**
     * Open app settings
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
    
    /**
     * Check if all storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all { isPermissionGranted(context, it) }
    }
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }
    
    /**
     * Check if microphone permission is granted
     */
    fun hasMicrophonePermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }
    
    /**
     * Should show permission rationale
     */
    fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}