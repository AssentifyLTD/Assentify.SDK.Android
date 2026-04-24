package com.assentify.sdk.Flow.FlowController

import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpResponseModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpRequestOtpModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpResponseOtpModel
import com.assentify.sdk.RemoteClient.RemoteClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object OtpHelper {

    fun requestOtp(
        configModel: ConfigModel,
        requestOtpModel: RequestOtpModel,
        onResult: (Boolean) -> Unit
    ) {
        val call = RemoteClient.remoteGatewayService.requestOtp(
            "Android Sdk",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            requestOtpModel
        )

        call.enqueue(object : Callback<RequestOtpResponseModel> {
            override fun onResponse(
                call: Call<RequestOtpResponseModel>,
                response: Response<RequestOtpResponseModel>
            ) {
                val success = response.isSuccessful && (response.body()?.isSuccessful == true)
                onResult(success)
            }

            override fun onFailure(call: Call<RequestOtpResponseModel>, t: Throwable) {
                onResult(false)
            }
        })
    }

    fun verifyOtp(
        configModel: ConfigModel,
        verifyOtpRequestOtpModel: VerifyOtpRequestOtpModel,
        onResult: (Boolean) -> Unit
    ) {
        val call = RemoteClient.remoteGatewayService.verifyOtp(
            "Android Sdk",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            verifyOtpRequestOtpModel
        )

        call.enqueue(object : Callback<VerifyOtpResponseOtpModel> {
            override fun onResponse(
                call: Call<VerifyOtpResponseOtpModel>,
                response: Response<VerifyOtpResponseOtpModel>
            ) {
                val success = response.isSuccessful && (response.body()?.isSuccessful == true)  && (response.body()?.data == true)
                onResult(success)
            }

            override fun onFailure(call: Call<VerifyOtpResponseOtpModel>, t: Throwable) {
                onResult(false)
            }
        })
    }
}