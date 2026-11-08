package com.assentify.sdk.Flow.IDStep

import android.content.Context
import android.content.Intent
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
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.PassportLivenessInstruction
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Core.Constants.UiLanguage
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.NfcStep.NfcScanActivity
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnErrorScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnLivenessScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnNormalCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnPassportExpired
import com.assentify.sdk.Flow.ReusableComposable.Events.OnSendScreen
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.NfcPassportResponseModelObject
import com.assentify.sdk.OnCompleteScreenData
import com.assentify.sdk.ScanPassport.LocalScanPassport
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassportCallback


class LocalPassportScanActivity : FragmentActivity(), ScanPassportCallback {

    private val cs by lazy { flowStrings() }
    private var start = mutableStateOf(false)
    private var feedbackText = mutableStateOf("")
    private var uploadingProgress = mutableStateOf(0)
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var dataIDModel = mutableStateOf<PassportResponseModel?>(null)

    // liveness tilt meter (0..100), shown under the feedback text while the passport is tilted
    private var tiltProgress = mutableStateOf(0)
    private var tiltProgressVisible = mutableStateOf(false)

    private var timeStarted = getCurrentDateTimeForTracking()
    private var isNavigating = false

    // the liveness owns the instruction text while it runs
    private var livenessActive = false

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

