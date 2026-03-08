package  com.assentify.sdk
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import  com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry

interface AssentifySdkCallback {
    fun onAssentifySdkInitError(message: String)
    fun onAssentifySdkInitSuccess(configModel: ConfigModel)

}
