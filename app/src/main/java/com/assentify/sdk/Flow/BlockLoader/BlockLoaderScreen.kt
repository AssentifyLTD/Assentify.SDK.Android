package com.assentify.sdk.Flow.BlockLoader

import androidx.compose.foundation.shape.RoundedCornerShape


import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Flow.Models.LocalStepModel
import okhttp3.internal.wait




@Composable
fun BlockLoaderScreen(
    logoBytes: ByteArray?,
    steps: List<LocalStepModel>,
    backgroundHexColor: Color,
    clicksHexColor: Color,
    listItemsSelectedHexColor: Color,
    listItemsUnSelectedHexColor: Color,
    onBack: () -> Unit,
    onStepClick: (LocalStepModel) -> Unit,
    onNext: () -> Unit
) {
    val ctx = LocalContext.current

    val logoBitmap: ImageBitmap? = remember(logoBytes) {
        logoBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundHexColor)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // top row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // centered logo visually by Row spacer balance
                    logoBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // place-holder to keep logo visually centered
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = "Complete Your Onboarding in 5 Steps",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                )
                Text(
                    text = "It will include capturing your ID and your face — it's fast, easy, and secure.",
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(steps) { index, step ->
                        StepCard(
                            step = step,
                            selectedColor = listItemsSelectedHexColor,
                            unselectedColor = listItemsUnSelectedHexColor,
                            onClick = { onStepClick(step) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }





                // next button
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = clicksHexColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}
