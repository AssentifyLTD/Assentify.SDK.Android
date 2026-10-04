package com.assentify.sdk.Flow.NfcStep

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.Events.OnNormalCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.NfcPassportResponseModelObject
import com.assentify.sdk.OnCompleteScreenData
import com.assentify.sdk.ScanNFC.ScanNfc
import com.assentify.sdk.ScanNFC.ScanNfcCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel

private const val TAG = "NfcScanActivity"
private const val SCREEN_TAG = "NfcScanScreen"

class NfcScanActivity : FragmentActivity(), ScanNfcCallback {

    private lateinit var scanNfc: ScanNfc
    private lateinit var passportResponseModel: PassportResponseModel
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var feedbackText = mutableStateOf("")
    private var dataIDModel = mutableStateOf<PassportResponseModel?>(null)

    private var timeStarted = getCurrentDateTimeForTracking()

    private var isComplete = mutableStateOf<Boolean>(false)
    private var isNavigating = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: savedInstanceState=${savedInstanceState != null}, timeStarted=$timeStarted")

        val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
        val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
        val nfcStrings = flowStrings()
        feedbackText.value = nfcStrings.nfcInitialFeedback
        Log.d(TAG, "onCreate: extractedDataLanguage=${flowEnv.extractedDataLanguage}")

        val storedModel = NfcPassportResponseModelObject.getPassportResponseModelObject()
        if (storedModel == null) {
            Log.e(TAG, "onCreate: PassportResponseModel from NfcPassportResponseModelObject is NULL — will crash on !!")
        } else {
            Log.d(TAG, "onCreate: PassportResponseModel loaded, hasExtractedModel=${storedModel.passportExtractedModel != null}")
        }
        passportResponseModel = storedModel!!

        scanNfc = assentifySdk.startScanNfc(
            this,
            languageCode = flowEnv.extractedDataLanguage,
            context = this
        )
        Log.d(TAG, "onCreate: ScanNfc initialized")

        val nfcSupported = scanNfc.isNfcSupported(activity = this)
        Log.d(TAG, "onCreate: isNfcSupported=$nfcSupported")
        if (nfcSupported) {
            val nfcEnabled = scanNfc.isNfcEnabled(activity = this)
            Log.d(TAG, "onCreate: isNfcEnabled=$nfcEnabled")
            if (nfcEnabled) {
                //
            } else {
                Log.w(TAG, "onCreate: NFC disabled, opening NFC settings")
                val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                startActivity(intent)
            }
        } else {
            Log.w(TAG, "onCreate: NFC not supported on this device")
            feedbackText.value = nfcStrings.nfcNotSupported
        }

