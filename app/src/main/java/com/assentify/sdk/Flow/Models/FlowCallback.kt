package com.assentify.sdk.Flow.Models



interface FlowCallBack {
    fun onStepCompleted(stepModel: FlowCompletedModel,)
    fun onFlowCompleted(flowData: List<FlowCompletedModel>,)
}