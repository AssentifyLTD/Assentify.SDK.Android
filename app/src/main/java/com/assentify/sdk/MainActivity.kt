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
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.DocumentTokensModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel
import com.assentify.sdk.RemoteClient.Models.StepDefinitions
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.IDExtractedModel
import com.assentify.sdk.ScanIDCard.IDResponseModel
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.PassportResponseModel
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.security.auth.callback.PasswordCallback

class MainActivity : AppCompatActivity(), AssentifySdkCallback ,  ContextAwareSigningCallback {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var scanID: ScanIDCard;
    private lateinit var face: FaceMatch;
    private lateinit var contextAwareSigning: ContextAwareSigning;

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
            500.0f,
            00.0f,
            50.0f,
            100.0f,
            "#FFFFFF",
            "#FFC400",

            );


        assentifySdk = AssentifySdk(
            "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
            "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
            "E893390F6835D4E1D2F12126B7AA2B0ED996C056C8893BA267A7FE90A8452200",
            environmentalConditions,
            this,
            true,
            true,
            false,
            false,
            true,
            true,
            true,
        );

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("Events Here", "onAssentifySdkInitError: " + message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("Events Here", "onAssentifySdkInitSuccess: ")
        AssentifySdkObject.setAssentifySdkObject(assentifySdk);
        contextAwareSigning = assentifySdk.startContextAwareSigning(this);

    }

    override fun onHasTokens(documentTokens: List<DocumentTokensModel>) {
        Log.e("Events Here",documentTokens.toString())
        val tokens = mutableMapOf<String,String>();
        documentTokens.forEach { it ->
            tokens[it.id.toString()] = "Test One"
        }
        contextAwareSigning.createUserDocumentInstance(tokens)
    }

    override fun onCreateUserDocumentInstance(userDocumentResponseModel: CreateUserDocumentResponseModel) {
        Log.e("Events Here", userDocumentResponseModel.documentId.toString())
        Log.e("Events Here",userDocumentResponseModel.templateInstance)

        contextAwareSigning.signature(userDocumentResponseModel.templateInstanceId,userDocumentResponseModel.documentId,"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")
    }

    override fun onSignature(signatureResponseModel: SignatureResponseModel) {
        Log.e("Events Here", signatureResponseModel.fileName)
        Log.e("Events Here",signatureResponseModel.signedDocumentUri)
    }

    override fun onError(message: String) {

    }


}
