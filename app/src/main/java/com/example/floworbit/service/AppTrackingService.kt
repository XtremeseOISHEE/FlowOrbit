package com.example.floworbit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.usage.UsageStatsManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.floworbit.data.blockedapps.BlockedAppsRepository
import com.example.floworbit.data.violationlog.ViolationLog
import com.example.floworbit.data.violationlog.ViolationLogRepository
import com.example.floworbit.util.AllowanceManager
import kotlinx.coroutines.*

class AppTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "app_tracking_channel"
        private const val NOTIF_ID = 901
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var blockedRepo: BlockedAppsRepository
    private lateinit var logRepo: ViolationLogRepository

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        blockedRepo = BlockedAppsRepository(applicationContext)
        logRepo = ViolationLogRepository(applicationContext)

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Tracking apps..."))

        startPollingForAppChanges()
    }

    /**
     * Sends an intent to return to the Android Home Screen.
     */
    private fun closeAppAndGoHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
    }

    /**
     * This coroutine POLLS for changes in the foreground app, and also re-checks the current
     * app's allowance every tick -- so it both shows the overlay when a blocked app is switched
     * to, AND closes the app the moment its 5-minute allowance runs out, even without a switch.
     */
    private fun startPollingForAppChanges() {
        serviceScope.launch {
            var lastForegroundPackage: String? = null
            var wasAllowedLastTick = false
            while (isActive) {
                val currentForegroundPackage = queryForegroundPackage()

                if (currentForegroundPackage != null) {
                    val isFocusSessionRunning = FocusTimerService.isRunning.value
                    val isAppBlocked = blockedRepo.isAppBlocked(currentForegroundPackage)
                    val isAllowedNow = AllowanceManager.isAllowedNow(applicationContext, currentForegroundPackage)

                    if (currentForegroundPackage != lastForegroundPackage) {
                        // Switched into this app: warn if it's blocked and not currently allowed.
                        if (isFocusSessionRunning && isAppBlocked && !isAllowedNow) {
                            showBlockOverlayFor(currentForegroundPackage)
                        }
                    } else if (wasAllowedLastTick && !isAllowedNow && isFocusSessionRunning && isAppBlocked) {
                        // Same app, but its allowance just expired while still in the foreground.
                        Log.d("FLOW", "Closing '$currentForegroundPackage' -- its allowance just expired.")
                        closeAppAndGoHome()
                    }

                    wasAllowedLastTick = isAllowedNow
                    lastForegroundPackage = currentForegroundPackage
                }
                delay(1000) // Check every second
            }
        }
    }

    /**
     * Helper function to show the warning overlay.
     */
    private suspend fun showBlockOverlayFor(packageName: String) {
        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        logRepo.insert(ViolationLog(packageName = packageName, label = appLabel))

        val intent = Intent(this@AppTrackingService, WarningOverlayService::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("pkg", packageName)
            putExtra("label", appLabel)
        }
        startService(intent)
    }

    /**
     * Finds the app that was most recently in the foreground using UsageStatsManager.
     */
    private fun queryForegroundPackage(): String? {
        return try {
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 60 * 1000, time)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e("FLOW", "Could not query foreground package", e)
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "App tracking", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification = 
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlowOrbit — tracking")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}