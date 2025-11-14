package com.assentify.sdk.Flow.ReusableComposable


import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import java.io.ByteArrayOutputStream

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    title: String = "Signature",
    cardColor: Color = Color(0xFF0C1B16),
    confirmColor: Color = Color(0xFFF0C24B),
    disabledConfirmColor: Color = Color(0xFFBDBDBD),
    penColorInt: Int = android.graphics.Color.WHITE, // still white on screen
    minStrokeWidth: Float = 3f,
    maxStrokeWidth: Float = 6f,
    onConfirmBase64: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // native SignaturePad instance
    val signaturePad = remember {
        com.github.gcacace.signaturepad.views.SignaturePad(context, null).apply {
            setMinWidth(minStrokeWidth)
            setMaxWidth(maxStrokeWidth)
            setPenColor(penColorInt) // white pen on screen
            setVelocityFilterWeight(0.9f)
        }
    }

    var hasSignature by remember { mutableStateOf(false) }
    var isExpanding by remember { mutableStateOf(false) }
    var containerWidthPx by remember { mutableStateOf(0) }

    // observe pad state
    DisposableEffect(signaturePad) {
        signaturePad.setOnSignedListener(object :
            com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() { hasSignature = true }
            override fun onClear() { hasSignature = false }
        })
        onDispose { signaturePad.setOnSignedListener(null) }
    }

    val containerWidthDp = with(density) { containerWidthPx.toDp() }
    val pillInitialWidth = 54.dp

    // Animate width fill
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanding) containerWidthDp else pillInitialWidth,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "confirmWidth"
    )

    // Animate text alpha transitions
    val confirmTextAlpha by animateFloatAsState(
        targetValue = if (isExpanding) 0f else 1f,
        animationSpec = tween(300),
        label = "confirmAlpha"
    )
    val confirmedTextAlpha by animateFloatAsState(
        targetValue = if (isExpanding) 1f else 0f,
        animationSpec = tween(500, delayMillis = 300),
        label = "confirmedAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .onSizeChanged { containerWidthPx = it.width }
    ) {
        // Drawing area
        Column(
            Modifier
                .fillMaxWidth()
                .padding(end = pillInitialWidth + 8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 6.dp)
            )

            AndroidView(
                factory = { signaturePad },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(start = 12.dp, end = 6.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent)
            )
        }

        // Expanding confirm overlay
        Box(
            modifier = Modifier
                .zIndex(2f)
                .align(Alignment.CenterEnd)
                .height(220.dp)
                .width(animatedWidth)
                .clip(
                    RoundedCornerShape(
                        topStart = if (animatedWidth < containerWidthDp) 20.dp else 18.dp,
                        bottomStart = if (animatedWidth < containerWidthDp) 20.dp else 18.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .background(if (hasSignature) confirmColor else disabledConfirmColor)
                .clickable(
                    enabled = hasSignature && !isExpanding,
                    onClick = {
                        val whiteBitmap = signaturePad.transparentSignatureBitmap

                        // 🔹 Convert white strokes → black
                        val blackBitmap = Bitmap.createBitmap(
                            whiteBitmap.width,
                            whiteBitmap.height,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(blackBitmap)
                        canvas.drawColor(android.graphics.Color.WHITE) // white background

                        val paint = android.graphics.Paint().apply {
                            colorFilter = android.graphics.ColorMatrixColorFilter(
                                android.graphics.ColorMatrix().apply {
                                    // invert white to black
                                    set(
                                        floatArrayOf(
                                            -1f,  0f,  0f,  0f, 255f, // R
                                            0f, -1f,  0f,  0f, 255f, // G
                                            0f,  0f, -1f,  0f, 255f, // B
                                            0f,  0f,  0f,  1f,   0f  // A
                                        )
                                    )
                                }
                            )
                        }
                        canvas.drawBitmap(whiteBitmap, 0f, 0f, paint)

                        // 🔹 Encode black version to Base64
                        val baos = ByteArrayOutputStream()
                        blackBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        onConfirmBase64(b64)

                        isExpanding = true
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isExpanding) {
                Text(
                    text = "Confirm",
                    color = Color.White.copy(alpha = confirmTextAlpha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(90f)
                )
            }

            // Step 2: Centered "Confirmed" text (visible during expansion)
            if (isExpanding) {
                Text(
                    text = "Confirmed",
                    color = Color.White.copy(alpha = confirmedTextAlpha),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}







