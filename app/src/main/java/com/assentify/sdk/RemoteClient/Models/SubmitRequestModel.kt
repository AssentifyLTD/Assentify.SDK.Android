package com.assentify.sdk.RemoteClient.Models
import org.json.JSONArray
import org.json.JSONObject

data class SubmitRequestModel(
    val stepId: Int,
    val stepDefinition: String,
    var extractedInformation: Map<String, String>
)

fun parseSubmitRequestModelList(jsonString :String) : List<SubmitRequestModel> {
    val json = JSONArray(jsonString)
    val submitRequestList = mutableListOf<SubmitRequestModel>()

    for (i in 0 until json.length()) {
        val jsonObject = json.getJSONObject(i)
        val stepId = jsonObject.getInt("stepId")
        val stepDefinition = jsonObject.getString("stepDefinition")
        val extractedInformationObject = jsonObject.getJSONObject("extractedInformation")
        val extractedInformation = extractedInformationObject.toMap()
        val submitRequestModel = SubmitRequestModel(stepId, stepDefinition, extractedInformation)
        submitRequestList.add(submitRequestModel)
    }

    return  submitRequestList;
};

fun JSONObject.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = this.getString(key)
        map[key] = value
    }
    return map
}