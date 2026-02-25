package com.assentify.sdk.Flow.AssistedDataEntryStep

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
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.AssistedDataEntry.AssistedDataEntryCallback
import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.AssistedDataEntry.Models.InputTypes
import com.assentify.sdk.AssistedDataEntryPagesObject
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes


class AssistedDataEntryActivity : ComponentActivity(), AssistedDataEntryCallback {


    private var status = mutableStateOf("InProgress")
    private var eventTypes = mutableStateOf(EventTypes.onSend)

    private var timeStarted = getCurrentDateTimeForTracking()

    private var assistedDataModel = mutableStateOf<AssistedDataEntryModel?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
        assentifySdk.startAssistedDataEntry(
            this,
            stepId = FlowController.getCurrentStep()!!.stepDefinition!!.stepId
        )

        /** Track Progress **/
        val  currentStep = FlowController.getCurrentStep()
        FlowController.trackProgress(
            currentStep = currentStep!!,
            response = null,
            inputData = FlowController.outputPropertiesToMap(currentStep.stepDefinition!!.outputProperties),
            status = "InProgress"
        )
        /***/


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@AssistedDataEntryActivity);
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AssistedDataEntryScreen(
                        assistedDataEntryModel = assistedDataModel.value,
                        eventTypes = eventTypes.value,
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onNext = {
                            val extractedInformation = mutableMapOf<String, String>()
                            val model =
                                AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
                            val pages = model!!.assistedDataEntryPages

                            for (page in pages) {
                                for (element in page.dataEntryPageElements) {
                                    val key = element.inputKey
                                    val isDirtyKey = element.isDirtyKey
                                    val value = element.value
                                    val fieldType = InputTypes.fromString(element.inputType)
                                    /** Normal Key **/
                                    if (!key.isNullOrBlank() && !value.isNullOrBlank()) {
                                        if (fieldType == InputTypes.PhoneNumber) {
                                            extractedInformation[key] =
                                                element.defaultCountryCode + value
                                        } else {
                                            extractedInformation[key] = value
                                        }

                                    }
                                    /** Dirty Key **/
                                    if (!isDirtyKey.isNullOrBlank() && !value.isNullOrBlank()) {
                                        if (fieldType == InputTypes.PhoneNumber) {
                                            extractedInformation[isDirtyKey] =
                                                element.defaultCountryCode + value
                                        } else {
                                            extractedInformation[isDirtyKey] = value
                                        }
                                    }
                                    /** Data Source Keys **/
                                    if (!element.dataSourceValues.isNullOrEmpty()) {
                                        element.dataSourceValues!!.forEach {
                                            extractedInformation[it.key] = it.value
                                        }
                                    }
                                }
                            }
                            status.value = "Completed"
                            FlowController.makeCurrentStepDone(extractedInformation,timeStarted);
                            FlowController.naveToNextStep(context = this)
                        },
                    )
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, AssistedDataEntryActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onAssistedDataEntryError(message: String) {
        runOnUiThread {
            eventTypes.value = EventTypes.onError
        }
    }

    override fun onAssistedDataEntrySuccess(assistedDataEntryModel: AssistedDataEntryModel) {
        runOnUiThread {
            assistedDataModel.value = assistedDataEntryModel;
            AssistedDataEntryPagesObject.clear();
            AssistedDataEntryPagesObject.setAssistedDataEntryModelObject(assistedDataEntryModel);
            eventTypes.value = EventTypes.onComplete
        }
    }



    override fun onStop() {
        callTrackProgress();
        super.onStop()
    }
    private fun callTrackProgress(){
        if(status.value != "Completed"){
            val extractedInformation = mutableMapOf<String, String>()
            val model =
                AssistedDataEntryPagesObject.getAssistedDataEntryModelObject()
            val pages = model!!.assistedDataEntryPages

            for (page in pages) {
                for (element in page.dataEntryPageElements) {
                    val key = element.inputKey
                    val isDirtyKey = element.isDirtyKey
                    val value = element.value
                    val fieldType = InputTypes.fromString(element.inputType)
                    /** Normal Key **/
                    if (!key.isNullOrBlank() && !value.isNullOrBlank()) {
                        if (fieldType == InputTypes.PhoneNumber) {
                            extractedInformation[key] =
                                element.defaultCountryCode + value
                        } else {
                            extractedInformation[key] = value
                        }

                    }
                    /** Dirty Key **/
                    if (!isDirtyKey.isNullOrBlank() && !value.isNullOrBlank()) {
                        if (fieldType == InputTypes.PhoneNumber) {
                            extractedInformation[isDirtyKey] =
                                element.defaultCountryCode + value
                        } else {
                            extractedInformation[isDirtyKey] = value
                        }
                    }
                    /** Data Source Keys **/
                    if (!element.dataSourceValues.isNullOrEmpty()) {
                        element.dataSourceValues!!.forEach {
                            extractedInformation[it.key] = it.value
                        }
                    }
                }
            }

            /** Track Progress **/
            val  currentStep = FlowController.getCurrentStep()
            FlowController.trackProgress(
                currentStep = currentStep!!,
                response = null,
                inputData = extractedInformation,
                status = status.value
            )
            /***/
        }

    }
}