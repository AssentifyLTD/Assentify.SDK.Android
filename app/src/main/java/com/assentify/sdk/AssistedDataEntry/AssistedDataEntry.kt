package com.assentify.sdk.AssistedDataEntry

import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryBaseModel
import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.RemoteClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

public  class AssistedDataEntry(private var apiKey: String,private var configModel: ConfigModel) {
    private var callback: AssistedDataEntryCallback? = null
    private var stepID: String? = null

    fun setCallback(callback: AssistedDataEntryCallback) {
        this.callback = callback
    }


    fun setStepId(stepId: String?) {
        this.stepID = stepId
        if (this.stepID == null) {
            val stepsCount = configModel.stepDefinitions.stream()
                .filter { item: StepDefinitions -> item.stepDefinition == "AssistedDataEntry" }
                .count()

            if (stepsCount == 1L) {
                for ((stepId1, stepDefinition) in configModel.stepDefinitions) {
                    if (stepDefinition == "AssistedDataEntry") {
                        this.stepID = stepId1.toString()
                        getAssistedDataEntryStep()
                        break
                    }
                }
            } else {
                requireNotNull(this.stepID) { "Step ID is required because multiple 'Assisted Data Entry' steps are present." }
            }
        }

    }

    private fun getAssistedDataEntryStep() {
        val remoteService = RemoteClient.remoteGatewayService
        val call = remoteService.getAssistedDataEntryStep(
            apiKey, "SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            stepID!!
        )
        call.enqueue(object : Callback<AssistedDataEntryBaseModel> {
            override fun onResponse(
                call: Call<AssistedDataEntryBaseModel>,
                response: Response<AssistedDataEntryBaseModel>
            ) {
                if (response.isSuccessful) {
                     val model: AssistedDataEntryModel = response.body()!!.data
                    callback!!.onAssistedDataEntrySuccess(model)
                }
            }

            override fun onFailure(call: Call<AssistedDataEntryBaseModel>, t: Throwable) {
                callback!!.onAssistedDataEntryError(t.message!!)

            }
        })
    }


}