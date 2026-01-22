package com.assentify.sdk.Flow.Terms


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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.Events.TermsAndConditionsEventTypes
import com.assentify.sdk.Flow.ReusableComposable.PdfViewerFromUrl
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.RemoteClient.Models.TermsConditionsModel


@Composable
fun TermsAndConditionsScreen(
    onBack: () -> Unit,
    onAccept: (Boolean) -> Unit,
    onDecline: () -> Unit,
    termsConditionsModel: TermsConditionsModel?,
    termsAndConditionsEventTypes: String,
    modifier: Modifier = Modifier
) {
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }



    BaseBackgroundContainer(
        modifier = modifier
            .fillMaxSize()

    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP + MIDDLE AREA
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                // TOP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint =   BaseTheme.BaseTextColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(BaseTheme.BaseLogo)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.CenterVertically),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(Modifier.height(10.dp))

                ProgressStepper(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 0.dp)
                )


                // MIDDLE (content area fills remaining space)
                when (termsAndConditionsEventTypes) {
                    TermsAndConditionsEventTypes.onSend -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f), // center loader in available middle space
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color =   BaseTheme.BaseTextColor,
                                strokeWidth = 6.dp
                            )
                        }
                    }

                    TermsAndConditionsEventTypes.onHasData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize().padding(horizontal = 10.dp)

                        ) {
                            Text(
                                text = termsConditionsModel!!.data.header!!,
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Bold,
                                color =   BaseTheme.BaseTextColor,
                                fontSize = 23.sp,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp, start = 20.dp, end = 20.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(
                                thickness = 1.dp,
                                color =   BaseTheme.BaseTextColor,
                            )
                            // MIDDLE PDF VIEW – takes all remaining height in this area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                                    .background(Color.Transparent)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                PdfViewerFromUrl(
                                    url = termsConditionsModel.data.file!!,
                                    fileName = "TermsConditions.pdf",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.Gray)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM – always at end of screen when we have data
            if (termsAndConditionsEventTypes == TermsAndConditionsEventTypes.onHasData) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 25.dp, horizontal = 25.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        15.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (termsConditionsModel!!.data.confirmationRequired == true) {
                                onDecline()
                            } else {
                                onAccept(false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                            .border(
                                1.dp,
                                Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                RoundedCornerShape(999.dp)
                            )
                    ) {
                        Text("Decline",
                            fontFamily = InterFont,
                            color =  Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                    Button(
                        onClick = {
                            onAccept(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp).background(
                                brush = BaseTheme.BaseClickColor!!.toBrush(),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Text(
                            termsConditionsModel!!.data.nextButtonTitle!!,
                            fontFamily = InterFont,
                            color =  BaseTheme.BaseSecondaryTextColor,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }

}

