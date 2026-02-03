package com.assentify.sdk.Flow.SubmitStep

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
import androidx.lifecycle.lifecycleScope
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Core.Constants.WrapUpKeys
import com.assentify.sdk.Core.Constants.getCurrentDateTime
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.Flow.ReusableComposable.Events.SubmitDataTypes
import com.assentify.sdk.FlowCallbackObject
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.SubmitData.SubmitDataCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubmitStepActivity : ComponentActivity(), SubmitDataCallback {


    private var submitDataTypes =
        mutableStateOf<String>(SubmitDataTypes.onSend)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assentifySdk = AssentifySdkObject.getAssentifySdkObject()

        /** Track Progress **/
        val initSteps = ConfigModelObject.getConfigModelObject().stepDefinitions
        val valuesWrapUp: MutableMap<String, String> = mutableMapOf()
        initSteps.forEach { item ->
            if (item.stepDefinition == StepsNames.WrapUp) {
                item.outputProperties.forEach { property ->
                    if (property.key.contains(WrapUpKeys.TimeEnded)) {
                        valuesWrapUp.put(property.key, getCurrentDateTime())
                    }
                }
                FlowController.trackProgress(
                    currentStep = LocalStepModel(
                        name = "",
                        description = "",
                        iconAssetPath = "",
                        isDone = false,
                        stepDefinition = item,
                        submitRequestModel = SubmitRequestModel(
                            stepDefinition = item.stepDefinition,
                            stepId = item.stepId,
                            extractedInformation = valuesWrapUp
                        )
                    ),
                    response = null,
                    inputData = valuesWrapUp,
                    status = "Completed"
                )
            }
        }

        /***/

        assentifySdk.startSubmitData(this, FlowController.getSubmitList())

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (submitDataTypes.value == SubmitDataTypes.onComplete) {
                    FlowCallbackObject.getFlowCallbackObject()
                        .onFlowCompleted(FlowController.getSubmitList())
                    finishAffinity();
                } else {
                    FlowController.backClick(this@SubmitStepActivity);
                }
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SubmitStepScreen(
                        submitDataTypes = submitDataTypes.value,
                        onBack = {
                            if (submitDataTypes.value == SubmitDataTypes.onComplete) {
                                FlowCallbackObject.getFlowCallbackObject()
                                    .onFlowCompleted(FlowController.getSubmitList())
                                finishAffinity();
                            } else {
                                FlowController.backClick(this@SubmitStepActivity);
                            }

                        },
                        onSubmit = {
                            if (submitDataTypes.value == SubmitDataTypes.onComplete) {
                                FlowCallbackObject.getFlowCallbackObject()
                                    .onFlowCompleted(FlowController.getSubmitList())
                                finishAffinity();
                            } else {
                                assentifySdk.startSubmitData(this, FlowController.getSubmitList())
                                submitDataTypes.value = SubmitDataTypes.onSend
                            }
                        },

                        )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, SubmitStepActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onSubmitError(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            submitDataTypes.value = SubmitDataTypes.onError
            delay(3000)
            submitDataTypes.value = SubmitDataTypes.none
        }
    }

    override fun onSubmitSuccess(message: String) {
        submitDataTypes.value = SubmitDataTypes.onComplete
    }
}