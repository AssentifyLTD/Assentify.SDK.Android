package com.assentify.sdk.Flow.ReusableComposable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.ApiKeyObject

@Composable
fun SecureImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiKey = ApiKeyObject.getApiKeyObject()

    val url = imageUrl
        .trim()
        .replaceFirst(Regex("^Https://"), "https://")
        .replaceFirst(Regex("^Http://"), "http://")

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .addHeader("X-Api-Key", apiKey)
            .crossfade(true)
            .listener(
                onError = { _, result ->
                    android.util.Log.e("SecureImage", "Load failed: $url", result.throwable)
                },
                onSuccess = { _, _ ->
                    android.util.Log.d("SecureImage", "Loaded: $url")
                }
            )
            .build(),
        modifier = modifier,
        contentDescription = null
    )
}
