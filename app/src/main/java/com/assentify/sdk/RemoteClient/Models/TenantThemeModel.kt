package com.assentify.sdk.RemoteClient.Models

data class TenantThemeModel(
    val id: Int,
    val tenantId: Int,
    val primaryColor: String,
    val accentColor: String,
    val backgroundCardColor: String,
    val topAndButtonColor: String,
    val backgroundBodyColor: String,
    val textColor: String,
    val secondaryTextColor: String,
    val logo: String?,
    val logoIcon: String?,
    val logoAccent: String?,
    val adminPrimaryLogo: String?,
    val stepperType: Int,
    val headerText: String?,
    val icon: String?,
    val loaderType: Int,
    val name: String,
    val isDefault: Boolean
)



