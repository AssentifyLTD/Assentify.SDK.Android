package com.assentify.sdk.RemoteClient.Models

import com.google.gson.annotations.SerializedName

data class CompareFacesRequest(
    @SerializedName("SourceImageHash") val sourceImageHash: String,
    @SerializedName("TargetImageHash") val targetImageHash: String
)

data class CompareFacesResponse(
    val scanStatus: String?,
    val status: Int?,
    val matchScore: Int?,
    val isLive: Boolean?,
    val error: String?
)