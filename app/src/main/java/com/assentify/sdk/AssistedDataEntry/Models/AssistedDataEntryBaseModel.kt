package com.assentify.sdk.AssistedDataEntry.Models

enum class InputTypes(val value: String) {
    Text("Text"),
    TextArea("TextArea"),
    Date("Date"),
    DropDown("DropDown"),
    Email("Email"),
    RadioButtonGroup("RadioButtonGroup"),
    Nationality("Nationality"),
    PhoneNumber("PhoneNumber"),
    PhoneNumberWithOTP("PhoneNumberWithOTP"),
    EmailWithOTP("EmailWithOTP");

    companion object {
        fun fromString(type: String?): InputTypes =
            values().find { it.value.equals(type, ignoreCase = true) } ?: Text
    }
}


data class AssistedDataEntryBaseModel(
    val statusCode: Int,
    val data: AssistedDataEntryModel
)

data class AssistedDataEntryModel(
    val allowAssistedDataEntry: Boolean,
    val assistedDataEntryPages: List<AssistedDataEntryPage>,
    val header: String,
    val subHeader: String,
    val inputProperties: List<InputProperty>
)

data class AssistedDataEntryPage(
    val title: String,
    val subTitle: String,
    val nextButtonTitle: String,
    var dataEntryPageElements: List<DataEntryPageElement>
)

data class DataEntryPageElement(
    var value: String?,
    var isLocalOtpValid: Boolean = false,
    var dataSourceValues: MutableMap<String, String>? = mutableMapOf(),
    val elementIdentifier: String,
    val endpointId: Int?,
    val dataSourceId: String?,
    val inputType: String,
    val sizeByRows: Int?,
    val textTitle: String?,
    val inputKey: String?,
    val isDirtyKey: String?,
    val mandatory: Boolean?,
    val allowAssistedEntry: Boolean?,
    val sourceKStep: String?,
    val dataKeys: List<String>?,
    val linkedControls: List<String>?,
    var applyRegex: Boolean?,
    val languageTransformation: Int?,
    val targetOutputLanguage: String?,
    var regexDescriptor: String?,
    val regexErrorMessage: String?,
    val showBasedOnParent: Boolean?,
    val dataSourceType: Int?,
    val enableDatePicker: Boolean?,
    val from: String?,
    val enableConstraints: Boolean?,
    val constraintType: Int?,
    val to: String?,
    val dataSourceContent: String?,
    val linkedChildren: Boolean?,
    val sendEmailVerificationLink: Boolean?,
    val hasRelatedDataTypes: Boolean?,
    val inputPropertyIdentifier: String?,
    val inputPropertyIdentifierList: List<String>?,
    val isLocked: Boolean?,
    val readOnly: Boolean?,
    val maxLength: Int?,
    val minLength: Int?,
    val otp: Boolean?,
    val otpSize: Int?,
    val otpType: Int?,
    val otpExpiryTime: Double?,
    val additionalFeatures: Boolean?,
    val children: Map<String, List<DataEntryPageElement>>?,
    var defaultCountryCode: String?
)

data class InputProperty(
    val id: Int,
    val sourcePropertyId: Int,
    val sourceStepId: Int,
    val sourceKey: String,
    val targetPropertyId: Int,
    val targetStepId: Int,
    val targetKey: String,
    val isDeleted: Boolean
)
