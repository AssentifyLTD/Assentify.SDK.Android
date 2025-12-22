package com.assentify.sdk.Flow.FaceStep

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Base64ImageObject
import com.assentify.sdk.Core.Constants.ActiveLiveEvents
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.FaceEvents
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceMatchManual
import com.assentify.sdk.FaceMatch.FaceMatchResult
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.Models.getImageUrlFromBaseResponseDataModel


class FaceMatchActivity : FragmentActivity(), FaceMatchCallback {

    private var start = mutableStateOf(false)
    private var feedbackText = mutableStateOf("")
    private var uploadingProgress = mutableStateOf(0)
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var currentActiveLiveEvents = mutableStateOf<ActiveLiveEvents>(ActiveLiveEvents.Good)
    private var faceModel = mutableStateOf<FaceResponseModel?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FaceMatchScanScreen(
                        activity = this@FaceMatchActivity, onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onRetry = {
                            start.value = false;
                            feedbackText.value = "";
                            uploadingProgress.value = 0;
                            eventTypes.value = EventTypes.none;
                            imageUrl.value = "";
                        },
                        onScanRetry = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onIDChange = {
                            FlowController.faceIDChange();
                            FlowController.backClick(context = this)
                        },
                        onNext = {
                            val outputProps: Map<String, String> =
                                faceModel.value!!.faceExtractedModel!!.outputProperties!!
                                    .mapValues { it.value.toString() }
                            Base64ImageObject.clear();
                            FlowController.makeCurrentStepDone(outputProps);
                            FlowController.naveToNextStep(context = this)
                        },
                        feedbackText = feedbackText.value,
                        imageUrl = imageUrl.value,
                        progress = uploadingProgress.value,
                        eventTypes = eventTypes.value,
                        faceModel = faceModel.value
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, FaceMatchActivity::class.java)
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
            uploadingProgress.value = progress;
        }
    }

    override fun onError(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onError
            imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
        }
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onRetry
            try {
                imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
            }catch (e:Exception){
                imageUrl.value = "" ;
            }

        }
    }

    override fun onComplete(dataModel: FaceResponseModel) {
        runOnUiThread {
            faceModel.value = dataModel;
            start.value = false;
            eventTypes.value = EventTypes.onComplete
        }
    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onLivenessUpdate
            imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
        }
    }

    override fun onCurrentLiveMoveChange(activeLiveEvents: ActiveLiveEvents) {
        runOnUiThread {
            currentActiveLiveEvents.value = activeLiveEvents;
            if (activeLiveEvents != ActiveLiveEvents.Good) {
                when (activeLiveEvents) {
                    ActiveLiveEvents.YawLeft -> feedbackText.value = "Please move your face left"
                    ActiveLiveEvents.YawRight -> feedbackText.value = "Please move your face right"
                    ActiveLiveEvents.PitchUp -> feedbackText.value = "Please move your face up"
                    ActiveLiveEvents.PitchDown -> feedbackText.value = "Please move your face down"
                    ActiveLiveEvents.WinkLeft -> feedbackText.value =
                        "Please wink with your left eye"

                    ActiveLiveEvents.WinkRight -> feedbackText.value =
                        "Please wink with your right eye"

                    ActiveLiveEvents.BLINK -> feedbackText.value = "Please blink your eyes"
                    ActiveLiveEvents.Good -> feedbackText.value = ""
                }
            }
        }

    }

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        faceEvents: FaceEvents,
        zoomType: ZoomType,
        detectedFaces: Int
    ) {
        runOnUiThread {
            if (start.value == false && currentActiveLiveEvents.value == ActiveLiveEvents.Good) {
                if (detectedFaces > 1) {
                    feedbackText.value =
                        "We detected more than one face.Please keep just your face in view."
                } else if (zoomType != ZoomType.SENDING && zoomType != ZoomType.NO_DETECT) {
                    if (zoomType == ZoomType.ZOOM_IN) {
                        feedbackText.value = "Please Move Closer"
                    }
                    if (zoomType == ZoomType.ZOOM_OUT) {
                        feedbackText.value = "Please Move Further"
                    }
                } else if (motion != MotionType.SENDING && motion != MotionType.NO_DETECT) {
                    feedbackText.value = "Please Hold Your Hand"

                } else if (brightnessEvents != BrightnessEvents.Good) {
                    if (brightnessEvents == BrightnessEvents.TooDark) {
                        feedbackText.value = "Please increase the lighting"
                    }
                    if (brightnessEvents == BrightnessEvents.TooBright) {
                        feedbackText.value = "Please reduce the lighting"
                    }
                } else if (faceEvents != FaceEvents.Good && faceEvents != FaceEvents.NO_DETECT) {
                    if (faceEvents == FaceEvents.RollLeft) {
                        feedbackText.value = "Please tilt your head to the right"
                    }
                    if (faceEvents == FaceEvents.RollRight) {
                        feedbackText.value = "Please tilt your head to the left"
                    }
                    if (faceEvents == FaceEvents.YawLeft) {
                        feedbackText.value = "Please turn your head to the right"
                    }
                    if (faceEvents == FaceEvents.YawRight) {
                        feedbackText.value = "Please turn your head to the left"
                    }
                    if (faceEvents == FaceEvents.PitchUp) {
                        feedbackText.value = "Please lower your head"
                    }
                    if (faceEvents == FaceEvents.PitchDown) {
                        feedbackText.value = "Please raise your head"
                    }
                } else {
                    if (motion == MotionType.SENDING && zoomType == ZoomType.SENDING && brightnessEvents == BrightnessEvents.Good && faceEvents == FaceEvents.Good) {
                        feedbackText.value = "Hold Steady"
                    }
                    if (motion == MotionType.NO_DETECT && zoomType == ZoomType.NO_DETECT && faceEvents == FaceEvents.NO_DETECT) {
                        feedbackText.value = "Please face within circle"
                    }
                }
            } else {
                if (currentActiveLiveEvents.value == ActiveLiveEvents.Good) {
                    feedbackText.value = ""
                }
            }
        }
    }

}

