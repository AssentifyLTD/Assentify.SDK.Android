package com.assentify.sdk.CheckEnvironment

import android.graphics.RectF
import android.util.Log
import com.assentify.sdk.Core.Constants.MotionType


class DetectMotion {

    fun calculatePercentageChange(rect1: RectF, rect2: RectF): MotionType {
        val centerX1: Float = rect1.centerX()
        val centerY1: Float = rect1.centerY()
        val centerX2: Float = rect2.centerX()
        val centerY2: Float = rect2.centerY()

        val percentageChangeX = (centerX2 - centerX1) / centerX1 * 100
        val percentageChangeY = (centerY2 - centerY1) / centerY1 * 100
        if (percentageChangeX in -20f..20f && percentageChangeY in -20f..20f) {
            return MotionType.SENDING
        }
        return MotionType.HOLD_YOUR_HAND
    }

    fun calculatePercentageChangePassport(rect1: RectF, rect2: RectF): MotionType {
        val centerX1: Float = rect1.centerX()
        val centerY1: Float = rect1.centerY()
        val centerX2: Float = rect2.centerX()
        val centerY2: Float = rect2.centerY()

        val percentageChangeX = (centerX2 - centerX1) / centerX1 * 100
        val percentageChangeY = (centerY2 - centerY1) / centerY1 * 100

        if (percentageChangeX in -20f..20f && percentageChangeY in -20f..20f) {
            return MotionType.SENDING
        }
        return MotionType.HOLD_YOUR_HAND
    }


}

