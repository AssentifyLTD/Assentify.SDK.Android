package com.assentify.sdk.Flow.SubmitStep


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.Events.SubmitDataTypes
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import kotlin.math.roundToInt

@Composable
fun SubmitStepScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    submitDataTypes: String,
    modifier: Modifier = Modifier
) {
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }




    var resetTick by remember { mutableStateOf(0) }

    if (submitDataTypes == SubmitDataTypes.onError) {
        resetTick++
    }
    val context = LocalContext.current

    val phoneIcon = remember("ic_phone.svg") {
        loadSvgFromAssets(context, "ic_phone.svg")
    }

    BaseBackgroundContainer(
        modifier = modifier
            .fillMaxSize()
         //   .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp,),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint =   BaseTheme.BaseTextColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))


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

                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // =========================
                // MIDDLE (takes remaining space)
                // =========================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // ✅ middle area
                    contentAlignment = Alignment.Center
                ) {
                    when (submitDataTypes) {

                        SubmitDataTypes.onSend -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(70.dp),
                                color =   BaseTheme.BaseTextColor,
                                strokeWidth = 6.dp
                            )
                        }

                        SubmitDataTypes.onError -> {
                            MiddleContent(
                                phoneIcon = phoneIcon,
                                flowEnv = flowEnv,
                                title = null,
                                message = "We couldn't complete your submission. Check your connection and retry.",
                                messageColor = BaseTheme.BaseRedColor
                            )
                        }

                        SubmitDataTypes.none -> {
                            MiddleContent(
                                phoneIcon = phoneIcon,
                                flowEnv = flowEnv,
                                title = "Ready to Submit?",
                                message = "Swipe the button below to confirm your submission.",
                                messageColor =   BaseTheme.BaseTextColor
                            )
                        }

                        SubmitDataTypes.onComplete -> {
                            MiddleContent(
                                phoneIcon = phoneIcon,
                                flowEnv = flowEnv,
                                title = "THANK YOU",
                                message = "Swipe the button below to continue.",
                                messageColor =   BaseTheme.BaseTextColor
                            )
                        }
                    }
                }

                // =========================
                // BOTTOM (fixed)
                // =========================
                when {
                    submitDataTypes != SubmitDataTypes.onSend &&
                            submitDataTypes != SubmitDataTypes.onComplete &&
                            submitDataTypes != SubmitDataTypes.onError -> {
                        SwipeToSubmit(
                            text = "Swipe to Submit",
                            resetKey = resetTick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 30.dp),
                            onComplete = { onSubmit() }
                        )
                    }

                    submitDataTypes == SubmitDataTypes.onComplete -> {
                        SwipeToSubmit(
                            text = "\t\t\t\t\t\t\tNext\t\t\t\t\t\t\t",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 30.dp),
                            onComplete = { onSubmit() }
                        )
                    }
                }
            }

        }
    }

}

@Composable
private fun MiddleContent(
    phoneIcon: Painter?,
    flowEnv: FlowEnvironmentalConditions,
    title: String?,
    message: String,
    messageColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier.padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            phoneIcon?.let {
                Image(
                    painter = it,
                    contentDescription = "phoneIcon",
                    modifier = Modifier
                        .height(400.dp),
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(
                        Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                    )
                )
            }


            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(BaseTheme.BaseLogo)
                    .crossfade(true)
                    .build(),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(140.dp)
                    .padding(top = 40.dp),
                contentScale = ContentScale.Fit
            )

        }

        title?.let {
            Text(
                text = it,
                color =   BaseTheme.BaseTextColor,
                fontSize = if (it == "THANK YOU") 38.sp else 30.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = message,
            color = messageColor,
            fontFamily = InterFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SwipeToSubmit(
    text: String = "Swipe to Submit",
    modifier: Modifier = Modifier,
    height: Dp = 65.dp,
    corner: Dp = 35.dp,
    onComplete: () -> Unit,
    resetKey: Any? = null,
) {

    val context = LocalContext.current


    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(0f) }

    val knobPadding = with(density) { 6.dp.toPx() }
    val knobMinWidthPx = with(density) { (height - 12.dp).toPx() * 2.2f } // wider knob for text

    var rawOffset by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = tween(150),
        label = "swipeOffset"
    )

    // Measure knob text width dynamically
    var textWidthPx by remember { mutableStateOf(0f) }
    val knobWidthPx by remember(textWidthPx) {
        mutableStateOf(maxOf(knobMinWidthPx, textWidthPx + with(density) { 32.dp.toPx() }))
    }

    val maxOffset by remember(trackWidthPx, knobWidthPx) {
        derivedStateOf { (trackWidthPx - knobPadding - knobWidthPx).coerceAtLeast(0f) }
    }

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    LaunchedEffect(resetKey) {
        rawOffset = 0f
    }

    val arrowsIcon = remember("ic_right_arrows.svg") {
        loadSvgFromAssets(context, "ic_right_arrows.svg")
    }


    val threshold = maxOffset * 0.75f

    fun settle() {
        if (rawOffset >= threshold) {
            rawOffset = maxOffset
            onComplete()
        } else rawOffset = 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background( BaseTheme.BaseClickColor!!.toBrush())
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        rawOffset = (rawOffset + dragAmount).coerceIn(0f, maxOffset)
                    },
                    onDragEnd = { settle() },
                    onDragCancel = { settle() }
                )
            }
    ) {
        // Right chevrons hint
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            arrowsIcon?.let {
                Image(
                    painter = it,
                    contentDescription = "arrowsIcon",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(
                          BaseTheme.BaseTextColor.copy(alpha = 0.5f),
                    )
                )
            }
        }

        // Draggable knob
        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxHeight(),
            shape = RoundedCornerShape(corner),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
            )
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = BaseTheme.BaseTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned { textWidthPx = it.size.width.toFloat() }
                )
            }
        }
    }
}


