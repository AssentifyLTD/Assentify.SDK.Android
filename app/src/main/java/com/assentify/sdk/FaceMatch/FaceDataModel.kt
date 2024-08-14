package com.assentify.sdk.FaceMatch

import com.assentify.sdk.Core.Constants.IdentificationDocumentCapture
import com.assentify.sdk.Core.Constants.fillIdentificationDocumentCapture
import org.json.JSONObject

class FaceResponseModel(
    var destinationEndpoint: String? = null,
    var faceExtractedModel: FaceExtractedModel? = null,
    var error: String? = null,
    var success: Boolean? = null
)

class FaceExtractedModel(
    var outputProperties: Map<String, Any>? = null,
    var extractedData: Map<String, Any>? = null,
    var baseImageFace: String? = null,
    var secondImageFace: String? = null,
    var percentageMatch: Int? = null,
    var isLive: Boolean? = null,
    var identificationDocumentCapture: IdentificationDocumentCapture? = null
) {
    companion object {
        fun fromJsonString(responseString: String): FaceExtractedModel? {
            try {
                val response = JSONObject(responseString)

                val baseImageFaceObject = response.optJSONObject("BaseImageFace") ?: JSONObject()
                val secondImageFaceObject = response.optJSONObject("SecondImageFace") ?: JSONObject()

                val baseImageFace = baseImageFaceObject.optString("FaceUrl")
                val secondImageFace = secondImageFaceObject.optString("FaceUrl")

                val percentageMatch = response.optDouble("PercentageMatch")?.toInt()
                val isLive = response.optBoolean("IsLive")

                val outputProperties = response.optJSONObject("OutputProperties")?.let { outputProps ->
                    val map = mutableMapOf<String, Any>()
                    outputProps.keys().forEach { key ->
                        map[key] = outputProps.get(key)
                    }
                    map
                } ?: mutableMapOf<String, Any>()

                val extractedData = mutableMapOf<String, Any>()
                outputProperties?.forEach { (key, value) ->
                    val newKey = key.substringAfter("FaceImageAcquisition_").split("_").joinToString(" ")
                    extractedData[newKey] = value
                }

                val identificationDocumentCapture = fillIdentificationDocumentCapture(outputProperties)

                return FaceExtractedModel(
                    outputProperties,
                    extractedData,
                    baseImageFace,
                    secondImageFace,
                    percentageMatch,
                    isLive,
                    identificationDocumentCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}
