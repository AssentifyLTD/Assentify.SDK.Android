package com.assentify.sdk.doccapture

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * STEP 1 — Geometry. Port of 08-reference-source/core/geometry.ts.
 *
 * A quad is DoubleArray(8) = [x0,y0, x1,y1, x2,y2, x3,y3], corners in order TL, TR, BR, BL,
 * in UPRIGHT camera-frame pixels (CAM space), origin top-left, x right, y down.
 *
 * Verified: poseOf matches all 320 poseOf lines of 07-conformance/vectors/core/geometry.jsonl exactly
 * (only when using StrictMath + jsHypot below; Math.log / Math.hypot give last-digit differences).
 */
data class Pose(
    val lr: Double,      // ln(left/right):  > 0 left edge nearer the camera
    val tb: Double,      // ln(top/bottom):  > 0 top edge nearer the camera
    val area: Double,    // px²
    val cx: Double, val cy: Double,   // centre
    val angle: Double,   // in-plane rotation, radians
    val aspect: Double,  // (top+bottom)/(left+right)  ≈ long/short for a landscape card
)

/** JavaScript Math.round (half toward +∞). Kotlin round() is half-to-even → wrong. */
fun jsRound(x: Double): Double { val f = floor(x); return if (x - f >= 0.5) f + 1 else f }

/** JavaScript (V8) Math.hypot: Kahan-compensated, scaled by the max. java.lang.Math.hypot differs in the last bit. */
fun jsHypot(a: Double, b: Double): Double {
    if (a.isInfinite() || b.isInfinite()) return Double.POSITIVE_INFINITY
    if (a.isNaN() || b.isNaN()) return Double.NaN
    val x = abs(a); val y = abs(b)
    val m = max(x, y)
    if (m == 0.0) return 0.0
    var sum = 0.0; var comp = 0.0
    for (v in doubleArrayOf(x, y)) {
        val n = v / m
        val summand = n * n - comp
        val prelim = sum + summand
        comp = (prelim - sum) - summand
        sum = prelim
    }
    return sqrt(sum) * m
}

object Geometry {
    // corner indices
    private const val TL = 0; private const val TR = 1; private const val BR = 2; private const val BL = 3

    private fun dist(q: DoubleArray, a: Int, b: Int) = jsHypot(q[2 * a] - q[2 * b], q[2 * a + 1] - q[2 * b + 1])

    /** cross(o, a, b) = (a.x-o.x)(b.y-o.y) - (a.y-o.y)(b.x-o.x) */
    private fun cross(q: DoubleArray, o: Int, a: Int, b: Int) =
        (q[2 * a] - q[2 * o]) * (q[2 * b + 1] - q[2 * o + 1]) - (q[2 * a + 1] - q[2 * o + 1]) * (q[2 * b] - q[2 * o])

    /** The pose of the document from its 4 corners. Not calibrated angles — relative proxies. */
    @JvmStatic
    fun poseOf(q: DoubleArray): Pose {
        val left = dist(q, TL, BL); val right = dist(q, TR, BR)
        val top = dist(q, TL, TR); val bottom = dist(q, BL, BR)
        return Pose(
            lr = StrictMath.log(left / right),
            tb = StrictMath.log(top / bottom),
            area = abs(cross(q, TL, TR, BR) + cross(q, TL, BR, BL)) / 2,
            cx = (q[0] + q[2] + q[4] + q[6]) / 4,
            cy = (q[1] + q[3] + q[5] + q[7]) / 4,
            angle = StrictMath.atan2(q[3] - q[1], q[2] - q[0]),
            aspect = (top + bottom) / (left + right),
        )
    }

    /**
     * Can this outline be the document at all?
     *  - every corner finite and at least frameMarginPx inside the picture
     *    (during sweep/return pass outside = cfg.tiltOverflow: corners may go a bit outside)
     *  - convex and not self-crossing
     *  - big enough: area >= minQuadAreaFrac * w * h
     */
    @JvmStatic @JvmOverloads
    fun isValidQuad(q: DoubleArray, w: Double, h: Double, frameMarginPx: Double, minQuadAreaFrac: Double,
                    outside: Double = 0.0): Boolean {
        val m = frameMarginPx - outside * w          // outside is a share of the WIDTH for x and y
        for (i in 0 until 4) {
            val x = q[2 * i]; val y = q[2 * i + 1]
            if (!x.isFinite() || !y.isFinite()) return false
            if (x < m || y < m || x > w - m || y > h - m) return false
        }
        val turns = DoubleArray(4) { cross(q, it, (it + 1) % 4, (it + 2) % 4) }
        if (!(turns.all { it > 0 } || turns.all { it < 0 })) return false
        return poseOf(q).area >= minQuadAreaFrac * w * h
    }

    /**
     * Only DRAW the outline before the steps when this is true:
     * flat (|lr|, |tb| <= frontalMax) and the right proportions (aspect within shapeTol of the document's).
     */
    @JvmStatic
    fun looksRight(q: DoubleArray, docAspect: Double, frontalMax: Double, shapeTol: Double): Boolean {
        val p = poseOf(q)
        return abs(p.lr) <= frontalMax && abs(p.tb) <= frontalMax && abs(p.aspect / docAspect - 1) <= shapeTol
    }

    /** Lebanese passport only (profile.spread): is the page ABOVE the photo page out of the picture? */
    @JvmStatic
    fun upperPageCut(q: DoubleArray, w: Double, h: Double, slack: Double): Boolean {
        val m = slack * (dist(q, TL, BL) + dist(q, TR, BR)) / 2
        fun out(x: Double, y: Double) = x < -m || y < -m || x > w + m || y > h + m
        return out(2 * q[0] - q[6], 2 * q[1] - q[7]) || out(2 * q[2] - q[4], 2 * q[3] - q[5])
    }

    /** Is (x, y) inside the quad (flat [x0,y0..x3,y3])? Used by the civil extract. */
    @JvmStatic
    fun insideQuad(x: Double, y: Double, q: DoubleArray): Boolean {
        var sign = 0.0
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            val c = (q[2 * j] - q[2 * i]) * (y - q[2 * i + 1]) - (q[2 * j + 1] - q[2 * i + 1]) * (x - q[2 * i])
            if (c == 0.0) continue
            if (sign != 0.0 && Math.signum(c) != sign) return false
            sign = Math.signum(c)
        }
        return true
    }
}