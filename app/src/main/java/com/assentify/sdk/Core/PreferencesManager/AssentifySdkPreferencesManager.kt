import android.content.Context
import android.content.SharedPreferences
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.google.gson.Gson

class AssentifySdkPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PreferencesKeys.PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()


    fun saveAssentifyPreferencesData(
        apiKey: String,
        configModel: ConfigModel,
        tenantIdentifier: String,
        interaction: String,
        environmentalConditions: EnvironmentalConditions,
        processMrz: Boolean?,
        storeCapturedDocument: Boolean?,
        performLivenessDetection: Boolean?,
        storeImageStream: Boolean?,
        saveCapturedVideoID: Boolean?,
        saveCapturedVideoFace: Boolean?
    ) {
        with(sharedPreferences.edit()) {
            putString(PreferencesKeys.API_KEY, apiKey)
            putString(PreferencesKeys.CONFIG_MODEL, gson.toJson(configModel))
            putString(PreferencesKeys.TENANT_IDENTIFIER, tenantIdentifier)
            putString(PreferencesKeys.INTERACTION, interaction)
            putString(PreferencesKeys.ENVIRONMENTAL_CONDITIONS, gson.toJson(environmentalConditions))
            putBoolean(PreferencesKeys.PROCESS_MRZ, processMrz ?: false)
            putBoolean(PreferencesKeys.STORE_CAPTURED_DOCUMENT, storeCapturedDocument ?: false)
            putBoolean(PreferencesKeys.PERFORM_LIVENESS_DETECTION, performLivenessDetection ?: false)
            putBoolean(PreferencesKeys.STORE_IMAGE_STREAM, storeImageStream ?: false)
            putBoolean(PreferencesKeys.SAVE_CAPTURED_VIDEO_ID, saveCapturedVideoID ?: false)
            putBoolean(PreferencesKeys.SAVE_CAPTURED_VIDEO_FACE, saveCapturedVideoFace ?: false)
            apply()
        }
    }

    fun getAssentifyPreferencesData(): AssentifyPreferencesData? {
        val apiKey = sharedPreferences.getString(PreferencesKeys.API_KEY, null)
        val configModelJson = sharedPreferences.getString(PreferencesKeys.CONFIG_MODEL, null)
        val tenantIdentifier = sharedPreferences.getString(PreferencesKeys.TENANT_IDENTIFIER, null)
        val interaction = sharedPreferences.getString(PreferencesKeys.INTERACTION, null)
        val environmentalConditionsJson = sharedPreferences.getString(PreferencesKeys.ENVIRONMENTAL_CONDITIONS, null)

        if (apiKey.isNullOrEmpty() ||
            configModelJson.isNullOrEmpty() ||
            tenantIdentifier.isNullOrEmpty() ||
            interaction.isNullOrEmpty() ||
            environmentalConditionsJson.isNullOrEmpty()
        ) {
            return null
        }

        val configModel = gson.fromJson(configModelJson, ConfigModel::class.java)
        val environmentalConditions = gson.fromJson(environmentalConditionsJson, EnvironmentalConditions::class.java)

        return AssentifyPreferencesData(
            apiKey = apiKey,
            configModel = configModel,
            tenantIdentifier = tenantIdentifier,
            interaction = interaction,
            environmentalConditions = environmentalConditions,
            processMrz = sharedPreferences.getBoolean(PreferencesKeys.PROCESS_MRZ, false),
            storeCapturedDocument = sharedPreferences.getBoolean(PreferencesKeys.STORE_CAPTURED_DOCUMENT, false),
            performLivenessDetection = sharedPreferences.getBoolean(PreferencesKeys.PERFORM_LIVENESS_DETECTION, false),
            storeImageStream = sharedPreferences.getBoolean(PreferencesKeys.STORE_IMAGE_STREAM, false),
            saveCapturedVideoID = sharedPreferences.getBoolean(PreferencesKeys.SAVE_CAPTURED_VIDEO_ID, false),
            saveCapturedVideoFace = sharedPreferences.getBoolean(PreferencesKeys.SAVE_CAPTURED_VIDEO_FACE, false)
        )
    }
}
