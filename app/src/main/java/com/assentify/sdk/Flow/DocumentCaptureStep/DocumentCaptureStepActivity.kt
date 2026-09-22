package com.assentify.sdk.Flow.DocumentCaptureStep

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.DocumentCapture.DocumentCapture
import com.assentify.sdk.DocumentCapture.DocumentCaptureCallback
import com.assentify.sdk.DocumentCapture.DocumentCaptureModel
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.BaseClick
import com.assentify.sdk.Flow.ReusableComposable.Events.DocumentCaptureStepEventTypes
import com.assentify.sdk.Flow.ReusableComposable.LogoSvgUrl
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper.ProgressStepper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


class DocumentCaptureStepActivity : FragmentActivity(), DocumentCaptureCallback {

    private var documentCaptureStepEventTypes =
        mutableStateOf<String>(DocumentCaptureStepEventTypes.onSend)

    private var documentCaptureObject = mutableStateOf<DocumentCaptureModel?>(null)
    private lateinit var documentCapture: DocumentCapture

    private var timeStarted = getCurrentDateTimeForTracking()

    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    private var isNavigating = false

    /** One UI state per DocumentCaptures entry, keyed by DocumentCaptures.stateKey. */
    private val itemStates = mutableMapOf<String, DocumentItemState>()

    /** Final output of this step, handed to makeCurrentStepDone. */
    private val resultMap = mutableMapOf<String, String>()

    /** In-flight combine+upload coroutine per slot, so a rapid add/remove cancels the stale one. */
    private val mergeJobs = mutableMapOf<String, Job>()

