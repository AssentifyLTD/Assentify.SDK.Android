package com.assentify.sdk.Core.FileUtils

import android.content.Context
import android.content.res.AssetManager
import android.graphics.drawable.PictureDrawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.caverock.androidsvg.SVG
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FileUtils {

    @Throws(IOException::class)
    fun loadModelFile(assets: AssetManager, modelFilename: String?): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename!!)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}

fun  loadSvgFromAssets(context: Context, assetPath: String): BitmapPainter? {
    return try {
        val inputStream: InputStream = context.assets.open(assetPath)
        val svg = SVG.getFromInputStream(inputStream)
        val picture = svg.renderToPicture()
        val drawable = PictureDrawable(picture)
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawPicture(picture)
        BitmapPainter(bitmap.asImageBitmap())
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}