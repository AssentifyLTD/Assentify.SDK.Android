package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes
import AssistedFormHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement

import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureNationalityDropdown(
    title: String,
    options: List<CountryOption>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()



    /** Default Value **/
    var defaultRaw by rememberSaveable { mutableStateOf<String>("") }
    LaunchedEffect(field.inputKey, field.languageTransformation) {
        if (field.languageTransformation == 0) {
            defaultRaw =  AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
        } else {
            if(defaultRaw.isEmpty()) {
                val dataList = listOf(
                    LanguageTransformationModel(
                        language = field.targetOutputLanguage!!,
                        languageTransformationEnum = field.languageTransformation!!,
                        value = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page)
                            ?: "",
                        key = field.inputKey!!,
                        dataType = field.inputType
                    )
                )
                AssistedFormHelper.valueTransformation(
                    field.targetOutputLanguage,
                    TransformationModel(LanguageTransformationModels = dataList)
                ) { data ->
                    if (data != null) {
                        defaultRaw = data.value
                        AssistedFormHelper.changeValue(field.inputKey, data.value, page);
                    } else {
                        defaultRaw =
                            AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
                    }
                }
            }
        }
    }


    val defaultCode = remember(defaultRaw, options) {
        val raw = defaultRaw.trim()

        options.firstOrNull { it.code3.equals(raw, ignoreCase = true) }?.code3
            ?: options.firstOrNull { it.name.equals(raw, ignoreCase = true) }?.code3
            ?: options.firstOrNull { it.name.contains(raw, ignoreCase = true) }?.code3
            ?: options.firstOrNull { it.code3.contains(raw, ignoreCase = true) }?.code3
            ?: ""
    }


    var expanded by remember { mutableStateOf(false) }
    var selectedCode by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultCode) }
    LaunchedEffect(defaultCode) { selectedCode = defaultCode }

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        return (field.isLocked == true) && identifiers.isNotEmpty()
    }
    val isReadOnly = (field.readOnly == true) || getIsLocked()

    val err by remember(field.inputKey, page, selectedCode) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    val pillColor = BaseTheme.FieldColor

    // Helper to show current selection text
    val selectedCountry = options.firstOrNull { it.code3.equals(selectedCode, true) }
    val displayText = selectedCountry?.let { "${flagEmoji(it.code2)}  ${it.name}" } ?: ""


    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color =   BaseTheme.BaseTextColor,
            fontSize = 14.sp,
            fontFamily = InterFont,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded && !isReadOnly,
            onExpandedChange = { if (!isReadOnly) expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = BaseTheme.BaseTextColor,),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown Arrow",
                        tint = BaseTheme.BaseTextColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(30.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = pillColor,
                    unfocusedContainerColor = pillColor,
                    disabledContainerColor = pillColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = BaseTheme.BaseTextColor,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .fillMaxWidth()
                    .height(55.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded && !isReadOnly,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(pillColor)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(flagEmoji(option.code2), color = BaseTheme.BaseTextColor,)
                                Spacer(Modifier.width(10.dp))
                                Text(option.name, color = BaseTheme.BaseTextColor,)
                            }
                        },
                        onClick = {
                            selectedCode = option.code3.uppercase()
                            expanded = false
                            onValueChange(selectedCode) // return ISO code
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(err, color = BaseTheme.BaseRedColor, fontSize = 12.sp)
        }
    }
}

data class CountryOption(
    val code3: String,       // ISO3, e.g., "USA"
    val code2: String,       // ISO2, e.g., "US"
    val name: String,        // e.g., "United States"
    val dialCode: String,    // e.g., "+1"
    val phoneRegex: Regex    // Local validation
)
fun flagEmoji(iso2: String): String {
    val code = iso2.trim().uppercase()
    if (code.length != 2) return "🏳"
    val base = 0x1F1E6 // 'A' regional indicator
    val a = base + (code[0] - 'A')
    val b = base + (code[1] - 'A')
    return String(Character.toChars(a)) + String(Character.toChars(b))
}


