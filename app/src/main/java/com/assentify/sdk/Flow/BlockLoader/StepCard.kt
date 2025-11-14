package com.assentify.sdk.Flow.BlockLoader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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




@Composable
fun StepCard(
    step: LocalStepModel,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (step.isDone) selectedColor else unselectedColor
    val circleColor = if (step.isDone) Color.White else selectedColor
    val iconTint = if (step.isDone) selectedColor else Color.White

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
                        tint = iconTint, // ✅ dynamic tint color
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column ( modifier = Modifier.weight(1f) ){
                Text(
                    text = step.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = step.description,
                    color = Color.White,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    fontSize = 13.sp
                )
            }
        }
    }
}