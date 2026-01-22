package com.assentify.sdk.Flow.ReusableComposable

import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.assentify.sdk.Core.Constants.BackgroundType
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun BaseBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    when (BaseTheme.BaseBackgroundType) {

        BackgroundType.Color -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(BaseTheme.BackgroundColor!!.toBrush())
            ) {
                content()
            }
        }

        BackgroundType.Image -> {

            SvgUrlBackground(url = BaseTheme.BaseBackgroundUrl, modifier = modifier) { content() }

        }

        else -> {}
    }
}



@Composable
fun SvgUrlBackground(
    url: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var drawable by remember(url) { mutableStateOf<PictureDrawable?>(null) }
    var error by remember(url) { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(url) {
        error = null
        drawable = null

        try {
            val picDrawable = withContext(Dispatchers.IO) {
                URL(url).openStream().use { input ->
                    val svg = SVG.getFromInputStream(input)
                    val picture: Picture = svg.renderToPicture()
                    PictureDrawable(picture)
                }
            }
            drawable = picDrawable
        } catch (t: Throwable) {
            error = t
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // Background SVG
        val ctx = LocalContext.current
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawable)
            }
        )

        // Your UI on top
        content()
    }
}

