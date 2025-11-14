package com.assentify.sdk.Flow.Terms

import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.TermsConditionsModel
import com.assentify.sdk.RemoteClient.RemoteClient

object TermsConditionsHelper {

    fun getTermsConditionsStep(
        configModel: ConfigModel,
        ID: Int,
        onResult: (TermsConditionsModel?) -> Unit
    ) {
        val call = RemoteClient.remoteGatewayService.getTermsConditionsStep(
            "", "Android SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            ID.toString()
        )

        call.enqueue(object : retrofit2.Callback<TermsConditionsModel> {
            override fun onResponse(
                call: retrofit2.Call<TermsConditionsModel>,
                response: retrofit2.Response<TermsConditionsModel>
            ) {
                if(response.isSuccessful){
                    val termsConditionsModel = response.body();
                    onResult(termsConditionsModel)
                }else{
                    onResult(null)
                }

            }

            override fun onFailure(call: retrofit2.Call<TermsConditionsModel>, t: Throwable) {
                onResult(null)
            }
        })
    }

}
