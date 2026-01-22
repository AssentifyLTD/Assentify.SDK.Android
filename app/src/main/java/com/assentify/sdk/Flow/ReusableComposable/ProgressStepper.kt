package com.assentify.sdk.Flow.ReusableComposable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LocalStepsObject

private enum class StepVisualState { Done, Active, Upcoming }

@Composable
fun ProgressStepper(
    modifier: Modifier = Modifier,
    nodeSize: Dp = 50.dp,
    ringWidth: Dp = 2.dp,
    gapWidth: Dp = 3.dp,
    dashLength: Dp = 12.dp,
    dashGap: Dp = 10.dp,
    connectorLength: Dp = 15.dp,
    connectorThickness: Dp = 2.dp,
    itemSpacing: Dp = 12.dp
) {
    val steps: List<LocalStepModel> = remember { LocalStepsObject.getLocalSteps().filter { it.show } }
    val activeIndex: Int =
        steps.indexOfFirst { !it.isDone }.takeIf { it != -1 } ?: steps.lastIndex.coerceAtLeast(0)

    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }
    val activeColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
    val doneColor = BaseTheme.BaseGreenColor
    val upcomingColor =   BaseTheme.BaseTextColor

    val density = LocalDensity.current
    val dashEffect = remember(dashLength, dashGap, density) {
        with(density) {
            PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), dashGap.toPx()),
                0f
            )
        }
    }

    val totalItemWidthPx = with(density) {
        (steps.size * nodeSize.toPx()) +
                ((steps.size - 1) * connectorLength.toPx()) +
                ((steps.size + 1) * itemSpacing.toPx())
    }
    val screenWidthPx = with(density) { LocalContext.current.resources.displayMetrics.widthPixels.toFloat() }
    val isScrollable = totalItemWidthPx > screenWidthPx

    Column(

    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(),
            contentAlignment = if (!isScrollable) Alignment.Center else Alignment.TopStart
        ) {
            val rowModifier = if (isScrollable)
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = itemSpacing / 2)
            else
                Modifier.padding(horizontal = itemSpacing / 2)

            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { i, step ->
                    val state = remember(steps, activeIndex, i, step.isDone) {
                        when {
                            step.isDone -> StepVisualState.Done
                            i == activeIndex -> StepVisualState.Active
                            else -> StepVisualState.Upcoming
                        }
                    }

                    Spacer(Modifier.width(itemSpacing / 2))

                    StepNode(
                        iconAssetPath = step.iconAssetPath,
                        state = state,
                        nodeSize = nodeSize,
                        ringWidth = ringWidth,
                        gapWidth = gapWidth,
                        activeColor = activeColor,
                        doneColor = doneColor,
                        upcomingColor = upcomingColor,
                    )

                    Spacer(Modifier.width(itemSpacing / 2))

                    if (i < steps.lastIndex) {
                        val connectorColor =
                            when {
                                steps[i].isDone -> doneColor
                                i == activeIndex -> activeColor
                                else -> upcomingColor.copy(alpha = 0.9f)
                            }

                        StepConnector(
                            modifier = Modifier
                                .width(connectorLength)
                                .height(connectorThickness),
                            color = connectorColor,
                            pathEffect = dashEffect
                        )
                    }
                }
            }

        }
        Text(
            text = "Step ${activeIndex + 1} out of ${steps.size} stpes",
            fontFamily = InterFont,
            fontWeight = FontWeight.Normal,
            color =   BaseTheme.BaseTextColor,
            fontSize = 8.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, start = 25.dp, end = 25.dp, bottom = 10.dp)
        )
    }

}

@Composable
private fun StepNode(
    iconAssetPath: String,
    state: StepVisualState,
    nodeSize: Dp,
    ringWidth: Dp,
    gapWidth: Dp,
    activeColor: Color,
    doneColor: Color,
    upcomingColor: Color,
    modifier: Modifier = Modifier,

) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val context = LocalContext.current
    val density = LocalDensity.current


    val ring = when (state) {
        StepVisualState.Upcoming ->   BaseTheme.BaseTextColor
        StepVisualState.Active -> Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
        StepVisualState.Done -> BaseTheme.BaseGreenColor
    }
    val fill = when (state) {
        StepVisualState.Upcoming -> Color.Transparent
        StepVisualState.Active -> Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
        StepVisualState.Done -> BaseTheme.BaseGreenColor
    }

    val iconFill = when (state) {
        StepVisualState.Upcoming ->   BaseTheme.BaseTextColor
        StepVisualState.Active -> Color.White
        StepVisualState.Done -> Color.White
    }

    Box(
        modifier = modifier
            .size(nodeSize)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val ringPx = with(density) { ringWidth.toPx() }
            val gapPx = with(density) { gapWidth.toPx() }
            val rOuter = this.size.minDimension / 2f

            // Ring
            drawCircle(
                color = ring,
                radius = rOuter - ringPx / 2f,
                style = Stroke(width = ringPx)
            )
            if (fill != Color.Transparent) {
                val rInner = rOuter - ringPx - gapPx
                if (rInner > 0f) drawCircle(color = fill, radius = rInner)
            }
        }

        val svgPainter = remember(iconAssetPath, context) {
            loadSvgFromAssets(context, iconAssetPath)
        }
        svgPainter?.let {
            Icon(
                painter = it,
                contentDescription = iconAssetPath,
                tint = iconFill,
                modifier = Modifier.size(nodeSize * 0.5f)
            )
        }
    }
}

@Composable
private fun StepConnector(
    modifier: Modifier,
    color: Color,
    pathEffect: PathEffect
) {
    Canvas(modifier) {
        val y = this.size.height / 2f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(this.size.width, y),
            strokeWidth = this.size.height,
            pathEffect = pathEffect,
            cap = StrokeCap.Round
        )
    }
}
