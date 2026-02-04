package com.assentify.sdk.RemoteClient.Models

data class TrackProgressRequest(
    val TenantIdentifier: String,
    val FlowIdentifier: String,
    val FlowInstanceId: String,
    val ApplicationId: String,
    val BlockIdentifier: String,
    val InstanceHash: String,
    val FlowName: String,
    val StepDefinition: String,
    val StepId: Int,
    val StepTypeId: Int,
    val Status: String,
    val DeviceName: String,
    val UserAgent: String,
    val Timestamp: String,
    val Language: String,
    val InputData: Any? = null,
    val Response: Any? = null,

)
