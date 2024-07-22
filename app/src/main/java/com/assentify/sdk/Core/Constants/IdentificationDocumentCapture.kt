package com.assentify.sdk.Core.Constants

import DataType
import LanguageTransformationModel
import TransformationModel
import android.util.Log


object IdentificationDocumentCaptureKeys {
    const val name = "OnBoardMe_IdentificationDocumentCapture_name"
    const val surname = "OnBoardMe_IdentificationDocumentCapture_surname"
    const val documentType = "OnBoardMe_IdentificationDocumentCapture_Document_Type"
    const val birthDate = "OnBoardMe_IdentificationDocumentCapture_Birth_Date"
    const val documentNumber = "OnBoardMe_IdentificationDocumentCapture_Document_Number"
    const val sex = "OnBoardMe_IdentificationDocumentCapture_Sex"
    const val expiryDate = "OnBoardMe_IdentificationDocumentCapture_Expiry_Date"
    const val country = "OnBoardMe_IdentificationDocumentCapture_Country"
    const val nationality = "OnBoardMe_IdentificationDocumentCapture_Nationality"
    const val idType = "OnBoardMe_IdentificationDocumentCapture_IDType"
    const val faceCapture = "OnBoardMe_IdentificationDocumentCapture_FaceCapture"
    const val image = "OnBoardMe_IdentificationDocumentCapture_Image"
    const val isSkippedAfterNFails = "OnBoardMe_IdentificationDocumentCapture_IsSkippedAfterNFails"
    const val isFailedFront = "OnBoardMe_IdentificationDocumentCapture_IsFailedFront"
    const val isFailedBack = "OnBoardMe_IdentificationDocumentCapture_IsFailedBack"
    const val skippedStatus = "OnBoardMe_IdentificationDocumentCapture_SkippedStatus"
    const val capturedVideoFront = "OnBoardMe_IdentificationDocumentCapture_CapturedVideoFront"
    const val capturedVideoBack = "OnBoardMe_IdentificationDocumentCapture_CapturedVideoBack"
    const val livenessStatus = "OnBoardMe_IdentificationDocumentCapture_LivenessStatus"
    const val isFrontAuth = "OnBoardMe_IdentificationDocumentCapture_IsFrontAuth"
    const val isBackAuth = "OnBoardMe_IdentificationDocumentCapture_IsBackAuth"
    const val isExpired = "OnBoardMe_IdentificationDocumentCapture_IsExpired"
    const val isTampering = "OnBoardMe_IdentificationDocumentCapture_IsTampering"
    const val tamperHeatMap = "OnBoardMe_IdentificationDocumentCapture_TamperHeatMap"
    const val isBackTampering = "OnBoardMe_IdentificationDocumentCapture_IsBackTampering"
    const val backTamperHeatmap = "OnBoardMe_IdentificationDocumentCapture_BackTamperHeatmap"
    const val originalFrontImage = "OnBoardMe_IdentificationDocumentCapture_OriginalFrontImage"
    const val originalBackImage = "OnBoardMe_IdentificationDocumentCapture_OriginalBackImage"
    const val ghostImage = "OnBoardMe_IdentificationDocumentCapture_GhostImage"
    const val idMaritalStatus = "OnBoardMe_IdentificationDocumentCapture_ID_MaritalStatus"
    const val idDateOfIssuance = "OnBoardMe_IdentificationDocumentCapture_ID_DateOfIssuance"
    const val idCivilRegisterNumber =
        "OnBoardMe_IdentificationDocumentCapture_ID_CivilRegisterNumber"
    const val idPlaceOfResidence = "OnBoardMe_IdentificationDocumentCapture_ID_PlaceOfResidence"
    const val idProvince = "OnBoardMe_IdentificationDocumentCapture_ID_Province"
    const val idGovernorate = "OnBoardMe_IdentificationDocumentCapture_ID_Governorate"
    const val idMothersName = "OnBoardMe_IdentificationDocumentCapture_ID_MothersName"
    const val idFathersName = "OnBoardMe_IdentificationDocumentCapture_ID_FathersName"
    const val idPlaceOfBirth = "OnBoardMe_IdentificationDocumentCapture_ID_PlaceOfBirth"
    const val idBackImage = "OnBoardMe_IdentificationDocumentCapture_ID_BackImage"
    const val idBloodType = "OnBoardMe_IdentificationDocumentCapture_ID_BloodType"
    const val idDrivingCategory = "OnBoardMe_IdentificationDocumentCapture_ID_DrivingCategory"
    const val idIssuanceAuthority = "OnBoardMe_IdentificationDocumentCapture_ID_IssuanceAuthority"
    const val idArmyStatus = "OnBoardMe_IdentificationDocumentCapture_ID_ArmyStatus"
    const val idProfession = "OnBoardMe_IdentificationDocumentCapture_ID_Profession"
    const val idUniqueNumber = "OnBoardMe_IdentificationDocumentCapture_ID_UniqueNumber"
    const val idDocumentTypeNumber =
        "5OnBoardMe_IdentificationDocumentCapture_ID_DocumentTypeNumber"
    const val idFees = "OnBoardMe_IdentificationDocumentCapture_ID_Fees"
    const val idReference = "OnBoardMe_IdentificationDocumentCapture_ID"
    const val idRegion = "OnBoardMe_IdentificationDocumentCapture_ID_Region"
    const val idRegistrationLocation =
        "OnBoardMe_IdentificationDocumentCapture_ID_RegistrationLocation"
    const val idFaceColor = "OnBoardMe_IdentificationDocumentCapture_ID_FaceColor"
    const val idEyeColor = "OnBoardMe_IdentificationDocumentCapture_ID_EyeColor"
    const val idSpecialMarks = "OnBoardMe_IdentificationDocumentCapture_ID_SpecialMarks"
    const val idCountryOfStay = "OnBoardMe_IdentificationDocumentCapture_ID_CountryOfStay"
    const val idIdentityNumber = "OnBoardMe_IdentificationDocumentCapture_ID_IdentityNumber"
    const val idPresentAddress = "OnBoardMe_IdentificationDocumentCapture_ID_PresentAddress"
    const val idPermanentAddress = "OnBoardMe_IdentificationDocumentCapture_ID_PermanentAddress"
    const val idFamilyNumber = "OnBoardMe_IdentificationDocumentCapture_ID_FamilyNumber"
    const val identityNumberBack = "OnBoardMe_IdentificationDocumentCapture_ID_IdentityNumberBack"
}

