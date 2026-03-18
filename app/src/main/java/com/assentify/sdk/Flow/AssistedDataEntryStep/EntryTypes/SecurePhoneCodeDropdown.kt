package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.FlowEnvironmentalConditionsObject

@Composable
fun SecurePhoneInput(
    title: String,
    options: List<CountryOption>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (selectedDial: String, localNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val pillColor = BaseTheme.FieldColor
    val isReadOnly = false

    var defaultIso2 = remember(field.inputKey, page) {
        field.defaultCountryCode!!.trim().uppercase()
    }

    defaultIso2 = options
        .firstOrNull { it.dialCode.equals(defaultIso2, true) }
        ?.code2
        ?: defaultIso2

    val defaultDial = remember(defaultIso2, options) {
        options.firstOrNull {
            it.code2.equals(defaultIso2, true) || it.dialCode.equals(defaultIso2, true)
        }?.dialCode ?: ""
    }

    val defaultRawNumber = remember(field.inputKey, page) {
        AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
    }
    val defaultLocal = remember(defaultRawNumber) { defaultRawNumber.filter { it.isDigit() } }

    var phoneRegex by remember { mutableStateOf("") }
    if (phoneRegex.isEmpty()) {
        val defaultRegex = options.firstOrNull { it.code2.equals(defaultIso2, true) }?.phoneRegex
        AssistedFormHelper.changeRegex(
            field.inputKey!!,
            defaultRegex!!.pattern,
            defaultDial,
            page
        )
    }

    var selectedIso2 by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultIso2) }
    var selectedDial by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultDial) }
    var localNumber by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultLocal) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var userStartedTyping by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIso2, options) {
        options.firstOrNull { it.code2.equals(selectedIso2, true) }?.dialCode?.let {
            selectedDial = it
        }
    }

    var err by remember(field.inputKey, page) { mutableStateOf("") }

    LaunchedEffect(selectedIso2, selectedDial, localNumber, phoneRegex) {
        val full = if (selectedDial.isBlank()) {
            localNumber.filter(Char::isDigit)
        } else {
            selectedDial + localNumber.filter(Char::isDigit)
        }

        onValueChange(selectedDial, localNumber)

        val helperError = AssistedFormHelper.validateField(field.inputKey!!, page) ?: ""
        err = helperError
    }

    val selected = options.firstOrNull { it.code2.equals(selectedIso2, true) }
    val codeDisplay = selected?.let { "${flagEmoji(it.code2)} ${it.dialCode}" }
        ?: if (selectedDial.isNotBlank()) selectedDial else "—"

    val filteredOptions = remember(searchQuery, options, userStartedTyping) {
        if (!userStartedTyping) {
            options
        } else {
            options.filter { option ->
                option.name.contains(searchQuery, ignoreCase = true) ||
                        option.code2.contains(searchQuery, ignoreCase = true) ||
                        option.code3.contains(searchQuery, ignoreCase = true) ||
                        option.dialCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (!field.isHidden!!) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = BaseTheme.BaseTextColor,
                fontSize = 14.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal
            )

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .width(140.dp)
                        .height(55.dp)
                        .clickable(enabled = !isReadOnly) {
                            searchQuery = ""
                            userStartedTyping = false
                            showSearchDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = pillColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = codeDisplay,
                            color = BaseTheme.BaseTextColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Choose code",
                            tint = BaseTheme.BaseTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                TextField(
                    value = localNumber,
                    onValueChange = { new ->
                        localNumber = new.filter { it.isDigit() || it == ' ' || it == '-' }
                    },
                    readOnly = isReadOnly,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = BaseTheme.BaseTextColor),
                    placeholder = { Text("", color = BaseTheme.BaseTextColor.copy(alpha = 0.6f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = pillColor,
                        unfocusedContainerColor = pillColor,
                        disabledContainerColor = pillColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = BaseTheme.BaseTextColor
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
                Text(
                    text = err,
                    color = BaseTheme.BaseRedColor,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showSearchDialog && !isReadOnly) {
        Dialog(
            onDismissRequest = {
                showSearchDialog = false
                searchQuery = ""
                userStartedTyping = false
            }
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BaseTheme.FieldColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Choose code",
                        color = BaseTheme.BaseTextColor,
                        fontSize = 16.sp,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(12.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            userStartedTyping = true
                        },
                        placeholder = {
                            Text(
                                text = "Search...",
                                color = BaseTheme.BaseTextColor.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = BaseTheme.BaseTextColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BaseTheme.FieldColor,
                            unfocusedContainerColor = BaseTheme.FieldColor,
                            disabledContainerColor = BaseTheme.FieldColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = BaseTheme.BaseTextColor,
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (filteredOptions.isEmpty()) {
                            item {
                                Text(
                                    text = "No results found",
                                    color = BaseTheme.BaseTextColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(filteredOptions) { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            AssistedFormHelper.changeRegex(
                                                field.inputKey!!,
                                                option.phoneRegex.pattern,
                                                option.dialCode,
                                                page
                                            )
                                            selectedIso2 = option.code2
                                            selectedDial = option.dialCode
                                            phoneRegex = option.phoneRegex.pattern
                                            showSearchDialog = false
                                            searchQuery = ""
                                            userStartedTyping = false
                                        }
                                        .padding(vertical = 14.dp, horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = flagEmoji(option.code2),
                                        color = BaseTheme.BaseTextColor
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = option.dialCode,
                                        color = BaseTheme.BaseTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}