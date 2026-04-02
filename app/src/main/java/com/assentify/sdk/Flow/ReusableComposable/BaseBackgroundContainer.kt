package com.assentify.sdk.Flow.ReusableComposable

import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.util.LruCache
import android.view.View
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
import java.net.HttpURLConnection
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

object SvgMemoryCache {
    private val cache = object : LruCache<String, PictureDrawable>(20) {
        override fun sizeOf(key: String, value: PictureDrawable): Int = 1
    }

    fun get(url: String): PictureDrawable? = cache.get(url)

    fun put(url: String, drawable: PictureDrawable) {
        cache.put(url, drawable)
    }

    fun clear() {
        cache.evictAll()
    }
}

suspend fun loadSvgDrawable(url: String): PictureDrawable {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            doInput = true
            connect()
        }

        connection.inputStream.use { input ->
            val svg = SVG.getFromInputStream(input)
            val picture: Picture = svg.renderToPicture()
            PictureDrawable(picture)
        }.also {
            connection.disconnect()
        }
    }
}

@Composable
fun SvgUrlBackground(
    url: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    var drawable by remember(url) { mutableStateOf(SvgMemoryCache.get(url)) }
    var isLoading by remember(url) { mutableStateOf(drawable == null) }
    var error by remember(url) { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(url) {
        if (drawable != null) return@LaunchedEffect

        isLoading = true
        error = null

        try {
            val loaded = loadSvgDrawable(url)
            SvgMemoryCache.put(url, loaded)
            drawable = loaded
        } catch (t: Throwable) {
            error = t
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val context = LocalContext.current

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawable)
            }
        )

        content()
    }
}
