package com.assentify.sdk.Core.Constants

import  com.assentify.sdk.RemoteClient.Models.ConfigModel

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
}


fun getVideoPath(configModel:ConfigModel,template:String,videoCounter:Int): String? {
    return  configModel.tenantIdentifier + "/" + configModel.blockIdentifier + "/" + configModel.instanceId + "/" + template  + "/" + "try_${videoCounter}"  + "/" +"${videoCounter}.mp4"
}

fun getIDTag(configModel:ConfigModel,templateName:String): String? {
    return  configModel.tenantIdentifier + "/" + configModel.instanceId + "," + templateName;
}

