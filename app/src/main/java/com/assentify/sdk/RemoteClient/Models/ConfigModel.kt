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
    val applicationId: String,
    val userStateStepMap: Map<String, List<UserState>>,
    val stepDefinitions: List<StepDefinitions>,
    val stepMap: List<StepMap>
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
    val processMrz: Boolean?,
    val documentLiveness: Boolean?,
    val storeCapturedDocument: Boolean?,
    val performLivenessDetection: Boolean?,
    val storeImageStream: Boolean?,
    val saveCapturedVideo: Boolean?,
    val identificationDocuments: List<IdentificationDocuments>?
)

data class StepMap(
    val id: Int,
    val stepType: Int,
    val stepName: String,
    val stepDefinition: String,
    val parentStepId: Int?,
    val branches: Any?,
    val numberOfBranches: Int?,
    val isVirtual: Boolean,
    val stepMapBranches: Any?
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

fun decodeConfigModelFromJson(jsonString: String): ConfigModel? {
    return try {
        val gson = Gson()
        gson.fromJson(jsonString, ConfigModel::class.java)
    } catch (e: Exception) {
        null
    }
}
