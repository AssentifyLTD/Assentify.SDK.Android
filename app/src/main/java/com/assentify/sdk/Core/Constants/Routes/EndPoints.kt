package com.assentify.sdk.Core.Constants.Routes

import  com.assentify.sdk.Core.Constants.BlockType

object EndPoints {
    const val IdentificationDocumentCapture =
        BaseUrls.SignalRHub + "identification-document-capture";
    const val FaceMatch = BaseUrls.SignalRHub + "extract-face";
}

object EndPointsUrls {
    fun etEndPointsUrls(blockType: BlockType): String {
        return when (blockType) {
            BlockType.READ_PASSPORT -> {
                EndPoints.IdentificationDocumentCapture
            }
            BlockType.ID_CARD -> {
                EndPoints.IdentificationDocumentCapture
            }
            BlockType.OTHER -> {
                EndPoints.IdentificationDocumentCapture
            }
            BlockType.FACE_MATCH -> {
                EndPoints.FaceMatch
            }
        }
    }
}

