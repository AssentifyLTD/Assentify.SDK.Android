package com.assentify.sdk.Questionnaire



public interface QuestionnaireCallback {
    fun onQuestionnaireCallbackError(message: String)
    fun onQuestionnaireCallbackSuccess(questionnaireModel: QuestionnaireModel)
}