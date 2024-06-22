package com.assentify.sdk.ScanOther

import com.assentify.sdk.Core.Constants.IdentificationDocumentCapture
import com.assentify.sdk.Core.Constants.fillIdentificationDocumentCapture
import org.json.JSONObject

open class OtherResponseModel(
    var destinationEndpoint: String? = null,
    var otherExtractedModel: OtherExtractedModel? = null,
    var error: String? = null,
    var success: Boolean? = null
)

open class OtherExtractedModel(
    var outputProperties: Map<String, Any>? = null,
    var extractedData: Map<String, Any>? = null,
    var additionalDetails: Map<String, Any>? = null,
    var imageUrl: String? = null,
    var faces: List<String>? = null,
    var identificationDocumentCapture: IdentificationDocumentCapture? = null
) {
    companion object {
        fun fromJsonString(responseString: String): OtherExtractedModel? {
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

                val imageUrl = response.optString("ImageUrl")
                val outputProperties = response.optJSONObject("OutputProperties")?.let { outputProps ->
                    val map = mutableMapOf<String, Any>()
                    outputProps.keys().forEach { key ->
                        map[key] = outputProps.get(key)
                    }
                    map
                }

                val additionalDetails = response.optJSONObject("AdditionalDetails")?.let { additionalDetail ->
                    val map = mutableMapOf<String, Any>()
                    additionalDetail.keys().forEach { key ->
                        map[key] = additionalDetail.get(key)
                    }
                    map
                }


                val extractedData = mutableMapOf<String, Any>()
                outputProperties?.forEach { (key, value) ->
                    val newKey = key.split("_").last()
                    extractedData[newKey] = value
                }

                val identificationDocumentCapture = fillIdentificationDocumentCapture(outputProperties)

                OtherExtractedModel(outputProperties, extractedData,additionalDetails, imageUrl, faces, identificationDocumentCapture)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}