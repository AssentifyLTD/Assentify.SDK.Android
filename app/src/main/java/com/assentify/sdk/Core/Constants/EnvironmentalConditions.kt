package com.assentify.sdk.Core.Constants

import android.util.Log


class EnvironmentalConditions(

    /**Detect**/
    var enableDetect: Boolean = true,
    var enableGuide: Boolean = true,


    var CustomColor: String,
    var HoldHandColor: String,

    var MotionCardLimit: Int = 10,
    var MotionPassportLimit: Int = 15,

    var BRIGHTNESS_HIGH_THRESHOLD: Int = 180,
    var BRIGHTNESS_LOW_THRESHOLD: Int = 50,

    var activeLiveType: ActiveLiveType = ActiveLiveType.NON,
) {


    init {
        require(CustomColor.isNotEmpty()) { "Invalid CustomColor value" }
        require(HoldHandColor.isNotEmpty()) { "Invalid HoldHandColor value" }
    }

    fun checkConditions(
        brightness: Double,
        environmentalConditions: EnvironmentalConditions,
    ): BrightnessEvents {
        return when {
            brightness < environmentalConditions.BRIGHTNESS_LOW_THRESHOLD -> BrightnessEvents.TooDark
            brightness > environmentalConditions.BRIGHTNESS_HIGH_THRESHOLD -> BrightnessEvents.TooBright
            else -> BrightnessEvents.Good
        }
    }

    fun isPredictionValid(confidence: Float): Boolean {
        val isValid =
            (confidence * 100) in ConstantsValues.PREDICTION_LOW_PERCENTAGE..ConstantsValues.PREDICTION_HIGH_PERCENTAGE
        return isValid
    }


}

