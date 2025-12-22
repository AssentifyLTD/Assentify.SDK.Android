package com.assentify.sdk.Flow.FlowController

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.assentify.sdk.ApiKeyObject
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Core.Constants.WrapUpKeys
import com.assentify.sdk.Core.Constants.getCurrentDateTime
import com.assentify.sdk.Flow.AssistedDataEntryStep.AssistedDataEntryActivity
import com.assentify.sdk.Flow.BlockLoader.BlockLoaderStepsComposeActivity
import com.assentify.sdk.Flow.ContextAwareStep.ContextAwareStepActivity
import com.assentify.sdk.Flow.FaceStep.HowToCaptureFaceActivity
import com.assentify.sdk.Flow.IDStep.IDStepComposeActivity
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.Flow.SubmitStep.SubmitStepActivity
import com.assentify.sdk.Flow.Terms.TermsAndConditionsComposeActivity
import com.assentify.sdk.IDImageObject
import com.assentify.sdk.LocalStepsObject
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FlowController {


    fun naveToNextStep(context: Context) {
        val currentStep = getCurrentStep();
        if (currentStep == null) {
            val intent = Intent(context, SubmitStepActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        } else {
            when (currentStep.stepDefinition!!.stepDefinition) {
                StepsNames.TermsConditions -> {
                    TermsAndConditionsComposeActivity.start(context = context)
                }

                StepsNames.IdentificationDocumentCapture -> {
                    IDStepComposeActivity.start(context = context)
                }

                StepsNames.FaceImageAcquisition -> {
                    HowToCaptureFaceActivity.start(context = context)
                }

                StepsNames.AssistedDataEntry -> {
                    AssistedDataEntryActivity.start(context = context)
                }

                StepsNames.ContextAwareSigning -> {
                    ContextAwareStepActivity.start(context = context)
                }
            }
        }

    }

    /** Split TODO LATER */
    
 /*   @RequiresApi(Build.VERSION_CODES.O)
    fun chekSplitStepAndMoveNext(context: Context) {
        val currentStep = getCurrentStep();
        val configModelObject = ConfigModelObject.getConfigModelObject();
        val isSplit =
            currentStep != null && currentStep.stepDefinition!!.stepDefinition == StepsNames.Split;
        if (isSplit) {

            val branches = currentStep.stepDefinition
                .customization
                .branches
                ?: emptyList()

            val matchedBranch = branches.firstOrNull { branch ->
                ConditionEvaluator.evaluateBranch(
                    branch = branch,
                )
            } ?: branches.firstOrNull { it.branchIndex == 0 }


            val splitStep = configModelObject.stepMap.first { it.id == currentStep.stepDefinition.stepId }
            val splitBranches = splitStep.branches
            matchedBranch?.let { branch ->
                when (branch.branchIndex) {
                    0 -> {

                        makeCurrentStepDone(emptyMap())
                        val steps = LocalStepsObject.getLocalSteps()
                        val newItems:MutableList<LocalStepModel>  = mutableListOf();
                        val hasWrapUp = splitBranches?.get(0)!!.firstOrNull() { it.stepDefinition == StepsNames.WrapUp }
                        if(hasWrapUp!=null){
                            steps.removeIf { !it.isDone }
                        }
                        val insertIndex = steps.indexOfFirst { !it.isDone }
                            .takeIf { it != -1 } ?: steps.size

                        var displayCounter = steps.filter { it.show && it.isDone }.size + 1;
                        splitBranches[0].forEach { splitBranch ->
                            val def = splitBranch.stepDefinition
                            if (def != StepsNames.WrapUp) {
                                val meta = getStepMeta(def) ?: return@forEach
                                newItems.add(
                                    LocalStepModel(
                                        name = "Step ${displayCounter}: ${meta.name}",
                                        description = meta.description,
                                        iconAssetPath = meta.icon,
                                        isDone = false,
                                        stepDefinition = configModelObject.stepDefinitions.first { it.stepId == splitBranch.id },
                                        submitRequestModel = SubmitRequestModel(
                                            stepDefinition = configModelObject.stepDefinitions.first { it.stepId == splitBranch.id }.stepDefinition,
                                            stepId = configModelObject.stepDefinitions.first { it.stepId == splitBranch.id }.stepId,
                                            extractedInformation = emptyMap()
                                        )
                                    )
                                )
                                displayCounter++
                            }

                        }
                        steps.addAll(insertIndex, newItems)
                        LocalStepsObject.setLocalSteps(steps)
                        naveToNextStep(context)

                    }

                    else -> {
                        makeCurrentStepDone(emptyMap())
                        val steps = LocalStepsObject.getLocalSteps()
                        val newItems:MutableList<LocalStepModel>  = mutableListOf();
                        val hasWrapUp = splitBranches?.get(branch.branchIndex)!!.firstOrNull() { it.stepDefinition == StepsNames.WrapUp }
                        if(hasWrapUp!=null){
                            steps.removeIf { !it.isDone }
                        }
                        val insertIndex = steps.indexOfFirst { !it.isDone }
                            .takeIf { it != -1 } ?: steps.size
                        var displayCounter = steps.filter { it.show && it.isDone }.size + 1;
                        splitBranches[branch.branchIndex].forEach { splitBranch ->
                            val def = splitBranch.stepDefinition
                            if (def != StepsNames.WrapUp) {
                                val meta = getStepMeta(def) ?: return@forEach
                                newItems.add(
                                    LocalStepModel(
                                        name = "Step ${displayCounter}: ${meta.name}",
                                        description = meta.description,
                                        iconAssetPath = meta.icon,
                                        isDone = false,
                                        stepDefinition = configModelObject.stepDefinitions.filter { it.stepId == splitBranch.id }
                                            .first(),
                                        submitRequestModel = SubmitRequestModel(
                                            stepDefinition = configModelObject.stepDefinitions.filter { it.stepId == splitBranch.id }
                                                .first().stepDefinition,
                                            stepId = configModelObject.stepDefinitions.filter { it.stepId == splitBranch.id }
                                                .first().stepId,
                                            extractedInformation = emptyMap()
                                        )
                                    )
                                )
                                displayCounter++
                            }

                        }
                        steps.addAll(insertIndex, newItems)
                        LocalStepsObject.setLocalSteps(steps)
                        naveToNextStep(context)
                    }
                }
            }


        } else {
            naveToNextStep(context)
        }


    }*/


    fun getCurrentStep(): LocalStepModel? {
        val steps = LocalStepsObject.getLocalSteps()
        return steps.firstOrNull { !it.isDone }
    }

    fun makeCurrentStepDone(extractedInformation: Map<String, String>) {
        val steps = LocalStepsObject.getLocalSteps()
        val currentStep = steps.firstOrNull { !it.isDone }
        val submitRequestModel = currentStep!!.submitRequestModel;
        submitRequestModel!!.extractedInformation = extractedInformation;
        currentStep.let {
            it.isDone = true
            it.submitRequestModel = submitRequestModel
        }
        LocalStepsObject.setLocalSteps(steps)
    }


    fun setImage(url: String) {
        IDImageObject.clear();
        IDImageObject.setImage(url);
    }

    fun getPreviousIDImage(): String {
        return IDImageObject.getImage() ?: ""
    }


    fun getAllDoneSteps(): List<LocalStepModel> {
        val steps = LocalStepsObject.getLocalSteps()
        return steps.filter { it.isDone }
    }

    fun faceIDChange() {
        val steps = LocalStepsObject.getLocalSteps()
        val idStep =
            steps.firstOrNull { it.stepDefinition!!.stepDefinition == StepsNames.IdentificationDocumentCapture }
        idStep.let {
            it!!.isDone = false
        }
        LocalStepsObject.setLocalSteps(steps)
    }


    fun getSubmitList(): List<SubmitRequestModel> {
        val steps = LocalStepsObject.getLocalSteps()
        val submitList = mutableListOf<SubmitRequestModel>()

        for (step in steps) {
            val submitModel = step.submitRequestModel
            if (submitModel != null) {
                submitList.add(submitModel)
            }
        }

        var wrapUp: SubmitRequestModel? = null;
        val initSteps = ConfigModelObject.getConfigModelObject().stepDefinitions
        initSteps.forEach { item ->
            /** WrapUp **/
            if (item.stepDefinition == StepsNames.WrapUp) {
                val values: MutableMap<String, String> = mutableMapOf()
                item.outputProperties.forEach { property ->
                    if (property.key.contains(WrapUpKeys.TimeEnded)) {
                        values.put(property.key, getCurrentDateTime())
                    }
                }
                wrapUp = SubmitRequestModel(
                    item.stepId, StepsNames.WrapUp,
                    values
                );

            }
        }
        submitList.add(wrapUp!!);
        return submitList
    }

    fun backClick(context: Context) {
        val intent = Intent(context, BlockLoaderStepsComposeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }


    suspend fun downloadImageAsBase64(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = ApiKeyObject.getApiKeyObject();
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.setRequestProperty("x-api-key", apiKey)

            connection.doInput = true
            connection.connect()

            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}