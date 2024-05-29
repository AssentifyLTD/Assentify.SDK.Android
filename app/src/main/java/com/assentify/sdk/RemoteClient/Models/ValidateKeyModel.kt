package com.assentify.sdk.RemoteClient.Models

data class ValidateKeyModel(
    val message: String,
    val statusCode: Int,
    val isSuccessful: Boolean,
)

