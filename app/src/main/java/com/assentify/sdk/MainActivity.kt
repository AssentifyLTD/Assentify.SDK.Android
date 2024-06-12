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

class MainActivity : AppCompatActivity() ,AssentifySdkCallback , ScanPassportCallback {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val environmentalConditions = EnvironmentalConditions(
                true,
                true,
                500.0f,
                00.0f,
                50.0f,
                100.0f,
                "#61A03A",
                "#FFC400",
                );

            assentifySdk = AssentifySdk(
                "3K3ObywhMNAjZa5Ce4Hg4QtJjX9PVaroCgNuMu1qidrjuXkT33sy83opg1BUcbxiAmfxn2QSj7jBZ3X19uVg",
                "021aeae5-10e8-4780-858c-08dbcbc01489",
                "8A7E9FE36FB149AA47F4835A986B7C0ADF282A6102D8C70FE8604B606A5F8E3E",
                environmentalConditions,
                this,
                true,
                true,
                true,
                true,
                true,
                true,
            );

        } else {
            ActivityCompat.requestPermissions(
                 this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message )
    }

    override fun onAssentifySdkInitSuccess(stepDefinitions: List<StepDefinitions>) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: "  )
        startAssentifySdk();
    }

    @SuppressLint("SuspiciousIndentation")
    fun startAssentifySdk() {
      //  Thread.sleep(1000)
       var scan = assentifySdk.startScanPassport(
            this,
        );
       var  fragmentManager = supportFragmentManager
       var  transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scan)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("MainActivity", "onError: ", )
    }

    override fun onSend() {
        Log.e("MainActivity", "onSend: ", )

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("MainActivity", "onRetry: ", )
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
        Log.e("MainActivity", "onComplete: " + dataModel )
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

    override fun onEnvironmentalConditionsChange(brightness: Double, motion: MotionType,zoomType: ZoomType) {

        println("MainActivity onEnvironmentalConditionsChange" + zoomType)
    }

    override fun onHasTemplates(templates: List<TemplatesByCountry>) {
        println("MainActivity onEnvironmentalConditionsChange")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                return
            }
        }
    }

}