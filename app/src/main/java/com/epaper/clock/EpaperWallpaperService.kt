package com.epaper.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    inner class EpaperEngine : Engine() {
        private val renderer = EpaperRenderer()
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) {
                    // Cập nhật lại mỗi 1000ms (1 giây) để kim đồng hồ chạy mượt
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
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.isVisible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                // Tắt hoàn toàn vòng lặp vẽ khi tắt màn hình để tiết kiệm 100% pin
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
