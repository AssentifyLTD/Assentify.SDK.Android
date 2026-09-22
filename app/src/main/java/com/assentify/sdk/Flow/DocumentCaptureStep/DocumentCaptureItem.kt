package com.assentify.sdk.Flow.DocumentCaptureStep

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.assentify.sdk.Core.Constants.UiLanguage
import com.assentify.sdk.DocumentCapture.DocumentCaptures
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

// ─────────────────────────── Config ───────────────────────────

/** `maxFileSize` from the config is assumed to be in MB. Change here if it is KB/bytes. */
private const val BYTES_PER_MAX_FILE_SIZE_UNIT = 1024L * 1024L

/** Must match `<provider android:authorities>` in the SDK manifest. */
private fun fileProviderAuthority(context: Context) = "${context.packageName}.assentify.fileprovider"

// ─────────────────────────── Slot kind ───────────────────────────

/**
 * What kind of content a slot currently holds. Drives the "slot contains → result" rules:
 *  - one file                → uploaded untouched
 *  - several IMAGE files     → stacked vertically, 10px gutter, written as PNG @ 0.95
 *  - several PDF files       → concatenated into one multi-page PDF, page order preserved
 *  - IMAGE + PDF in one slot → refused; the add is rejected, user must clear the slot first
 */
enum class SlotKind { IMAGE, PDF, OTHER }

fun kindOf(mimeType: String): SlotKind = when {
    mimeType.startsWith("image/") -> SlotKind.IMAGE
    mimeType == "application/pdf" -> SlotKind.PDF
    else -> SlotKind.OTHER
}

// ─────────────────────────── State ───────────────────────────

enum class UploadStatus { Uploading, Uploaded, Failed }

enum class DocError { UploadFailed, Uploading, Required, MinCount }

enum class PickerError { FormatNotAllowed, FileTooLarge, ReadFailed, CameraFailed, MixedType }

class CapturedItem(
    val id: String,
    val file: File,
    val displayName: String,
    val mimeType: String,
) {
    var status by mutableStateOf(UploadStatus.Uploading)
    var resultData: Map<String, String> = emptyMap()
    var uploadedAt: String? = null

    val isImage: Boolean get() = mimeType.startsWith("image/")
    val extension: String get() = displayName.substringAfterLast('.', "")
}

/** Stable key used to route upload callbacks back to the right card. */
val DocumentCaptures.stateKey: String
    get() = id ?: keyIdentifier ?: documentTitle.orEmpty()

class DocumentItemState(val doc: DocumentCaptures) {
    val items = mutableStateListOf<CapturedItem>()
    var showError by mutableStateOf(false)
    var pickerError by mutableStateOf<PickerError?>(null)

    /** The kind of the items currently sitting in this slot (null when empty). Slots are kept homogeneous. */
    val kind: SlotKind? get() = items.firstOrNull()?.let { kindOf(it.mimeType) }

    /**
     * Total items (live captures + uploaded files) allowed on this card.
     * NOTE: treated maxLiveCaptureImages / maxLiveCaptureLength as "max number of items".
     */
    val maxItems: Int
        get() = (doc.maxLiveCaptureLength ?: 1).coerceAtLeast(1)

    val minItems: Int
        get() = max(doc.minLiveCaptureImages ?: 0, if (doc.mandatory == true) 1 else 0)
            .coerceAtMost(maxItems)

    val canAddMore: Boolean get() = items.size < maxItems

    fun validationError(): DocError? = when {
        items.any { it.status == UploadStatus.Failed } -> DocError.UploadFailed
        items.any { it.status == UploadStatus.Uploading } -> DocError.Uploading
        doc.mandatory == true && items.isEmpty() -> DocError.Required
        items.isNotEmpty() && items.size < minItems -> DocError.MinCount
        else -> null
    }
}

// ─────────────────────────── Composable ───────────────────────────

