package com.assentify.sdk.Flow.BlockLoader

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Core.Constants.BlockLoaderKeys
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Core.Constants.getCurrentDateTime
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LocalStepsObject
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel


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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flowEnvironmentalConditions =
            FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions();

        val configModel = ConfigModelObject.getConfigModelObject()


        setContent {
            BlockLoaderScreen(
                steps = buildStepsFromConfig(configModel),
                onBack = { onBackPressedDispatcher.onBackPressed() },
                onStepClick = { /* navigate if needed */ },
                onNext = {
                    FlowController.naveToNextStep(context = this)
                }
            )
        }
    }


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
            status = "Completed"
        )
    } else {
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