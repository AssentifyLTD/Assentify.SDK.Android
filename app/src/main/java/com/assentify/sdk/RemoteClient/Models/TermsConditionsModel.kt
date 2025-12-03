package com.assentify.sdk.RemoteClient.Models
data class TermsConditionsModel(
    val statusCode: Int,
    val data: TermsConditionsDataModel
)

data class TermsConditionsDataModel(
    val header: String?,
    val subHeader: String?,
    val file: String?,
    val nextButtonTitle: String?,
    val confirmationRequired: Boolean?,
)
