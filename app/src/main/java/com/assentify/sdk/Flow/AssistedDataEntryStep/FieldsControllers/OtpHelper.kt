package com.assentify.sdk.Flow.AssistedDataEntryStep.FieldsControllers

import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpResponseModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpRequestOtpModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpResponseOtpModel
import com.assentify.sdk.RemoteClient.RemoteClient

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

        call.enqueue(object : retrofit2.Callback<RequestOtpResponseModel> {
            override fun onResponse(
                call: retrofit2.Call<RequestOtpResponseModel>,
                response: retrofit2.Response<RequestOtpResponseModel>
            ) {
                val success = response.isSuccessful && (response.body()?.isSuccessful == true)
                onResult(success)
            }

            override fun onFailure(call: retrofit2.Call<RequestOtpResponseModel>, t: Throwable) {
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

        call.enqueue(object : retrofit2.Callback<VerifyOtpResponseOtpModel> {
            override fun onResponse(
                call: retrofit2.Call<VerifyOtpResponseOtpModel>,
                response: retrofit2.Response<VerifyOtpResponseOtpModel>
            ) {
                val success = response.isSuccessful && (response.body()?.isSuccessful == true)  && (response.body()?.data == true)
                onResult(success)
            }

            override fun onFailure(call: retrofit2.Call<VerifyOtpResponseOtpModel>, t: Throwable) {
                onResult(false)
            }
        })
    }
}
