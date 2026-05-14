package com.assentify.sdk.RemoteClient.Models
data class SignatureRequestModel(
    val documentId: Int,
    val documentInstanceId: Int,
    val documentName: String,
    val username: String,
    val requiresAdditionalData: Boolean,
    val signature: String,
    var signerName: String? = null,
    var otpCode: String?= null,
    var otpLine: String?= null,
    var deviceLine: String?= null,
    var hasOtp: Boolean?= null,
    var signerContact: String?= null,
    var enableVisualVerifier: Boolean?= null,
    var faceImageUrl: String?= null,
)
