package com.assentify.sdk.Flow.ReusableComposable

import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun LogoSvgUrl(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable ((Throwable) -> Unit)? = null,
) {
    var drawable by remember(url) { mutableStateOf(SvgMemoryCache.get(url)) }
    var isLoading by remember(url) { mutableStateOf(drawable == null) }
    var loadError by remember(url) { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(url) {
        if (drawable != null) return@LaunchedEffect

        isLoading = true
        loadError = null

        try {
            val loaded = loadSvgDrawable(url)
            SvgMemoryCache.put(url, loaded)
            drawable = loaded
        } catch (t: Throwable) {
            loadError = t
        } finally {
            isLoading = false
        }
    }

    val context = LocalContext.current

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawable)
            }
        )

        loadError?.let { err ->
            error?.invoke(err) ?: Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Failed to load",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}