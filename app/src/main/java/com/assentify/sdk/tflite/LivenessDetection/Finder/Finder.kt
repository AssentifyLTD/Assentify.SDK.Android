package com.assentify.sdk.tflite.LivenessDetection.Finder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Which of the model's 4 heads to read. Pick the one for the document you are capturing. */
enum class DocHead(val index: Int) {
    CARD(0),            // ID cards, licences, any card
    PASSPORT_PAGE(1),   // passport photo page
    PASSPORT_SPREAD(2), // whole open passport (optional assist)
    A4(3)               // A4 sheets
}

data class FinderResult(
    /** true if a document was found (at least 3 sure corners). */
    val found: Boolean,
    /** Final outline TL, TR, BR, BL in bitmap pixels, or null if no document. */
    val corners: List<PointF>?,
    /** Decoded position of every corner, even unsure ones. */
    val rawCorners: List<PointF>,
    /** Peak score (0..1) of each corner TL, TR, BR, BL. A corner is sure when >= 0.5. */
    val peaks: List<Float>,
    /** Index (0..3) of a corner that was filled in from the other three, or null. */
    val inferredCorner: Int?
)

/**
 * Runs Finder_fp32.tflite on a Bitmap.
 *
 * Usage:
 *   val finder = Finder(context)                 // once
 *   val result = finder.detect(bitmap, DocHead.CARD)
 *   if (result.found) { result.corners!! ... }
 *   finder.close()                                // when done
 *
 * Not thread-safe: use one instance per thread, and call it off the main thread.
 */
