package com.assentify.sdk.CheckEnvironment
import android.graphics.RectF
import android.util.Log
import  com.assentify.sdk.Core.Constants.ZoomType
const val ZoomLimit = 5;
class DetectZoom{
    fun calculatePercentageChangeWidth(rect: RectF): ZoomType {
        val aspectRatioDifference = rect.width();

        if (aspectRatioDifference in 150f..250f) {
            return ZoomType.SENDING
        }
        if (aspectRatioDifference < 250) {
            return ZoomType.ZOOM_IN
        }
        if (aspectRatioDifference > 150) {
            return ZoomType.ZOOM_OUT
        }
        return ZoomType.NO_DETECT
    }

    fun calculateFacePercentageChangeWidth(rect: RectF): ZoomType {
        val aspectRatioDifference = rect.width();
        if (aspectRatioDifference in 130f..200f) {
            return ZoomType.SENDING
        }
        if (aspectRatioDifference < 200) {
            return ZoomType.ZOOM_IN
        }
        if (aspectRatioDifference > 130f) {
            return ZoomType.ZOOM_OUT
        }
        return ZoomType.NO_DETECT
    }

}

