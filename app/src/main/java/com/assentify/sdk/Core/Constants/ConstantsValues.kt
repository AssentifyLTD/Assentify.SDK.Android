package com.assentify.sdk.Core.Constants

import  com.assentify.sdk.RemoteClient.Models.ConfigModel

object ConstantsValues {
    const val ModelFileName = "best-fp16.tflite"
    const val AssetsPath = "file:///android_asset/"
    const val LabelFileName = "file:///android_asset/classes.txt"
    const val IsQuantized = false
    const val InputSize = 256
    const val FolderImagesName = "TempImages";
    const val FolderVideosName = "TempVideos";
    const val ImageName = "TEMP"
    const val VideoName = "VID"
    const val MINIMUM_CONFIDENCE_TF_OD_API = 0.3f

}

fun getVideoPath(configModel:ConfigModel,template:String,videoCounter:Int): String? {
    return  configModel.tenantIdentifier + "/" + configModel.blockIdentifier + "/" + configModel.instanceId + "/" + template  + "/" + "try_${videoCounter}"  + "/" +"${videoCounter}.mp4"
}

