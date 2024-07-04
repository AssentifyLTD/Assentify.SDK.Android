package com.assentify.sdk.RemoteClient.Models
data class DocumentTokensModel(
    val id: Int,
    val templateId: Int,
    val tokenValue: String,
    val displayName: String,
    val tokenTypeEnum: Int,
)