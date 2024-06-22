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
import android.widget.LinearLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanOther.OtherResponseModel
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.google.gson.Gson

class MainActivity : AppCompatActivity(), FaceMatchCallback {
    private lateinit var assentifySdk: AssentifySdk
    private lateinit var test_container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test_container = findViewById(R.id.test_container)
        assentifySdk = AssentifySdkObject.getAssentifySdkObject()
        startAssentifySdk();

    }


    @SuppressLint("SuspiciousIndentation")
    fun startAssentifySdk() {
        //  Thread.sleep(1000)
      val  base64Image =
            ImageToBase64Converter().execute("https://storagetestassentify.blob.core.windows.net/userfiles/021aeae5-10e8-4780-858c-08dbcbc01489/1b8acfe2-c0a2-40c8-b5a4-ff03732482b6/adb40e823bea4fe992de49beca4b39fa/fb44ed05-1577-4ecb-81be-b180d70cf746/faceFrame.jpg").get()
        var scan = assentifySdk.startFaceMatch(
            this,base64Image
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scan)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("V17", "onError: " + dataModel)
    }

    override fun onSend() {
        Log.e("V17", "onSend: ")

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("V17", "onRetry: " + dataModel)
    }


    override fun onComplete(dataModel: FaceResponseModel) {
        Log.e("V17", "onComplete: " + dataModel.destinationEndpoint)
        Log.e("V17", "onComplete: " + dataModel.error)
        Log.e("V17", "onComplete: " + dataModel.success)
        dataModel.faceExtractedModel!!.outputProperties!!.forEach { t, u ->
            Log.e("V17", "onComplete: " +  t + "outputProperties->>" + u )
        }
        dataModel.faceExtractedModel!!.extractedData!!.forEach { t, u ->
            Log.e("V17", "onComplete: " +  t + "extractedData->>" + u )
        }

        Log.e("V17", "onComplete: baseImageFace" + dataModel.faceExtractedModel!!.baseImageFace)
        Log.e("V17", "onComplete: secondImageFace" + dataModel.faceExtractedModel!!.secondImageFace)
        Log.e("V17", "onComplete: percentageMatch" + dataModel.faceExtractedModel!!.percentageMatch)
        Log.e("V17", "onComplete: isLive" + dataModel.faceExtractedModel!!.isLive)
        Log.e("V17", "onComplete: " + dataModel.faceExtractedModel!!.identificationDocumentCapture!!.surname)
        Log.e("V17", "onComplete: " + dataModel.faceExtractedModel!!.identificationDocumentCapture!!.sex)
        Log.e("V17", "onComplete: " + dataModel.faceExtractedModel!!.identificationDocumentCapture!!.nationality)
        Log.e("V17", "onComplete: " + dataModel.faceExtractedModel!!.identificationDocumentCapture!!.name)
        test_container.visibility = View.VISIBLE
        /*  runOnUiThread {
              val intent = Intent(this, FaceMatchPage::class.java)
              intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
              startActivity(intent)
              //finish()
          }*/
    }



}