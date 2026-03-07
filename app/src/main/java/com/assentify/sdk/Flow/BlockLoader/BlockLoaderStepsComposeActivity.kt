package com.assentify.sdk.Flow.BlockLoader

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.ApiKeyObject
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Core.Constants.BackgroundStyle
import com.assentify.sdk.Core.Constants.BackgroundType
import com.assentify.sdk.Core.Constants.BlockLoaderKeys
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Core.Constants.getCurrentDateTime
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LocalStepsObject
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.RemoteClient.Models.TenantThemeModel
import com.assentify.sdk.RemoteClient.RemoteClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


object BaseTheme {
    val BaseTextColor =
        Color(android.graphics.Color.parseColor(FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().textColor))
    val BaseSecondaryTextColor =
        Color(android.graphics.Color.parseColor(FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().secondaryTextColor))
    val FieldColor =
        Color(android.graphics.Color.parseColor(FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().backgroundCardColor))
    val BaseAccentColor =
        FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().accentColor;

    val BackgroundColor =
        FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().backgroundColor;
    val BaseClickColor =
        FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().clickColor;

    val BaseGreenColor = Color(android.graphics.Color.parseColor(ConstantsValues.DetectColor))
    val BaseRedColor = Color.Red

    val BaseLogo = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().logoUrl;
    val BaseBackgroundType =
        FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().backgroundType;
    val BaseBackgroundUrl =
        FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions().svgBackgroundImageUrl;

}

class BlockLoaderStepsComposeActivity : ComponentActivity() {
    lateinit var flowEnvironmentalConditions: FlowEnvironmentalConditions;
    private var isLoading by mutableStateOf(true)
    private var hasTheme by mutableStateOf(false)

    var firstInit = LocalStepsObject.getLocalSteps().isEmpty();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val interaction = intent.getStringExtra("interaction")


