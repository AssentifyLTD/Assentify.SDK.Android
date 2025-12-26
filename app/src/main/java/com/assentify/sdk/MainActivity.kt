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
            "36CBxFWo93rP0U6oBnJm85n7urUuBvF9PRAFXkcVQTnYHABigvnHweRtn93XgnAS3zfMSPI8oeLQL9PLbVA",
            "c6fe8eea-7f40-401a-f62a-08dde079f57f",
            "6083BE1F5A878AC0281F90975EAAF93148515D2F8C64F5D175019FA96926F654",
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
                "#000000",
                "#FFFFFF",
                "#FFC400",
                "#FFDE00",
                "#0C1F16",
                language = Language.English,
                enableNfc = true,
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



