package com.assentify.sdk.Flow.ReusableComposable

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import java.io.File
import kotlin.math.max

@Composable
fun PdfViewerFromBase64(
    base64Data: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    val pdfBytes = remember(base64Data) {
        Base64.decode(base64Data, Base64.DEFAULT)
    }

    // Write to cache (unique name per content to avoid stale file reuse)
    val pdfFile = remember(base64Data) {
        File(context.cacheDir, "temp_${base64Data.hashCode()}.pdf").apply {
            writeBytes(pdfBytes)
        }
    }

    var showFullScreen by remember { mutableStateOf(false) }

    val iconPainter = remember("ic_fullscreen.svg") {
        loadSvgFromAssets(context, "ic_fullscreen.svg")
    }

    // Load only the first page bitmap for inline preview
    val inlineBitmapState = remember { mutableStateOf<Bitmap?>(null) }
    val inlineLoading = remember { mutableStateOf(true) }
    val inlineError = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfFile.absolutePath) {
        inlineLoading.value = true
        inlineError.value = null
        inlineBitmapState.value = null

        runCatching {
            renderPdfPageToBitmap(pdfFile, pageIndex = 0, scale = 1.5f)
        }.onSuccess { bmp ->
            inlineBitmapState.value = bmp
        }.onFailure { e ->
            inlineError.value = e.message ?: "Failed to render PDF"
        }

        inlineLoading.value = false
    }

    // --- Inline Preview ---
    Box(
        modifier = modifier
            .background(Color.White)
            .clip(RoundedCornerShape(12.dp))
    ) {
        when {
            inlineLoading.value -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor))
                    )
                }
            }

            inlineError.value != null -> {
                // Keep it simple: black text can be added if you want
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    // No Text import in your snippet; add if you want to show error
                    // Text(inlineError.value ?: "Error")
                }
            }

            inlineBitmapState.value != null -> {
                Image(
                    bitmap = inlineBitmapState.value!!.asImageBitmap(),
                    contentDescription = "PDF preview",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

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
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
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
                    .background(Color.Transparent)
            ) {
                PdfRendererPagedViewer(
                    pdfFile = pdfFile,
                    modifier = Modifier.fillMaxSize()
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

/**
 * Full PDF viewer using PdfRenderer (no native .so libs bundled by your app),
 * compatible with 16KB devices.
 *
 * - Vertical paging via LazyColumn
 * - Simple pinch-to-zoom (applies to the whole list)
 */
@Composable
private fun PdfRendererPagedViewer(
    pdfFile: File,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    val pages = remember { mutableStateListOf<Bitmap>() }
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    // Simple global zoom for all pages in fullscreen
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pdfFile.absolutePath) {
        loading.value = true
        error.value = null
        pages.clear()

        runCatching {
            renderAllPages(pdfFile, targetScale = 2.0f) // tweak for sharper text
        }.onSuccess { bitmaps ->
            pages.addAll(bitmaps)
        }.onFailure { e ->
            error.value = e.message ?: "Failed to render PDF"
        }

        loading.value = false
    }

    // Make sure we release bitmaps when leaving fullscreen
    DisposableEffect(Unit) {
        onDispose {
            pages.forEach { it.recycle() }
            pages.clear()
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                    scale = newScale
                    pan += panChange
                }
            }
    ) {
        when {
            loading.value -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor))
                    )
                }
            }

            error.value != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Add Text if you want
                    // Text(error.value ?: "Error", color = Color.White)
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                    ) {
                        items(
                            items = pages,
                            key = { bmp -> bmp.hashCode() }
                        ) { bmp ->
                            // Keep aspect ratio; fill width
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Renders a single page into a Bitmap and closes PdfRenderer properly. */
private fun renderPdfPageToBitmap(
    pdfFile: File,
    pageIndex: Int,
    scale: Float = 1.0f
): Bitmap {
    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)

    try {
        val safeIndex = pageIndex.coerceIn(0, max(0, renderer.pageCount - 1))
        val page = renderer.openPage(safeIndex)

        try {
            val width = max(1, (page.width * scale).toInt())
            val height = max(1, (page.height * scale).toInt())

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            return bitmap
        } finally {
            page.close()
        }
    } finally {
        renderer.close()
        pfd.close()
    }
}

/** Renders all pages into Bitmaps. Consider paging/caching if PDFs are huge. */
private fun renderAllPages(
    pdfFile: File,
    targetScale: Float = 1.5f
): List<Bitmap> {
    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)

    try {
        val out = ArrayList<Bitmap>(renderer.pageCount)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            try {
                val width = max(1, (page.width * targetScale).toInt())
                val height = max(1, (page.height * targetScale).toInt())

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                out.add(bitmap)
            } finally {
                page.close()
            }
        }
        return out
    } finally {
        renderer.close()
        pfd.close()
    }
}
