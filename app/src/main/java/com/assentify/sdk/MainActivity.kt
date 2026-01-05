package com.assentify.sdk


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
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

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        showLoader()

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
            false,
            true,
            "#FFDE00",
            CountDownNumbersColor = "#FFDE00",
            activeLiveType = ActiveLiveType.Actions,
            activeLivenessCheckCount = 3,
            faceLivenessRetryCount = 2,
            minRam = 4
        );

        assentifySdk = AssentifySdk(
            "QwWzzKOYLkDzCLJ9lENlgvRQ1kmkKDv76KbJ9sPfr9Joxwj2DUuzC7htaZP89RqzgB9i9lHc4IpYOA7g",
            "2937c91f-c905-434b-d13d-08dcc04755ec",
            "6A0001F3C7B0F99B14F5BB17B0694BE751F189ADB62A3811591E27558FC30503",
            environmentalConditions,
            assentifySdkCallback = this,
            performActiveLivenessFace = false,
            context = this,
        );

    }

    private fun showLoader() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressBar.visibility = View.GONE
    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        hideLoader()
        AssentifySdkObject.setAssentifySdkObject(assentifySdk)
        runOnUiThread {
            val appLogoBytes = loadImageFromAssetsAsByteArray(this, "app_logo.png")
            val customProperties: MutableMap<String, String> = mutableMapOf()
            val flowEnvironmentalConditions = FlowEnvironmentalConditions(
                appLogoBytes,
                backgroundHexColor = "#0D2918",
                clicksHexColor = "#0BD884",
                textHexColor = "#F4F4F4",
                listItemsSelectedHexColor = "#D29E00",
                listItemsTextSelectedHexColor = "#F4F4F4",
                listItemsUnSelectedHexColor = "#0D1512",
                listItemsTextUnSelectedHexColor = "#F4F4F4",
                /*  "#000000",
                  "#FFFFFF",
                  "#FFC400",
                  "#FFDE00",
                  "#000000",
                  "#0C1F16",
                  "#FFFFFF",*/
                language = Language.English,
                enableNfc = false,
                enableQr = true,
                blockLoaderCustomProperties = customProperties

            );
            assentifySdk.startFlow(
                this@MainActivity,
                flowCallback = this,
                flowEnvironmentalConditions = flowEnvironmentalConditions
            )
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



