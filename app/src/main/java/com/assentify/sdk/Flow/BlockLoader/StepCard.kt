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
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.Models.LocalStepModel


@Composable
fun StepCard(
    step: LocalStepModel,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit
) {


    val backgroundColor = if (step.isDone)  selectedColor else unselectedColor
    val circleColor = if (step.isDone) Color.White else selectedColor
    val iconColor = if (step.isDone) selectedColor else  Color.White
    val textColor = if (step.isDone)  BaseTheme.BaseSecondaryTextColor else  BaseTheme.BaseTextColor

    val context = LocalContext.current

    val iconPainter = remember(step.iconAssetPath) {
       loadSvgFromAssets(context, step.iconAssetPath)
    }

    Card(
        modifier = Modifier
            .padding(top = 5.dp, start = 20.dp, end = 20.dp)
            .heightIn(min = 110.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 23.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Show icon if available
            iconPainter?.let {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(color = circleColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = it,
                        contentDescription = step.name,
                        tint = iconColor, // ✅ dynamic tint color
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column ( modifier = Modifier.weight(1f) ){
                Text(
                    text = step.name,
                    color =textColor,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Light,
                    color = textColor,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Start,
                    fontSize = 10.sp
                )
            }
        }
    }
}