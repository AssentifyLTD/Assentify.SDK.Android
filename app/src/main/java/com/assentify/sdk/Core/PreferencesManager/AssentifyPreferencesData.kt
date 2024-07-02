import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.RemoteClient.Models.ConfigModel

data class AssentifyPreferencesData(
    val apiKey: String,
    val configModel: ConfigModel?,
    val tenantIdentifier: String,
    val interaction: String,
    val environmentalConditions: EnvironmentalConditions?,
    val processMrz: Boolean,
    val storeCapturedDocument: Boolean,
    val performLivenessDetection: Boolean,
    val storeImageStream: Boolean,
    val saveCapturedVideoID: Boolean,
    val saveCapturedVideoFace: Boolean
)