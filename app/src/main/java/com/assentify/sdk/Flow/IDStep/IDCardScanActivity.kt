package com.assentify.sdk.Flow.IDStep

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.IdLivenessInstruction
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Core.Constants.UiLanguage
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.QrStep.HowToCaptureQrActivity
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnErrorScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnFlipCardScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnLivenessScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnNormalCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnSendScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnWrongTemplateScreen
import com.assentify.sdk.Flow.ReusableComposable.HoldCardInHandPopup
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.Models.getImageUrlFromBaseResponseDataModel
import com.assentify.sdk.OnCompleteScreenData
import com.assentify.sdk.QrIDResponseModelObject
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanIDCard.ScanIDCardManual
import com.assentify.sdk.ScanIDCard.ScanIDCardResult
import com.assentify.sdk.SelectedTemplatesObject


class IDCardScanActivity : FragmentActivity(), IDCardCallback {

    private val cs by lazy { flowStrings() }
    private var start = mutableStateOf(false)
    private var isLastPageValue = mutableStateOf(false)
    private var isFrontPageValue = mutableStateOf(false)
    private var feedbackText = mutableStateOf("")
    private var uploadingProgress = mutableStateOf(0)
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var livenessImage = mutableStateOf<Bitmap?>(null)      // the local liveness's image (null = use imageUrl)
    private var classifiedTemplateValue = mutableStateOf<String>("")
    private var extractedInformation = mutableStateOf<Map<String, String>?>(null)

    // card liveness tilt meter (0..100), shown under the instruction while the card is tilted
    private var tiltProgress = mutableStateOf(0)
    private var tiltProgressVisible = mutableStateOf(false)

    // the local liveness owns the instruction text while it runs
    private var livenessActive = false

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    private var timeStarted = getCurrentDateTimeForTracking()

