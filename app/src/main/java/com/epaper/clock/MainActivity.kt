package com.epaper.clock

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("epaper_prefs", Context.MODE_PRIVATE)

        // Sự kiện đếm ngược
        val edtEventTitle = findViewById<EditText>(R.id.edtEventTitle)
        val edtEventDate = findViewById<EditText>(R.id.edtEventDate)

        // Câu nói
        val edtQuote = findViewById<EditText>(R.id.edtQuote)
        val btnQ1 = findViewById<Button>(R.id.btnQuote1)
        val btnQ2 = findViewById<Button>(R.id.btnQuote2)
        val btnQ3 = findViewById<Button>(R.id.btnQuote3)
        val btnQ4 = findViewById<Button>(R.id.btnQuote4)

        // Kế hoạch & Mục tiêu + Checkboxes
        val cbShowToday = findViewById<CheckBox>(R.id.cbShowToday)
        val edtToday = findViewById<EditText>(R.id.edtTodayGoal)
        val cbShowWeek = findViewById<CheckBox>(R.id.cbShowWeek)
        val edtWeek = findViewById<EditText>(R.id.edtWeekGoal)
        val cbShowMonth = findViewById<CheckBox>(R.id.cbShowMonth)
        val edtMonth = findViewById<EditText>(R.id.edtMonthGoal)
        val cbShowYear = findViewById<CheckBox>(R.id.cbShowYear)
        val edtYear = findViewById<EditText>(R.id.edtYearGoal)

        val btnSave = findViewById<Button>(R.id.btnSaveGoals)
        val btnSet = findViewById<Button>(R.id.btnSetWallpaper)

        btnQ1.setOnClickListener { edtQuote.setText("Làm Chủ Bản Thân") }
        btnQ2.setOnClickListener { edtQuote.setText("Kỷ luật là tự do") }
        btnQ3.setOnClickListener { edtQuote.setText("Hành trình vạn dặm khởi từ một bước") }
        btnQ4.setOnClickListener { edtQuote.setText("Mỗi ngày tốt hơn hôm qua 1%") }

        // Tải dữ liệu đã lưu
        edtEventTitle.setText(prefs.getString("event_title", "Tết Dương Lịch"))
        edtEventDate.setText(prefs.getString("event_date", "01/01/2027"))

        edtQuote.setText(prefs.getString("daily_quote", "Làm Chủ Bản Thân"))

        cbShowToday.isChecked = prefs.getBoolean("show_today", true)
        edtToday.setText(prefs.getString("today_goal", "Fix xong tool cookie"))

        cbShowWeek.isChecked = prefs.getBoolean("show_week", true)
        edtWeek.setText(prefs.getString("week_goal", "Hoàn thành mục tiêu tuần"))

        cbShowMonth.isChecked = prefs.getBoolean("show_month", false)
        edtMonth.setText(prefs.getString("month_goal", ""))

        cbShowYear.isChecked = prefs.getBoolean("show_year", false)
        edtYear.setText(prefs.getString("year_goal", ""))

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("event_title", edtEventTitle.text.toString().trim())
                putString("event_date", edtEventDate.text.toString().trim())

                putString("daily_quote", edtQuote.text.toString().trim())

                putBoolean("show_today", cbShowToday.isChecked)
                putString("today_goal", edtToday.text.toString().trim())

                putBoolean("show_week", cbShowWeek.isChecked)
                putString("week_goal", edtWeek.text.toString().trim())

                putBoolean("show_month", cbShowMonth.isChecked)
                putString("month_goal", edtMonth.text.toString().trim())

                putBoolean("show_year", cbShowYear.isChecked)
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
