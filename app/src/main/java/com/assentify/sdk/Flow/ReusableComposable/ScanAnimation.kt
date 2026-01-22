
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import kotlin.math.max

@Composable
fun ScanAnimation(
    sourceIconName: String,
    modifier: Modifier = Modifier,
    width: Dp =250.dp,
    cornerRadius: Dp =0.dp,
    durationMs: Int = 1200,
) {
    val context = LocalContext.current
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    // Your loader returns Painter?
    val bgPainter = remember(sourceIconName) { loadSvgFromAssets(context, sourceIconName) }
    val barPainter = remember { loadSvgFromAssets(context, "ic_bar.svg") }

    var boxWpx by remember { mutableStateOf(0) }
    var boxHpx by remember { mutableStateOf(0) }

    val transition = rememberInfiniteTransition(label = "scan")

    // bar moves from top -> bottom -> top (in PX for perfect bounds)
    val yPx by transition.animateFloat(
        initialValue = 0f,
        targetValue = max(0f, boxHpx.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yPx"
    )

    val a by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)                // ✅ EXACT like GIF (800x450)
            .clip(RoundedCornerShape(cornerRadius))
            .graphicsLayer { clip = true }
            .onSizeChanged {
                boxWpx = it.width
                boxHpx = it.height
            }
    ) {
        // Background (fills whole 16:9 box)
        bgPainter?.let {
            Image(
                painter = it,
                colorFilter = ColorFilter.tint(
                    Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bar: keep height as-is, but scale width to match background width
        barPainter?.let { bar ->
            val barW = bar.intrinsicSize.width
            val scaleX =
                if (barW.isFinite() && barW > 0f && boxWpx > 0)
                    (boxWpx / barW).coerceAtLeast(1f)
                else 1f

            Image(
                painter = bar,
                colorFilter = ColorFilter.tint(
                        Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                ),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = yPx
                        this.scaleX = scaleX
                        this.scaleY = 1.8f
                    }
                    .alpha(a)
            )
        }
    }
}
