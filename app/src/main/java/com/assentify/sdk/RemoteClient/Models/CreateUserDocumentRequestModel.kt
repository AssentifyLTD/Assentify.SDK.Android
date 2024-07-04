package com.assentify.sdk.RemoteClient.Models
data class CreateUserDocumentRequestModel(
    val userId: String,
    val documentTemplateId: Int,
    val data: Map<String, String>,
    val outputType: Int
)