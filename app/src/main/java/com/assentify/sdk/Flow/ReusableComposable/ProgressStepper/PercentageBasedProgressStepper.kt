package com.assentify.sdk.Flow.ReusableComposable.ProgressStepper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.LocalStepsObject


@Composable
fun PercentageBasedProgressStepper(
    modifier: Modifier = Modifier,
    nodeSize: Dp = 57.dp,
    connectorLength: Dp = 40.dp,
    connectorThickness: Dp = 3.dp,
    onBack: () -> Unit,
) {
    // ── Colors ────────────────────────────────────────────────────────────────
    val activeColor   = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
    val doneColor     = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
    val upcomingColor = BaseTheme.FieldColor

    // ── Steps ─────────────────────────────────────────────────────────────────
    val steps      = remember { LocalStepsObject.getLocalSteps().filter { it.show } }
    val totalSteps = steps.size
    val doneCount  = steps.count { it.isDone }

    // ── Range math ────────────────────────────────────────────────────────────
    val rangeStart = BaseTheme.RangeStart.toFloat()
    val rangeEnd   = BaseTheme.RangeEnd.toFloat()
    val rangeWidth = (rangeEnd - rangeStart).coerceAtLeast(0f)
    val pctPerStep = if (totalSteps > 0) rangeWidth / totalSteps else 0f
    val currentPct = rangeStart + (doneCount * pctPerStep)

    // ── Node positions ────────────────────────────────────────────────────────
    val safeNodeCount = BaseTheme.NodeCount.coerceAtLeast(2)
    val nodePcts: List<Float> = (0 until safeNodeCount).map { i ->
        rangeStart + (i.toFloat() / (safeNodeCount - 1)) * rangeWidth
    }

    // ── Node states ───────────────────────────────────────────────────────────
    val firstUpcomingIndex = nodePcts.indexOfFirst { it > currentPct }
        .takeIf { it != -1 } ?: safeNodeCount

    data class NodeInfo(
        val pct: Float,
        val state: StepVisualState,
        val fillFraction: Float,
    )

    val nodes: List<NodeInfo> = nodePcts.mapIndexed { i, pct ->
        val state = when {
            i < firstUpcomingIndex  -> StepVisualState.Done
            i == firstUpcomingIndex -> StepVisualState.Active
            else                    -> StepVisualState.Upcoming
        }
        val fillFraction = if (state == StepVisualState.Active) {
            val slotStart = if (i == 0) rangeStart else nodePcts[i - 1]
            val slotWidth = pct - slotStart
            if (slotWidth > 0f) ((currentPct - slotStart) / slotWidth).coerceIn(0f, 1f)
            else 0f
        } else 0f
        NodeInfo(pct = pct, state = state, fillFraction = fillFraction)
    }

    val connectorsDone = nodes.dropLast(1).map { it.state == StepVisualState.Done }
    val sideWidth      = 52.dp
    val density        = LocalDensity.current

    var activeNodeCenterX by remember { mutableStateOf(0f) }
    var rowWidthPx        by remember { mutableStateOf(0f) }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {

            // ── Back button ───────────────────────────────────────────────────
            Box(
                modifier         = Modifier.width(sideWidth),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick  = { onBack() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BaseTheme.FieldColor.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = BaseTheme.BaseTextColor,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            // ── Nodes + connectors ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { rowWidthPx = it.size.width.toFloat() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    nodes.forEachIndexed { i, node ->

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (node.state == StepVisualState.Active) {
                                Modifier.onGloballyPositioned { coords ->
                                    activeNodeCenterX =
                                        coords.positionInParent().x + coords.size.width / 2f
                                }
                            } else Modifier
                        ) {
                            StepNode(
                                number        = i + 1,
                                state         = node.state,
                                fillFraction  = node.fillFraction,
                                size          = nodeSize,
                                activeColor   = activeColor,
                                doneColor     = doneColor,
                                upcomingColor = upcomingColor,
                            )
                        }

                        if (i < nodes.lastIndex) {
                            Box(
                                modifier         = Modifier.height(nodeSize),
                                contentAlignment = Alignment.Center
                            ) {
                                StepConnector(
                                    modifier      = Modifier
                                        .width(connectorLength)
                                        .height(connectorThickness),
                                    done          = connectorsDone[i],
                                    activeColor   = activeColor,
                                    upcomingColor = upcomingColor,
                                )
                            }
                        }
                    }
                }
            }

            // ── % badge ───────────────────────────────────────────────────────
            Box(
                modifier         = Modifier.width(sideWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "${currentPct.toInt()}%",
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    color      = activeColor,
                    fontSize   = 13.sp,
                )
            }
        }

        // ── Caret + title aligned under active node ───────────────────────────
        val activeIndex = nodes.indexOfFirst { it.state == StepVisualState.Active }
        if (activeIndex != -1 && rowWidthPx > 0f) {
            val offsetDp = with(density) {
                val boxCenterPx = rowWidthPx / 2f
                (activeNodeCenterX - boxCenterPx).toDp()
            }

            Spacer(Modifier.height(5.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.offset(x = offsetDp)
            ) {
                Caret(visible = true, activeColor = activeColor)
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = BaseTheme.StepperTitle,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    color      = upcomingColor,
                    fontSize   = 13.sp,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ── Step node ─────────────────────────────────────────────────────────────────

@Composable
private fun StepNode(
    number: Int,
    state: StepVisualState,
    fillFraction: Float,
    size: Dp,
    activeColor: Color,
    doneColor: Color,
    upcomingColor: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawNode(
                state         = state,
                fillFraction  = fillFraction,
                activeColor   = activeColor,
                doneColor     = doneColor,
                upcomingColor = upcomingColor,
            )
        }

        val textColor = when (state) {
            StepVisualState.Done     -> BaseTheme.BaseSecondaryTextColor
            StepVisualState.Active   -> BaseTheme.BaseTextColor
            StepVisualState.Upcoming -> BaseTheme.BaseTextColor
        }
        Text(
            text       = "$number",
            fontFamily = InterFont,
            fontWeight = FontWeight.Medium,
            color      = textColor,
            fontSize   = 17.sp,
        )
    }
}

// ── Canvas drawing ────────────────────────────────────────────────────────────

private fun DrawScope.drawNode(
    state: StepVisualState,
    fillFraction: Float,
    activeColor: Color,
    doneColor: Color,
    upcomingColor: Color,
) {
    val r  = size.minDimension / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f

    when (state) {
        StepVisualState.Done -> {
            drawCircle(color = doneColor, radius = r, center = Offset(cx, cy))
        }
        StepVisualState.Active -> {
            drawCircle(color = upcomingColor, radius = r, center = Offset(cx, cy))
            val minFraction       = 0.15f
            val effectiveFraction = minFraction + fillFraction * (1f - minFraction)
            val fillWidth         = r * 2 * effectiveFraction
            if (effectiveFraction > 0f) {
                clipRect(
                    left   = cx - r,
                    top    = cy - r,
                    right  = cx - r + fillWidth,
                    bottom = cy + r,
                ) {
                    drawCircle(color = activeColor, radius = r, center = Offset(cx, cy))
                }
            }
        }
        StepVisualState.Upcoming -> {
            drawCircle(color = upcomingColor, radius = r, center = Offset(cx, cy))
        }
    }
}

// ── Connector ─────────────────────────────────────────────────────────────────

@Composable
private fun StepConnector(
    modifier: Modifier,
    done: Boolean,
    activeColor: Color,
    upcomingColor: Color,
) {
    val color = if (done) activeColor else upcomingColor
    Canvas(modifier) {
        val y = size.height / 2f
        drawLine(
            color       = color,
            start       = Offset(0f, y),
            end         = Offset(size.width, y),
            strokeWidth = size.height,
            cap         = StrokeCap.Round,
        )
    }
}

// ── Caret ─────────────────────────────────────────────────────────────────────

@Composable
private fun Caret(
    visible: Boolean,
    activeColor: Color,
) {
    Canvas(modifier = Modifier.size(width = 10.dp, height = 6.dp)) {
        if (!visible) return@Canvas
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path = path, color = activeColor)
    }
}