val allCountries = listOf(
    // North America
    CountryOption("USA", "US", "United States", "+1", Regex("^[2-9][0-9]{9}$")),
    CountryOption("CAN", "CA", "Canada", "+1", Regex("^[2-9][0-9]{9}$")),
    CountryOption("MEX", "MX", "Mexico", "+52", Regex("^[1-9]\\d{9,10}$")),

    // Central America & Caribbean
    CountryOption("GTM", "GT", "Guatemala", "+502", Regex("^[2-9]\\d{7}$")),
    CountryOption("HND", "HN", "Honduras", "+504", Regex("^[3-9]\\d{7}$")),
    CountryOption("SLV", "SV", "El Salvador", "+503", Regex("^[67]\\d{7}$")),
    CountryOption("NIC", "NI", "Nicaragua", "+505", Regex("^[58]\\d{7}$")),
    CountryOption("CRI", "CR", "Costa Rica", "+506", Regex("^[5-8]\\d{7}$")),
    CountryOption("PAN", "PA", "Panama", "+507", Regex("^[6-7]\\d{7}$")),
    CountryOption("DOM", "DO", "Dominican Republic", "+1-809", Regex("^[2-9]\\d{7}$")),
    CountryOption("HTI", "HT", "Haiti", "+509", Regex("^[34]\\d{7}$")),
    CountryOption("JAM", "JM", "Jamaica", "+1-876", Regex("^[2-9]\\d{7}$")),
    CountryOption("PRI", "PR", "Puerto Rico", "+1-787", Regex("^[2-9]\\d{7}$")),
    CountryOption("CUB", "CU", "Cuba", "+53", Regex("^5\\d{7}$")),

    // South America
    CountryOption("BRA", "BR", "Brazil", "+55", Regex("^[1-9]\\d{9,10}$")),
    CountryOption("ARG", "AR", "Argentina", "+54", Regex("^\\d{10}$")),
    CountryOption("COL", "CO", "Colombia", "+57", Regex("^3\\d{9}$")),
    CountryOption("PER", "PE", "Peru", "+51", Regex("^9\\d{8}$")),
    CountryOption("CHL", "CL", "Chile", "+56", Regex("^[2-9]\\d{8}$")),
    CountryOption("ECU", "EC", "Ecuador", "+593", Regex("^9\\d{8}$")),
    CountryOption("VEN", "VE", "Venezuela", "+58", Regex("^4\\d{9}$")),
    CountryOption("BOL", "BO", "Bolivia", "+591", Regex("^[67]\\d{7}$")),
    CountryOption("PRY", "PY", "Paraguay", "+595", Regex("^9\\d{8}$")),
    CountryOption("URY", "UY", "Uruguay", "+598", Regex("^9\\d{7}$")),

    // Europe
    CountryOption("GBR", "GB", "United Kingdom", "+44", Regex("^\\d{10,11}$")),
    CountryOption("DEU", "DE", "Germany", "+49", Regex("^\\d{10,12}$")),
    CountryOption("FRA", "FR", "France", "+33", Regex("^[1-9]\\d{8}$")),
    CountryOption("ITA", "IT", "Italy", "+39", Regex("^[3-9]\\d{8,10}$")),
    CountryOption("ESP", "ES", "Spain", "+34", Regex("^[6-9]\\d{8}$")),
    CountryOption("PRT", "PT", "Portugal", "+351", Regex("^9\\d{8}$")),
    CountryOption("NLD", "NL", "Netherlands", "+31", Regex("^[6-9]\\d{8}$")),
    CountryOption("BEL", "BE", "Belgium", "+32", Regex("^4\\d{8}$")),
    CountryOption("CHE", "CH", "Switzerland", "+41", Regex("^(7|4)\\d{8}$")),
    CountryOption("AUT", "AT", "Austria", "+43", Regex("^[6-9]\\d{8,9}$")),
    CountryOption("SWE", "SE", "Sweden", "+46", Regex("^[1-9]\\d{6,9}$")),
    CountryOption("NOR", "NO", "Norway", "+47", Regex("^\\d{8}$")),
    CountryOption("DNK", "DK", "Denmark", "+45", Regex("^\\d{8}$")),
    CountryOption("FIN", "FI", "Finland", "+358", Regex("^[4-5]\\d{8}$")),
    CountryOption("POL", "PL", "Poland", "+48", Regex("^[5-8]\\d{8}$")),
    CountryOption("RUS", "RU", "Russia", "+7", Regex("^9\\d{9}$")),
    CountryOption("UKR", "UA", "Ukraine", "+380", Regex("^[3-9]\\d{8}$")),
    CountryOption("IRL", "IE", "Ireland", "+353", Regex("^[8-9]\\d{8}$")),
    CountryOption("GRC", "GR", "Greece", "+30", Regex("^6\\d{9}$")),
    CountryOption("CZE", "CZ", "Czech Republic", "+420", Regex("^[6-7]\\d{8}$")),
    CountryOption("ROU", "RO", "Romania", "+40", Regex("^7\\d{8}$")),
    CountryOption("HUN", "HU", "Hungary", "+36", Regex("^[2-9]\\d{8}$")),
    CountryOption("BGR", "BG", "Bulgaria", "+359", Regex("^[6789]\\d{8}$")),
    CountryOption("SRB", "RS", "Serbia", "+381", Regex("^6\\d{7,8}$")),
    CountryOption("HRV", "HR", "Croatia", "+385", Regex("^9\\d{8}$")),
    CountryOption("SVK", "SK", "Slovakia", "+421", Regex("^9\\d{8}$")),
    CountryOption("SVN", "SI", "Slovenia", "+386", Regex("^[3-7]\\d{7}$")),
    CountryOption("BLR", "BY", "Belarus", "+375", Regex("^[2-9]\\d{8}$")),
    CountryOption("LTU", "LT", "Lithuania", "+370", Regex("^6\\d{7}$")),
    CountryOption("LVA", "LV", "Latvia", "+371", Regex("^2\\d{7}$")),
    CountryOption("EST", "EE", "Estonia", "+372", Regex("^[5-9]\\d{7}$")),
    CountryOption("ALB", "AL", "Albania", "+355", Regex("^6\\d{7}$")),
    CountryOption("MKD", "MK", "North Macedonia", "+389", Regex("^7\\d{7}$")),
    CountryOption("BIH", "BA", "Bosnia and Herzegovina", "+387", Regex("^6\\d{7}$")),
    CountryOption("MLT", "MT", "Malta", "+356", Regex("^[79]\\d{7}$")),
    CountryOption("ISL", "IS", "Iceland", "+354", Regex("^\\d{7}$")),
    CountryOption("LUX", "LU", "Luxembourg", "+352", Regex("^[24-9]\\d{7}$")),

    // Middle East
    CountryOption("SAU", "SA", "Saudi Arabia", "+966", Regex("^5\\d{8}$")),
    CountryOption("ARE", "AE", "United Arab Emirates", "+971", Regex("^5\\d{8}$")),
    CountryOption("TUR", "TR", "Turkey", "+90", Regex("^5\\d{9}$")),
    CountryOption("IRN", "IR", "Iran", "+98", Regex("^9\\d{9}$")),
    CountryOption("IRQ", "IQ", "Iraq", "+964", Regex("^7[3-9]\\d{8}$")),
    CountryOption("JOR", "JO", "Jordan", "+962", Regex("^7\\d{8}$")),
    CountryOption("LBN", "LB", "Lebanon", "+961", Regex("^\\d{7,8}$")),
    CountryOption("KWT", "KW", "Kuwait", "+965", Regex("^[569]\\d{7}$")),
    CountryOption("QAT", "QA", "Qatar", "+974", Regex("^3\\d{7}$")),
    CountryOption("OMN", "OM", "Oman", "+968", Regex("^(9|7)\\d{7}$")),
    CountryOption("BHR", "BH", "Bahrain", "+973", Regex("^3\\d{7}$")),
    CountryOption("YEM", "YE", "Yemen", "+967", Regex("^7\\d{8}$")),
    CountryOption("SYR", "SY", "Syria", "+963", Regex("^9\\d{8}$")),

    // Africa
    CountryOption("EGY", "EG", "Egypt", "+20", Regex("^1\\d{8,9}$")),
    CountryOption("ZAF", "ZA", "South Africa", "+27", Regex("^[6-8]\\d{8}$")),
    CountryOption("NGA", "NG", "Nigeria", "+234", Regex("^\\d{7,10}$")),
    CountryOption("KEN", "KE", "Kenya", "+254", Regex("^(7|1)\\d{8}$")),
    CountryOption("ETH", "ET", "Ethiopia", "+251", Regex("^9\\d{8}$")),
    CountryOption("GHA", "GH", "Ghana", "+233", Regex("^[235]\\d{8}$")),
    CountryOption("TZA", "TZ", "Tanzania", "+255", Regex("^[67]\\d{8}$")),
    CountryOption("UGA", "UG", "Uganda", "+256", Regex("^[7]\\d{8}$")),
    CountryOption("DZA", "DZ", "Algeria", "+213", Regex("^[5-7]\\d{8}$")),
    CountryOption("MAR", "MA", "Morocco", "+212", Regex("^[5-9]\\d{8}$")),
    CountryOption("TUN", "TN", "Tunisia", "+216", Regex("^[2459]\\d{7}$")),
    CountryOption("LBY", "LY", "Libya", "+218", Regex("^9[1-9]\\d{7}$")),
    CountryOption("SDN", "SD", "Sudan", "+249", Regex("^9\\d{8}$")),
    CountryOption("SEN", "SN", "Senegal", "+221", Regex("^7\\d{8}$")),
    CountryOption("CIV", "CI", "Ivory Coast", "+225", Regex("^[0-9]\\d{9}$")),
    CountryOption("CMR", "CM", "Cameroon", "+237", Regex("^[236-9]\\d{7}$")),
    CountryOption("AGO", "AO", "Angola", "+244", Regex("^9\\d{8}$")),
    CountryOption("MOZ", "MZ", "Mozambique", "+258", Regex("^8[2-9]\\d{7}$")),
    CountryOption("ZMB", "ZM", "Zambia", "+260", Regex("^9\\d{8}$")),
    CountryOption("ZWE", "ZW", "Zimbabwe", "+263", Regex("^7\\d{8}$")),
    CountryOption("MWI", "MW", "Malawi", "+265", Regex("^[1789]\\d{7}$")),
    CountryOption("RWA", "RW", "Rwanda", "+250", Regex("^7\\d{8}$")),
    CountryOption("BDI", "BI", "Burundi", "+257", Regex("^[79]\\d{7}$")),

    // Asia
    CountryOption("CHN", "CN", "China", "+86", Regex("^1\\d{10}$")),
    CountryOption("IND", "IN", "India", "+91", Regex("^[6-9]\\d{9}$")),
    CountryOption("PAK", "PK", "Pakistan", "+92", Regex("^3\\d{9}$")),
    CountryOption("IDN", "ID", "Indonesia", "+62", Regex("^8\\d{8,11}$")),
    CountryOption("JPN", "JP", "Japan", "+81", Regex("^(70|80|90)\\d{8}$")),
    CountryOption("PHL", "PH", "Philippines", "+63", Regex("^9\\d{9}$")),
    CountryOption("VNM", "VN", "Vietnam", "+84", Regex("^(3|5|7|8|9)\\d{8}$")),
    CountryOption("THA", "TH", "Thailand", "+66", Regex("^[689]\\d{8}$")),
    CountryOption("MYS", "MY", "Malaysia", "+60", Regex("^1\\d{8,9}$")),
    CountryOption("SGP", "SG", "Singapore", "+65", Regex("^[689]\\d{7}$")),
    CountryOption("KOR", "KR", "South Korea", "+82", Regex("^1\\d{9}$")),
    CountryOption("BGD", "BD", "Bangladesh", "+880", Regex("^1[3-9]\\d{8}$")),
    CountryOption("AFG", "AF", "Afghanistan", "+93", Regex("^7\\d{8}$")),
    CountryOption("NPL", "NP", "Nepal", "+977", Regex("^9\\d{9}$")),
    CountryOption("LKA", "LK", "Sri Lanka", "+94", Regex("^7\\d{8}$")),
    CountryOption("MMR", "MM", "Myanmar", "+95", Regex("^9\\d{8}$")),
    CountryOption("KHM", "KH", "Cambodia", "+855", Regex("^[1-9]\\d{7}$")),
    CountryOption("LAO", "LA", "Laos", "+856", Regex("^[2-9]\\d{7}$")),
    CountryOption("MNG", "MN", "Mongolia", "+976", Regex("^[5-9]\\d{7}$")),
    CountryOption("TWN", "TW", "Taiwan", "+886", Regex("^9\\d{8}$")),
    CountryOption("HKG", "HK", "Hong Kong", "+852", Regex("^[569]\\d{7}$")),
    CountryOption("MAC", "MO", "Macau", "+853", Regex("^6\\d{7}$")),
    CountryOption("BRN", "BN", "Brunei", "+673", Regex("^[2-9]\\d{6}$")),
    CountryOption("MDV", "MV", "Maldives", "+960", Regex("^[7-9]\\d{6}$")),
    CountryOption("BTN", "BT", "Bhutan", "+975", Regex("^[17]\\d{7}$")),

    // Oceania
    CountryOption("AUS", "AU", "Australia", "+61", Regex("^(4\\d{8}|[2378]\\d{8,9})$")),
    CountryOption("NZL", "NZ", "New Zealand", "+64", Regex("^(2\\d{7,9}|[34679]\\d{7,9})$")),
    CountryOption("PNG", "PG", "Papua New Guinea", "+675", Regex("^[7-9]\\d{7}$")),
    CountryOption("FJI", "FJ", "Fiji", "+679", Regex("^[7-9]\\d{6}$")),
    CountryOption("SLB", "SB", "Solomon Islands", "+677", Regex("^[7-9]\\d{6}$")),
    CountryOption("VUT", "VU", "Vanuatu", "+678", Regex("^[5-9]\\d{6}$")),
    CountryOption("WSM", "WS", "Samoa", "+685", Regex("^[7-9]\\d{6}$")),
    CountryOption("TON", "TO", "Tonga", "+676", Regex("^[7-9]\\d{6}$")),

    // Central Asia
    CountryOption("KAZ", "KZ", "Kazakhstan", "+7", Regex("^[67]\\d{9}$")),
    CountryOption("UZB", "UZ", "Uzbekistan", "+998", Regex("^[679]\\d{8}$")),
    CountryOption("KGZ", "KG", "Kyrgyzstan", "+996", Regex("^[5-7]\\d{8}$")),
    CountryOption("TJK", "TJ", "Tajikistan", "+992", Regex("^[9]\\d{8}$")),
    CountryOption("TKM", "TM", "Turkmenistan", "+993", Regex("^[6-8]\\d{7}$"))
)
