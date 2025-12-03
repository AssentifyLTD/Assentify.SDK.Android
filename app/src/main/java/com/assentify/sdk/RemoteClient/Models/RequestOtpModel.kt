package com.assentify.sdk.RemoteClient.Models


data class RequestOtpModel(
    val token: String,
    val inputType: String,
    val otpSize: Int,
    val otpType: Int,
    val otpExpiryTime: Double,
)

data class RequestOtpResponseModel(
    val message: String?,
    val error: String?,
    val statusCode: Int,
    val isSuccessful: Boolean,
    val data: Boolean,
    val otpExpiryTime: Double,
)


data class VerifyOtpRequestOtpModel(
    val token: String,
    val otp: String,
    val otpExpiryTime: Double,
)

data class VerifyOtpResponseOtpModel(
    val message: String?,
    val error: String?,
    val statusCode: Int,
    val isSuccessful: Boolean,
    val data: Boolean,
)



