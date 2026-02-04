package  com.assentify.sdk.ScanQr

import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanIDCard.IDResponseModel

sealed class ScanQrResult {
    data class Manual(val data: ScanQrManual) : ScanQrResult()
    data class Auto(val data: ScanQr) : ScanQrResult()
}


interface ScanQrCallback {

    fun onStartQrScan()

    fun onCompleteQrScan(dataModel: IDResponseModel)

    fun onErrorQrScan(message: String,dataModel: BaseResponseDataModel)
    fun onUploadingProgress(progress:Int){}

}
