package com.assentify.sdk


import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassportCallback


class MainActivity : AppCompatActivity(), AssentifySdkCallback , ScanPassportCallback{
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
                "#FFFFFF",
                "#FFC400",
            );

        /* assentifySdk = AssentifySdk(
                context = this,
                environmentalConditions = environmentalConditions,
                assentifySdkCallback =  this
            );*/


         assentifySdk = AssentifySdk(
                apiKey = "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
                tenantIdentifier = "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
                interaction = "F0D1B6A7D863E9E4089B70EE5786D3D8DF90EE7BDD12BE315019E1F2FC0E875A",
                environmentalConditions = environmentalConditions,
                assentifySdkCallback = this,
                performLivenessDocument = false
            )


        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("AssentifySdk", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("AssentifySdk", "onAssentifySdkInitSuccess: ")
        startAssentifySdk();

    }

    fun startAssentifySdk() {
        var scanPassport = assentifySdk.startScanPassport(
            this@MainActivity,
            Language.Arabic
        );
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scanPassport)
        transaction.addToBackStack(null)
        transaction.commit()

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

    override fun onError(dataModel: BaseResponseDataModel) {
        Log.e("AssentifySdk", "onError: ")
    }

    override fun onSend() {
        Log.e("AssentifySdk", "onSend: ")
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {
        Log.e("AssentifySdk", "onRetry: ")
    }

    override fun onComplete(dataModel: PassportResponseModel) {
        Log.e("AssentifySdk", "onComplete: ${dataModel.passportExtractedModel!!.extractedData}")
    }

}


