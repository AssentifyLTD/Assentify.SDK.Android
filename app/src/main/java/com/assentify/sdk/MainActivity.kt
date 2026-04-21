package com.assentify.sdk


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.ActiveLiveType
import com.assentify.sdk.Core.Constants.BackgroundType
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.Flow.Models.FlowCompletedModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel

data class StartConfig(
    val apiKey: String,
    val language: String,
    val enableDetect: Boolean,
    val enableNfc: Boolean,
    val enableQr: Boolean
)

class MainActivity : AppCompatActivity(), AssentifySdkCallback, FlowCallBack {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private lateinit var progressBar: ProgressBar
    private lateinit var config: StartConfig


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /** Camera Permission **/
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

        /** UI **/
        val etApiKey = findViewById<EditText>(R.id.etApiKey)


          etApiKey.setText("tsUJAAfkAdkqaLsTBTigN0unsTBksNfPNh56OxmbwWqhbV7LVhfoohaP95svqHmyccVlaqe1RHubOHagwpg")


  /*    etApiKey.setText("QwWzzKOYLkDzCLJ9lENlgvRQ1kmkKDv76KbJ9sPfr9Joxwj2DUuzC7htaZP89RqzgB9i9lHc4IpYOA7g")
        etInteractionHash.setText("E4BDD59C3B69A3F89AE8C756FCD67EBC72A45F405B256B3C3BDD643BE282B195")
        etTenantIdentifier.setText("2937c91f-c905-434b-d13d-08dcc04755ec")*/


        val spLanguage = findViewById<Spinner>(R.id.spLanguage)

        val swEnableDetect = findViewById<SwitchCompat>(R.id.swEnableDetect)
        val swEnableNfc = findViewById<SwitchCompat>(R.id.swEnableNfc)
        val swEnableQr = findViewById<SwitchCompat>(R.id.swEnableQr)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val nextFlow = findViewById<Button>(R.id.nextFlow)
      //  nextFlow.visibility = View.GONE
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


        nextFlow.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }

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
            if (config.apiKey.isEmpty()) {
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
                    "#e30505",
                    CountDownNumbersColor = "#ffc400",
                    activeLiveType = ActiveLiveType.Actions,
                    activeLivenessCheckCount = 3,
                    faceLivenessRetryCount = 2,
                    minRam = 1
                );
                assentifySdk = AssentifySdk(
                    apiKey = config.apiKey,
                    configFileName =   "configFile1",
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
        Toast.makeText(this, "AssentifySdk Init Success ${configModel.flowName}", Toast.LENGTH_SHORT).show()
        runOnUiThread {
            /** INIT FLOW **/
            val customProperties: MutableMap<String, String> = mutableMapOf()
            customProperties.put("phoneNumber", "value1")

            val flowEnvironmentalConditions = FlowEnvironmentalConditions(
                /**PortalTheme**/
                backgroundType = BackgroundType.Color,
                //svgBackgroundImageUrl = "https://api.dicebear.com/7.x/shapes/svg?seed=patternA",

                /**Theme 1**/
              /*  logoUrl = "https://image2url.com/r2/default/images/1769694393603-0afa5733-d9a5-4b0d-9134-868d3a750069.png",
                textColor = "#000000",
                accentColor = "#ffc400",
                secondaryTextColor = "#ffffff",
                backgroundCardColor = "#f3f4f6",
                backgroundColor = BackgroundStyle.Solid("#ffffff"),
                clickColor = BackgroundStyle.Solid("#ffc400"),*/

                /**Theme 2**/
             /*   logoUrl = "https://i.postimg.cc/3xY0ybsp/icon-1-(1).png",
                textColor = "#000000",
                accentColor = "#833F89",
                secondaryTextColor = "#000000",
                backgroundCardColor = "#F2F2F2",
                backgroundColor = BackgroundStyle.Solid("#FFFFFF"),
                clickColor = BackgroundStyle.Gradient(
                    colorsHex = listOf("#833F89", "#C82B47"),
                    angleDegrees = 90f,
                    holdUntil = 0.4f
                ),*/

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
            /** END **/
        }

    }

    /** Step Completed **/
    override fun onStepCompleted(stepModel: FlowCompletedModel) {
        Log.e("onStepCompleted", stepModel.stepData.toString())
        Log.e("onStepCompleted", stepModel.submitRequestModel?.stepId.toString())
        Log.e("onStepCompleted", stepModel.submitRequestModel?.stepDefinition.toString())
        Log.e("onStepCompleted", stepModel.submitRequestModel?.extractedInformation.toString())
    }


    /** FLOW Completed **/
    override fun onFlowCompleted(flowData: List<FlowCompletedModel>) {
        val x = flowData;
        flowData.forEach {
            Log.e("onFlowCompleted", it.stepData.toString())
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