    private var isNavigating = false
    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** Track Progress **/
        val currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = null,
            inputData = FlowController.outputPropertiesToMap(currentStep.stepDefinition!!.outputProperties),
            status = "InProgress"
        )
        /***/

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IDCardScanScreen(
                        activity = this@IDCardScanActivity,
                        onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onRetry = {
                            start.value = false;
                            feedbackText.value = "";
                            uploadingProgress.value = 0;
                            eventTypes.value = EventTypes.none;
                            imageUrl.value = "";
                            livenessImage.value = null
                            tiltProgress.value = 0
                            tiltProgressVisible.value = false
                            livenessActive = false
                        },
                        onFlip = {
                            start.value = false;
                            feedbackText.value = "";
                            uploadingProgress.value = 0;
                            eventTypes.value = EventTypes.none;
                            livenessImage.value = null
                            tiltProgress.value = 0
                            tiltProgressVisible.value = false
                            livenessActive = false
                        },
                        onNext = { hasQr ->
                            if (flowEnv.enableQr && hasQr) {
                                HowToCaptureQrActivity.start(
                                    context = this,
                                );
                            } else {
                                if (!isNavigating) {
                                    isNavigating = true
                                    FlowController.makeCurrentStepDone(extractedInformation.value!!, timeStarted);
                                    FlowController.naveToNextStep(context = this)
                                }
                            }
                        },
                        feedbackText = feedbackText.value,
                        tiltProgress = tiltProgress.value,
                        tiltProgressVisible = tiltProgressVisible.value,
                        imageUrl = imageUrl.value,
                        livenessImage = livenessImage.value,
                        progress = uploadingProgress.value,
                        eventTypes = eventTypes.value,
                        isLastPage = isLastPageValue.value,
                        classifiedTemplate = classifiedTemplateValue.value,
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, IDCardScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    /** The server's image URL from a response, or "" (the local liveness sends no response). */
    private fun imageUrlOf(dataModel: BaseResponseDataModel): String = try {
        getImageUrlFromBaseResponseDataModel(dataModel.response!!)
    } catch (e: Exception) {
        ""
    }

    /**  Events **/

    override fun onSend() {
        runOnUiThread {
            start.value = true;
            eventTypes.value = EventTypes.onSend
            tiltProgressVisible.value = false
        }
    }

    override fun onUploadingProgress(progress: Int) {
        runOnUiThread {
            uploadingProgress.value = progress;
        }
    }

    override fun onError(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            tiltProgressVisible.value = false
            livenessActive = false
            eventTypes.value = EventTypes.onError
            imageUrl.value = imageUrlOf(dataModel)
        }
        /** Track Progress **/
        val currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "Error" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response ?: "{}"),
            status = "InProgress"
        )
        /***/
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            tiltProgressVisible.value = false
            livenessActive = false
            eventTypes.value = EventTypes.onRetry
            imageUrl.value = imageUrlOf(dataModel)
        }
        /** Track Progress **/
        val currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "Retry" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response ?: "{}"),
            status = "InProgress"
        )
        /***/
    }

    override fun onComplete(
        dataModel: IDResponseModel,
        isFrontPage: Boolean,
        isLastPage: Boolean,
        classifiedTemplate: String
    ) {
        runOnUiThread {
            val currentMap = extractedInformation.value?.toMutableMap() ?: mutableMapOf()
            currentMap.putAll(
                dataModel.iDExtractedModel!!.transformedProperties!!.filterKeys {
                    it.isNotEmpty() && it !in currentMap
                }
            )
            extractedInformation.value = currentMap
            OnCompleteScreenData.clear();
            OnCompleteScreenData.setData(extractedInformation.value);
            start.value = false;
            tiltProgressVisible.value = false
            livenessActive = false
            eventTypes.value = EventTypes.onComplete
            isFrontPageValue.value = isFrontPage
            isLastPageValue.value = isLastPage
            classifiedTemplateValue.value = classifiedTemplate
            if (isFrontPage) {
                QrIDResponseModelObject.setQrIDResponseModelObject(dataModel)
                imageUrl.value = dataModel.iDExtractedModel!!.imageUrl.toString();
                dataModel.iDExtractedModel!!.outputProperties?.forEach { (key, value) ->
                    if (key.contains(FlowController.getFaceMatchInputImageKey())) {
                        FlowController.setImage(value.toString())
                    }
                }
            }
        }
        if (isLastPage) {
            /** Track Progress **/
            val currentStep = FlowController.getCurrentStep()
            FlowController.trackProgress(
                currentStep = currentStep!!,
                response = "Completed",
                inputData = extractedInformation.value,
                status = "InProgress"
            )
            /***/
        }
    }

    /**
     * The liveness failed. From the server: the image URL is in dataModel.response and image is null.
     * From the LOCAL card liveness (suspected copy / inconclusive / camera stalled): image is the card as the camera saw it.
     */
    override fun onLivenessUpdate(dataModel: BaseResponseDataModel, image: Bitmap?) {
        runOnUiThread {
            livenessActive = false
            start.value = false;
            tiltProgressVisible.value = false
            livenessImage.value = image
            imageUrl.value = if (image != null) "" else imageUrlOf(dataModel)
            eventTypes.value = EventTypes.onLivenessUpdate
        }
        /** Track Progress **/
        val currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "LivenessUpdate" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response ?: "{}"),
            status = "InProgress"
        )
        /***/
    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            tiltProgressVisible.value = false
            livenessActive = false
            eventTypes.value = EventTypes.onWrongTemplate
            imageUrl.value = imageUrlOf(dataModel)
        }
        /** Track Progress **/
        val currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "WrongTemplate" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response ?: "{}"),
            status = "InProgress"
        )
        /***/
    }


    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        zoom: ZoomType,
        isCentered: Boolean,
    ) {
        runOnUiThread {
            if (livenessActive) return@runOnUiThread                 // the local liveness owns the text
            if (start.value == false) {
                if (zoom != ZoomType.SENDING && zoom != ZoomType.NO_DETECT) {
                    if (zoom == ZoomType.ZOOM_IN) {
                        feedbackText.value = cs.moveIdCloser
                    }
                    if (zoom == ZoomType.ZOOM_OUT) {
                        feedbackText.value = cs.moveIdFurther
                    }
                } else if (motion != MotionType.SENDING && motion != MotionType.NO_DETECT) {
                    feedbackText.value = cs.holdYourHand

                } else if (brightnessEvents != BrightnessEvents.Good) {
                    if (brightnessEvents == BrightnessEvents.TooDark) {
                        feedbackText.value = cs.increaseLighting
                    }
                    if (brightnessEvents == BrightnessEvents.TooBright) {
                        feedbackText.value = cs.reduceLighting
                    }

                } else {
                    if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && brightnessEvents == BrightnessEvents.Good && isCentered) {
                        feedbackText.value = cs.holdSteady
                    }
                    if (motion == MotionType.NO_DETECT && zoom == ZoomType.NO_DETECT) {
                        feedbackText.value = cs.presentId
                    } else if (!isCentered) {
                        feedbackText.value = cs.centerCard
                    }
                }
            } else {
                feedbackText.value = ""
            }
        }
    }

    /** The local card liveness: what the user should do now, and the tilt meter (0..100). */
    override fun onLivenessInstruction(instruction: IdLivenessInstruction?, progress: Int) {
        if (instruction == null) {
            return
        }
        runOnUiThread {
            // the liveness owns the instruction text while it runs
            livenessActive = instruction != IdLivenessInstruction.KEEP_HOLDING
                    && instruction != IdLivenessInstruction.BARCODE_READ

            feedbackText.value = when (instruction) {
                // ---------- POSITION ----------
                IdLivenessInstruction.PLACE_CARD -> cs.idLivenessPlaceCard

                // ---------- FLAT HOLD ----------
                IdLivenessInstruction.KEEP_STEADY -> cs.livenessKeepSteady
                IdLivenessInstruction.KEEP_FLAT -> cs.idLivenessKeepFlat
                IdLivenessInstruction.ADJUST_POSITION -> cs.idLivenessReduceGlare

                // ---------- LIGHT ----------
                IdLivenessInstruction.DO_NOT_MOVE -> cs.idLivenessLightOn

                // ---------- TILT ----------
                IdLivenessInstruction.LIFT_RIGHT -> cs.idLivenessLiftRight
                IdLivenessInstruction.LIFT_TOP -> cs.idLivenessLiftTop
                IdLivenessInstruction.LIFT_BOTTOM -> cs.idLivenessLiftBottom
                IdLivenessInstruction.A_BIT_MORE -> cs.livenessMoveLittleMore
                IdLivenessInstruction.HOLD_THERE -> cs.idLivenessHoldThere
                IdLivenessInstruction.OTHER_EDGE -> cs.idLivenessOtherEdge
                IdLivenessInstruction.PICK_UP -> cs.idLivenessPickUp

                // ---------- DONE ----------
                IdLivenessInstruction.KEEP_HOLDING -> cs.livenessKeepHolding

                // ---------- ID BACK: the barcode ----------
                IdLivenessInstruction.READING_BARCODE -> cs.idLivenessReadingBarcode
                IdLivenessInstruction.BARCODE_READ -> cs.idLivenessBarcodeRead

                // ---------- PAUSES (the SDK continues by itself after a few seconds) ----------
                IdLivenessInstruction.HOLD_IN_HAND -> cs.idLivenessHoldInHand
                IdLivenessInstruction.PHONE_TURNED -> cs.idLivenessPhoneTurned
                IdLivenessInstruction.TOO_BRIGHT -> cs.livenessTooBright
            }

            // ---------- tilt meter (0..100) ----------
            tiltProgress.value = progress
            tiltProgressVisible.value = when (instruction) {
                IdLivenessInstruction.LIFT_RIGHT,
                IdLivenessInstruction.LIFT_TOP,
                IdLivenessInstruction.LIFT_BOTTOM,
                IdLivenessInstruction.A_BIT_MORE,
                IdLivenessInstruction.HOLD_THERE,
                IdLivenessInstruction.OTHER_EDGE,
                IdLivenessInstruction.PICK_UP,
                IdLivenessInstruction.KEEP_HOLDING -> true           // shows 100% at the end
                else -> false                                         // hidden before the tilt and in the pauses
            }
        }
    }

}

