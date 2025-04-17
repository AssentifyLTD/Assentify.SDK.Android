/*package com.assentify.sdk


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

class MainActivity : AppCompatActivity() ,AssentifySdkCallback , FaceMatchCallback {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var scanID:ScanIDCard;
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
                true,
                true,
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
                false,
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

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: "  )
        Log.e("onAssentifySdkInitSuccess", assentifySdk.getTemplates().toString() )
        startAssentifySdk();
    }
    fun startAssentifySdk() {

        val  image = "https://storagetestassentify.blob.core.windows.net/userfiles/b096e6ea-2a81-44cb-858e-08dbcbc01489/ca0162f9-8cfe-409f-91d8-9c2d42d53207/4f445a214f5a4b7fa74dc81243ccf590/b19c2053-efae-42e8-8696-177809043a9c/ReadPassport/image.jpeg"
        val base64Image =
            ImageToBase64Converter().execute(image).get()
        var face = assentifySdk.startFaceMatch(

            this@MainActivity,
            // This activity implemented from from FaceMatchCallback
            base64Image ,
            true// Target  Image
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, face)
        transaction.addToBackStack(null)
        transaction.commit()

    }

    *//*    fun startAssentifySdk() {
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
                data, // List<KycDocumentDetails> || Your selected template ||,
                Language.English ,// Optional the default is the doc language
            );

            var fragmentManager = supportFragmentManager
            var transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, scanID)
            transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
            transaction.commit()

        }*//*

    //    fun startAssentifySdk() {
//       var scanID = assentifySdk.startScanPassport(
//           this@MainActivity,
//           Language.Arabic// This activity implemented from from IDCardCallback
//       );
//       var fragmentManager = supportFragmentManager
//       var transaction = fragmentManager.beginTransaction()
//       transaction.replace(R.id.fragmentContainer, scanID)
//       transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
//       transaction.commit()
//
//   }
    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onError: ", )
    }

    override fun onSend() {
        Log.e("IDSCAN", "onSend: ", )

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onRetry: ", )
    }

    override fun onComplete(dataModel: FaceResponseModel) {
        Log.e("IDSCAN transformedProperties", "onComplete: " + dataModel.faceExtractedModel!!.extractedData )
    }

    *//*    override fun onComplete(dataModel: PassportResponseModel) {
            Log.e("IDSCAN extractedData", "onComplete: " + dataModel.passportExtractedModel!!.extractedData )
            Log.e("IDSCAN outputProperties", "onComplete: " + dataModel.passportExtractedModel!!.outputProperties )
            Log.e("IDSCAN transformedProperties", "onComplete: " + dataModel.passportExtractedModel!!.transformedProperties )
        }*//*

    *//*
       override fun onComplete(dataModel: FaceResponseModel) {
           Log.e("IDSCAN", "onComplete: " + dataModel.faceExtractedModel!!.extractedData )
       }
    *//*

    *//*    override fun onComplete(dataModel: IDResponseModel, order: Int) {
            runOnUiThread {
                scanID.stopScanning();
              //  finish();
            }
            Log.e("IDSCAN extractedData", "onComplete: " + dataModel.iDExtractedModel!!.extractedData )
            Log.e("IDSCAN outputProperties", "onComplete: " + dataModel.iDExtractedModel!!.outputProperties )
            Log.e("IDSCAN transformedProperties", "onComplete: " + dataModel.iDExtractedModel!!.transformedProperties )
        }*//*

    *//*    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
            Log.e("IDSCAN", "onWrongTemplate" )
        }*//*

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {

    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {

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
        faceEvents: FaceEvents,
        zoomType: ZoomType
    ) {
        runOnUiThread { textView.text =  "Face Events : " + faceEvents.toString()}
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

class ImageToBase64Converter : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg params: String): String? {
        val imageUrl = params[0]
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
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
        // You can handle the result here if needed
        // For example, you can use the result in another function or class
    }
}*/