class IdentificationDocumentCapture {
    var name: Any? = null
    var surname: Any? = null
    var documentType: Any? = null
    var birthDate: Any? = null
    var documentNumber: Any? = null
    var sex: Any? = null
    var expiryDate: Any? = null
    var country: Any? = null
    var nationality: Any? = null
    var idType: Any? = null
    var faceCapture: Any? = null
    var image: Any? = null
    var isSkippedAfterNFails: Any? = null
    var isFailedFront: Any? = null
    var isFailedBack: Any? = null
    var skippedStatus: Any? = null
    var capturedVideoFront: Any? = null
    var capturedVideoBack: Any? = null
    var livenessStatus: Any? = null
    var isFrontAuth: Any? = null
    var isBackAuth: Any? = null
    var isExpired: Any? = null
    var isTampering: Any? = null
    var tamperHeatMap: Any? = null
    var isBackTampering: Any? = null
    var backTamperHeatmap: Any? = null
    var originalFrontImage: Any? = null
    var originalBackImage: Any? = null
    var ghostImage: Any? = null
    var idMaritalStatus: Any? = null
    var idDateOfIssuance: Any? = null
    var idCivilRegisterNumber: Any? = null
    var idPlaceOfResidence: Any? = null
    var idProvince: Any? = null
    var idGovernorate: Any? = null
    var idMothersName: Any? = null
    var idFathersName: Any? = null
    var idPlaceOfBirth: Any? = null
    var idBackImage: Any? = null
    var idBloodType: Any? = null
    var idDrivingCategory: Any? = null
    var idIssuanceAuthority: Any? = null
    var idArmyStatus: Any? = null
    var idProfession: Any? = null
    var idUniqueNumber: Any? = null
    var idDocumentTypeNumber: Any? = null
    var idFees: Any? = null
    var idReference: Any? = null
    var idRegion: Any? = null
    var idRegistrationLocation: Any? = null
    var idFaceColor: Any? = null
    var idEyeColor: Any? = null
    var idSpecialMarks: Any? = null
    var idCountryOfStay: Any? = null
    var idIdentityNumber: Any? = null
    var idPresentAddress: Any? = null
    var idPermanentAddress: Any? = null
    var idFamilyNumber: Any? = null
    var identityNumberBack: Any? = null
}

