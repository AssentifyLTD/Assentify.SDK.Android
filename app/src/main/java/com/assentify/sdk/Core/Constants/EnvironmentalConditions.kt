package com.assentify.sdk.Core.Constants


class EnvironmentalConditions(

    /**Detect**/
    var enableDetect: Boolean = true,
    var enableGuide: Boolean = true,


    var CustomColor: String,
    var HoldHandColor: String
) {


    init {
        require(CustomColor.isNotEmpty()) { "Invalid CustomColor value" }
        require(HoldHandColor.isNotEmpty()) { "Invalid HoldHandColor value" }
    }

    fun checkConditions(
        brightness: Double,
    ): BrightnessEvents {
        return when {
            brightness < ConstantsValues.BRIGHTNESS_LOW_THRESHOLD -> BrightnessEvents.TooDark
            brightness > ConstantsValues.BRIGHTNESS_HIGH_THRESHOLD -> BrightnessEvents.TooBright
            else -> BrightnessEvents.Good
        }
    }

    fun isPredictionValid(confidence: Float): Boolean {
        val isValid =
            (confidence * 100) in ConstantsValues.PREDICTION_LOW_PERCENTAGE..ConstantsValues.PREDICTION_HIGH_PERCENTAGE
        return isValid
    }


}

