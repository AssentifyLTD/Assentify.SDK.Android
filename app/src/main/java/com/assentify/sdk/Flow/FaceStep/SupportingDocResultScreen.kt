package com.assentify.sdk.Flow.FaceStep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.SecureImage
import com.assentify.sdk.RemoteClient.Models.CompareFacesResponse
import com.assentify.sdk.RemoteClient.Models.IdentityMatchResponse


@Composable
fun SupportingDocResultScreen(
    selfieImage: String,
    supportingDocImage: String,
    faceResult: CompareFacesResponse?,
    identityResult: IdentityMatchResponse?,
    onNext: (isSkip: Boolean) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val faceCurrentStep = FlowController.getCurrentStep()!!.stepDefinition!!.customization
    val s = flowStrings()

    val threshold = faceCurrentStep.faceMatchPassThreshold ?: 60
    val allowSkip = faceCurrentStep.supportingDocumentAllowSkip == true

    val match = faceResult?.matchScore ?: 0
    val isFaceMatched = match >= threshold
    val isSamePerson = identityResult?.samePerson == true
    val isPassed = isFaceMatched && isSamePerson

    val title = if (isPassed) s.verificationSuccess else s.verificationFail
    val subTitle = if (isPassed) s.verificationSuccessMsg else s.verificationFailMsg
    val borderColor = if (isPassed) BaseTheme.BaseGreenColor else BaseTheme.BaseRedColor
    val accentColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))

    // Message under the progress circle, one per status
    val statusMessage = when {
        isPassed -> s.supportingDocPassedMsg
        !isFaceMatched && isSamePerson -> s.supportingDocFaceNoMatchMsg
        isFaceMatched && !isSamePerson -> s.supportingDocIdentityMismatchMsg
        else -> s.supportingDocBothFailedMsg
    }

    BaseBackgroundContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // TOP + MIDDLE (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(140.dp))

                Text(
                    text = title,
                    color = BaseTheme.BaseTextColor,
                    fontSize = 25.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = subTitle,
                    color = BaseTheme.BaseTextColor,
                    fontSize = 10.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Light,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(selfieImage, supportingDocImage).forEach { image ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(140.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(0.dp))
                        ) {
                            SecureImage(
                                imageUrl = image,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(25.dp))

                MatchProgress(
                    percentage = match,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    progressColor = borderColor
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = statusMessage,
                    color = BaseTheme.BaseTextColor,
                    fontSize = 13.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(24.dp))
            }

            // BOTTOM ACTIONS (always visible)
            Spacer(Modifier.height(12.dp))

            if (isPassed) {
                // 1) Score >= threshold AND same person
                ResultButton(text = s.next, color = BaseTheme.BaseGreenColor) { onNext(false) }
            } else {
                // 2) Score below threshold OR not same person
                ResultButton(text = s.retry, color = BaseTheme.BaseRedColor, onClick = onRetry)

                // 3) Same failure, but skipping is allowed
                if (allowSkip) {
                    Spacer(Modifier.height(8.dp))
                    ResultButton(text = s.skip, color = accentColor) { onNext(true) }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun ResultButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(color = color, shape = RoundedCornerShape(28.dp))
    ) {
        Text(
            text,
            fontFamily = InterFont,
            fontWeight = BaseTheme.BaseClickFontWeight,
            color = BaseTheme.BaseSecondaryTextColor,
            modifier = Modifier.padding(vertical = 7.dp)
        )
    }
}