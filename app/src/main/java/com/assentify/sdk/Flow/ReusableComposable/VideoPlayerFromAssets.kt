package com.assentify.sdk.Flow.ReusableComposable

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerFromAssets(
    assetFileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        val dataSourceFactory = { AssetDataSource(context) }
        val dataSource = dataSourceFactory()

        val assetUri = Uri.parse("asset:///$assetFileName")

        val mediaItem = MediaItem.fromUri(assetUri)

        val dataSpec = DataSpec.Builder()
            .setUri(assetUri)
            .build()

        dataSource.open(dataSpec)

        // Build MediaSource manually
        val mediaSource = ProgressiveMediaSource.Factory { dataSource }
            .createMediaSource(mediaItem)

        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL // Loop video
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        }
    )
}

