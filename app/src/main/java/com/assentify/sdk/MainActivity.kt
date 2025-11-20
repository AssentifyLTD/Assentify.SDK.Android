package com.assentify.sdk


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.ActiveLiveType
import com.assentify.sdk.Core.Constants.DoneFlags
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.FaceMatch.FaceMatchManual
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.StepMap
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanIDCard.ScanIDCardManual
import com.assentify.sdk.ScanOther.ScanOtherManual
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.assentify.sdk.ScanPassport.ScanPassportManual
import com.assentify.sdk.ScanPassport.ScanPassportResult
import com.assentify.sdk.ScanQr.ScanQrManual
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), AssentifySdkCallback, ScanPassportCallback{
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var passportClick: LinearLayout
    private lateinit var idClick: LinearLayout
    private lateinit var otherClick: LinearLayout
    private lateinit var loadingText: LinearLayout
    private lateinit var scanPassportManual: ScanPassportManual
    private lateinit var scanQrManual: ScanQrManual
    private lateinit var scanIDCardManual: ScanIDCardManual
    private lateinit var scanIDCard: ScanIDCard
    private lateinit var scanOtherManual: ScanOtherManual
    private lateinit var faceMatchManual: FaceMatchManual
    private  var newOder: Int = 0;

    var dataKycDocumentDetails: List<KycDocumentDetails> = listOf(
        KycDocumentDetails(
            name = "",
            order = 0,
            templateProcessingKeyInformation = "75b683bb-eb81-4965-b3f0-c5e5054865e7",
            templateSpecimen = "",
            hasQrCode = false
        ),
        KycDocumentDetails(
            name = "",
            order = 1,
            templateProcessingKeyInformation = "eae46fac-1763-4d31-9acc-c38d29fe56e4",
            templateSpecimen = "",
            hasQrCode = false
        ),
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }


        val environmentalConditions = EnvironmentalConditions(
            true,
            true,
            "#FFFFFF",
            "#FFC400",
            activeLiveType = ActiveLiveType.NONE,
            activeLivenessCheckCount = 2,
            retryCount = 4,
            minRam = 2,

        );
        // F0D1B6A7D863E9E4089B70EE5786D3D8DF90EE7BDD12BE315019E1F2FC0E875A
        // 2726A3681C0CAB2177AEDA50A70A96FD29757A20493AD02577B7578FE8EDE3CA
        assentifySdk = AssentifySdk(
            "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
            "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
            "F0D1B6A7D863E9E4089B70EE5786D3D8DF90EE7BDD12BE315019E1F2FC0E875A",
            environmentalConditions,
            assentifySdkCallback = this,
            performActiveLivenessFace = true,
            context = applicationContext,


        );
        val takeImage = findViewById<Button>(R.id.takeImage)

        takeImage.setOnClickListener {
            scanQrManual.takePicture();
        }

       val nextPage  = findViewById<Button>(R.id.nextPage)
        nextPage.setOnClickListener {
            if (newOder < dataKycDocumentDetails.size) {
                scanIDCard.changeTemplateId(
                    dataKycDocumentDetails[newOder].templateProcessingKeyInformation
                )
            }
        }
    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: ")
        val stepDefinitionsTemp: MutableList<StepMap> = mutableListOf()
        val template = assentifySdk.getTemplates();
        template.forEach { item ->
            Log.e("IDSCAN", item.name)
            item.templates.forEach { it ->
                it.kycDocumentDetails.forEach { kyc ->
                    Log.e("IDSCAN", kyc.name)
                    Log.e("IDSCAN", kyc.templateProcessingKeyInformation)
                    Log.e("IDSCAN", kyc.hasQrCode.toString())
                }

            }

        }

        startAssentifySdk();
    }

  /*      fun startAssentifySdk() {
            val  image = "https://storagetestassentify.blob.core.windows.net/userfiles/b096e6ea-2a81-44cb-858e-08dbcbc01489/ca0162f9-8cfe-409f-91d8-9c2d42d53207/4f445a214f5a4b7fa74dc81243ccf590/b19c2053-efae-42e8-8696-177809043a9c/ReadPassport/image.jpeg";            val base64Image =
                ImageToBase64Converter().execute(image).get()

            when (val result = assentifySdk.startFaceMatch(
                this@MainActivity,
                base64Image,
                true,
            )) {
                is FaceMatchResult.Manual -> {
                    Log.e("IDSCAN", "ScanOtherResult")
                    faceMatchManual = result.data
                    var fragmentManager = supportFragmentManager
                    var transaction = fragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, result.data)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
                is FaceMatchResult.Auto -> {
                    Log.e("IDSCAN", "ScanOtherResult")
                    var fragmentManager = supportFragmentManager
                    var transaction = fragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, result.data)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
            }


        }*/
  /*  fun startAssentifySdk() {



         when (val result = assentifySdk.startScanIDCard(
             this@MainActivity,// This activity implemented from from IDCardCallback
             dataKycDocumentDetails, // List<KycDocumentDetails> || Your selected template ||,
             Language.English // Optional the default is the doc language
         )) {
             is ScanIDCardResult.Manual -> {
                 Log.e("IDSCAN", "ScanPassportManual")
                 scanIDCardManual = result.data
                 var fragmentManager = supportFragmentManager
                 var transaction = fragmentManager.beginTransaction()
                 transaction.replace(R.id.fragmentContainer, result.data)
                 transaction.addToBackStack(null)
                 transaction.commit()
                 scanIDCardManual.changeTemplateId(
                     dataKycDocumentDetails[0].templateProcessingKeyInformation
                 )
             }
             is ScanIDCardResult.Auto -> {
                 scanIDCard = result.data
                 Log.e("IDSCAN", "ScanPassport")
                 var fragmentManager = supportFragmentManager
                 var transaction = fragmentManager.beginTransaction()
                 transaction.replace(R.id.fragmentContainer, result.data)
                 transaction.addToBackStack(null)
                 transaction.commit()
             }
         }

    }*/

   /* fun startAssentifySdk() {

      when (val result = assentifySdk.startScanOther(
          this@MainActivity,
          Language.Arabic,
      )) {
          is ScanOtherResult.Manual -> {
              Log.e("IDSCAN", "ScanOtherResult")
              scanOtherManual = result.data
              var fragmentManager = supportFragmentManager
              var transaction = fragmentManager.beginTransaction()
              transaction.replace(R.id.fragmentContainer, result.data)
              transaction.addToBackStack(null)
              transaction.commit()
          }
          is ScanOtherResult.Auto -> {
              Log.e("IDSCAN", "ScanOther")
              var fragmentManager = supportFragmentManager
              var transaction = fragmentManager.beginTransaction()
              transaction.replace(R.id.fragmentContainer, result.data)
              transaction.addToBackStack(null)
              transaction.commit()
          }
      }



  }*/

    fun startAssentifySdk() {

        when (val result = assentifySdk.startScanPassport(
            this@MainActivity,
            Language.Arabic,
        )) {
            is ScanPassportResult.Manual -> {
                Log.e("IDSCAN", "ScanPassportManual")
                scanPassportManual = result.data
                var fragmentManager = supportFragmentManager
                var transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, result.data)
                transaction.addToBackStack(null)
                transaction.commit()
            }
            is ScanPassportResult.Auto -> {
                Log.e("IDSCAN", "ScanPassport")
                var fragmentManager = supportFragmentManager
                var transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, result.data)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }



    }

   /*   fun startAssentifySdk() {

          var data: List<KycDocumentDetails> = listOf(
              KycDocumentDetails(
                  name = "",
                  order = 0,
                  templateProcessingKeyInformation = "a1ec8a0d-067c-4ce1-8420-820d7789cf83",
                  templateSpecimen = "",
                  hasQrCode = false
              ),
          )
          when (val result =  assentifySdk.startScanQr(
              this@MainActivity,
              data ,
              language = Language.English
          )) {
              is ScanQrResult.Manual -> {
                  Log.e("IDSCAN", "ScanPassportManual")
                  scanQrManual = result.data
                  var fragmentManager = supportFragmentManager
                  var transaction = fragmentManager.beginTransaction()
                  transaction.replace(R.id.fragmentContainer, result.data)
                  transaction.addToBackStack(null)
                  transaction.commit()
              }
              is ScanQrResult.Auto -> {
                  Log.e("IDSCAN", "ScanPassport")
                  var fragmentManager = supportFragmentManager
                  var transaction = fragmentManager.beginTransaction()
                  transaction.replace(R.id.fragmentContainer, result.data)
                  transaction.addToBackStack(null)
                  transaction.commit()
              }
          }


      }*/


    /*override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onLivenessUpdate")
    }
*/
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