fun fillIdentificationDocumentCapture(outputProperties: Map<String, Any>?): IdentificationDocumentCapture {
    val identificationDocumentCapture = IdentificationDocumentCapture()

    outputProperties?.forEach { (key, value) ->
        when {
            key.contains(IdentificationDocumentCaptureKeys.name) -> identificationDocumentCapture.name =
                value

            key.contains(IdentificationDocumentCaptureKeys.surname) -> identificationDocumentCapture.surname =
                value

            key.contains(IdentificationDocumentCaptureKeys.documentType) -> identificationDocumentCapture.documentType =
                value

            key.contains(IdentificationDocumentCaptureKeys.birthDate) -> identificationDocumentCapture.birthDate =
                value

            key.contains(IdentificationDocumentCaptureKeys.documentNumber) -> identificationDocumentCapture.documentNumber =
                value

            key.contains(IdentificationDocumentCaptureKeys.sex) -> identificationDocumentCapture.sex =
                value

            key.contains(IdentificationDocumentCaptureKeys.expiryDate) -> identificationDocumentCapture.expiryDate =
                value

            key.contains(IdentificationDocumentCaptureKeys.country) -> identificationDocumentCapture.country =
                value

            key.contains(IdentificationDocumentCaptureKeys.nationality) -> identificationDocumentCapture.nationality =
                value

            key.contains(IdentificationDocumentCaptureKeys.idType) -> identificationDocumentCapture.idType =
                value

            key.contains(IdentificationDocumentCaptureKeys.faceCapture) -> identificationDocumentCapture.faceCapture =
                value

            key.contains(IdentificationDocumentCaptureKeys.image) -> identificationDocumentCapture.image =
                value

            key.contains(IdentificationDocumentCaptureKeys.isSkippedAfterNFails) -> identificationDocumentCapture.isSkippedAfterNFails =
                value

            key.contains(IdentificationDocumentCaptureKeys.isFailedFront) -> identificationDocumentCapture.isFailedFront =
                value

            key.contains(IdentificationDocumentCaptureKeys.isFailedBack) -> identificationDocumentCapture.isFailedBack =
                value

            key.contains(IdentificationDocumentCaptureKeys.skippedStatus) -> identificationDocumentCapture.skippedStatus =
                value

            key.contains(IdentificationDocumentCaptureKeys.capturedVideoFront) -> identificationDocumentCapture.capturedVideoFront =
                value

            key.contains(IdentificationDocumentCaptureKeys.capturedVideoBack) -> identificationDocumentCapture.capturedVideoBack =
                value

            key.contains(IdentificationDocumentCaptureKeys.livenessStatus) -> identificationDocumentCapture.livenessStatus =
                value

            key.contains(IdentificationDocumentCaptureKeys.isFrontAuth) -> identificationDocumentCapture.isFrontAuth =
                value

            key.contains(IdentificationDocumentCaptureKeys.isBackAuth) -> identificationDocumentCapture.isBackAuth =
                value

            key.contains(IdentificationDocumentCaptureKeys.isExpired) -> identificationDocumentCapture.isExpired =
                value

            key.contains(IdentificationDocumentCaptureKeys.isTampering) -> identificationDocumentCapture.isTampering =
                value

            key.contains(IdentificationDocumentCaptureKeys.tamperHeatMap) -> identificationDocumentCapture.tamperHeatMap =
                value

            key.contains(IdentificationDocumentCaptureKeys.isBackTampering) -> identificationDocumentCapture.isBackTampering =
                value

            key.contains(IdentificationDocumentCaptureKeys.backTamperHeatmap) -> identificationDocumentCapture.backTamperHeatmap =
                value

            key.contains(IdentificationDocumentCaptureKeys.originalFrontImage) -> identificationDocumentCapture.originalFrontImage =
                value

            key.contains(IdentificationDocumentCaptureKeys.originalBackImage) -> identificationDocumentCapture.originalBackImage =
                value

            key.contains(IdentificationDocumentCaptureKeys.ghostImage) -> identificationDocumentCapture.ghostImage =
                value

            key.contains(IdentificationDocumentCaptureKeys.idMaritalStatus) -> identificationDocumentCapture.idMaritalStatus =
                value

            key.contains(IdentificationDocumentCaptureKeys.idDateOfIssuance) -> identificationDocumentCapture.idDateOfIssuance =
                value

            key.contains(IdentificationDocumentCaptureKeys.idCivilRegisterNumber) -> identificationDocumentCapture.idCivilRegisterNumber =
                value

            key.contains(IdentificationDocumentCaptureKeys.idPlaceOfResidence) -> identificationDocumentCapture.idPlaceOfResidence =
                value

            key.contains(IdentificationDocumentCaptureKeys.idProvince) -> identificationDocumentCapture.idProvince =
                value

            key.contains(IdentificationDocumentCaptureKeys.idGovernorate) -> identificationDocumentCapture.idGovernorate =
                value

            key.contains(IdentificationDocumentCaptureKeys.idMothersName) -> identificationDocumentCapture.idMothersName =
                value

            key.contains(IdentificationDocumentCaptureKeys.idFathersName) -> identificationDocumentCapture.idFathersName =
                value

            key.contains(IdentificationDocumentCaptureKeys.idPlaceOfBirth) -> identificationDocumentCapture.idPlaceOfBirth =
                value

            key.contains(IdentificationDocumentCaptureKeys.idBackImage) -> identificationDocumentCapture.idBackImage =
                value

            key.contains(IdentificationDocumentCaptureKeys.idBloodType) -> identificationDocumentCapture.idBloodType =
                value

            key.contains(IdentificationDocumentCaptureKeys.idDrivingCategory) -> identificationDocumentCapture.idDrivingCategory =
                value

            key.contains(IdentificationDocumentCaptureKeys.idIssuanceAuthority) -> identificationDocumentCapture.idIssuanceAuthority =
                value

            key.contains(IdentificationDocumentCaptureKeys.idArmyStatus) -> identificationDocumentCapture.idArmyStatus =
                value

            key.contains(IdentificationDocumentCaptureKeys.idProfession) -> identificationDocumentCapture.idProfession =
                value

            key.contains(IdentificationDocumentCaptureKeys.idUniqueNumber) -> identificationDocumentCapture.idUniqueNumber =
                value

            key.contains(IdentificationDocumentCaptureKeys.idDocumentTypeNumber) -> identificationDocumentCapture.idDocumentTypeNumber =
                value

            key.contains(IdentificationDocumentCaptureKeys.idFees) -> identificationDocumentCapture.idFees =
                value

            key.contains(IdentificationDocumentCaptureKeys.idReference) -> identificationDocumentCapture.idReference =
                value

            key.contains(IdentificationDocumentCaptureKeys.idRegion) -> identificationDocumentCapture.idRegion =
                value

            key.contains(IdentificationDocumentCaptureKeys.idRegistrationLocation) -> identificationDocumentCapture.idRegistrationLocation =
                value

            key.contains(IdentificationDocumentCaptureKeys.idFaceColor) -> identificationDocumentCapture.idFaceColor =
                value

            key.contains(IdentificationDocumentCaptureKeys.idEyeColor) -> identificationDocumentCapture.idEyeColor =
                value

            key.contains(IdentificationDocumentCaptureKeys.idSpecialMarks) -> identificationDocumentCapture.idSpecialMarks =
                value

            key.contains(IdentificationDocumentCaptureKeys.idCountryOfStay) -> identificationDocumentCapture.idCountryOfStay =
                value

            key.contains(IdentificationDocumentCaptureKeys.idIdentityNumber) -> identificationDocumentCapture.idIdentityNumber =
                value

            key.contains(IdentificationDocumentCaptureKeys.idPresentAddress) -> identificationDocumentCapture.idPresentAddress =
                value

            key.contains(IdentificationDocumentCaptureKeys.idPermanentAddress) -> identificationDocumentCapture.idPermanentAddress =
                value

            key.contains(IdentificationDocumentCaptureKeys.idFamilyNumber) -> identificationDocumentCapture.idFamilyNumber =
                value

            key.contains(IdentificationDocumentCaptureKeys.identityNumberBack) -> identificationDocumentCapture.identityNumberBack =
                value
        }
    }


    return identificationDocumentCapture
}

