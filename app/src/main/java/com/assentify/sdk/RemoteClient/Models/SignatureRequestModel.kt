package com.assentify.sdk.RemoteClient.Models
data class SignatureRequestModel(
    val documentId: Int,
    val documentInstanceId: Int,
    val documentName: String,
    val username: String,
    val requiresAdditionalData: Boolean,
    val signature: String,
)