        /* onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
             override fun handleOnBackPressed() {
                 FlowController.backClick(this@LocalPassportScanActivity);
             }
         })*/

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PassportScanScreen(
                        activity = this@LocalPassportScanActivity, onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onRetry = {
                            start.value = false;
                            feedbackText.value = "";
                            uploadingProgress.value = 0;
                            eventTypes.value = EventTypes.none;
                            imageUrl.value = ""
                            tiltProgress.value = 0
                            tiltProgressVisible.value = false
                        },
                        onNext = {
                            if (flowEnv.enableNfc) {
                                NfcScanActivity.start(context = this)
                            } else {

                                if (!isNavigating) {
                                    isNavigating = true

                                    FlowController.makeCurrentStepDone(
                                        dataIDModel.value!!.passportExtractedModel!!.transformedProperties!!,
                                        timeStarted
                                    );
                                    FlowController.naveToNextStep(this)
                                }


                            }
                        },
                        feedbackText = feedbackText.value,
                        tiltProgress = tiltProgress.value,
                        tiltProgressVisible = tiltProgressVisible.value,
                        imageUrl = imageUrl.value,
                        progress = uploadingProgress.value,
                        eventTypes = eventTypes.value
                    )
                }
            }
        }
    }


    companion object {

        fun start(context: Context) {
            val intent = Intent(context, LocalPassportScanActivity::class.java)
            context.startActivity(intent)
        }
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
            uploadingProgress.value = 100;
        }
    }

    override fun onError(dataModel: BaseResponseDataModel) {
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        if (dataModel.error == "Expired Passport Detected") {
            runOnUiThread {
                start.value = false;
                tiltProgressVisible.value = false
                eventTypes.value = EventTypes.onExpired
            }
        } else {
            runOnUiThread {
                start.value = false;
                tiltProgressVisible.value = false
                eventTypes.value = EventTypes.onRetry
            }
        }

    }

    /** The local liveness failed (suspected copy / inconclusive / camera stalled): the liveness screen. */
    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            livenessActive = false
            start.value = false
            tiltProgressVisible.value = false
            imageUrl.value = ""                                       // the local liveness has no server image
            eventTypes.value = EventTypes.onLivenessUpdate
        }
    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
    }

    override fun onComplete(dataModel: PassportResponseModel) {
        runOnUiThread {
            OnCompleteScreenData.clear();
            OnCompleteScreenData.setData(dataModel.passportExtractedModel!!.transformedProperties);
            NfcPassportResponseModelObject.setPassportResponseModelObject(dataModel)
            dataIDModel.value = dataModel;
            start.value = false;
            tiltProgressVisible.value = false
            eventTypes.value = EventTypes.onComplete
            imageUrl.value = dataModel.passportExtractedModel!!.imageUrl!!
            dataModel.passportExtractedModel!!.outputProperties?.forEach { (key, value) ->
                if (key.contains(FlowController.getFaceMatchInputImageKey())) {
                    FlowController.setImage(value.toString())
                }
            }
        }
    }

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        zoom: ZoomType,
        isCentered: Boolean,
    ) {
        runOnUiThread {
            if (livenessActive) return@runOnUiThread                 // the liveness owns the text
            if (start.value == false) {
                if (zoom != ZoomType.SENDING && zoom != ZoomType.NO_DETECT) {
                    if (zoom == ZoomType.ZOOM_IN) {
                        feedbackText.value = cs.movePassportCloser
                    }
                    if (zoom == ZoomType.ZOOM_OUT) {
                        feedbackText.value = cs.movePassportFurther
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
                    if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && brightnessEvents == BrightnessEvents.Good) {
                        feedbackText.value = cs.holdSteady
                    }
                    if (motion == MotionType.NO_DETECT && zoom == ZoomType.NO_DETECT) {
                        feedbackText.value = cs.presentPassport
                    } else if (!isCentered) {
                        feedbackText.value = cs.centerCard
                    }
                }
            } else {
                feedbackText.value = ""
            }
        }
    }

    override fun onLivenessInstruction(instruction: PassportLivenessInstruction?, progress: Int) {
        if (instruction == null) {
            return
        }

        // the liveness owns the instruction text while it runs
        livenessActive = instruction != PassportLivenessInstruction.KEEP_HOLDING

        val text = when (instruction) {
            // ---------- POSITION ----------
            PassportLivenessInstruction.PLACE_DOCUMENT -> cs.livenessPlaceDocument

            // ---------- BASELINE ----------
            PassportLivenessInstruction.KEEP_STEADY -> cs.livenessKeepSteady
            PassportLivenessInstruction.KEEP_FLAT -> cs.livenessKeepSteady
            PassportLivenessInstruction.ADJUST_POSITION -> cs.livenessKeepSteady

            // ---------- LIGHT ----------
            PassportLivenessInstruction.DO_NOT_MOVE -> cs.livenessKeepSteady

            // ---------- SWEEP ----------
            PassportLivenessInstruction.LIFT_BOTTOM -> cs.livenessLiftBottom
            PassportLivenessInstruction.MOVE_A_LITTLE_MORE -> cs.livenessMoveLittleMore   // tilt reached, colours awaited
            PassportLivenessInstruction.HOLD_THERE -> cs.livenessHoldThere
            PassportLivenessInstruction.OTHER_SIDE -> cs.livenessOtherSide
            PassportLivenessInstruction.HOLD_IT_UP -> cs.livenessHoldItUp

            // ---------- RETURN (no longer sent) ----------
            PassportLivenessInstruction.PLACE_FLAT_AGAIN -> cs.livenessPlaceFlatAgain

            // ---------- DONE ----------
            PassportLivenessInstruction.KEEP_HOLDING -> cs.livenessKeepHolding

            // ---------- PAUSES (the SDK continues by itself after a few seconds) ----------
            PassportLivenessInstruction.HOLD_IN_HAND -> cs.livenessHoldInHand
            PassportLivenessInstruction.PHONE_TURNED -> cs.livenessPhoneTurned
            PassportLivenessInstruction.TOO_BRIGHT -> cs.livenessTooBright
        }

        feedbackText.value = text

        // ---------- tilt meter (0..100) ----------
        tiltProgress.value = progress
        tiltProgressVisible.value = when (instruction) {
            PassportLivenessInstruction.LIFT_BOTTOM,
            PassportLivenessInstruction.MOVE_A_LITTLE_MORE,
            PassportLivenessInstruction.HOLD_THERE,
            PassportLivenessInstruction.OTHER_SIDE,
            PassportLivenessInstruction.HOLD_IT_UP -> true
            else -> false                                          // hidden before the tilt, in the pauses, and while the MRZ is read
        }
    }

}