fun getLanguageTransformationEnum(key: String): Int {
    return when {
        key.contains(IdentificationDocumentCaptureKeys.name) ||
                key.contains(IdentificationDocumentCaptureKeys.surname) ||
                key.contains(IdentificationDocumentCaptureKeys.idType) ||

                key.contains(IdentificationDocumentCaptureKeys.idPlaceOfResidence) ||
                key.contains(IdentificationDocumentCaptureKeys.idProvince) ||
                key.contains(IdentificationDocumentCaptureKeys.idGovernorate) ||
                key.contains(IdentificationDocumentCaptureKeys.idMothersName) ||
                key.contains(IdentificationDocumentCaptureKeys.idFathersName) ||
                key.contains(IdentificationDocumentCaptureKeys.idPlaceOfBirth) ||
                key.contains(IdentificationDocumentCaptureKeys.idIssuanceAuthority) ||
                key.contains(IdentificationDocumentCaptureKeys.idArmyStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.idReference) ||
                key.contains(IdentificationDocumentCaptureKeys.idRegistrationLocation) ||
                key.contains(IdentificationDocumentCaptureKeys.idPresentAddress) ||
                key.contains(IdentificationDocumentCaptureKeys.idPermanentAddress) ->
            LanguageTransformationEnum.Transliteration

        key.contains(IdentificationDocumentCaptureKeys.idCountryOfStay) ||
                key.contains(IdentificationDocumentCaptureKeys.idRegion) ||
                key.contains(IdentificationDocumentCaptureKeys.idMaritalStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.documentType) ||
                key.contains(IdentificationDocumentCaptureKeys.country) ||
                key.contains(IdentificationDocumentCaptureKeys.nationality) ||
                key.contains(IdentificationDocumentCaptureKeys.sex) ||
                key.contains(IdentificationDocumentCaptureKeys.idDateOfIssuance) ||
                key.contains(IdentificationDocumentCaptureKeys.documentNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.birthDate) ||
                key.contains(IdentificationDocumentCaptureKeys.expiryDate) ||
                key.contains(IdentificationDocumentCaptureKeys.idFamilyNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.identityNumberBack) ||
                key.contains(IdentificationDocumentCaptureKeys.idIdentityNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.idFaceColor) ||
                key.contains(IdentificationDocumentCaptureKeys.idEyeColor) ||
                key.contains(IdentificationDocumentCaptureKeys.idSpecialMarks) ||
                key.contains(IdentificationDocumentCaptureKeys.idDocumentTypeNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.idFees) ||
                key.contains(IdentificationDocumentCaptureKeys.idUniqueNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.idProfession) ||
                key.contains(IdentificationDocumentCaptureKeys.idDrivingCategory) ||
                key.contains(IdentificationDocumentCaptureKeys.idBloodType) ||
                key.contains(IdentificationDocumentCaptureKeys.idCivilRegisterNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.faceCapture) ||
                key.contains(IdentificationDocumentCaptureKeys.image) ||
                key.contains(IdentificationDocumentCaptureKeys.capturedVideoFront) ||
                key.contains(IdentificationDocumentCaptureKeys.capturedVideoBack) ||
                key.contains(IdentificationDocumentCaptureKeys.livenessStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.isFrontAuth) ||
                key.contains(IdentificationDocumentCaptureKeys.isBackAuth) ||
                key.contains(IdentificationDocumentCaptureKeys.isExpired) ||
                key.contains(IdentificationDocumentCaptureKeys.isTampering) ||
                key.contains(IdentificationDocumentCaptureKeys.tamperHeatMap) ||
                key.contains(IdentificationDocumentCaptureKeys.isBackTampering) ||
                key.contains(IdentificationDocumentCaptureKeys.backTamperHeatmap) ||
                key.contains(IdentificationDocumentCaptureKeys.originalFrontImage) ||
                key.contains(IdentificationDocumentCaptureKeys.originalBackImage) ||
                key.contains(IdentificationDocumentCaptureKeys.ghostImage) ||
                key.contains(IdentificationDocumentCaptureKeys.isSkippedAfterNFails) ||
                key.contains(IdentificationDocumentCaptureKeys.isFailedFront) ||
                key.contains(IdentificationDocumentCaptureKeys.isFailedBack) ||
                key.contains(IdentificationDocumentCaptureKeys.skippedStatus) ->
            LanguageTransformationEnum.Translation

        else -> LanguageTransformationEnum.Transliteration
    }
}