/*    override fun onStartQrScan() {
        Log.e("IDSCAN", "onStartQrScan")
    }

    override fun onCompleteQrScan(dataModel: IDResponseModel) {
        Log.e("IDSCAN", dataModel.iDExtractedModel?.outputProperties.toString())
        Log.e("IDSCAN", dataModel.iDExtractedModel?.extractedData.toString())
        Log.e("IDSCAN", dataModel.iDExtractedModel?.imageUrl.toString())
    }

    override fun onErrorQrScan(message: String) {
        Log.e("IDSCAN", "onErrorQrScan")
    }*/

    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onError")
    }

    override fun onSend() {
        Log.e("IDSCAN", "onSend")

    }

   override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onRetry")
    }

   /* override fun onComplete(dataModel: OtherResponseModel) {
        Log.e("IDSCAN", dataModel.otherExtractedModel?.outputProperties.toString())
        Log.e("IDSCAN", dataModel.otherExtractedModel?.extractedData.toString())
    }*/

 /*     override fun onComplete(dataModel: FaceResponseModel,doneFlag:DoneFlags) {
          Log.e("IDSCAN", dataModel.faceExtractedModel?.outputProperties.toString())
          Log.e("IDSCAN", dataModel.faceExtractedModel?.extractedData.toString())
          Log.e("IDSCAN", dataModel.faceExtractedModel?.baseImageFace.toString())
          Log.e("IDSCAN", doneFlag.toString())
          //f.closeCamera();
      }*/

   /* override fun onSubmitError(message: String) {
        Log.e("IDSCAN", "onSubmitError")
    }

    override fun onSubmitSuccess(message: String) {
        Log.e("IDSCAN","onSubmitSuccess")
    }*/

