package com.assentify.sdk.Flow.FlowController

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.assentify.sdk.ApiKeyObject
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Core.Constants.WrapUpKeys
import com.assentify.sdk.Core.Constants.getCurrentDateTime
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Flow.AssistedDataEntryStep.AssistedDataEntryActivity
import com.assentify.sdk.Flow.BlockLoader.BlockLoaderStepsComposeActivity
import com.assentify.sdk.Flow.ContextAwareStep.MultipleFilesContextAwareStepActivity
import com.assentify.sdk.Flow.FaceStep.HowToCaptureFaceActivity
import com.assentify.sdk.Flow.IDStep.IDStepComposeActivity
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.Flow.SubmitStep.SubmitStepActivity
import com.assentify.sdk.Flow.Terms.TermsAndConditionsComposeActivity
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.IDImageObject
import com.assentify.sdk.LocalStepsObject
import com.assentify.sdk.R
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.RemoteClient.Models.TrackNextRequest
import com.assentify.sdk.RemoteClient.Models.TrackProgressRequest
import com.assentify.sdk.RemoteClient.RemoteClient
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FlowController {


    fun naveToNextStep(context: Context) {
        val currentStep = getCurrentStep();
        if (currentStep == null) {
            SubmitStepActivity.start(context = context)
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
                    MultipleFilesContextAwareStepActivity.start(context = context)
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

        /** Track Next **/
        val currentIndex = steps.indexOfFirst { !it.isDone }
        val nextStep = if (currentIndex != -1 && currentIndex + 1 < steps.size) {
            steps[currentIndex + 1]
        } else {
            null
        }
        trackNext(currentStep!!, nextStep)

        /** Make Current Step Done **/
        val submitRequestModel = currentStep.submitRequestModel;
        submitRequestModel!!.extractedInformation = extractedInformation;
        currentStep.let {
            it.isDone = true
            it.submitRequestModel = submitRequestModel
        }
        LocalStepsObject.setLocalSteps(steps)

    }


    fun getFaceMatchInputImageKey(): String {
        val key = ConstantsValues.ProvidedFaceImageKey
        val currentStep = getCurrentStep()
        val steps = LocalStepsObject.getLocalSteps()

        val faceStep =
            steps.firstOrNull { it.stepDefinition?.stepDefinition == StepsNames.FaceImageAcquisition }

        return if (faceStep!!.stepDefinition!!.inputProperties.isNotEmpty()) {
            if (faceStep.stepDefinition.inputProperties.first().sourceStepId == currentStep!!.stepDefinition!!.stepId) {
                faceStep.stepDefinition.inputProperties.first().sourceKey
            } else {
                "NON"
            }
        } else {
            key
        }
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

    private fun trackNext(currentStep: LocalStepModel, nextStep: LocalStepModel?) {
        val remoteService = RemoteClient.remoteGatewayService
        val configModel = ConfigModelObject.getConfigModelObject()
        val apiKey = ApiKeyObject.getApiKeyObject()
        val flowEnvironmentalConditions =
            FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
        val userAgent = System.getProperty("http.agent")
            ?: "Android ${Build.VERSION.RELEASE}; ${Build.MODEL}"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val body = TrackNextRequest(
            ApplicationId = configModel.applicationId,
            BlockIdentifier = configModel.blockIdentifier,
            DeviceName = deviceName,
            FlowIdentifier = configModel.flowIdentifier,
            FlowInstanceId = configModel.flowInstanceId,
            FlowName = configModel.flowName,
            InstanceHash = configModel.instanceHash,
            TenantIdentifier = configModel.tenantIdentifier,
            IsSuccessful = true,
            Language = flowEnvironmentalConditions.language,
            TimeEnded = getCurrentDateTimeForTracking(),
            UserAgent = userAgent,
            StepDefinition = currentStep.stepDefinition!!.stepDefinition,
            StepId = currentStep.stepDefinition.stepId,
            StepTypeId = configModel.stepMap.find { it.id == currentStep.stepDefinition.stepId }!!.stepType,

            NextStepDefinition = if (nextStep != null) nextStep.stepDefinition!!.stepDefinition else StepsNames.WrapUp,
            NextStepId = if (nextStep != null) nextStep.stepDefinition!!.stepId else configModel.stepMap.find { it.stepDefinition == StepsNames.WrapUp }!!.id,
            NextStepTypeId = if (nextStep != null) configModel.stepMap.find { it.id == nextStep.stepDefinition!!.stepId }!!.stepType else configModel.stepMap.find { it.stepDefinition == StepsNames.WrapUp }!!.stepType,

            BlockType = "Engagement",
            StatusCode = 200,
        )
        val call = remoteService.trackNext(
            apiKey,
            "SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            body
        );
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {}
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
        })
    }


     fun trackProgress(currentStep: LocalStepModel,inputData:Any? = null,response:Any? = null,status: String) {
        val remoteService = RemoteClient.remoteGatewayService
        val configModel = ConfigModelObject.getConfigModelObject()
        val apiKey = ApiKeyObject.getApiKeyObject()
        val flowEnvironmentalConditions =
            FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
        val userAgent = System.getProperty("http.agent")
            ?: "Android ${Build.VERSION.RELEASE}; ${Build.MODEL}"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val body = TrackProgressRequest(
            TenantIdentifier = configModel.tenantIdentifier,
            FlowIdentifier = configModel.flowIdentifier,
            FlowInstanceId = configModel.flowInstanceId,
            ApplicationId = configModel.applicationId,
            BlockIdentifier = configModel.blockIdentifier,
            InstanceHash = configModel.instanceHash,
            FlowName = configModel.flowName,
            StepDefinition = currentStep.stepDefinition!!.stepDefinition,
            StepId = currentStep.stepDefinition.stepId,
            StepTypeId = configModel.stepMap.find { it.id == currentStep.stepDefinition.stepId }!!.stepType,
            DeviceName = deviceName,
            UserAgent = userAgent,
            Timestamp = getCurrentDateTimeForTracking(),
            Language = flowEnvironmentalConditions.language,
            Status = status,
            InputData = prepareTrackProgressInputData(currentStep,inputData),
            Response = response
        )
        val call = remoteService.trackProgress(
            apiKey,
            "SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            body
        );
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {}
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
        })
    }

    fun extractAfterDash(error: String?,): String {
        if (error.isNullOrEmpty()) return ""

        return if (error.contains("-")) {
            error.substringAfter("-").trim()
        } else {
            ""
        }
    }

    fun decodeToJsonObject(originalString: String?): JsonObject? {
        if (originalString.isNullOrEmpty()) return null

        return try {
            Gson().fromJson(originalString, JsonObject::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun  prepareTrackProgressInputData(currentStep: LocalStepModel,inputData:Any?):Map<String, Any?>{
        val  stepsData = mutableMapOf<String, Any?>()
        val steps = LocalStepsObject.getLocalSteps()
        steps.forEach {
            if(it.isDone ){
                val  extractedInformation= mutableMapOf<String, Any>()
                extractedInformation["stepId"] = it.stepDefinition!!.stepId
                extractedInformation.putAll(it.submitRequestModel!!.extractedInformation)
                stepsData[prepareStepName(it.stepDefinition.stepDefinition,it.stepDefinition.stepId)]  = extractedInformation
            }
        }
        val  extractedInformation = mutableMapOf<String, Any?>()
        extractedInformation["stepId"] = currentStep.stepDefinition!!.stepId
        toMap(inputData)?.let {
            extractedInformation.putAll(it)
        }
        stepsData[prepareStepName(currentStep.stepDefinition.stepDefinition,currentStep.stepDefinition.stepId)] = extractedInformation
        return stepsData;
    }
    fun toMap(input: Any?): Map<String, Any?>? {
        return when (input) {
            null -> null

            is Map<*, *> ->
                input.filterKeys { it is String }
                    .mapKeys { it.key as String }

            is JsonObject ->
                Gson().fromJson(
                    input,
                    object : TypeToken<Map<String, Any?>>() {}.type
                )
            else -> null
        }
    }

    fun prepareStepName(
        stepDefinition: String,
        stepId: Int
    ): String {
        val steps = LocalStepsObject.getLocalSteps()

        val duplicatesCount = steps.count {
            it.stepDefinition?.stepDefinition == stepDefinition
        }

        return if (duplicatesCount > 1) {
            "${stepDefinition}_$stepId"
        } else {
            stepDefinition
        }
    }

}

val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_light, FontWeight.Light),
)