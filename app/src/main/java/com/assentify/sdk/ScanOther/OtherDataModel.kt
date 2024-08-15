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
    var transformedProperties: Map<String, String>? = null,
    var extractedData: Map<String, Any>? = null,
    var additionalDetails: Map<String, Any>? = null,
    var transformedDetails: Map<String, String>? = null,
    var imageUrl: String? = null,
    var faces: List<String>? = null,
    var identificationDocumentCapture: IdentificationDocumentCapture? = null
) {
    companion object {
        fun fromJsonString(
            responseString: String,
            transformedProperties: Map<String, String>?,
            transformedDetails: Map<String, String>?,
        ): OtherExtractedModel? {
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
                val outputProperties =
                    response.optJSONObject("OutputProperties")?.let { outputProps ->
                        val map = mutableMapOf<String, Any>()
                        outputProps.keys().forEach { key ->
                            map[key] = outputProps.get(key)
                        }
                        map
                    }

                val additionalDetails =
                    response.optJSONObject("AdditionalDetails")?.let { additionalDetail ->
                        val map = mutableMapOf<String, Any>()
                        additionalDetail.keys().forEach { key ->
                            map[key] = additionalDetail.get(key)
                        }
                        map
                    }




                val identificationDocumentCapture =
                    fillIdentificationDocumentCapture(outputProperties)

                val transformedPropertiesResult: MutableMap<String, String> = mutableMapOf()
                if (transformedProperties!!.isEmpty()) {
                    outputProperties?.forEach { (key, value) ->
                        if (value.toString().isNotEmpty()) {
                            transformedPropertiesResult.put(key, value.toString())
                        }
                    }
                } else {
                    transformedPropertiesResult.putAll(transformedProperties)
                }

                val transformedDetailsResult: MutableMap<String, String> = mutableMapOf()
                if (transformedDetails!!.isEmpty()) {
                    additionalDetails?.forEach { (key, value) ->
                        if (value.toString().isNotEmpty()) {
                            transformedDetailsResult.put(key, value.toString())
                        }
                    }
                } else {
                    transformedDetailsResult.putAll(transformedDetails)
                }

                val extractedData = mutableMapOf<String, Any>()
                transformedPropertiesResult?.forEach { (key, value) ->
                    val newKey = key.substringAfter("IdentificationDocumentCapture_").split("_").joinToString(" ")
                    extractedData[newKey] = value
                }

                OtherExtractedModel(
                    outputProperties,
                    transformedPropertiesResult,
                    extractedData,
                    additionalDetails,
                    transformedDetailsResult,
                    imageUrl,
                    faces,
                    identificationDocumentCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}