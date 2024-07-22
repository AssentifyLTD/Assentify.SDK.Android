package com.assentify.sdk.LanguageTransformation

interface LanguageTransformationCallback {
    fun onTranslatedSuccess(properties :Map<String, String>?)
    fun onTranslatedError(properties :Map<String, String>?)
}