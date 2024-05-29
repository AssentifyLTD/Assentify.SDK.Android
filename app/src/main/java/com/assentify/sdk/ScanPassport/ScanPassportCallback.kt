package   com.assentify.sdk.ScanPassport

import  com.assentify.sdk.Core.Constants.MotionType
import  com.assentify.sdk.Core.Constants.ZoomType
import  com.assentify.sdk.Models.BaseResponseDataModel


interface ScanPassportCallback {
    fun onError(dataModel: BaseResponseDataModel)

    fun onSend()

    fun onRetry(dataModel: BaseResponseDataModel)

    fun onClipPreparationComplete(dataModel: BaseResponseDataModel)

    fun onStatusUpdated(dataModel: BaseResponseDataModel)

    fun onUpdated(dataModel: BaseResponseDataModel)

    fun onLivenessUpdate(dataModel: BaseResponseDataModel)

    fun onComplete(dataModel: BaseResponseDataModel)

    fun onCardDetected(dataModel: BaseResponseDataModel)

    fun onMrzExtracted(dataModel: BaseResponseDataModel)

    fun onMrzDetected(dataModel: BaseResponseDataModel)

    fun onNoMrzDetected(dataModel: BaseResponseDataModel)

    fun onFaceDetected(dataModel: BaseResponseDataModel)

    fun onNoFaceDetected(dataModel: BaseResponseDataModel)

    fun onFaceExtracted(dataModel: BaseResponseDataModel)

    fun onQualityCheckAvailable(dataModel: BaseResponseDataModel)

    fun onDocumentCaptured(dataModel: BaseResponseDataModel)

    fun onDocumentCropped(dataModel: BaseResponseDataModel)

    fun onUploadFailed(dataModel: BaseResponseDataModel)

    fun onEnvironmentalConditionsChange(
        brightness: Double,
        motion: MotionType,
        zoom: ZoomType,

        )

}
