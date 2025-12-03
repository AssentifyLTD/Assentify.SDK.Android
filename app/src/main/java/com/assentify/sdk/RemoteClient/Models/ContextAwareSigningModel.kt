package com.assentify.sdk.RemoteClient.Models
data class ContextAwareSigningModel(
    val statusCode: Int,
    val data: DataModel
)

data class DataModel(
    val selectedTemplates: List<Int>,
    val header: String?,
    val confirmationMessage: String?,
    val subHeader: String?,
)
