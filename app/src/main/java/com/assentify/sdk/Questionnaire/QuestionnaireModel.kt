package com.assentify.sdk.Questionnaire


data class QuestionnaireModel(
    val header: String?,
    val subHeader: String?,
    val svgLogoUrl: String?,
    val questions: List<QuestionModel>?,
    val nextButtonTitle: String?,
    val isNormalClick: Boolean?,
)

data class QuestionModel(
    val title: String,
    val subTitle: String,
    val weight: Int,
    val image: String,
    val keyProperty: String,
    val keyPropertyIdentifier: String,
    val valueProperty: String,
    val valuePropertyIdentifier: String,
    val value: String,
    val allowMultipleAnswers: Boolean,
    val answers: List<AnswerModel>
)

data class AnswerModel(
    val key: String,
    val value: String,
    val weight: Int
)