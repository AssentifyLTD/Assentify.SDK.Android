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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val apiKey =  ApiKeyObject.getApiKeyObject();

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .addHeader("X-Api-Key", apiKey)
            .crossfade(true)
            .build(),
        modifier = modifier,
        contentDescription = "Secure Image",
    )
}
