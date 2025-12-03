package com.assentify.sdk.ProcessingRHub

import  com.assentify.sdk.Models.BaseResponseDataModel


interface RemoteProcessingCallback {
    fun onMessageReceived(eventName: String, BaseResponseDataModel: BaseResponseDataModel)
    fun onUploadProgress(progress: Int){
        //
    }
}

