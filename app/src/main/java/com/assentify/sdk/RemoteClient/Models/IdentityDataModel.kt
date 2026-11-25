package com.assentify.sdk.RemoteClient.Models

import com.google.gson.annotations.SerializedName

object IdentityField {
    const val FIRST_NAME = "FirstName"
    const val LAST_NAME = "LastName"
    const val DATE_OF_BIRTH = "DateOfBirth"
    const val FATHER_NAME = "FatherName"

    // config code -> JSON key the API expects
    val jsonKeys = mapOf(
        FIRST_NAME to "firstName",
        LAST_NAME to "lastName",
        DATE_OF_BIRTH to "dob",
        FATHER_NAME to "fatherName"
    )
}

data class IdentityMatchRequest(
    @SerializedName("primary") val primary: Map<String, String>,
    @SerializedName("document") val document: Map<String, String>,
    @SerializedName("useCase") val useCase: Int = IdentityMatchUseCase.SUPPORTING_DOC
)

fun buildIdentity(
    identityFields: List<String>,
    values: Map<String, String?>
): Map<String, String> =
    identityFields
        .mapNotNull { code ->
            val key = IdentityField.jsonKeys[code] ?: return@mapNotNull null
            val value = values[code]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            key to value
        }
        .toMap()


object IdentityMatchUseCase {
    const val SUPPORTING_DOC = 0
    const val MRZ_VIZ = 1
}

data class IdentityMatchResponse(
    @SerializedName("samePerson") val samePerson: Boolean?,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("reason") val reason: String?
)