@Composable
fun DocumentCaptureItem(
    state: DocumentItemState,
    onFileReady: (file: File, name: String, mimeType: String) -> Unit,
    onRemove: (CapturedItem) -> Unit,
    onRetry: (CapturedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val doc = state.doc
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = flowStrings()

    val accent = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
    val red = BaseTheme.BaseRedColor

    // ── Camera (full-resolution, saved to app cache, never the gallery) ──
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path != null) {
            val file = File(path)
            if (success && file.exists() && file.length() > 0) {
                onFileReady(file, file.name, "image/jpeg")
            } else {
                file.delete()
            }
        }
    }

    fun launchCamera() {
        state.pickerError = null
        try {
            val dir = File(context.cacheDir, "document_capture").apply { mkdirs() }
            val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
            pendingPhotoPath = file.absolutePath
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            state.pickerError = PickerError.CameraFailed
        }
    }

    // ── File picker ──
    // Deliberately NOT restricted to doc.allowedFormats here: some document providers (e.g. the
    // Downloads app) report a wrong/generic mime type for files (PDFs showing up as "BIN file"),
    // which makes Android's picker grey them out even though the file itself is fine. So we accept
    // anything here and enforce allowedFormats ourselves afterward, in readPickedFile, which checks
    // the file's extension as well as its reported mime type.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            state.pickerError = null
            scope.launch {
                when (val r = readPickedFile(context, uri, doc)) {
                    is PickResult.Ok -> onFileReady(r.file, r.name, r.mime)
                    is PickResult.Error -> state.pickerError = r.error
                }
            }
        }
    }

    // ── Error text ──
    val pickerError = state.pickerError
    val errorText: String? = when {
        pickerError != null -> when (pickerError) {
            PickerError.FormatNotAllowed -> s.docFormatNotAllowed(doc.allowedFormats.orEmpty().joinToString(", ") { it.uppercase() })
            PickerError.FileTooLarge -> s.docFileTooLarge(doc.maxFileSize ?: 0)
            PickerError.ReadFailed -> s.docReadFailed
            PickerError.CameraFailed -> s.docCameraFailed
            PickerError.MixedType -> if (BaseTheme.BaseUiLanguage == UiLanguage.Arabic)
                "هذا الحقل يحتوي بالفعل على ملف${if (state.kind != null) " من نوع ${state.kind!!.name.lowercase()}" else ""}. " +
                        "قم بإزالته أولاً إذا كنت تريد رفع نوع ملف مختلف هنا."
            else
                "This slot already contains ${state.kind?.name?.lowercase() ?: "a"} file(s). " +
                        "Remove them first if you want to upload a different file type here."
        }
        state.showError -> when (state.validationError()) {
            DocError.UploadFailed -> s.docFixFailed
            DocError.Uploading -> s.docWaitForUpload
            DocError.Required -> s.docRequired
            DocError.MinCount -> s.docMinCount(state.minItems)
            null -> null
        }
        else -> null
    }

    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // card left/right margin
            .background(BaseTheme.FieldColor, shape)
            .border(1.dp, if (errorText != null) red else accent.copy(alpha = 0.2f), shape)
            .padding(horizontal = 20.dp, vertical = 16.dp) // card inner left/right padding
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = doc.documentTitle.orEmpty(),
                fontFamily = InterFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = BaseTheme.BaseTextColor,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (doc.mandatory == true) {
                Text(
                    text = " *",
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = red
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.maxItems > 1) {
                Text(
                    text = "${state.items.size}/${state.maxItems}",
                    fontFamily = InterFont,
                    fontSize = 12.sp,
                    color = BaseTheme.BaseSecondaryTextColor
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Captured / uploaded items
        state.items.forEach { item ->
            CapturedItemRow(
                item = item,
                accent = accent,
                onRemove = { onRemove(item) },
                onRetry = { onRetry(item) }
            )
        }

        // Actions
        // Locked while a previous item is still uploading, in addition to being at capacity.
        val canAdd = state.canAddMore && state.items.none { it.status == UploadStatus.Uploading }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp), // buttons left/right padding
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (doc.allowLiveCapture == true) {
                ActionButton(
                    label = s.docTakePicture,
                    icon = Icons.Filled.CameraAlt,
                    enabled = canAdd,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                ) { launchCamera() }
            }
            if (doc.allowFileUpload == true) {
                ActionButton(
                    label = s.docUploadFile,
                    icon = Icons.Filled.UploadFile,
                    enabled = canAdd,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.pickerError = null
                    filePicker.launch(arrayOf("*/*"))
                }
            }
        }

        // Hint for file uploads
        if (doc.allowFileUpload == true) {
            val formats = doc.allowedFormats.orEmpty()
            val hint = buildList {
                if (formats.isNotEmpty()) add(formats.joinToString(", ") { it.uppercase() })
                doc.maxFileSize?.takeIf { it > 0 }?.let { add("max $it MB") }
            }.joinToString(" · ")
            if (hint.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = hint,
                    fontFamily = InterFont,
                    fontSize = 11.sp,
                    color = BaseTheme.BaseSecondaryTextColor
                )
            }
        }

        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorText,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = red
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 1f else 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BaseTheme.BaseTextColor,
            disabledContentColor = BaseTheme.BaseTextColor.copy(alpha = 0.4f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontFamily = InterFont,
            fontWeight = BaseTheme.BaseClickFontWeight,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CapturedItemRow(
    item: CapturedItem,
    accent: Color,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    val s = flowStrings()
    val thumbShape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(BaseTheme.BaseTextColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isImage) {
            AsyncImage(
                model = item.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(thumbShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.15f), thumbShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.extension.uppercase().take(4).ifEmpty { "FILE" },
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = BaseTheme.BaseTextColor
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = BaseTheme.BaseTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when (item.status) {
                UploadStatus.Uploading -> Text(
                    text = s.docUploading,
                    fontFamily = InterFont,
                    fontSize = 11.sp,
                    color = BaseTheme.BaseSecondaryTextColor
                )
                UploadStatus.Uploaded -> Text(
                    text = s.docUploaded,
                    fontFamily = InterFont,
                    fontSize = 11.sp,
                    color = BaseTheme.BaseGreenColor
                )
                UploadStatus.Failed -> Text(
                    text = s.docUploadFailed,
                    fontFamily = InterFont,
                    fontSize = 11.sp,
                    color = BaseTheme.BaseRedColor
                )
            }
        }

        when (item.status) {
            UploadStatus.Uploading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = accent
            )
            UploadStatus.Failed -> IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry",
                    tint = BaseTheme.BaseRedColor
                )
            }
            UploadStatus.Uploaded -> Unit
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove",
                tint = BaseTheme.BaseTextColor.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────── File helpers ───────────────────────────

private sealed interface PickResult {
    data class Ok(val file: File, val name: String, val mime: String) : PickResult
    data class Error(val error: PickerError) : PickResult
}

private fun normalizeFormats(allowed: List<String>?): List<String> =
    allowed.orEmpty().map { it.trim().lowercase().removePrefix(".") }.filter { it.isNotEmpty() }

/**
 * Some devices/OEM builds have an incomplete android.webkit.MimeTypeMap table and can fail to
 * resolve even common extensions (pdf included), silently dropping them from the document
 * picker's allowed mime types. Resolve known extensions ourselves first, and only fall back to
 * the system table for anything unusual.
 */
private val KNOWN_MIME_TYPES = mapOf(
    "pdf" to "application/pdf",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "webp" to "image/webp",
    "gif" to "image/gif",
    "bmp" to "image/bmp",
    "tif" to "image/tiff",
    "tiff" to "image/tiff",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
)

private fun mimeTypeForExtension(ext: String): String? =
    KNOWN_MIME_TYPES[ext] ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)

