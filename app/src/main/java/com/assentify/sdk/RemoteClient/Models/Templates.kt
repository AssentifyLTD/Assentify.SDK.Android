package com.assentify.sdk.RemoteClient.Models

import com.google.gson.Gson


data class TemplatesByCountry(
    val id: Int,
    val name: String,
    val sourceCountryCode: String,
    val flag: String,
    val templates: List<Templates>
) {
}


data class KycDocumentDetails(
    val name: String,
    var order:Int,
    var templateSpecimen:String,
    var hasQrCode:Boolean,
    val templateProcessingKeyInformation: String,
)

data class Templates(
    val id: Int,
    val sourceCountryFlag: String,
    val sourceCountryCode: String,
    val kycDocumentType: String,
    val sourceCountry: String,
    val kycDocumentDetails: List<KycDocumentDetails>
)

fun encodeTemplatesByCountryToJson(data: List<TemplatesByCountry>): String {
  val gson = Gson()
  return try {
    gson.toJson(data)
  } catch (e: Exception) {
    "${e.message}"
  }
}
