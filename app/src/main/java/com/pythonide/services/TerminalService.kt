package com.pythonide.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pythonide.R
import com.pythonide.ui.activities.MainActivity

/**
 * Terminal service for background command execution
 */
class TerminalService : Service() {
    
    companion object {
        private const val CHANNEL_ID = "terminal_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START_TERMINAL = "start_terminal"
        private const val ACTION_STOP_TERMINAL = "stop_terminal"
        
        @Volatile
        private var instance: TerminalService? = null
        
        fun initialize(context: Context) {
            // Terminal service initialization
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TERMINAL -> {
                startTerminal()
            }
            ACTION_STOP_TERMINAL -> {
                stopTerminal()
            }
        }
        
        return START_STICKY
    }
    
    private fun startTerminal() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.terminal_title))
            .setContentText(getString(R.string.terminal_running))
            .setSmallIcon(R.drawable.ic_terminal)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun stopTerminal() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}