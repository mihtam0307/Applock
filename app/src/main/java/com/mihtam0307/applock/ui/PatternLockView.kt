package com.mihtam0307.applock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * Khoá hình kiểu lưới 3x3 chấm tròn (giống khoá màn hình Android).
 * Kết quả trả về (onPatternComplete) là chuỗi số thứ tự các điểm đã nối,
 * ví dụ "0,1,2,5,8" — dùng để băm và so sánh làm mật khẩu.
 */
class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onPatternComplete: ((String) -> Unit)? = null

    private val dotRadius = 14f * resources.displayMetrics.density
    private val dotRadiusActive = 20f * resources.displayMetrics.density
    private val touchSlop = 40f * resources.displayMetrics.density

    private val dots = FloatArray(18) // 9 điểm x (x,y)
    private val selected = mutableListOf<Int>()

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88FFFFFF")
        style = Paint.Style.FILL
    }
    private val dotActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C9CFF")
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C9CFF")
        strokeWidth = 6f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var currentX = 0f
    private var currentY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cellW = w / 3f
        val cellH = h / 3f
        for (row in 0..2) {
            for (col in 0..2) {
                val index = row * 3 + col
                dots[index * 2] = cellW * col + cellW / 2f
                dots[index * 2 + 1] = cellH * row + cellH / 2f
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selected.clear()
                tryAddDot(event.x, event.y)
                currentX = event.x
                currentY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                tryAddDot(event.x, event.y)
                currentX = event.x
                currentY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (selected.size >= 2) {
                    onPatternComplete?.invoke(selected.joinToString(","))
                }
                postDelayed({
                    selected.clear()
                    invalidate()
                }, 250)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun tryAddDot(x: Float, y: Float) {
        for (i in 0 until 9) {
            if (selected.contains(i)) continue
            val dx = x - dots[i * 2]
            val dy = y - dots[i * 2 + 1]
            if (hypot(dx, dy) < touchSlop) {
                selected.add(i)
                break
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ các đường nối đã chọn
        for (i in 0 until selected.size - 1) {
            val a = selected[i]
            val b = selected[i + 1]
            canvas.drawLine(dots[a * 2], dots[a * 2 + 1], dots[b * 2], dots[b * 2 + 1], linePaint)
        }
        // Vẽ đường đang kéo tới vị trí ngón tay
        if (selected.isNotEmpty()) {
            val last = selected.last()
            canvas.drawLine(dots[last * 2], dots[last * 2 + 1], currentX, currentY, linePaint)
        }

        // Vẽ các chấm
        for (i in 0 until 9) {
            val isSelected = selected.contains(i)
            canvas.drawCircle(
                dots[i * 2], dots[i * 2 + 1],
                if (isSelected) dotRadiusActive else dotRadius,
                if (isSelected) dotActivePaint else dotPaint
            )
        }
    }

    fun reset() {
        selected.clear()
        invalidate()
    }
}
