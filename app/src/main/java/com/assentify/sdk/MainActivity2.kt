package com.assentify.sdk


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.assentify.sdk.Core.Constants.ActiveLiveType
import com.assentify.sdk.Core.Constants.BackgroundType
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.Flow.Models.FlowCompletedModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel


class MainActivity2 : AppCompatActivity(), AssentifySdkCallback, FlowCallBack {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private lateinit var progressBar: ProgressBar
    private lateinit var config: StartConfig


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)


        /** UI **/
        val etApiKey = findViewById<EditText>(R.id.etApiKey)


        etApiKey.setText( "QwWzzKOYLkDzCLJ9lENlgvRQ1kmkKDv76KbJ9sPfr9Joxwj2DUuzC7htaZP89RqzgB9i9lHc4IpYOA7g")

        val spLanguage = findViewById<Spinner>(R.id.spLanguage)

        val swEnableDetect = findViewById<SwitchCompat>(R.id.swEnableDetect)
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
            val apiKey = etApiKey.text?.toString()?.trim().orEmpty()
            val language = spLanguage.selectedItem?.toString() ?: Language.NON

            config = StartConfig(
                apiKey = apiKey,
                language = language,
                enableDetect = swEnableDetect.isChecked,
                enableNfc = swEnableNfc.isChecked,
                enableQr = swEnableQr.isChecked
            )

            // Validation
            if (config.apiKey.isEmpty() ) {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "API key , Tenant Identifier , Interaction Hash are required.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else {
                /** INIT SDK **/
                val environmentalConditions = EnvironmentalConditions(
                    config.enableDetect,
                    "#ffc400",
                    CountDownNumbersColor = "#ffc400",
                    activeLiveType = ActiveLiveType.Actions,
                    activeLivenessCheckCount = 3,
                    faceLivenessRetryCount = 2,
                    minRam = 1
                );
                assentifySdk = AssentifySdk(
                    apiKey = config.apiKey,
                    configFileName =   "configFile2",
                    environmentalConditions,
                    assentifySdkCallback = this,
                    performActiveLivenessFace = false,
                    context = this,
                );
                showLoader()

            }
            /** END **/
        }
    }


    /** INIT SDK ERROR **/
    override fun onAssentifySdkInitError(message: String) {
        hideLoader()
        Toast.makeText(this, "AssentifySdk Init Error", Toast.LENGTH_SHORT).show()
    }

    /** INIT SDK SUCCESS **/
    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        hideLoader()
        runOnUiThread {
            /** INIT FLOW **/
            val customProperties: MutableMap<String, String> = mutableMapOf()

            val flowEnvironmentalConditions = FlowEnvironmentalConditions(
                // logoUrl = "https://image2url.com/r2/default/images/1769694393603-0afa5733-d9a5-4b0d-9134-868d3a750069.png",
                // svgBackgroundImageUrl = "https://api.dicebear.com/7.x/shapes/svg?seed=patternA",
                backgroundType = BackgroundType.Color,
                /*   textColor = "#000000",
                   accentColor = "#ffc400",
                   secondaryTextColor = "#ffffff",
                   backgroundCardColor = "#f3f4f6",
                   backgroundColor = BackgroundStyle.Solid("#ffffff"),
                   clickColor = BackgroundStyle.Solid("#ffc400"),
                   blockLoaderCustomProperties = customProperties,*/
                language = config.language,
                enableNfc = config.enableNfc,
                enableQr = config.enableQr,
            );

            assentifySdk.startFlow(
                this@MainActivity2,
                flowCallback = this,
                flowEnvironmentalConditions = flowEnvironmentalConditions
            )
            /** END **/
        }

    }

    /** Step Completed **/
    override fun onStepCompleted(stepModel: FlowCompletedModel) {
        Log.e("onStepCompleted", stepModel.stepData.toString())
        Log.e("onStepCompleted", stepModel.submitRequestModel?.extractedInformation.toString())
    }



    /** FLOW Completed **/
    override fun onFlowCompleted(flowData: List<FlowCompletedModel>) {
        val x = flowData;
        flowData.forEach {
            Log.e("onFlowCompleted",it.stepData.toString())
            Log.e("onFlowCompleted", it.submitRequestModel?.extractedInformation.toString())

        }
        Toast.makeText(this, "Flow Completed", Toast.LENGTH_SHORT).show()
    }

    /** UI **/
    private fun showLoader() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressBar.visibility = View.GONE
    }

}



