package com.assentify.sdk.DocumentCapture

public data class DocumentCaptureModel(
    val header: String?,
    val subHeader: String?,
    val svgLogoUrl: String?,
    val documentCaptures: List<DocumentCaptures>?,
    val nextButtonTitle: String?,
    val isNormalClick: Boolean?,
)

public data class DocumentCaptures(
    val id: String?,
    val documentNameKey: String?,
    val documentUploadTimeKey: String?,
    val keyIdentifier: String?,
    val documentTitle: String?,
    val mandatory: Boolean?,
    val allowLiveCapture: Boolean?,
    val maxLiveCaptureLength: Int?,
    val allowFileUpload: Boolean?,
    val enableAutoCrop: Boolean?,
    val allowedFormats: List<String>?,
    val maxFileSize: Int?,
    val minLiveCaptureImages: Int?,
    val maxLiveCaptureImages: Int?,
)