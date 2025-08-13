package com.assentify.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class RectFInfo(
    val rectF: RectF,
    val confidence: String,
    val className: String
)

class RectangleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

     var rectangleColor: Int? = Color.parseColor("#00FFFFFF");



    fun setCustomColor(CustomColor: String?) {
        rectangleColor = Color.parseColor(CustomColor)?.or(10);
    }
    private val textPaint: Paint = Paint().apply {
        color =Color.WHITE
        textSize = 50f
    }

    private val cornerSize = 40f

    private val listRectF: MutableList<Pair<RectF, String>> = mutableListOf()

    fun setListRect(items: MutableList<Pair<RectF, String>>) {
        listRectF.clear()
        listRectF.addAll(items)
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paddingLeft = 20f
        val snapshot = ArrayList(listRectF)
        snapshot.forEach { (rectF, text) ->
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize
            textPaint.color = rectangleColor!!;
            val backgroundRectF = RectF(
                rectF.left + paddingLeft,
                rectF.top,
                rectF.left + paddingLeft + textWidth,
                rectF.top + textHeight
            )
            val backgroundPaint = Paint().apply {
                color = rectangleColor!!
                style = Paint.Style.STROKE
            }
            val rectPaint: Paint = Paint().apply {
                color = rectangleColor!!;
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }
            canvas.drawRect(backgroundRectF, backgroundPaint)

            canvas.drawRoundRect(rectF, cornerSize, cornerSize, rectPaint)
            canvas.drawText(text, rectF.left + paddingLeft, rectF.top + textHeight, textPaint)
        }
    }

}
