package com.assentify.sdk.Flow.ReusableComposable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
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
    modifier: Modifier,
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val height = 54.dp
    val corner = 100.dp

    var trackWidthPx by remember { mutableStateOf(0f) }
    val knobSizePx = with(density) { (height - 10.dp).toPx() }
    val knobPaddingPx = with(density) { 5.dp.toPx() }

    val maxOffset by remember(trackWidthPx) {
        derivedStateOf { (trackWidthPx - knobSizePx - knobPaddingPx * 2).coerceAtLeast(0f) }
    }

    var rawOffset by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = tween(50),
        label = "swipeOffset"
    )

    val threshold = maxOffset * 0.85f

    fun settle() {
        if (rawOffset >= threshold) {
            rawOffset = maxOffset
            onNext()
        } else {
            rawOffset = 0f
        }
    }

    val arrowsIcon = remember("ic_right_arrows.svg") {
        loadSvgFromAssets(context, "ic_right_arrows.svg")
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(
                color = if (isActive) BaseTheme.FieldColor  // ← dim when inactive
                else BaseTheme.FieldColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(corner)
            )
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .pointerInput(isActive) {                        // ← reacts to isActive changes
                if (!isActive) return@pointerInput           // ← block drag if not active
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        rawOffset = (rawOffset + dragAmount).coerceIn(0f, maxOffset)
                    },
                    onDragEnd = { settle() },
                    onDragCancel = { settle() }
                )
            },
        contentAlignment = Alignment.Center
    ) {

        // ── Label ──────────────────────────────────────────────────
        Text(
            text = label,
            fontFamily = InterFont,
            color = if (isActive) BaseTheme.BaseTextColor
            else BaseTheme.BaseTextColor.copy(alpha = 0.4f),  // ← dim when inactive
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Right arrows ───────────────────────────────────────────
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
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(
                        BaseTheme.BaseTextColor.copy(
                            alpha = if (isActive) 0.5f else 0.2f  // ← dim when inactive
                        )
                    )
                )
            }
        }

        // ── Sliding Icon ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .padding(start = 5.dp)
                .size(with(density) { knobSizePx.toDp() })
                .clip(CircleShape)
                .background(
                    brush = BaseTheme.BaseClickColor!!.toBrush(),
                    alpha = if (isActive) 1f else 0.4f  // ← dim when inactive
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) BaseTheme.FieldColor
                else BaseTheme.FieldColor.copy(alpha = 0.4f),  // ← dim when inactive
                modifier = Modifier.size(25.dp)
            )
        }
    }
}