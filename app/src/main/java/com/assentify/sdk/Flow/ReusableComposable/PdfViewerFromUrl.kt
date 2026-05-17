package com.assentify.sdk.Flow.ReusableComposable

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

// ─────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────
private const val TAG                = "PdfViewer"
private const val MIN_CONTENT_PIXELS = 50
private const val WHITE_TOLERANCE    = 245
private const val RENDER_SCALE       = 2.5f
private const val PADDING_PX         = 24

// ─────────────────────────────────────────────
// FileProvider — backed by res/xml/sdk_file_provider_paths.xml
// ─────────────────────────────────────────────
class InlineFileProvider : FileProvider() {
    companion object {
        fun getUri(context: Context, file: File): Uri =
            getUriForFile(
                context,
                "${context.packageName}.inline_provider",
                file
            )
    }
}

// ─────────────────────────────────────────────
// Open PDF in system viewer
// ─────────────────────────────────────────────
fun openPdfWithSystemViewer(context: Context, pdfFile: File) {
    try {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            InlineFileProvider.getUri(context, pdfFile)
        } else {
            @Suppress("DEPRECATION")
            Uri.fromFile(pdfFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        context.startActivity(Intent.createChooser(intent, "Open PDF with…"))

    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open PDF with system viewer", e)
        Toast.makeText(context, "Failed to open PDF", Toast.LENGTH_SHORT).show()
    }
}

// ─────────────────────────────────────────────
// Public entry point
// ─────────────────────────────────────────────
@Composable
fun PdfViewerFromUrl(
    url: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val iconFullscreen = remember("ic_fullscreen.svg") {
        loadSvgFromAssets(context, "ic_fullscreen.svg")
    }
    val iconDownload = remember("ic_download.svg") {
        loadSvgFromAssets(context, "ic_download.svg")
    }

    var isLoading     by remember(url) { mutableStateOf(true) }
    var error         by remember(url) { mutableStateOf<String?>(null) }
    var pdfFile       by remember(url) { mutableStateOf<File?>(null) }
    var previewBitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    val pages         = remember(url) { mutableStateListOf<Bitmap>() }

    // Download + render preview
    LaunchedEffect(url) {
        isLoading = true
        error     = null
        pdfFile   = null
        previewBitmap?.recycle()
        previewBitmap = null
        pages.forEach { it.recycle() }
        pages.clear()

        try {
            val f = withContext(Dispatchers.IO) { downloadPdfToCache(context, url) }
            pdfFile = f
            previewBitmap = withContext(Dispatchers.IO) {
                renderFirstMeaningfulPage(f, scale = 1.5f)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Load failed: $url", t)
            error = t.message ?: "Failed to load PDF"
        } finally {
            isLoading = false
        }
    }

    // ── Inline preview ──
    Box(
        modifier = modifier
            .background(Color.White)
            .clip(RoundedCornerShape(12.dp))
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BaseTheme.BaseTextColor)
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Failed to load PDF", color = BaseTheme.BaseRedColor)
                }
            }

            pdfFile != null -> {
                // Preview bitmap or placeholder
                if (previewBitmap != null && !previewBitmap!!.isRecycled) {
                    Image(
                        bitmap             = previewBitmap!!.asImageBitmap(),
                        contentDescription = "PDF preview",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PDF", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }

                // Action buttons
                Row(
                    modifier              = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download
                    IconButton(
                        onClick  = { startPdfDownload(context, url, fileName) },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        iconDownload?.let {
                            Icon(
                                painter            = it,
                                contentDescription = "Download PDF",
                                modifier           = Modifier.size(22.dp),
                                tint               = Color.White
                            )
                        }
                    }

                    // Open in system viewer
                    IconButton(
                        onClick  = { pdfFile?.let { openPdfWithSystemViewer(context, it) } },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        iconFullscreen?.let {
                            Icon(
                                painter            = it,
                                contentDescription = "Open full screen",
                                tint               = Color.White,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Cleanup on url change or composable disposal
    DisposableEffect(url) {
        onDispose {
            previewBitmap?.recycle()
            pages.forEach { it.recycle() }
            pages.clear()
        }
    }
}

// ─────────────────────────────────────────────
// Render helpers
// ─────────────────────────────────────────────

private fun renderFirstMeaningfulPage(pdfFile: File, scale: Float): Bitmap? {
    return try {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                var fallback: Bitmap? = null

                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val w   = max(1, (page.width  * scale).toInt())
                        val h   = max(1, (page.height * scale).toInt())
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        if (fallback == null) fallback = bmp

                        if (countNonWhitePixels(bmp) >= MIN_CONTENT_PIXELS) {
                            return@use bmp   // first meaningful page found
                        }
                    }
                }
                fallback // every page was blank — return page 0 anyway
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "renderFirstMeaningfulPage failed", e)
        null
    }
}

private fun renderPage(page: PdfRenderer.Page): Bitmap? {
    return try {
        val w   = max(1, (page.width  * RENDER_SCALE).toInt())
        val h   = max(1, (page.height * RENDER_SCALE).toInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bmp
    } catch (e: Exception) {
        Log.e(TAG, "renderPage failed", e)
        null
    }
}

// ─────────────────────────────────────────────
// Pixel helpers
// ─────────────────────────────────────────────

private fun countNonWhitePixels(bitmap: Bitmap): Int {
    val w      = bitmap.width
    val h      = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    var count = 0
    for (i in pixels.indices step 4) {       // sample every 4th pixel for speed
        val p = pixels[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8)  and 0xFF
        val b =  p         and 0xFF
        if (r < WHITE_TOLERANCE || g < WHITE_TOLERANCE || b < WHITE_TOLERANCE) count++
    }
    return count
}

private fun smartCrop(bitmap: Bitmap): Bitmap {
    val w      = bitmap.width
    val h      = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w;  var maxX = 0
    var minY = h;  var maxY = 0

    for (y in 0 until h) {
        for (x in 0 until w) {
            val p = pixels[y * w + x]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8)  and 0xFF
            val b =  p         and 0xFF
            if (r < WHITE_TOLERANCE || g < WHITE_TOLERANCE || b < WHITE_TOLERANCE) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    if (maxX <= minX || maxY <= minY) return bitmap

    val left   = (minX - PADDING_PX).coerceAtLeast(0)
    val top    = (minY - PADDING_PX).coerceAtLeast(0)
    val right  = (maxX + PADDING_PX).coerceAtMost(w)
    val bottom = (maxY + PADDING_PX).coerceAtMost(h)

    val cw   = right  - left
    val ch   = bottom - top
    val fill = (cw.toFloat() * ch) / (w.toFloat() * h)

    return if (fill > 0.80f) bitmap
    else Bitmap.createBitmap(bitmap, left, top, cw, ch)
}

// ─────────────────────────────────────────────
// Network helpers
// ─────────────────────────────────────────────

@Suppress("BlockingMethodInNonBlockingContext")
private fun downloadPdfToCache(context: Context, urlStr: String): File {
    val outFile = File(context.cacheDir, "pdf_${urlStr.hashCode()}.pdf")
    val conn    = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        connectTimeout          = 15_000
        readTimeout             = 20_000
        instanceFollowRedirects = true
        requestMethod           = "GET"
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

    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
}