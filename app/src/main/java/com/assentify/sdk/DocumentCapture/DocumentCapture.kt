package com.assentify.sdk.DocumentCapture

import com.assentify.sdk.Core.Constants.EventsErrorMessages
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Customization
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.RemoteClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

public class DocumentCapture(private var apiKey: String, private var configModel: ConfigModel) {
    private var callback: DocumentCaptureCallback? = null
    private var stepID: String? = null

    fun setCallback(callback: DocumentCaptureCallback) {
        this.callback = callback
    }


    fun setStepId(stepId: String?) {
        this.stepID = stepId
        if (this.stepID == null) {
            val stepsCount = configModel.stepDefinitions.stream()
                .filter { item: StepDefinitions -> item.stepDefinition == StepsNames.DocumentCapture }
                .count()

            if (stepsCount == 1L) {
                for ((stepId1, stepDefinition) in configModel.stepDefinitions) {
                    if (stepDefinition == StepsNames.DocumentCapture) {
                        this.stepID = stepId1.toString()
                        getDocumentCaptureFromConfigFile()
                        break
                    }
                }
            } else {
                requireNotNull(this.stepID) { "Step ID is required because multiple 'Document Capture' steps are present." }
            }
        } else {
            getDocumentCaptureFromConfigFile()
        }

    }


    private fun getDocumentCaptureFromConfigFile() {
        val stepDefinitions = configModel.stepDefinitions
        stepDefinitions.forEach {
            if (it.stepId == this.stepID!!.toInt()) {
                val model: DocumentCaptureModel = it.customization.toDocumentCaptureModel()
                callback!!.onDocumentCaptureCallbackSuccess(model)
            }
        }
    }

    fun Customization.toDocumentCaptureModel(): DocumentCaptureModel {
        return DocumentCaptureModel(
            header = this.header,
            subHeader = this.subHeader,
            svgLogoUrl = this.svgLogoUrl,
            documentCaptures = this.documentCaptures,
            nextButtonTitle = this.nextButtonTitle,
            isNormalClick = this.isNormalClick ?: false
        )
    }

    /**
     * Uploads a captured photo or a picked file (jpg / png / pdf ...).
     *
     * @param documentKey DocumentCaptures key of the card that owns this file (routes callbacks)
     * @param itemId      id of the item inside that card (one card can hold several files)
     * @param docId       DocumentCaptures.id, used in the blob path
     */
    fun uploadDocument(
        file: File,
        mimeType: String,
        documentKey: String,
        itemId: String,
    ) {
        val body = file.asRequestBody(mimeType.toMediaType())
        val filePart = MultipartBody.Part.createFormData("files", documentKey, body)
        upload(filePart, documentKey, itemId)
    }

    private fun upload(
        filePart: MultipartBody.Part,
        documentKey: String,
        itemId: String,
    ) {


        val filesList = listOf(filePart)

        val additionalValuesList = listOf(
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.flowIdentifier,
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId,
            configModel.instanceId,
        ).map { it.toString().toRequestBody("text/plain".toMediaTypeOrNull()) }

        val call = RemoteClient.remoteBlobStorageService.uploadBulk(
            containerName = "documentcapture",
            referer = "https://platform.assentify.com/",
            filePath = documentKey,
            xBlockIdentifier = configModel.blockIdentifier,
            xTenantIdentifier = configModel.tenantIdentifier,
            xFlowIdentifier = configModel.flowIdentifier,
            xFlowInstanceId = configModel.flowInstanceId,
            xInstanceHash = configModel.instanceHash,
            xInstanceId = configModel.instanceId,
            files = filesList,
            additionalValues = additionalValuesList,
        )

        call.enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(
                call: Call<Map<String, String>>,
                response: Response<Map<String, String>>
            ) {
                val result = response.body()
                if (response.isSuccessful && result != null) {
                    callback?.onUploadDocumentCaptureCallbackSuccess(documentKey, itemId, result)
                } else {
                    callback?.onUploadDocumentCaptureCallbackError(
                        documentKey, itemId, EventsErrorMessages.OnErrorMessage
                    )
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                callback?.onUploadDocumentCaptureCallbackError(
                    documentKey, itemId, EventsErrorMessages.OnErrorMessage
                )
            }
        })
    }
}