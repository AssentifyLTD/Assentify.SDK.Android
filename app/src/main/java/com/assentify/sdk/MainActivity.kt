package com.assentify.sdk

import AssentifySdk
import AssentifySdkCallback
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanPassport.ScanPassportCallback

class MainActivity : AppCompatActivity(), AssentifySdkCallback, IDCardCallback,ScanPassportCallback {
    private lateinit var assentifySdk: AssentifySdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val environmentalConditions = EnvironmentalConditions(
            500.0f,
            00.0f,
            50.0f,
            100.0f,
            "#61A03A",
            "#FFC400",

            );

        assentifySdk = AssentifySdk(
            "NvyTlImKg8lgToRGICsIV5AYGwxgObA2DdJVfCxrXKJFHPnepy0Ur38sPyoT0FJWHEkxh8LJ8uBtO4X4sg",
            "318e2ca7-fde8-4c47-bbcc-0c94b905630f",
            "CA7240AC8456E25F619B0A60CA0334FFFC7440AEF7514768419398ED044C6B9F",
            environmentalConditions,
            this,
            true,
            true,
            false,
            true,
            true,
            true,
        );


    }

    fun startAssentifySdk() {


        var scanPassport = assentifySdk.startScanPassport(
            this@MainActivity,
        );
        Thread.sleep(1000)
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scanPassport)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    /*    var data: List<KycDocumentDetails> = listOf(
            KycDocumentDetails(name = "", order = 0, templateProcessingKeyInformation = "75b683bb-eb81-4965-b3f0-c5e5054865e7"),
            KycDocumentDetails(name = "", order = 1, templateProcessingKeyInformation = "eae46fac-1763-4d31-9acc-c38d29fe56e4"),
        )

        var scanPassport = assentifySdk.startScanIDCard(
            this@MainActivity,
            data
        );
        Thread.sleep(1000)
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scanPassport)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()*/

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("Events Here onAssentifySdkInitError", message)
    }

    override fun onAssentifySdkInitSuccess(stepDefinitions: List<StepDefinitions>) {
        Log.e("Events Here onAssentifySdkInitSuccess", stepDefinitions.toString())
        assentifySdk.getTemplates();
        runOnUiThread {
            startAssentifySdk();
        }

    }

    override fun onHasTemplates(templates: List<TemplatesByCountry>) {
        Log.e("Events Here onHasTemplates", templates.toString())
    }

    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("EVENT HERE","onError")
    }

    override fun onSend() {
        Log.e("EVENT HERE","onSend")
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("EVENT HERE","onRetry")
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
        Log.e("EVENT HERE","onComplete")    }

    override fun onComplete(dataModel: BaseResponseDataModel,order:Int) {
    Log.e("EVENT HERE","onComplete")
    Log.e("EVENT HERE",order.toString())
    Log.e("EVENT HERE",dataModel.toString())
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

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
        Log.e("EVENT HERE","onWrongTemplate")
    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {

    }

    override fun onEnvironmentalConditionsChange(
        brightness: Double,
        motion: MotionType,
        zoom: ZoomType
    ) {

    }
}