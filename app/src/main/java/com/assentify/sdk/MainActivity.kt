package com.assentify.sdk


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assentify.sdk.AssistedDataEntry.AssistedDataEntryCallback
import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.Core.Constants.ActiveLiveType
import com.assentify.sdk.Core.Constants.DoneFlags
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.FaceMatch.FaceMatchManual
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.StepMap
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanIDCard.ScanIDCardManual
import com.assentify.sdk.ScanOther.OtherResponseModel
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanOther.ScanOtherManual
import com.assentify.sdk.ScanOther.ScanOtherResult
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.assentify.sdk.ScanPassport.ScanPassportManual
import com.assentify.sdk.ScanPassport.ScanPassportResult
import com.assentify.sdk.ScanQr.ScanQrManual
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), AssentifySdkCallback, AssistedDataEntryCallback{
    private lateinit var assentifySdk: AssentifySdk



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val environmentalConditions = EnvironmentalConditions(
            true,
            true,
            "#FFFFFF",
            "#FFC400",
            activeLiveType = ActiveLiveType.NONE,
            activeLivenessCheckCount = 2,
            retryCount = 3,
            minRam = 1

        );
        // F0D1B6A7D863E9E4089B70EE5786D3D8DF90EE7BDD12BE315019E1F2FC0E875A
        // 2726A3681C0CAB2177AEDA50A70A96FD29757A20493AD02577B7578FE8EDE3CA
        assentifySdk = AssentifySdk(
            "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
            "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
            "6874D79505DDEA1F78BEF96BDA94336686F30EA1DAE69F10FE72F454238721B7",
            environmentalConditions,
            assentifySdkCallback = this,
            performActiveLivenessFace = true,
            context = applicationContext,


        );

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("AssentifyTAG", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("AssentifyTAG", "onAssentifySdkInitSuccess: ")
        assentifySdk.startAssistedDataEntry(this)

    }

    override fun onAssistedDataEntryError(message: String) {
        Log.e("AssentifyTAG", "onAssistedDataEntryError: ${message}")
    }

    override fun onAssistedDataEntrySuccess(assistedDataEntryModel: AssistedDataEntryModel) {
        Log.e("AssentifyTAG", "onAssistedDataEntrySuccess: ")
        Log.e("AssentifyTAG",assistedDataEntryModel.toString())
        val  model = assistedDataEntryModel;
    }


}

