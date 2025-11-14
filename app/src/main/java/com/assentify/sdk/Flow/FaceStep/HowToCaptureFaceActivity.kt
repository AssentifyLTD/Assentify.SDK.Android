package com.assentify.sdk.Flow.FaceStep

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


class HowToCaptureFaceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@HowToCaptureFaceActivity);
            }
        })





        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HowToCaptureFaceScreen(
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onNext = {
                            FaceMatchActivity.start(context = this)

                        },
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, HowToCaptureFaceActivity::class.java)
            context.startActivity(intent)
        }
    }
}