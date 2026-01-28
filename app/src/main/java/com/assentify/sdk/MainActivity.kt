package com.assentify.sdk


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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

data class StartConfig(
    val apiKey: String,
    val interactionHash: String,
    val tenantIdentifier: String,
    val language: String,
    val enableDetect: Boolean,
    val enableGuide: Boolean,
    val enableNfc: Boolean,
    val enableQr: Boolean
)

class MainActivity : AppCompatActivity(), AssentifySdkCallback, FlowCallBack {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private lateinit var progressBar: ProgressBar
    private lateinit  var config:StartConfig


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
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val etInteractionHash = findViewById<EditText>(R.id.etInteractionHash)
        val etTenantIdentifier = findViewById<EditText>(R.id.etTenantIdentifier)

        val spLanguage = findViewById<Spinner>(R.id.spLanguage)

        val swEnableDetect = findViewById<SwitchCompat>(R.id.swEnableDetect)
        val swEnableGuide = findViewById<SwitchCompat>(R.id.swEnableGuide)
        val swEnableNfc = findViewById<SwitchCompat>(R.id.swEnableNfc)
        val swEnableQr = findViewById<SwitchCompat>(R.id.swEnableQr)

        val btnStart = findViewById<Button>(R.id.btnStart)
         progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Languages list for dropdown
        val languages = listOf(
            Language.English,
            Language.Arabic,
            Language.Azerbaijani,
            Language.Belarusian,
            Language.Georgian,
            Language.Korean,
            Language.Latvian,
            Language.Lithuanian,
            Language.Punjabi,
            Language.Russian,
            Language.Sanskrit,
            Language.Sindhi,
            Language.Thai,
            Language.Turkish,
            Language.Ukrainian,
            Language.Urdu,
            Language.Uyghur,
            Language.NON
        )

        spLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )

        btnStart.setOnClickListener {


            btnStart.isEnabled = false

            val apiKey = etApiKey.text?.toString()?.trim().orEmpty()
            val interactionHash = etInteractionHash.text?.toString()?.trim().orEmpty()
            val tenantIdentifier = etTenantIdentifier.text?.toString()?.trim().orEmpty()
            val language = spLanguage.selectedItem?.toString() ?: Language.NON

             config = StartConfig(
                apiKey = "QwWzzKOYLkDzCLJ9lENlgvRQ1kmkKDv76KbJ9sPfr9Joxwj2DUuzC7htaZP89RqzgB9i9lHc4IpYOA7g",
                interactionHash = "E4BDD59C3B69A3F89AE8C756FCD67EBC72A45F405B256B3C3BDD643BE282B195",
                tenantIdentifier = "2937c91f-c905-434b-d13d-08dcc04755ec",
                language = language,
                enableDetect = swEnableDetect.isChecked,
                enableGuide = swEnableGuide.isChecked,
                enableNfc = swEnableNfc.isChecked,
                enableQr = swEnableQr.isChecked
            )

            // Basic validation example
            if (config.apiKey.isEmpty() || config.tenantIdentifier.isEmpty() || config.interactionHash.isEmpty()) {
                progressBar.visibility = View.GONE
                btnStart.isEnabled = true
                Toast.makeText(this, "API key , Tenant Identifier , Interaction Hash are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoader()
            val environmentalConditions = EnvironmentalConditions(
                config.enableDetect,
                config.enableGuide,
                "#FFDE00",
                CountDownNumbersColor = "#FFDE00",
                activeLiveType = ActiveLiveType.Actions,
                activeLivenessCheckCount = 3,
                faceLivenessRetryCount = 2,
                minRam = 4
            );
            assentifySdk = AssentifySdk(
                config.apiKey,
                config.tenantIdentifier,
                config.interactionHash,
                environmentalConditions,
                assentifySdkCallback = this,
                performActiveLivenessFace = false,
                context = this,
            );
        }
    }


    private fun showLoader() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressBar.visibility = View.GONE
    }

    override fun onAssentifySdkInitError(message: String) {
        hideLoader()
        Toast.makeText(this, "AssentifySdk Init Error", Toast.LENGTH_SHORT).show()
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        hideLoader()
        AssentifySdkObject.setAssentifySdkObject(assentifySdk)
        runOnUiThread {
            val customProperties: MutableMap<String, String> = mutableMapOf()

            val flowEnvironmentalConditions = FlowEnvironmentalConditions(
                backgroundType = BackgroundType.Color,
                language = config.language,
                enableNfc = config.enableNfc,
                enableQr = config.enableQr,
                blockLoaderCustomProperties = customProperties,
            );


            assentifySdk.startFlow(
                this@MainActivity,
                flowCallback = this,
                flowEnvironmentalConditions = flowEnvironmentalConditions
            )
        }

    }


    override fun onFlowCompleted(submitRequestModel: List<SubmitRequestModel>) {
        Toast.makeText(this, "Flow Completed", Toast.LENGTH_SHORT).show()

    }

}



