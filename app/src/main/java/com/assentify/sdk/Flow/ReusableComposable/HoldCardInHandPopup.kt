package com.assentify.sdk.Flow.ReusableComposable



import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings

// The drawing's canvas (all four layer SVGs are 220 x 100) and the motion, in its units.
private const val ART_W = 220f
private const val ART_H = 100f
private const val CARD_PIVOT_X = 70f                                // the card's bottom-left corner: it lifts from there
private const val CARD_PIVOT_Y = 86f
private const val CARD_LIFT = 10f                                   // how far the card rises
private const val CARD_TILT_DEG = -16f                              // and tips (the right end comes up)
private const val ARROW_FROM = 4f                                   // the arrow bobs from +4 to -6
private const val ARROW_TO = -6f
private const val CYCLE_MS = 900                                    // one way; the loop goes up and back down

/**
 * Shown on top of the camera before the card scan starts: hold the card in your hand, not on a table.
 * The camera behind it is dimmed and does not take taps; the scan starts when Continue is pressed.
 *
 * The drawing is four SVG layers in assets, white, each tinted from BaseTheme and animated here
 * (Android does not play SVG animation):
 *   held-card-table.svg  BaseTextColor at 45 %   still
 *   held-card-card.svg   BaseTextColor           lifts and tips
 *   held-card-lines.svg  BaseAccentColor         moves with the card
 *   held-card-arrow.svg  BaseAccentColor         bobs up and brightens
 * Without the assets, the same drawing is painted in code.
 *
 * The texts come from the caller (any language, any document); each defaults to FlowStrings when not given:
 *   title       "Hold the card in your hand"
 *   message     "Pick it up off the table, then continue."   (empty string = no message line)
 *   buttonText  "Continue"
 */
@Composable
fun HoldCardInHandPopup(
    title: String? = null,
    message: String? = null,
    buttonText: String? = null,
    onContinue: () -> Unit,
) {
    val s = flowStrings()
    val titleText = title ?: s.holdCardTitle
    val messageText = message ?: s.holdCardMsg
    val continueText = buttonText ?: s.continueButton
    val context = LocalContext.current
    val accent = remember(BaseTheme.BaseAccentColor) {
        Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
    }
    val layers = remember {
        HeldCardLayers(
            table = loadSvgFromAssets(context, "held-card-table.svg"),
            card = loadSvgFromAssets(context, "held-card-card.svg"),
            lines = loadSvgFromAssets(context, "held-card-lines.svg"),
            arrow = loadSvgFromAssets(context, "held-card-arrow.svg"),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(                                             // swallows taps on the camera behind
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(BaseTheme.FieldColor)
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            HeldCardAnimation(
                layers = layers,
                lineColor = BaseTheme.BaseTextColor,
                accent = accent
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = titleText,
                color = BaseTheme.BaseTextColor,
                fontSize = 22.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (messageText.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))

                Text(
                    text = messageText,
                    color = BaseTheme.BaseTextColor.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = BaseTheme.BaseClickColor!!.toBrush(),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Text(
                    continueText,
                    color = BaseTheme.BaseSecondaryTextColor,
                    fontFamily = InterFont,
                    fontWeight = BaseTheme.BaseClickFontWeight,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 7.dp)
                )
            }
        }
    }
}

/** The four layers from assets (null when a file is missing). */
private class HeldCardLayers(val table: Painter?, val card: Painter?, val lines: Painter?, val arrow: Painter?) {
    val complete get() = table != null && card != null && lines != null && arrow != null
}

/** The card lifts off the table and tips up; the arrow bobs and brightens. Loops. */
@Composable
private fun HeldCardAnimation(layers: HeldCardLayers, lineColor: Color, accent: Color) {
    val transition = rememberInfiniteTransition(label = "heldCard")
    val p by transition.animateFloat(                               // 0 = flat on the table, 1 = lifted
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lift"
    )

    val artModifier = Modifier
        .width(200.dp)
        .aspectRatio(ART_W / ART_H)

    if (!layers.complete) {
        HeldCardDrawn(p, lineColor, accent, artModifier)
        return
    }

    // every layer fills the same box, so the SVGs line up exactly as they were drawn
    val cardMotion = Modifier.graphicsLayer {
        transformOrigin = TransformOrigin(CARD_PIVOT_X / ART_W, CARD_PIVOT_Y / ART_H)
        rotationZ = CARD_TILT_DEG * p
        translationY = -CARD_LIFT / ART_H * size.height * p
    }
    val arrowMotion = Modifier.graphicsLayer {
        translationY = (ARROW_FROM + (ARROW_TO - ARROW_FROM) * p) / ART_H * size.height
        alpha = 0.55f + 0.45f * p
    }

    Box(modifier = artModifier) {
        Layer(layers.table!!, lineColor.copy(alpha = 0.45f), Modifier)
        Layer(layers.card!!, lineColor, cardMotion)
        Layer(layers.lines!!, accent, cardMotion)
        Layer(layers.arrow!!, accent, arrowMotion)
    }
}

@Composable
private fun Layer(painter: Painter, tint: Color, motion: Modifier) {
    Icon(
        painter = painter,
        contentDescription = null,
        tint = tint,                                                // white SVG -> the theme colour
        modifier = Modifier.fillMaxSize().then(motion)
    )
}

/** The same drawing painted in code (used only when the assets are missing). */
@Composable
private fun HeldCardDrawn(p: Float, lineColor: Color, accent: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val k = size.width / ART_W                                  // art units -> px
        val stroke = 4f * k

        // table
        drawLine(lineColor.copy(alpha = 0.45f), Offset(30f * k, 88f * k), Offset(190f * k, 88f * k),
            strokeWidth = 3f * k, cap = StrokeCap.Round)

        // card + its lines: rotate about the bottom-left corner, then lift
        translate(top = -CARD_LIFT * k * p) {
            rotate(degrees = CARD_TILT_DEG * p, pivot = Offset(CARD_PIVOT_X * k, CARD_PIVOT_Y * k)) {
                drawRoundRect(lineColor, topLeft = Offset(70f * k, 50f * k), size = Size(58f * k, 36f * k),
                    cornerRadius = CornerRadius(6f * k), style = Stroke(width = stroke))
                drawLine(accent, Offset(80f * k, 61f * k), Offset(104f * k, 61f * k), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(accent, Offset(80f * k, 71f * k), Offset(96f * k, 71f * k), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }

        // arrow
        val dy = (ARROW_FROM + (ARROW_TO - ARROW_FROM) * p) * k
        val a = accent.copy(alpha = accent.alpha * (0.55f + 0.45f * p))
        drawLine(a, Offset(152f * k, 80f * k + dy), Offset(152f * k, 24f * k + dy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(a, Offset(152f * k, 24f * k + dy), Offset(141f * k, 35f * k + dy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(a, Offset(152f * k, 24f * k + dy), Offset(163f * k, 35f * k + dy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}