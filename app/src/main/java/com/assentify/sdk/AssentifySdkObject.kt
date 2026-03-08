package  com.assentify.sdk

import android.content.Context
import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

private const val PREF_NAME = "assentify_sdk_prefs"


object AssentifySdkObject {
    private lateinit var assentifySdk: AssentifySdk
    fun setAssentifySdkObject(assentifySdk: AssentifySdk) {
        this.assentifySdk = assentifySdk;
    }

    fun getAssentifySdkObject(): AssentifySdk {
        return this.assentifySdk
    }

}

object InteractionObject {
    private lateinit var interaction: String
    fun setInteractionObject(interaction: String) {
        this.interaction = interaction;
    }

    fun getInteractionObject(): String {
        return this.interaction
    }

}

object ContextObject {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getContext(): Context {
        return requireNotNull(appContext) { "ContextObject is not initialized" }
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

    private const val PREF_NAME = "assentify_sdk_prefs"

    fun setConfigModelObject(
        configModel: ConfigModel?
    ) {

        val prefs = ContextObject.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val json = Gson().toJson(configModel)

        prefs.edit()
            .putString("ConfigModelObject_${InteractionObject.getInteractionObject()}", json)
            .apply()
    }

    fun getConfigModelObject(
    ): ConfigModel? {

        val prefs = ContextObject.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val json = prefs.getString(
            "ConfigModelObject_${InteractionObject.getInteractionObject()}",
            null
        )

        return json?.let {
            Gson().fromJson(it, ConfigModel::class.java)
        }
    }
}


object LocalStepsObject {

    fun setLocalSteps(
        localSteps: MutableList<LocalStepModel>
    ) {

        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val json = Gson().toJson(localSteps)

        prefs.edit()
            .putString("LocalStepsObject_${InteractionObject.getInteractionObject()}", json)
            .apply()
    }

    fun getLocalSteps(
    ): MutableList<LocalStepModel> {

        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val json = prefs.getString("LocalStepsObject_${InteractionObject.getInteractionObject()}", null)

        if (json.isNullOrEmpty()) return mutableListOf()

        val type = object : TypeToken<MutableList<LocalStepModel>>() {}.type

        return Gson().fromJson(json, type)
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
    private  var passportResponseModel: PassportResponseModel? = null
    fun setPassportResponseModelObject(passportResponseModel: PassportResponseModel) {
        this.passportResponseModel = passportResponseModel
    }

    fun getPassportResponseModelObject(): PassportResponseModel? {
        return passportResponseModel
    }

    fun clear() {
       passportResponseModel = null
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


    fun setImage(value: String?) {
        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString("Base64ImageObject_${InteractionObject.getInteractionObject()}", value)
            .apply()
    }

    fun getImage(): String? {
        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        return prefs.getString("Base64ImageObject_${InteractionObject.getInteractionObject()}", null)
    }

    fun clear() {
        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .remove("Base64ImageObject_${InteractionObject.getInteractionObject()}")
            .apply()
    }
}

object IDImageObject {


    fun setImage(value: String?) {
        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString("IDImageObject_${InteractionObject.getInteractionObject()}", value)
            .apply()
    }

    fun getImage(): String? {
        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        return prefs.getString("IDImageObject_${InteractionObject.getInteractionObject()}", null)
    }

    fun clear() {
        val prefs = ContextObject.getContext().applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .remove("IDImageObject_${InteractionObject.getInteractionObject()}")
            .apply()
    }
}


object OnCompleteScreenData {
    private var data: Map<String, String>? = emptyMap()

    fun setData(value: Map<String, String>?) {
        data = value
    }

    fun getData(): Map<String, String>?? = data

    fun clear() {
        data =  emptyMap()
    }

}





