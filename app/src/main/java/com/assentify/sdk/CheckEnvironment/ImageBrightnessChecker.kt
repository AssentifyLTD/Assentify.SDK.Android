package com.assentify.sdk.CheckEnvironment
import android.graphics.Bitmap
import android.graphics.Color


class ImageBrightnessChecker {
    fun getAverageBrightness(bitmap: Bitmap): Int {
        var totalBrightness = 0
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val brightness = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
            totalBrightness += brightness
        }

        val averageBrightness = totalBrightness / (width * height)
        return averageBrightness
    }


}