@Composable
fun FaceMatchScanScreen(
    activity: FaceMatchActivity,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onScanRetry: () -> Unit = {},
    onIDChange: () -> Unit = {},
    onNext: () -> Unit = {},
    feedbackText: String,
    imageUrl: String,
    progress: Int,
    eventTypes: String,
    faceModel: FaceResponseModel?,
) {




    var isManual by remember { mutableStateOf<Boolean>(false) }
    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    var faceMatch by remember { mutableStateOf<FaceMatch?>(null) }
    var faceMatchManual by remember { mutableStateOf<FaceMatchManual?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        if (eventTypes != EventTypes.none) {
            if (eventTypes == EventTypes.onSend) {
                OnFaceSendScreen(progress)
            }
            if (eventTypes == EventTypes.onRetry || eventTypes == EventTypes.onError || eventTypes == EventTypes.onLivenessUpdate) {
                OnFaceErrorScreen(imageUrl, onRetry = {
                    onRetry();
                })
            }
            if (eventTypes == EventTypes.onComplete) {
                FaceResultScreen(faceModel!!, onNext = {
                    if (isManual) {
                        faceMatchManual!!.stopScanning()
                    } else {
                        faceMatch!!.stopScanning()
                    }
                    onNext();
                }, onRetry = {
                    onScanRetry();
                }, onIDChange = {
                    if (isManual) {
                        faceMatchManual!!.stopScanning()
                    } else {
                        faceMatch!!.stopScanning()
                    }
                    onIDChange();
                })
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

                val result = assentifySdk.startFaceMatch(
                    activity,
                    Base64ImageObject.getImage()!!,
                    showCountDown = true,
                    stepId = FlowController.getCurrentStep()!!.stepDefinition!!.stepId
                )

                val fm = activity.supportFragmentManager
                val tx = fm.beginTransaction()
                when (result) {
                    is FaceMatchResult.Manual -> {
                        isManual = true;
                        faceMatchManual = result.data
                        tx.replace(container.id, result.data)
                    }

                    is FaceMatchResult.Auto -> {
                        isManual = false;
                        faceMatch = result.data
                        tx.replace(container.id, result.data)
                    }
                }
                tx.commitAllowingStateLoss()
                container
            },

            )



        DisposableEffect(isManual, faceMatch, faceMatchManual) {
            onDispose {
                try {
                    if (isManual) {
                        faceMatchManual?.stopScanning()
                    } else {
                        faceMatch?.stopScanning()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isManual) {
                        faceMatchManual!!.stopScanning()
                    } else {
                        faceMatch!!.stopScanning()
                    }
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                logoBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(10.dp))

            ProgressStepper(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }

        if (eventTypes == EventTypes.none) {
            if (isManual) {
                Button(
                    onClick = {
                        faceMatchManual!!.takePicture();
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor)),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Take Photo",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 30.dp, bottom = 100.dp)
                ) {
                    Text(
                        feedbackText,
                        color = Color.White,
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