package com.assentify.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.ScanNFC.ScanNfc
import com.assentify.sdk.ScanNFC.ScanNfcCallback
import org.jmrtd.lds.icao.MRZInfo
import android.os.Process
import android.util.Base64
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.getRandomEvents
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), AssentifySdkCallback, ScanNfcCallback,
    ScanPassportCallback {
    private lateinit var assentifySdk: AssentifySdk
    private lateinit var scanNfc: ScanNfc
    private lateinit var info: TextView
    private lateinit var faceImageView: ImageView
    private lateinit var loadingLayout: ProgressBar
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private lateinit var scanPassport: ScanPassport
    private var passportResponseModel: PassportResponseModel? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val copyButton = findViewById<Button>(R.id.copy_button)

        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", info.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }


        info = findViewById(R.id.info)
        faceImageView = findViewById(R.id.faceImageView)
        loadingLayout = findViewById(R.id.loading_layout)
        loadingLayout.visibility = View.GONE


        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val environmentalConditions = EnvironmentalConditions(
                true,
                true,
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
                false,
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
        Log.e("AssentifyActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e(
            "IDSCAN",
            configModel.tenantIdentifier
        )
        Log.e(
            "IDSCAN",
            configModel.blockIdentifier
        )
        Log.e(
            "IDSCAN",
            configModel.instanceId
        )
        scanNfc = assentifySdk.startScanNfc(
            this, // This activity implemented from ScanNfcCallback // Optional the default is the doc language
           context = this// Context
        )


        if (scanNfc.isNfcSupported(activity = this)) {
            if (scanNfc.isNfcEnabled(activity = this)) {
                info.text = "NFC Supported on this device, And Enabled.";
            } else {
                info.text = "NFC Supported on this device, And not Enabled.";
                val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                startActivity(intent)
            }
        } else {
            info.text = "NFC Not supported on this device.";
        }
        startAssentifySdk();
    }



/** Passport **/



    fun startAssentifySdk() {
        scanPassport = assentifySdk.startScanPassport(
            this@MainActivity,
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scanPassport)
        transaction.addToBackStack(null)
        transaction.commit()

    }

    override fun onError(dataModel: BaseResponseDataModel) {
        info.text = "onError ..."
    }

    override fun onSend() {
        info.text = "Sending ... "
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        info.text = "onRetry ... "
    }

    override fun onComplete(dataModel: PassportResponseModel) {
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.outputProperties.toString()
        )
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.transformedProperties.toString()
        )
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.extractedData.toString()
        )
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.faces.toString()
        )
        val base64Image =
            ImageToBase64Converter().execute(dataModel.passportExtractedModel?.faces!!.first()).get()
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        val xBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        faceImageView.setImageBitmap(xBitmap)
        info.text = "Start NFC Plz ... "
        scanPassport.stopScanning();
        scanPassport?.let {
            supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
        passportResponseModel = dataModel;
    }



/** NFC **/


    override fun onResume() {
        super.onResume()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter != null) {
            val intent = Intent(applicationContext, this.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            val filter = arrayOf(arrayOf(ConstantsValues.NfcTechTag))
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        adapter?.disableForegroundDispatch(this)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (passportResponseModel != null) {
            scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel!!)
        }
    }


    override fun onStartNfcScan() {
        runOnUiThread { loadingLayout.visibility = View.VISIBLE }
    }

    override fun onCompleteNfcScan(dataModel: PassportResponseModel) {
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.outputProperties.toString()
        )
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.transformedProperties.toString()
        )
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.extractedData.toString()
        )
        Log.e(
            "IDSCAN",
            dataModel.passportExtractedModel?.faces.toString()
        )
        runOnUiThread {
            loadingLayout.visibility = View.GONE
            showInfo("onCompleteNfcScan", dataModel)
            val base64Image =
                ImageToBase64Converter().execute(dataModel.passportExtractedModel?.faces!!.first()).get()
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val xBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            faceImageView.setImageBitmap(xBitmap)
        }

    }

    override fun onErrorNfcScan(dataModel: PassportResponseModel, message: String) {
        runOnUiThread {
            loadingLayout.visibility = View.VISIBLE
            info.text = message
        }
    }



/** onRequestPermissionsResult **/

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


/** showInfo **/


    fun showInfo(eventName: String, dataModel: PassportResponseModel) {
        runOnUiThread {
            info.text = buildString {
                appendLine("${eventName} ::")

                appendLine("outputProperties:")
                dataModel.passportExtractedModel?.outputProperties?.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }

                appendLine("\nextractedData:")
                dataModel.passportExtractedModel?.extractedData?.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }

                appendLine("\ntransformedProperties:")
                dataModel.passportExtractedModel?.transformedProperties?.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }

                appendLine("\nidentificationDocumentCapture:")
                dataModel.passportExtractedModel?.identificationDocumentCapture?.let { capture ->
                    capture::class.java.declaredFields.forEach { field ->
                        field.isAccessible = true
                        appendLine("  ${field.name}: ${field.get(capture)}")
                    }
                }
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