fun getLDataType(key: String): String {
    return when {
        key.contains(IdentificationDocumentCaptureKeys.name) ||
                key.contains(IdentificationDocumentCaptureKeys.surname) ||
                key.contains(IdentificationDocumentCaptureKeys.documentType) ||
                key.contains(IdentificationDocumentCaptureKeys.country) ||
                key.contains(IdentificationDocumentCaptureKeys.nationality) ||
                key.contains(IdentificationDocumentCaptureKeys.idType) ||
                key.contains(IdentificationDocumentCaptureKeys.idMaritalStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.idPlaceOfResidence) ||
                key.contains(IdentificationDocumentCaptureKeys.idProvince) ||
                key.contains(IdentificationDocumentCaptureKeys.idGovernorate) ||
                key.contains(IdentificationDocumentCaptureKeys.idMothersName) ||
                key.contains(IdentificationDocumentCaptureKeys.idFathersName) ||
                key.contains(IdentificationDocumentCaptureKeys.idPlaceOfBirth) ||
                key.contains(IdentificationDocumentCaptureKeys.idDrivingCategory) ||
                key.contains(IdentificationDocumentCaptureKeys.idIssuanceAuthority) ||
                key.contains(IdentificationDocumentCaptureKeys.idArmyStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.idProfession) ||
                key.contains(IdentificationDocumentCaptureKeys.idFees) ||
                key.contains(IdentificationDocumentCaptureKeys.idReference) ||
                key.contains(IdentificationDocumentCaptureKeys.idRegion) ||
                key.contains(IdentificationDocumentCaptureKeys.idRegistrationLocation) ||
                key.contains(IdentificationDocumentCaptureKeys.idFaceColor) ||
                key.contains(IdentificationDocumentCaptureKeys.idEyeColor) ||
                key.contains(IdentificationDocumentCaptureKeys.idSpecialMarks) ||
                key.contains(IdentificationDocumentCaptureKeys.idCountryOfStay) ||
                key.contains(IdentificationDocumentCaptureKeys.idPresentAddress) ||
                key.contains(IdentificationDocumentCaptureKeys.idPermanentAddress) ||
                key.contains(IdentificationDocumentCaptureKeys.sex) ||
                key.contains(IdentificationDocumentCaptureKeys.faceCapture) ||
                key.contains(IdentificationDocumentCaptureKeys.image) ||
                key.contains(IdentificationDocumentCaptureKeys.isSkippedAfterNFails) ||
                key.contains(IdentificationDocumentCaptureKeys.isFailedFront) ||
                key.contains(IdentificationDocumentCaptureKeys.isFailedBack) ||
                key.contains(IdentificationDocumentCaptureKeys.skippedStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.capturedVideoFront) ||
                key.contains(IdentificationDocumentCaptureKeys.capturedVideoBack) ||
                key.contains(IdentificationDocumentCaptureKeys.livenessStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.isFrontAuth) ||
                key.contains(IdentificationDocumentCaptureKeys.isBackAuth) ||
                key.contains(IdentificationDocumentCaptureKeys.isExpired) ||
                key.contains(IdentificationDocumentCaptureKeys.isTampering) ||
                key.contains(IdentificationDocumentCaptureKeys.tamperHeatMap) ||
                key.contains(IdentificationDocumentCaptureKeys.isBackTampering) ||
                key.contains(IdentificationDocumentCaptureKeys.backTamperHeatmap) ||
                key.contains(IdentificationDocumentCaptureKeys.originalFrontImage) ||
                key.contains(IdentificationDocumentCaptureKeys.originalBackImage) ||
                key.contains(IdentificationDocumentCaptureKeys.ghostImage) ->
            DataType.Text

        key.contains(IdentificationDocumentCaptureKeys.birthDate) ||
                key.contains(IdentificationDocumentCaptureKeys.expiryDate) ||
                key.contains(IdentificationDocumentCaptureKeys.idDateOfIssuance) ->
            DataType.Date

        key.contains(IdentificationDocumentCaptureKeys.idIdentityNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.idUniqueNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.idFamilyNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.identityNumberBack) ||
                key.contains(IdentificationDocumentCaptureKeys.idDocumentTypeNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.idCivilRegisterNumber) ||
                key.contains(IdentificationDocumentCaptureKeys.documentNumber) ->
            DataType.Text // should be number

        else -> DataType.Text
    }
}

