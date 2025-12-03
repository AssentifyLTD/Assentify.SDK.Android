package com.assentify.sdk.Flow.Models

import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel

data class LocalStepModel(
    val name: String,
    val show: Boolean = true,
    val description: String,
    val iconAssetPath: String,
    var isDone: Boolean = false,
    val stepDefinition: StepDefinitions?,
    var submitRequestModel: SubmitRequestModel?,
)