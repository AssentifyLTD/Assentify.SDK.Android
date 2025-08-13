package com.assentify.sdk.SubmitData

import android.util.Log
import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import  com.assentify.sdk.RemoteClient.RemoteClient
import com.assentify.sdk.logging.BugsnagObject
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SubmitData(
    private val apiKey: String,
    private val submitDataCallback: SubmitDataCallback,
    private val submitRequestModel: List<SubmitRequestModel>,
    private val configModel: ConfigModel
) {

    init {
        submitSata()
    }

    private fun submitSata() {
        val remoteService = RemoteClient.remoteGatewayService
        val call = remoteService.submit(
            apiKey,
            "SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            submitRequestModel
        );
        BugsnagObject.logInfo("Data submission started. ${submitRequestModelLog(submitRequestModel)}",configModel)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {

                if (response.isSuccessful) {
                    BugsnagObject.logInfo("Data submission success : Response Body $response",configModel)
                    submitDataCallback.onSubmitSuccess(response.message());
                } else {
                    BugsnagObject.logInfo(" Data submission Response Data: ${response.errorBody()?.string()}",configModel)
                    BugsnagObject.logInfo("Data submission failed : Response Body $response",configModel)
                    submitDataCallback.onSubmitError(response.message());
                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                BugsnagObject.logInfo("Data submission failed : Message ${t.message}",configModel)
                submitDataCallback.onSubmitError(t.message!!);
            }
        })
    }

    private fun submitRequestModelLog(submitRequestModel: List<SubmitRequestModel>): Map<String, Any> {
        val gson = Gson()
        val jsonString = gson.toJson(submitRequestModel)
        return mapOf("submitRequestModel" to jsonString)
    }


}
