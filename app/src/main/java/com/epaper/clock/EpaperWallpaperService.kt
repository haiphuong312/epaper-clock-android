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
                    // Cập nhật lại mỗi 1000ms (1 giây) để kim giây nhảy đều đặn
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
            loadGoalsFromPrefs()

            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
        }

        private fun loadGoalsFromPrefs() {
            renderer.todayGoal = prefs.getString("today_goal", "Tập thể dục 30p • Đọc 20 trang sách") ?: ""
            renderer.monthGoal = prefs.getString("month_goal", "Tiết kiệm chi tiêu • Hoàn thành dự án") ?: ""
            renderer.yearGoal = prefs.getString("year_goal", "Chạy bộ 500km • Học kỹ năng mới") ?: ""
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            loadGoalsFromPrefs()
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
                loadGoalsFromPrefs()
                handler.post(drawRunnable)
            } else {
                // Tắt hoàn toàn vòng lặp vẽ khi màn hình tắt để 0% tốn pin
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
