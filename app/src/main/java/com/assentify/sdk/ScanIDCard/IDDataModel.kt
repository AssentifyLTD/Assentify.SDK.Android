package com.assentify.sdk.ScanIDCard

import com.assentify.sdk.Core.Constants.IdentificationDocumentCapture
import com.assentify.sdk.Core.Constants.fillIdentificationDocumentCapture
import org.json.JSONObject

open class IDResponseModel(
    var destinationEndpoint: String? = null,
    var iDExtractedModel: IDExtractedModel? = null,
    var error: String? = null,
    var success: Boolean? = null
)

open class IDExtractedModel(
    var outputProperties: Map<String, Any>? = null,
    var extractedData: Map<String, Any>? = null,
    var imageUrl: String? = null,
    var faces: List<String>? = null,
    var identificationDocumentCapture: IdentificationDocumentCapture? = null
) {
    companion object {
        fun fromJsonString(responseString: String): IDExtractedModel? {
            return try {
                val response = JSONObject(responseString)

                val faces = mutableListOf<String>()
                response.optJSONArray("faces")?.let { faceArray ->
                    for (i in 0 until faceArray.length()) {
                        faceArray.optJSONObject(i)?.getString("FaceUrl")?.let { faceUrl ->
                            faces.add(faceUrl)
                        }
                    }
                }

                val imageUrl = response.optString("IdCardImageUrl")
                val outputProperties = response.optJSONObject("OutputProperties")?.let { outputProps ->
                    val map = mutableMapOf<String, Any>()
                    outputProps.keys().forEach { key ->
                        map[key] = outputProps.get(key)
                    }
                    map
                }

                val extractedData = mutableMapOf<String, Any>()
                outputProperties?.forEach { (key, value) ->
                    val newKey = key.split("_").last()
                    extractedData[newKey] = value
                }

                val identificationDocumentCapture = fillIdentificationDocumentCapture(outputProperties)

                IDExtractedModel(outputProperties, extractedData, imageUrl, faces, identificationDocumentCapture)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}