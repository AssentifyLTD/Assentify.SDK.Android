package com.assentify.sdk.SubmitData

import SubmitRequestModel
import android.util.Log
import com.assentify.sdk.Core.Constants.SentryKeys
import com.assentify.sdk.Core.Constants.SentryManager
import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import  com.assentify.sdk.RemoteClient.RemoteClient
import io.sentry.SentryLevel
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
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
              val responseBody = response.body()?.string()

                if (response.isSuccessful) {
                    submitDataCallback.onSubmitSuccess();
                } else {
                    submitDataCallback.onSubmitError(response.message());
                }
                SentryManager.registerEvent(SentryKeys.Submit + ":" +  responseBody , SentryLevel.INFO)

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                SentryManager.registerEvent(SentryKeys.Submit + ":" +  t.message , SentryLevel.INFO)
                submitDataCallback.onSubmitError(t.message!!);
            }
        })
    }


}
