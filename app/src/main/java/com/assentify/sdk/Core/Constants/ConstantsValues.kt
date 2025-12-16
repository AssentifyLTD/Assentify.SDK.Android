package com.assentify.sdk.Core.Constants

import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// const val BRIGHTNESS_HIGH_THRESHOLD: Float = 180.0f;
// const val BRIGHTNESS_LOW_THRESHOLD: Float = 50.0f;
object ConstantsValues {
    const val CardName = "Card"
    const val FaceName = "Face"
    const val DetectColor = "#00FF00"
    const val ModelFileName = "best-fp16.tflite"
    const val AssetsPath = "file:///android_asset/"
    const val LabelFileName = "file:///android_asset/classes.txt"
    const val IsQuantized = false
    const val InputSize = 256
    const val InputFaceModelsSize = 224
    const val ModelLiveModelFileName = "check-liveness.tflite";
    const val FaceCheckQualityThresholdPositiveUp = 10;
    const val FaceCheckQualityThresholdPositive = 15;
    const val FaceCheckQualityThresholdNegative = -15;
    const val PREDICTION_LOW_PERCENTAGE: Float = 50.0f;
    const val PREDICTION_HIGH_PERCENTAGE: Float = 100.0f;
    const val NfcTechTag  = "android.nfc.tech.IsoDep";
    const val AudioFaceSuccess  = "audio_face_success.mp3";
    const val AudioCardSuccess  = "audio_card_success.mp3";
    const val AudioWrong = "audio_wrong.mp3";
    const val ClarityProjectId = "spm0s4tjn6";

    const val ProvidedFaceImageKey = "OnBoardMe_IdentificationDocumentCapture_Image";
}

object EventsErrorMessages {
    const val OnErrorMessage = "Your internet connection seems unstable. Please check your connection and try again"

    const val OnWrongTemplateMessage = "Please double-check that you selected the correct ID type and presenting this ID type"
    const val OnRetryCardMessage = "We couldn’t read your card. Try again in better lighting and make sure the card is clear and visible"
    const val OnLivenessCardUpdateMessage = "Please use your original physical ID card, not a photo or copy"

    const val OnRetryFaceMessage = "We couldn't complete your request"
    const val OnLivenessFaceUpdateMessage = "Please make sure your face is well lit, look directly at the camera, and avoid using photos or videos"
}



object StepsNames {
    const val WrapUp = "WrapUp"
    const val BlockLoader = "BlockLoader"
    const val TermsConditions = "TermsConditions"
    const val AssistedDataEntry = "AssistedDataEntry"
    const val FaceImageAcquisition = "FaceImageAcquisition"
    const val IdentificationDocumentCapture = "IdentificationDocumentCapture"
    const val ContextAwareSigning = "ContextAwareSigning"
}

class WrapUpKeys{
    companion object {
        const val TimeEnded = "OnBoardMe_WrapUp_TimeEnded"
    }
}

class BlockLoaderKeys{
    companion object {
        const val DeviceName = "OnBoardMe_BlockLoader_DeviceName"
        const val FlowName = "OnBoardMe_BlockLoader_FlowName"
        const val TimeStarted = "OnBoardMe_BlockLoader_TimeStarted"
        const val Application = "OnBoardMe_BlockLoader_Application"
        const val UserAgent = "OnBoardMe_BlockLoader_UserAgent"
        const val InstanceHash = "OnBoardMe_BlockLoader_InstanceHash"
        const val InteractionID = "OnBoardMe_BlockLoader_Interaction"
    }
}

fun getVideoPath(configModel:ConfigModel,template:String,videoCounter:Int): String {
    return  configModel.tenantIdentifier + "/" + configModel.blockIdentifier + "/" + configModel.instanceId + "/" + template  + "/" + "try_${videoCounter}"  + "/" +"${videoCounter}.mp4"
}

fun getIDTag(configModel:ConfigModel,templateName:String): String {
    return  configModel.tenantIdentifier + "/" + configModel.instanceId + "," + templateName;
}

fun getCurrentDateTime(): String {
    val currentDate = Date()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    return dateFormat.format(currentDate)
}

