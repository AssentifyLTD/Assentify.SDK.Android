package com.assentify.sdk.Flow.ContextAwareStep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.ResendOtpControl
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.OtpHelper
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpRequestOtpModel

@Composable
fun SigningEmailWithOtp(
    title: String,
    contextAwareSigningModel: ContextAwareSigningModel,
    onValueChange: (String) -> Unit,
    onValid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configModelObject = ConfigModelObject.getConfigModelObject()

    var email by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var sendingOtp by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf("") }

    val otpSize = contextAwareSigningModel.data.otpSize
    val otpType = contextAwareSigningModel.data.otpType
    var otp by remember { mutableStateOf("") }

    var isOtpStep by remember { mutableStateOf(false) }

    val errToShow by remember(email) {
        derivedStateOf {
            when {
                email.isNotBlank() && !emailLooksValid(email) -> "Please enter a valid email address"
                else -> ""
            }
        }
    }

    fun sendOtp() {
        if (sendingOtp || !emailLooksValid(email) || configModelObject == null) return

        sendingOtp = true
        requestError = ""

        val requestOtpModel = RequestOtpModel(
            token = email.trim(),
            inputType = "EmailWithOtp",
            otpSize = contextAwareSigningModel.data.otpSize ?: 8,
            otpType = contextAwareSigningModel.data.otpType ?: 1,
            otpExpiryTime = contextAwareSigningModel.data.otpExpiryTime ?: 1.0
        )

        OtpHelper.requestOtp(configModelObject, requestOtpModel) { success ->
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
            TextField(
                value = email,
                onValueChange = {
                    email = it
                    requestError = ""
                    onValueChange(it)
                },
                singleLine = true,
                placeholder = {
                    Text(title)
                },
                trailingIcon = {
                    if (emailLooksValid(email)) {
                        Box(
                            modifier = Modifier.background(
                                Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                shape = RoundedCornerShape(12.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { sendOtp() },
                                enabled = !sendingOtp && emailLooksValid(email)
                            ) {
                                if (sendingOtp) {
                                    Box(
                                        modifier = Modifier.size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = BaseTheme.BaseTextColor,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .scale(1f)
                                        )
                                    }
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
                    keyboardType = KeyboardType.Email
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
        } else {
            TextField(
                value = if (isVerified) email else otp,
                readOnly = isVerified,
                onValueChange = { raw ->
                    if (!isVerified && !verifying) {
                        requestError = ""

                        val filtered = filterByOtpType(raw, otpType ?: 1)
                            .take(otpSize ?: 8)

                        otp = filtered

                        if (filtered.length == (otpSize ?: 8) && otpMatchesType(filtered, otpType ?: 1)) {
                            verifying = true

                            val verifyOtpRequestOtpModel = VerifyOtpRequestOtpModel(
                                token = email.trim(),
                                otp = otp,
                                otpExpiryTime = contextAwareSigningModel.data.otpExpiryTime ?: 1.0
                            )

                            OtpHelper.verifyOtp(configModelObject!!, verifyOtpRequestOtpModel) { success ->
                                verifying = false

                                if (success) {
                                    isVerified = true
                                    requestError = ""
                                    onValid()
                                } else {
                                    isVerified = false
                                    requestError = "Invalid OTP. Please try again."
                                }
                            }
                        }
                    }
                },
                singleLine = true,
                placeholder = {
                    Text("OTP (${otpSize ?: 8})", color = BaseTheme.BaseTextColor)
                },
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
                    cursorColor = BaseTheme.FieldColor,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isVerified) {
                    Text(
                        "Verified successfully",
                        color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                        fontSize = 12.sp
                    )
                } else {
                    if (verifying) {
                        Text(
                            "Otp verifying ...",
                            color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                            fontSize = 12.sp
                        )
                    } else if (sendingOtp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(14.dp),
                                strokeWidth = 2.dp,
                                color = BaseTheme.BaseTextColor,
                            )
                            Spacer(modifier = Modifier.height(0.dp))
                            Text(
                                "Sending OTP...",
                                color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        ResendOtpControl(
                            expiryMinutes = contextAwareSigningModel.data.otpExpiryTime!!,
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
}

/* ---------- Helpers ---------- */

private val emailRegex = Regex(
    "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
    RegexOption.IGNORE_CASE
)

private fun emailLooksValid(s: String): Boolean {
    val x = s.trim()
    if (!(x.contains('@') && x.contains('.') && x.length >= 5)) {
        return false
    }
    return emailRegex.matches(x)
}

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