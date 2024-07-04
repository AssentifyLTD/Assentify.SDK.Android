package com.assentify.sdk.RemoteClient.Models
data class SignatureResponseModel(
    val signedDocument: String,
    val fileName: String,
    val signedDocumentUri: String
)