@Composable
fun PassportScanScreen(
    activity: LocalPassportScanActivity,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNext: () -> Unit = {},
    feedbackText: String,
    tiltProgress: Int,
    tiltProgressVisible: Boolean,
    imageUrl: String,
    progress: Int,
    eventTypes: String,
) {


    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    var localScanPassport by remember { mutableStateOf<LocalScanPassport?>(null) }

    val layoutDirection = if (BaseTheme.BaseUiLanguage == UiLanguage.Arabic)
        LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (eventTypes != EventTypes.none) {
                if (eventTypes == EventTypes.onSend) {
                    OnSendScreen(100)
                }
                if (eventTypes == EventTypes.onExpired) {
                    OnPassportExpired(imageUrl, onRetry = {
                        onRetry();
                    })
                }
                if (eventTypes == EventTypes.onRetry || eventTypes == EventTypes.onError || eventTypes == EventTypes.onWrongTemplate) {
                    OnErrorScreen(imageUrl, onRetry = {
                        localScanPassport?.retryLiveness()  // the liveness starts again from Position
                        onRetry();
                    })
                }
                if (eventTypes == EventTypes.onLivenessUpdate) {
                    OnLivenessScreen(imageUrl, onRetry = {
                        localScanPassport?.retryLiveness()  // a stopped liveness (suspected_copy / inconclusive) starts again
                        onRetry();
                    })
                }
                if (eventTypes == EventTypes.onComplete) {
                    val showResultPage =
                        FlowController.getCurrentStep()!!.stepDefinition!!.customization.showResultPage
                            ?: false; if (flowEnv.enableNfc) {
                        OnNormalCompleteScreen(imageUrl, onNext = {
                            localScanPassport?.stopScanning()
                            onNext();
                        })
                    } else {
                        if (showResultPage) {
                            OnCompleteScreen(imageUrl, onNext = {
                                localScanPassport?.stopScanning()
                                onNext();
                            })
                        } else {
                            OnNormalCompleteScreen(imageUrl, onNext = {
                                localScanPassport?.stopScanning()
                                onNext();
                            })
                        }

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

                    val result = assentifySdk.startLocalScanPassport(
                        activity,
                        flowEnv.extractedDataLanguage,
                        stepId = FlowController.getCurrentStep()!!.stepDefinition!!.stepId
                    )
                    // result.setDocumentKind("lb")                // Lebanese passport rules; default: other_passport

                    val fm = activity.supportFragmentManager
                    val tx = fm.beginTransaction()
                    localScanPassport = result
                    tx.replace(container.id, result)
                    tx.commitAllowingStateLoss()
                    container
                },

                )

            DisposableEffect(localScanPassport) {
                onDispose {
                    try {
                        localScanPassport?.stopScanning()
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
                            localScanPassport?.stopScanning()
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
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                            .padding(top = 20.dp)
                    )
                }

            }

            if (eventTypes == EventTypes.none) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp, end = 20.dp, bottom = 25.dp)
                ) {
                    LocalLivenessFeedback(
                        text = feedbackText,
                        tiltProgress = tiltProgress,
                        tiltProgressVisible = tiltProgressVisible
                    )
                }


            }

        }
    }
}

/**
 * The instruction in a rounded container (FieldColor background, BaseTextColor text), with the tilt meter
 * (0..100%) underneath while the passport is tilted. Same design as PassportScanActivity's LivenessFeedback.
 */
@Composable
private fun LocalLivenessFeedback(
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
                .padding(top = 12.dp)                                  // space above the box
                .fillMaxWidth()
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(BaseTheme.FieldColor)
                .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)          // smaller inside padding
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
                        PulsingDot(color = accent, active = !done)
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
private fun LocalPulsingDot(color: Color, active: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.6f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
        Box(                                                     // soft halo
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (active) 0.35f else 0f }
                .clip(CircleShape)
                .background(color)
        )
        Box(                                                     // solid centre
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}