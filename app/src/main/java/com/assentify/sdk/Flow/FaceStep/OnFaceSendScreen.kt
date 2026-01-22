package com.assentify.sdk.Flow.FaceStep

import ScanAnimation
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
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun OnFaceSendScreen(
    process: Int
) {
    val context = LocalContext.current
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    val iconUploading = remember {
        loadSvgFromAssets(context, "ic_uploading_face.svg")
    }

    BaseBackgroundContainer(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

       if(process == 100 || process == 99 ){
           ScanAnimation(
               sourceIconName = "ic_face_id.svg"
           );
       }else{
           iconUploading?.let {
               Icon(
                   painter = it,
                   contentDescription = "UploadingFace",
                   modifier = Modifier.size(150.dp),
                   tint = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
               )
           }
       }

        Spacer(Modifier.height(32.dp))
        if(process != 100 && process != 99){
            Text(
                text = "$process%",
                color =   BaseTheme.BaseTextColor.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(8.dp))
            // 🔹 Progress bar
            LinearProgressIndicator(
                progress = { process / 100f },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                trackColor =   BaseTheme.FieldColor
            )
            Spacer(Modifier.height(12.dp))

        }


        if(process == 100 || process == 99){
            Text(
                text = "Processing Face please wait",
                color =   BaseTheme.BaseTextColor,
                fontSize = 18.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }else{
            Text(
                text = "Uploading Face please wait",
                color =   BaseTheme.BaseTextColor,
                fontSize = 18.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

    }}
}
