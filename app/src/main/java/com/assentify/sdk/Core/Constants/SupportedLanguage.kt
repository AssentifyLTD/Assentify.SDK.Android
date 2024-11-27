package com.assentify.sdk.Core.Constants

const val  FullNameKey = "FullName"


fun getSelectedWords(input: String, numberOfWords: Int): String {
    if (input.isBlank()) return ""
    val words = input.trim().split("\\s+".toRegex())
    return words.take(numberOfWords).joinToString(" ")
}

fun getRemainingWords(input: String, numberOfWords: Int): String {
    if (input.isBlank()) return ""
    val words = input.trim().split("\\s+".toRegex())
    return if (words.size <= numberOfWords) "" else words.drop(numberOfWords).joinToString(" ")
}
object Language {
    const val English = "en"
    const val Arabic = "ar"
    const val Azerbaijani = "az"
    const val Belarusian = "be"
    const val Georgian = "ka"
    const val Korean = "ko"
    const val Latvian = "lv"
    const val Lithuanian = "lt"
    const val Punjabi = "pa"
    const val Russian = "ru"
    const val Sanskrit = "sa"
    const val Sindhi = "sd"
    const val Thai = "th"
    const val Turkish = "tr"
    const val Ukrainian = "uk"
    const val Urdu = "ur"
    const val Uyghur = "ug"
    const val NON = "NON"
}
object LanguageTransformationEnum {
   const val Transliteration = 1
   const val Translation = 2
}

