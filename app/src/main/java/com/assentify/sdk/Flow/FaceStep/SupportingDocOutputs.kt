package com.assentify.sdk.Flow.FaceStep

import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.HasSubmittedObject.FaceModelObject
import com.assentify.sdk.RemoteClient.Models.CompareFacesResponse
import com.assentify.sdk.RemoteClient.Models.IdentityMatchResponse
import org.json.JSONObject

/**
 * The output properties sent when the supporting-document step is done (Next or Skip on the result screen).
 * Shared by SupportingPassportScanActivity and SupportingIDCardScanActivity (same as SupportingDocChecks.swift).
 */
object SupportingDocOutputs {

    /** The face step's supporting-document output keys (looked up in the step's outputProperties). */
    object OutputKey {
        const val EXTRACTIONS_JSON = "OnBoardMe_FaceImageAcquisition_SupportingDocumentExtractionsJSON"
        const val IMAGE = "OnBoardMe_FaceImageAcquisition_SupportingDocumentImage"
        const val FACE = "OnBoardMe_FaceImageAcquisition_SupportingDocumentFace"
        const val FIRST_NAME = "OnBoardMe_FaceImageAcquisition_SupportingDocumentFirstName"
        const val LAST_NAME = "OnBoardMe_FaceImageAcquisition_SupportingDocumentLastName"
        const val FACE_MATCH_OUTCOME = "OnBoardMe_FaceImageAcquisition_FaceMatchOutcome"
        const val MATCH_PERCENTAGE = "OnBoardMe_FaceImageAcquisition_SupportingDocumentMatchPercentage"
        const val FinalMatchPercentage = "OnBoardMe_FaceImageAcquisition_FinalMatchPercentage"
        const val MISMATCH_FIELDS = "OnBoardMe_FaceImageAcquisition_IdentityMismatchFields"
    }

    /** Keys read from the supporting document's transformed properties (exact names). TODO: confirm. */
    object DocumentKey {
        const val ORIGINAL_IMAGE = "OnBoardMe_IdentificationDocumentCapture_OriginalFrontImage"
        const val FIRST_NAME = "OnBoardMe_IdentificationDocumentCapture_name"
        const val LAST_NAME = "OnBoardMe_IdentificationDocumentCapture_surname"
    }

    /** The values of OnBoardMe_FaceImageAcquisition_FaceMatchOutcome. */
    object FaceMatchOutcome {
        const val SUPPORTING_DOCUMENT_SUCCESS = "SupportingDocumentSuccess"
        const val SUPPORTING_DOCUMENT_SKIPPED = "SupportingDocumentSkipped"
    }

    /**
     * Everything sent when the supporting-document step is done.
     * Call it BEFORE makeCurrentStepDone(): it reads the current (face) step's outputProperties.
     */
    fun outputProperties(
        isSkip: Boolean,
        faceResult: CompareFacesResponse?,
        identityResult: IdentityMatchResponse?,
        documentProperties: Map<String, String>,
        documentFace: String,
    ): MutableMap<String, String> {
        val output = faceOutputProperties(true)

        // The face step as written in the config file, and the output keys it declares
        val currentStepId = FlowController.getCurrentStep()?.stepDefinition?.stepId
        val step = ConfigModelObject.getConfigModelObject()
            ?.stepDefinitions
            ?.firstOrNull { it.stepId == currentStepId }
        val declaredKeys = step?.outputProperties.orEmpty()
            .map { it.key }                                   // TODO: your OutputProperties key field
            .sorted()

        /** Finds the step's real key (exact first, then the first that contains the name) and writes to it. */
        fun set(name: String, value: String) {
            val key = declaredKeys.firstOrNull { it == name }
                ?: declaredKeys.firstOrNull { it.contains(name) }
                ?: return
            output[key] = value
        }

        // every extracted value of the supporting document, as one JSON string
        set(OutputKey.EXTRACTIONS_JSON, JSONObject(documentProperties.toSortedMap() as Map<*, *>).toString())

        // single values from the document
        set(OutputKey.IMAGE, documentProperties[DocumentKey.ORIGINAL_IMAGE].orEmpty())
        set(OutputKey.FIRST_NAME, documentProperties[DocumentKey.FIRST_NAME].orEmpty())
        set(OutputKey.LAST_NAME, documentProperties[DocumentKey.LAST_NAME].orEmpty())

        // the face cropped from the document (first item of faces)
        set(OutputKey.FACE, documentFace)

        // the identity fields that did not match
        set(OutputKey.MISMATCH_FIELDS, identityResult!!.reason!!)

        // the compare-faces score
        set(OutputKey.MATCH_PERCENTAGE, faceResult?.matchScore?.toString().orEmpty())
        set(OutputKey.FinalMatchPercentage, faceResult?.matchScore?.toString().orEmpty())

        // skipped or passed
        set(
            OutputKey.FACE_MATCH_OUTCOME,
            if (isSkip) FaceMatchOutcome.SUPPORTING_DOCUMENT_SKIPPED
            else FaceMatchOutcome.SUPPORTING_DOCUMENT_SUCCESS
        )

        return output
    }

    /** The face step's own output properties, with IsSkippedStatus set from isSkip. */
    private fun faceOutputProperties(isSkip: Boolean): MutableMap<String, String> {
        val output = FaceModelObject.getFaceModelObject()
            ?.faceExtractedModel
            ?.outputProperties
            .orEmpty()
            .mapValues { it.value.toString() }
            .toMutableMap()

        output.keys
            .firstOrNull { it.contains("OnBoardMe_FaceImageAcquisition_IsSkippedStatus") }
            ?.let { output[it] = isSkip.toString() }

        return output
    }

    /** The step's identity fields when the identity match failed or did not answer, otherwise "". */
    private fun mismatchFields(identityResult: IdentityMatchResponse?, identityFields: List<String>?): String =
        if (identityResult?.samePerson == true) "" else identityFields.orEmpty().joinToString(",")
}