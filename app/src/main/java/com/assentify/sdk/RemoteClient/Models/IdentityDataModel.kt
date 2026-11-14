package com.assentify.sdk.RemoteClient.Models

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
    val primary: Map<String, String>,
    val document: Map<String, String>,
    val useCase: Int = IdentityMatchUseCase.SUPPORTING_DOC
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
    val samePerson: Boolean?,
    val confidence: Double?,
    val reason: String?
)