package com.assentify.sdk


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDExtractedModel
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.security.auth.callback.PasswordCallback

class MainActivity : AppCompatActivity(), AssentifySdkCallback, FaceMatchCallback {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var scanID: ScanIDCard;
    private lateinit var face: FaceMatch;

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
                "#FFFFFF",
                "#FFC400",

                );


            assentifySdk = AssentifySdk(
                "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
                "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
                "F0D1B6A7D863E9E4089B70EE5786D3D8DF90EE7BDD12BE315019E1F2FC0E875A",
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

        val myButton: Button = findViewById(R.id.startAgain)

        myButton.setOnClickListener {
            face.startScanning();
        }

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: ")
        assentifySdk.getTemplates()?.forEach { item ->
            Log.e("MainActivity", "onAssentifySdkInitSuccess: ${item.name}")
            item.templates.forEach { item2 ->
                Log.e(
                    "MainActivity",
                    "onAssentifySdkInitSuccess: Templates ${item2.kycDocumentType}"
                )
                Log.e("MainActivity", "onAssentifySdkInitSuccess: Templates ${item2.id}")
            }
        }

      startAssentifySdk();
    }

    fun startAssentifySdk() {

        val image =
            "https://storagetestassentify.blob.core.windows.net/userfiles/b096e6ea-2a81-44cb-858e-08dbcbc01489/ca0162f9-8cfe-409f-91d8-9c2d42d53207/4f445a214f5a4b7fa74dc81243ccf590/b19c2053-efae-42e8-8696-177809043a9c/ReadPassport/image.jpeg"
        val base64Image =
            ImageToBase64Converter().execute(image).get()
        face = assentifySdk.startFaceMatch(
            this@MainActivity, // This activity implemented from from FaceMatchCallback
            base64Image // Target  Image
        );
        Thread.sleep(1000)
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, face)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }

    /*
        fun startAssentifySdk() {
            var data: List<KycDocumentDetails> = listOf(
                KycDocumentDetails(
                    name = "",
                    order = 0,
                    templateProcessingKeyInformation = "75b683bb-eb81-4965-b3f0-c5e5054865e7",
                    templateSpecimen = ""
                ),
                KycDocumentDetails(
                    name = "",
                    order = 1,
                    templateProcessingKeyInformation = "eae46fac-1763-4d31-9acc-c38d29fe56e4",
                    templateSpecimen = ""
                ),
            )
             scanID = assentifySdk.startScanIDCard(
                this@MainActivity,// This activity implemented from from IDCardCallback
                data,
                 language = Language.English// List<KycDocumentDetails> || Your selected template ||,
            );

            var fragmentManager = supportFragmentManager
            var transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, scanID)
            transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
            transaction.commit()

        }
    */

    /*   fun startAssentifySdk() {
          var scanID = assentifySdk.startScanPassport(
              this@MainActivity,
              Language.Arabic// This activity implemented from from IDCardCallback
          );
          var fragmentManager = supportFragmentManager
          var transaction = fragmentManager.beginTransaction()
          transaction.replace(R.id.fragmentContainer, scanID)
          transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
          transaction.commit()

      }*/
    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onError: ")
    }

    override fun onSend() {
        Log.e("IDSCAN", "onSend: ")

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onRetry: ")
    }


    /*
        override fun onComplete(dataModel: PassportResponseModel) {
            Log.e("IDSCAN extractedData", "onComplete: " + dataModel.passportExtractedModel!!.extractedData )
            Log.e("IDSCAN outputProperties", "onComplete: " + dataModel.passportExtractedModel!!.outputProperties )
            Log.e("IDSCAN transformedProperties", "onComplete: " + dataModel.passportExtractedModel!!.transformedProperties )
        }
    */


    override fun onComplete(dataModel: FaceResponseModel) {
        Log.e("IDSCAN", "onComplete: " + dataModel.faceExtractedModel!!.extractedData)
    }


    /*   override fun onComplete(dataModel: IDResponseModel, order: Int) {
         *//*  runOnUiThread {
            scanID.stopScanning();
          //  finish();
        }*//*
        Log.e("IDSCAN extractedData", "onComplete: " + dataModel.iDExtractedModel!!.extractedData )
        Log.e("IDSCAN outputProperties", "onComplete: " + dataModel.iDExtractedModel!!.outputProperties )
        Log.e("IDSCAN transformedProperties", "onComplete: " + dataModel.iDExtractedModel!!.transformedProperties )
    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onWrongTemplate" )
    }*/

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {

    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onLivenessUpdate: ")
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

    /*    override fun onEnvironmentalConditionsChange(brightness: Double, motion: MotionType,zoomType: ZoomType) {

            println("MainActivity onEnvironmentalConditionsChange" + zoomType)
        }*/

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

class ImageToBase64Converter() : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg params: String): String? {
        val imageUrl = params[0]
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true

            // Add the X-Api-Key header
            connection.setRequestProperty("X-Api-Key", "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w")

            connection.connect()
            val input: InputStream = connection.inputStream
            val bitmap: Bitmap = BitmapFactory.decodeStream(input)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(base64Image: String?) {
        // Handle the base64 image result here if needed
    }
}

