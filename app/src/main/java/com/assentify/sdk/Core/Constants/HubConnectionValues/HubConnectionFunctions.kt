package com.assentify.sdk.Core.Constants

object HubConnectionFunctions {
    fun etHubConnectionFunction(blockType: BlockType): String {
        return when (blockType) {
            BlockType.READ_PASSPORT -> {
                "v2/api/IdentificationDocument/ReadPassport";
            }
            BlockType.ID_CARD -> {
                "v2/api/IdentificationDocument/ReadId";
            }
            BlockType.OTHER -> {
                "v2/api/IdentificationDocument/Other";
            }
            BlockType.FACE_MATCH -> {
                "api/IdentificationDocument/FaceMatchWithImage";
            }
            BlockType.QR -> {
                "v2/api/IdentificationDocument/ReadIdQrCode";
            }

        }
    }
}
