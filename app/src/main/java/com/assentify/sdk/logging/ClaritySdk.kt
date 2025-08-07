package com.assentify.sdk.logging

import android.app.Activity
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.LogLevel

object ClaritySdk {

    private var initialized = false
    fun initialize(activity: Activity) {
        if (!initialized) {
            val config = ClarityConfig(
                projectId = ConstantsValues.ClarityProjectId,
                logLevel = LogLevel.Verbose
            )
            Clarity.initialize(activity, config)
            initialized = true
        }
    }

}
