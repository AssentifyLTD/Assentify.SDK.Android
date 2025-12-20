package com.assentify.sdk.Flow.Terms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.ReusableComposable.Events.TermsAndConditionsEventTypes
import com.assentify.sdk.RemoteClient.Models.TermsConditionsModel

class TermsAndConditionsComposeActivity : ComponentActivity() {

    private var termsConditionsModel = mutableStateOf<TermsConditionsModel?>(null)
    private var termsAndConditionsEventTypes = mutableStateOf<String>(TermsAndConditionsEventTypes.onSend)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configModel = ConfigModelObject.getConfigModelObject()



        TermsConditionsHelper.getTermsConditionsStep(configModel, FlowController.getCurrentStep()!!.stepDefinition!!.stepId) { termsModel ->
            termsConditionsModel.value = termsModel
            termsAndConditionsEventTypes.value = TermsAndConditionsEventTypes.onHasData
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@TermsAndConditionsComposeActivity);
            }
        })


        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TermsAndConditionsScreen(
                        termsConditionsModel = termsConditionsModel.value,
                        termsAndConditionsEventTypes = termsAndConditionsEventTypes.value,
                        onBack   = { onBackPressedDispatcher.onBackPressed() },
                        onAccept = {
                            val confirmationKey = FlowController.getCurrentStep()!!.stepDefinition!!.outputProperties.first().key
                            val extractedInformation: Map<String, String> = mapOf(
                                confirmationKey to "true"
                            )
                            FlowController.makeCurrentStepDone(extractedInformation);
                            FlowController.chekSplitStepAndMoveNext(this)
                        },
                        onDecline = {
                            onBackPressedDispatcher.onBackPressed()
                        }
                    )
                }
            }
        }
    }


    companion object {

        fun start(context: Context) {
            val intent = Intent(context, TermsAndConditionsComposeActivity::class.java)
            context.startActivity(intent)
        }
    }
}
