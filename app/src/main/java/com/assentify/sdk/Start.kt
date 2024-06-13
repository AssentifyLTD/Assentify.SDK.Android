package com.assentify.sdk

import AssentifySdk
import AssentifySdkCallback
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry

class Start : AppCompatActivity(), AssentifySdkCallback {

    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
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
                "#61A03A",
                "#FFC400",
            );

            assentifySdk = AssentifySdk(
                "3K3ObywhMNAjZa5Ce4Hg4QtJjX9PVaroCgNuMu1qidrjuXkT33sy83opg1BUcbxiAmfxn2QSj7jBZ3X19uVg",
                "021aeae5-10e8-4780-858c-08dbcbc01489",
                "8A7E9FE36FB149AA47F4835A986B7C0ADF282A6102D8C70FE8604B606A5F8E3E",
                environmentalConditions,
                this,
                true,
                true,
                true,
                true,
                true,
                true,
            );

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("MainActivity", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(stepDefinitions: List<StepDefinitions>) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: ")

        runOnUiThread {
            AssentifySdkObject.setAssentifySdkObject(assentifySdk)
            val intent = Intent(this, MainActivity::class.java);
            startActivity(intent)
        }
    }

    override fun onHasTemplates(templates: List<TemplatesByCountry>) {
        println("MainActivity onEnvironmentalConditionsChange")
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