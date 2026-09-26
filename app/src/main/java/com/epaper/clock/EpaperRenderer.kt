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
    var temperature: Int = 31

    var dailyQuote: String = "Hành trình vạn dặm bắt đầu từ một bước chân."
    var todayGoal: String = "Tập thể dục 30p • Đọc 20 trang sách"
    var monthGoal: String = "Tiết kiệm chi tiêu • Hoàn thành dự án"
    var yearGoal: String = "Chạy bộ 500km • Học kỹ năng mới"

    fun render(canvas: Canvas, width: Int, height: Int) {
        // 1. Nền đen sâu AMOLED
        canvas.drawColor(bgBezelColor)

        // 2. Khung thẻ chính E-paper Đồng hồ & Lịch
        val cardMarginH = width * 0.035f
        val cardWidth = width - (cardMarginH * 2)
        val cardHeight = cardWidth * 0.76f
        val cardLeft = cardMarginH
        val cardTop = height * 0.075f // Đặt gọn gàng ở 7.5% mép trên
        val cardRight = cardLeft + cardWidth
        val cardBottom = cardTop + cardHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cornerRadius = 14f

        // Vẽ nền thẻ E-ink & Viền khung ngoài
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Viền chỉ đôi bên trong
        val innerPadding = cardWidth * 0.018f
        val innerRect = RectF(
            cardLeft + innerPadding,
            cardTop + innerPadding,
            cardRight - innerPadding,
            cardBottom - innerPadding
        )
        canvas.drawRect(innerRect, linePaint)

        // Phân chia layout thẻ chính:
        val headerHeight = cardHeight * 0.15f
        val footerHeight = cardHeight * 0.23f
        val headerBottom = innerRect.top + headerHeight
        val footerTop = innerRect.bottom - footerHeight

        canvas.drawLine(innerRect.left, headerBottom, innerRect.right, headerBottom, linePaint)
        canvas.drawLine(innerRect.left, footerTop, innerRect.right, footerTop, linePaint)

        val now = Calendar.getInstance()

        // VẼ PHẦN HEADER
        renderHeader(canvas, innerRect.left, innerRect.top, innerRect.right, headerBottom, now)

        // VẼ PHẦN BODY (Đồng hồ kim + Kim giây & Lịch tháng)
        val splitX = innerRect.left + (innerRect.width() * 0.44f)
        renderAnalogClock(canvas, innerRect.left, headerBottom, splitX, footerTop, now)
        renderCalendar(canvas, splitX, headerBottom, innerRect.right, footerTop, now)

        // VẼ PHẦN FOOTER (3 Cột)
        renderFooter(canvas, innerRect.left, footerTop, innerRect.right, innerRect.bottom, now)

        // 3. VẼ THẺ MỤC TIÊU & CÂU NÓI TRUYỀN CẢM HỨNG PHÍA DƯỚI
        renderGoalAndQuoteCard(canvas, cardLeft, cardBottom + 16f, cardRight, now)
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

        // Ngày dd/MM/yyyy
        inkPaint.textAlign = Paint.Align.LEFT
        inkPaint.textSize = textSize
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
        canvas.drawText(dateStr, left + 18f, centerY + (textSize * 0.35f), inkPaint)

        // Buổi: Sáng / Trưa / Chiều / Tối
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
        val dowX = left + ((right - left) * 0.63f)
        canvas.drawText(dowStr, dowX, centerY + (textSize * 0.32f), textRegular)

        // Điện áp & icon pin
        val voltStr = String.format(Locale.US, "%.1fv", batteryVoltage)
        textRegular.textAlign = Paint.Align.RIGHT
        textRegular.textSize = textSize * 0.68f
        val batRight = right - 12f
        val batWidth = 32f
        val batHeight = 18f
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

        // Họa tiết hoa nan hoa tỏa tròn ở tâm
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

        // Vạch chia phút (60 vạch) và vạch giờ (12 vạch)
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

        // Số 1 đến 12
        inkPaint.textAlign = Paint.Align.CENTER
        inkPaint.textSize = radius * 0.22f
        val numberRadius = radius * 0.72f

        for (i in 1..12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val numX = (cx + numberRadius * sin(angle)).toFloat()
            val numY = (cy - numberRadius * cos(angle)).toFloat() + (inkPaint.textSize * 0.35f)
            canvas.drawText(i.toString(), numX, numY, inkPaint)
        }

        // Giờ, Phút, Giây
        val hours = cal.get(Calendar.HOUR)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)

        val hourAngle = Math.toRadians(((hours * 30) + (minutes * 0.5)).toDouble())
        val minuteAngle = Math.toRadians((minutes * 6).toDouble())
        val secAngle = Math.toRadians((seconds * 6).toDouble())

        // 1. Kim giờ
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
        }
        val hourLen = radius * 0.52f
        canvas.drawLine(
            cx, cy,
            (cx + hourLen * sin(hourAngle)).toFloat(),
            (cy - hourLen * cos(hourAngle)).toFloat(),
            hourPaint
        )

        // 2. Kim phút
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
        }
        val minLen = radius * 0.80f
        canvas.drawLine(
            cx, cy,
            (cx + minLen * sin(minuteAngle)).toFloat(),
            (cy - minLen * cos(minuteAngle)).toFloat(),
            minPaint
        )

        // 3. Kim giây
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 2.2f
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
        canvas.drawCircle(cx, cy, 7.5f, dotPaint)
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

    private fun renderFooter(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cal: Calendar
    ) {
        val totalWidth = right - left
        val col1Width = totalWidth * 0.37f
        val col2Width = totalWidth * 0.36f
        val col1Right = left + col1Width
        val col2Right = col1Right + col2Width

        canvas.drawLine(col1Right, top, col1Right, bottom, linePaint)
        canvas.drawLine(col2Right, top, col2Right, bottom, linePaint)

        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val lunar = LunarCalendar.getLunarDate(day, month, year)

        // Ô 1: Can Chi + Âm Lịch
        val footerTextSize = (bottom - top) * 0.27f
        textRegular.textAlign = Paint.Align.LEFT
        textRegular.textSize = footerTextSize
        val fPadding = 18f

        val line1Y = top + (bottom - top) * 0.42f
        val line2Y = top + (bottom - top) * 0.82f

        canvas.drawText("${lunar.canChiYear} (${lunar.animalYear})", left + fPadding, line1Y, textRegular)
        canvas.drawText("Âm Lịch ${lunar.day}/${lunar.month}", left + fPadding, line2Y, textRegular)

        // Ô 2: Sự kiện đếm ngược
        val event = LunarCalendar.getUpcomingEvent(cal)
        canvas.drawText(event.first, col1Right + fPadding, line1Y, textRegular)
        canvas.drawText("còn ${event.second} ngày", col1Right + fPadding, line2Y, textRegular)

        // Ô 3: Nhiệt độ số LED 7 đoạn
        val col3CenterX = (col2Right + right) / 2f
        val tempTextSize = (bottom - top) * 0.68f

        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = tempTextSize
            textAlign = Paint.Align.RIGHT
        }
        val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = tempTextSize * 0.48f
            textAlign = Paint.Align.LEFT
        }

        val baselineY = top + (bottom - top) * 0.74f
        canvas.drawText("$temperature°", col3CenterX + 10f, baselineY, tempPaint)
        canvas.drawText("C", col3CenterX + 12f, baselineY - (tempTextSize * 0.35f), cPaint)
    }

    /**
     * VẼ THẺ MỤC TIÊU & CÂU NÓI TRUYỀN CẢM HỨNG (GOALS & QUOTES CARD)
     */
    private fun renderGoalAndQuoteCard(canvas: Canvas, left: Float, top: Float, right: Float, cal: Calendar) {
        val cardWidth = right - left
        val cardHeight = cardWidth * 0.60f
        val bottom = top + cardHeight
        val rect = RectF(left, top, right, bottom)
        val cornerRadius = 14f

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cardPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val pad = cardWidth * 0.018f
        val inner = RectF(left + pad, top + pad, right - pad, bottom - pad)
        canvas.drawRect(inner, linePaint)

        // 1. KHU VỰC CÂU NÓI TRUYỀN CẢM HỨNG (CHIẾM 35% CHIỀU CAO THẺ)
        val quoteH = cardHeight * 0.35f
        val quoteBottom = inner.top + quoteH
        canvas.drawLine(inner.left, quoteBottom, inner.right, quoteBottom, linePaint)

        // Tiêu đề nhỏ "CHÂM NGÔN HÔM NAY"
        val qTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = quoteH * 0.22f
            textAlign = Paint.Align.CENTER
        }
        val centerX = (inner.left + inner.right) / 2f
        canvas.drawText("❝ CHÂM NGÔN TRUYỀN CẢM HỨNG ❞", centerX, inner.top + (quoteH * 0.30f), qTitlePaint)

        // Nội dung câu nói chữ nghiêng mềm mại
        val quoteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textSize = quoteH * 0.32f
            textAlign = Paint.Align.CENTER
        }
        val maxQuoteW = inner.width() - 32f
        var displayQuote = "“ $dailyQuote ”"
        if (quoteTextPaint.measureText(displayQuote) > maxQuoteW) {
            displayQuote = android.text.TextUtils.ellipsize(
                displayQuote,
                TextPaint(quoteTextPaint),
                maxQuoteW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
        }
        canvas.drawText(displayQuote, centerX, inner.top + (quoteH * 0.72f), quoteTextPaint)

        // 2. KHU VỰC 3 MỤC TIÊU CÔNG VIỆC: NGÀY, THÁNG, NĂM
        val contentH = inner.bottom - quoteBottom
        val rowH = contentH / 3f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = rowH * 0.38f
            textAlign = Paint.Align.LEFT
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = rowH * 0.36f
            textAlign = Paint.Align.LEFT
        }

        val year = cal.get(Calendar.YEAR)
        val rows = arrayOf(
            Triple("Hôm nay:", todayGoal, "☑"),
            Triple("Tháng này:", monthGoal, "★"),
            Triple("Năm $year:", yearGoal, "🎯")
        )

        for (i in rows.indices) {
            val rY = quoteBottom + (i * rowH)
            if (i > 0) {
                val dashPaint = Paint(linePaint).apply {
                    pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                    strokeWidth = 1f
                }
                canvas.drawLine(inner.left + 10f, rY, inner.right - 10f, rY, dashPaint)
            }

            val icon = rows[i].third
            val label = rows[i].first
            val value = rows[i].second
            val baseline = rY + (rowH * 0.62f)

            labelPaint.textSize = rowH * 0.36f
            canvas.drawText("$icon $label", inner.left + 16f, baseline, labelPaint)

            val labelW = labelPaint.measureText("$icon $label ")
            val maxValW = (inner.right - 16f) - (inner.left + 16f + labelW)

            var displayVal = value
            if (valPaint.measureText(displayVal) > maxValW) {
                displayVal = android.text.TextUtils.ellipsize(
                    displayVal,
                    TextPaint(valPaint),
                    maxValW,
                    android.text.TextUtils.TruncateAt.END
                ).toString()
            }
            canvas.drawText(displayVal, inner.left + 16f + labelW, baseline, valPaint)
        }
    }
}
