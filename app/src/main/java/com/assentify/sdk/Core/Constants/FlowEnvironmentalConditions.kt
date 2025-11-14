package com.assentify.sdk.Core.Constants



public  class FlowEnvironmentalConditions(
    val appLogo: ByteArray?,
    val backgroundHexColor: String,
    val clicksHexColor: String,
    val listItemsSelectedHexColor: String,
    val listItemsUnSelectedHexColor: String,
    val language :String = Language.NON,
    val enableNfc :Boolean = false,
    val blockLoaderCustomProperties :Map<String,Any> = emptyMap(),
) {
    init {
        require(appLogo != null) { "appLogo is required" }
        require(backgroundHexColor.isNotEmpty()) { "backgroundHexColor is required" }
        require(clicksHexColor.isNotEmpty()) { "clicksHexColor is required" }
        require(listItemsSelectedHexColor.isNotEmpty()) { "listItemsSelectedHexColor is required" }
        require(listItemsUnSelectedHexColor.isNotEmpty()) { "listItemsUnSelectedHexColor is required" }
    }
}


