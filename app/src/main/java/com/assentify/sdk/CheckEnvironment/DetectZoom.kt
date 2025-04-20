package com.assentify.sdk.CheckEnvironment
import android.graphics.RectF
import android.util.Log
import  com.assentify.sdk.Core.Constants.ZoomType
const val ZoomLimit = 5;
const val ZoomPassportLimit = 5;
class DetectZoom{
    fun calculatePercentageChangeWidth(rect: RectF): ZoomType {
        val aspectRatioDifference = rect.width();

        if (aspectRatioDifference in 150f..220f) {
            return ZoomType.SENDING
        }
        if (aspectRatioDifference < 150f) {
            return ZoomType.ZOOM_IN
        }
        if (aspectRatioDifference > 220f) {
            return ZoomType.ZOOM_OUT
        }
        return ZoomType.NO_DETECT
    }

    fun calculateFacePercentageChangeWidth(rect: RectF): ZoomType {
        val aspectRatioDifference = rect.width();
        if (aspectRatioDifference in 120f..150f) {
            return ZoomType.SENDING
        }
        if (aspectRatioDifference < 120f) {
            return ZoomType.ZOOM_IN
        }
        if (aspectRatioDifference > 150f) {
            return ZoomType.ZOOM_OUT
        }
        return ZoomType.NO_DETECT
    }

}

