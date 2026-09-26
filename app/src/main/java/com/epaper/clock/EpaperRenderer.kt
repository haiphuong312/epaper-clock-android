package com.epaper.clock

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class EpaperRenderer {

    // Màu mực E-Paper
    private val bgBezelColor = Color.parseColor("#121212")     // Nền ngoài đen AMOLED Samsung
    private val cardBgColor = Color.parseColor("#EAEAE6")      // Nền giấy E-Ink xám nhạt
    private val inkColor = Color.parseColor("#151515")         // Mực đen đậm
    private val inkMutedColor = Color.parseColor("#6E6E6E")    // Mực xám nhạt cho vạch/tiêu đề
    private val borderStrokeColor = Color.parseColor("#222222")// Viền khung đen

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBgColor
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
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
        strokeWidth = 2.2f
    }

    var batteryVoltage: Float = 3.9f

    // Sự kiện tùy chỉnh
    var eventTitle: String = "Tết Dương Lịch"
    var eventDateStr: String = "01/01/2027"

    // Câu nói hay & chú thích diễn giải
    var dailyQuote: String = "Làm Chủ Bản Thân"
    var quoteNote: String = "Kiểm soát cảm xúc, rèn luyện tư duy và hành động kỷ luật mỗi ngày để vươn tới tự do đích thực."

    // Kế hoạch và hiển thị
    var showToday: Boolean = true
    var todayGoal: String = "Fix xong tool cookie"

    var showWeek: Boolean = true
    var weekGoal: String = "Hoàn thành mục tiêu tuần"

    var showMonth: Boolean = false
    var monthGoal: String = ""

    var showYear: Boolean = false
    var yearGoal: String = ""

    fun render(canvas: Canvas, width: Int, height: Int) {
        canvas.drawColor(bgBezelColor)

        val cardMarginH = width * 0.035f
        val cardWidth = width - (cardMarginH * 2)
        val cardHeight = cardWidth * 0.86f
        val cardLeft = cardMarginH
        val cardTop = height * 0.055f // Nâng nhẹ lên 5.5% để phía dưới cực kỳ rộng rãi cho thẻ mục tiêu to
        val cardRight = cardLeft + cardWidth
        val cardBottom = cardTop + cardHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cornerRadius = 14f

        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        val innerPadding = cardWidth * 0.018f
        val innerRect = RectF(
            cardLeft + innerPadding,
            cardTop + innerPadding,
            cardRight - innerPadding,
            cardBottom - innerPadding
        )
        canvas.drawRect(innerRect, linePaint)

        // Phân chia 4 phần trong Thẻ chính:
        val headerHeight = cardHeight * 0.13f
        val digitalClockHeight = cardHeight * 0.15f
        val footerHeight = cardHeight * 0.20f

        val headerBottom = innerRect.top + headerHeight
        val digitalClockBottom = headerBottom + digitalClockHeight
        val footerTop = innerRect.bottom - footerHeight

        // Kẻ 3 đường phân cách ngang
        canvas.drawLine(innerRect.left, headerBottom, innerRect.right, headerBottom, linePaint)
        canvas.drawLine(innerRect.left, digitalClockBottom, innerRect.right, digitalClockBottom, linePaint)
        canvas.drawLine(innerRect.left, footerTop, innerRect.right, footerTop, linePaint)

        val now = Calendar.getInstance()

        // 1. HEADER (Ngày, Buổi, Thứ, Pin)
        renderHeader(canvas, innerRect.left, innerRect.top, innerRect.right, headerBottom, now)

        // 2. DÒNG ĐỒNG HỒ SỐ 24H TO RÕ
        renderDigitalClock24h(canvas, innerRect.left, headerBottom, innerRect.right, digitalClockBottom, now)

        // 3. BODY (Đồng hồ kim & Lịch tháng)
        val splitX = innerRect.left + (innerRect.width() * 0.44f)
        renderAnalogClock(canvas, innerRect.left, digitalClockBottom, splitX, footerTop, now)
        renderCalendar(canvas, splitX, digitalClockBottom, innerRect.right, footerTop, now)

        // 4. FOOTER (2 CỘT: Âm Lịch & Sự kiện đếm ngược)
        renderFooter2Columns(canvas, innerRect.left, footerTop, innerRect.right, innerRect.bottom, now)

        // 5. THẺ MỤC TIÊU & CHÂM NGÔN TO RÕ RÀNG VỚI CHÚ THÍCH XUỐNG DÒNG
        renderSpaciousGoalAndQuoteCard(canvas, cardLeft, cardBottom + 16f, cardRight, now)
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
        val textSize = (bottom - top) * 0.46f

        inkPaint.textAlign = Paint.Align.LEFT
        inkPaint.textSize = textSize
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
        canvas.drawText(dateStr, left + 16f, centerY + (textSize * 0.35f), inkPaint)

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val periodStr = when (hour) {
            in 4..10 -> "Sáng"
            in 11..13 -> "Trưa"
            in 14..17 -> "Chiều"
            else -> "Tối"
        }
        textRegular.textAlign = Paint.Align.CENTER
        textRegular.textSize = textSize * 0.88f
        val periodX = left + ((right - left) * 0.43f)
        canvas.drawText(periodStr, periodX, centerY + (textSize * 0.32f), textRegular)

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
        val dowX = left + ((right - left) * 0.63f)
        canvas.drawText(dowStr, dowX, centerY + (textSize * 0.32f), textRegular)

        val voltStr = String.format(Locale.US, "%.1fv", batteryVoltage)
        textRegular.textAlign = Paint.Align.RIGHT
        textRegular.textSize = textSize * 0.68f
        val batRight = right - 12f
        val batWidth = 30f
        val batHeight = 17f
        val batX = batRight - batWidth
        val batY = centerY - (batHeight / 2f)

        canvas.drawText(voltStr, batX - 8f, centerY + (textSize * 0.25f), textRegular)

        val batPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRect(batX, batY, batX + batWidth, batY + batHeight, batPaint)
        canvas.drawRect(batX + batWidth, batY + 4f, batX + batWidth + 4f, batY + batHeight - 4f, batPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(batX + 3f, batY + 3f, batX + (batWidth * 0.75f), batY + batHeight - 3f, fillPaint)
    }

    private fun renderDigitalClock24h(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val rowHeight = bottom - top

        val digitalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = rowHeight * 0.76f
            textAlign = Paint.Align.CENTER
        }

        val time24h = SimpleDateFormat("HH : mm : ss", Locale.getDefault()).format(cal.time)
        val baseline = centerY + (digitalPaint.textSize * 0.34f)
        canvas.drawText(time24h, centerX, baseline, digitalPaint)
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
        val radius = ((right - left).coerceAtMost(bottom - top) / 2f) * 0.88f

        val flowerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4D4D0")
            strokeWidth = 1f
        }
        for (i in 0 until 48) {
            val ang = Math.toRadians((i * 7.5).toDouble())
            val flLen = radius * 0.38f
            canvas.drawLine(
                cx, cy,
                (cx + flLen * sin(ang)).toFloat(),
                (cy - flLen * cos(ang)).toFloat(),
                flowerPaint
            )
        }

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6).toDouble())
            val isHour = i % 5 == 0
            val tickLength = if (isHour) radius * 0.13f else radius * 0.05f
            tickPaint.strokeWidth = if (isHour) 3.5f else 1.8f

            val startX = (cx + (radius - tickLength) * sin(angle)).toFloat()
            val startY = (cy - (radius - tickLength) * cos(angle)).toFloat()
            val stopX = (cx + radius * sin(angle)).toFloat()
            val stopY = (cy - radius * cos(angle)).toFloat()

            canvas.drawLine(startX, startY, stopX, stopY, tickPaint)
        }

        inkPaint.textAlign = Paint.Align.CENTER
        inkPaint.textSize = radius * 0.22f
        val numberRadius = radius * 0.72f

        for (i in 1..12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val numX = (cx + numberRadius * sin(angle)).toFloat()
            val numY = (cy - numberRadius * cos(angle)).toFloat() + (inkPaint.textSize * 0.35f)
            canvas.drawText(i.toString(), numX, numY, inkPaint)
        }

        val hours = cal.get(Calendar.HOUR)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)

        val hourAngle = Math.toRadians(((hours * 30) + (minutes * 0.5)).toDouble())
        val minuteAngle = Math.toRadians((minutes * 6).toDouble())
        val secAngle = Math.toRadians((seconds * 6).toDouble())

        // Kim giờ
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 6.5f
            strokeCap = Paint.Cap.ROUND
        }
        val hourLen = radius * 0.52f
        canvas.drawLine(
            cx, cy,
            (cx + hourLen * sin(hourAngle)).toFloat(),
            (cy - hourLen * cos(hourAngle)).toFloat(),
            hourPaint
        )

        // Kim phút
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 4.2f
            strokeCap = Paint.Cap.ROUND
        }
        val minLen = radius * 0.80f
        canvas.drawLine(
            cx, cy,
            (cx + minLen * sin(minuteAngle)).toFloat(),
            (cy - minLen * cos(minuteAngle)).toFloat(),
            minPaint
        )

        // Kim giây
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 2.0f
            strokeCap = Paint.Cap.ROUND
        }
        val secLen = radius * 0.85f
        val secTailLen = radius * 0.22f

        canvas.drawLine(
            cx, cy,
            (cx + secLen * sin(secAngle)).toFloat(),
            (cy - secLen * cos(secAngle)).toFloat(),
            secPaint
        )
        canvas.drawLine(
            cx, cy,
            (cx - secTailLen * sin(secAngle)).toFloat(),
            (cy + secTailLen * cos(secAngle)).toFloat(),
            secPaint
        )
        val tailDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(
            (cx - (secTailLen * 0.75f) * sin(secAngle)).toFloat(),
            (cy + (secTailLen * 0.75f) * cos(secAngle)).toFloat(),
            4f,
            tailDotPaint
        )

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

        val headers = arrayOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
        val headerY = top + (calHeight * 0.17f)
        textMuted.textAlign = Paint.Align.CENTER
        textMuted.textSize = colWidth * 0.42f

        for (i in 0 until 7) {
            val hx = left + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(headers[i], hx, headerY, textMuted)
        }

        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val rowHeight = (calHeight - (headerY - top) - 16f) / 6f
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
            color = cardBgColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = colWidth * 0.48f
            textAlign = Paint.Align.CENTER
        }

        for (day in 1..maxDays) {
            val dx = left + (currentXIndex * colWidth) + (colWidth / 2f)
            val dy = headerY + (currentYIndex * rowHeight)

            if (day == currentDay) {
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

    private fun renderFooter2Columns(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val totalWidth = right - left
        val splitX = left + (totalWidth * 0.50f)

        canvas.drawLine(splitX, top, splitX, bottom, linePaint)

        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val lunar = LunarCalendar.getLunarDate(day, month, year)

        val footerTextSize = (bottom - top) * 0.28f
        textRegular.textAlign = Paint.Align.LEFT
        textRegular.textSize = footerTextSize
        val fPadding = 20f

        val line1Y = top + (bottom - top) * 0.42f
        val line2Y = top + (bottom - top) * 0.82f

        canvas.drawText(lunar.canChiYear, left + fPadding, line1Y, textRegular)
        canvas.drawText("Âm Lịch ${lunar.day}/${lunar.month}", left + fPadding, line2Y, textRegular)

        val daysLeft = calculateDaysUntil(eventDateStr, cal)
        val displayEventTitle = if (eventTitle.isNotBlank()) eventTitle else "Sự kiện"
        canvas.drawText(truncateText(displayEventTitle, textRegular, (right - splitX) - 30f), splitX + fPadding, line1Y, textRegular)
        canvas.drawText("còn $daysLeft ngày", splitX + fPadding, line2Y, textRegular)
    }

    private fun calculateDaysUntil(targetDateStr: String, currentCal: Calendar): Long {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val targetDate = sdf.parse(targetDateStr)
            if (targetDate != null) {
                val diff = targetDate.time - currentCal.timeInMillis
                (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            } else {
                96
            }
        } catch (_: Exception) {
            96
        }
    }

    /**
     * THẺ MỤC TIÊU & CHÂM NGÔN TO RÕ RÀNG VỚI CHÚ THÍCH TỰ ĐỘNG XUỐNG DÒNG
     */
    private fun renderSpaciousGoalAndQuoteCard(canvas: Canvas, left: Float, top: Float, right: Float, cal: Calendar) {
        val activeGoals = mutableListOf<Triple<String, String, String>>()
        val year = cal.get(Calendar.YEAR)

        if (showToday && todayGoal.isNotBlank()) {
            activeGoals.add(Triple("Hôm nay:", todayGoal, "☑"))
        }
        if (showWeek && weekGoal.isNotBlank()) {
            activeGoals.add(Triple("Tuần này:", weekGoal, "📅"))
        }
        if (showMonth && monthGoal.isNotBlank()) {
            activeGoals.add(Triple("Tháng này:", monthGoal, "★"))
        }
        if (showYear && yearGoal.isNotBlank()) {
            activeGoals.add(Triple("Năm $year:", yearGoal, "🎯"))
        }

        val cardWidth = right - left
        // Chiều cao rộng rãi, to đẹp như cũ (~0.62 tỉ lệ)
        val cardHeight = cardWidth * 0.62f
        val bottom = top + cardHeight

        val rect = RectF(left, top, right, bottom)
        val cornerRadius = 14f

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cardPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val pad = cardWidth * 0.018f
        val inner = RectF(left + pad, top + pad, right - pad, bottom - pad)
        canvas.drawRect(inner, linePaint)

        // 1. PHẦN CHÂM NGÔN & CHÚ THÍCH (Chiếm 45% chiều cao thẻ)
        val quoteSectionHeight = cardHeight * 0.44f
        val quoteBottom = inner.top + quoteSectionHeight
        canvas.drawLine(inner.left, quoteBottom, inner.right, quoteBottom, linePaint)

        val centerX = (inner.left + inner.right) / 2f
        val maxTextWidth = inner.width() - 36f

        // Tiêu đề nhỏ trên cùng
        val qHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = cardWidth * 0.030f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("❝ CHÂM NGÔN TRUYỀN CẢM HỨNG ❞", centerX, inner.top + (cardHeight * 0.07f), qHeaderPaint)

        // Tiêu đề châm ngôn TO VÀ ĐẬM (Chữ to như cũ)
        val quoteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textSize = cardWidth * 0.052f
            textAlign = Paint.Align.CENTER
        }
        val displayTitle = truncateText("“ $dailyQuote ”", quoteTitlePaint, maxTextWidth)
        canvas.drawText(displayTitle, centerX, inner.top + (cardHeight * 0.16f), quoteTitlePaint)

        // Chú thích diễn giải bên dưới: Chữ nhỏ hơn, tự động ngắt từ và xuống dòng gọn gàng
        if (quoteNote.isNotBlank()) {
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4A4A48")
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textSize = cardWidth * 0.032f
                textAlign = Paint.Align.CENTER
            }
            val lineHeight = notePaint.textSize * 1.35f
            drawMultiLineText(
                canvas = canvas,
                text = quoteNote,
                x = centerX,
                startY = inner.top + (cardHeight * 0.24f),
                paint = notePaint,
                maxWidth = maxTextWidth,
                lineHeight = lineHeight,
                maxLines = 3
            )
        }

        // 2. PHẦN KẾ HOẠCH MỤC TIÊU (Chiếm 56% còn lại)
        val goalsTop = quoteBottom
        val goalCount = activeGoals.size.coerceAtLeast(1)
        val availableGoalsHeight = inner.bottom - goalsTop
        val rowH = availableGoalsHeight / goalCount

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = (rowH * 0.38f).coerceAtMost(cardWidth * 0.038f)
            textAlign = Paint.Align.LEFT
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = (rowH * 0.36f).coerceAtMost(cardWidth * 0.036f)
            textAlign = Paint.Align.LEFT
        }

        for (i in activeGoals.indices) {
            val rY = goalsTop + (i * rowH)
            if (i > 0) {
                val dashPaint = Paint(linePaint).apply {
                    pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                    strokeWidth = 1f
                }
                canvas.drawLine(inner.left + 12f, rY, inner.right - 12f, rY, dashPaint)
            }

            val icon = activeGoals[i].third
            val label = activeGoals[i].first
            val value = activeGoals[i].second
            val baseline = rY + (rowH * 0.62f)

            canvas.drawText("$icon $label", inner.left + 18f, baseline, labelPaint)

            val labelW = labelPaint.measureText("$icon $label ")
            val maxValW = (inner.right - 18f) - (inner.left + 18f + labelW)

            val displayVal = truncateText(value, valPaint, maxValW)
            canvas.drawText(displayVal, inner.left + 18f + labelW, baseline, valPaint)
        }
    }

    /**
     * HÀM TỰ ĐỘNG NGẮT TỪ VÀ XUỐNG DÒNG (PURE KOTLIN, KHÔNG PHỤ THUỘC TEXTPAINT)
     */
    private fun drawMultiLineText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float,
        lineHeight: Float,
        maxLines: Int = 3
    ) {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = startY
        var linesDrawn = 0

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    if (linesDrawn == maxLines - 1) {
                        canvas.drawText(truncateText(currentLine, paint, maxWidth), x, currentY, paint)
                        return
                    }
                    canvas.drawText(currentLine, x, currentY, paint)
                    linesDrawn++
                    currentY += lineHeight
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty() && linesDrawn < maxLines) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) {
            t = t.dropLast(1)
        }
        return if (t.isEmpty()) "" else "$t…"
    }
}