private fun pickerMimeTypes(allowed: List<String>?): Array<String> {
    val types = normalizeFormats(allowed).mapNotNull { a ->
        if (a.contains("/")) a else mimeTypeForExtension(a)
    }.distinct()
    return if (types.isEmpty()) arrayOf("*/*") else types.toTypedArray()
}

private fun isFormatAllowed(name: String, mime: String, allowed: List<String>?): Boolean {
    val list = normalizeFormats(allowed)
    if (list.isEmpty()) return true
    val ext = name.substringAfterLast('.', "").lowercase()
    val m = mime.lowercase()
    return list.any { a ->
        when {
            a.endsWith("/*") -> m.startsWith(a.removeSuffix("*"))
            a.contains("/") -> a == m
            a == "jpg" || a == "jpeg" -> ext == "jpg" || ext == "jpeg"
            else -> ext == a
        }
    }
}

private suspend fun readPickedFile(
    context: Context,
    uri: Uri,
    doc: DocumentCaptures,
): PickResult = withContext(Dispatchers.IO) {
    try {
        var name = "document_${System.currentTimeMillis()}"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (n >= 0) c.getString(n)?.let { name = it }
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
            }
        }

        val ext = name.substringAfterLast('.', "").lowercase()
        // Some document providers report a generic/wrong type for a file (e.g. PDFs as
        // "application/octet-stream" / "BIN file"). Trust the resolver only when it gives us
        // something specific; otherwise derive the type from the file's extension, which is
        // what actually reflects the file's real format here.
        val resolverMime = context.contentResolver.getType(uri)
        val mime = resolverMime
            ?.takeIf { it != "application/octet-stream" && it != "*/*" }
            ?: mimeTypeForExtension(ext)
            ?: resolverMime
            ?: "application/octet-stream"

        if (!isFormatAllowed(name, mime, doc.allowedFormats)) {
            return@withContext PickResult.Error(PickerError.FormatNotAllowed)
        }

        val maxSize = doc.maxFileSize?.takeIf { it > 0 }?.let { it * BYTES_PER_MAX_FILE_SIZE_UNIT }
        if (maxSize != null && size > maxSize) {
            return@withContext PickResult.Error(PickerError.FileTooLarge)
        }

        val dir = File(context.cacheDir, "document_capture").apply { mkdirs() }
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(dir, "${System.currentTimeMillis()}_$safeName")

        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        if (!copied) return@withContext PickResult.Error(PickerError.ReadFailed)

        // size may have been unknown up-front
        if (maxSize != null && out.length() > maxSize) {
            out.delete()
            return@withContext PickResult.Error(PickerError.FileTooLarge)
        }

        PickResult.Ok(out, name, mime)
    } catch (e: Exception) {
        PickResult.Error(PickerError.ReadFailed)
    }
}