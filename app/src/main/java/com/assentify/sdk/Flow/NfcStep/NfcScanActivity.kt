package com.assentify.sdk.Flow.NfcStep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
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

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStateReceiverRegistered = false

    /** One callback instance, reused for every reader-mode registration. Runs on a binder thread. */
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        try {
            val intent = Intent(NfcAdapter.ACTION_TECH_DISCOVERED)
                .putExtra(NfcAdapter.EXTRA_TAG, tag)
            scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel)
        } catch (t: Throwable) {
            Log.e(TAG, "Tag handling failed", t)
            runOnUiThread {
                feedbackText.value = flowStrings().nfcConnectionLost
                eventTypes.value = EventTypes.onError
            }
        }
    }

    /** NFC switched on while this screen is visible (e.g. back from Settings): register again */
    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
            Log.d(TAG, "NFC adapter state changed: $state")
            if (state == NfcAdapter.STATE_ON) {
                feedbackText.value = flowStrings().nfcInitialFeedback
                enableReaderModeSafely("adapter turned on")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
        val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
        val nfcStrings = flowStrings()
        feedbackText.value = nfcStrings.nfcInitialFeedback
        passportResponseModel = NfcPassportResponseModelObject.getPassportResponseModelObject()!!

        scanNfc = assentifySdk.startScanNfc(
            this,
            languageCode = flowEnv.extractedDataLanguage,
            context = this
        )

        if (scanNfc.isNfcSupported(activity = this)) {
            if (!scanNfc.isNfcEnabled(activity = this)) {
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
        } else {
            feedbackText.value = nfcStrings.nfcNotSupported
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NfcScanScreen(
                        onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onSkip = {
                            eventTypes.value = EventTypes.onComplete
                        },
                        onNext = {
                            if (!isNavigating) {
                                isNavigating = true
                                if (isComplete.value) {
                                    FlowController.makeCurrentStepDone(dataIDModel.value!!.passportExtractedModel!!.transformedProperties!!, timeStarted)
                                    FlowController.naveToNextStep(this)
                                } else {
                                    FlowController.makeCurrentStepDone(passportResponseModel.passportExtractedModel!!.transformedProperties!!, timeStarted)
                                    FlowController.naveToNextStep(this)
                                }
                            }
                        },
                        onRetry = {
                            feedbackText.value = flowStrings().nfcInitialFeedback
                            eventTypes.value = EventTypes.none
                            imageUrl.value = ""
                            // Make sure the reader is armed again for the next attempt
                            enableReaderModeSafely("retry")
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
        private const val TAG = "AssentifyNFC"

        /**
         * How often Android checks that the passport is still in the field while we talk to it.
         * On some chipsets a short delay collides with long reads (DG2) and causes TagLostException.
         * Tune this if logs show failures at "read DG2".
         */
        private const val PRESENCE_CHECK_DELAY_MS = 5000

        /**
         * Second registration after onResume. On the first entry the system can silently drop the
         * onResume registration ("Caller is not in foreground") or the previous screen's teardown
         * can cancel it. Registering again is harmless: it just replaces the previous one.
         */
        private const val DELAYED_REGISTER_MS = 700L

        private const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        fun start(context: Context) {
            val intent = Intent(context, NfcScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    private fun isResumedNow(): Boolean =
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    /** Registers reader mode. Safe to call many times; ignored when not resumed or NFC is off. */
    private fun enableReaderModeSafely(reason: String) {
        if (!isResumedNow()) {
            Log.d(TAG, "[$reason] Not resumed: reader mode not enabled")
            return
        }
        val adapter = NfcAdapter.getDefaultAdapter(this) ?: return
        if (!adapter.isEnabled) {
            Log.w(TAG, "[$reason] NFC is turned off: reader mode not enabled")
            return
        }
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS)
        }
        try {
            adapter.enableReaderMode(this, readerCallback, READER_FLAGS, options)
            Log.d(TAG, "[$reason] Reader mode enabled (presence check $PRESENCE_CHECK_DELAY_MS ms)")
        } catch (t: Throwable) {
            Log.e(TAG, "[$reason] enableReaderMode failed", t)
        }
    }

    private fun registerNfcStateReceiver() {
        if (isStateReceiverRegistered) return
        try {
            ContextCompat.registerReceiver(
                this,
                nfcStateReceiver,
                IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isStateReceiverRegistered = true
        } catch (t: Throwable) {
            Log.w(TAG, "Could not register NFC state receiver", t)
        }
    }

    private fun unregisterNfcStateReceiver() {
        if (!isStateReceiverRegistered) return
        try {
            unregisterReceiver(nfcStateReceiver)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not unregister NFC state receiver", t)
        }
        isStateReceiverRegistered = false
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
        registerNfcStateReceiver()
        enableReaderModeSafely("onResume")
        mainHandler.postDelayed(
            { enableReaderModeSafely("onResume +${DELAYED_REGISTER_MS}ms") },
            DELAYED_REGISTER_MS
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The window is really in front now: the most reliable moment to (re)register
        if (hasFocus) enableReaderModeSafely("window focus")
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacksAndMessages(null)
        unregisterNfcStateReceiver()
        // Must match enableReaderMode (disableForegroundDispatch does NOT turn reader mode off)
        try {
            NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this)
            Log.d(TAG, "Reader mode disabled")
        } catch (t: Throwable) {
            Log.w(TAG, "disableReaderMode failed", t)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Fallback path in case the system delivers the tag as an intent instead of through reader mode
        scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel)
    }

    /**  Events **/
    override fun onStartNfcScan() {
        runOnUiThread {
            feedbackText.value = flowStrings().nfcReading
            eventTypes.value = EventTypes.onSend
        }
    }

    override fun onCompleteNfcScan(dataModel: PassportResponseModel) {
        runOnUiThread {
            isComplete.value = true
            dataIDModel.value = dataModel
            feedbackText.value = ""
            OnCompleteScreenData.clear()
            OnCompleteScreenData.setData(dataModel.passportExtractedModel!!.transformedProperties)
            eventTypes.value = EventTypes.onComplete
            imageUrl.value = dataModel.passportExtractedModel?.imageUrl ?: ""
            val faces = dataModel.passportExtractedModel?.faces
            if (!faces.isNullOrEmpty()) {
                FlowController.setImage(faces.first())
            }
        }
    }

    override fun onErrorNfcScan(dataModel: PassportResponseModel, message: String) {
        Log.w(TAG, "NFC scan error: $message")
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

    val iconSvg = remember {
        loadSvgFromAssets(context, "ic_nfc.svg")
    }

    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(0.dp) }

    BaseBackgroundContainer(modifier = Modifier.fillMaxSize()) {

        if (eventTypes == EventTypes.onComplete) {
            val showResultPage = FlowController.getCurrentStep()!!.stepDefinition!!.customization.showResultPage
                ?: false
            if (showResultPage) {
                OnCompleteScreen(imageUrl, onNext = { onNext() })
            } else {
                OnNormalCompleteScreen(imageUrl, onNext = { onNext() })
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
                    IconButton(onClick = { onBack() }) {
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

                // Middle section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(180.dp),
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

                // Bottom section
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