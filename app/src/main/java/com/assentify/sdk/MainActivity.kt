package com.assentify.sdk


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.ActiveLiveType
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel

class MainActivity : AppCompatActivity(), AssentifySdkCallback, FlowCallBack {
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
            "#173A2A",
            CountDownNumbersColor = "#173A2A",
            activeLiveType = ActiveLiveType.NONE,
            activeLivenessCheckCount = 2,
            faceLivenessRetryCount = 2,
            minRam = 1

        );


         assentifySdk = AssentifySdk(
                "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
                "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
                "6A0001F3C7B0F99B14F5BB17B0694BE751F189ADB62A3811591E27558FC30503",
                 environmentalConditions,
                 assentifySdkCallback = this,
                 performActiveLivenessFace = true,
                 context = this,
                );

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        AssentifySdkObject.setAssentifySdkObject(assentifySdk)
        Log.e("MainActivity", "onAssentifySdkInitSuccess: ")

        runOnUiThread {
            val appLogoBytes = loadImageFromAssetsAsByteArray(this, "app_logo.png")
            val customProperties: MutableMap<String, String> = mutableMapOf()
            val flowEnvironmentalConditions = FlowEnvironmentalConditions(
                appLogoBytes,
                "#173A2A",
                "#6AD379",
                "#E1BE4D",
                "#0C1F16",
                language = Language.English,
                enableNfc = false,
                enableQr = true,
                blockLoaderCustomProperties = customProperties

            );
            assentifySdk.startFlow(this@MainActivity, flowCallback = this, flowEnvironmentalConditions = flowEnvironmentalConditions)
        }

    }

    fun loadImageFromAssetsAsByteArray(context: Context, assetPath: String): ByteArray {
        return context.assets.open(assetPath).use { input ->
            input.readBytes()
        }
    }

    override fun onFlowCompleted(submitRequestModel: List<SubmitRequestModel>) {
        Log.e("IDSCAN", submitRequestModel.toString())
    }

}



