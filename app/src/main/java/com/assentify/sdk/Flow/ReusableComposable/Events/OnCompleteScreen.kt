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
fun OnCompleteScreen(
    imageUrl: String,
    onNext: () -> Unit = {},
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val context = LocalContext.current

    val iconSvg= remember {
        loadSvgFromAssets(context, "ic_complete.svg")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
            .statusBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(150.dp))
        // TOP + MIDDLE CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Gentle spacing instead of rigid 150.dp
            Spacer(Modifier.height(32.dp))

            // MAIN IMAGE + ICON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                SecureImage(imageUrl = imageUrl)

                iconSvg?.let {
                    Icon(
                        painter = it,
                        contentDescription = "ic_complete",
                        modifier = Modifier.size(100.dp),
                        tint = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
                    )
                }
            }

            Spacer(Modifier.height(25.dp))

            Text(
                text = "ID Processed Successfully",
                color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Push content up and free space for bottom button
            Spacer(modifier = Modifier.weight(1f))
        }

        // BOTTOM BUTTON (always pinned)
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
