package com.epaper.clock

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("epaper_prefs", Context.MODE_PRIVATE)

        val edtToday = findViewById<EditText>(R.id.edtTodayGoal)
        val edtMonth = findViewById<EditText>(R.id.edtMonthGoal)
        val edtYear = findViewById<EditText>(R.id.edtYearGoal)
        val btnSave = findViewById<Button>(R.id.btnSaveGoals)
        val btnSet = findViewById<Button>(R.id.btnSetWallpaper)

        // Tải mục tiêu đã lưu trước đó (hoặc gợi ý mặc định)
        edtToday.setText(prefs.getString("today_goal", "Tập thể dục 30p • Đọc 20 trang sách"))
        edtMonth.setText(prefs.getString("month_goal", "Tiết kiệm chi tiêu • Hoàn thành dự án"))
        edtYear.setText(prefs.getString("year_goal", "Chạy bộ 500km • Học kỹ năng mới"))

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("today_goal", edtToday.text.toString().trim())
                putString("month_goal", edtMonth.text.toString().trim())
                putString("year_goal", edtYear.text.toString().trim())
                apply()
            }
            Toast.makeText(this, "Đã lưu mục tiêu thành công! Màn hình khóa sẽ tự cập nhật.", Toast.LENGTH_SHORT).show()
        }

        btnSet.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, EpaperWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }
    }
}
