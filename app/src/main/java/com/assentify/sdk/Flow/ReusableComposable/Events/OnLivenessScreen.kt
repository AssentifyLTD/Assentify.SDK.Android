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
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.SecureImage
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun OnLivenessScreen(
    imageUrl: String,
    onRetry: () -> Unit = {},
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val context = LocalContext.current

    val iconSvg = remember {
        loadSvgFromAssets(context, "ic_error.svg")
    }
    BaseBackgroundContainer(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(150.dp))
        // TOP + MIDDLE (dynamic / centered content)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Small top spacing to breathe (replaces rigid 150.dp)
            Spacer(Modifier.height(32.dp))

            // MIDDLE IMAGE + ICON
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
                        contentDescription = "ic_error",
                        modifier = Modifier.size(60.dp),
                        tint = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                    )
                }
            }

            Spacer(Modifier.height(25.dp))

            Text(
                text = "Seems like you didn't provide a real ID",
                color = BaseTheme.BaseTextColor,
                fontSize = 20.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(15.dp))

            Text(
                text = "Make sure its one of the above IDs presented and allowed by NXT Finance to verify your identity.",
                color = BaseTheme.BaseTextColor,
                fontSize = 10.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Thin,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Push content upward, keeping the bottom button pinned
            Spacer(modifier = Modifier.weight(1f))
        }

        // BOTTOM BUTTON (always pinned)
        Button(
            onClick = onRetry,

            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .background(
                    brush = BaseTheme.BaseClickColor!!.toBrush(),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Text(
                "Retry",
                color = BaseTheme.BaseSecondaryTextColor,
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(vertical = 7.dp)
            )
        }
    }
}

}
