package com.assentify.sdk.tflite.LivenessDetection.Finder

import com.assentify.sdk.doccapture.Geometry
import kotlin.math.hypot

/**
 * For an OPEN passport shown with BOTH pages (like the reference's lb_passport "spread" flow).
 *
 * Problem seen on device: finder2's bottom corners sit firmly on the photo page, but a top corner sometimes lands on the
 * TOP OF THE UPPER PAGE instead of the fold (both pages are about the same height, so that side looks ~2x too long).
 *
 * Fix (package helper, not REF): the expected photo-page height is (bottom edge length) / pageAspect.
 * For each side (left: TL-BL, right: TR-BR):
 *   side ~ 1x expected height (0.70 .. 1.35)  -> keep the top corner (it is on the fold)
 *   side ~ 2x expected height (1.60 .. 2.50)  -> the top corner is on the upper page: move it to the MIDPOINT of the side
 *                                                (= the fold, since the two pages are the same height)
 *   anything else                             -> null (cannot tell where the photo page is this frame)
 *
 * Checked on the device log: e.g. TL(59,224) TR(491,198) BR(474,781) BL(65,797)  (aspect 0.73, both pages)
 *   -> TL(62,510) TR(482,490) BR(474,781) BL(65,797)  aspect 1.44, lr -0.02, tb +0.03  (the photo page).
 *
 * The reverse question, "is the upper page in view?", is REF's Geometry.upperPageCut(quad, w, h, spread.cutMax = 0.05).
 *
 * Java:
 *   double[] page = SpreadFix.photoPage(raw, 1.4204545454545454);    // null = unclear this frame
 *   boolean bothPages = SpreadFix.upperPageInView(page, w, h);        // false -> ask "show both pages"
 */
object SpreadFix {

    @JvmStatic
    fun photoPage(q: DoubleArray, pageAspect: Double): DoubleArray? {
        fun d(a: Int, b: Int) = hypot(q[2 * a] - q[2 * b], q[2 * a + 1] - q[2 * b + 1])
        val bottom = d(3, 2)
        if (bottom < 1.0) return null
        val expected = bottom / pageAspect
        val out = q.copyOf()
        for ((top, bot) in listOf(0 to 3, 1 to 2)) {        // left side TL-BL, right side TR-BR
            val r = d(top, bot) / expected
            when {
                r in 0.70..1.35 -> Unit                           // top corner on the fold: keep
                r in 1.60..2.50 -> {                              // top corner on the upper page: halve the side
                    out[2 * top] = (q[2 * top] + q[2 * bot]) / 2
                    out[2 * top + 1] = (q[2 * top + 1] + q[2 * bot + 1]) / 2
                }
                else -> return null
            }
        }
        return out
    }

    /** true = the page above the photo page fits in the picture (REF upperPageCut with spread.cutMax). */
    @JvmStatic @JvmOverloads
    fun upperPageInView(page: DoubleArray, w: Double, h: Double, cutMax: Double = 0.05): Boolean =
        !Geometry.upperPageCut(page, w, h, cutMax)
}