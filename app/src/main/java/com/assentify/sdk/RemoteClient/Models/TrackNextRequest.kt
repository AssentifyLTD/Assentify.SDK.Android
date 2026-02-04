package com.assentify.sdk.RemoteClient.Models

data class TrackNextRequest(
    val AdditionalInformation: Any? = null,
    val ApplicationId: String,
    val BlockIdentifier: String,
    val BlockType: String,
    val DeviceName: String,
    val FlowIdentifier: String,
    val FlowInstanceId: String,
    val FlowName: String,
    val InstanceHash: String,
    val IsSuccessful: Boolean,
    val Language: String,
    val NextStepDefinition: String,
    val NextStepId: Int,
    val NextStepTypeId: Int,
    val PhoneNumber: String? = null,
    val Response: Any? = null,
    val StatusCode: Int,
    val StepDefinition: String,
    val StepId: Int,
    val StepTypeId: Int,
    val TenantIdentifier: String,
    val TimeEnded: String,
    val UserAgent: String
)
