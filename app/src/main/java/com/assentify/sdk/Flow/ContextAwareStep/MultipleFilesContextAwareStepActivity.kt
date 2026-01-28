package com.assentify.sdk.Flow.ContextAwareStep

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
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

/// TODO TEST ALL WITH Multiple Files
data class SelectedTemplatesTokens(
    val templateId: Int,
    val templateName: String,
    val documentTokens: List<TokensMappings>,
)


data class DocumentWithTokensModel(
    val templateId: Int,
    val createUserDocumentResponseModel: CreateUserDocumentResponseModel,
)

data class DocumentWithTokensAndSinged(
    val templateId: Int,
    val signatureResponseModel: SignatureResponseModel,
)

class MultipleFilesContextAwareStepActivity : FragmentActivity(), ContextAwareSigningCallback {

    private var contextAwareStepEventTypes =
        mutableStateOf<String>(ContextAwareStepEventTypes.onSend)
    private var contextAwareSigningObject = mutableStateOf<ContextAwareSigningModel?>(null)
    private var currentTemplateId = mutableStateOf<Int?>(null)

    private var selectedTemplates = mutableStateListOf<SelectedTemplatesTokens>()

    private var documentWithTokensObject = mutableStateListOf<DocumentWithTokensModel?>()

    private var documentWithTokensAndSinged = mutableStateListOf<DocumentWithTokensAndSinged?>()

    var approvedDocumentsObject = mutableStateListOf<DocumentWithTokensModel?>()

