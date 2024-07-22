package com.assentify.sdk

import LanguageTransformationModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanOther.OtherResponseModel
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassportCallback

class MainActivity : AppCompatActivity() ,AssentifySdkCallback , IDCardCallback {
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
                false,
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
        Log.e("MainActivity", "onAssentifySdkInitError: " + message )
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("MainActivity", "onAssentifySdkInitSuccess: "  )
        startAssentifySdk();
    }

    override fun onHasTemplates(templates: List<TemplatesByCountry>) {
    }

    @SuppressLint("SuspiciousIndentation")
    fun startAssentifySdk() {
        //  Thread.sleep(1000)
        var data: List<KycDocumentDetails> = listOf(
            KycDocumentDetails(
                name = "",
                order = 0,
                templateProcessingKeyInformation = "75b683bb-eb81-4965-b3f0-c5e5054865e7"
            ),
            KycDocumentDetails(
                name = "",
                order = 1,
                templateProcessingKeyInformation = "eae46fac-1763-4d31-9acc-c38d29fe56e4"
            ),
        )

        var scan = assentifySdk.startScanIDCard(
            this,
            data,
            Language.English
        );
        var  fragmentManager = supportFragmentManager
        var  transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, scan)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
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

    }

    override fun onSend() {
        Log.e("MainActivity", "onSend:"   )
    }

    override fun onRetry(dataModel: BaseResponseDataModel) {

    }

    override fun onComplete(dataModel: IDResponseModel,order:Int) {
        dataModel.iDExtractedModel!!.outputProperties!!.forEach { t, u ->

                Log.e("MainActivity", "outputProperties Key" + t + " Value "+ u  )

        }
        dataModel.iDExtractedModel!!.transformedProperties!!.forEach { t, u ->

            Log.e("MainActivity", "transformedProperties Key" + t + " Value "+ u  )

        }
        Log.e("MainActivity", "______________________${order}___________________________" )



    }

    override fun onWrongTemplate(dataModel: BaseResponseDataModel) {
    }

}