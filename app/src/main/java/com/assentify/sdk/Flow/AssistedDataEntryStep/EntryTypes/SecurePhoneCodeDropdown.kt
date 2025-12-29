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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.FlowEnvironmentalConditionsObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurePhoneInput(
    title: String,
    options: List<CountryOption>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (selectedDial:String, localNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val pillColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor))
    val accent = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
    val isReadOnly = false

    val defaultIso2 = remember(field.inputKey, page) {
        field.defaultCountryCode!!.trim().uppercase()
    }

    val defaultDial = remember(defaultIso2, options) {
        options.firstOrNull { it.code2.equals(defaultIso2, true) }?.dialCode ?: ""
    }

    val defaultRawNumber = remember(field.inputKey, page) {
        AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
    }
    val defaultLocal = remember(defaultRawNumber) { defaultRawNumber.filter { it.isDigit() } }


    var phoneRegex by remember { mutableStateOf("") }
    if(phoneRegex.isEmpty()){
        val defaultRegex = options.firstOrNull { it.code2.equals(defaultIso2, true) }?.phoneRegex
        AssistedFormHelper.changeRegex(field.inputKey!!,defaultRegex!!.pattern,defaultDial, page)
    }






    var expanded by remember { mutableStateOf(false) }
    var selectedIso2 by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultIso2) }
    var selectedDial by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultDial) }
    var localNumber by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultLocal) }

    LaunchedEffect(selectedIso2, options) {
        options.firstOrNull { it.code2.equals(selectedIso2, true) }?.dialCode?.let { selectedDial = it }
    }


    var err by remember(field.inputKey, page) { mutableStateOf("") }


    LaunchedEffect(selectedIso2, selectedDial, localNumber,phoneRegex) {
        val full = if (selectedDial.isBlank()) localNumber.filter(Char::isDigit)
        else selectedDial + localNumber.filter(Char::isDigit)
        onValueChange(selectedDial,localNumber,)
        // 3) then call your helper validation (reads updated model)
        val helperError = AssistedFormHelper.validateField(field.inputKey!!, page) ?: ""

        // 4) set the error state after both checks
        err =  helperError
    }




    val selected = options.firstOrNull { it.code2.equals(selectedIso2, true) }
    val codeDisplay = selected?.let { "${flagEmoji(it.code2)} ${it.dialCode}" }
        ?: if (selectedDial.isNotBlank()) selectedDial else "—"

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // ---- Country code dropdown (ISO2) ----
            ExposedDropdownMenuBox(
                expanded = expanded && !isReadOnly,
                onExpandedChange = { if (!isReadOnly) expanded = !expanded },
                modifier = Modifier.width(140.dp)
            ) {
                TextField(
                    value = codeDisplay,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Choose code",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = pillColor,
                        unfocusedContainerColor = pillColor,
                        disabledContainerColor = pillColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
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
                                    Text(flagEmoji(option.code2), color = Color.White)
                                    Spacer(Modifier.width(10.dp))
                                    Text("${option.dialCode}", color = Color.White)
                                }
                            },
                            onClick = {
                                AssistedFormHelper.changeRegex(field.inputKey!!,option.phoneRegex.pattern,option.dialCode, page)
                                selectedIso2 = option.code2
                                selectedDial = option.dialCode
                                phoneRegex = option.phoneRegex.pattern
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // ---- Local number input ----
            TextField(
                value = localNumber,
                onValueChange = { new ->
                    localNumber = new.filter { it.isDigit() || it == ' ' || it == '-' }
                },
                readOnly = isReadOnly,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                placeholder = { Text("", color = Color.White.copy(alpha = 0.6f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = pillColor,
                    unfocusedContainerColor = pillColor,
                    disabledContainerColor = pillColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
            )
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(err, color = accent, fontSize = 12.sp)
        }
    }
}


