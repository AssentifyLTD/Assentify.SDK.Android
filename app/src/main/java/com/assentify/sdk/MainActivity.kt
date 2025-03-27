


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
import android.nfc.NfcAdapter
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
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback

class MainActivity : AppCompatActivity(), AssentifySdkCallback, ScanNfcCallback ,ScanPassportCallback {
    private lateinit var assentifySdk: AssentifySdk
    private lateinit var scanNfc: ScanNfc
    private lateinit var info: TextView
    private lateinit var faceImageView: ImageView
    private lateinit var loadingLayout: ProgressBar
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private lateinit var scanPassport: ScanPassport
    private var passportResponseModel : PassportResponseModel? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val copyButton = findViewById<Button>(R.id.copy_button)

        copyButton.setOnClickListener {
            // triggerOnNewIntent();
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

            scanNfc = assentifySdk.startScanNfc(
               this,
                languageCode = Language.Arabic,
                apiKey = "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w"
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
        startAssentifySdk();
    }

  /** Passport **/

    fun startAssentifySdk() {
        scanPassport = assentifySdk.startScanPassport(
           this@MainActivity,
           Language.Arabic//
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
        if(passportResponseModel!=null){
            scanNfc.onActivityNewIntent(intent = intent, dataModel = passportResponseModel!!)
        }
    }


    override fun onStartNfcScan() {
        runOnUiThread { loadingLayout.visibility = View.VISIBLE }
    }

    override fun onCompleteNfcScan(dataModel: PassportResponseModel, finalBitmap: Bitmap) {
       runOnUiThread {
           loadingLayout.visibility = View.GONE
           showInfo("onCompleteNfcScan",dataModel)
           faceImageView.setImageBitmap(finalBitmap)
       }

    }

    override fun onErrorNfcScan(dataModel: PassportResponseModel,message: String) {
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
    fun showInfo(eventName:String , dataModel: PassportResponseModel){
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



