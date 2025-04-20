package com.assentify.sdk.Core.FileUtils

import android.content.Context
import android.media.MediaPlayer
import java.io.IOException

 class AssetsAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(fileName: String) {
        stopAudio()

        try {
            val assetManager = context.assets
            val assetFileDescriptor = assetManager.openFd(fileName)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                prepare()
                start()

                setOnCompletionListener {
                    releaseMediaPlayer()
                }
            }

            assetFileDescriptor.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
}