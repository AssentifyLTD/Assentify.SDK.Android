package com.assentify.sdk.Flow.FlowController

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
            when (currentStep!!.stepDefinition!!.stepDefinition) {
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

    fun getPreviousIDScanStep(): LocalStepModel? {
        val steps = LocalStepsObject.getLocalSteps()
        return steps.lastOrNull { it.stepDefinition!!.stepDefinition == StepsNames.IdentificationDocumentCapture && it.isDone }
    }

    fun getPreviousFaceScanStep(): LocalStepModel? {
        val steps = LocalStepsObject.getLocalSteps()
        return steps.lastOrNull { it.stepDefinition!!.stepDefinition == StepsNames.FaceImageAcquisition && it.isDone }
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }


    suspend fun downloadImageAsBase64(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.setRequestProperty("x-api-key", "YOUR_API_KEY_HERE")

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