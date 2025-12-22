package  com.assentify.sdk

import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanPassport.PassportResponseModel

object AssentifySdkObject {
    private lateinit var assentifySdk: AssentifySdk
    fun setAssentifySdkObject(assentifySdk: AssentifySdk) {
        this.assentifySdk = assentifySdk;
    }

    fun getAssentifySdkObject(): AssentifySdk {
        return this.assentifySdk
    }

}


object FlowEnvironmentalConditionsObject {
    private lateinit var flowEnvironmentalConditions: FlowEnvironmentalConditions;
    fun setFlowEnvironmentalConditions(flowEnvironmentalConditions: FlowEnvironmentalConditions) {
        this.flowEnvironmentalConditions = flowEnvironmentalConditions;
    }

    fun getFlowEnvironmentalConditions(): FlowEnvironmentalConditions {
        return this.flowEnvironmentalConditions
    }

}

object ConfigModelObject {
    private lateinit var configModel: ConfigModel
    fun setConfigModelObject(configModel: ConfigModel) {
        this.configModel = configModel;
    }

    fun getConfigModelObject(): ConfigModel {
        return this.configModel
    }

}


object LocalStepsObject {
    private var localSteps: MutableList<LocalStepModel> = mutableListOf()

    fun setLocalSteps(localSteps: MutableList<LocalStepModel>) {
        this.localSteps = localSteps
    }

    fun getLocalSteps(): MutableList<LocalStepModel> {
        return localSteps
    }
}

object SelectedTemplatesObject {
    private lateinit var templates: Templates
    fun setSelectedTemplatesObject(templates: Templates) {
        this.templates = templates
    }

    fun getSelectedTemplatesObject(): Templates {
        return templates
    }
}

object ApiKeyObject {
    private lateinit var apiKey: String
    fun setApiKeyObject(templates: String) {
        this.apiKey = templates
    }

    fun getApiKeyObject(): String {
        return apiKey
    }
}


object AssistedDataEntryPagesObject {
    private var assistedDataEntryModel: AssistedDataEntryModel? = null

    fun setAssistedDataEntryModelObject(model: AssistedDataEntryModel?) {
        assistedDataEntryModel = model
    }

    fun getAssistedDataEntryModelObject(): AssistedDataEntryModel? = assistedDataEntryModel

    fun clear() {
        assistedDataEntryModel = null
    }

}


object NfcPassportResponseModelObject {
    private lateinit var passportResponseModel: PassportResponseModel
    fun setPassportResponseModelObject(passportResponseModel: PassportResponseModel) {
        this.passportResponseModel = passportResponseModel
    }

    fun getPassportResponseModelObject(): PassportResponseModel {
        return passportResponseModel
    }
}

object QrIDResponseModelObject {
    private lateinit var iDResponseModel: IDResponseModel
    fun setQrIDResponseModelObject(iDResponseModel: IDResponseModel) {
        this.iDResponseModel = iDResponseModel
    }

    fun getQrIDResponseModelObject(): IDResponseModel {
        return iDResponseModel
    }
}



object FlowCallbackObject {
    private lateinit var flowCallback: FlowCallBack
    fun setFlowCallbackObject(flowCallback: FlowCallBack) {
        this.flowCallback = flowCallback
    }

    fun getFlowCallbackObject(): FlowCallBack {
        return flowCallback
    }
}




object Base64ImageObject {
    private var image: String? = null

    fun setImage(value: String?) {
        image = value
    }

    fun getImage(): String? = image

    fun clear() {
        image = null
    }

}

object IDImageObject {
    private var image: String? = null

    fun setImage(value: String?) {
        image = value
    }

    fun getImage(): String? = image

    fun clear() {
        image = null
    }

}





