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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun SecureEmailWithOtpField(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    onValid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val configModelObject = ConfigModelObject.getConfigModelObject()


    var email by remember(field.inputKey) { mutableStateOf("") }
    var isVerified by remember() { mutableStateOf(false) }
    var verifying by remember() { mutableStateOf(false) }

    val otpSize = field.otpSize
    val otpType = field.otpType
    var otp by remember(field.inputKey) { mutableStateOf("") }

    // which step are we on?
    var isOtpStep by remember(field.inputKey) { mutableStateOf(false) }

    val errToShow by remember {
        derivedStateOf {
            if (email.isNotBlank() && !emailLooksValid(email)) {
                "Please enter a valid email address"
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

            TextField(
                value = email,
                onValueChange = {
                    email = it
                    onValueChange(it)
                },
                singleLine = true,
                trailingIcon = {
                    if (emailLooksValid(email))
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            TextButton(
                                onClick = {
                                    val requestOtpModel = RequestOtpModel(
                                        token = email.trim(),
                                        inputType = field.inputType,
                                        otpSize = field.otpSize ?: 6,
                                        otpType = field.otpType ?: 1,
                                        otpExpiryTime = field.otpExpiryTime ?: 1.0   //
                                    )
                                    OtpHelper.requestOtp(configModelObject, requestOtpModel) { success ->
                                        if (success) {
                                            isOtpStep = true
                                            otp = ""
                                        } else {
                                            // show error
                                        }
                                    }

                                },
                                enabled = emailLooksValid(email)
                            ) {
                                Text(
                                    "Send OTP",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                    unfocusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                    cursorColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            )
        } else {
            TextField(
                value = if(isVerified) email else otp,
                readOnly = isVerified,
                onValueChange = { raw ->
                    if(!isVerified){
                        val filtered = filterByOtpType(raw, otpType!!)
                            .take(otpSize!!)
                            .let { if (otpType == 2 || otpType == 3) it else it }
                        otp = filtered
                        if (filtered.length == otpSize && otpMatchesType(filtered, otpType!!)) {
                            verifying = true;
                            val verifyOtpRequestOtpModel = VerifyOtpRequestOtpModel(
                                token = email.trim(),
                                otp = otp,
                                otpExpiryTime = field.otpExpiryTime ?: 1.0
                            )
                            OtpHelper.verifyOtp(configModelObject, verifyOtpRequestOtpModel) { success ->
                                if (success) {
                                    verifying = false;
                                    isVerified = true;
                                    onValid();
                                } else {
                                    verifying = false;
                                    isVerified = false;
                                }
                            }

                        }
                    }


                },
                singleLine = true,
                placeholder = { Text("OTP ($otpSize)", color = Color.White) },
                keyboardOptions = KeyboardOptions(
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
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if(isVerified){
                        Text(
                            "Verified successfully",
                            color = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                            fontSize = 12.sp
                        )
                    }else{

                        if(!verifying){
                            ResendOtpControl(
                                expiryMinutes = field.otpExpiryTime!!,
                                onResend = {
                                    val requestOtpModel = RequestOtpModel(
                                        token = email.trim(),
                                        inputType = field.inputType,
                                        otpSize = field.otpSize ?: 6,
                                        otpType = field.otpType ?: 1,
                                        otpExpiryTime = field.otpExpiryTime ?: 1.0   //
                                    )
                                    OtpHelper.requestOtp(configModelObject, requestOtpModel) { success ->
                                        if (success) {
                                            isOtpStep = true
                                            otp = ""
                                        } else {
                                            // show error
                                        }
                                    }
                                }
                            )
                        }

                    }

                }

            }


        }

        if(verifying){
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
                color = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                fontSize = 12.sp
            )
        }
    }


}


/* ---------- Helpers ---------- */

private fun emailLooksValid(s: String): Boolean {
    val x = s.trim()
    // Simple & fast email check good for UI gating; keep your server-side validation as source of truth
    return x.contains('@') && x.contains('.') && x.length >= 5
}

private fun otpMatchesType(value: String, type: Int): Boolean = when (type) {
    1 -> value.all { it.isDigit() }                          // numeric
    2 -> value.all { it.isLetterOrDigit() }                  // alphanumeric
    3 -> value.all { it.isLetter() }                         // letters only
    else -> true
}

private fun filterByOtpType(raw: String, type: Int): String = when (type) {
    1 -> raw.filter { it.isDigit() }
    2 -> raw.filter { it.isLetterOrDigit() }
    3 -> raw.filter { it.isLetter() }
    else -> raw
}


@Composable
fun ResendOtpControl(
    expiryMinutes: Double,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    enabledTextColor: Color = Color.White,
    disabledTextColor: Color = Color.White.copy(alpha = 0.7f)
) {
    val totalMs = (expiryMinutes * 60_000).toLong().coerceAtLeast(1_000L)
    var remainingMs by remember(expiryMinutes) { mutableStateOf(totalMs) }
    var ticking by remember { mutableStateOf(true) }

    LaunchedEffect(remainingMs, ticking) {
        if (!ticking) return@LaunchedEffect
        if (remainingMs > 0) {
            kotlinx.coroutines.delay(1_000)
            remainingMs = (remainingMs - 1_000).coerceAtLeast(0)
        }
    }

    LaunchedEffect(remainingMs) {
        if (remainingMs <= 0) ticking = false
    }

    val minutes = (remainingMs / 1000) / 60
    val seconds = (remainingMs / 1000) % 60
    val countdownLabel = String.format("%d:%02d", minutes, seconds)
    val canResend = remainingMs <= 0

    TextButton(
        onClick = {
            onResend()

            remainingMs = totalMs
            ticking = true
        },
        enabled = canResend,
        modifier = modifier
    ) {
        Text(
            text = if (canResend) "Resend OTP" else "Resend in $countdownLabel",
            color = if (canResend) enabledTextColor else disabledTextColor
        )
    }
}
