package com.assentify.sdk.Flow.IDStep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.assentify.sdk.SelectedTemplatesObject


class HowToCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedTemplate = SelectedTemplatesObject.getSelectedTemplatesObject();


        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HowToCaptureScreen(
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onNext = {
                            if (selectedTemplate.id == -1) {
                                PassportScanActivity.start(context = this)
                            } else {
                                IDCardScanActivity.start(context = this)
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, HowToCaptureActivity::class.java)
            context.startActivity(intent)
        }
    }
}