fun ignoredKeys(key: String): Boolean {
    return when {
        key.contains(IdentificationDocumentCaptureKeys.capturedVideoFront) ||
                key.contains(IdentificationDocumentCaptureKeys.capturedVideoBack) ||
                key.contains(IdentificationDocumentCaptureKeys.livenessStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.isFrontAuth) ||
                key.contains(IdentificationDocumentCaptureKeys.isBackAuth) ||
                key.contains(IdentificationDocumentCaptureKeys.isExpired) ||
                key.contains(IdentificationDocumentCaptureKeys.isTampering) ||
                key.contains(IdentificationDocumentCaptureKeys.tamperHeatMap) ||
                key.contains(IdentificationDocumentCaptureKeys.isBackTampering) ||
                key.contains(IdentificationDocumentCaptureKeys.backTamperHeatmap) ||
                key.contains(IdentificationDocumentCaptureKeys.originalFrontImage) ||
                key.contains(IdentificationDocumentCaptureKeys.originalBackImage) ||
                key.contains(IdentificationDocumentCaptureKeys.ghostImage) ||
                key.contains(IdentificationDocumentCaptureKeys.isSkippedAfterNFails) ||
                key.contains(IdentificationDocumentCaptureKeys.isFailedFront) ||
                key.contains(IdentificationDocumentCaptureKeys.isFailedBack) ||
                key.contains(IdentificationDocumentCaptureKeys.skippedStatus) ||
                key.contains(IdentificationDocumentCaptureKeys.image) ||
                key.contains(IdentificationDocumentCaptureKeys.faceCapture) -> true

        else -> false;
    }
}

fun getIgnoredProperties(properties: Map<String, Any>): Map<String, String> {
    val ignoredProperties = mutableMapOf<String, String>()
    properties.forEach { key, value ->
        if (ignoredKeys(key)) {
            ignoredProperties[key] = value.toString();
        }
    }

    return ignoredProperties;
}

fun preparePropertiesToTranslate(
    language: String,
    properties: Map<String, Any>
): TransformationModel {
    val languageTransformationModels = mutableListOf<LanguageTransformationModel>()
    properties.forEach { key, value ->
        if (!ignoredKeys(key)) {
            languageTransformationModels.add(
                LanguageTransformationModel(
                    languageTransformationEnum = getLanguageTransformationEnum(key),
                    key = key,
                    value = value.toString(),
                    language = language,
                    dataType = getLDataType(key),
                )
            )
        }
    }

    return TransformationModel(languageTransformationModels)
}

