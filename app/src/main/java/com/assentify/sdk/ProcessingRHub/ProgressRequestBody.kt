package com.assentify.sdk.ProcessingRHub

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink

public class ProgressRequestBody(
    private val fileData: ByteArray,
    private val contentType: String,
    private val listener: ProgressListener
) : RequestBody() {

    interface ProgressListener {
        fun onProgressUpdate(percentage: Int)
    }

    override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = fileData.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val bufferSize = 2048L
        var uploaded: Long = 0
        val buffer = ByteArray(bufferSize.toInt())
        val inputStream = fileData.inputStream()

        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            sink.write(buffer, 0, read)
            uploaded += read
            val progress = (100 * uploaded / contentLength()).toInt()
            listener.onProgressUpdate(progress)
        }
    }
}