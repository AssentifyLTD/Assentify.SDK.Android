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

    /// SDK TODO
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data("https://blob.assentify.com/v2/Document/_UPJe9GIoCL82L1Cg6mt_tJGPgp-9uCh8fN0b2QjcL4=/image.jpg")
            .addHeader("X-Api-Key", "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w")
          /*  .data(imageUrl)
            .addHeader("X-Api-Key", apiKey)*/
            .crossfade(true)
            .build(),
        modifier = modifier,
        contentDescription = "Secure Image",
    )
}
