package   com.assentify.sdk.FaceMatch

import com.assentify.sdk.Core.Constants.ActiveLiveEvents
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.DoneFlags
import com.assentify.sdk.Core.Constants.FaceEvents
import  com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import  com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanOther.OtherResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportManual

sealed class FaceMatchResult {
    data class Manual(val data: FaceMatchManual) : FaceMatchResult()
    data class Auto(val data: FaceMatch) : FaceMatchResult()
}


interface FaceMatchCallback {
    fun onError(dataModel: BaseResponseDataModel)

    fun onSend()

    fun onRetry(dataModel: BaseResponseDataModel)

    fun onComplete(dataModel: FaceResponseModel,doneFlag: DoneFlags)

    fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {}

    fun onStatusUpdated(dataModel: BaseResponseDataModel) {}

    fun onUpdated(dataModel: BaseResponseDataModel) {}

    fun onLivenessUpdate(dataModel: BaseResponseDataModel) {}

    fun onCardDetected(dataModel: BaseResponseDataModel) {}

    fun onMrzExtracted(dataModel: BaseResponseDataModel) {}

    fun onMrzDetected(dataModel: BaseResponseDataModel) {}

    fun onNoMrzDetected(dataModel: BaseResponseDataModel) {}

    fun onFaceDetected(dataModel: BaseResponseDataModel) {}

    fun onNoFaceDetected(dataModel: BaseResponseDataModel) {}

    fun onFaceExtracted(dataModel: BaseResponseDataModel) {}

    fun onQualityCheckAvailable(dataModel: BaseResponseDataModel) {}

    fun onDocumentCaptured(dataModel: BaseResponseDataModel) {}

    fun onDocumentCropped(dataModel: BaseResponseDataModel) {}

    fun onUploadFailed(dataModel: BaseResponseDataModel) {}

    fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        faceEvents: FaceEvents,
        zoomType: ZoomType,
        detectedFaces:Int
    ) {
    }

    fun onCurrentLiveMoveChange(activeLiveEvents: ActiveLiveEvents) {}
}
