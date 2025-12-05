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
            appConfiguration.tenantIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            appConfiguration.blockIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            appConfiguration.instanceId.toRequestBody("text/plain".toMediaTypeOrNull()),
            templateId.toRequestBody("text/plain".toMediaTypeOrNull()),
            "true"
                .toRequestBody("text/plain".toMediaTypeOrNull()),
            byteArrayToPart(
                byteArray = byteArrayImage,
                partName = "Image",
                fileName = "image.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
                val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
                callback!!.onUploadProgress(pct);
            },
            connectionId.toRequestBody("text/plain".toMediaTypeOrNull()),
            metadata.toRequestBody("text/plain".toMediaTypeOrNull()),
            traceIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            isManualCapture.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            isAutoCapture.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
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
                            error = EventsErrorMessages.OnErrorMessage,
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
                        error = EventsErrorMessages.OnErrorMessage,
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
            appConfiguration.tenantIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            appConfiguration.blockIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            appConfiguration.instanceId.toRequestBody("text/plain".toMediaTypeOrNull()),
            templateId.toRequestBody("text/plain".toMediaTypeOrNull()),
            performLivenessDoc.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull()),
            processMrz.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "false".toRequestBody("text/plain".toMediaTypeOrNull()), //  DisableDataExtraction
            storeImageStream.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "false".toRequestBody("text/plain".toMediaTypeOrNull()), // isVideo
            clipsPath.toRequestBody("text/plain".toMediaTypeOrNull()),
            "true".toRequestBody("text/plain".toMediaTypeOrNull()), // isMobile
            byteArrayToPart(
                byteArray = byteArrayImage,
                partName = "Image",
                fileName = "image.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
                val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
                callback!!.onUploadProgress(pct);
            },
            checkForFace.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            connectionId.toRequestBody("text/plain".toMediaTypeOrNull()),
            saveCapturedVideo.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            storeCapturedDocument.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            traceIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            isManualCapture.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            isAutoCapture.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            tryCount.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            tag.toRequestBody("text/plain".toMediaTypeOrNull()),
            processCivilExtractQrCode.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "false".toRequestBody("text/plain".toMediaTypeOrNull()), // RequireFaceExtraction
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
                            error = EventsErrorMessages.OnErrorMessage,
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
                        error = EventsErrorMessages.OnErrorMessage,
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
            appConfiguration.tenantIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            appConfiguration.blockIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            appConfiguration.instanceId.toRequestBody("text/plain".toMediaTypeOrNull()),
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
            traceIdentifier.toRequestBody("text/plain".toMediaTypeOrNull()),
            "true".toRequestBody("text/plain".toMediaTypeOrNull()), // isMobile
            byteArrayToPart(
                byteArray = secondImage,
                partName = "SecondImage",
                fileName = "selfieImage.jpg",
                mimeType = "image/jpeg"
            ){ sent, total, done ->
              //  val pct = if (total > 0) ((sent * 100) / total).toInt() else -1
              //  callback!!.onUploadProgress(pct);
            },
            isLivenessEnabled.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            tryCount.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            isAutoCapture.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            isManualCapture.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            connectionId.toRequestBody("text/plain".toMediaTypeOrNull()),

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
                            error = EventsErrorMessages.OnErrorMessage,
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
                        error = EventsErrorMessages.OnErrorMessage,
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

