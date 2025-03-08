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
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FaceEvents
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

class MainActivity : AppCompatActivity(), AssentifySdkCallback, ScanPassportCallback {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var scanID: ScanIDCard;
    private lateinit var face: FaceMatch;
    private lateinit var textView: TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val environmentalConditions = EnvironmentalConditions(
                enableDetect = true, /** Default  true **/
                enableGuide = true, /** Default  true **/
                CustomColor = "#FFFFFF",
                HoldHandColor = "#FFC400",
            );


            assentifySdk = AssentifySdk(
                "z8zY4zgWnPqFDI2U05OeR6KMdDPAqqsBdVUU9hJAKZBEekIRNGgcJ8ItJJt1glsCY6IoDGkJScHEJmxRCNBEQ",
                "318e2ca7-fde8-4c47-bbcc-0c94b905630f",
                "77686B18F8337CF753183AE2DFE00C0F575879B450EAD98186F124666E27D038",
                environmentalConditions,
                this,
                true,/** Default value from flow configuration **/
                true, /** Default value from flow configuration **/
                true,/** Default value from flow configuration **/
                true, /** Default value from flow configuration **/
                true, /** Default value from flow configuration **/
                true, /** Default value from flow configuration **/
                true, /** Default value from flow configuration **/
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
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: ")
        startAssentifySdk();
    }

  /*  fun startAssentifySdk() {

        val image =
            "https://storagetestassentify.blob.core.windows.net/userfiles/b096e6ea-2a81-44cb-858e-08dbcbc01489/ca0162f9-8cfe-409f-91d8-9c2d42d53207/4f445a214f5a4b7fa74dc81243ccf590/b19c2053-efae-42e8-8696-177809043a9c/ReadPassport/image.jpeg"
        val base64Image =
            ImageToBase64Converter().execute(image).get()
        face = assentifySdk.startFaceMatch(
            this@MainActivity, // This activity implemented from from FaceMatchCallback
            base64Image, showCountDown = true // Target  Image
        );
        Thread.sleep(1000)
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, face)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()

    }*/

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

     fun startAssentifySdk() {
          var scanID = assentifySdk.startScanPassport(
              this@MainActivity,
              Language.Arabic// This activity implemented from from IDCardCallback
          );
          var fragmentManager = supportFragmentManager
          var transaction = fragmentManager.beginTransaction()
          transaction.replace(R.id.fragmentContainer, scanID)
          transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
          transaction.commit()

      }
    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onError: ")
    }

    override fun onSend() {
        Log.e("IDSCAN", "onSend: ")

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onRetry: ")
    }


        override fun onComplete(dataModel: PassportResponseModel) {
            Log.e("IDSCAN", "onComplete: " + dataModel.passportExtractedModel!!.extractedData )
            Log.e("IDSCAN", "onComplete: " + dataModel.passportExtractedModel!!.outputProperties )
            Log.e("IDSCAN", "onComplete: " + dataModel.passportExtractedModel!!.transformedProperties )
        }


/*    override fun onComplete(dataModel: FaceResponseModel) {
        Log.e("IDSCAN", "onComplete: " + dataModel.faceExtractedModel!!.extractedData)
    }*/


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

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        zoomType: ZoomType
    ) {
        runOnUiThread { textView.text = "Brightness Events : "+ brightnessEvents.toString() +  "\n\n" + "Zoom Events : " + zoomType.toString() }
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

class ImageToBase64Converter() : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg params: String): String? {
        val imageUrl = params[0]
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true

            // Add the X-Api-Key header
            connection.setRequestProperty(
                "X-Api-Key",
                "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w"
            )

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

