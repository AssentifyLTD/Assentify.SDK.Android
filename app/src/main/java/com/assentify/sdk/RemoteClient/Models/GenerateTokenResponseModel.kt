package com.assentify.sdk.RemoteClient.Models
data class GenerateTokenResponseModel (
    val access_token: String,
    val token_type: String,
    val refresh_token: String,
)