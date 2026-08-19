package com.assentify.sdk.Flow.ReusableComposable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Base64
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.assentify.sdk.Core.Constants.UiLanguage
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.flowStrings
import java.io.ByteArrayOutputStream

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    title: String = "Signature",
    isLoading: Boolean = false,
    penColorInt: Int = android.graphics.Color.WHITE,
    minStrokeWidth: Float = 3f,
    maxStrokeWidth: Float = 6f,
    // Pass this explicitly if your app manages language state itself
    // (e.g. a saved user preference) rather than relying on the system locale.
    onConfirmBase64: (String) -> Unit,
) {
    val strings = flowStrings()
    val context = LocalContext.current
    val density = LocalDensity.current

    val layoutDirection = if (BaseTheme.BaseUiLanguage == UiLanguage.Arabic) LayoutDirection.Ltr else LayoutDirection.Rtl

    // Everything below reads Start/End (alignment, padding, corner radii),
    // so forcing the direction here is enough to flip the whole layout.
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {

        val signaturePad = remember {
            com.github.gcacace.signaturepad.views.SignaturePad(
                context,
                null
            ).apply {
                setMinWidth(minStrokeWidth)
                setMaxWidth(maxStrokeWidth)
                setPenColor(penColorInt)
                setVelocityFilterWeight(0.9f)
            }
        }

        var hasSignature by remember {
            mutableStateOf(false)
        }

        var isExpanding by remember {
            mutableStateOf(false)
        }

        var containerWidthPx by remember {
            mutableStateOf(0)
        }

        DisposableEffect(signaturePad) {
            signaturePad.setOnSignedListener(
                object :
                    com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener {

                    override fun onStartSigning() = Unit

                    override fun onSigned() {
                        hasSignature = true
                    }

                    override fun onClear() {
                        hasSignature = false
                    }
                }
            )

            onDispose {
                signaturePad.setOnSignedListener(null)
            }
        }

        val containerWidthDp = with(density) {
            containerWidthPx.toDp()
        }

        val confirmInitialWidth = 62.dp

        val animatedWidth by animateDpAsState(
            targetValue = if (isExpanding && containerWidthPx > 0) {
                containerWidthDp
            } else {
                confirmInitialWidth
            },
            animationSpec = tween(
                durationMillis = 600,
                easing = LinearOutSlowInEasing
            ),
            label = "confirmWidth"
        )

        val confirmedTextAlpha by animateFloatAsState(
            targetValue = if (isExpanding) {
                1f
            } else {
                0f
            },
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = 250
            ),
            label = "confirmedTextAlpha"
        )

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(18.dp))
                .background(BaseTheme.BaseClickColor!!.toBrush())
                .onSizeChanged {
                    containerWidthPx = it.width
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center),
                    color = BaseTheme.BaseTextColor,
                    strokeWidth = 4.dp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            // "start" now resolves to the correct visual side
                            // (left for LTR/English, right for RTL/Arabic)
                            start = confirmInitialWidth + 8.dp
                        )
                ) {
                    Text(
                        text = title,
                        color = BaseTheme.BaseTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            start = 14.dp,
                            top = 12.dp,
                            bottom = 6.dp
                        )
                    )

                    AndroidView(
                        factory = {
                            signaturePad
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(
                                start = 12.dp,
                                end = 6.dp,
                                bottom = 12.dp
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Transparent)
                    )
                }

                Box(
                    modifier = Modifier
                        .zIndex(2f)
                        .align(Alignment.CenterStart) // start = left in LTR, right in RTL
                        .fillMaxHeight()
                        .width(animatedWidth)
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = if (isExpanding) {
                                    0.dp
                                } else {
                                    20.dp
                                },
                                bottomEnd = if (isExpanding) {
                                    0.dp
                                } else {
                                    20.dp
                                }
                            )
                        )
                        .background(
                            if (hasSignature) {
                                Color(
                                    android.graphics.Color.parseColor(
                                        BaseTheme.BaseAccentColor
                                    )
                                )
                            } else {
                                BaseTheme.FieldColor
                            }
                        )
                        .clickable(
                            enabled = hasSignature && !isExpanding,
                            onClick = {
                                val base64 = createBlackSignatureBase64(
                                    signatureBitmap =
                                        signaturePad.transparentSignatureBitmap
                                )

                                onConfirmBase64(base64)
                                isExpanding = true
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isExpanding) {
                        Text(
                            text = strings.confirmSignature,
                            color = BaseTheme.BaseTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(
                                if (BaseTheme.BaseUiLanguage == UiLanguage.English) -90f else 90f
                            )
                        )
                    } else {
                        Text(
                            text = strings.confirmedSignature,
                            color = BaseTheme.BaseTextColor.copy(
                                alpha = confirmedTextAlpha
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun createBlackSignatureBase64(
    signatureBitmap: Bitmap
): String {
    val outputBitmap = Bitmap.createBitmap(
        signatureBitmap.width,
        signatureBitmap.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(outputBitmap)

    canvas.drawColor(
        android.graphics.Color.WHITE
    )

    val paint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply {
                set(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        )
    }

    canvas.drawBitmap(
        signatureBitmap,
        0f,
        0f,
        paint
    )

    return ByteArrayOutputStream().use { outputStream ->
        outputBitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            outputStream
        )

        Base64.encodeToString(
            outputStream.toByteArray(),
            Base64.NO_WRAP
        )
    }
}