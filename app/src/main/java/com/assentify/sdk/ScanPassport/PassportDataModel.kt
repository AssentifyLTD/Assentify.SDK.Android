package com.assentify.sdk.ScanPassport

import com.assentify.sdk.Core.Constants.IdentificationDocumentCapture
import com.assentify.sdk.Core.Constants.fillIdentificationDocumentCapture
import org.json.JSONObject

open class PassportResponseModel(
    var destinationEndpoint: String? = null,
    var passportExtractedModel: PassportExtractedModel? = null,
    var error: String? = null,
    var success: Boolean? = null
)

open class PassportExtractedModel(
    var outputProperties: Map<String, Any>? = null,
    var transformedProperties: Map<String, String>? = null,
    var extractedData: Map<String, Any>? = null,
    var imageUrl: String? = null,
    var faces: List<String>? = null,
    var identificationDocumentCapture: IdentificationDocumentCapture? = null
) {
    companion object {
        fun fromJsonString(
            responseString: String,
            transformedProperties: Map<String, String>?
        ): PassportExtractedModel? {
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

                val extractedData = mutableMapOf<String, Any>()
                outputProperties?.forEach { (key, value) ->
                    val newKey = key.substringAfter("IdentificationDocumentCapture_").split("_").joinToString(" ")
                    extractedData[newKey] = value
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
                PassportExtractedModel(
                    outputProperties,
                    transformedPropertiesResult,
                    extractedData,
                    imageUrl,
                    faces,
                    identificationDocumentCapture,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}