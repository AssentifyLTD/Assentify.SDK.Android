package com.assentify.sdk.SubmitData

import SubmitRequestModel
import android.util.Log
import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import  com.assentify.sdk.RemoteClient.RemoteClient
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
                    println("Response body: $responseBody")
                    submitDataCallback.onSubmitSuccess();
                } else {
                    submitDataCallback.onSubmitError(response.message());
                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                submitDataCallback.onSubmitError(t.message!!);
            }
        })
    }


}
