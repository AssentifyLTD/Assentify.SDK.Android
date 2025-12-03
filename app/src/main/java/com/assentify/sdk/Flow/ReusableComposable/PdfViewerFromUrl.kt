package com.assentify.sdk.Flow.ReusableComposable

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

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

    val iconSvg= remember {
        loadSvgFromAssets(context, "ic_download.svg")
    }

    var isLoading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var pdfFile by remember(url) { mutableStateOf<File?>(null) }

    // Download the PDF to cache when URL changes
    LaunchedEffect(url) {
        isLoading = true
        error = null
        try {
            pdfFile = withContext(Dispatchers.IO) { downloadPdfToCache(context, url) }
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
                        color = Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor))
                    )
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Failed to load PDF",
                        color = Color.Red
                    )
                }
            }
            pdfFile != null -> {
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
                                contentDescription = "ic_download",
                                modifier = Modifier.size(100.dp).padding(5.dp),
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
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}