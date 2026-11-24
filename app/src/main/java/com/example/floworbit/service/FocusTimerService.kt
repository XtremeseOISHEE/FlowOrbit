package com.example.floworbit.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.floworbit.MainActivity
import com.example.floworbit.data.profile.UserProfileRepository
import com.example.floworbit.util.BoringModeManager
import com.example.floworbit.util.DNDManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusTimerService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START = "FOCUS_START"
        const val ACTION_PAUSE = "FOCUS_PAUSE"
        const val ACTION_STOP = "FOCUS_STOP"
        const val ACTION_BORING_MODE_ON = "BORING_MODE_ON"
        const val ACTION_BORING_MODE_OFF = "BORING_MODE_OFF"

        const val EXTRA_DURATION = "duration"

        private val _remaining = MutableStateFlow(0L)
        val remainingTime = _remaining.asStateFlow()

        private val _running = MutableStateFlow(false)
        val isRunning = _running.asStateFlow()

        private val _boringModeActive = MutableStateFlow(false)
        val boringModeActive = _boringModeActive.asStateFlow()
    }

    private var timerJob: Job? = null
    private lateinit var dndManager: DNDManager
    private lateinit var boringModeManager: BoringModeManager
    private val userProfileRepository = UserProfileRepository() // Create instance
    private val serviceScope = CoroutineScope(Dispatchers.IO) // Scope for database operations

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        dndManager = DNDManager(this)
        boringModeManager = BoringModeManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val isResuming = !_running.value && _remaining.value > 0
                if (isResuming) {
                    startTimer(_remaining.value)
                } else {
                    val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                    startTimer(duration)
                }
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
            ACTION_BORING_MODE_ON -> enableBoringModeManually()
            ACTION_BORING_MODE_OFF -> disableBoringModeManually()
        }

        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        timerJob?.cancel()

        _remaining.value = durationMillis
        _running.value = true

        startForegroundWith(buildNotification(formatTime(durationMillis)))

        if (dndManager.hasAccess()) {
            dndManager.enableDND()
        }
        boringModeManager.enable()
        _boringModeActive.value = boringModeManager.isActive()

        timerJob = serviceScope.launch {
            while (_running.value && _remaining.value > 0) {
                delay(1000)
                _remaining.value -= 1000

                updateNotification(formatTime(_remaining.value))

                if (_remaining.value <= 0) {
                    // Switch to main thread for finishing up
                    withContext(Dispatchers.Main) {
                        finishSession()
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        _running.value = false
        timerJob?.cancel()
        updateNotification("Paused: ${formatTime(_remaining.value)}")
    }

    private fun stopTimer() {
        _running.value = false
        timerJob?.cancel()
        _remaining.value = 0L

        if (dndManager.hasAccess()) {
            dndManager.disableDND()
        }
        endSessionBoringMode()

        stopForegroundIfIdle()
    }

    private fun finishSession() {
        // ⭐ This is the logic to update the user's profile
        serviceScope.launch {
            val userId = userProfileRepository.getCurrentUserId()
            if (userId != null) {
                userProfileRepository.incrementSessionCount(userId)
            }
        }

        _running.value = false
        timerJob?.cancel()

        updateNotification("Completed!")

        if (dndManager.hasAccess()) {
            dndManager.disableDND()
        }
        endSessionBoringMode()

        stopForegroundIfIdle()
    }

    /** A focus session ending shouldn't fight a user who separately pinned Boring Mode on by hand. */
    private fun endSessionBoringMode() {
        if (!boringModeManager.isManuallyPinned()) {
            boringModeManager.disable()
        }
        _boringModeActive.value = boringModeManager.isActive()
    }

    private fun enableBoringModeManually() {
        boringModeManager.setManualPin(true)
        startForegroundWith(buildBoringModeNotification())
        boringModeManager.enable()
        _boringModeActive.value = boringModeManager.isActive()
    }

    private fun disableBoringModeManually() {
        boringModeManager.setManualPin(false)
        boringModeManager.disable()
        _boringModeActive.value = false
        stopForegroundIfIdle()
    }

    private fun startForegroundWith(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Only tears the service down once nothing (timer or manual Boring Mode) still needs it alive. */
    private fun stopForegroundIfIdle() {
        if (!_running.value && !boringModeManager.isActive()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Timer")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildBoringModeNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Boring Mode active")
            .setContentText("Screen is grayscale · tap to open FlowOrbit")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Focus Timer", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Swiping the app away from Recents mid-session shouldn't leave the whole phone grayscale,
        // even if the user had manually pinned it on -- recoverability beats the preference here.
        boringModeManager.setManualPin(false)
        boringModeManager.disable()
        _boringModeActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel() // Ensure coroutines are cancelled when service is destroyed
        // Safety net: if the service dies unexpectedly (crash, low-memory kill) while Boring Mode
        // is on, revert it here too rather than leaving the device stuck in grayscale.
        boringModeManager.setManualPin(false)
        boringModeManager.disable()
        _boringModeActive.value = false
    }
}
