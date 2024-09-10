package com.assentify.sdk.CheckEnvironment

import android.graphics.RectF


class DetectIfRectFInsideTheScreen {
    fun isRectFWithinMargins(
        rect: RectF,
        screenWidth: Float,
        screenHeight: Float,
    ): Boolean {

        val topMarginPercentage = 0.10f;
        val bottomMarginPercentage = 0.10f;
        val sideMarginPercentage = 0.10f;
        val topMargin = screenHeight * topMarginPercentage
        val bottomMargin = screenHeight * bottomMarginPercentage
        val sideMargin = screenWidth * sideMarginPercentage

        val effectiveTop = topMargin
        val effectiveBottom = screenHeight - bottomMargin
        val effectiveLeft = sideMargin
        val effectiveRight = screenWidth - sideMargin

        return rect.top >= effectiveTop &&
                rect.bottom <= effectiveBottom &&
                rect.left >= effectiveLeft &&
                rect.right <= effectiveRight
    }


}

