package com.assentify.sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.assentify.sdk.AssentifySdk
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.ExtractedModel
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDExtractedModel
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanIDCard.ScanIDCard

class ScanIDActivity : AppCompatActivity(), IDCardCallback {
    var idExtractedModel: IDExtractedModel? = null;
    private lateinit var scanID:ScanIDCard;
    lateinit var infoText: TextView;
    val extractedData: MutableMap<String, String> = mutableMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        infoText =  findViewById(R.id.infoTextTest)
        infoText.visibility = View.GONE
        startAssentifySdk();
    }

    /** ID **/
    fun startAssentifySdk() {
        var selected = KysModel.getSelected();
        var data: List<KycDocumentDetails> = selected.kycDocumentDetails
        val assentifySdk: AssentifySdk = AssentifySdkObject.getAssentifySdkObject();
         scanID = assentifySdk.startScanIDCard(
            this@ScanIDActivity,// This activity implemented from from IDCardCallback
            data, // List<KycDocumentDetails> || Your selected template ||,
            Language.English ,// Optional the default is the doc language
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scanID)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    override fun onCardDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {

    }



    var image = "";
    override fun onComplete(dataModel: IDResponseModel, order: Int) {
        var selected = KysModel.getSelected();
        var data: List<KycDocumentDetails> = selected.kycDocumentDetails
        infoText.text = "Done Front"
        Log.e("onComplete",dataModel.iDExtractedModel.toString())
        runOnUiThread {
            if(data.size == 1){
                image =  dataModel.iDExtractedModel!!.imageUrl!!;
                extractedData.putAll(convertMapToFilteredStringMap(dataModel.iDExtractedModel!!.extractedData!!))
                extractedData?.let { ExtractedModel.setExtractedModel(it) };
                scanID.stopScanning();
                val intent = Intent(this, NavToFace::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("image",image)
                startActivity(intent)
            }else{
                if (order == 0) {
                    image =  dataModel.iDExtractedModel!!.imageUrl!!;
                    extractedData.putAll(convertMapToFilteredStringMap(dataModel.iDExtractedModel!!.extractedData!!))
                }
                if (order == 1) {
                    extractedData.putAll(convertMapToFilteredStringMap(dataModel.iDExtractedModel!!.extractedData!!))
                    extractedData?.let { ExtractedModel.setExtractedModel(it) };
                    scanID.stopScanning();
                    val intent = Intent(this, NavToFace::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("image",image)
                    startActivity(intent)
                }
            }

        }
    }


    override fun onDocumentCaptured(dataModel: BaseResponseDataModel) {

    }

    override fun onDocumentCropped(dataModel: BaseResponseDataModel) {

    }



    override fun onError(dataModel: BaseResponseDataModel) {
        infoText.text = "onError"
    }

    override fun onFaceDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onFaceExtracted(dataModel: BaseResponseDataModel) {

    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        infoText.text = "onLivenessUpdate"
    }

    override fun onMrzDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onMrzExtracted(dataModel: BaseResponseDataModel) {

    }

    override fun onNoFaceDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onNoMrzDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onQualityCheckAvailable(dataModel: BaseResponseDataModel) {

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        infoText.text = "onRetry"
    }

    override fun onSend() {
        infoText.visibility = View.VISIBLE
        infoText.text = "Sending ..."
    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {

    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {

    }

    fun convertMapToFilteredStringMap(originalMap: Map<String, Any>): Map<String, String> {
        return originalMap.filterValues { it is String }.mapValues { it.value as String }
    }

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        zoom: ZoomType
    ) {
    }
}