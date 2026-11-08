package com.assentify.sdk.tflite.LivenessDetection.Finder

import android.graphics.Bitmap
import java.nio.charset.Charset
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The PDF417 barcode on the back of the Lebanese ID card (02-core-spec/checks/barcode-qr.md).
 *
 *  - Images to read it from (capture/barcode-prep.ts): the card straightened from the FULL camera frame, the band that holds
 *    the barcode cut out 1712 px wide, plus a sharpened and a thresholded copy; the lower band first (its place), then the
 *    upper one (a card held upside down). Six tries in order.
 *  - Its content (core/id-back.ts): windows-1256 text, fields split at commas; the card number is the first 12-digit field.
 *
 * Recorded only: never a verdict. Pure Kotlin; the reading itself is done by the platform reader (ML Kit PDF417).
 */
object IdBackBarcode {

    /** The card's shape, width to height (85.6 x 54 mm). */
    const val CARD_ASPECT = 85.6 / 54
    const val TRY_WIDTH = 1712

    /** A grey image, one value 0..255 per pixel. */
    class Gray(@JvmField val width: Int, @JvmField val height: Int, @JvmField val data: IntArray)

    /** The ID back's barcode taken apart. */
    class Content(
        @JvmField val text: String,              // the whole content, windows-1256 (every byte one character)
        @JvmField val fields: List<String>,      // split at commas, trimmed; fields that are not text are kept in place
        @JvmField val number: String?,           // the first field of exactly 12 digits (the number on the front), or null
    ) {
        /** Only the text fields (Arabic letters, digits, spaces, / . - ( )): what can be shown or compared. */
        fun textFields(): List<String> = fields.filter { isTextField(it) }
    }

    // =============================================================================================
    // Images (barcode-prep.ts)
    // =============================================================================================

