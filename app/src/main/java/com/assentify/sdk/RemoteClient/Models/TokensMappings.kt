package com.assentify.sdk.RemoteClient.Models

data class TokensMappings(
    val id: Int,
    val tokenId: Int,
    val aBlockIdentifier: String,
    val stepId: Int,
    val aFlowPropertyIdentifier: String,
    val flowName: String,
    val blockName: String,
    val stepName: String,
    val displayName: String,
    val sourceKey: String,
    val isDeleted: Boolean,
    val type: Int
)
