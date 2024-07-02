package com.assentify.sdk.Core.Constants


data class EnvironmentalConditions(

    /**Detect**/
    var enableDetect: Boolean,
    var enableGuide: Boolean,

    /**BRIGHTNESS**/
    var BRIGHTNESS_HIGH_THRESHOLD: Float,
    var BRIGHTNESS_LOW_THRESHOLD: Float,


    /**PREDICTION**/
    var PREDICTION_LOW_PERCENTAGE: Float,
    var PREDICTION_HIGH_PERCENTAGE: Float,

    var CustomColor: String,
    var HoldHandColor: String
) {

    var ASPECT_RATIO_SIZE_WIDTH = 4;
    var ASPECT_RATIO_SIZE_HEIGHT = 3;

    init {

        require(BRIGHTNESS_HIGH_THRESHOLD >= 0.0f) { "Invalid BRIGHTNESS_HIGH_THRESHOLD value" }
        require(BRIGHTNESS_LOW_THRESHOLD >= 0.0f) { "Invalid BRIGHTNESS_LOW_THRESHOLD value" }

        require(PREDICTION_LOW_PERCENTAGE >= 0.0f) { "Invalid PREDICTION_LOW_PERCENTAGE value" }
        require(PREDICTION_HIGH_PERCENTAGE >= 0.0f) { "Invalid PREDICTION_HIGH_PERCENTAGE value" }

        require(ASPECT_RATIO_SIZE_WIDTH >= 0) { "Invalid DESIRED_PREVIEW_SIZE_WIDTH value" }
        require(ASPECT_RATIO_SIZE_HEIGHT >= 0) { "Invalid DESIRED_PREVIEW_SIZE_HEIGHT value" }
        require(CustomColor.isNotEmpty()) { "Invalid CustomColor value" }
        require(HoldHandColor.isNotEmpty()) { "Invalid HoldHandColor value" }

    }

    fun checkConditions(
        brightness: Double,

    ): Boolean {

        val isBrightnessValid =
            brightness in BRIGHTNESS_LOW_THRESHOLD..BRIGHTNESS_HIGH_THRESHOLD

        return isBrightnessValid
    }

    fun isPredictionValid(confidence: Float): Boolean {
        val isValid =
            (confidence * 100) in PREDICTION_LOW_PERCENTAGE..PREDICTION_HIGH_PERCENTAGE
        return isValid
    }


}

