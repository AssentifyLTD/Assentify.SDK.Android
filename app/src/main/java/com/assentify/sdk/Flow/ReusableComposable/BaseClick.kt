package com.assentify.sdk.Flow.ReusableComposable

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun  BaseClick(
    isNormalClick: Boolean = true,
    onNext: () -> Unit = {},
    label: String = "Let's Go",
    icon: ImageVector = Icons.Default.Check,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 25.dp, horizontal = 25.dp),
    sliderModifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 25.dp, horizontal = 25.dp)
) {
    if (isNormalClick) {
        NormalClick(onNext = onNext, label = label, modifier = modifier,isActive = isActive)
    } else {
        SliderClick(onNext = onNext, label = label, icon = icon, modifier = sliderModifier,isActive = isActive)
    }
}

@Composable
private fun NormalClick(
    onNext: () -> Unit,
    label: String,
    modifier: Modifier,
    isActive: Boolean = true

) {
    Button(
        onClick = onNext,
        enabled = isActive,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier,

    ) {
        Text(
            text = label,
            fontFamily = InterFont,
            color = BaseTheme.BaseSecondaryTextColor,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(vertical = 7.dp)
        )
    }
}

@Composable
private fun SliderClick(
    onNext: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val sliderHeight = 54.dp
    val sliderShape = RoundedCornerShape(100.dp)

    // Initial yellow area is circular.
    val minimumFillWidth = sliderHeight

    var trackWidthPx by remember {
        mutableFloatStateOf(0f)
    }

    var rawOffset by remember {
        mutableFloatStateOf(0f)
    }

    var isCompleting by remember {
        mutableStateOf(false)
    }

    val minimumFillWidthPx = with(density) {
        minimumFillWidth.toPx()
    }

    val maxOffset by remember(trackWidthPx, minimumFillWidthPx) {
        derivedStateOf {
            (trackWidthPx - minimumFillWidthPx).coerceAtLeast(0f)
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = tween(
            durationMillis = 120,
            easing = LinearOutSlowInEasing
        ),
        label = "sliderOffset"
    )

    /*
     * Yellow fill starts as a circle and becomes a pill while dragging.
     */
    val fillWidthPx by remember(
        animatedOffset,
        minimumFillWidthPx,
        trackWidthPx
    ) {
        derivedStateOf {
            (minimumFillWidthPx + animatedOffset)
                .coerceIn(
                    minimumValue = minimumFillWidthPx,
                    maximumValue = trackWidthPx.coerceAtLeast(
                        minimumFillWidthPx
                    )
                )
        }
    }

    val fillWidth = with(density) {
        fillWidthPx.toDp()
    }

    val threshold by remember(maxOffset) {
        derivedStateOf {
            maxOffset * 0.85f
        }
    }

    fun settleSlider() {
        if (isCompleting) return

        if (rawOffset >= threshold && maxOffset > 0f) {
            isCompleting = true
            rawOffset = maxOffset

            scope.launch {
                delay(180)
                onNext()

                delay(100)
                rawOffset = 0f
                isCompleting = false
            }
        } else {
            rawOffset = 0f
        }
    }

    val arrowsIcon = remember(context) {
        loadSvgFromAssets(
            context,
            "ic_right_arrows.svg"
        )
    }

    Box(
        modifier = modifier
            .height(sliderHeight)
            .clip(sliderShape)
            .background(
                color = if (isActive) {
                    BaseTheme.FieldColor
                } else {
                    BaseTheme.FieldColor.copy(alpha = 0.4f)
                }
            )
            .onGloballyPositioned { coordinates ->
                trackWidthPx = coordinates.size.width.toFloat()
            }
            .pointerInput(
                isActive,
                isCompleting,
                maxOffset,
                isRtl
            ) {
                if (!isActive || isCompleting) {
                    return@pointerInput
                }

                detectHorizontalDragGestures(
                    onDragStart = {
                        rawOffset = animatedOffset
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()

                        val adjustedDrag = if (isRtl) {
                            -dragAmount
                        } else {
                            dragAmount
                        }

                        rawOffset = (
                                rawOffset + adjustedDrag
                                ).coerceIn(
                                minimumValue = 0f,
                                maximumValue = maxOffset
                            )
                    },
                    onDragEnd = {
                        settleSlider()
                    },
                    onDragCancel = {
                        settleSlider()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {

        /*
         * One complete yellow rounded pill.
         *
         * There is no separate circle and no gap from the parent edge.
         */
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(fillWidth)
                .clip(sliderShape)
                .background(
                    brush = BaseTheme.BaseClickColor!!.toBrush(),
                    alpha = if (isActive) 1f else 0.4f
                )
        )

        // Center label
        Text(
            text = label,
            modifier = Modifier.align(Alignment.Center),
            fontFamily = InterFont,
            color = if (isActive) {
                BaseTheme.BaseTextColor
            } else {
                BaseTheme.BaseTextColor.copy(alpha = 0.4f)
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // Trailing arrows
        arrowsIcon?.let { painter ->
            Image(
                painter = painter,
                contentDescription = "Swipe to continue",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .size(20.dp)
                    .scale(
                        scaleX = if (isRtl) -1f else 1f,
                        scaleY = 1f
                    ),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(
                    BaseTheme.BaseTextColor.copy(
                        alpha = if (isActive) 0.5f else 0.2f
                    )
                )
            )
        }

        /*
         * Check icon follows the rounded end of the yellow fill.
         * It has no separate circular background.
         */
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) {
                BaseTheme.FieldColor
            } else {
                BaseTheme.FieldColor.copy(alpha = 0.4f)
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val iconCenterPosition =
                        fillWidthPx - (minimumFillWidthPx / 2f)

                    IntOffset(
                        x = iconCenterPosition.roundToInt(),
                        y = 0
                    )
                }
                .offset(x = (-11).dp)
                .size(22.dp)
        )
    }
}