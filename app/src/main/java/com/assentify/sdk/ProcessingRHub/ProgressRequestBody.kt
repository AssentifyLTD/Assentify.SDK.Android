package com.assentify.sdk.ProcessingRHub

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okio.*

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, contentLength: Long, done: Boolean) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = try {
        delegate.contentLength()
    } catch (e: Exception) {
        -1 // unknown
    }

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var bytesSoFar = 0L

        val forwarding = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesSoFar += byteCount
                onProgress(bytesSoFar, total, bytesSoFar == total)
            }
        }

        val buffered = forwarding.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
    }
}
