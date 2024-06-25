package com.assentify.sdk.SubmitData

interface SubmitDataCallback {
    fun onSubmitError(message: String)
    fun onSubmitSuccess(message: String)
}
