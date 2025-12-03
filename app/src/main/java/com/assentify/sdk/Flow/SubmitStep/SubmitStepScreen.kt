package com.assentify.sdk.Flow.SubmitStep


import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    val bg = Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor))
    val accent = Color.White
    val pill = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor))

    var resetTick by remember { mutableStateOf(0) }

    if(submitDataTypes == SubmitDataTypes.onError){
        resetTick++
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
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
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))

                    logoBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                        )
                    } ?: Spacer(Modifier.size(22.dp))

                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp))
                }
            }

            // MIDDLE (all states share one weighted box)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (submitDataTypes) {
                    SubmitDataTypes.onSend -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(70.dp),
                            color = Color.White,
                            strokeWidth = 6.dp
                        )
                    }

                    SubmitDataTypes.onError -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(170.dp)
                                    .padding(bottom = 24.dp)
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    accent.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            radius = size.minDimension / 2f
                                        )
                                    }
                            ) {
                                logoBitmap?.let {
                                    Image(
                                        bitmap = it,
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(130.dp)
                                            .graphicsLayer {
                                                shadowElevation = 12f
                                                shape = RoundedCornerShape(18.dp)
                                                clip = false
                                            }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "We couldn't complete your submission. Check your connection and retry.",
                                color = Color.Red,
                                fontSize = 15.sp,
                                lineHeight = 25.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }

                    SubmitDataTypes.none -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(170.dp)
                                    .padding(bottom = 24.dp)
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    accent.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            radius = size.minDimension / 2f
                                        )
                                    }
                            ) {
                                logoBitmap?.let {
                                    Image(
                                        bitmap = it,
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(130.dp)
                                            .graphicsLayer {
                                                shadowElevation = 12f
                                                shape = RoundedCornerShape(18.dp)
                                                clip = false
                                            }
                                    )
                                }
                            }

                            Text(
                                text = "Ready to Submit?",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Swipe the button below to confirm your submission.",
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 25.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    SubmitDataTypes.onComplete -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(170.dp)
                                    .padding(bottom = 24.dp)
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    accent.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            radius = size.minDimension / 2f
                                        )
                                    }
                            ) {
                                logoBitmap?.let {
                                    Image(
                                        bitmap = it,
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(130.dp)
                                            .graphicsLayer {
                                                shadowElevation = 12f
                                                shape = RoundedCornerShape(18.dp)
                                                clip = false
                                            }
                                    )
                                }
                            }

                            Text(
                                text = "THANK YOU",
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "For submitting and welcome to NXT\nNavigation x Transform",
                                color = Color.White,
                                fontSize = 18.sp,
                                lineHeight = 25.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // BOTTOM
            when {
                submitDataTypes != SubmitDataTypes.onSend &&
                        submitDataTypes != SubmitDataTypes.onComplete &&
                        submitDataTypes != SubmitDataTypes.onError -> {
                    SwipeToSubmit(
                        text = "Swipe to Submit",
                        resetKey = resetTick,
                        trackColor = pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 30.dp),
                        onComplete = { onSubmit() }
                    )
                }

                submitDataTypes == SubmitDataTypes.onComplete -> {
                    SwipeToSubmit(
                        text = " \t\tNext\t\t ",
                        trackColor = pill,
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



@Composable
fun SwipeToSubmit(
    text: String = "Swipe to Submit",
    trackColor: Color = Color(0xFF62D17D), // flat green track
    knobColor: Color = Color.White,         // flat white knob
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier,
    height: Dp = 70.dp,
    corner: Dp = 35.dp,
    onComplete: () -> Unit,
    resetKey: Any? = null,
) {
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


    LaunchedEffect(resetKey) {
        rawOffset = 0f
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
            .background(trackColor)
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }

        // Draggable knob
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxHeight()
                .clip(RoundedCornerShape(corner))
                .background(knobColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
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


