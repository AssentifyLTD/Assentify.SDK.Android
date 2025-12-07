package com.assentify.sdk.Flow.IDStep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.SelectedTemplatesObject

class IDStepComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@IDStepComposeActivity);
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IDStepScreen(
                        onNext = {
                            val selectedTemplate = SelectedTemplatesObject.getSelectedTemplatesObject();
                            HowToCaptureActivity.start(
                                context = this, // Activity
                            );
                            /// SDK TODO Qr
                         /*   if (selectedTemplate.id == -1 || !selectedTemplate.kycDocumentDetails.first().hasQrCode) {
                                HowToCaptureActivity.start(
                                    context = this, // Activity
                                );
                            } else {
                                HowToCaptureQrActivity.start(
                                    context = this, // Activity
                                )
                            }*/

                        },
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onDocumentSelected = { selectedTemplate ->
                            SelectedTemplatesObject.setSelectedTemplatesObject(selectedTemplate)
                        },
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, IDStepComposeActivity::class.java)
            context.startActivity(intent)
        }
    }
}