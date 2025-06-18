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
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanOther.OtherResponseModel
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel

class ScanOtherActivity : AppCompatActivity() , ScanOtherCallback {
    lateinit var infoText: TextView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        infoText =  findViewById(R.id.infoTextTest)
        infoText.visibility = View.GONE

        startAssentifySdk();
    }

    /**OTHER**/
    fun startAssentifySdk() {
        val assentifySdk: AssentifySdk = AssentifySdkObject.getAssentifySdkObject();
       val scanOther = assentifySdk.startScanOther(
            this@ScanOtherActivity,// This activity implemented from from ScanOtherCallback
           Language.Arabic // Optional the default is the doc language
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scanOther)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
    }

    override fun onSend() {
        infoText.visibility = View.VISIBLE
        infoText.text = "Sending ..."
    }

    override fun onError(dataModel: BaseResponseDataModel) {

    }

    override fun onCardDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {
        
    }

    override fun onComplete(dataModel: OtherResponseModel){
        infoText.visibility = View.GONE
        Log.e("onComplete",dataModel.otherExtractedModel.toString())
        dataModel.otherExtractedModel!!.extractedData?.let { ExtractedModel.setExtractedModel(it) };
        val intent = Intent(this, NavToFace::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("image",dataModel.otherExtractedModel!!.imageUrl)
        startActivity(intent)
    }

    override fun onDocumentCaptured(dataModel: BaseResponseDataModel) {
        
    }

    override fun onDocumentCropped(dataModel: BaseResponseDataModel) {
        
    }


    /// ... etc



    override fun onFaceDetected(dataModel: BaseResponseDataModel) {
        
    }

    override fun onFaceExtracted(dataModel: BaseResponseDataModel) {
        
    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        
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



    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {
        
    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {
        
    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {
        
    }

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        zoom: ZoomType
    ) {
    }
}