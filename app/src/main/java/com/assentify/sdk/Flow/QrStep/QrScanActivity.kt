package com.assentify.sdk.Flow.QrStep

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnErrorScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnSendScreen
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.QrIDResponseModelObject
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanQr.ScanQr
import com.assentify.sdk.ScanQr.ScanQrCallback
import com.assentify.sdk.ScanQr.ScanQrManual
import com.assentify.sdk.ScanQr.ScanQrResult
import com.assentify.sdk.SelectedTemplatesObject


class QrScanActivity : FragmentActivity(), ScanQrCallback {

    private var start = mutableStateOf(false)
    private var uploadingProgress = mutableStateOf(0)
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var dataIDModel = mutableStateOf<IDResponseModel?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@QrScanActivity);
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QrScanScreen(
                        activity = this@QrScanActivity,
                        onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onRetry = {
                            start.value = false;
                            uploadingProgress.value = 0;
                            eventTypes.value = EventTypes.none;
                        },
                        onNext = {
                            FlowController.makeCurrentStepDone(dataIDModel.value!!.iDExtractedModel!!.transformedProperties!!);
                            FlowController.naveToNextStep(context = this)
                        },
                        progress = uploadingProgress.value,
                        eventTypes = eventTypes.value,
                        imageUrl = imageUrl.value,
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, QrScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    /**  Events **/

    override fun onStartQrScan() {
        runOnUiThread {
            start.value = true;
            eventTypes.value = EventTypes.onSend
        }
    }

    override fun onCompleteQrScan(dataModel: IDResponseModel) {
        runOnUiThread {
            /// SDK TODO
            val finalIDResponseModelObject = QrIDResponseModelObject.getQrIDResponseModelObject()
            val finalMap = mutableMapOf<String, String>()
            for ((key, value) in finalIDResponseModelObject.iDExtractedModel!!.transformedProperties!!) {
                if (key.contains("Image")) {
                    finalMap[key] = value
                } else {
                    if (!dataModel.iDExtractedModel!!.transformedProperties!!.containsKey(key)) {
                        finalMap[key] = value
                    } else if (dataModel.iDExtractedModel!!.transformedProperties!![key]!!.isNotEmpty()) {
                        finalMap[key] = value
                    } else {
                        finalMap[key] = dataModel.iDExtractedModel!!.transformedProperties!![key]!!
                    }

                }
            }
            finalIDResponseModelObject.iDExtractedModel!!.transformedProperties = finalMap;
            dataIDModel.value = finalIDResponseModelObject;
            start.value = false;
            eventTypes.value = EventTypes.onComplete
            imageUrl.value = dataModel.iDExtractedModel!!.imageUrl!!

        }
    }

    override fun onErrorQrScan(message: String) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onError
        }
    }

    override fun onUploadingProgress(progress: Int) {
        runOnUiThread {
            uploadingProgress.value = progress;
        }
    }


}

@Composable
fun QrScanScreen(
    activity: QrScanActivity,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNext: () -> Unit = {},
    progress: Int,
    eventTypes: String,
    imageUrl: String,
) {


    var isManual by remember { mutableStateOf<Boolean>(false) }
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

    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    var scanQr by remember { mutableStateOf<ScanQr?>(null) }
    var scanQrManual by remember { mutableStateOf<ScanQrManual?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        if (eventTypes != EventTypes.none) {
            if (eventTypes == EventTypes.onSend) {
                OnSendScreen(progress)
            }
            if (eventTypes == EventTypes.onError) {
                OnErrorScreen("", onRetry = {
                    onRetry();
                })
            }
            if (eventTypes == EventTypes.onComplete) {
                OnCompleteScreen(imageUrl, onNext = {
                    if (isManual) {
                        scanQrManual?.stopScanning()
                    } else {
                        scanQr?.stopScanning()
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

                val result = assentifySdk.startScanQr(
                    activity,
                    templatesByCountry = templatesByCountry,
                    flowEnv.language,
                    stepId = FlowController.getCurrentStep()!!.stepDefinition!!.stepId
                )

                val fm = activity.supportFragmentManager
                val tx = fm.beginTransaction()
                when (result) {
                    is ScanQrResult.Manual -> {
                        isManual = true;
                        scanQrManual = result.data
                        tx.replace(container.id, result.data)
                    }

                    is ScanQrResult.Auto -> {
                        isManual = false;
                        scanQr = result.data
                        tx.replace(container.id, result.data)
                    }
                }
                tx.commitAllowingStateLoss()
                container
            },
        )

        DisposableEffect(isManual, scanQr, scanQrManual) {
            onDispose {
                try {
                    if (isManual) {
                        scanQrManual?.stopScanning()
                    } else {
                        scanQr?.stopScanning()
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
                        scanQrManual?.stopScanning()
                    } else {
                        scanQr?.stopScanning()
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
                        scanQrManual!!.takePicture();
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
            }
        }

    }
}


