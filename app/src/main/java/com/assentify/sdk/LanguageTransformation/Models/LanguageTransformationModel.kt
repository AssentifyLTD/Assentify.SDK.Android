data class LanguageTransformationModel(
    val languageTransformationEnum: Int,
    val key: String,
    val value: String,
    val language: String,
    val dataType: String
)

data class TransformationModel(
    val LanguageTransformationModels: List<LanguageTransformationModel>
)