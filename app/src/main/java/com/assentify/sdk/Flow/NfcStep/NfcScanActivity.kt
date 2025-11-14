package com.assentify.sdk.Flow.NfcStep

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.Events.OnCompleteScreen
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.NfcPassportResponseModelObject
import com.assentify.sdk.ScanNFC.ScanNfc
import com.assentify.sdk.ScanNFC.ScanNfcCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel


class NfcScanActivity : FragmentActivity(), ScanNfcCallback {

    private lateinit var scanNfc: ScanNfc
    private lateinit var passportResponseModel: PassportResponseModel
    private var eventTypes = mutableStateOf<String>(EventTypes.none)
    private var imageUrl = mutableStateOf<String>("")
    private var feedbackText = mutableStateOf("Position the passport on the bottom of the phone where the NFC chip reader is and ensure that you have the passport close enough for detection and reading.")
    private var dataIDModel = mutableStateOf<PassportResponseModel?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
        val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
       passportResponseModel =  NfcPassportResponseModelObject.getPassportResponseModelObject()



        scanNfc = assentifySdk.startScanNfc(
            this,
            languageCode = flowEnv.language,
            context = this
        )

        if (scanNfc.isNfcSupported(activity = this)) {
            if (scanNfc.isNfcEnabled(activity = this)) {
                //
            } else {
                val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                startActivity(intent)
            }
        } else {
            feedbackText.value = "NFC Not supported on this device."
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@NfcScanActivity);
            }
        })

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
                        onNext = {
                            FlowController.makeCurrentStepDone(dataIDModel.value!!.passportExtractedModel!!.transformedProperties!!);
                            FlowController.naveToNextStep(this)
                        },
                        onRetry = {
                            feedbackText.value = "Position the passport on the bottom of the phone where the NFC chip reader is and ensure that you have the passport close enough for detection and reading.";
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
            val intent = Intent(context, NfcScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter != null) {
            val intent = Intent(applicationContext, this.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            val filter = arrayOf(arrayOf(ConstantsValues.NfcTechTag))
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        adapter?.disableForegroundDispatch(this)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
      scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel)
    }

    /**  Events **/
    override fun onStartNfcScan() {
        runOnUiThread {
            feedbackText.value = "Nfc reading..."
            eventTypes.value = EventTypes.onSend
        }
    }

    override fun onCompleteNfcScan(dataModel: PassportResponseModel) {
        runOnUiThread {
            dataIDModel.value = dataModel;
            feedbackText.value = ""
            eventTypes.value = EventTypes.onComplete
            imageUrl.value = dataModel.passportExtractedModel!!.imageUrl!!
        }
    }

    override fun onErrorNfcScan(dataModel: PassportResponseModel, message: String) {
        runOnUiThread {
            feedbackText.value = "Connection lost. Keep the phone still on the passport and try again."
            eventTypes.value = EventTypes.onError
        }
    }




}

@Composable
fun NfcScanScreen(
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    onRetry: () -> Unit = {},
    eventTypes: String,
    imageUrl: String,
    feedbackText: String,
) {

    val context = LocalContext.current

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }



    val iconSvg= remember {
        loadSvgFromAssets(context, "ic_nfc.svg")
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))) {

       if (eventTypes == EventTypes.onComplete) {
           OnCompleteScreen(imageUrl, onNext = {
               onNext();
           })
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

        if(eventTypes != EventTypes.onComplete)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 150.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Text(
                text = "NFC Based Capture",
                color = Color.White,
                fontSize = 24.sp,
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
                        .size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconSvg?.let {
                        Icon(
                            painter = it,
                            contentDescription = "ic_nfc",
                            modifier = Modifier.size(240.dp),
                            tint = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                if(eventTypes == EventTypes.onSend){
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.CenterHorizontally),
                        color = Color.White,
                        strokeWidth = 6.dp
                    )
                }else{
                    Text(
                        "NFC DETECTED",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }


                Spacer(Modifier.height(12.dp))

                Text(
                    text = feedbackText,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            // 🔹 Bottom section
            val isError = eventTypes == EventTypes.onError
           if(isError){
               Button(
                   onClick = {onRetry() },
                   colors = ButtonDefaults.buttonColors(
                       containerColor = Color.Red,
                       contentColor = Color.White
                   ),
                   shape = RoundedCornerShape(28.dp),
                   modifier = Modifier
                       .align(Alignment.BottomCenter)
                       .padding(bottom = 20.dp)
                       .fillMaxWidth()
                       .height(54.dp)
               ) {
                   Text(
                       text =  "Retry" ,
                       fontWeight = FontWeight.Bold,
                       fontSize = 16.sp
                   )
               }
           }
        }





    }
}


