package com.assentify.sdk.Models


data class IdDocumentCaptureRequest(
    val connectionId: String?,
    val tenantId: String?,
    val blockId: String?,
    val instanceId: String?,
    val totalNumberOfClipsProcessed: Int,
    val traceIdentifier: String?,
    val isMobile: Boolean,
    val saveCapturedVideo: Boolean,
    val storeCapturedDocument: Boolean,
    val userAgentString: String?,
    val mime: String?,
    val image: String?,
    val requireFaceExtraction: Boolean,
    val enableSlimProcessing: Boolean,
    val storeImageStream: Boolean,
    val processMrz: Boolean,
    val templateId: String?,
    val ipAddress: String?,
    val checkForFace: Boolean,
    val isLivenessEnabled: Boolean,
    val videoClipB64: String?,
    val IsVideo: Boolean,
    val Clips: List<String>,
    val secondImage: String,

    )


fun prepareRequest(
    tenantId : String,
    blockId: String,
    instanceId : String,
    message: String,
    templateId: String,
    secondImage: String,
    checkForFace: Boolean,
    processMrz: Boolean,
    performLivenessDetection: Boolean,
    saveCapturedVideo: Boolean,
    storeCapturedDocument: Boolean,
    storeImageStream: Boolean,
    clips: List<String>
): IdDocumentCaptureRequest {
    val idDocumentCaptureRequest: IdDocumentCaptureRequest;
    if (secondImage.isNotEmpty()) {
        idDocumentCaptureRequest = IdDocumentCaptureRequest(
            tenantId = tenantId,
            blockId = blockId,
            instanceId = instanceId,
            connectionId = "",
            totalNumberOfClipsProcessed = 12,
            isMobile = true,
            image = "base64EncodedStringOrPlaceholder",
            requireFaceExtraction = false,
            enableSlimProcessing = true,
            processMrz = processMrz,
            templateId = "",
            ipAddress = "sampleIpAddress",
            checkForFace = checkForFace,
            userAgentString = "",
            traceIdentifier = "traceIdentifier",
            mime = "video/mp4",
            isLivenessEnabled = performLivenessDetection,
            IsVideo = false,
            videoClipB64 = message,
            Clips = clips,
            secondImage = secondImage,
            saveCapturedVideo = saveCapturedVideo,
            storeCapturedDocument = storeCapturedDocument,
            storeImageStream = storeImageStream,

            )
    } else {
        idDocumentCaptureRequest = IdDocumentCaptureRequest(
            tenantId = tenantId,
            blockId = blockId,
            instanceId = instanceId,
            connectionId = "",
            totalNumberOfClipsProcessed = 12,
            isMobile = true,
            image = "base64EncodedStringOrPlaceholder",
            requireFaceExtraction = false,
            enableSlimProcessing = true,
            processMrz = processMrz,
            templateId = templateId,
            ipAddress = "sampleIpAddress",
            checkForFace = checkForFace,
            userAgentString = "",
            traceIdentifier = "traceIdentifier",
            mime = "video/mp4",
            isLivenessEnabled = false,
            IsVideo = false,
            videoClipB64 = message,
            secondImage = secondImage,
            Clips = emptyList(),
            saveCapturedVideo = saveCapturedVideo,
            storeCapturedDocument = storeCapturedDocument,
            storeImageStream = storeImageStream,
        )
    }
    return idDocumentCaptureRequest;
}





