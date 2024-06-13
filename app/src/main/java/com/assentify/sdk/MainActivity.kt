package com.assentify.sdk

import AssentifySdk
import AssentifySdkCallback
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.google.gson.Gson

class MainActivity : AppCompatActivity(), ScanPassportCallback {
    private lateinit var assentifySdk: AssentifySdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        assentifySdk = AssentifySdkObject.getAssentifySdkObject()
        startAssentifySdk();

    }


    @SuppressLint("SuspiciousIndentation")
    fun startAssentifySdk() {
        //  Thread.sleep(1000)
        var scan = assentifySdk.startScanPassport(
            this,
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scan)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("MainActivity", "onError: ")
    }

    override fun onSend() {
        Log.e("MainActivity", "onSend: ")

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("MainActivity", "onRetry: ")
    }

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {

    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {

    }

    override fun onComplete(dataModel: BaseResponseDataModel) {
        Log.e("MainActivity", "onComplete: " + dataModel)
    }

    override fun onCardDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onMrzExtracted(dataModel: BaseResponseDataModel) {

    }

    override fun onMrzDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onNoMrzDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onFaceDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onNoFaceDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onFaceExtracted(dataModel: BaseResponseDataModel) {

    }

    override fun onQualityCheckAvailable(dataModel: BaseResponseDataModel) {

    }

    override fun onDocumentCaptured(dataModel: BaseResponseDataModel) {

    }

    override fun onDocumentCropped(dataModel: BaseResponseDataModel) {

    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {

    }

    override fun onEnvironmentalConditionsChange(
        brightness: Double,
        motion: MotionType,
        zoomType: ZoomType
    ) {

        println("MainActivity onEnvironmentalConditionsChange" + zoomType)
    }


}