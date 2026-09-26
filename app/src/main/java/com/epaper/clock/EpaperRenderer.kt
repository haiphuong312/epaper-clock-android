package com.epaper.clock

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class EpaperRenderer {

    // Màu mực E-Paper
    private val bgBezelColor = Color.parseColor("#1A1A1A")     // Nền màn hình ngoài đen AMOLED
    private val cardBgColor = Color.parseColor("#EAEAE6")      // Nền giấy E-Ink xám nhạt
    private val inkColor = Color.parseColor("#121212")         // Mực đen
    private val inkMutedColor = Color.parseColor("#7A7A78")    // Mực xám nhạt cho vạch/tiêu đề
    private val borderStrokeColor = Color.parseColor("#252525")// Viền khung

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBgColor
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val textRegular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val textMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkMutedColor
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    var batteryVoltage: Float = 3.85f
    var temperature: Int = 31

    fun render(canvas: Canvas, width: Int, height: Int) {
        // 1. Phủ đen toàn bộ màn hình (Tiết kiệm pin AMOLED của Samsung)
        canvas.drawColor(bgBezelColor)

        // 2. Tính toán kích thước khung thẻ E-paper trung tâm (Tỉ lệ ~ 4:3)
        val cardMarginHorizontal = width * 0.04f
        val cardWidth = width - (cardMarginHorizontal * 2)
        val cardHeight = cardWidth * 0.78f
        val cardLeft = cardMarginHorizontal
        val cardTop = (height - cardHeight) * 0.38f // Đặt cân đối ở nửa trên/giữa màn hình
        val cardRight = cardLeft + cardWidth
        val cardBottom = cardTop + cardHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cornerRadius = 12f

        // Vẽ nền thẻ giấy E-Ink & Viền kép
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        val innerPadding = cardWidth * 0.02f
        val innerRect = RectF(
            cardLeft + innerPadding,
            cardTop + innerPadding,
            cardRight - innerPadding,
            cardBottom - innerPadding
        )
        canvas.drawRect(innerRect, linePaint)

        // Phân chia layout:
        // Header cao 14%
        // Body cao 64%
        // Footer cao 22%
        val headerHeight = cardHeight * 0.14f
        val footerHeight = cardHeight * 0.22f
        val bodyHeight = cardHeight - headerHeight - footerHeight

        val headerBottom = innerRect.top + headerHeight
        val footerTop = innerRect.bottom - footerHeight

        // Kẻ 2 đường phân cách ngang
        canvas.drawLine(innerRect.left, headerBottom, innerRect.right, headerBottom, linePaint)
        canvas.drawLine(innerRect.left, footerTop, innerRect.right, footerTop, linePaint)

        val now = Calendar.getInstance()

        // 3. VẼ PHẦN HEADER
        renderHeader(canvas, innerRect.left, innerRect.top, innerRect.right, headerBottom, now)

        // 4. VẼ PHẦN BODY (Đồng hồ kim bên trái, Lịch tháng bên phải)
        val splitX = innerRect.left + (innerRect.width() * 0.44f)
        renderAnalogClock(canvas, innerRect.left, headerBottom, splitX, footerTop, now)
        renderCalendar(canvas, splitX, headerBottom, innerRect.right, footerTop, now)

        // 5. VẼ PHẦN FOOTER (3 Cột)
        renderFooter(canvas, innerRect.left, footerTop, innerRect.right, innerRect.bottom, now)
    }

    private fun renderHeader(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val centerY = (top + bottom) / 2f
        val textSize = (bottom - top) * 0.45f
        inkPaint.textSize = textSize

        // Ngày dd/MM/yyyy
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
        canvas.drawText(dateStr, left + 20f, centerY + (textSize * 0.35f), inkPaint)

        // Buổi: Sáng / Trưa / Chiều / Tối
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val periodStr = when (hour) {
            in 4..10 -> "Sáng"
            in 11..13 -> "Trưa"
            in 14..17 -> "Chiều"
            else -> "Tối"
        }
        textRegular.textSize = textSize * 0.85f
        val periodX = left + ((right - left) * 0.42f)
        canvas.drawText(periodStr, periodX, centerY + (textSize * 0.3f), textRegular)

        // Thứ
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dowStr = when (dayOfWeek) {
            Calendar.MONDAY -> "Thứ hai"
            Calendar.TUESDAY -> "Thứ ba"
            Calendar.WEDNESDAY -> "Thứ tư"
            Calendar.THURSDAY -> "Thứ năm"
            Calendar.FRIDAY -> "Thứ sáu"
            Calendar.SATURDAY -> "Thứ bảy"
            else -> "Chủ nhật"
        }
        val dowX = left + ((right - left) * 0.58f)
        canvas.drawText(dowStr, dowX, centerY + (textSize * 0.3f), textRegular)

        // Điện áp & icon pin bên góc phải
        val voltStr = String.format(Locale.US, "%.1fv", batteryVoltage)
        textRegular.textSize = textSize * 0.65f
        val voltWidth = textRegular.measureText(voltStr)
        val voltX = right - voltWidth - 55f
        canvas.drawText(voltStr, voltX, centerY + (textSize * 0.25f), textRegular)

        // Vẽ biểu tượng pin nhỏ
        val batX = right - 45f
        val batY = centerY - 10f
        val batPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(batX, batY, batX + 28f, batY + 18f, batPaint)
        canvas.drawRect(batX + 28f, batY + 4f, batX + 32f, batY + 14f, batPaint) // Cực dương pin
        // Mức pin bên trong
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(batX + 3f, batY + 3f, batX + 20f, batY + 15f, fillPaint)
    }

    private fun renderAnalogClock(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val radius = ((right - left).coerceAtMost(bottom - top) / 2f) * 0.85f

        // 1. Vẽ các vạch chia phút (60 vạch) và vạch giờ (12 vạch)
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6).toDouble())
            val isHour = i % 5 == 0
            val tickLength = if (isHour) radius * 0.12f else radius * 0.05f
            tickPaint.strokeWidth = if (isHour) 3.5f else 1.5f

            val startX = (cx + (radius - tickLength) * sin(angle)).toFloat()
            val startY = (cy - (radius - tickLength) * cos(angle)).toFloat()
            val stopX = (cx + radius * sin(angle)).toFloat()
            val stopY = (cy - radius * cos(angle)).toFloat()

            canvas.drawLine(startX, startY, stopX, stopY, tickPaint)
        }

        // 2. Vẽ số 1 đến 12
        inkPaint.textSize = radius * 0.22f
        inkPaint.textAlign = Paint.Align.CENTER
        val numberRadius = radius * 0.72f

        for (i in 1..12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val numX = (cx + numberRadius * sin(angle)).toFloat()
            val numY = (cy - numberRadius * cos(angle)).toFloat() + (inkPaint.textSize * 0.35f)
            canvas.drawText(i.toString(), numX, numY, inkPaint)
        }

        // 3. Kim giờ & Kim phút
        val hours = cal.get(Calendar.HOUR)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)

        val hourAngle = Math.toRadians(((hours * 30) + (minutes * 0.5)).toDouble())
        val minuteAngle = Math.toRadians((minutes * 6).toDouble())

        // Kim giờ (ngắn và dày)
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        val hourLen = radius * 0.5f
        canvas.drawLine(
            cx, cy,
            (cx + hourLen * sin(hourAngle)).toFloat(),
            (cy - hourLen * cos(hourAngle)).toFloat(),
            hourPaint
        )

        // Kim phút (dài và thanh)
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        val minLen = radius * 0.78f
        canvas.drawLine(
            cx, cy,
            (cx + minLen * sin(minuteAngle)).toFloat(),
            (cy - minLen * cos(minuteAngle)).toFloat(),
            minPaint
        )

        // Chốt trục kim ở tâm
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 7f, dotPaint)
    }

    private fun renderCalendar(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val calWidth = right - left
        val calHeight = bottom - top
        val colWidth = calWidth / 7f

        // Hàng tiêu đề thứ: CN  T2  T3  T4  T5  T6  T7
        val headers = arrayOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
        val headerY = top + (calHeight * 0.16f)
        textMuted.textSize = colWidth * 0.42f
        textMuted.textAlign = Paint.Align.CENTER

        for (i in 0 until 7) {
            val hx = left + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(headers[i], hx, headerY, textMuted)
        }

        // Tính toán các ngày trong tháng hiện tại
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0: CN, 1: T2...
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val rowHeight = (calHeight - (headerY - top) - 20f) / 6f
        var currentXIndex = firstDayOfWeek
        var currentYIndex = 1

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = colWidth * 0.48f
            textAlign = Paint.Align.CENTER
        }

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.FILL
        }

        val currentDayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBgColor // Chữ màu trắng/nền khi ở trong vòng tròn đen
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = colWidth * 0.48f
            textAlign = Paint.Align.CENTER
        }

        for (day in 1..maxDays) {
            val dx = left + (currentXIndex * colWidth) + (colWidth / 2f)
            val dy = headerY + (currentYIndex * rowHeight)

            if (day == currentDay) {
                // Khoanh tròn đen ngày hôm nay y như ảnh mẫu
                canvas.drawCircle(dx, dy - (dayPaint.textSize * 0.32f), colWidth * 0.42f, circlePaint)
                canvas.drawText(day.toString(), dx, dy, currentDayTextPaint)
            } else {
                canvas.drawText(day.toString(), dx, dy, dayPaint)
            }

            currentXIndex++
            if (currentXIndex > 6) {
                currentXIndex = 0
                currentYIndex++
            }
        }
    }

    private fun renderFooter(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val totalWidth = right - left
        val col1Width = totalWidth * 0.36f
        val col2Width = totalWidth * 0.42f
        val col1Right = left + col1Width
        val col2Right = col1Right + col2Width

        // 2 vạch đứng phân cách
        canvas.drawLine(col1Right, top, col1Right, bottom, linePaint)
        canvas.drawLine(col2Right, top, col2Right, bottom, linePaint)

        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val lunar = LunarCalendar.getLunarDate(day, month, year)

        // Ô 1: Can Chi + Âm Lịch
        val footerTextSize = (bottom - top) * 0.26f
        textRegular.textSize = footerTextSize
        textRegular.textAlign = Paint.Align.LEFT
        val fPadding = 18f

        val line1Y = top + (bottom - top) * 0.42f
        val line2Y = top + (bottom - top) * 0.82f

        canvas.drawText("${lunar.canChiYear} (${lunar.animalYear})", left + fPadding, line1Y, textRegular)
        canvas.drawText("Âm Lịch ${lunar.day}/${lunar.month}", left + fPadding, line2Y, textRegular)

        // Ô 2: Sự kiện đếm ngược
        val event = LunarCalendar.getUpcomingEvent(cal)
        canvas.drawText(event.first, col1Right + fPadding, line1Y, textRegular)
        canvas.drawText("còn ${event.second} ngày", col1Right + fPadding, line2Y, textRegular)

        // Ô 3: Nhiệt độ số LED 7 đoạn (Góc phải dưới)
        val tempX = col2Right + fPadding
        val tempTextSize = (bottom - top) * 0.65f
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = tempTextSize
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("$temperature°", tempX, top + (bottom - top) * 0.72f, tempPaint)
        
        val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = tempTextSize * 0.45f
            textAlign = Paint.Align.LEFT
        }
        val tempMeasure = tempPaint.measureText("$temperature°")
        canvas.drawText("C", tempX + tempMeasure + 4f, top + (bottom - top) * 0.45f, cPaint)
    }
}
