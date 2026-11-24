package com.example.floworbit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.floworbit.util.AllowanceManager
import com.example.floworbit.util.OverlayPermissionHelper

class WarningOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "warning_overlay_channel"
        private const val NOTIF_ID = 222
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlowOrbit")
            .setContentText("Showing focus overlay")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pkg = intent?.getStringExtra("pkg") ?: ""
        val label = intent?.getStringExtra("label") ?: pkg

        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            startActivity(
                OverlayPermissionHelper.requestOverlayIntent(packageName).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(pkg, label)
        return START_NOT_STICKY
    }

    private fun showOverlay(packageName: String, label: String) {
        val canGrant = AllowanceManager.canGrantAllowance(applicationContext, packageName)

        val tv = TextView(this).apply {
            text = "You opened '$label'.\nStay focused on your task!"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 40, 40, 40)
        }

        val btnGoHome = Button(this).apply { text = "Go Home (Close App)" }

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            if (canGrant) {
                val btnContinue = Button(this@WarningOverlayService).apply { text = "Allow 5 min" }
                btnContinue.setOnClickListener {
                    AllowanceManager.grantAllowance(applicationContext, packageName)
                    removeOverlay()
                }
                addView(btnContinue, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 32 })
            } else {
                val remainingMin = (AllowanceManager.cooldownRemainingMillis(applicationContext, packageName) / 60000L) + 1
                val cooldownNote = TextView(this@WarningOverlayService).apply {
                    text = "5-minute pass already used -- available again in $remainingMin min."
                    setTextColor(Color.parseColor("#DCAAA8"))
                    textSize = 15f
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(0, 32, 0, 0)
                }
                addView(cooldownNote)
            }

            addView(btnGoHome, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 })
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#E6000000"))
            addView(tv)
            addView(buttonLayout)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = container
        windowManager?.addView(container, params)

        btnGoHome.setOnClickListener {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(home)
            AllowanceManager.clearAllowance(applicationContext, packageName) // Stop timer if user closes early; cooldown stays
            removeOverlay()
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
