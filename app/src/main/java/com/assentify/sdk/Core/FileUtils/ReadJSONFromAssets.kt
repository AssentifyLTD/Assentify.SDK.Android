package com.assentify.sdk.Core.FileUtils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ReadJSONFromAsset(private val context: Context) {

    private val identifier = "[ReadJSONFromAsset]"

    fun readJSONFromAssets(path: String): String {
        return try {
            val file = context.assets.open(path)
            val bufferedReader = BufferedReader(InputStreamReader(file))
            val stringBuilder = StringBuilder()
            bufferedReader.useLines { lines ->
                lines.forEach { stringBuilder.append(it) }
            }
            val jsonString = stringBuilder.toString()
            jsonString
        } catch (e: Exception) {
            Log.e(
                identifier,
                "Error reading JSON: $e."
            )
            e.printStackTrace()
            ""
        }
    }
}
