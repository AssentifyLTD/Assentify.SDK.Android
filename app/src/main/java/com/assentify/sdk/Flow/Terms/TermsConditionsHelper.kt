package com.assentify.sdk.Flow.Terms

import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Customization
import com.assentify.sdk.RemoteClient.Models.TermsConditionsDataModel
import com.assentify.sdk.RemoteClient.Models.TermsConditionsModel

object TermsConditionsHelper {


    fun getTermsConditionsStepFromConfigFile(
        configModel: ConfigModel,
        ID: Int,
        onResult: (TermsConditionsModel?) -> Unit
    ) {

        val  stepDefinitions = configModel.stepDefinitions
        stepDefinitions.forEach {
           if(it.stepId == ID){
               val termsConditionsModel = it.customization.toTermsConditionsModel()
               onResult(termsConditionsModel)
           }
        }
    }

    fun Customization.toTermsConditionsModel(): TermsConditionsModel {
        return TermsConditionsModel(
            statusCode = 200,
            data = TermsConditionsDataModel(
                header = this.header,
                subHeader = this.subHeader,
                file = this.file,
                nextButtonTitle = this.nextButtonTitle,
                confirmationRequired = this.confirmationRequired
            )
        )
    }
}