    /** toGray: (77 R + 150 G + 29 B) >> 8 of every pixel. */
    @JvmStatic
    fun toGray(frame: Bitmap): Gray {
        val w = frame.width
        val h = frame.height
        val px = IntArray(w * h)
        frame.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val p = px[i]
            px[i] = (((p shr 16) and 255) * 77 + ((p shr 8) and 255) * 150 + (p and 255) * 29) shr 8
        }
        return Gray(w, h, px)
    }

    /** The six tries: lower band (0.40..1.02) plain, sharpened, thresholded; then upper band (-0.02..0.60) the same. */
    @JvmStatic
    @JvmOverloads
    fun tries(g: Gray, quad: DoubleArray, width: Int = TRY_WIDTH): List<Gray> {
        val out = ArrayList<Gray>(6)
        for (band in arrayOf(doubleArrayOf(0.4, 1.02), doubleArrayOf(-0.02, 0.6))) {
            val b = cardBand(g, quad, width, band[0], band[1])
            out.add(b)
            out.add(sharpen(b, 4))
            out.add(threshold(b, 15, 5.0))
        }
        return out
    }

    /**
     * The band of the card from `from` to `to` of its height (0 = top), straightened and `width` px wide, bilinear;
     * outside the picture = 255. quad: the card's corners in the picture, top left first, clockwise.
     */
    @JvmStatic
    fun cardBand(g: Gray, quad: DoubleArray, width: Int, from: Double, to: Double): Gray {
        val hq = unitToQuad(quad)
        val cardH = width / CARD_ASPECT
        val height = max(1, ((to - from) * cardH).roundToInt())
        val out = IntArray(width * height)
        val w = g.width
        val hh = g.height
        val d = g.data
        for (y in 0 until height) {
            val v = from + (y + 0.5) / cardH
            for (x in 0 until width) {
                val u = (x + 0.5) / width
                val ww = hq[6] * u + hq[7] * v + hq[8]
                val px = (hq[0] * u + hq[1] * v + hq[2]) / ww - 0.5
                val py = (hq[3] * u + hq[4] * v + hq[5]) / ww - 0.5
                val ix = floor(px).toInt()
                val iy = floor(py).toInt()
                if (ix < 0 || iy < 0 || ix + 1 >= w || iy + 1 >= hh) { out[y * width + x] = 255; continue }
                val fx = px - ix
                val fy = py - iy
                val i = iy * w + ix
                val top = d[i] + (d[i + 1] - d[i]) * fx
                val bottom = d[i + w] + (d[i + w + 1] - d[i + w]) * fx
                out[y * width + x] = clamp8(top + (bottom - top) * fy)
            }
        }
        return Gray(width, height, out)
    }

    /** Sharpened: 1.8 x the picture less 0.8 x its box blur of radius r. */
    @JvmStatic
    fun sharpen(g: Gray, r: Int): Gray {
        val m = boxMean(g, r)
        return Gray(g.width, g.height, IntArray(g.data.size) { i -> clamp8(1.8 * g.data[i] - 0.8 * m[i]) })
    }

    /** Black and white against the local mean: darker than its surroundings by more than c turns black. */
    @JvmStatic
    fun threshold(g: Gray, r: Int, c: Double): Gray {
        val m = boxMean(g, r)
        return Gray(g.width, g.height, IntArray(g.data.size) { i -> if (g.data[i] < m[i] - c) 0 else 255 })
    }

    /** The grey image as an opaque bitmap, for the barcode reader. */
    @JvmStatic
    fun toBitmap(g: Gray): Bitmap {
        val px = IntArray(g.data.size) { i -> val v = g.data[i]; (0xFF shl 24) or (v shl 16) or (v shl 8) or v }
        return Bitmap.createBitmap(px, g.width, g.height, Bitmap.Config.ARGB_8888)
    }

    /** The mean of each point's square neighbourhood of radius r (summed-area table), stored as 32-bit floats. */
    private fun boxMean(g: Gray, r: Int): FloatArray {
        val w = g.width
        val h = g.height
        val sat = DoubleArray((w + 1) * (h + 1))
        for (y in 0 until h) {
            var row = 0.0
            for (x in 0 until w) {
                row += g.data[y * w + x]
                sat[(y + 1) * (w + 1) + x + 1] = sat[y * (w + 1) + x + 1] + row
            }
        }
        val out = FloatArray(w * h)
        for (y in 0 until h) {
            val y0 = max(0, y - r)
            val y1 = min(h, y + r + 1)
            for (x in 0 until w) {
                val x0 = max(0, x - r)
                val x1 = min(w, x + r + 1)
                val s = sat[y1 * (w + 1) + x1] - sat[y0 * (w + 1) + x1] - sat[y1 * (w + 1) + x0] + sat[y0 * (w + 1) + x0]
                out[y * w + x] = (s / ((y1 - y0) * (x1 - x0))).toFloat()
            }
        }
        return out
    }

    /** The homography taking (u, v) in the unit square to the quad's corners (TL, TR, BR, BL). */
    private fun unitToQuad(q: DoubleArray): DoubleArray {
        val x0 = q[0]; val y0 = q[1]; val x1 = q[2]; val y1 = q[3]; val x2 = q[4]; val y2 = q[5]; val x3 = q[6]; val y3 = q[7]
        val dx1 = x1 - x2; val dx2 = x3 - x2; val dy1 = y1 - y2; val dy2 = y3 - y2
        val sx = x0 - x1 + x2 - x3; val sy = y0 - y1 + y2 - y3
        val den = dx1 * dy2 - dx2 * dy1
        val g = if (den == 0.0) 0.0 else (sx * dy2 - dx2 * sy) / den
        val h = if (den == 0.0) 0.0 else (dx1 * sy - sx * dy1) / den
        return doubleArrayOf(x1 - x0 + g * x1, x3 - x0 + h * x3, x0, y1 - y0 + g * y1, y3 - y0 + h * y3, y0, g, h, 1.0)
    }

    /** Uint8ClampedArray: clamp to 0..255, round half to even, NaN = 0. */
    private fun clamp8(v: Double): Int = if (v.isNaN()) 0 else Math.rint(min(255.0, max(0.0, v))).toInt()

    // =============================================================================================
    // Content (id-back.ts)
    // =============================================================================================

    private val CP1256: Charset = Charset.forName("windows-1256")
    private val TWELVE_DIGITS = Regex("^\\d{12}$")
    private val TEXT_FIELD = Regex("^[\u0621-\u064A\u0660-\u0669\u067E\u0686\u06A9\u06AF\u06CC0-9 /.\\-()]*$")

    /** The barcode's bytes as text (windows-1256). */
    @JvmStatic
    fun decode(bytes: ByteArray): String = String(bytes, CP1256)

    /** The content taken apart, or null if it holds no comma-separated fields. */
    @JvmStatic
    fun read(text: String): Content? {
        val fields = text.split(",").map { it.trim() }                // keeps empty fields, as JavaScript's split
        if (fields.size < 2) return null
        return Content(text, fields, fields.firstOrNull { TWELVE_DIGITS.matches(it) })
    }

    /** Is this field text (Arabic letters, digits, spaces and the punctuation of dates and names)? */
    @JvmStatic
    fun isTextField(field: String): Boolean = TEXT_FIELD.matches(field)

    /** Bytes as base64, for handing the raw content on. */
    @JvmStatic
    fun toBase64(bytes: ByteArray): String {                          // no java.util.Base64: it needs API 26
        val a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val sb = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 255
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 255 else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 255 else -1
            sb.append(a[b0 shr 2])
            sb.append(a[((b0 and 3) shl 4) or (if (b1 < 0) 0 else b1 shr 4)])
            sb.append(if (b1 < 0) '=' else a[((b1 and 15) shl 2) or (if (b2 < 0) 0 else b2 shr 6)])
            sb.append(if (b2 < 0) '=' else a[b2 and 63])
            i += 3
        }
        return sb.toString()
    }
}