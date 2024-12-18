package com.assentify.sdk


import DataType
import LanguageTransformationModel
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
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.LanguageTransformationEnum
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
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

class MainActivity : AppCompatActivity(), AssentifySdkCallback , ContextAwareSigningCallback {
    private lateinit var assentifySdk: AssentifySdk
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var scanID: ScanIDCard;
    private lateinit var face: FaceMatch;

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
                "#FFFFFF",
                "#FFC400",

                );


            assentifySdk = AssentifySdk(
                "7UXZBSN2CeGxamNnp9CluLJn7Bb55lJo2SjXmXqiFULyM245nZXGGQvs956Fy5a5s1KoC4aMp5RXju8w",
                "4232e33b-1a90-4b74-94a4-08dcab07bc4d",
                "F0D1B6A7D863E9E4089B70EE5786D3D8DF90EE7BDD12BE315019E1F2FC0E875A",
                environmentalConditions,
                this,
                true,
                true,
                false,
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
        Log.e("ContextAwareSigning","onAssentifySdkInitError ${message}");
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("ContextAwareSigning","onAssentifySdkInitSuccess");
        assentifySdk.startContextAwareSigning(this)
    }

    override fun onHasTokens(documentTokens: List<DocumentTokensModel>) {
        Log.e("ContextAwareSigning","onHasTokens");
        documentTokens.forEach{item ->
            Log.e("ContextAwareSigning onHasTokens", item.id.toString());
            Log.e("ContextAwareSigning onHasTokens", item.tokenValue);
            Log.e("ContextAwareSigning onHasTokens", item.displayName);
            Log.e("ContextAwareSigning onHasTokens", item.templateId.toString());
            Log.e("ContextAwareSigning onHasTokens", item.tokenTypeEnum.toString());
        }
    }

    override fun onCreateUserDocumentInstance(userDocumentResponseModel: CreateUserDocumentResponseModel) {
        Log.e("ContextAwareSigning onCreateUserDocumentInstance",userDocumentResponseModel.templateInstance);
        Log.e("ContextAwareSigning onCreateUserDocumentInstance", userDocumentResponseModel.documentId.toString());
        Log.e("ContextAwareSigning onCreateUserDocumentInstance", userDocumentResponseModel.templateInstanceId.toString());
        Log.e("ContextAwareSigning onCreateUserDocumentInstance", userDocumentResponseModel.isPdf.toString());
    }

    override fun onSignature(signatureResponseModel: SignatureResponseModel) {
        Log.e("ContextAwareSigning onSignature",signatureResponseModel.signedDocument);
        Log.e("ContextAwareSigning onSignature",signatureResponseModel.fileName);
        Log.e("ContextAwareSigning onSignature",signatureResponseModel.signedDocumentUri);
    }

    override fun onError(message: String) {
        Log.e("ContextAwareSigning onError",message);
    }


}

