package com.assentify.sdk


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.assentify.sdk.Core.Constants.ActiveLiveEvents
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FaceEvents
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.StepMap
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDExtractedModel
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.assentify.sdk.ScanQr.ScanQrCallback
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.security.auth.callback.PasswordCallback

class MainActivity : AppCompatActivity(), AssentifySdkCallback{
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var passportClick: LinearLayout
    private lateinit var idClick: LinearLayout
    private lateinit var otherClick: LinearLayout
    private lateinit var loadingText: LinearLayout
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
        );

        assentifySdk = AssentifySdk(
            "6YfQqRbSfTinDuyk7TfFAAdDL55t4EBId4Il2vOFQycl4KydnWsGkA76UNLI3GcrHW3nqoz1gvoewxqsJA",
            "45b09829-03da-4f2b-9f5b-08dc10ed75aa",
            "EC1D0856245C8FF13A1428AA0CC2820FE91D33BA317BCB4568CEECCF0E448641",
            environmentalConditions,
            this,
            true,
            true,
            false,
            true,
            true,
            true,
        );

        loadingText = findViewById(R.id.loadingText);
        loadingText.visibility = View.VISIBLE

        passportClick = findViewById(R.id.passportClick);
        idClick = findViewById(R.id.idClick);
        otherClick = findViewById(R.id.otherClick);


        passportClick.visibility = View.GONE
        idClick.visibility = View.GONE
        otherClick.visibility = View.GONE

        passportClick.setOnClickListener {
            val intent = Intent(this, ScanPassportActivity::class.java);
            startActivity(intent)
        }
        idClick.setOnClickListener {
           val intent = Intent(this, KycActivity::class.java);
            startActivity(intent)
        }
        otherClick.setOnClickListener {
            val intent = Intent(this, ScanOtherActivity::class.java);
            startActivity(intent)
        }

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: ")
        val stepDefinitionsTemp: MutableList<StepMap> = mutableListOf()
        configModel.stepDefinitions.forEach { item ->
            Log.e("onAssentifySdkInitSuccess", item.toString())
        }

        AssentifySdkObject.setAssentifySdkObject(assentifySdk);

        KysModel.setKys(assentifySdk.getTemplates());
        passportClick.visibility = View.VISIBLE
        idClick.visibility = View.VISIBLE
       //  otherClick.visibility = View.VISIBLE
        loadingText.visibility = View.GONE

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


}





