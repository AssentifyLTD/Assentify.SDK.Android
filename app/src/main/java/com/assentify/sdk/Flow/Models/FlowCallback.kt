package com.assentify.sdk.Flow.Models



interface FlowCallBack {
    fun onFlowCompleted(flowData: List<FlowCompletedModel>,)
}