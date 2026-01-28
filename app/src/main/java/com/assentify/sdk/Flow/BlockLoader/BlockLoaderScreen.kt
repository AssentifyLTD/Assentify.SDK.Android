package com.assentify.sdk.Flow.BlockLoader


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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun BlockLoaderScreen(
    steps: List<LocalStepModel>,
    onBack: () -> Unit,
    onStepClick: (LocalStepModel) -> Unit,
    onNext: () -> Unit
) {
    val ctx = LocalContext.current
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    MaterialTheme(colorScheme = darkColorScheme()) {
        BaseBackgroundContainer(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // ======= TOP + MIDDLE (HEADER + STEPS LIST) =======
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // take all space between top and bottom button
                ) {

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
                        // centered logo visually by Row spacer balance
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(BaseTheme.BaseLogo)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // place-holder to keep logo visually centered
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    // header
                    Text(
                        text = "Complete Your\nOnboarding in ${steps.size} Steps",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        color = BaseTheme.BaseTextColor,
                        fontSize = 23.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 25.dp, start = 25.dp, end = 20.dp)
                    )
                    Text(
                        text = "It will include capturing your ID and your face — it's fast, easy, and secure.",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        color = BaseTheme.BaseTextColor,
                        fontSize = 12.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp, start = 25.dp, end = 25.dp, bottom = 10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        itemsIndexed(steps) { index, step ->
                            StepCard(
                                step = step,
                                selectedColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                                unselectedColor = BaseTheme.FieldColor,
                                onClick = { onStepClick(step) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                // ======= BOTTOM BUTTON (ALWAYS AT END OF SCREEN) =======
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 25.dp, horizontal = 25.dp)
                        .background(
                            brush = BaseTheme.BaseClickColor!!.toBrush(),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Text(
                        "Next",
                        fontFamily = InterFont,
                        color = BaseTheme.BaseSecondaryTextColor,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }
    }

}