        setContent {
            if (isLoading) {
                if(hasTheme){
                    BaseBackgroundContainer(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LogoDotsLoader()
                        }
                    }
                }else{
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition()

                            repeat(3) { index ->

                                val delay = index * 200

                                val animatedScale by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 600,
                                            delayMillis = delay
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .scale(animatedScale)
                                        .background(
                                            Color.LightGray,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

            }else
            {
                flowEnvironmentalConditions = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions();

                val configModel = ConfigModelObject.getConfigModelObject()

                BlockLoaderScreen(
                    steps = buildStepsFromConfig(configModel),
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onStepClick = { /* navigate if needed */ },
                    onNext = {
                        /** Track Progress **/
                        if(firstInit){
                            val steps = LocalStepsObject.getLocalSteps();
                            val currentStep =
                                steps.find { it.stepDefinition!!.stepDefinition == StepsNames.BlockLoader }!!;
                            FlowController.trackProgress(
                                currentStep = currentStep,
                                response = null,
                                inputData = currentStep.submitRequestModel!!.extractedInformation,
                                status = "Completed"
                            )
                        }
                        /***/
                        FlowController.naveToNextStep(context = this)
                    }
                )
            }

        }

        getStart(interaction!!)
    }

    @Composable
    fun LogoDotsLoader() {

        val infiniteTransition = rememberInfiniteTransition()

        val scale by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            /// Logo
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(BaseTheme.BaseLogo)
                    .crossfade(true)
                    .build(),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(80.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(20.dp))

            /// Animated dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->

                    val delay = index * 200

                    val animatedScale by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = delay
                            ),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .scale(animatedScale)
                            .background(
                                BaseTheme.BaseTextColor,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
    /// API
    private fun getStart(interaction: String) {
        val remoteService = RemoteClient.remoteApiService
        val call = remoteService.getStart(interaction!!)
        call.enqueue(object : Callback<ConfigModel> {
            override fun onResponse(
                call: Call<ConfigModel>,
                response: Response<ConfigModel>
            ) {
                if (response.isSuccessful) {

                    val configModel = response.body()!!
                    ConfigModelObject.setConfigModelObject(configModel);
                    getTenantTheme(configModel)
                }
            }

            override fun onFailure(call: Call<ConfigModel>, t: Throwable) {
            }
        })
    }

    private fun getTenantTheme(configModel: ConfigModel) {
        val remoteService = RemoteClient.remoteApiService
        val call = remoteService.getTenantTheme(
            ApiKeyObject.getApiKeyObject(),
            "SDK",
            configModel!!.flowInstanceId,
            configModel!!.tenantIdentifier,
            configModel!!.blockIdentifier,
            configModel!!.instanceId,
            configModel!!.flowIdentifier,
            configModel!!.instanceHash,
            configModel!!.tenantIdentifier
        )
        call.enqueue(object : Callback<TenantThemeModel> {
            override fun onResponse(
                call: Call<TenantThemeModel>,
                response: Response<TenantThemeModel>
            ) {
                if (response.isSuccessful) {
                    val tenantThemeModel = response.body()!!
                    val flowEnvironmentalConditions = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions();
                    if (flowEnvironmentalConditions.logoUrl.isEmpty()) {
                        flowEnvironmentalConditions.logoUrl = tenantThemeModel!!.logoIcon!!;
                    }
                    if (flowEnvironmentalConditions.svgBackgroundImageUrl.isEmpty()) {
                        flowEnvironmentalConditions.svgBackgroundImageUrl =
                            "tenantThemeModel!!.svgBackgroundImageUrl!!";
                    }
                    if (flowEnvironmentalConditions.textColor.isEmpty()) {
                        flowEnvironmentalConditions.textColor = tenantThemeModel!!.textColor;
                    }
                    if (flowEnvironmentalConditions.secondaryTextColor.isEmpty()) {
                        flowEnvironmentalConditions.secondaryTextColor = tenantThemeModel!!.secondaryTextColor;
                    }
                    if (flowEnvironmentalConditions.backgroundCardColor.isEmpty()) {
                        flowEnvironmentalConditions.backgroundCardColor =
                            tenantThemeModel!!.backgroundCardColor;
                    }
                    if (flowEnvironmentalConditions.accentColor.isEmpty()) {
                        flowEnvironmentalConditions.accentColor = tenantThemeModel!!.accentColor;
                    }
                    if (flowEnvironmentalConditions.backgroundColor == null) {
                        if (flowEnvironmentalConditions.backgroundType == BackgroundType.Color) {
                            flowEnvironmentalConditions.backgroundColor =
                                BackgroundStyle.Solid(tenantThemeModel!!.backgroundBodyColor)
                        } else {
                            flowEnvironmentalConditions.backgroundColor =
                                BackgroundStyle.Solid(tenantThemeModel!!.backgroundCardColor)
                        }
                    }
                    if (flowEnvironmentalConditions.clickColor == null) {
                        flowEnvironmentalConditions.clickColor =
                            BackgroundStyle.Solid(tenantThemeModel!!.accentColor)
                    }
                    FlowEnvironmentalConditionsObject.setFlowEnvironmentalConditions(flowEnvironmentalConditions);
                    hasTheme = true;
                    getTemplatesByCountry()
                }

            }

            override fun onFailure(call: Call<TenantThemeModel>, t: Throwable) {
            }
        })
    }

    private fun getTemplatesByCountry() {
        val remoteService = RemoteClient.remoteIdPowerService
        val call: Call<List<Templates>> = remoteService.getTemplates()

        call.enqueue(object : Callback<List<Templates>> {
            override fun onResponse(
                call: Call<List<Templates>>,
                response: Response<List<Templates>>
            ) {
                if (response.isSuccessful) {
                    val remoteResult: List<Templates>? = response.body()
                    val filteredList = filterBySourceCountryCode(remoteResult)
                    val templatesByCountry = ArrayList<TemplatesByCountry>()
                    filteredList?.forEach { data ->
                        val item = TemplatesByCountry(
                            data.id,
                            data.sourceCountry,
                            data.sourceCountryCode,
                            data.sourceCountryFlag,
                            filterTemplatesCountryCode(remoteResult, data.sourceCountryCode)!!
                        )
                        templatesByCountry.add(item)
                    }
                    val sdkObject = AssentifySdkObject.getAssentifySdkObject()
                    sdkObject.allTemplates = templatesByCountry;
                    AssentifySdkObject.setAssentifySdkObject(sdkObject)
                   isLoading = false;
                }
            }

            override fun onFailure(call: Call<List<Templates>>, t: Throwable) {
            }
        })
    }

    private fun filterBySourceCountryCode(dataList: List<Templates>?): List<Templates>? {
        val filteredList = ArrayList<Templates>()
        val uniqueSourceCountryCodes = ArrayList<String>()
        dataList?.forEach { data ->
            if (!uniqueSourceCountryCodes.contains(data.sourceCountryCode)) {
                filteredList.add(data)
                uniqueSourceCountryCodes.add(data.sourceCountryCode)
            }
        }
        return filteredList
    }

    private fun filterTemplatesCountryCode(
        dataList: List<Templates>?,
        countryCode: String
    ): List<Templates>? {
        val filteredList = ArrayList<Templates>()
        dataList?.forEach { data ->
            if (data.sourceCountryCode == countryCode) {
                filteredList.add(data)
            }
        }
        return filteredList
    }

///

}




private fun buildStepsFromConfig(configModel: ConfigModel): List<LocalStepModel> {
    val tempList = LocalStepsObject.getLocalSteps();
    if (tempList.isEmpty()) {
        /** BlockLoader **/
        val flowEnvironmentalConditions =
            FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions();
        val values: MutableMap<String, String> = mutableMapOf()
        val initSteps = ConfigModelObject.getConfigModelObject().stepDefinitions
        initSteps.forEach { item ->
            if (item.stepDefinition == StepsNames.BlockLoader) {
                ///
                item.outputProperties.forEach { property ->
                    if (property.key.contains(BlockLoaderKeys.TimeStarted)) {
                        values.put(property.key, getCurrentDateTime())
                    }
                    if (property.key.contains(BlockLoaderKeys.DeviceName)) {
                        values.put(
                            property.key,
                            "${Build.MANUFACTURER} ${Build.MODEL}"
                        )
                    }
                    if (property.key.contains(BlockLoaderKeys.Application)) {
                        values.put(property.key, configModel.applicationId)
                    }
                    if (property.key.contains(BlockLoaderKeys.FlowName)) {
                        values.put(property.key, configModel.flowName)
                    }
                    if (property.key.contains(BlockLoaderKeys.InstanceHash)) {
                        values.put(property.key, configModel.instanceHash)
                    }
                    if (property.key.contains(BlockLoaderKeys.UserAgent)) {
                        val userAgent = System.getProperty("http.agent")
                            ?: "Android ${Build.VERSION.RELEASE}; ${Build.MODEL}"
                        values.put(property.key, userAgent)
                    }
                    if (property.key.contains(BlockLoaderKeys.InteractionID)) {
                        values.put(property.key, configModel.instanceId)
                    }
                }

                item.outputProperties.forEach { property ->
                    flowEnvironmentalConditions.blockLoaderCustomProperties.forEach { customProperties ->
                        if (property.key.contains(customProperties.key)) {
                            values.put(property.key, customProperties.value.toString())
                        }
                    }

                }
            }
        }
        tempList.add(
            LocalStepModel(
                name = StepsNames.BlockLoader,
                description = "",
                iconAssetPath = "",
                isDone = true,
                show = false,
                stepDefinition = configModel.stepDefinitions.first { it.stepDefinition == StepsNames.BlockLoader },
                submitRequestModel = SubmitRequestModel(
                    stepDefinition = StepsNames.BlockLoader,
                    stepId = configModel.stepDefinitions.first { it.stepDefinition == StepsNames.BlockLoader }.stepId,
                    extractedInformation = values
                )
            )
        )
        var displayCounter = 1

        configModel.stepMap.forEach { step ->
            val def = step.stepDefinition
            if (def == StepsNames.TermsConditions ||
                def == StepsNames.IdentificationDocumentCapture ||
                def == StepsNames.FaceImageAcquisition
                || def == StepsNames.AssistedDataEntry ||
                def == StepsNames.ContextAwareSigning
            ) {
                val meta = getStepMeta(def) ?: return@forEach
                tempList.add(
                    LocalStepModel(
                        name = "Step ${displayCounter}: ${meta.name}",
                        description = meta.description,
                        iconAssetPath = meta.icon,
                        isDone = false,
                        stepDefinition = configModel.stepDefinitions.filter { it.stepId == step.id }
                            .first(),
                        submitRequestModel = SubmitRequestModel(
                            stepDefinition = configModel.stepDefinitions.filter { it.stepId == step.id }
                                .first().stepDefinition,
                            stepId = configModel.stepDefinitions.filter { it.stepId == step.id }
                                .first().stepId,
                            extractedInformation = emptyMap()
                        )
                    )
                )
                displayCounter++
            }

            /* val isSplit = (def == StepsNames.Split)
             if(isSplit){
                 tempList.add(
                     LocalStepModel(
                         name = "",
                         description = "",
                         iconAssetPath = "",
                         show = false,
                         isDone = false,
                         stepDefinition = configModel.stepDefinitions.filter { it.stepId == step.id }
                             .first(),
                         submitRequestModel = SubmitRequestModel(
                             stepDefinition = configModel.stepDefinitions.filter { it.stepId == step.id }
                                 .first().stepDefinition,
                             stepId = configModel.stepDefinitions.filter { it.stepId == step.id }
                                 .first().stepId,
                             extractedInformation = emptyMap()
                         )
                     )
                 )
             }else{
                 tempList.add(
                     LocalStepModel(
                         name = "Step ${displayCounter}: ${meta.name}",
                         description = meta.description,
                         iconAssetPath = meta.icon,
                         isDone = false,
                         stepDefinition = configModel.stepDefinitions.filter { it.stepId == step.id }
                             .first(),
                         submitRequestModel = SubmitRequestModel(
                             stepDefinition = configModel.stepDefinitions.filter { it.stepId == step.id }
                                 .first().stepDefinition,
                             stepId = configModel.stepDefinitions.filter { it.stepId == step.id }
                                 .first().stepId,
                             extractedInformation = emptyMap()
                         )
                     )
                 )
                 displayCounter++
             }*/
        }
        LocalStepsObject.setLocalSteps(tempList)

        val steps = LocalStepsObject.getLocalSteps();
        val currentStep =
            steps.find { it.stepDefinition!!.stepDefinition == StepsNames.BlockLoader }!!;
        FlowController.trackProgress(
            currentStep = currentStep,
            response = null,
            inputData = currentStep.submitRequestModel!!.extractedInformation,
            status = "InProgress"
        )
    }

    return tempList.filter { it.show }
}

data class StepMeta(
    val name: String,
    val description: String,
    val icon: String
)

fun getStepMeta(stepDefinition: String): StepMeta? = when (stepDefinition) {
    StepsNames.TermsConditions -> StepMeta(
        name = "Terms & Conditions",
        description = "Read and accept the Terms & Conditions.",
        icon = "ic_terms_step.svg"
    )

    StepsNames.IdentificationDocumentCapture -> StepMeta(
        name = "Scan Your ID",
        description = "Take a photo of your national ID or passport to verify your identity.",
        icon = "ic_id_step.svg"
    )

    StepsNames.FaceImageAcquisition -> StepMeta(
        name = "Take Selfie",
        description = "Use your camera to securely confirm your identity with a quick selfie scan.",
        icon = "ic_face_step.svg"
    )

    StepsNames.AssistedDataEntry -> StepMeta(
        name = "Information Capture",
        description = "Provide basic personal details like name, address, and employment info.",
        icon = "ic_data_entry_step.svg"
    )

    StepsNames.ContextAwareSigning -> StepMeta(
        name = "eKYC Signing",
        description = "Provide a digital signature to complete onboarding.",
        icon = "ic_signing_step.svg"
    )

    else -> null
}