package com.assentify.sdk.Core.Constants

import android.util.Base64
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
import java.util.UUID

class RemoteProcessing {

    private var callback: RemoteProcessingCallback? = null

    fun setCallback(callback: RemoteProcessingCallback) {
        this.callback = callback
    }


    fun starQrProcessing(
        url: String,
        byteArrayImage: ByteArray,
        appConfiguration: ConfigModel,
        templateId: String,
        connectionId: String,
        stepId: String,
        metadata: String,
        isManualCapture : Boolean,
        isAutoCapture : Boolean,
    ) {
        val traceIdentifier = UUID.randomUUID().toString()
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
            byteArrayToPart(
                byteArray = byteArrayImage,
                partName = "Image",
                fileName = "image.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
                val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
                callback!!.onUploadProgress(pct);
            },
            RequestBody.create("text/plain".toMediaTypeOrNull(), connectionId),
            RequestBody.create("text/plain".toMediaTypeOrNull(), metadata),
            RequestBody.create("text/plain".toMediaTypeOrNull(), traceIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isManualCapture.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isAutoCapture.toString()),
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
        processCivilExtractQrCode:Boolean,
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
            RequestBody.create("text/plain".toMediaTypeOrNull(), processCivilExtractQrCode.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "false"), // RequireFaceExtraction
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

    fun starProcessingFace(
        url: String,
        appConfiguration: ConfigModel,
        stepId: String,
        selfieImage: ByteArray,
        livenessFrames:  List<ByteArray>,
        secondImage: ByteArray,
        isLivenessEnabled:Boolean,
        tryNumber:Int,
        isAutoCapture : Boolean,
        isManualCapture : Boolean,
        connectionId: String,
    ) {
        var currentClip = 0;
        val tryCount  = tryNumber + 1;
        val traceIdentifier = UUID.randomUUID().toString()
        val call = RemoteClient.remoteWidgetsService.starProcessingFace(
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
            byteArrayToPart(
                byteArray = selfieImage,
                partName = "SelfieImage",
                fileName = "selfieImage.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
                val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
                if(isManualCapture){
                        callback!!.onUploadProgress(pct);
                }
                if(isAutoCapture){
                    if(!isLivenessEnabled){
                        callback!!.onUploadProgress(pct);

                    }
                }
            },
            byteArraysToParts(
                byteArrays = livenessFrames,
                partName = "livenessFrames",
                filePrefix = "frame",
                mimeType = "image/jpeg",
            ){ sent, total, done ->
                val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
                if(isAutoCapture){
                    if(isLivenessEnabled){
                        if(pct == 100){
                            currentClip += 1;
                            val currentProgress  = currentClip *  8.33333333
                            callback!!.onUploadProgress(currentProgress.toInt());
                        }

                    }
                }
            },
            RequestBody.create("text/plain".toMediaTypeOrNull(), traceIdentifier),
            RequestBody.create("text/plain".toMediaTypeOrNull(), "true"), // isMobile
            byteArrayToPart(
                byteArray = secondImage,
                partName = "SecondImage",
                fileName = "selfieImage.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
              //  val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
              //  callback!!.onUploadProgress(pct);
            },
            RequestBody.create("text/plain".toMediaTypeOrNull(), isLivenessEnabled.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), tryCount.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isAutoCapture.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), isManualCapture.toString()),
            RequestBody.create("text/plain".toMediaTypeOrNull(), connectionId),

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
       // Log.e("byteArrayToPart",image.toString())
        val progressBody = ProgressRequestBody(requestBody, onProgress)
        return MultipartBody.Part.createFormData(partName, fileName, progressBody)
    }

    private fun byteArraysToParts(
        byteArrays: List<ByteArray>,
        partName: String,
        filePrefix: String,
        mimeType: String,
        onProgress: (bytesWritten: Long, contentLength: Long, done: Boolean) -> Unit
    ): List<MultipartBody.Part> {
        return byteArrays.mapIndexed { index, byteArray ->
            val fileName = "${filePrefix}_${index}.jpg"
            val requestBody = byteArray.toRequestBody(mimeType.toMediaTypeOrNull())
            val image =  Base64.encodeToString(byteArray, Base64.DEFAULT);
        //    Log.e("byteArrayToPart",image.toString())
            val progressBody = ProgressRequestBody(requestBody, onProgress)
            MultipartBody.Part.createFormData(partName, fileName, progressBody)
        }
    }

}

