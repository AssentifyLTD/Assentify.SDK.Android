package com.assentify.sdk.Core.Constants

import android.util.Log
import  com.assentify.sdk.Core.Constants.Routes.BaseUrls
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.Models.parseDataToBaseResponseDataModel
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback
import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import  com.assentify.sdk.RemoteClient.RemoteClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.UUID

class RemoteProcessing {

    private var callback: RemoteProcessingCallback? = null

    fun setCallback(callback: RemoteProcessingCallback) {
        this.callback = callback
    }

    fun uploadVideo(
        counter: Int,
        uploadFile: File,
        appConfiguration: ConfigModel,
        templateId: String
    ) {
        val fileRequestBody = RequestBody.create(null, File(uploadFile.absolutePath))
        val filePart = MultipartBody.Part.createFormData(
            "files", "${counter}.mp4", fileRequestBody
        )
        val call = RemoteClient.remoteBlobStorageService.uploadFile(
            BaseUrls.ContainerName,
            "${counter}",
            filePart,
            RequestBody.create("text/plain".toMediaTypeOrNull(), appConfiguration.tenantIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), appConfiguration.blockIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), appConfiguration.instanceId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), templateId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "try_${counter}"),
        )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val responseBodyString = responseBody.string()

                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            }
        })

    }

    fun starProcessing(
        url: String,
        videoClip: String,
        selfieImage: String,
        appConfiguration: ConfigModel,
        templateId: String,
        secondImage: String,
        connectionId: String,
        clipsPath: String,
        checkForFace: Boolean,
        processMrz: Boolean,
        performLivenessDetection: Boolean,
        saveCapturedVideo: Boolean,
        storeCapturedDocument: Boolean,
        isVideo: Boolean,
        storeImageStream: Boolean,
        stepDefinition: String,
    ) {
        val traceIdentifier = UUID.randomUUID().toString()

        val call = RemoteClient.remoteWidgetsService.starProcessing(
            url,
            appConfiguration.stepDefinitions.filter { it.stepDefinition == stepDefinition }.first().stepId.toString(),
            appConfiguration.blockIdentifier,
            appConfiguration.flowIdentifier,
            appConfiguration.flowInstanceId,
            appConfiguration.instanceHash,
            appConfiguration.instanceId,
            appConfiguration.tenantIdentifier,
            RequestBody.create("text/plain".toMediaTypeOrNull(), appConfiguration.tenantIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), appConfiguration.blockIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), appConfiguration.instanceId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), templateId),
            RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                performLivenessDetection.toString()
            ),
            RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                performLivenessDetection.toString()
            ),
            RequestBody.create("text/plain".toMediaTypeOrNull(), processMrz.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "false"),
            RequestBody.create("text/plain".toMediaTypeOrNull(), storeImageStream.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isVideo.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), clipsPath),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "true"),
            RequestBody.create("text/plain".toMediaTypeOrNull(), videoClip),
            RequestBody.create("text/plain".toMediaTypeOrNull(), checkForFace.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), connectionId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), secondImage),
            RequestBody.create("text/plain".toMediaTypeOrNull(), saveCapturedVideo.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), storeCapturedDocument.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), traceIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), selfieImage),

            )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    val baseResponseDataModel = parseDataToBaseResponseDataModel(responseBody)
                 //   Log.e("IDSCAN",baseResponseDataModel.response!!);
                    callback!!.onMessageReceived(
                        baseResponseDataModel.destinationEndpoint!!,
                        baseResponseDataModel
                    )

                }else {
                    val responseBody = response.body()?.string() ?: ""
                    callback!!.onMessageReceived(
                        HubConnectionTargets.ON_ERROR,
                        BaseResponseDataModel(
                            destinationEndpoint = HubConnectionTargets.ON_ERROR,
                            response = "",
                            error = "",
                            success = false
                        )
                    );
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback!!.onMessageReceived(
                    HubConnectionTargets.ON_ERROR,
                    BaseResponseDataModel(
                        destinationEndpoint = HubConnectionTargets.ON_ERROR,
                        response = "",
                        error = t.message,
                        success = false
                    )
                );
            }
        })


    }


}
