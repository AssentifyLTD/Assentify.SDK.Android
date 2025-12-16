package com.assentify.sdk.Core.Constants


class EnvironmentalConditions(

    /**Detect**/
    var enableDetect: Boolean = true,
    var enableGuide: Boolean = true,


    var HoldHandColor: String,
    var CountDownNumbersColor: String = "#00FFFFFF",

    var MotionCardLimit: Int = 10,
    var MotionPassportLimit: Int = 15,

    var BRIGHTNESS_HIGH_THRESHOLD: Int = 180,
    var BRIGHTNESS_LOW_THRESHOLD: Int = 50,

    var activeLiveType: ActiveLiveType = ActiveLiveType.NONE,

    var activeLivenessCheckCount : Int = 0,

    var retryCount : Int = 3,
    var faceLivenessRetryCount : Int = 2,

    var minRam : Int = 8,
    var minCPUCores : Int = 6,
) {


    init {
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

