package com.assentify.sdk.Core.Constants

object HubConnectionFunctions {
    fun etHubConnectionFunction(blockType: BlockType): String {
        return when (blockType) {
            BlockType.READ_PASSPORT -> {
                "api/IdentificationDocument/ReadPassport";
            }
            BlockType.ID_CARD -> {
                "api/IdentificationDocument/ReadId";
            }
            BlockType.OTHER -> {
                "api/IdentificationDocument/Other";
            }
            BlockType.FACE_MATCH -> {
                "api/IdentificationDocument/FaceMatchWithImage";
            }
            BlockType.QR -> {
                "api/IdentificationDocument/ReadIdQrCode";
            }

        }
    }
}
