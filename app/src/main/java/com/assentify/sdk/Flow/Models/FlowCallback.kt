package com.assentify.sdk.Flow.Models

import FlowCompletedModel


interface FlowCallBack {
    fun onFlowCompleted(flowData: List<FlowCompletedModel>,)
}