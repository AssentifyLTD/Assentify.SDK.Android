package com.assentify.sdk.Flow.ContextAwareStep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.CountryOption
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.ResendOtpControl
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.flagEmoji
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.OtpHelper
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpRequestOtpModel

@Composable
fun SigningPhoneWithOtp(
    title: String,
    contextAwareSigningModel: ContextAwareSigningModel,
    onValueChange: (String) -> Unit,
    onValid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configModelObject = ConfigModelObject.getConfigModelObject()

    // Lebanon constants
    val countryFlag = "🇱🇧"
    val countryDial = "+961"

    var localNumber by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var sendingOtp by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf("") }

    val otpSize = contextAwareSigningModel.data.otpSize ?: 8
    val otpType = contextAwareSigningModel.data.otpType ?: 1
    var otp by remember { mutableStateOf("") }

    var isOtpStep by remember { mutableStateOf(false) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var userStartedTyping by rememberSaveable { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }

    val searchableCountries = remember {
        listOf(
            CountryOption("LBN", "LB", "Lebanon", "+961", Regex("^\\d{7,8}$"))
        )
    }

    val e164Phone = remember(localNumber) {
        buildLebanonE164(localNumber, countryDial)
    }

    val errToShow by remember(localNumber) {
        derivedStateOf {
            if (localNumber.isNotBlank() && !phoneLooksValidLB(localNumber)) {
                "Please enter a valid Lebanese number"
            } else {
                ""
            }
        }
    }

    val filteredCountries = remember(searchQuery, userStartedTyping, searchableCountries) {
        if (!userStartedTyping) {
            searchableCountries
        } else {
            searchableCountries.filter { option ->
                option.name.contains(searchQuery, ignoreCase = true) ||
                        option.code2.contains(searchQuery, ignoreCase = true) ||
                        option.code3.contains(searchQuery, ignoreCase = true) ||
                        option.dialCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun sendOtp() {
        if (sendingOtp || !phoneLooksValidLB(localNumber) || configModelObject == null) return

        sendingOtp = true
        requestError = ""

        val req = RequestOtpModel(
            token = e164Phone,
            inputType = "PhoneNumberWithOtp",
            otpSize = otpSize,
            otpType = otpType,
            otpExpiryTime = contextAwareSigningModel.data.otpExpiryTime ?: 1.0
        )

        OtpHelper.requestOtp(configModelObject, req) { success ->
            sendingOtp = false

            if (success) {
                isOtpStep = true
                otp = ""
            } else {
                requestError = "Failed to send OTP. Please try again."
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!isOtpStep) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(0.38f)
                        .height(55.dp)
                        .clickable {
                            searchQuery = ""
                            userStartedTyping = false
                            showCountryDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = BaseTheme.FieldColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$countryFlag $countryDial",
                            color = BaseTheme.BaseTextColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Choose code",
                            tint = BaseTheme.BaseTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                TextField(
                    value = localNumber,
                    onValueChange = {
                        val onlyDigits = it.filter(Char::isDigit)
                        localNumber = onlyDigits.take(8)
                        requestError = ""
                        onValueChange(buildLebanonE164(localNumber, countryDial))
                    },
                    singleLine = true,
                    placeholder = {
                        Text(title)
                    },
                    trailingIcon = {
                        if (phoneLooksValidLB(localNumber)) {
                            Box(
                                modifier = Modifier.background(
                                    Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = { sendOtp() },
                                    enabled = phoneLooksValidLB(localNumber) && !sendingOtp
                                ) {
                                    if (sendingOtp) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = BaseTheme.BaseTextColor,

                                        )
                                    } else {
                                        Text(
                                            "Send OTP",
                                            color = BaseTheme.BaseTextColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BaseTheme.FieldColor,
                        unfocusedContainerColor = BaseTheme.FieldColor,
                        cursorColor = BaseTheme.BaseTextColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = BaseTheme.BaseTextColor,
                        unfocusedTextColor = BaseTheme.BaseTextColor,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(0.62f)
                        .height(55.dp)
                )
            }
        } else {
            TextField(
                value = if (isVerified) e164Phone else otp,
                readOnly = isVerified,
                onValueChange = { raw ->
                    if (!isVerified && !verifying) {
                        requestError = ""

                        val filtered = filterByOtpType(raw, otpType).take(otpSize)
                        otp = filtered

                        if (filtered.length == otpSize && otpMatchesType(filtered, otpType)) {
                            verifying = true

                            val verifyReq = VerifyOtpRequestOtpModel(
                                token = e164Phone,
                                otp = otp,
                                otpExpiryTime = contextAwareSigningModel.data.otpExpiryTime ?: 1.0
                            )

                            OtpHelper.verifyOtp(configModelObject!!, verifyReq) { success ->
                                verifying = false
                                isVerified = success

                                if (success) {
                                    requestError = ""
                                    onValid()
                                } else {
                                    requestError = "Invalid OTP. Please try again."
                                }
                            }
                        }
                    }
                },
                singleLine = true,
                placeholder = { Text("OTP ($otpSize)", color = BaseTheme.BaseTextColor) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (otpType) {
                        1 -> KeyboardType.Number
                        2 -> KeyboardType.Ascii
                        3 -> KeyboardType.Text
                        else -> KeyboardType.Ascii
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BaseTheme.FieldColor,
                    unfocusedContainerColor = BaseTheme.FieldColor,
                    cursorColor = BaseTheme.BaseTextColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = BaseTheme.BaseTextColor,
                    unfocusedTextColor = BaseTheme.BaseTextColor,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            )

            Spacer(Modifier.height(6.dp))

            if (isVerified) {
                Text(
                    "Verified successfully",
                    color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                    fontSize = 12.sp
                )
            } else {
                when {
                    verifying -> {
                        Text(
                            "Otp verifying ...",
                            color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                            fontSize = 12.sp
                        )
                    }

                    sendingOtp -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = BaseTheme.BaseTextColor,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sending OTP...",
                                color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                fontSize = 12.sp
                            )
                        }
                    }

                    else -> {
                        ResendOtpControl(
                            expiryMinutes = contextAwareSigningModel.data.otpExpiryTime ?: 1.0,
                            onResend = {
                                sendOtp()
                            }
                        )
                    }
                }
            }
        }

        if (requestError.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                requestError,
                color = BaseTheme.BaseRedColor,
                fontSize = 12.sp
            )
        }

        if (errToShow.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                errToShow,
                color = BaseTheme.BaseRedColor,
                fontSize = 12.sp
            )
        }
    }

    if (showCountryDialog) {
        Dialog(
            onDismissRequest = {
                showCountryDialog = false
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
                            .height(220.dp)
                    ) {
                        if (filteredCountries.isEmpty()) {
                            item {
                                Text(
                                    text = "No results found",
                                    color = BaseTheme.BaseTextColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(filteredCountries) { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showCountryDialog = false
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
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = option.name,
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

/* ---------- Phone helpers (Lebanon) ---------- */

private fun phoneLooksValidLB(local: String): Boolean {
    val digits = local.filter(Char::isDigit)
    val pattern = Regex("^(03|70|71|76|78|79|81)\\d{6}$")
    return pattern.matches(digits)
}

private fun buildLebanonE164(local: String, countryDial: String = "+961"): String {
    val digits = local.filter(Char::isDigit)
    val normalized = if (digits.startsWith("0")) digits.drop(1) else digits
    return countryDial + normalized
}

/* ---------- OTP helpers ---------- */

private fun otpMatchesType(value: String, type: Int): Boolean = when (type) {
    1 -> value.all { it.isDigit() }
    2 -> value.all { it.isLetterOrDigit() }
    3 -> value.all { it.isLetter() }
    else -> true
}

private fun filterByOtpType(raw: String, type: Int): String = when (type) {
    1 -> raw.filter { it.isDigit() }
    2 -> raw.filter { it.isLetterOrDigit() }
    3 -> raw.filter { it.isLetter() }
    else -> raw
}