    private var signatureObject = mutableStateOf<String?>(null)

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
                FlowController.backClick(this@MultipleFilesContextAwareStepActivity);
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MultipleFilesContextAwareStepScreen(
                        onBack = {
                            onBackPressedDispatcher.onBackPressed()
                        },
                        onNext = {
                            val extractedInformation = mutableMapOf<String, String>()
                            for (outputProperty in FlowController.getCurrentStep()!!.stepDefinition!!.outputProperties) {
                                if (outputProperty.key.contains("OnBoardMe_ContextAwareSigning_DocumentURL")) {
                                    extractedInformation[outputProperty.key] =
                                        documentWithTokensAndSinged.first()!!.signatureResponseModel.signedDocumentUri
                                }
                            }
                            FlowController.makeCurrentStepDone(extractedInformation)
                            FlowController.naveToNextStep(context = this)

                        },
                        onSign = { signature, approvedDocuments ->
                            contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onSend
                            approvedDocumentsObject.addAll(approvedDocuments);
                            signatureObject.value = signature
                            contextAwareSigning.signature(
                                approvedDocuments.first()!!.createUserDocumentResponseModel.templateInstanceId,
                                approvedDocuments.first()!!.createUserDocumentResponseModel.documentId,
                                signature
                            )


                        },
                        onCreateUserDocumentResponseModel = {
                            contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onSend
                            val tokenValues = mutableMapOf<String, String>()
                            it.documentTokens.forEach { token ->
                                tokenValues[token.tokenId.toString()] =
                                    getValueByKey(token.sourceKey)
                            }
                            currentTemplateId.value = it.templateId;
                            contextAwareSigning.createUserDocumentInstance(
                                it.templateId,
                                tokenValues
                            )
                        },
                        eventTypes = contextAwareStepEventTypes.value,
                        contextAwareSigningObject = contextAwareSigningObject.value,
                        selectedTemplates = selectedTemplates,
                        documentWithTokensObject = documentWithTokensObject,
                        documentWithTokensAndSinged = documentWithTokensAndSinged,
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, MultipleFilesContextAwareStepActivity::class.java)
            context.startActivity(intent)
        }
    }

    private fun getValueByKey(key: String): String {
        val doneList = FlowController.getAllDoneSteps();
        doneList.forEach { step ->
            for (info in step.submitRequestModel!!.extractedInformation) {
                if (info.key == key) {
                    return info.value;
                }
            }
        }
        return "";
    }

    override fun onHasTokens(
        templateId: Int,
        documentTokens: List<TokensMappings>,
        contextAwareSigningModel: ContextAwareSigningModel?
    ) {

        contextAwareSigningObject.value = contextAwareSigningModel;
        selectedTemplates.add(
            SelectedTemplatesTokens(
                templateId, "Agreement", documentTokens
            )
        );

        if (contextAwareSigningObject.value!!.data.selectedTemplates.size == selectedTemplates.size) {
            contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onTokensComplete
        }
    }


    override fun onCreateUserDocumentInstance(userDocumentResponseModel: CreateUserDocumentResponseModel) {
        documentWithTokensObject.add(
            DocumentWithTokensModel(
                currentTemplateId.value!!,
                userDocumentResponseModel
            )
        )

        contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onTokensComplete
    }

    override fun onSignature(signatureResponseModel: SignatureResponseModel) {
        documentWithTokensAndSinged.add(
            DocumentWithTokensAndSinged(
                approvedDocumentsObject.first()!!.templateId,
                signatureResponseModel
            )
        )
        approvedDocumentsObject.removeFirstOrNull()
        if (contextAwareSigningObject.value!!.data.selectedTemplates.size == documentWithTokensAndSinged.size) {
            contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onSignature
        } else {
            contextAwareSigning.signature(
                approvedDocumentsObject.first()!!.createUserDocumentResponseModel.templateInstanceId,
                approvedDocumentsObject.first()!!.createUserDocumentResponseModel.documentId,
                signatureObject.value!!
            )
        }
    }

    override fun onError(message: String) {
        contextAwareStepEventTypes.value = ContextAwareStepEventTypes.onError
    }

}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun MultipleFilesContextAwareStepScreen(
    onBack: () -> Unit = {},
    onCreateUserDocumentResponseModel: (SelectedTemplatesTokens) -> Unit = {},
    onNext: () -> Unit = {},
    onSign: (String, MutableList<DocumentWithTokensModel?>) -> Unit = { _, _ -> },
    eventTypes: String,
    contextAwareSigningObject: ContextAwareSigningModel?,
    selectedTemplates: MutableList<SelectedTemplatesTokens>,
    documentWithTokensObject: MutableList<DocumentWithTokensModel?>,
    documentWithTokensAndSinged: MutableList<DocumentWithTokensAndSinged?>,
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    var checked by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<SelectedTemplatesTokens?>(null) }

    val approvedDocuments = remember {
        mutableStateListOf<DocumentWithTokensModel?>()
    }
    var showTemplates by remember { mutableStateOf(false) }

    var signatureB64 by remember { mutableStateOf<String?>("") }

    BaseBackgroundContainer(
        modifier = Modifier
            .fillMaxSize()
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

            ProgressStepper(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp) // below header
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp) // leave room for bottom button
                    .verticalScroll(rememberScrollState())
            ) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp



                if (eventTypes == ContextAwareStepEventTypes.onSend) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight - 200.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            color =   BaseTheme.BaseTextColor,
                            strokeWidth = 6.dp
                        )
                    }
                }


                /// END

                if (eventTypes == ContextAwareStepEventTypes.onTokensComplete || eventTypes == ContextAwareStepEventTypes.onSignature) {

                    if (selectedTemplate == null || documentWithTokensObject.find { selectedTemplate!!.templateId == it?.templateId } == null) {

                        /// TOP Screen
                        Text(
                            text = contextAwareSigningObject!!.data.header!!,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            color =   BaseTheme.BaseTextColor,
                            fontSize = 23.sp,
                            lineHeight = 34.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 25.dp)
                        )

                        if (contextAwareSigningObject.data.subHeader != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = contextAwareSigningObject.data.subHeader,
                                color =   BaseTheme.BaseTextColor,
                                fontSize = 15.sp,
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 25.dp)
                            )
                        }

                        if (contextAwareSigningObject.data.confirmationMessage != null) {
                            val confirmationMessage =
                                remember(contextAwareSigningObject!!.data.confirmationMessage) {
                                    contextAwareSigningObject.data.confirmationMessage
                                        ?.let { raw ->
                                            removeHtml(raw)
                                                .replace(Regex("\\s*\\n\\s*"), " ")
                                                .replace(Regex("\\s+"), " ")
                                                .trim()
                                        }
                                        ?: ""
                                }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = confirmationMessage,
                                color =   BaseTheme.BaseTextColor,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 25.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) { /// Center Screen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {

                                Checkbox(
                                    modifier = Modifier.padding(0.dp),
                                    checked = checked,
                                    onCheckedChange = {
                                        if (approvedDocuments.size != selectedTemplates.size) {
                                            checked = it

                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(
                                            android.graphics.Color.parseColor(
                                                BaseTheme.BaseAccentColor
                                            )
                                        ),
                                        uncheckedColor = Color(
                                            android.graphics.Color.parseColor(
                                                BaseTheme.BaseAccentColor
                                            )
                                        ),
                                        checkmarkColor = BaseTheme.BaseTextColor,
                                    )
                                )

                                //  Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "I agree to the terms and conditions",
                                    color =   BaseTheme.BaseTextColor,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = InterFont,
                                    fontWeight = FontWeight.Light,
                                    textAlign = TextAlign.Start,
                                )
                            }
                            Text(
                                text = if (approvedDocuments.size != selectedTemplates.size) "Please review and approve the below files" else "Thank you for approving",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Bold,
                                color =   BaseTheme.BaseTextColor,
                                fontSize = 15.sp,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 25.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            if (approvedDocuments.size != selectedTemplates.size) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .padding(horizontal = 25.dp),
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    itemsIndexed(selectedTemplates) { index, document ->
                                        DocumentRow(
                                            title = document.templateName,
                                            isActive = checked,
                                            isApproved = approvedDocuments.any { it!!.templateId == document.templateId },
                                            onClick = {
                                                if (checked) {
                                                    selectedTemplate = document
                                                    if (documentWithTokensObject.find { selectedTemplate!!.templateId == it?.templateId } == null) {
                                                        onCreateUserDocumentResponseModel(
                                                            document
                                                        )
                                                    }
                                                }

                                            }
                                        )
                                    }
                                }
                            } else {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 25.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            BaseTheme.FieldColor
                                        )
                                        .clickable { showTemplates = !showTemplates }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Files Reviewed and Approved",
                                        color = BaseTheme.BaseTextColor, // green text like image
                                        fontFamily = InterFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Icon(
                                        imageVector = if (showTemplates) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BaseTheme.BaseTextColor
                                    )
                                }
                                AnimatedVisibility(
                                    visible = showTemplates,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    /// Center Screen
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .padding(horizontal = 25.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.Top
                                    ) {
                                        itemsIndexed(selectedTemplates) { index, document ->
                                            DocumentRow(
                                                title = document.templateName,
                                                isActive = checked,
                                                isApproved = approvedDocuments.any { it!!.templateId == document.templateId },
                                                onClick = {
                                                    if (checked) {
                                                        selectedTemplate = document
                                                        if (documentWithTokensObject.find { selectedTemplate!!.templateId == it?.templateId } == null) {
                                                            onCreateUserDocumentResponseModel(
                                                                document
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    /// END
                                }

                            }

                            /// END


                        }
                    }


                    if (selectedTemplate != null && documentWithTokensAndSinged.find { selectedTemplate!!.templateId == it?.templateId } != null && eventTypes == ContextAwareStepEventTypes.onSignature) {
                        DocumentPageFromUrl(
                            url = documentWithTokensAndSinged.find { selectedTemplate!!.templateId == it?.templateId }!!.signatureResponseModel.signedDocumentUri,
                            onCancel = {
                                selectedTemplate = null
                            }
                        )
                    }

                    if (selectedTemplate != null && documentWithTokensObject.find { selectedTemplate!!.templateId == it?.templateId } != null && eventTypes != ContextAwareStepEventTypes.onSignature) {
                        DocumentPage(
                            userDocumentResponseModel = documentWithTokensObject.find { selectedTemplate!!.templateId == it?.templateId }!!.createUserDocumentResponseModel,
                            onAccept = {
                                approvedDocuments.add(
                                    DocumentWithTokensModel(
                                        selectedTemplate!!.templateId,
                                        documentWithTokensObject.find { selectedTemplate!!.templateId == it?.templateId }!!.createUserDocumentResponseModel

                                    )
                                )
                                selectedTemplate = null
                            },
                            isApproved = approvedDocuments.any { it!!.templateId == selectedTemplate!!.templateId },
                            onCancel = {
                                selectedTemplate = null
                            }
                        )
                    }
                    if (selectedTemplate == null && approvedDocuments.size == selectedTemplates.size && eventTypes != ContextAwareStepEventTypes.onSignature) {
                        Spacer(Modifier.height(30.dp))
                        SignaturePad(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .padding(horizontal = 25.dp),
                            onConfirmBase64 = { signature ->
                                signatureB64 = signature;
                            }
                        )
                    }
                    // Spacer(Modifier.height(30.dp))
                    // ============ BOTTOM ============ //

                }

            }
        }
        if (selectedTemplate == null && eventTypes == ContextAwareStepEventTypes.onTokensComplete) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.Transparent)

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Button(
                        onClick = {
                            if (signatureB64!!.isNotEmpty()) {
                                onSign(signatureB64!!, approvedDocuments)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 25.dp, horizontal = 25.dp) .background(
                                brush = if (signatureB64!!.isNotEmpty())
                                    BaseTheme.BaseClickColor!!.toBrush()
                                else
                                    SolidColor(BaseTheme.FieldColor),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Text(
                            "Accept Terms & Sign",
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Normal,
                            color = BaseTheme.BaseSecondaryTextColor,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }


        }
        if (selectedTemplate == null && eventTypes == ContextAwareStepEventTypes.onSignature) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.Transparent)

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Button(
                        onClick = {
                            onNext()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 25.dp, horizontal = 25.dp)  .background(
                                brush = BaseTheme.BaseClickColor!!.toBrush(),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Text(
                            "Next",
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Normal,
                            color = BaseTheme.BaseSecondaryTextColor,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }


        }
    }

}