class Finder(
    context: Context,
    modelAsset: String,
    numThreads: Int
) : AutoCloseable {

    companion object {
        const val IN = 160      // input is 160 x 160 x 3
        const val GRID = 40     // output grid is 40 x 40
        const val CH = 48       // 4 heads x 12 maps
        const val SURE = 0.5f

        /** Decodes the NHWC maps for one head into corners in a width x height frame. */
        fun decode(maps: FloatArray, head: DocHead, width: Int, height: Int): FinderResult {
            val base = head.index * 12
            val pts = ArrayList<PointF>(4)
            val peaks = ArrayList<Float>(4)

            for (c in 0 until 4) {
                val ch = base + c
                // argmax over the 1600 cells (first maximum wins)
                var at = 0
                var best = maps[ch]
                for (cell in 1 until GRID * GRID) {
                    val v = maps[cell * CH + ch]
                    if (v > best) { best = v; at = cell }
                }
                val gx = at % GRID
                val gy = at / GRID
                val ox = maps[at * CH + base + 4 + 2 * c]
                val oy = maps[at * CH + base + 5 + 2 * c]
                val x = (gx + 0.5f + (ox - 0.5f) * 2f) / GRID * width
                val y = (gy + 0.5f + (oy - 0.5f) * 2f) / GRID * height
                pts.add(PointF(x, y))
                peaks.add(best)
            }

            val unsure = (0 until 4).filter { peaks[it] < SURE }
            return when (unsure.size) {
                0 -> FinderResult(true, pts.map { PointF(it.x, it.y) }, pts, peaks, null)
                1 -> {
                    // One corner hidden (e.g. a thumb): use the parallelogram's 4th corner.
                    val i = unsure[0]
                    val a = pts[(i + 3) % 4]
                    val b = pts[(i + 1) % 4]
                    val o = pts[(i + 2) % 4]
                    val fixed = pts.map { PointF(it.x, it.y) }.toMutableList()
                    fixed[i] = PointF(a.x + b.x - o.x, a.y + b.y - o.y)
                    FinderResult(true, fixed, pts, peaks, i)
                }
                else -> FinderResult(false, null, pts, peaks, null)
            }
        }
    }

    private val interpreter: Interpreter
    private val input = ByteBuffer.allocateDirect(IN * IN * 3 * 4).order(ByteOrder.nativeOrder())
    private val output = ByteBuffer.allocateDirect(GRID * GRID * CH * 4).order(ByteOrder.nativeOrder())

    /** Raw output of the last run: 40 x 40 x 48, NHWC. maps[(y*40 + x)*48 + ch]. */
    val maps = FloatArray(GRID * GRID * CH)

    init {
        val options = Interpreter.Options().setNumThreads(numThreads)
        interpreter = Interpreter(loadModel(context, modelAsset), options)
    }

    /** Runs the model and decodes one head. Corners are in the bitmap's pixel coordinates. */
    fun detect(bitmap: Bitmap, head: DocHead): FinderResult {
        run(bitmap)
        return decode(maps, head, bitmap.width, bitmap.height)
    }

    /** Runs the model only. Returns the raw maps (the same array every call). */
    fun run(bitmap: Bitmap): FloatArray {
        val src = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
        else bitmap.copy(Bitmap.Config.ARGB_8888, false) // e.g. HARDWARE bitmaps

        input.rewind()
        input.asFloatBuffer().put(resizeArea(src, IN, IN))
        input.rewind()
        output.rewind()
        interpreter.run(input, output)
        output.rewind()
        output.asFloatBuffer().get(maps)
        return maps
    }

    override fun close() = interpreter.close()

    private fun loadModel(context: Context, name: String): MappedByteBuffer =
        context.assets.openFd(name).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }

    /**
     * Squashes the whole bitmap to dw x dh with area averaging (like OpenCV INTER_AREA),
     * as RGB floats in 0..1, NHWC. No crop, no letterbox, no mean subtraction.
     */
    private fun resizeArea(bmp: Bitmap, dw: Int, dh: Int): FloatArray {
        val sw = bmp.width
        val sh = bmp.height
        val px = IntArray(sw * sh)
        bmp.getPixels(px, 0, sw, 0, 0, sw, sh)

        val xw = areaWeights(sw, dw)
        val yw = areaWeights(sh, dh)

        // Horizontal pass: sh rows x dw columns x RGB
        val tmp = FloatArray(sh * dw * 3)
        for (y in 0 until sh) {
            val row = y * sw
            for (dx in 0 until dw) {
                val idx = xw[dx].first
                val w = xw[dx].second
                var r = 0f; var g = 0f; var b = 0f
                for (k in idx.indices) {
                    val p = px[row + idx[k]]
                    val wk = w[k]
                    r += ((p shr 16) and 0xFF) * wk
                    g += ((p shr 8) and 0xFF) * wk
                    b += (p and 0xFF) * wk
                }
                val o = (y * dw + dx) * 3
                tmp[o] = r; tmp[o + 1] = g; tmp[o + 2] = b
            }
        }

        // Vertical pass, then round to 0..255 (as OpenCV does for 8-bit images) and scale to 0..1
        val out = FloatArray(dw * dh * 3)
        for (dy in 0 until dh) {
            val idx = yw[dy].first
            val w = yw[dy].second
            for (dx in 0 until dw) {
                var r = 0f; var g = 0f; var b = 0f
                for (k in idx.indices) {
                    val o = (idx[k] * dw + dx) * 3
                    val wk = w[k]
                    r += tmp[o] * wk; g += tmp[o + 1] * wk; b += tmp[o + 2] * wk
                }
                val o = (dy * dw + dx) * 3
                out[o] = r.roundToInt().coerceIn(0, 255) / 255f
                out[o + 1] = g.roundToInt().coerceIn(0, 255) / 255f
                out[o + 2] = b.roundToInt().coerceIn(0, 255) / 255f
            }
        }
        return out
    }

    /** For each output index: the source indices it covers and their weights (summing to 1). */
    private fun areaWeights(srcLen: Int, dstLen: Int): Array<Pair<IntArray, FloatArray>> {
        val scale = srcLen.toDouble() / dstLen
        return Array(dstLen) { d ->
            val start = d * scale
            val end = (d + 1) * scale
            val first = floor(start).toInt()
            val last = min(ceil(end).toInt(), srcLen) - 1
            val idx = IntArray(last - first + 1) { first + it }
            val w = FloatArray(idx.size) { i ->
                val s = idx[i].toDouble()
                ((min(end, s + 1.0) - max(start, s)) / scale).toFloat()
            }
            idx to w
        }
    }

    /// Test

    /**
     * Crops the document out of the bitmap and straightens it.
     * corners must be TL, TR, BR, BL in bitmap pixels (as returned by Finder2).
     */
    fun cropDocument(src: Bitmap, corners: List<PointF>): Bitmap? {
        val (tl, tr, br, bl) = corners

        // Output size: the longer of each pair of opposite edges
        val outW = max(distance(tl, tr), distance(bl, br)).roundToInt()
        val outH = max(distance(tl, bl), distance(tr, br)).roundToInt()
        if (outW < 1 || outH < 1) return null

        val srcPts = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )
        val dstPts = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        if (!matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)) return null

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        Canvas(out).drawBitmap(src, matrix, paint)
        return out
    }

    private fun distance(a: PointF, b: PointF): Float =
        hypot(a.x - b.x, a.y - b.y)
}