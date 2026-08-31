package com.assentify.sdk.Flow.IDStep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    private var timeStarted = getCurrentDateTimeForTracking()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@LocalPassportScanActivity);
            }
        })

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
                        },
                        onNext = {
                            if (flowEnv.enableNfc) {
                                NfcScanActivity.start(context = this)
                            } else {
                                FlowController.makeCurrentStepDone(
                                    dataIDModel.value!!.passportExtractedModel!!.transformedProperties!!,
                                    timeStarted
                                );
                                FlowController.naveToNextStep(this)
                            }
                        },
                        feedbackText = feedbackText.value,
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
                eventTypes.value = EventTypes.onExpired
            }
        } else {
            runOnUiThread {
                start.value = false;
                eventTypes.value = EventTypes.onRetry
            }
        }

    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
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
            eventTypes.value = EventTypes.onComplete
            imageUrl.value = dataModel.passportExtractedModel!!.imageUrl!!
            dataModel.passportExtractedModel!!.outputProperties?.forEach { (key, value) ->
                val X = FlowController.getFaceMatchInputImageKey()
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

}

@Composable
fun PassportScanScreen(
    activity: LocalPassportScanActivity,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNext: () -> Unit = {},
    feedbackText: String,
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
                        onRetry();
                    })
                }
                if (eventTypes == EventTypes.onLivenessUpdate) {
                    OnLivenessScreen(imageUrl, onRetry = {
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
                        .padding(start = 12.dp, end = 30.dp, bottom = 25.dp)
                ) {
                    Text(
                        feedbackText,
                        color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }


            }

        }
    }
}


