package com.ff.headshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.ff.headshot.databinding.OverlayButtonBinding
import com.ff.headshot.databinding.OverlayMenuBinding

class FloatingMenuService : Service() {

    private lateinit var wm: WindowManager
    private var buttonView: View? = null
    private var menuView: View? = null
    private var menuVisible = false

    private lateinit var btnBinding: OverlayButtonBinding

    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "ffheadshot_channel"
        @Volatile var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isRunning = true
        startForegroundWithNotification()
        createFloatingButton()
    }

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID, "FF Headshot Panel", NotificationManager.IMPORTANCE_LOW
            ).also { it.setSound(null, null); it.enableLights(false); it.enableVibration(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }.setContentTitle("FF Headshot Panel")
            .setContentText("Panel active — tap FF button in game")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    private fun baseParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    // ── Floating trigger button ──────────────────────────────────────────────

    private fun createFloatingButton() {
        btnBinding = OverlayButtonBinding.inflate(LayoutInflater.from(this))
        buttonView = btnBinding.root

        val params = baseParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 320 }

        var initX = 0; var initY = 0; var rawX = 0f; var rawY = 0f; var moved = false

        buttonView!!.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    rawX = ev.rawX; rawY = ev.rawY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - rawX).toInt()
                    val dy = (ev.rawY - rawY).toInt()
                    if (dx * dx + dy * dy > 100) moved = true
                    params.x = initX + dx; params.y = initY + dy
                    wm.updateViewLayout(buttonView, params); true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) { if (menuVisible) hideMenu() else showMenu() }; false
                }
                else -> false
            }
        }

        wm.addView(buttonView, params)
    }

    // ── Mod Menu ─────────────────────────────────────────────────────────────

    private fun showMenu() {
        if (menuVisible) return
        menuVisible = true

        val mb = OverlayMenuBinding.inflate(LayoutInflater.from(this))
        menuView = mb.root

        val params = baseParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }

        // Headshot toggle
        mb.switchHeadshot.isChecked = HeadshotService.isHeadshotEnabled
        mb.tvHeadshotStatus.text = if (HeadshotService.isHeadshotEnabled) "ON" else "OFF"
        mb.tvHeadshotStatus.setTextColor(
            if (HeadshotService.isHeadshotEnabled) Color.parseColor("#00FF88") else Color.parseColor("#FF4444")
        )

        mb.switchHeadshot.setOnCheckedChangeListener { _, checked ->
            HeadshotService.isHeadshotEnabled = checked
            // Broadcast to accessibility service
            sendBroadcast(Intent(HeadshotService.ACTION_TOGGLE).apply {
                putExtra(HeadshotService.EXTRA_ENABLED, checked)
                setPackage(packageName)
            })
            mb.tvHeadshotStatus.text = if (checked) "ON" else "OFF"
            mb.tvHeadshotStatus.setTextColor(
                if (checked) Color.parseColor("#00FF88") else Color.parseColor("#FF4444")
            )
        }

        mb.btnCloseMenu.setOnClickListener { hideMenu() }

        wm.addView(menuView, params)
    }

    private fun hideMenu() {
        menuView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        menuView = null
        menuVisible = false
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        hideMenu()
        buttonView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        buttonView = null
        isRunning = false
    }
}
