package com.assentify.sdk.Flow.ReusableComposable.Events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.ReusableComposable.GifPlayer
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun OnSendScreen(
    process: Int
) {
    val context = LocalContext.current
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    val iconUploading = remember {
        loadSvgFromAssets(context, "ic_uploading.svg")
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor))) // dark green background
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

       if(process == 100){
           GifPlayer("file:///android_asset/gif_processing.gif")
       }else{
           iconUploading?.let {
               Icon(
                   painter = it,
                   contentDescription = "Uploading",
                   modifier = Modifier.size(250.dp),
                   tint = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
               )
           }
       }

        Spacer(Modifier.height(32.dp))
        if(process != 100){
            Text(
                text = "$process%",
                color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)).copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            // 🔹 Progress bar
            LinearProgressIndicator(
                progress = { process / 100f },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                trackColor = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)).copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(12.dp))

        }


        if(process == 100){
            Text(
                text = "Processing ID please wait",
                color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }else{
            Text(
                text = "Uploading ID please wait",
                color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}
