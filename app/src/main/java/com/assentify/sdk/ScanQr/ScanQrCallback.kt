package  com.assentify.sdk.ScanQr

import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanIDCard.IDResponseModel


interface ScanQrCallback {

    fun onStartQrScan()

    fun onCompleteQrScan(dataModel: IDResponseModel)

    fun onErrorQrScan(message: String)


}
