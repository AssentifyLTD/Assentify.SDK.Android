package com.assentify.sdk.Core.Constants

import android.util.Base64
import android.util.Log
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.Models.parseDataToBaseResponseDataModel
import com.assentify.sdk.ProcessingRHub.ProgressRequestBody
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.RemoteClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Part
import retrofit2.http.Url
import java.util.UUID
class RemoteProcessing {

    private var callback: RemoteProcessingCallback? = null

    fun setCallback(callback: RemoteProcessingCallback) {
        this.callback = callback
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
        performLivenessDoc: Boolean,
        performLivenessFace: Boolean,
        saveCapturedVideo: Boolean,
        storeCapturedDocument: Boolean,
        isVideo: Boolean,
        storeImageStream: Boolean,
        stepId: String,
        clipParts: List<String>,
    ) {
        val traceIdentifier = UUID.randomUUID().toString()

        val clips = clipParts.mapIndexed { index, clip ->
            MultipartBody.Part.createFormData(
                "clips[$index]",
                clip
            )
        }


        val call = RemoteClient.remoteWidgetsService.starProcessing(
            url,
            stepId,
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
                performLivenessDoc.toString()
            ),
            RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                performLivenessFace.toString()
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
            clips,
            )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    val baseResponseDataModel = parseDataToBaseResponseDataModel(responseBody)
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

    fun starQrProcessing(
        url: String,
        videoClip: String,
        appConfiguration: ConfigModel,
        templateId: String,
        connectionId: String,
        stepId: String,
        metadata: String,
    ) {
        val call = RemoteClient.remoteWidgetsService.starQrProcessing(
            url,
            stepId,
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
                "true"
            ),
            RequestBody.create("text/plain".toMediaTypeOrNull(), videoClip),
            RequestBody.create("text/plain".toMediaTypeOrNull(), connectionId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), metadata),
        )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    val baseResponseDataModel = parseDataToBaseResponseDataModel(responseBody)
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

    fun starProcessingIDs(
        url: String,
        byteArrayImage: ByteArray,
        appConfiguration: ConfigModel,
        templateId: String,
        connectionId: String,
        clipsPath: String,
        checkForFace: Boolean,
        processMrz: Boolean,
        performLivenessDoc: Boolean,
        saveCapturedVideo: Boolean,
        storeCapturedDocument: Boolean,
        storeImageStream: Boolean,
        stepId: String,
        isManualCapture : Boolean,
        isAutoCapture : Boolean,
        tryNumber:Int,
        tag:String,
    ) {
        val tryCount  = tryNumber + 1;
        val traceIdentifier = UUID.randomUUID().toString()
        val call = RemoteClient.remoteWidgetsService.starProcessingIDs(
            url,
            stepId,
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
                performLivenessDoc.toString()
            ),
            RequestBody.create("text/plain".toMediaTypeOrNull(), processMrz.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "false"), //  DisableDataExtraction
            RequestBody.create("text/plain".toMediaTypeOrNull(), storeImageStream.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "false"), // isVideo
            RequestBody.create("text/plain".toMediaTypeOrNull(), clipsPath),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "true"), // isMobile
            byteArrayToPart(
                byteArray = byteArrayImage,
                partName = "Image",
                fileName = "image.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
                val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
                callback!!.onUploadProgress(pct);
            },
            RequestBody.create("text/plain".toMediaTypeOrNull(), checkForFace.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), connectionId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), saveCapturedVideo.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), storeCapturedDocument.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), traceIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isManualCapture.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isAutoCapture.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), tryCount.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), tag),
        )
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    val baseResponseDataModel = parseDataToBaseResponseDataModel(responseBody)
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

    private fun byteArrayToPart(
        byteArray: ByteArray,
        partName: String,
        fileName: String,
        mimeType: String,
        onProgress: (bytesWritten: Long, contentLength: Long, done: Boolean) -> Unit
    ): MultipartBody.Part {
        val requestBody = byteArray.toRequestBody(mimeType.toMediaTypeOrNull())
        val image =  Base64.encodeToString(byteArray, Base64.DEFAULT);
        val progressBody = ProgressRequestBody(requestBody, onProgress)
        return MultipartBody.Part.createFormData(partName, fileName, progressBody)
    }

}
