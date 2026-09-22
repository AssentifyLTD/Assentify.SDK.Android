package com.assentify.sdk.DocumentCapture



interface DocumentCaptureCallback {
    fun onDocumentCaptureCallbackError(message: String)
    fun onDocumentCaptureCallbackSuccess(documentCaptureModel: DocumentCaptureModel)
    fun onUploadDocumentCaptureCallbackSuccess(documentKey: String, itemId: String, data: Map<String, String>)
    fun onUploadDocumentCaptureCallbackError(documentKey: String, itemId: String, message: String)
}