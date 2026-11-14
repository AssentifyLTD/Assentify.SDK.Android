package com.assentify.sdk.Flow.FaceStep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.assentify.sdk.RemoteClient.Models.Templates

class SupportingDocComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lateinit var templates: Templates;


        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SupportingDocScreen(
                        onNext = {
                            if (templates.id == -1) {
                                SupportingPassportScanActivity.start(context = this)
                            } else {
                                SupportingIDCardScanActivity.start(context = this)
                            }
                        },
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onDocumentSelected = { selectedTemplate ->
                            templates = selectedTemplate;
                        },
                        onTemplateClick = { selectedTemplate ->
                            templates = selectedTemplate;
                            if (templates.id == -1) {
                                SupportingPassportScanActivity.start(context = this)
                            } else {
                                SupportingIDCardScanActivity.start(context = this)
                            }

                        },
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, SupportingDocComposeActivity::class.java)
            context.startActivity(intent)
        }
    }
}