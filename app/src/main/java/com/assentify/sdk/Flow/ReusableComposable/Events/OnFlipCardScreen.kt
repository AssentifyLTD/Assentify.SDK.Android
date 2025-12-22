package com.assentify.sdk.Flow.ReusableComposable.Events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.ReusableComposable.SecureImage
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun OnFlipCardScreen(
    expectedImageUrl: String,
    onNext: () -> Unit = {},
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val context = LocalContext.current

    val iconSvg= remember {
        loadSvgFromAssets(context, "ic_flip_card.svg")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
            .statusBarsPadding() // safe top
            .padding(horizontal = 32.dp, vertical = 24.dp), // general page padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(150.dp))
        // TOP + MIDDLE
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // small breathing space instead of 150.dp


            // MIDDLE
            Text(
                text = "Capture Back of ID",
                color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            iconSvg?.let {
                Icon(
                    painter = it,
                    contentDescription = "ic_flip_card",
                    modifier = Modifier.size(150.dp),
                    tint = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
                )
            }

            Spacer(Modifier.height(25.dp))

            Text(
                text = "Please flip the card provided to take the back of the card",
                color =Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (expectedImageUrl.isNotEmpty()) {
                Spacer(Modifier.height(30.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SecureImage(imageUrl = expectedImageUrl)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Expected Card Type",
                    color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                      lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(30.dp))
            }

            // push content up, leave space for bottom button
            Spacer(modifier = Modifier.weight(1f))
        }

        // BOTTOM
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor)),
                contentColor = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                "Next",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }


}
