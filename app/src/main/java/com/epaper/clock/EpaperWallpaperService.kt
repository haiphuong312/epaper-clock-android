package com.epaper.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class EpaperWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return EpaperEngine()
    }

    inner class EpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val renderer = EpaperRenderer()
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private lateinit var prefs: SharedPreferences

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) {
                    handler.postDelayed(this, 1000)
                }
            }
        }

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    if (voltage > 0) {
                        renderer.batteryVoltage = voltage / 1000f
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs = getSharedPreferences("epaper_prefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            loadDataFromPrefs()

            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
        }

        private fun loadDataFromPrefs() {
            renderer.eventTitle = prefs.getString("event_title", "Tết Dương Lịch") ?: "Tết Dương Lịch"
            renderer.eventDateStr = prefs.getString("event_date", "01/01/2027") ?: "01/01/2027"

            renderer.dailyQuote = prefs.getString("daily_quote", "Làm Chủ Bản Thân") ?: "Làm Chủ Bản Thân"
            renderer.quoteNote = prefs.getString("quote_note", "Kiểm soát cảm xúc, rèn luyện tư duy và hành động kỷ luật mỗi ngày để vươn tới tự do đích thực.") ?: ""

            renderer.showToday = prefs.getBoolean("show_today", true)
            renderer.todayGoal = prefs.getString("today_goal", "Fix xong tool cookie") ?: ""

            renderer.showWeek = prefs.getBoolean("show_week", true)
            renderer.weekGoal = prefs.getString("week_goal", "Hoàn thành mục tiêu tuần") ?: ""

            renderer.showMonth = prefs.getBoolean("show_month", false)
            renderer.monthGoal = prefs.getString("month_goal", "") ?: ""

            renderer.showYear = prefs.getBoolean("show_year", false)
            renderer.yearGoal = prefs.getString("year_goal", "") ?: ""
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            loadDataFromPrefs()
            drawFrame()
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            try {
                prefs.unregisterOnSharedPreferenceChangeListener(this)
            } catch (_: Exception) {}
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.isVisible = visible
            if (visible) {
                loadDataFromPrefs()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderer.render(canvas, canvas.width, canvas.height)
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
