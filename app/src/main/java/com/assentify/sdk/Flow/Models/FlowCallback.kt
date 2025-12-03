package com.assentify.sdk.Flow.Models

import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel


interface FlowCallBack {
    fun onFlowCompleted(submitRequestModel: List<SubmitRequestModel>,)
}