    /** documentKey -> the synthetic itemId used for that slot's current merged (combined) upload, if any. */
    private val pendingMergeIds = mutableMapOf<String, String>()

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            File(cacheDir, "document_capture").deleteRecursively()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = null,
            inputData = FlowController.outputPropertiesToMap(currentStep.stepDefinition!!.outputProperties),
            status = "InProgress"
        )

        documentCapture = assentifySdk.startDocumentCapture(
            this,
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@DocumentCaptureStepActivity)
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DocumentCaptureStepScreen(
                        onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onNext = {
                            if (!isNavigating && validateAll()) {
                                isNavigating = true

                                FlowController.makeCurrentStepDone(resultMap.toMap(), timeStarted)
                                FlowController.naveToNextStep(this)
                            }
                        },
                        eventTypes = documentCaptureStepEventTypes.value,
                        documentCaptureObject = documentCaptureObject.value
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DocumentCaptureStepActivity::class.java)
            context.startActivity(intent)
        }
    }

    // ───────────── SDK callbacks ─────────────

    override fun onDocumentCaptureCallbackError(message: String) {
        documentCaptureStepEventTypes.value = DocumentCaptureStepEventTypes.onError
    }

    override fun onDocumentCaptureCallbackSuccess(documentCaptureModel: DocumentCaptureModel) {
        documentCaptureObject.value = documentCaptureModel
        documentCaptureStepEventTypes.value = DocumentCaptureStepEventTypes.onComplete
    }

    override fun onUploadDocumentCaptureCallbackSuccess(
        documentKey: String,
        itemId: String,
        data: Map<String, String>,
    ) {
        val state = itemStates[documentKey] ?: return

        if (pendingMergeIds[documentKey] == itemId) {
            // This upload represents the whole slot (combined images / concatenated PDF).
            // Fan the result out to every item currently displayed in the slot.
            val now = getCurrentDateTimeForTracking().toString()
            state.items.forEach { item ->
                item.resultData = data
                item.uploadedAt = now
                item.status = UploadStatus.Uploaded
            }
        } else {
            val item = state.items.firstOrNull { it.id == itemId } ?: return // removed while uploading
            item.resultData = data
            item.uploadedAt = getCurrentDateTimeForTracking().toString()
            item.status = UploadStatus.Uploaded
        }

        rebuildResult()
    }

    override fun onUploadDocumentCaptureCallbackError(
        documentKey: String,
        itemId: String,
        message: String,
    ) {
        val state = itemStates[documentKey] ?: return
        if (pendingMergeIds[documentKey] == itemId) {
            state.items.forEach { it.status = UploadStatus.Failed }
        } else {
            state.items.firstOrNull { it.id == itemId }?.status = UploadStatus.Failed
        }
    }

    // ───────────── Item actions ─────────────

    private fun stateFor(doc: com.assentify.sdk.DocumentCapture.DocumentCaptures): DocumentItemState =
        itemStates.getOrPut(doc.stateKey) { DocumentItemState(doc) }

    private fun addAndUpload(state: DocumentItemState, file: File, name: String, mime: String) {
        if (!state.canAddMore) {
            file.delete()
            return
        }

        // "images and PDFs" in the same slot are refused — user must clear the slot and pick one kind.
        val existingKind = state.kind
        val newKind = kindOf(mime)
        if (existingKind != null && existingKind != newKind) {
            file.delete()
            state.pickerError = PickerError.MixedType
            return
        }

        state.pickerError = null
        val item = CapturedItem(UUID.randomUUID().toString(), file, name, mime)
        state.items.add(item)
        reprocessSlot(state)
    }

    private fun retryUpload(state: DocumentItemState, item: CapturedItem) {
        // Retrying re-derives the whole slot's result (single/combined) rather than just this file,
        // since a multi-image or multi-PDF slot's uploaded artifact is shared across its items.
        reprocessSlot(state)
    }

    private fun removeItem(state: DocumentItemState, item: CapturedItem) {
        state.items.remove(item)
        item.file.delete()
        reprocessSlot(state)
    }

    /**
     * Re-derives what should be uploaded for a slot, per the "slot contains → result" rules:
     *  - empty                    -> nothing to upload
     *  - one file                 -> uploaded untouched
     *  - several images           -> stacked vertically, 10px gutter, PNG @ 0.95
     *  - several PDFs             -> concatenated into one multi-page PDF, order preserved
     *  - several "other" files    -> uploaded individually, untouched (not covered by the table)
     *
     * Mixed image+PDF slots never reach this function: addAndUpload refuses the add before it happens.
     */
    private fun reprocessSlot(state: DocumentItemState) {
        val key = state.doc.stateKey
        mergeJobs.remove(key)?.cancel()

        val items = state.items
        if (items.isEmpty()) {
            pendingMergeIds.remove(key)
            rebuildResult()
            return
        }

        val kind = kindOf(items.first().mimeType)

        // Reset visible state, then decide how to (re)upload.
        items.forEach {
            it.status = UploadStatus.Uploading
            it.resultData = emptyMap()
            it.uploadedAt = null
        }
        rebuildResult()

        if (items.size == 1 || kind == SlotKind.OTHER) {
            // One file, or a kind the table doesn't ask us to combine: upload each item as-is.
            pendingMergeIds.remove(key)
            items.forEach { item ->
                documentCapture.uploadDocument(item.file, item.mimeType, key, item.id)
            }
            return
        }

        // Several images, or several PDFs: build one combined artifact and upload it once.
        val job = lifecycleScope.launch {
            try {
                val outDir = File(cacheDir, "document_capture").apply { mkdirs() }
                val timestamp = System.currentTimeMillis()
                val combinedFile: File
                val combinedMime: String

                when (kind) {
                    SlotKind.IMAGE -> {
                        combinedFile = withContext(Dispatchers.IO) {
                            combineImagesVertically(
                                files = items.map { it.file },
                                out = File(outDir, "combined_${key}_$timestamp.png")
                            )
                        }
                        combinedMime = "image/png"
                    }
                    SlotKind.PDF -> {
                        combinedFile = withContext(Dispatchers.IO) {
                            concatenatePdfs(
                                files = items.map { it.file },
                                out = File(outDir, "combined_${key}_$timestamp.pdf")
                            )
                        }
                        combinedMime = "application/pdf"
                    }
                    SlotKind.OTHER -> return@launch // unreachable, guarded above
                }

                val mergeId = "merge_${key}_$timestamp"
                pendingMergeIds[key] = mergeId
                documentCapture.uploadDocument(combinedFile, combinedMime, key, mergeId)
            } catch (e: Exception) {
                items.forEach { it.status = UploadStatus.Failed }
            }
        }
        mergeJobs[key] = job
    }

    /**
     * Several images -> stacked vertically on one canvas, 10px gutter between them,
     * written out as PNG at quality 0.95.
     */
    private fun combineImagesVertically(files: List<File>, out: File): File {
        val gutterPx = 10
        val quality95 = 95 // 0.95 as an Android compress-quality int (0-100)

        val bitmaps = files.map { f ->
            BitmapFactory.decodeFile(f.absolutePath)
                ?: error("Could not decode image: ${f.name}")
        }

        val width = bitmaps.maxOf { it.width }
        val height = bitmaps.sumOf { it.height } + gutterPx * (bitmaps.size - 1)

        val canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(canvasBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        var y = 0
        bitmaps.forEach { bmp ->
            val x = (width - bmp.width) / 2
            canvas.drawBitmap(bmp, x.toFloat(), y.toFloat(), null)
            y += bmp.height + gutterPx
            bmp.recycle()
        }

        out.outputStream().use { stream ->
            canvasBitmap.compress(Bitmap.CompressFormat.PNG, quality95, stream)
        }
        canvasBitmap.recycle()
        return out
    }

    /**
     * Several PDFs -> concatenated into one multi-page PDF, page order preserved (source file order,
     * then page order within each source file). Pages are rasterized via PdfRenderer and redrawn into
     * a new PdfDocument, so no external PDF library is required.
     */
    private fun concatenatePdfs(files: List<File>, out: File): File {
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        files.forEach { srcFile ->
            ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = PdfRenderer(pfd)
                try {
                    for (i in 0 until renderer.pageCount) {
                        val srcPage = renderer.openPage(i)
                        val pageInfo = PdfDocument.PageInfo
                            .Builder(srcPage.width, srcPage.height, pageNumber)
                            .create()
                        val page = pdfDocument.startPage(pageInfo)

                        val bitmap = Bitmap.createBitmap(srcPage.width, srcPage.height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        srcPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)

                        bitmap.recycle()
                        srcPage.close()
                        pageNumber++
                    }
                } finally {
                    renderer.close()
                }
            }
        }

        out.outputStream().use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return out
    }

    /**
     * Rebuilds the result map from everything currently uploaded, so removing an item
     * also removes its values.
     */
    private fun rebuildResult() {
        resultMap.clear()
        itemStates.values.forEach { st ->
            val uploaded = st.items.filter { it.status == UploadStatus.Uploaded }
            if (uploaded.isEmpty()) return@forEach

            uploaded.forEach { resultMap.putAll(it.resultData) }
        }
    }

    private fun validateAll(): Boolean {
        var valid = true
        documentCaptureObject.value?.documentCaptures?.forEach { doc ->
            val st = stateFor(doc)
            st.showError = true
            if (st.validationError() != null) valid = false
        }
        return valid
    }

    // ───────────── UI ─────────────

    @Composable
    fun DocumentCaptureStepScreen(
        onBack: () -> Unit = {},
        onNext: () -> Unit = {},
        eventTypes: String,
        documentCaptureObject: DocumentCaptureModel?,
    ) {

        val s = flowStrings()

        BaseBackgroundContainer(
            modifier = Modifier.fillMaxSize()
        ) {
            val model = documentCaptureObject ?: return@BaseBackgroundContainer

            Column(modifier = Modifier.fillMaxSize()) {

                // ───────────── TOP (fixed) ─────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (BaseTheme.StepperType == StepperType.Normal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = BaseTheme.BaseTextColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(BaseTheme.BaseLogo)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Logo",
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(Modifier.weight(1f))
                            Spacer(Modifier.size(48.dp)) // balances the back button
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    ProgressStepper(
                        onBack = { onBack() },
                        normalModifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        percentageBased = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    )
                }

                // ───────────── MIDDLE (scrollable) ─────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(10.dp))

                    val hasLogoHeader = !model.svgLogoUrl.isNullOrEmpty() &&
                            !model.header.isNullOrEmpty() &&
                            !model.subHeader.isNullOrEmpty()

                    if (hasLogoHeader) {
                        LogoSvgUrl(
                            url = model.svgLogoUrl!!,
                            modifier = Modifier.size(width = 70.dp, height = 70.dp)
                        )
                        Text(
                            text = model.header!!,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            color = BaseTheme.BaseTextColor,
                            fontSize = 23.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                        )
                        Text(
                            text = model.subHeader!!,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Medium,
                            color = BaseTheme.BaseTextColor.copy(0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp, start = 20.dp, end = 20.dp)
                        )
                    } else if (!model.header.isNullOrEmpty()) {
                        Text(
                            text = model.header,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            color = BaseTheme.BaseTextColor,
                            fontSize = 23.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    if (eventTypes == DocumentCaptureStepEventTypes.onComplete) {
                        model.documentCaptures?.forEach { doc ->
                            val state = stateFor(doc)
                            DocumentCaptureItem(
                                state = state,
                                onFileReady = { file, name, mime ->
                                    addAndUpload(state, file, name, mime)
                                },
                                onRemove = { item -> removeItem(state, item) },
                                onRetry = { item -> retryUpload(state, item) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // ───────────── BOTTOM (fixed) ─────────────
                BaseClick(
                    isNormalClick = model.isNormalClick ?: true,
                    label = model.nextButtonTitle ?: s.next,
                    isActive = validateAll(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 25.dp, horizontal = 25.dp)
                        .background(
                            brush = if (validateAll())
                                BaseTheme.BaseClickColor!!.toBrush()
                            else
                                SolidColor(BaseTheme.FieldColor),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    sliderModifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 25.dp, horizontal = 25.dp),
                    onNext = { onNext() }
                )
            }
        }
    }
}