        /* onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
             override fun handleOnBackPressed() {
                 FlowController.backClick(this@NfcScanActivity);
             }
         })*/

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NfcScanScreen(
                        onBack = {
                            Log.d(TAG, "onBack clicked")
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onSkip = {
                            Log.d(TAG, "onSkip clicked: eventTypes ${eventTypes.value} -> ${EventTypes.onComplete}")
                            eventTypes.value = EventTypes.onComplete
                        },
                        onNext = {
                            Log.d(TAG, "onNext clicked: isNavigating=$isNavigating, isComplete=${isComplete.value}")

                            if (!isNavigating) {
                                isNavigating = true
                                try {
                                    if (isComplete.value) {
                                        Log.d(TAG, "onNext: using NFC scan result (dataIDModel), isNull=${dataIDModel.value == null}")
                                        FlowController.makeCurrentStepDone(dataIDModel.value!!.passportExtractedModel!!.transformedProperties!!, timeStarted);
                                        FlowController.naveToNextStep(this)
                                    } else {
                                        Log.d(TAG, "onNext: NFC skipped, using original passportResponseModel")
                                        FlowController.makeCurrentStepDone(passportResponseModel.passportExtractedModel!!.transformedProperties!!, timeStarted);
                                        FlowController.naveToNextStep(this)
                                    }
                                    Log.d(TAG, "onNext: navigated to next step")
                                } catch (e: Exception) {
                                    Log.e(TAG, "onNext: failed to complete step / navigate", e)
                                    isNavigating = false
                                    throw e
                                }
                            } else {
                                Log.w(TAG, "onNext: ignored, navigation already in progress")
                            }
                        },
                        onRetry = {
                            Log.d(TAG, "onRetry clicked: resetting state")
                            feedbackText.value = flowStrings().nfcInitialFeedback;
                            eventTypes.value = EventTypes.none;
                            imageUrl.value = ""
                        },
                        imageUrl = imageUrl.value,
                        eventTypes = eventTypes.value,
                        feedbackText = feedbackText.value,
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            Log.d(TAG, "start: launching NfcScanActivity from ${context.javaClass.simpleName}")
            val intent = Intent(context, NfcScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
        val adapter = NfcAdapter.getDefaultAdapter(this) ?: run {
            Log.w(TAG, "onResume: NfcAdapter is null"); return
        }
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)
        }
        adapter.enableReaderMode(
            this,
            { tag ->
                Log.d(TAG, "readerMode: tag discovered, techs=${tag.techList.joinToString()}")
                // Wrap the tag in an Intent so the existing SDK API keeps working
                val intent = Intent(NfcAdapter.ACTION_TECH_DISCOVERED)
                    .putExtra(NfcAdapter.EXTRA_TAG, tag)
                scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel)
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            options
        )
        Log.d(TAG, "onResume: reader mode enabled")
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this)
        Log.d(TAG, "onPause: reader mode disabled")
    }



    override fun onDestroy() {
        Log.d(TAG, "onDestroy: isComplete=${isComplete.value}, eventTypes=${eventTypes.value}")
        super.onDestroy()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: action=${intent.action}, hasTag=${intent.hasExtra(NfcAdapter.EXTRA_TAG)}")
        scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel)
    }

    /**  Events **/
    override fun onStartNfcScan() {
        Log.d(TAG, "onStartNfcScan: chip reading started")
        runOnUiThread {
            feedbackText.value = flowStrings().nfcReading
            eventTypes.value = EventTypes.onSend
        }
    }

    override fun onCompleteNfcScan(dataModel: PassportResponseModel) {
        val extracted = dataModel.passportExtractedModel
        Log.d(
            TAG,
            "onCompleteNfcScan: success, hasExtractedModel=${extracted != null}, " +
                    "hasImageUrl=${!extracted?.imageUrl.isNullOrEmpty()}, " +
                    "facesCount=${extracted?.faces?.size ?: 0}, " +
                    "propertiesCount=${extracted?.transformedProperties?.size ?: 0}"
        )
        runOnUiThread {
            try {
                isComplete.value = true;
                dataIDModel.value = dataModel;
                feedbackText.value = ""
                OnCompleteScreenData.clear();
                OnCompleteScreenData.setData(dataModel.passportExtractedModel!!.transformedProperties);
                eventTypes.value = EventTypes.onComplete
                imageUrl.value = dataModel.passportExtractedModel!!.imageUrl!!
                if (dataModel.passportExtractedModel!!.faces!!.isNotEmpty()) {
                    Log.d(TAG, "onCompleteNfcScan: setting face image in FlowController")
                    FlowController.setImage(dataModel.passportExtractedModel!!.faces!!.first())
                } else {
                    Log.w(TAG, "onCompleteNfcScan: no faces returned from chip")
                }
            } catch (e: Exception) {
                Log.e(TAG, "onCompleteNfcScan: failed to process result", e)
                throw e
            }
        }
    }

    override fun onErrorNfcScan(dataModel: PassportResponseModel, message: String) {
        Log.e(TAG, "onErrorNfcScan: message=$message")
        runOnUiThread {
            feedbackText.value = flowStrings().nfcConnectionLost
            eventTypes.value = EventTypes.onError
        }
    }
}

