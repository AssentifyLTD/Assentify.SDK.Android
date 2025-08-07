package com.assentify.sdk.logging

import android.app.Activity
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Severity
import com.bugsnag.android.performance.BugsnagPerformance


object BugsnagObject {

    private var initialized = false
    fun initialize(activity: Activity,configModel:ConfigModel) {
        if (!initialized) {
            Bugsnag.start(activity.applicationContext)
            BugsnagPerformance.start(activity.applicationContext)
            logInfo("Sdk started successfully", configModel)
            initialized = true
        }
    }


    fun logInfo(message: String, configModel: ConfigModel) {
        Bugsnag.notify(Exception(message)) { event ->
            event.severity = Severity.INFO
            event.context = extractConfigMap(message,configModel).toString()
            extractConfigMap(message,configModel).forEach { (key, value) ->
                event.addMetadata("configmodel", key, value)
            }
            true
        }
    }


    fun logError(
        exception: Throwable,
        configModel: ConfigModel,
    ) {
        Bugsnag.notify(exception) { event ->
            exception.message?.let {
                extractConfigMap(it,configModel).forEach { (key, value) ->
                    event.addMetadata("configmodel", key, value)
                }
            }
            true
        }
    }

    private fun extractConfigMap(message:String,configModel: ConfigModel): Map<String, Any> {
        return mapOf(
            "message" to message,
            "flowName" to configModel.flowName,
            "blockName" to configModel.blockName,
            "instanceHash" to configModel.instanceHash,
            "flowInstanceId" to configModel.flowInstanceId,
            "tenantIdentifier" to configModel.tenantIdentifier,
            "blockIdentifier" to configModel.blockIdentifier,
            "flowIdentifier" to configModel.flowIdentifier,
            "instanceId" to configModel.instanceId
        )
    }


}

