package com.assentify.sdk.Core.Constants

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** Text Color **/

/** Style **/
sealed class BackgroundStyle {
    data class Solid(val hex: String) : BackgroundStyle()

    data class Gradient(
        val colorsHex: List<String>,
        val angleDegrees: Float = 90f,
        val holdUntil: Float = 0.4f
    ) : BackgroundStyle()
}


fun Color.Companion.fromHex(hex: String): Color {
    val clean = hex.trim().removePrefix("#")
    val argb = when (clean.length) {
        6 -> "FF$clean"
        8 -> clean
        else -> error("Invalid hex: $hex")
    }
    return Color(argb.toLong(16))
}


fun BackgroundStyle.toBrush(): Brush = when (this) {
    is BackgroundStyle.Solid -> SolidColor(Color.fromHex(hex))

    is BackgroundStyle.Gradient -> {
        val first = Color.fromHex(colorsHex.first())
        val last = Color.fromHex(colorsHex.last())

        val t = holdUntil.coerceIn(0f, 1f)

        // If you only need vertical/horizontal, do it like this:
        if (angleDegrees == 90f) {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to first,
                    t to first,     // stay first color until t (ex: 0.8)
                    1.0f to last
                )
            )
        } else if (angleDegrees == 0f) {
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to first,
                    t to first,
                    1.0f to last
                )
            )
        } else {
            // Fallback: smooth linear gradient for other angles (no stops math here)
            Brush.linearGradient(colorsHex.map { Color.fromHex(it) })
        }
    }
}

/** First Color **/
fun BackgroundStyle.firstColor(): Color = when (this) {
    is BackgroundStyle.Solid -> Color.fromHex(hex)
    is BackgroundStyle.Gradient -> Color.fromHex(colorsHex.first())
}


/** Field Color **/
fun Color.darken(factor: Float = 0.6f): Color {
    return copy(
        red = (red * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue = (blue * factor).coerceIn(0f, 1f)
    )
}


/** Flow Environmental Conditions **/

enum class BackgroundType(val type : String) {
    Image("Image"),
    Color("Color")
}

enum class StepperType(val type : String) {
    Normal("Normal"),
    PercentageBased("PercentageBased")
}

public class FlowEnvironmentalConditions(
    var logoUrl: String = "",
    var svgBackgroundImageUrl: String = "",
    var textColor: String = "",
    var secondaryTextColor: String = "",
    var backgroundCardColor: String = "",
    var accentColor: String = "",
    var backgroundColor: BackgroundStyle? = null,
    var clickColor: BackgroundStyle? = null,
    var backgroundType: BackgroundType?,


    val stepperType: StepperType = StepperType.Normal,
    var rangeStart: Int = 25,// Next
    var rangeEnd : Int = 50, // Next
    var stepperTitle : String = "Identification",

    val language: String = Language.NON,
    val enableNfc: Boolean = false,
    val enableQr: Boolean = false,
    val showCountDown: Boolean = true,
    val blockLoaderCustomProperties: Map<String, Any> = emptyMap(),
) {

    init {
        require(backgroundType != null) { "appLogo is required" }
    }

}


