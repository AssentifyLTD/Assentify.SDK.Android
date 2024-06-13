package com.assentify.sdk

import AssentifySdk
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.Models.BaseResponseDataModel

class FaceMatchPage : AppCompatActivity() ,  FaceMatchCallback{
    private lateinit var image: String;
    private lateinit var base64Image:String;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_match_page)
        image = ""
        base64Image = "base64ImageImage"
        Thread.sleep(1000)
        startAssentifySdk();
    }

    fun startAssentifySdk() {

        val assentifySdk: AssentifySdk = AssentifySdkObject.getAssentifySdkObject();
        var face = assentifySdk.startFaceMatch(
            this@FaceMatchPage,
            base64Image
        );
        Thread.sleep(1000)
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, face)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    override fun onCardDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {

    }

    override fun onComplete(dataModel: BaseResponseDataModel) {
        Log.e("Events Here Face Page",dataModel.toString())

    }

    override fun onDocumentCaptured(dataModel: BaseResponseDataModel) {

    }

    override fun onDocumentCropped(dataModel: BaseResponseDataModel) {

    }

    override fun onEnvironmentalConditionsChange(brightness: Double, motion: MotionType) {
    }


    override fun onError(dataModel: BaseResponseDataModel) {

    }

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

    override fun onRetry(dataModel: BaseResponseDataModel) {

    }

    override fun onSend() {
        Log.e("Events Here Face Page","onSend")
    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {

    }

}