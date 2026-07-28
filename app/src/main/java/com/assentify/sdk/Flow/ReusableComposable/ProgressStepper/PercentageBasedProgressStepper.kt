package com.assentify.sdk.Flow.ReusableComposable.ProgressStepper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.LocalStepsObject
import kotlin.math.max

@Composable
fun PercentageBasedProgressStepper(
    modifier: Modifier = Modifier,
    maxNodeSize: Dp = 57.dp,
    minNodeSize: Dp = 40.dp,
    maxConnectorLength: Dp = 40.dp,
    minConnectorLength: Dp = 12.dp,
    connectorThickness: Dp = 3.dp,
    backToNodeSpacing: Dp = 12.dp,
    titleWidth: Dp = 180.dp,
    onBack: () -> Unit,
) {
    val activeColor = Color(
        android.graphics.Color.parseColor(
            BaseTheme.BaseAccentColor
        )
    )

    val doneColor = Color(
        android.graphics.Color.parseColor(
            BaseTheme.BaseAccentColor
        )
    )

    val upcomingColor = BaseTheme.FieldColor

    val steps = remember {
        LocalStepsObject
            .getLocalSteps()
            .filter { it.show }
    }

    val totalSteps = steps.size
    val doneCount = steps.count { it.isDone }

    val rangeStart = BaseTheme.RangeStart.toFloat()
    val rangeEnd = BaseTheme.RangeEnd.toFloat()
    val rangeWidth = (rangeEnd - rangeStart).coerceAtLeast(0f)

    val percentagePerStep = if (totalSteps > 0) {
        rangeWidth / totalSteps
    } else {
        0f
    }

    val currentPercentage =
        rangeStart + (doneCount * percentagePerStep)

    val safeNodeCount = BaseTheme.NodeCount.coerceAtLeast(2)

    val nodePercentages = remember(
        safeNodeCount,
        rangeStart,
        rangeWidth
    ) {
        (0 until safeNodeCount).map { index ->
            rangeStart +
                    (index.toFloat() / (safeNodeCount - 1)) *
                    rangeWidth
        }
    }

    val firstUpcomingIndex = nodePercentages
        .indexOfFirst { percentage ->
            percentage > currentPercentage
        }
        .takeIf { it != -1 }
        ?: safeNodeCount

    val nodes = nodePercentages.mapIndexed { index, percentage ->
        val state = when {
            index < firstUpcomingIndex -> {
                StepVisualState.Done
            }

            index == firstUpcomingIndex -> {
                StepVisualState.Active
            }

            else -> {
                StepVisualState.Upcoming
            }
        }

        val fillFraction =
            if (state == StepVisualState.Active) {
                val slotStart = if (index == 0) {
                    rangeStart
                } else {
                    nodePercentages[index - 1]
                }

                val slotWidth = percentage - slotStart

                if (slotWidth > 0f) {
                    (
                            (currentPercentage - slotStart) /
                                    slotWidth
                            ).coerceIn(0f, 1f)
                } else {
                    0f
                }
            } else {
                0f
            }

        NodeInfo(
            percentage = percentage,
            state = state,
            fillFraction = fillFraction
        )
    }

    val completedConnectors = nodes
        .dropLast(1)
        .map { node ->
            node.state == StepVisualState.Done
        }

    val sideWidth = 52.dp
    val density = LocalDensity.current

    var stepperRowCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    var activeNodeCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalWidth = maxWidth

            val availableStepperWidth = (
                    totalWidth -
                            sideWidth * 2 -
                            backToNodeSpacing
                    ).coerceAtLeast(0.dp)

            val nodeCount = safeNodeCount

            val idealTotalWidth =
                maxNodeSize * nodeCount +
                        maxConnectorLength *
                        max(nodeCount - 1, 0)

            val nodeAndConnectorSize =
                if (
                    idealTotalWidth <= availableStepperWidth ||
                    nodeCount <= 1
                ) {
                    maxNodeSize to maxConnectorLength
                } else {
                    val totalWithMinimumConnectors =
                        maxNodeSize * nodeCount +
                                minConnectorLength *
                                max(nodeCount - 1, 0)

                    if (
                        totalWithMinimumConnectors <=
                        availableStepperWidth
                    ) {
                        val extraSpace =
                            availableStepperWidth -
                                    (maxNodeSize * nodeCount)

                        val calculatedConnectorLength =
                            (
                                    extraSpace /
                                            max(nodeCount - 1, 1)
                                    ).coerceIn(
                                    minConnectorLength,
                                    maxConnectorLength
                                )

                        maxNodeSize to calculatedConnectorLength
                    } else {
                        val remainingWidthForNodes =
                            availableStepperWidth -
                                    minConnectorLength *
                                    max(nodeCount - 1, 0)

                        val calculatedNodeSize =
                            (
                                    remainingWidthForNodes /
                                            nodeCount
                                    ).coerceIn(
                                    minNodeSize,
                                    maxNodeSize
                                )

                        calculatedNodeSize to minConnectorLength
                    }
                }

            val nodeSize = nodeAndConnectorSize.first
            val connectorLength = nodeAndConnectorSize.second

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        stepperRowCoordinates = coordinates
                    }
            ) {
                Box(
                    modifier = Modifier.width(sideWidth),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                BaseTheme.FieldColor.copy(
                                    alpha = 0.5f
                                )
                            )
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BaseTheme.BaseTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(
                        backToNodeSpacing
                    )
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        nodes.forEachIndexed { index, node ->
                            Box(
                                contentAlignment =
                                    Alignment.Center,
                                modifier =
                                    if (
                                        node.state ==
                                        StepVisualState.Active
                                    ) {
                                        Modifier.onGloballyPositioned {
                                                coordinates ->
                                            activeNodeCoordinates =
                                                coordinates
                                        }
                                    } else {
                                        Modifier
                                    }
                            ) {
                                StepNode(
                                    number = index + 1,
                                    state = node.state,
                                    fillFraction =
                                        node.fillFraction,
                                    size = nodeSize,
                                    activeColor = activeColor,
                                    doneColor = doneColor,
                                    upcomingColor =
                                        upcomingColor
                                )
                            }

                            if (index < nodes.lastIndex) {
                                Box(
                                    modifier = Modifier.height(
                                        nodeSize
                                    ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    StepConnector(
                                        modifier = Modifier
                                            .width(
                                                connectorLength
                                            )
                                            .height(
                                                connectorThickness
                                            ),
                                        done =
                                            completedConnectors[
                                                index
                                            ],
                                        activeColor =
                                            activeColor,
                                        upcomingColor =
                                            upcomingColor
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.width(sideWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text =
                            "${currentPercentage.toInt()}%",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        fontSize = 13.sp
                    )
                }
            }
        }

        val activeNodeIndex = nodes.indexOfFirst { node ->
            node.state == StepVisualState.Active
        }

        val rowCoordinates = stepperRowCoordinates
        val nodeCoordinates = activeNodeCoordinates

        if (
            activeNodeIndex != -1 &&
            rowCoordinates != null &&
            nodeCoordinates != null
        ) {
            val nodeCenterInsideRow =
                rowCoordinates.localPositionOf(
                    sourceCoordinates = nodeCoordinates,
                    relativeToSource = Offset(
                        x = nodeCoordinates.size.width / 2f,
                        y = 0f
                    )
                ).x

            val rowWidthInPixels =
                rowCoordinates.size.width.toFloat()

            val physicalOffset = with(density) {
                (
                        nodeCenterInsideRow -
                                rowWidthInPixels / 2f
                        ).toDp()
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            /*
             * absoluteOffset is required because physicalOffset is calculated
             * using physical screen coordinates.
             *
             * Modifier.offset would automatically mirror the value in RTL.
             */
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                modifier = Modifier.absoluteOffset(
                    x = physicalOffset
                )
            ) {
                Caret(
                    visible = true,
                    activeColor = activeColor
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = BaseTheme.StepperTitle,
                    modifier = Modifier.width(titleWidth),
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    color = upcomingColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class NodeInfo(
    val percentage: Float,
    val state: StepVisualState,
    val fillFraction: Float
)

@Composable
private fun StepNode(
    number: Int,
    state: StepVisualState,
    fillFraction: Float,
    size: Dp,
    activeColor: Color,
    doneColor: Color,
    upcomingColor: Color
) {
    /*
     * This detects the current Compose direction:
     *
     * English = LTR
     * Arabic = RTL
     */
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            drawNode(
                state = state,
                fillFraction = fillFraction,
                activeColor = activeColor,
                doneColor = doneColor,
                upcomingColor = upcomingColor,
                isRtl = isRtl
            )
        }

        val textColor = when (state) {
            StepVisualState.Done -> {
                BaseTheme.BaseSecondaryTextColor
            }

            StepVisualState.Active,
            StepVisualState.Upcoming -> {
                BaseTheme.BaseTextColor
            }
        }

        Text(
            text = number.toString(),
            fontFamily = InterFont,
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 17.sp
        )
    }
}

private fun DrawScope.drawNode(
    state: StepVisualState,
    fillFraction: Float,
    activeColor: Color,
    doneColor: Color,
    upcomingColor: Color,
    isRtl: Boolean
) {
    val radius = size.minDimension / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    when (state) {
        StepVisualState.Done -> {
            drawCircle(
                color = doneColor,
                radius = radius,
                center = Offset(
                    x = centerX,
                    y = centerY
                )
            )
        }

        StepVisualState.Active -> {
            /*
             * Draw the upcoming/background color first.
             */
            drawCircle(
                color = upcomingColor,
                radius = radius,
                center = Offset(
                    x = centerX,
                    y = centerY
                )
            )

            val minimumFillFraction = 0.15f

            val effectiveFillFraction =
                minimumFillFraction +
                        fillFraction *
                        (1f - minimumFillFraction)

            val fillWidth =
                radius * 2f * effectiveFillFraction

            if (effectiveFillFraction > 0f) {
                val circleLeft = centerX - radius
                val circleRight = centerX + radius

                val clipLeft: Float
                val clipRight: Float

                if (isRtl) {
                    /*
                     * Arabic:
                     * Start filling from the right side and move left.
                     */
                    clipLeft = circleRight - fillWidth
                    clipRight = circleRight
                } else {
                    /*
                     * English:
                     * Start filling from the left side and move right.
                     */
                    clipLeft = circleLeft
                    clipRight = circleLeft + fillWidth
                }

                clipRect(
                    left = clipLeft,
                    top = centerY - radius,
                    right = clipRight,
                    bottom = centerY + radius
                ) {
                    drawCircle(
                        color = activeColor,
                        radius = radius,
                        center = Offset(
                            x = centerX,
                            y = centerY
                        )
                    )
                }
            }
        }

        StepVisualState.Upcoming -> {
            drawCircle(
                color = upcomingColor,
                radius = radius,
                center = Offset(
                    x = centerX,
                    y = centerY
                )
            )
        }
    }
}

@Composable
private fun StepConnector(
    modifier: Modifier,
    done: Boolean,
    activeColor: Color,
    upcomingColor: Color
) {
    val connectorColor = if (done) {
        activeColor
    } else {
        upcomingColor
    }

    Canvas(modifier = modifier) {
        val centerY = size.height / 2f

        drawLine(
            color = connectorColor,
            start = Offset(
                x = 0f,
                y = centerY
            ),
            end = Offset(
                x = size.width,
                y = centerY
            ),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun Caret(
    visible: Boolean,
    activeColor: Color
) {
    Canvas(
        modifier = Modifier.size(
            width = 10.dp,
            height = 6.dp
        )
    ) {
        if (!visible) {
            return@Canvas
        }

        val path = Path().apply {
            moveTo(
                x = 0f,
                y = 0f
            )

            lineTo(
                x = size.width,
                y = 0f
            )

            lineTo(
                x = size.width / 2f,
                y = size.height
            )

            close()
        }

        drawPath(
            path = path,
            color = activeColor
        )
    }
}