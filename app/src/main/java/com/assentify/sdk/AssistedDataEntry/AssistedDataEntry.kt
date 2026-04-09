package com.assentify.sdk.AssistedDataEntry

import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Customization
import com.assentify.sdk.RemoteClient.Models.StepDefinitions

public class AssistedDataEntry(private var apiKey: String, private var configModel: ConfigModel) {
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
                        getAssistedDataEntryStepFromConfigFile()
                        break
                    }
                }
            } else {
                requireNotNull(this.stepID) { "Step ID is required because multiple 'Assisted Data Entry' steps are present." }
            }
        } else {
            getAssistedDataEntryStepFromConfigFile()
        }

    }


    private fun getAssistedDataEntryStepFromConfigFile() {
        val stepDefinitions = configModel.stepDefinitions
        stepDefinitions.forEach {
            if (it.stepId == this.stepID!!.toInt()) {
                val model: AssistedDataEntryModel = it.customization.toAssistedDataEntryModel()
                callback!!.onAssistedDataEntrySuccess(model)
            }
        }
    }

    fun Customization.toAssistedDataEntryModel(): AssistedDataEntryModel {
        return AssistedDataEntryModel(
            header = this.header!!,
            subHeader = this.subHeader!!,
            allowAssistedDataEntry = this.allowAssistedDataEntry!!,
            assistedDataEntryPages = this.assistedDataEntryPages!!,
            inputProperties = this.inputProperties!!
        )
    }


}