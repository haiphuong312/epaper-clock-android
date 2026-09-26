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

        val edtQuote = findViewById<EditText>(R.id.edtQuote)
        val edtToday = findViewById<EditText>(R.id.edtTodayGoal)
        val edtMonth = findViewById<EditText>(R.id.edtMonthGoal)
        val edtYear = findViewById<EditText>(R.id.edtYearGoal)
        val btnSave = findViewById<Button>(R.id.btnSaveGoals)
        val btnSet = findViewById<Button>(R.id.btnSetWallpaper)

        // Các nút chọn nhanh câu nói
        val btnQ1 = findViewById<Button>(R.id.btnQuote1)
        val btnQ2 = findViewById<Button>(R.id.btnQuote2)
        val btnQ3 = findViewById<Button>(R.id.btnQuote3)
        val btnQ4 = findViewById<Button>(R.id.btnQuote4)

        btnQ1.setOnClickListener { edtQuote.setText("Kỷ luật là chiếc cầu nối giữa mục tiêu và thành tựu.") }
        btnQ2.setOnClickListener { edtQuote.setText("Hành trình vạn dặm luôn bắt đầu từ một bước chân.") }
        btnQ3.setOnClickListener { edtQuote.setText("Mỗi ngày nỗ lực tốt hơn hôm qua 1% là đủ vĩ đại.") }
        btnQ4.setOnClickListener { edtQuote.setText("Tập trung trọn vẹn vào hiện tại để kiến tạo tương lai.") }

        // Tải dữ liệu đã lưu
        edtQuote.setText(prefs.getString("daily_quote", "Hành trình vạn dặm bắt đầu từ một bước chân."))
        edtToday.setText(prefs.getString("today_goal", "Tập thể dục 30p • Đọc 20 trang sách"))
        edtMonth.setText(prefs.getString("month_goal", "Tiết kiệm chi tiêu • Hoàn thành dự án"))
        edtYear.setText(prefs.getString("year_goal", "Chạy bộ 500km • Học kỹ năng mới"))

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("daily_quote", edtQuote.text.toString().trim())
                putString("today_goal", edtToday.text.toString().trim())
                putString("month_goal", edtMonth.text.toString().trim())
                putString("year_goal", edtYear.text.toString().trim())
                apply()
            }
            Toast.makeText(this, "Đã lưu thành công! Màn hình khóa đang cập nhật...", Toast.LENGTH_SHORT).show()
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
