package com.assentify.sdk.RemoteClient.Models
data class CreateUserDocumentResponseModel(
    val templateInstance: String,
    val templateInstanceId: Int,
    val documentId: Int,
    val isPdf: Boolean
)