/*      override fun onComplete(dataModel: IDResponseModel, order: Int,doneFlag: DoneFlags) {
          Log.e("IDSCAN", dataModel.iDExtractedModel?.outputProperties.toString())
          Log.e("IDSCAN", dataModel.iDExtractedModel?.extractedData.toString())
          Log.e("IDSCAN", dataModel.iDExtractedModel?.imageUrl.toString())
          Log.e("IDSCAN", doneFlag.toString())
          Log.e("IDSCAN", order.toString())
           newOder = order+1;

      }
      override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
          Log.e("IDSCAN", "onWrongTemplate")
      }*/

    override fun onComplete(dataModel: PassportResponseModel, doneFlag:DoneFlags) {
        Log.e("IDSCAN", dataModel.passportExtractedModel?.outputProperties.toString())
        Log.e("IDSCAN", dataModel.passportExtractedModel?.extractedData.toString())
        Log.e("IDSCAN", dataModel.passportExtractedModel?.imageUrl.toString())
        Log.e("IDSCAN", doneFlag.toString())

    }
    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
        Log.e("IDSCAN", "onWrongTemplate")
    }


   /* override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        faceEvents: FaceEvents,
        zoomType: ZoomType,
        detectedFaces:Int
    ) {
        val faceTextView = findViewById<TextView>(R.id.faceEventText)

        runOnUiThread {
            faceTextView.text = detectedFaces.toString()
        }
    }


    override fun onCurrentLiveMoveChange(activeLiveEvents: ActiveLiveEvents) {
       *//* val faceTextView = findViewById<TextView>(R.id.faceEventText)

        runOnUiThread {
            faceTextView.text = activeLiveEvents.name
        }*//*
    }*/
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
}

