package com.assentify.sdk.Models

import com.google.gson.Gson
import org.json.JSONObject

data class BaseResponseDataModel(
    var destinationEndpoint: String?,
    var response: String?,
    var error: String?,
    var success: Boolean?,
    var classifiedTemplate: String,

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
        error = json.optString("error"),
        success = json.optBoolean("success", false),
        classifiedTemplate = json.optString("classifiedTemplate", "")
    )
}


fun getImageUrlFromBaseResponseDataModel(jsonString: String?): String {
    if (jsonString.isNullOrBlank()) {
        return ""
    }

    return try {
        val json = JSONObject(jsonString)
        json.optString("ImageUrl", "")
    } catch (e: Exception) {
        ""
    }
}

