package com.assentify.sdk.Flow.ReusableComposable

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

@Composable
fun PdfViewerFromUrl(
    url: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    var showFullScreen by remember { mutableStateOf(false) }

    val iconPainter = remember("ic_fullscreen.svg") {
        loadSvgFromAssets(context, "ic_fullscreen.svg")
    }
    val iconSvg = remember {
        loadSvgFromAssets(context, "ic_download.svg")
    }

    var isLoading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var pdfFile by remember(url) { mutableStateOf<File?>(null) }

    // Inline preview bitmap (page 0 only)
    var previewBitmap by remember(url) { mutableStateOf<Bitmap?>(null) }

    // Fullscreen pages
    val pages = remember(url) { mutableStateListOf<Bitmap>() }

    // Download the PDF to cache when URL changes
    LaunchedEffect(url) {
        isLoading = true
        error = null
        pdfFile = null
        previewBitmap = null
        pages.clear()

        try {
            val f = withContext(Dispatchers.IO) { downloadPdfToCache(context, url) }
            pdfFile = f

            // render preview (first page)
            previewBitmap = withContext(Dispatchers.IO) {
                renderPdfPageToBitmap(f, pageIndex = 0, scale = 1.5f)
            }
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load PDF"
        } finally {
            isLoading = false
        }
    }

    // --- Inline Preview ---
    Box(
        modifier = modifier
            .background(Color.White)
            .clip(RoundedCornerShape(12.dp))
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color =   BaseTheme.BaseTextColor
                    )
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Failed to load PDF", color = BaseTheme.BaseRedColor)
                }
            }

            pdfFile != null && previewBitmap != null -> {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "PDF preview",
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 🔹 Download icon
                    IconButton(
                        onClick = {
                            startPdfDownload(
                                context = context,
                                url = url,
                                fileName = fileName
                            )
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        iconSvg?.let {
                            Icon(
                                painter = it,
                                contentDescription = "Download PDF",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // 🔹 Full-screen icon
                    IconButton(
                        onClick = { showFullScreen = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        iconPainter?.let {
                            Icon(
                                painter = it,
                                contentDescription = "Open full screen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Full-screen Dialog ---
    if (showFullScreen && pdfFile != null) {
        Dialog(onDismissRequest = { showFullScreen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                PdfRendererFullScreen(
                    pdfFile = pdfFile!!,
                    pages = pages,
                    modifier = Modifier.fillMaxSize()
                )

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

    // Cleanup preview bitmap when composable leaves / url changes
    DisposableEffect(url) {
        onDispose {
            previewBitmap?.recycle()
            previewBitmap = null

            pages.forEach { it.recycle() }
            pages.clear()
        }
    }
}

@Composable
private fun PdfRendererFullScreen(
    pdfFile: File,
    pages: MutableList<Bitmap>,
    modifier: Modifier = Modifier
) {
    var loading by remember(pdfFile.absolutePath) { mutableStateOf(pages.isEmpty()) }
    var error by remember(pdfFile.absolutePath) { mutableStateOf<String?>(null) }
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    LaunchedEffect(pdfFile.absolutePath) {
        if (pages.isNotEmpty()) return@LaunchedEffect
        loading = true
        error = null

        try {
            val rendered = withContext(Dispatchers.IO) {
                renderAllPages(pdfFile, targetScale = 2.0f)
            }
            pages.clear()
            pages.addAll(rendered)
        } catch (t: Throwable) {
            error = t.message ?: "Failed to render PDF"
        } finally {
            loading = false
        }
    }

    when {
        loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color =   BaseTheme.BaseTextColor
                )
            }
        }

        error != null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Failed to render PDF", color = Color.White)
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                itemsIndexed(pages, key = { index, _ -> index }) { index, bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF page ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private fun downloadPdfToCache(context: Context, urlStr: String): File {
    val uriHash = urlStr.hashCode().toString()
    val outFile = File(context.cacheDir, "pdf_$uriHash.pdf")

    val url = URL(urlStr)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 20_000
        instanceFollowRedirects = true
        requestMethod = "GET"
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome Safari"
        )
    }

    conn.inputStream.use { input ->
        FileOutputStream(outFile).use { output ->
            val buf = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                output.write(buf, 0, read)
            }
            output.flush()
        }
    }
    conn.disconnect()
    return outFile
}

fun startPdfDownload(
    context: Context,
    url: String,
    fileName: String = "document.pdf"
) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setDescription("Downloading PDF")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}

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

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    } finally {
        renderer.close()
        pfd.close()
    }
}

private fun renderAllPages(
    pdfFile: File,
    targetScale: Float = 2.0f
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

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
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
