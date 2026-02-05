/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class FrameTimeGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFC107")  // Amber for frame time
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, 800f,
            Color.parseColor("#80FFC107"),
            Color.parseColor("#00000000"),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30FFFFFF")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val avgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9800")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")  // Green for 60fps target (16.67ms)
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var frameTimeData: List<Pair<Long, Double>> = emptyList()
    private var avgFrameTime: Double = 0.0
    private var maxFrameTime = 50.0  // Dynamic max
    private var minFrameTime = 0.0
    
    private val padding = 80f
    private val topPadding = 40f
    private val bottomPadding = 80f

    fun setData(data: List<Pair<Long, Double>>, avg: Double) {
        this.frameTimeData = data
        this.avgFrameTime = avg
        
        // Calculate dynamic max (round up to nearest 10)
        if (data.isNotEmpty()) {
            val dataMax = data.maxOf { it.second }
            maxFrameTime = max(((dataMax / 10).toInt() + 1) * 10.0, 50.0)
        }
        
        post {
            fillPaint.shader = LinearGradient(
                0f, topPadding, 0f, height - bottomPadding,
                Color.parseColor("#80FFC107"),
                Color.parseColor("#10FFC107"),
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (frameTimeData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val graphWidth = width - 2 * padding
        val graphHeight = height - topPadding - bottomPadding

        drawGrid(canvas, graphWidth, graphHeight)
        drawTargetLine(canvas, graphWidth, graphHeight)  // 60fps target line
        drawAverageLine(canvas, graphWidth, graphHeight)
        drawGraph(canvas, graphWidth, graphHeight)
        drawLegend(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val message = "No frame time data available"
        val textWidth = textPaint.measureText(message)
        canvas.drawText(
            message,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // Calculate frame time steps
        val step = (maxFrameTime / 4).toInt()
        val frameTimeSteps = (0..4).map { it * step }
        
        for (frameTime in frameTimeSteps) {
            val y = (topPadding + graphHeight * (1 - frameTime / maxFrameTime)).toFloat()
            
            canvas.drawLine(padding, y, padding + graphWidth, y, gridPaint)
            val label = "${frameTime}ms"
            canvas.drawText(label, padding - 70f, y + 10f, textPaint)
        }

        // Y-axis label
        canvas.save()
        canvas.rotate(-90f, 15f, height / 2f)
        val yLabel = "Frame Time (ms)"
        val yLabelWidth = textPaint.measureText(yLabel)
        canvas.drawText(yLabel, (width - yLabelWidth) / 2, 30f, textPaint)
        canvas.restore()

        // Time labels on X-axis
        if (frameTimeData.isNotEmpty()) {
            val startTime = frameTimeData.first().first
            val endTime = frameTimeData.last().first
            val duration = endTime - startTime

            canvas.drawText("0s", padding, height - bottomPadding + 25f, textPaint)
            
            val middleTime = formatDuration(duration / 2)
            val middleX = padding + graphWidth / 2
            canvas.drawText(middleTime, middleX - 30f, height - bottomPadding + 25f, textPaint)
            
            val endTimeStr = formatDuration(duration)
            val endX = padding + graphWidth - textPaint.measureText(endTimeStr)
            canvas.drawText(endTimeStr, endX, height - bottomPadding + 25f, textPaint)
        }

        val xLabel = "Time"
        val xLabelWidth = textPaint.measureText(xLabel)
        canvas.drawText(xLabel, (width - xLabelWidth) / 2, height - bottomPadding + 55f, textPaint)
    }

    private fun drawTargetLine(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // 16.67ms = 60fps target
        val targetFrameTime = 16.67
        if (targetFrameTime <= maxFrameTime) {
            val y = (topPadding + graphHeight * (1 - targetFrameTime / maxFrameTime)).toFloat()
            canvas.drawLine(padding, y, padding + graphWidth, y, targetLinePaint)
        }
    }

    private fun drawAverageLine(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        val y = (topPadding + graphHeight * (1 - avgFrameTime / maxFrameTime)).toFloat()
        canvas.drawLine(padding, y, padding + graphWidth, y, avgLinePaint)
    }

    private fun drawGraph(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        if (frameTimeData.size < 2) return

        val startTime = frameTimeData.first().first
        val endTime = frameTimeData.last().first
        val timeDuration = max(endTime - startTime, 1L)

        val path = Path()
        val fillPath = Path()

        frameTimeData.forEachIndexed { index, (timestamp, frameTime) ->
            val x = padding + ((timestamp - startTime).toFloat() / timeDuration) * graphWidth
            
            val normalizedFrameTime = max(minFrameTime, min(maxFrameTime, frameTime))
            val y = (topPadding + graphHeight * (1 - normalizedFrameTime / maxFrameTime)).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - bottomPadding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        val lastX = padding + graphWidth
        fillPath.lineTo(lastX, height - bottomPadding)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }

    private fun drawLegend(canvas: Canvas) {
        val legendX = padding
        val legendY = 20f
        val lineLength = 40f
        val spacing = 150f

        // Frame Time line
        canvas.drawLine(legendX, legendY, legendX + lineLength, legendY, linePaint)
        canvas.drawText(context.getString(R.string.gb_frame_time), legendX + lineLength + 10f, legendY + 8f, textPaint.apply { textSize = 24f })

        // Average line
        canvas.drawLine(legendX + spacing * 1.5f, legendY, legendX + spacing * 1.5f + lineLength, legendY, avgLinePaint)
        canvas.drawText("Avg", legendX + spacing * 1.5f + lineLength + 10f, legendY + 8f, textPaint)

        // 60fps target line
        canvas.drawLine(legendX + spacing * 2.5f, legendY, legendX + spacing * 2.5f + lineLength, legendY, targetLinePaint)
        canvas.drawText("60fps", legendX + spacing * 2.5f + lineLength + 10f, legendY + 8f, textPaint)

        // Reset text size
        textPaint.textSize = 28f
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%dh%dm", hours, minutes % 60)
            minutes > 0 -> String.format("%dm%ds", minutes, seconds % 60)
            else -> String.format("%ds", seconds)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 800
        val desiredHeight = 600

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}
