package com.assentify.sdk.Flow.FaceStep

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.SecureImage
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun FaceResultScreen(
    faceModel: FaceResponseModel,
    onNext: () -> Unit = {},
    onRetry: () -> Unit = {},
    onIDChange: () -> Unit = {},
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    val match = faceModel.faceExtractedModel?.percentageMatch ?: 0
    val baseImage = faceModel.faceExtractedModel?.baseImageFace.orEmpty()
    val secondImage = faceModel.faceExtractedModel?.secondImageFace.orEmpty()

    val title =
        if (match > 50) "Verification Successful" else "Verification UnSuccessful"
    val subTitle =
        if (match > 50) "We have been able to make sure its you!"
        else "Your face did not match with the provided document. Provide another document or confirm."

    val borderColor = when {
        match!! > 50 -> BaseTheme.BaseGreenColor
        match!! > 30 ->   Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
        else -> BaseTheme.BaseRedColor
    }
    BaseBackgroundContainer(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()              // respect status bar
            .padding(horizontal = 24.dp)      // side padding like the mock
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // TOP + MIDDLE CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(150.dp))

            // TOP
            Text(
                text = title,
                color =   BaseTheme.BaseTextColor,
                fontSize = 25.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = subTitle,
                color =   BaseTheme.BaseTextColor,
                fontSize = 10.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Light,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // MIDDLE
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val imageWidth = 140.dp
                    val imageHeight = 160.dp

                    Box(
                        modifier = Modifier
                            .width(imageWidth)
                            .height(imageHeight)
                            .border(1.dp, borderColor, RoundedCornerShape(0.dp))
                    ) {
                        SecureImage(
                            imageUrl = baseImage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(imageWidth)
                            .height(imageHeight)
                            .border(1.dp, borderColor, RoundedCornerShape(0.dp))
                    ) {
                        SecureImage(
                            imageUrl = secondImage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(25.dp))

                MatchProgress(
                    percentage = match,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Push content up, leave room for the bottom buttons
            Spacer(Modifier.weight(1f))
        }

        // BOTTOM ACTIONS
        when {
            match > 50 -> {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)  .background(
                            brush = BaseTheme.BaseClickColor!!.toBrush(),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Text("Next",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        color =  BaseTheme.BaseSecondaryTextColor,
                        modifier = Modifier.padding(vertical = 7.dp))
                }
            }

            else -> {
                when {
                    match > 30 -> {
                        val corner = RoundedCornerShape(999.dp)

                        Button(
                            onClick = onRetry,
                            shape = corner,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)  .background(
                                    brush = BaseTheme.BaseClickColor!!.toBrush(),
                                    shape = RoundedCornerShape(28.dp)
                                ) // consistent height
                        ) {
                            Text("Retry",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                color =  BaseTheme.BaseSecondaryTextColor,
                                modifier = Modifier.padding(vertical = 7.dp))
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = corner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.dp, Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)), corner)
                        ) {
                            Text("Confirm & Proceed",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                color =  Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                modifier = Modifier.padding(vertical = 7.dp))
                        }
                    }

                    else -> {
                        val corner = RoundedCornerShape(999.dp)

                        Button(
                            onClick = onIDChange,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = corner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .background(
                                    brush = BaseTheme.BaseClickColor!!.toBrush(),
                                    shape = RoundedCornerShape(28.dp)
                                )
                        ) {
                            Text("Provide Supporting ID to match with",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                color =  BaseTheme.BaseSecondaryTextColor,
                                modifier = Modifier.padding(vertical = 7.dp))
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                            shape = corner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.dp, BaseTheme.BaseRedColor, corner)
                        ) {
                            Text("Override & Proceed",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                color =  BaseTheme.BaseRedColor,
                                modifier = Modifier.padding(vertical = 7.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding())
    }}

}


@Composable
fun MatchProgress(
    percentage: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 2.dp,
) {
    val pct = (percentage ?: 0).coerceIn(0, 100)
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val trackColor =   BaseTheme.BaseTextColor

    val progressColor = when {
        percentage!! > 50 -> BaseTheme.BaseGreenColor
        percentage!! > 30 ->Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
        else -> BaseTheme.BaseRedColor
    }
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            val arcInset = stroke.width / 2f
            val arcSize = Size(
                width = this.size.width - stroke.width,
                height = this.size.height - stroke.width
            )

            // background circle
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(arcInset, arcInset),
                size = arcSize,
                style = stroke
            )

            // progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * (pct / 100f),
                useCenter = false,
                topLeft = Offset(arcInset, arcInset),
                size = arcSize,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$pct%",
                color = progressColor,
                fontSize = 15.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "Match",
                color = progressColor,
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            )
        }
    }
}

