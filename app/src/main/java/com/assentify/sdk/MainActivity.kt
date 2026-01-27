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
import com.assentify.sdk.Core.Constants.BackgroundType
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
            "E4BDD59C3B69A3F89AE8C756FCD67EBC72A45F405B256B3C3BDD643BE282B195",
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
            val customProperties: MutableMap<String, String> = mutableMapOf()

            val flowEnvironmentalConditions = FlowEnvironmentalConditions(
                backgroundType = BackgroundType.Color,
                /** Mint Theme **/
                /*textColor = "#ffffff",
                 secondaryTextColor = "#ffffff",
                 backgroundCardColor = "#0D1512",
                 accentColor = "#D29E00",
                 backgroundColor = BackgroundStyle.Gradient(
                     colorsHex = listOf("#0D2918", "#0D1512"),
                     angleDegrees = 90f,
                     holdUntil = 0.6f
                 ),
                 clickColor = BackgroundStyle.Solid("#0BD884"),*/
                /** X Theme **/
                /*  textHexColor = TextColor.BLACK,
                    accentColor = "#833F89",
                    fieldHexColor = "#F2F2F2",
                    backgroundColor = BackgroundStyle.Solid("#FFFFFF"),
                     clickColor = BackgroundStyle.Gradient(
                         colorsHex = listOf("#833F89", "#C82B47"),
                         angleDegrees = 0f,
                         holdUntil = 0.6f
                     ),*/
                /** Mix **/
                /*  logoUrl = "https://dummyimage.com/300x300/000/fff.png&text=LOGO",
                  accentColor = "#833F89",
                  clickColor = BackgroundStyle.Gradient(
                      colorsHex = listOf("#833F89", "#C82B47"),
                      angleDegrees = 0f,
                      holdUntil = 0.6f
                  ),*/
                language = Language.English,
                enableNfc = false,
                enableQr = true,
                blockLoaderCustomProperties = customProperties,

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