@Composable
fun IDCardScanScreen(
    activity: IDCardScanActivity,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNext: (hasQr: Boolean) -> Unit =
        { _ -> },
    onFlip: () -> Unit = {},
    feedbackText: String,
    tiltProgress: Int,
    tiltProgressVisible: Boolean,
    imageUrl: String,
    livenessImage: Bitmap?,
    progress: Int,
    eventTypes: String,
    isLastPage: Boolean,
    classifiedTemplate: String,
) {

    var isManual by remember { mutableStateOf<Boolean>(false) }
    var kycDocumentDetails by remember { mutableStateOf<List<KycDocumentDetails>>(emptyList()) }
    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val selectedTemplate = SelectedTemplatesObject.getSelectedTemplatesObject()

    val templatesByCountry = remember {
        AssentifySdkObject.getAssentifySdkObject().getTemplates(
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId
        ).first {
            it.sourceCountryCode == selectedTemplate.sourceCountryCode
        }
    }

    if (classifiedTemplate.isNotEmpty()) {
        for (it in templatesByCountry.templates) {
            for (it1 in it.kycDocumentDetails) {
                if (it1.templateProcessingKeyInformation == classifiedTemplate) {
                    kycDocumentDetails = it.kycDocumentDetails;
                }
            }
        }
    }


    var scanID by remember { mutableStateOf<ScanIDCard?>(null) }
    var scanIDManual by remember { mutableStateOf<ScanIDCardManual?>(null) }

    // "Hold the card in your hand": shown on top of the camera when the scan opens, until Continue
    var showHoldPopup by remember { mutableStateOf(true) }

    val layoutDirection = if (BaseTheme.BaseUiLanguage == UiLanguage.Arabic)
        LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (eventTypes != EventTypes.none) {
                if (eventTypes == EventTypes.onSend) {
                    OnSendScreen(progress)
                }
                if (eventTypes == EventTypes.onRetry || eventTypes == EventTypes.onError) {
                    OnErrorScreen(imageUrl, onRetry = {
                        if (!isManual) scanID?.retryLiveness()      // the local liveness starts again from Position
                        onRetry();
                    })
                }
                if (eventTypes == EventTypes.onWrongTemplate) {
                    OnWrongTemplateScreen(
                        imageUrl,
                        expectedImageUrl = "",
                        onRetry = {
                            if (!isManual) scanID?.retryLiveness()
                            onRetry();
                        })
                }
                if (eventTypes == EventTypes.onLivenessUpdate) {
                    OnLivenessScreen(
                        imageUrl = imageUrl,
                        imageBitmap = livenessImage,                // null (server) -> the URL is shown
                        onRetry = {
                            if (!isManual) scanID?.retryLiveness()  // a stopped local liveness starts again
                            onRetry();
                        })
                }
                if (eventTypes == EventTypes.onComplete) {
                    if (isLastPage) {
                        val showResultPage = FlowController.getCurrentStep()!!.stepDefinition!!.customization.showResultPage
                            ?: false;

                        if (flowEnv.enableQr && kycDocumentDetails.first { it.templateProcessingKeyInformation == classifiedTemplate }.hasQrCode) {
                            OnNormalCompleteScreen(imageUrl, onNext = {
                                if (isManual) {
                                    scanIDManual?.stopScanning()
                                } else {
                                    scanID?.stopScanning()
                                }
                                onNext(
                                    kycDocumentDetails.first { it.templateProcessingKeyInformation == classifiedTemplate }.hasQrCode,
                                );
                            })
                        } else {
                            if (showResultPage) {
                                OnCompleteScreen(imageUrl, onNext = {
                                    if (isManual) {
                                        scanIDManual?.stopScanning()
                                    } else {
                                        scanID?.stopScanning()
                                    }
                                    onNext(
                                        kycDocumentDetails.first { it.templateProcessingKeyInformation == classifiedTemplate }.hasQrCode,
                                    );
                                })

                            } else {
                                OnNormalCompleteScreen(imageUrl, onNext = {
                                    if (isManual) {
                                        scanIDManual?.stopScanning()
                                    } else {
                                        scanID?.stopScanning()
                                    }
                                    onNext(
                                        kycDocumentDetails.first { it.templateProcessingKeyInformation == classifiedTemplate }.hasQrCode,
                                    );
                                })
                            }
                        }

                    } else {
                        OnFlipCardScreen(
                            kycDocumentDetails.first { it.templateProcessingKeyInformation != classifiedTemplate }.templateSpecimen,
                            onNext = {
                                if (isManual) {
                                    scanIDManual?.changeTemplateId()
                                } else {
                                    scanID?.changeTemplateId()      // the back: the classifier and the liveness start again
                                }
                                onFlip();
                            })
                    }
                }


            }
            AndroidView(
                modifier = if (eventTypes != EventTypes.none) Modifier.size(0.dp) else Modifier.fillMaxSize(),
                factory = { context ->
                    val container = FrameLayout(context).apply {
                        id = View.generateViewId()
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val result = assentifySdk.startScanIDCard(
                        activity,
                        templatesByCountry = templatesByCountry,
                        flowEnv.extractedDataLanguage,
                        stepId = FlowController.getCurrentStep()!!.stepDefinition!!.stepId
                    )

                    val fm = activity.supportFragmentManager
                    val tx = fm.beginTransaction()
                    when (result) {
                        is ScanIDCardResult.Manual -> {
                            isManual = true;
                            scanIDManual = result.data
                            tx.replace(container.id, result.data)
                        }

                        is ScanIDCardResult.Auto -> {
                            isManual = false;
                            scanID = result.data
                            tx.replace(container.id, result.data)
                        }
                    }
                    tx.commitAllowingStateLoss()
                    container
                },
            )

            DisposableEffect(isManual, scanID, scanIDManual) {
                onDispose {
                    try {
                        if (isManual) {
                            scanIDManual?.stopScanning()
                        } else {
                            scanID?.stopScanning()
                        }
                    } catch (_: Exception) { /* ignore */
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (eventTypes == EventTypes.none || BaseTheme.StepperType == StepperType.Normal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isManual) {
                                scanIDManual?.stopScanning()
                            } else {
                                scanID?.stopScanning()
                            }
                            onBack()
                        }) {
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
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.size(48.dp))
                    }
                }


                Spacer(Modifier.height(10.dp))

                if (eventTypes != EventTypes.none) {
                    ProgressStepper(
                        onBack = { onBack() },
                        normalModifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        percentageBased = Modifier
                            .fillMaxWidth().padding(horizontal = 0.dp).padding(top = 20.dp)
                    )
                }
            }

            if (eventTypes == EventTypes.none) {
                if (isManual) {
                    Button(
                        onClick = {
                            scanIDManual!!.takePicture();
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(vertical = 25.dp, horizontal = 25.dp)
                            .fillMaxWidth().background(
                                brush = BaseTheme.BaseClickColor!!.toBrush(),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Text(
                            flowStrings().takePhoto,
                            fontFamily = InterFont,
                            fontWeight = BaseTheme.BaseClickFontWeight,
                            color = BaseTheme.BaseSecondaryTextColor,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                } else if (!showHoldPopup) {                // the instructions start once the popup is closed
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .align(Alignment.BottomCenter)
                            .padding(start = 32.dp, end = 32.dp, bottom = 25.dp)
                    ) {
                        IdLivenessFeedback(
                            text = feedbackText,
                            tiltProgress = tiltProgress,
                            tiltProgressVisible = tiltProgressVisible
                        )
                    }
                }
            }

            // ---------- on top of everything: hold the card in your hand (auto scan only) ----------
            if (showHoldPopup && eventTypes == EventTypes.none && !isManual) {
                HoldCardInHandPopup(
                    title = flowStrings().holdCardTitle,
                    message = flowStrings().holdCardMsg,
                    buttonText = flowStrings().continueButton,
                    onContinue = {
                        showHoldPopup = false               // only closes the popup: the scan goes on as it is
                    })
            }

        }
    }
}

