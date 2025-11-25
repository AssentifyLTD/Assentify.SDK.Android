package com.assentify.sdk.Flow.ContextAwareStep

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.ReusableComposable.Events.ContextAwareStepEventTypes
import com.assentify.sdk.Flow.ReusableComposable.PdfViewerFromBase64
import com.assentify.sdk.Flow.ReusableComposable.PdfViewerFromUrl
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.Flow.ReusableComposable.SignaturePad
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel
import com.assentify.sdk.RemoteClient.Models.TokensMappings


class ContextAwareStepActivity : FragmentActivity(), ContextAwareSigningCallback {

    private var contextAwareStepEventTypes =
        mutableStateOf<String>(ContextAwareStepEventTypes.onSend)
    private var contextAwareSigningObject = mutableStateOf<ContextAwareSigningModel?>(null)
    private var userDocumentResponseObject = mutableStateOf<CreateUserDocumentResponseModel?>(null)
    private var signatureResponseObject = mutableStateOf<SignatureResponseModel?>(null)

    private lateinit var contextAwareSigning: ContextAwareSigning;

    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        contextAwareSigning = assentifySdk.startContextAwareSigning(
            this,
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@ContextAwareStepActivity);
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContextAwareStepScreen(
                        onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onNext = {
                            if (contextAwareStepEventTypes.value == ContextAwareStepEventTypes.onSignature) {
                                val extractedInformation = mutableMapOf<String, String>()
                                for (outputProperty in FlowController.getCurrentStep()!!.stepDefinition!!.outputProperties) {
                                    if (outputProperty.key.contains("OnBoardMe_ContextAwareSigning_DocumentURL")) {
                                        extractedInformation[outputProperty.key] =
                                            signatureResponseObject.value!!.signedDocumentUri
                                    }
                                }
                                FlowController.makeCurrentStepDone(extractedInformation)
                                FlowController.naveToNextStep(context = this)
                            } else {
                                contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onSend
                                contextAwareSigning.signature(
                                    userDocumentResponseObject.value!!.templateInstanceId,
                                    userDocumentResponseObject.value!!.documentId,
                                    it
                                )
                            }

                        },
                        eventTypes = contextAwareStepEventTypes.value,
                        contextAwareSigningObject = contextAwareSigningObject.value,
                        userDocumentResponseObject = userDocumentResponseObject.value,
                        signatureResponseObject = signatureResponseObject.value,
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, ContextAwareStepActivity::class.java)
            context.startActivity(intent)
        }
    }
    private fun getValueByKey(key: String): String {
        val doneList = FlowController.getAllDoneSteps();
        doneList.forEach { step ->
                for (info in step.submitRequestModel!!.extractedInformation) {
                    if (info.key == key) {
                       return  info.value;
                    }
                }
        }
        return "";
    }

    override fun onHasTokens(
        documentTokens: List<TokensMappings>,
        contextAwareSigningModel: ContextAwareSigningModel?
    ) {
        val tokenValues = mutableMapOf<String, String>()
        documentTokens.forEach {
            tokenValues[it.tokenId.toString()] = getValueByKey(it.sourceKey)
        }
        contextAwareSigningObject.value = contextAwareSigningModel;
        contextAwareSigning.createUserDocumentInstance(tokenValues)
    }


    override fun onCreateUserDocumentInstance(userDocumentResponseModel: CreateUserDocumentResponseModel) {
        userDocumentResponseObject.value = userDocumentResponseModel
        contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onTokensComplete
    }

    override fun onSignature(signatureResponseModel: SignatureResponseModel) {
        signatureResponseObject.value = signatureResponseModel
        contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onSignature
    }

    override fun onError(message: String) {
        contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onError
    }

}

@Composable
fun ContextAwareStepScreen(
    onBack: () -> Unit = {},
    onNext: (String) -> Unit = {},
    eventTypes: String,
    contextAwareSigningObject: ContextAwareSigningModel?,
    userDocumentResponseObject: CreateUserDocumentResponseModel?,
    signatureResponseObject: SignatureResponseModel?,
) {


    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }


    var signatureB64 by remember { mutableStateOf<String?>("") }

    var checked by remember { mutableStateOf(false) }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
    ) {
        // ============ TOP ============ //
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
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
                    .padding(horizontal = 0.dp, vertical = 6.dp)
            )
        }

        // ============ MIDDLE ============ //
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp) // below header
        ) {
            // 🔹 Scrollable column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 50.dp) // leave room for bottom button
                    .verticalScroll(rememberScrollState())
            ) {
                when (eventTypes) {
                    ContextAwareStepEventTypes.onSend -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp), // keep centered during scroll
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = Color.White,
                                strokeWidth = 6.dp
                            )
                        }
                    }

                    ContextAwareStepEventTypes.onTokensComplete -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = contextAwareSigningObject!!.data.header!!,
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (contextAwareSigningObject.data.subHeader != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = contextAwareSigningObject.data.subHeader,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(30.dp))

                            PdfViewerFromBase64(
                                base64Data = userDocumentResponseObject!!.templateInstance,
                                modifier = if (checked) Modifier
                                    .width(200.dp)
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray) else Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .padding(horizontal = 15.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray)
                            )

                            if (contextAwareSigningObject.data.confirmationMessage != null && !checked) {
                                Spacer(Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {


                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { checked = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(
                                                android.graphics.Color.parseColor(
                                                    flowEnv.listItemsSelectedHexColor
                                                )
                                            ),
                                            uncheckedColor = Color(
                                                android.graphics.Color.parseColor(
                                                    flowEnv.listItemsSelectedHexColor
                                                )
                                            ),
                                            checkmarkColor = Color.White
                                        )
                                    )

                                    Text(
                                        text = contextAwareSigningObject.data.confirmationMessage,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))
                            if (checked) {
                                SignaturePad(
                                    cardColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                                    confirmColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp),
                                    onConfirmBase64 = { signature ->
                                        signatureB64 = signature;
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    ContextAwareStepEventTypes.onSignature -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = contextAwareSigningObject!!.data.header!!,
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (contextAwareSigningObject.data.subHeader != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = contextAwareSigningObject.data.subHeader,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(30.dp))

                            PdfViewerFromUrl(
                                url = signatureResponseObject!!.signedDocumentUri,
                                fileName = "SignedDocument.pdf",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .padding(horizontal = 15.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray)
                            )


                            Spacer(Modifier.height(12.dp))

                        }
                    }
                }
            }
        }


        // ============ BOTTOM ============ //
        if (eventTypes == ContextAwareStepEventTypes.onTokensComplete || eventTypes == ContextAwareStepEventTypes.onSignature) {
            Button(
                onClick = {
                    onNext(signatureB64!!)
                },
                enabled = signatureB64!!.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor)),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    if (eventTypes == ContextAwareStepEventTypes.onTokensComplete) "Accept & Sign" else "Next",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }

}