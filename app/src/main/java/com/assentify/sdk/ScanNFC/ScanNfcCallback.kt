package  com.assentify.sdk.ScanNFC

import android.graphics.Bitmap
import com.assentify.sdk.ScanPassport.PassportResponseModel
import org.jmrtd.lds.icao.MRZInfo


interface ScanNfcCallback {

    fun onStartNfcScan()

    fun onCompleteNfcScan(dataModel: PassportResponseModel)

    fun onErrorNfcScan(dataModel: PassportResponseModel,message: String)


}
