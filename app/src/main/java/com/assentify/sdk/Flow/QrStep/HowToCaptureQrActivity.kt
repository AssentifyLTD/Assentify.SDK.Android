package com.assentify.sdk.Flow.QrStep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier


class HowToCaptureQrActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HowToCaptureQrScreen(
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onNext = {
                            QrScanActivity.start(
                                context = this
                            )
                        },
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, HowToCaptureQrActivity::class.java)
            context.startActivity(intent)
        }
    }
}