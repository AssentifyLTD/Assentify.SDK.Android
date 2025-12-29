package com.assentify.sdk.CheckEnvironment

import android.graphics.RectF
import com.assentify.sdk.Core.Constants.MotionType

/// MotionLimit = 10;
/// MotionPassportLimit = 15;
class DetectMotion {
    fun calculatePercentageChangeFace(rect1: RectF, rect2: RectF): MotionType {
        val centerX1: Float = rect1.centerX()
        val centerY1: Float = rect1.centerY()
        val centerX2: Float = rect2.centerX()
        val centerY2: Float = rect2.centerY()

        val percentageChangeX = (centerX2 - centerX1) / centerX1 * 100
        val percentageChangeY = (centerY2 - centerY1) / centerY1 * 100
        if (percentageChangeX in -15f..15f && percentageChangeY in -15f..15f) {
            return MotionType.SENDING
        }
        return MotionType.HOLD_YOUR_HAND
    }
    fun calculatePercentageChange(rect1: RectF, rect2: RectF): MotionType {
        val centerX1: Float = rect1.centerX()
        val centerY1: Float = rect1.centerY()
        val centerX2: Float = rect2.centerX()
        val centerY2: Float = rect2.centerY()

        val percentageChangeX = (centerX2 - centerX1) / centerX1 * 100
        val percentageChangeY = (centerY2 - centerY1) / centerY1 * 100
        if (percentageChangeX in -5f..5f && percentageChangeY in -5f..5f) {
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

        if (percentageChangeX in -10f..10f && percentageChangeY in -10f..10f) {
            return MotionType.SENDING
        }
        return MotionType.HOLD_YOUR_HAND
    }


}