fun removeHtml(value: String): String {
    return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
}

@Composable
fun DocumentRow(
    title: String,
    isApproved: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                BaseTheme.FieldColor
            } else {
                BaseTheme.FieldColor
                    .copy(alpha = 0.3f) // 10% opacity
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left icon
            if (isActive)
                Icon(
                    imageVector = if (isApproved) Icons.Default.CheckCircleOutline else Icons.Default.FileOpen,
                    contentDescription = null,
                    tint = if (isApproved) BaseTheme.BaseGreenColor else Color(
                        android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)
                    ),
                    modifier = if (isApproved) Modifier.size(30.dp) else Modifier.size(22.dp)
                )

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Text(
                text = title,
                fontFamily = InterFont,
                color = if (isActive) BaseTheme.BaseTextColor else BaseTheme.BaseTextColor.copy(alpha = 0.3f),
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            // Right eye icon
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = if (isActive) Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)) else Color(
                    android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)
                ).copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DocumentPage(
    userDocumentResponseModel: CreateUserDocumentResponseModel,
    isApproved: Boolean,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    PdfViewerFromBase64(
        base64Data = userDocumentResponseModel!!.templateInstance,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isApproved) LocalConfiguration.current.screenHeightDp.dp - 280.dp else LocalConfiguration.current.screenHeightDp.dp - 340.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 25.dp)
            .border(1.dp, Color.Gray)
    )

    if (!isApproved)
        Spacer(Modifier.height(20.dp))
    if (!isApproved)
        Button(
            onClick = {
                onAccept()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 25.dp)
                .background(
                    brush = BaseTheme.BaseClickColor!!.toBrush(),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Text(
                "Approve",
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal,
                color = BaseTheme.BaseSecondaryTextColor,
                modifier = Modifier.padding(vertical = 7.dp)
            )
        }
    Spacer(Modifier.height(15.dp))
    Button(
        onClick = {
            onCancel()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 25.dp)
            .border(
                1.dp,
                Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                RoundedCornerShape(999.dp)
            )
    ) {
        Text(
            "Cancel",
            fontFamily = InterFont,
            color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(vertical = 7.dp)
        )
    }
}

@Composable
fun DocumentPageFromUrl(
    url: String,
    onCancel: () -> Unit
) {

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    PdfViewerFromUrl(
        url = url,
        fileName = "SignedDocument.pdf",
        modifier = Modifier
            .fillMaxWidth()
            .height(LocalConfiguration.current.screenHeightDp.dp - 280.dp)
            .padding(horizontal = 15.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray)
    )


    Spacer(Modifier.height(15.dp))
    Button(
        onClick = {
            onCancel()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = BaseTheme.BaseTextColor
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 25.dp)
            .border(
                1.dp,
                Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                RoundedCornerShape(999.dp)
            )
    ) {
        Text(
            "Cancel",
            fontFamily = InterFont,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(vertical = 7.dp)
        )
    }
}