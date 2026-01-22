package com.assentify.sdk.Flow.IDStep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.assentify.sdk.Flow.ReusableComposable.VideoPlayerFromAssets
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.SelectedTemplatesObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToCaptureScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }
    val selectedTemplate = remember { SelectedTemplatesObject.getSelectedTemplatesObject() }



    val title = if (selectedTemplate.id == -1) "Present Your Passport" else "Present Your ID";
    val subTitle =
        if (selectedTemplate.id == -1) "Watch how easy it is to capture your Passport" else "Watch how easy it is to capture your ID";
    val assetVideoFileName =
        if (selectedTemplate.id == -1) "passport-video.mp4"
        else "id-video.mp4";


    BaseBackgroundContainer(
        modifier = modifier
            .fillMaxSize()
           // .padding(horizontal = 12.dp, vertical = 8.dp)
            .systemBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP + MIDDLE
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                // Top
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
                            tint =  BaseTheme.BaseTextColor,
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

                Text(
                    title,
                    color =   BaseTheme.BaseTextColor,
                    fontSize = 25.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // MIDDLE – video takes the flexible space
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 10.dp)
                ) {
                    VideoPlayerFromAssets(
                        assetFileName = assetVideoFileName,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    subTitle,
                    color =   BaseTheme.BaseTextColor,
                    fontSize = 25.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    "Just make sure to be in a well lit area with no direct light reflecting on the ID or Passport presented.",
                    color =   BaseTheme.BaseTextColor,
                    fontSize = 12.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Thin,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            // BOTTOM – pinned
            Button(
                onClick = {
                    onNext()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),

                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 25.dp, horizontal = 25.dp).background(
                        brush = BaseTheme.BaseClickColor!!.toBrush(),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Text(
                    "Lets Start",
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Normal,
                    color =BaseTheme.BaseSecondaryTextColor,
                    modifier = Modifier.padding(vertical = 7.dp)
                )
            }
        }
    }

}