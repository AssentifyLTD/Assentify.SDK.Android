package  com.assentify.sdk
import com.assentify.sdk.RemoteClient.Models.ConfigModel

interface AssentifySdkCallback {
    fun onAssentifySdkInitError(message: String)
    fun onAssentifySdkInitSuccess(configModel: ConfigModel?)

}
