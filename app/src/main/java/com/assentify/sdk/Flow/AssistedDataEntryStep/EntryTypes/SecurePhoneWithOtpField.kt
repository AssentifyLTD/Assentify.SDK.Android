package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.AssistedDataEntryStep.FieldsControllers.OtpHelper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.RemoteClient.Models.RequestOtpModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpRequestOtpModel

@Composable
fun SecurePhoneWithOtpField(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    onValid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val configModelObject = ConfigModelObject.getConfigModelObject()

    // Lebanon constants
    val countryFlag = "🇱🇧"
    val countryIso2 = "LB"
    val countryDial = "+961"

    var localNumber by remember(field.inputKey) { mutableStateOf("") }   // user types here (no country code)
    var isVerified by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    val otpSize = field.otpSize ?: 6
    val otpType = field.otpType ?: 1
    var otp by remember(field.inputKey) { mutableStateOf("") }

    var isOtpStep by remember(field.inputKey) { mutableStateOf(false) }

    // Compose the full E.164 phone for server
    val e164Phone by remember(localNumber) {
        mutableStateOf(buildLebanonE164(localNumber, countryDial))
    }

    val errToShow by remember(localNumber) {
        derivedStateOf {
            if (localNumber.isNotBlank() && !phoneLooksValidLB(localNumber)) {
                "Please enter a valid Lebanese number"
            } else ""
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (!isOtpStep || isVerified) title else "Enter OTP",
            color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        if (!isOtpStep) {
            // PHONE INPUT (LB only – shows flag and +961)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = "$countryFlag $countryDial",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                        unfocusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),
                        unfocusedTextColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor))
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(0.38f)
                        .height(55.dp)
                )

                TextField(
                    value = localNumber,
                    onValueChange = {
                        val onlyDigits = it.filter(Char::isDigit)
                        localNumber = onlyDigits.take(8) // LB local lengths are typically 7–8
                        onValueChange(buildLebanonE164(localNumber, countryDial))
                    },
                    singleLine = true,
                    placeholder = { Text("", color = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor))) },
                    trailingIcon = {
                        if (phoneLooksValidLB(localNumber))
                            Box(
                                modifier = Modifier.background(
                                    Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ) {
                                TextButton(
                                    onClick = {
                                        val req = RequestOtpModel(
                                            token = e164Phone,                   // send E.164 to backend
                                            inputType = field.inputType,
                                            otpSize = otpSize,
                                            otpType = otpType,
                                            otpExpiryTime = field.otpExpiryTime ?: 1.0
                                        )
                                        OtpHelper.requestOtp(configModelObject, req) { success ->
                                            if (success) {
                                                isOtpStep = true
                                                otp = ""
                                            } else {
                                                // TODO: show error UI
                                            }
                                        }
                                    },
                                    enabled = phoneLooksValidLB(localNumber)
                                ) {
                                    Text(
                                        "Send OTP",
                                        color = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextSelectedHexColor)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                        unfocusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                        cursorColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),
                        unfocusedTextColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),
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
            // OTP INPUT
            TextField(
                value = if (isVerified) e164Phone else otp,
                readOnly = isVerified,
                onValueChange = { raw ->
                    if (!isVerified) {
                        val filtered = filterByOtpType(raw, otpType)
                            .take(otpSize)
                            .let { if (otpType == 2 || otpType == 3) it else it }
                        otp = filtered
                        if (filtered.length == otpSize && otpMatchesType(filtered, otpType)) {
                            verifying = true
                            val verifyReq = VerifyOtpRequestOtpModel(
                                token = e164Phone,
                                otp = otp,
                                otpExpiryTime = field.otpExpiryTime ?: 1.0
                            )
                            OtpHelper.verifyOtp(configModelObject, verifyReq) { success ->
                                verifying = false
                                isVerified = success
                                if (success) onValid()
                            }
                        }
                    }
                },
                singleLine = true,
                placeholder = { Text("OTP ($otpSize)", color = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor))) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = when (otpType) {
                        1 -> KeyboardType.Number
                        2 -> KeyboardType.Ascii
                        3 -> KeyboardType.Text
                        else -> KeyboardType.Ascii
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                    unfocusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                    cursorColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),
                    unfocusedTextColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),
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
                    color = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                    fontSize = 12.sp
                )
            } else {
                if (!verifying) {
                    ResendOtpControl(
                        expiryMinutes = field.otpExpiryTime ?: 1.0,
                        onResend = {
                            val req = RequestOtpModel(
                                token = e164Phone,
                                inputType = field.inputType,
                                otpSize = otpSize,
                                otpType = otpType,
                                otpExpiryTime = field.otpExpiryTime ?: 1.0
                            )
                            OtpHelper.requestOtp(configModelObject, req) { success ->
                                if (success) {
                                    isOtpStep = true
                                    otp = ""
                                } else {
                                    // TODO: show error UI
                                }
                            }
                        }
                    )
                }
            }
        }

        if (verifying) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Otp verifying ...",
                color = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                fontSize = 12.sp
            )
        }
        if (errToShow.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                errToShow,
                color = Color.Red,
                fontSize = 12.sp
            )
        }
    }
}

/* ---------- Phone helpers (Lebanon) ---------- */

// Accept 7–8 digits for LB after removing a single leading '0' if present
private fun phoneLooksValidLB(local: String): Boolean {
    val digits = local.filter(Char::isDigit)
    val normalized = if (digits.startsWith("0")) digits.drop(1) else digits
    return normalized.length in 7..8
}

// Build E.164: +961 + normalized local part (strip leading 0, digits only)
private fun buildLebanonE164(local: String, countryDial: String = "+961"): String {
    val digits = local.filter(Char::isDigit)
    val normalized = if (digits.startsWith("0")) digits.drop(1) else digits
    return countryDial + normalized
}

/* ---------- Reuse your OTP helpers from the email component ---------- */

private fun otpMatchesType(value: String, type: Int): Boolean = when (type) {
    1 -> value.all { it.isDigit() }           // numeric
    2 -> value.all { it.isLetterOrDigit() }   // alphanumeric
    3 -> value.all { it.isLetter() }          // letters only
    else -> true
}

private fun filterByOtpType(raw: String, type: Int): String = when (type) {
    1 -> raw.filter { it.isDigit() }
    2 -> raw.filter { it.isLetterOrDigit() }
    3 -> raw.filter { it.isLetter() }
    else -> raw
}
