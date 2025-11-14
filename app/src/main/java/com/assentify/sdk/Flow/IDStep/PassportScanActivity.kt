package com.assentify.sdk.Flow.IDStep

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.DoneFlags
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.NfcStep.NfcScanActivity
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnErrorScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnLivenessScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnSendScreen
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.Models.getImageUrlFromBaseResponseDataModel
import com.assentify.sdk.NfcPassportResponseModelObject
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.assentify.sdk.ScanPassport.ScanPassportManual
import com.assentify.sdk.ScanPassport.ScanPassportResult


class PassportScanActivity : FragmentActivity(), ScanPassportCallback {

    private var start = mutableStateOf(false)
    private var feedbackText = mutableStateOf("")
    private var uploadingProgress = mutableStateOf(0)
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var dataIDModel = mutableStateOf<PassportResponseModel?>(null)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@PassportScanActivity);
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PassportScanScreen(
                        activity = this@PassportScanActivity, onBack = {
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
                                FlowController.makeCurrentStepDone(dataIDModel.value!!.passportExtractedModel!!.transformedProperties!!);
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
            val intent = Intent(context, PassportScanActivity::class.java)
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
            }        }
    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onLivenessUpdate
            imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
        }
    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onWrongTemplate
            imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
        }
    }

    override fun onComplete(dataModel: PassportResponseModel, doneFlag: DoneFlags) {
        runOnUiThread {
            NfcPassportResponseModelObject.setPassportResponseModelObject(dataModel)
            dataIDModel.value = dataModel;
            start.value = false;
            eventTypes.value = EventTypes.onComplete
        }
    }

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        zoom: ZoomType
    ) {
        runOnUiThread {
            if (start.value == false) {
                if (zoom != ZoomType.SENDING && zoom != ZoomType.NO_DETECT) {
                    if (zoom == ZoomType.ZOOM_IN) {
                        feedbackText.value = "Move Passport Closer"
                    }
                    if (zoom == ZoomType.ZOOM_OUT) {
                        feedbackText.value = "Move Passport Further"
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

                } else {
                    if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && brightnessEvents == BrightnessEvents.Good) {
                        feedbackText.value = "Hold Steady"
                    }
                    if (motion == MotionType.NO_DETECT && zoom == ZoomType.NO_DETECT) {
                        feedbackText.value = "Please present passport"
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
    activity: PassportScanActivity,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNext: () -> Unit = {},
    feedbackText: String,
    imageUrl: String,
    progress: Int,
    eventTypes: String,
) {


    var isManual by remember { mutableStateOf<Boolean>(false) }
    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    var scanPassport by remember { mutableStateOf<ScanPassport?>(null) }
    var scanPassportManual by remember { mutableStateOf<ScanPassportManual?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        if (eventTypes != EventTypes.none) {
            if (eventTypes == EventTypes.onSend) {
                OnSendScreen(progress)
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
                OnCompleteScreen(imageUrl, onNext = {
                    if (isManual) {
                        scanPassportManual?.stopScanning()
                    } else {
                        scanPassport?.stopScanning()
                    }
                    onNext();
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

                val result = assentifySdk.startScanPassport(
                    activity,
                    flowEnv.language,
                    stepId = FlowController.getCurrentStep()!!.stepDefinition!!.stepId
                )

                val fm = activity.supportFragmentManager
                val tx = fm.beginTransaction()
                when (result) {
                    is ScanPassportResult.Manual -> {
                        isManual = true;
                        scanPassportManual = result.data
                        tx.replace(container.id, result.data)
                    }

                    is ScanPassportResult.Auto -> {
                        isManual = false;
                        scanPassport = result.data
                        tx.replace(container.id, result.data)
                    }
                }
                tx.commitAllowingStateLoss()
                container
            },

            )

        DisposableEffect(isManual, scanPassport, scanPassportManual) {
            onDispose {
                try {
                    if (isManual) {
                        scanPassportManual?.stopScanning()
                    } else {
                        scanPassport?.stopScanning()
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
                        scanPassportManual!!.stopScanning()
                    } else {
                        scanPassport!!.stopScanning()
                    }
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
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
                        scanPassportManual!!.takePicture();
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


