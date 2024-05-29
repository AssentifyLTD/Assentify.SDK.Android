package com.assentify.sdk.Core.FileUtils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class StorageUtils(
    private val folderImageName: String,
    private val folderVideosName: String,
    private val imageName: String,
    private val videoName: String
) {


    // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path
    // context.cacheDir.path

    fun saveBitmapToFile(bitmap: Bitmap, name: String, context: Context): String? {
        val mediaStorageDir = File(
            context.cacheDir.path + "/${folderImageName}",
            ""
        )
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        val file = File(mediaStorageDir.path + File.separator + "${imageName}_${name}.PNG")
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.absolutePath
    }

    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    fun deleteFolderContents(folderPath: String) {
        val folder = File(folderPath)
        if (folder.exists()) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    file.delete()
                }
            }
        }
    }

    fun getOutputMediaFile(context: Context): File {
        val mediaStorageDir = File(
            context.cacheDir.path + "/${folderVideosName}",
            ""
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
            }
        }
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(mediaStorageDir.path + File.separator + "${videoName}_${timeStamp}.mp4")
    }

    fun getImagesPath(context: Context): String {
        return context.cacheDir.path + "/${folderImageName}/" + "${imageName}_%d.PNG"
    }

    fun getImageFolder(context: Context): String {
        return context.cacheDir.path + "/${folderImageName}";
    }

    fun getVideosFolder(context: Context): String {
        return context.cacheDir.path + "/${folderVideosName}";
    }
}