/**
 * The instruction in a rounded container (FieldColor background, BaseTextColor text), with the tilt meter
 * (0..100%) underneath while the card is tilted. Same design as the passport screens' LivenessFeedback.
 */
@Composable
private fun IdLivenessFeedback(
    text: String,
    tiltProgress: Int,
    tiltProgressVisible: Boolean,
) {
    val visible = text.isNotEmpty() || tiltProgressVisible
    val accent = remember(BaseTheme.BaseAccentColor) {
        Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
    }
    val percent = tiltProgress.coerceIn(0, 100)
    val done = percent >= 100

    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "tiltProgress"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 3 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 3 },
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(BaseTheme.FieldColor)
                .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .animateContentSize(tween(220)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---------- instruction ----------
            Crossfade(targetState = text, animationSpec = tween(220), label = "feedbackText") { current ->
                if (current.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IdPulsingDot(color = accent, active = !done)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            current,
                            color = BaseTheme.BaseTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // ---------- tilt meter ----------
            AnimatedVisibility(
                visible = tiltProgressVisible,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animated)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.65f), accent)))
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (done) flowStrings().livenessDone else flowStrings().livenessTiltProgress,
                            color = BaseTheme.BaseTextColor.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (done) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                            }
                            Text(
                                "$percent%",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A small dot that gently pulses while the user still has something to do. */
@Composable
private fun IdPulsingDot(color: Color, active: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.6f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (active) 0.35f else 0f }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}