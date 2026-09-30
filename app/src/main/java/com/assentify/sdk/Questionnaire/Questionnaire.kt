package com.assentify.sdk.Questionnaire

import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Customization
import com.assentify.sdk.RemoteClient.Models.StepDefinitions

public class Questionnaire(private var apiKey: String, private var configModel: ConfigModel) {
    private var callback: QuestionnaireCallback? = null
    private var stepID: String? = null

    fun setCallback(callback: QuestionnaireCallback) {
        this.callback = callback
    }


    fun setStepId(stepId: String?) {
        this.stepID = stepId
        if (this.stepID == null) {
            val stepsCount = configModel.stepDefinitions.stream()
                .filter { item: StepDefinitions -> item.stepDefinition == StepsNames.Questionnaire }
                .count()

            if (stepsCount == 1L) {
                for ((stepId1, stepDefinition) in configModel.stepDefinitions) {
                    if (stepDefinition == StepsNames.Questionnaire) {
                        this.stepID = stepId1.toString()
                        getQuestionnaireFromConfigFile()
                        break
                    }
                }
            } else {
                requireNotNull(this.stepID) { "Step ID is required because multiple 'Questionnaire' steps are present." }
            }
        } else {
            getQuestionnaireFromConfigFile()
        }

    }


    private fun getQuestionnaireFromConfigFile() {
        val stepDefinitions = configModel.stepDefinitions
        stepDefinitions.forEach {
            if (it.stepId == this.stepID!!.toInt()) {
                val model: QuestionnaireModel = it.customization.toQuestionnaireModel()
                callback!!.onQuestionnaireCallbackSuccess(model)
            }
        }
    }

    fun Customization.toQuestionnaireModel(): QuestionnaireModel {
        return QuestionnaireModel(
            header = this.header,
            subHeader = this.subHeader,
            svgLogoUrl = this.svgLogoUrl,
            questions = this.questions,
            nextButtonTitle = this.nextButtonTitle,
            isNormalClick = this.isNormalClick ?: false
        )
    }
}