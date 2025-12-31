package com.assentify.sdk.Flow.BlockLoader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@Composable
fun StepCard(
    step: LocalStepModel,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    val backgroundColor = if (step.isDone)  selectedColor else unselectedColor
    val circleColor = if (step.isDone) Color.White else selectedColor
    val iconColor = if (step.isDone) selectedColor else  Color.White
    val textColor = if (step.isDone) Color(android.graphics.Color.parseColor(flowEnv.listItemsTextSelectedHexColor)) else Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor))

    val context = LocalContext.current

    val iconPainter = remember(step.iconAssetPath) {
       loadSvgFromAssets(context, step.iconAssetPath)
    }

    Card(
        modifier = Modifier
            .padding(top = 10.dp, start = 20.dp, end = 20.dp)
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Show icon if available
            iconPainter?.let {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(color = circleColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = it,
                        contentDescription = step.name,
                        tint = iconColor, // ✅ dynamic tint color
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column ( modifier = Modifier.weight(1f) ){
                Text(
                    text = step.name,
                    color =textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = step.description,
                    color = textColor,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    fontSize = 13.sp
                )
            }
        }
    }
}