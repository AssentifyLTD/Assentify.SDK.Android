package com.assentify.sdk.Flow.Terms


import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    logoBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(Modifier.height(10.dp))

                ProgressStepper(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )

                Spacer(Modifier.height(12.dp))
                when (termsAndConditionsEventTypes) {
                    TermsAndConditionsEventTypes.onSend -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(), // keep centered during scroll
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = Color.White,
                                strokeWidth = 6.dp
                            )
                        }
                    }

                    TermsAndConditionsEventTypes.onHasData -> {
                        Text(
                            text = termsConditionsModel!!.data.header!!,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                // .weight(1f)
                                .fillMaxWidth()
                                .fillMaxWidth()
                                .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                                .background(Color.Transparent)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            PdfViewerFromUrl(
                                url = termsConditionsModel.data.file!!,
                                fileName = "TermsConditions.pdf",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                15.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                          if(termsConditionsModel.data.confirmationRequired!!){
                                              onDecline()
                                          }else{
                                              onAccept(false)
                                          }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
                                ),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .border(
                                        1.dp,
                                        Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                                        RoundedCornerShape(999.dp)
                                    )
                            ) {
                                Text("Decline", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            }
                            Button(
                                onClick = {
                                    onAccept(true)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor)),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                            ) {
                                Text(
                                    termsConditionsModel.data.nextButtonTitle!!,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                    }
                }
            }


        }
    }
}

