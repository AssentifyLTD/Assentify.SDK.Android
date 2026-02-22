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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.QrStep.HowToCaptureQrActivity
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnErrorScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnFlipCardScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnLivenessScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnNormalCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnSendScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnWrongTemplateScreen
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
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

    private var start = mutableStateOf(false)
    private var isLastPageValue = mutableStateOf(false)
    private var isFrontPageValue = mutableStateOf(false)
    private var feedbackText = mutableStateOf("")
    private var uploadingProgress = mutableStateOf(0)
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var classifiedTemplateValue = mutableStateOf<String>("")
    private var extractedInformation = mutableStateOf<Map<String, String>?>(null)

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    private var timeStarted = getCurrentDateTimeForTracking()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@IDCardScanActivity);
            }
        })

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
                        },
                        onFlip = {
                            start.value = false;
                            feedbackText.value = "";
                            uploadingProgress.value = 0;
                            eventTypes.value = EventTypes.none;
                        },
                        onNext = { hasQr ->
                            if (flowEnv.enableQr && hasQr) {
                                HowToCaptureQrActivity.start(
                                    context = this,
                                );
                            } else {
                                FlowController.makeCurrentStepDone(extractedInformation.value!!,timeStarted);
                                FlowController.naveToNextStep(context = this)
                            }
                        },
                        feedbackText = feedbackText.value,
                        imageUrl = imageUrl.value,
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
        /** Track Progress **/
        val  currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "Error" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response),
            status = "InProgress"
        )
        /***/
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onRetry
            try {
                imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
            } catch (e: Exception) {
                imageUrl.value = "";
            }
        }
        /** Track Progress **/
        val  currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "Retry" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response),
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
            currentMap.putAll(dataModel.iDExtractedModel!!.transformedProperties!!)
            extractedInformation.value = currentMap
            OnCompleteScreenData.clear();
            OnCompleteScreenData.setData(extractedInformation.value);
            start.value = false;
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
        if(isLastPage){
            /** Track Progress **/
            val  currentStep = FlowController.getCurrentStep()
            FlowController.trackProgress(
                currentStep = currentStep!!,
                response = "Completed",
                inputData = extractedInformation.value,
                status = "Completed"
            )
            /***/
        }



    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onLivenessUpdate
            imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
        }
        /** Track Progress **/
        val  currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "LivenessUpdate" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response),
            status = "InProgress"
        )
        /***/
    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
        runOnUiThread {
            start.value = false;
            eventTypes.value = EventTypes.onWrongTemplate
            imageUrl.value = getImageUrlFromBaseResponseDataModel(dataModel.response!!);
        }
        /** Track Progress **/
        val  currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = "WrongTemplate" + " - " + FlowController.extractAfterDash(dataModel.responseJsonObject?.optString("error")),
            inputData = FlowController.decodeToJsonObject(dataModel.response),
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
            if (start.value == false) {
                if (zoom != ZoomType.SENDING && zoom != ZoomType.NO_DETECT) {
                    if (zoom == ZoomType.ZOOM_IN) {
                        feedbackText.value = "Move ID Closer"
                    }
                    if (zoom == ZoomType.ZOOM_OUT) {
                        feedbackText.value = "Move ID Further"
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
                    if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && brightnessEvents == BrightnessEvents.Good &&  isCentered) {
                        feedbackText.value = "Hold Steady"
                    }
                    if (motion == MotionType.NO_DETECT && zoom == ZoomType.NO_DETECT) {
                        feedbackText.value = "Please present ID"
                    }else if(!isCentered){
                        feedbackText.value = "Please center your card"
                    }
                }
            } else {
                feedbackText.value = ""
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
    imageUrl: String,
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

    Box(modifier = Modifier.fillMaxSize()) {

        if (eventTypes != EventTypes.none) {
            if (eventTypes == EventTypes.onSend) {
                OnSendScreen(progress)
            }
            if (eventTypes == EventTypes.onRetry || eventTypes == EventTypes.onError) {
                OnErrorScreen(imageUrl, onRetry = {
                    onRetry();
                })
            }
            if (eventTypes == EventTypes.onWrongTemplate) {
                OnWrongTemplateScreen(
                    imageUrl,
                    expectedImageUrl = "",
                    onRetry = {
                        onRetry();
                    })
            }
            if (eventTypes == EventTypes.onLivenessUpdate) {
                OnLivenessScreen(imageUrl, onRetry = {
                    onRetry();
                })
            }
            if (eventTypes == EventTypes.onComplete) {
                if (isLastPage) {
                    val showResultPage = FlowController.getCurrentStep()!!.stepDefinition!!.customization.showResultPage
                        ?: false;

                    if(flowEnv.enableQr && kycDocumentDetails.first { it.templateProcessingKeyInformation == classifiedTemplate }.hasQrCode ){
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
                    }else{
                        if(showResultPage){
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

                        }else{
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
                                scanID?.changeTemplateId()
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
                    flowEnv.language,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isManual) {
                        scanIDManual!!.stopScanning()
                    } else {
                        scanID!!.stopScanning()
                    }
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint =   BaseTheme.BaseTextColor,
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

            Spacer(Modifier.height(10.dp))

            if(eventTypes != EventTypes.none){
                ProgressStepper(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
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
                        )
                ) {
                    Text(
                        "Take Photo",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        color = BaseTheme.BaseSecondaryTextColor,
                        modifier = Modifier.padding(vertical = 7.dp)
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
                        color =   BaseTheme.BaseTextColor,
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


