package com.assentify.sdk.Flow.ReusableComposable

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import java.io.File


@Composable
fun PdfViewerFromBase64(
    base64Data: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pdfBytes = remember(base64Data) {
        Base64.decode(base64Data, Base64.DEFAULT)
    }

    val pdfFile = remember {
        File(context.cacheDir, "temp.pdf").apply { writeBytes(pdfBytes) }
    }

    var showFullScreen by remember { mutableStateOf(false) }
    val iconPainter = remember("ic_fullscreen.svg") {
        loadSvgFromAssets(context, "ic_fullscreen.svg")
    }

    // --- Inline Preview ---
    Box(
        modifier = modifier
            .background(Color.White)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                com.github.barteksc.pdfviewer.PDFView(ctx, null).apply {
                    fromFile(pdfFile)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .load()
                }
            }
        )

        // 🔹 Full-screen icon overlay
        IconButton(
            onClick = { showFullScreen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                .size(36.dp)
        ) {

            iconPainter?.let {
                Icon(
                    painter = it,
                    contentDescription = "passport",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),

                )
            }
        }
    }

    // --- Full-screen Dialog ---
    if (showFullScreen) {
        Dialog(onDismissRequest = { showFullScreen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        com.github.barteksc.pdfviewer.PDFView(ctx, null).apply {
                            fromFile(pdfFile)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .load()
                        }
                    }
                )

                // 🔹 Close (X) icon
                IconButton(
                    onClick = { showFullScreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
