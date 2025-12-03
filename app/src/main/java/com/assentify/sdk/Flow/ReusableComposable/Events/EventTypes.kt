package com.assentify.sdk.Flow.ReusableComposable.Events

object EventTypes {
    const val none = "none"
    const val onSend = "onSend"
    const val onError = "onError"
    const val onRetry = "onRetry"
    const val onClipPreparationComplete = "onClipPreparationComplete"
    const val onStatusUpdated = "onStatusUpdated"
    const val onUpdated = "onUpdated"
    const val onLivenessUpdate = "onLivenessUpdate"
    const val onComplete = "onComplete"
    const val onCompleteBack = "onCompleteBack"
    const val onCardDetected = "onCardDetected"
    const val onMrzExtracted = "onMrzExtracted"
    const val onMrzDetected = "onMrzDetected"
    const val onNoMrzDetected = "onNoMrzDetected"
    const val onFaceDetected = "onFaceDetected"
    const val onNoFaceDetected = "onNoFaceDetected"
    const val onFaceExtracted = "onFaceExtracted"
    const val onQualityCheckAvailable = "onQualityCheckAvailable"
    const val onDocumentCaptured = "onDocumentCaptured"
    const val onDocumentCropped = "onDocumentCropped"
    const val onUploadFailed = "onUploadFailed"
    const val onWrongTemplate = "onWrongTemplate"
}

object ContextAwareStepEventTypes {
    const val none = "none"
    const val onSend = "onSend"
    const val onError = "onError"
    const val onTokensComplete = "onComplete"
    const val onSignature = "onSignature"
}

object TermsAndConditionsEventTypes {
    const val none = "none"
    const val onSend = "onSend"
    const val onHasData = "onHasData"

}

object SubmitDataTypes {
    const val none = "none"
    const val onSend = "onSend"
    const val onError = "onError"
    const val onComplete = "onComplete"


}



