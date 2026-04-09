import android.content.Context
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.TenantThemeModel
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

class ConfigFileManager(
    private val context: Context,
    private val fileName: String
) {

    private val file: File
        get() = File(context.filesDir, fileName)

    private fun readFromAssets(): String {
        return context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun validateJson(content: String) {
        try {
            JSONObject(content)
        } catch (e: Exception) {
            try {
                JSONArray(content)
            } catch (e2: Exception) {
                throw IllegalArgumentException("Invalid JSON content")
            }
        }
    }
    fun initFromAssetsIfNeeded() {
        if (!file.exists()) {
            val json = readFromAssets()
            file.writeText(json)
        }
    }


    fun read(): String? {
        return try {
            file.readText()
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun readEngagement(): ConfigModel? {
        val json = read() ?: return null

        return try {
            val jsonObject = JSONObject(json)
            val engagementJson = jsonObject.getJSONObject("engagement")
            Gson().fromJson(engagementJson.toString(), ConfigModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun readTheme(): TenantThemeModel? {
        val json = read() ?: return null

        return try {
            val jsonObject = JSONObject(json)
            val themeJson = jsonObject.getJSONObject("theme")
            Gson().fromJson(themeJson.toString(), TenantThemeModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun write(newContent: String) {
        validateJson(newContent)
        file.writeText(newContent)
    }


    fun clear() {
        // You can choose:
        // file.writeText("") // empty
        file.writeText("{}") // safer default JSON
    }



}