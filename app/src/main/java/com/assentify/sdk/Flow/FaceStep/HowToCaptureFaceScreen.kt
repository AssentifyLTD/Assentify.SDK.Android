package com.assentify.sdk.Flow.FaceStep

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.assentify.sdk.Base64ImageObject
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.VideoPlayerFromAssets
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToCaptureFaceScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }



    var base64Image by rememberSaveable { mutableStateOf<String?>(null) }


    val docUrl = FlowController.getPreviousIDImage()

    LaunchedEffect(docUrl) {
        base64Image = FlowController.downloadImageAsBase64(docUrl)
    }


    BaseBackgroundContainer(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                            tint = BaseTheme.BaseTextColor,
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
                    "Face Match",
                    color = BaseTheme.BaseTextColor,
                    fontSize = 25.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // MIDDLE – video area flexes with available space
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 10.dp)
                ) {
                    VideoPlayerFromAssets(
                        assetFileName = "face-video.mp4",
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Watch how easy it is\nto Take Selfie",
                    color = BaseTheme.BaseTextColor,
                    fontSize = 25.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "The selfie includes liveness capture to ensure your real follow the on screen instructions.",
                    color = BaseTheme.BaseTextColor,
                    fontSize = 12.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Thin,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // BOTTOM – pinned (loader OR button)
            if (base64Image == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = BaseTheme.BaseTextColor,
                        strokeWidth = 4.dp
                    )
                }
            } else {
                Button(
                    onClick = {
                        Base64ImageObject.clear()
                        Base64ImageObject.setImage(base64Image)
                        onNext()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),

                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                        .background(
                            brush = BaseTheme.BaseClickColor!!.toBrush(),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Text(
                        "Next",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        color = BaseTheme.BaseSecondaryTextColor,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }
    }

}