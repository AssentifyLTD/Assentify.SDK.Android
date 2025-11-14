package com.assentify.sdk.Models
import com.google.gson.Gson
import org.json.JSONObject

data class BaseResponseDataModel(
    var destinationEndpoint: String?,
    var response: String?,
    var error: String?,
    var success: Boolean?,

)


fun encodeBaseResponseDataModelToJson(data: BaseResponseDataModel?): String {
    val gson = Gson()
    return try {
        gson.toJson(data)
    } catch (e: Exception) {
        "${e.message}"
    }
}

fun parseDataToBaseResponseDataModel(data: String): BaseResponseDataModel {
    val json = JSONObject(data)
    return BaseResponseDataModel(
        destinationEndpoint = json.optString("destinationEndpoint", ""),
        response = json.optString("response", ""),
        error = json.optString("error", null),
        success = json.optBoolean("success", false)
    )
}

fun getImageUrlFromBaseResponseDataModel(jsonString: String): String {
    val json = JSONObject(jsonString)
    val imageUrl = json.optString("ImageUrl", "")
    return  imageUrl;
}



