package com.assentify.sdk.ScanPassport

import com.assentify.sdk.Core.Constants.IdentificationDocumentCapture
import com.assentify.sdk.Core.Constants.MrzKeys
import com.assentify.sdk.Core.Constants.fillIdentificationDocumentCapture
import com.assentify.sdk.RemoteClient.Models.OutputProperties
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

                val extractedData = mutableMapOf<String, Any>()
                transformedPropertiesResult?.forEach { (key, value) ->
                    val newKey = key.substringAfter("IdentificationDocumentCapture_").split("_").joinToString(" ")
                    extractedData[newKey] = value
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


        fun fromOutputProperties(
            passportImageUrl: String,
            transformedProperties: Map<String, String>?,
            stepOutputProperties: List<OutputProperties>,
            mrzResult : Map<String, Object>
        ): PassportExtractedModel? {
            return try {

                val faces = mutableListOf<String>()
                val imageUrl = passportImageUrl

                val outputProperties = buildOutputProperties(passportImageUrl,stepOutputProperties,mrzResult)



                val identificationDocumentCapture = fillIdentificationDocumentCapture(outputProperties)

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

                val extractedData = mutableMapOf<String, Any>()
                transformedPropertiesResult?.forEach { (key, value) ->
                    val newKey = key.substringAfter("IdentificationDocumentCapture_").split("_").joinToString(" ")
                    extractedData[newKey] = value
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


fun buildOutputProperties(imageUrl: String, stepOutputProperties: List<OutputProperties>,
                          mrzResult : Map<String, Object>): Map<String, Any>{

    //


    val outputPropertiesResult: MutableMap<String, Any> = mutableMapOf()

    stepOutputProperties.forEach {
        when {
            it.key.contains(MrzKeys.KEY_FIRST_NAME) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_FIRST_NAME] ?: ""
            it.key.contains(MrzKeys.KEY_LAST_NAME) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_LAST_NAME] ?: ""
            it.key.contains(MrzKeys.KEY_BIRTH_DATE) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_BIRTH_DATE] ?: ""
            it.key.contains(MrzKeys.KEY_DOCUMENT_NUMBER) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_DOCUMENT_NUMBER] ?: ""
            it.key.contains(MrzKeys.KEY_SEX) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_SEX] ?: ""
            it.key.contains(MrzKeys.KEY_EXPIRY_DATE) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_EXPIRY_DATE] ?: ""
            it.key.contains(MrzKeys.KEY_COUNTRY) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_COUNTRY] ?: ""
            it.key.contains(MrzKeys.KEY_NATIONALITY) ->
                outputPropertiesResult[it.key] = mrzResult[MrzKeys.KEY_COUNTRY] ?: ""
            it.key.contains("IdentificationDocumentCapture_IDType") ->
                outputPropertiesResult[it.key] = "Passport"
            it.key.contains("OnBoardMe_IdentificationDocumentCapture_Image") ->
                outputPropertiesResult[it.key] = imageUrl
            it.key.contains(MrzKeys.KEY_DOCUMENT_TYPE) ->
                outputPropertiesResult[it.key] = "Passport"
            else ->
                outputPropertiesResult[it.key] = ""
        }
    }

    return outputPropertiesResult;
}