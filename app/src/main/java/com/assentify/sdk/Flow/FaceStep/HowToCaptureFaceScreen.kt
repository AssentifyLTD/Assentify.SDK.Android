package com.assentify.sdk.Flow.FaceStep

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Base64ImageObject
import com.assentify.sdk.Flow.FlowController.FlowController
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

    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    var base64Image by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    /// SDK TODO

    var docUrl =
        "https://storagetestassentify.blob.core.windows.net/userfiles/b096e6ea-2a81-44cb-858e-08dbcbc01489/ca0162f9-8cfe-409f-91d8-9c2d42d53207/4f445a214f5a4b7fa74dc81243ccf590/b19c2053-efae-42e8-8696-177809043a9c/ReadPassport/image.jpeg"

  /*    val extractedInfo = FlowController.getPreviousIDScanStep()
             ?.submitRequestModel
             ?.extractedInformation

         extractedInfo?.forEach { (key, value) ->
             if (key.contains("OnBoardMe_IdentificationDocumentCapture_OriginalFrontImage", )) {
                 Log.d("MapSearch", "Key: $key, Value: $value")
                 docUrl = value;
             }
         }*/


    LaunchedEffect(docUrl) {
        base64Image = FlowController.downloadImageAsBase64(docUrl)
        isLoading = false
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
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
                            imageVector = Icons.Filled.ArrowBack,
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

                Text(
                    "Face Match",
                    color = Color.White,
                    fontSize = 25.sp,
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
                            .padding(horizontal = 20.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Watch how easy it is\nto Take Selfie",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "The selfie includes liveness capture to ensure your real follow the on screen instructions.",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // BOTTOM – pinned (loader OR button)
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Color.White,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor)),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Text(
                        "Next",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }

}