package com.assentify.sdk.RemoteClient.Models
import com.google.gson.Gson

data class ConfigModel(
    val stepId: Int,
    val flowName: String,
    val blockName: String,
    val instanceHash: String,
    val stepDefinition: Any?,
    val flowInstanceId: String,
    val tenantIdentifier: String,
    val blockIdentifier: String,
    val flowIdentifier: String,
    val customProperties: Map<String, Any>,
    val defaultLanguageId: Int,
    val instanceId: String,
    val userStateStepMap: Map<String, List<UserState>>,
    val stepDefinitions: List<StepDefinitions>
)

data class UserState(
    val id: Int,
    val key: String,
    val displayName: String,
    val isRequired: Boolean,
    val isExcluded: Boolean,
    val type: Int
)

data class StepDefinitions(
    val stepId: Int,
    val stepDefinition: String,
    val customization: Customization,
    val outputProperties: List<OutputProperties>,
)

data class OutputProperties(
    val id: Int,
    val key: String,
    val displayName: String,
    val isRequired: Boolean,
    val isExcluded: Boolean,
    val type: Int
)

data class Customization(
    val ProcessMrz: Boolean?,
    val StoreCapturedDocument: Boolean?,
    val PerformLivenessDetection: Boolean?,
    val StoreImageStream: Boolean?,
    val SaveCapturedVideo: Boolean?,
    val identificationDocuments: List<IdentificationDocuments>?
)


data class IdentificationDocuments(
  val key: String?, // IdentificationDocument.IdCard
  val selectedCountries: List<String>?,
  val supportedIdCards: List<String>,

)

fun encodeStepDefinitionsToJson(data: List<StepDefinitions>): String {
    val gson = Gson()
    return try {
        gson.toJson(data)
    } catch (e: Exception) {
        "${e.message}"
    }
}