@Composable
fun NfcScanScreen(
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    onSkip: () -> Unit = {},
    onRetry: () -> Unit = {},
    eventTypes: String,
    imageUrl: String,
    feedbackText: String,
) {

    val context = LocalContext.current
    val s = flowStrings()

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    // Log only when the event type actually changes, not on every recomposition
    LaunchedEffect(eventTypes) {
        Log.d(SCREEN_TAG, "eventTypes changed -> $eventTypes, hasImage=${imageUrl.isNotEmpty()}")
    }

    val iconSvg = remember {
        loadSvgFromAssets(context, "ic_nfc.svg").also {
            if (it == null) Log.w(SCREEN_TAG, "ic_nfc.svg failed to load from assets")
        }
    }

    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(0.dp) }

    BaseBackgroundContainer(modifier = Modifier
        .fillMaxSize()
    ) {

        if (eventTypes == EventTypes.onComplete) {
            val showResultPage = FlowController.getCurrentStep()!!.stepDefinition!!.customization.showResultPage
                ?: false;
            LaunchedEffect(showResultPage) {
                Log.d(SCREEN_TAG, "Showing complete screen, showResultPage=$showResultPage")
            }
            if (showResultPage) {
                OnCompleteScreen(imageUrl, onNext = {
                    Log.d(SCREEN_TAG, "OnCompleteScreen: next clicked")
                    onNext();
                })
            } else {
                OnNormalCompleteScreen(imageUrl, onNext = {
                    Log.d(SCREEN_TAG, "OnNormalCompleteScreen: next clicked")
                    onNext();
                })
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
                .onGloballyPositioned { coordinates ->
                    headerHeightDp = with(density) { coordinates.size.height.toDp() }
                }
        ) {
            if (BaseTheme.StepperType == StepperType.Normal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
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
                        contentScale = ContentScale.Fit,
                        onError = { state ->
                            Log.w(SCREEN_TAG, "Logo failed to load", state.result.throwable)
                        }
                    )

                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(10.dp))

            ProgressStepper(
                onBack = { onBack() },
                normalModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                percentageBased = Modifier
                    .fillMaxWidth().padding(horizontal = 5.dp).padding(top = 20.dp)
            )

        }

        if (eventTypes != EventTypes.onComplete) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeightDp + 40.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
            ) {
                Text(
                    text = s.nfcCapture,
                    color = BaseTheme.BaseTextColor,
                    fontSize = 24.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )

                // 🔹 Middle section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    // Dashed outline behind icon
                    Box(
                        modifier = Modifier
                            .size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        iconSvg?.let {
                            Icon(
                                painter = it,
                                contentDescription = "ic_nfc",
                                modifier = Modifier.size(240.dp),
                                tint = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (eventTypes == EventTypes.onSend) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(60.dp)
                                .align(Alignment.CenterHorizontally),
                            color = BaseTheme.BaseTextColor,
                            strokeWidth = 6.dp
                        )
                    } else {
                        Text(
                            s.nfcDetected,
                            color = BaseTheme.BaseTextColor,
                            fontSize = 22.sp,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }


                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = feedbackText,
                        color = BaseTheme.BaseTextColor,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Thin,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                // 🔹 Bottom section
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {

                    val isError = eventTypes == EventTypes.onError

                    if (isError) {
                        Button(
                            onClick = { onRetry() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BaseTheme.BaseRedColor
                            ),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                text = s.retry,
                                fontFamily = InterFont,
                                fontWeight = BaseTheme.BaseClickFontWeight,
                                color = BaseTheme.BaseSecondaryTextColor,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = BaseTheme.BaseClickColor!!.toBrush(),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Text(
                            text = s.skip,
                            color = BaseTheme.BaseSecondaryTextColor,
                            fontFamily = InterFont,
                            fontWeight = BaseTheme.BaseClickFontWeight,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}