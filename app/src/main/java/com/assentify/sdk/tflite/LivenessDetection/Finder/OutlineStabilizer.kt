package com.assentify.sdk.tflite.LivenessDetection.Finder


import com.assentify.sdk.doccapture.Geometry
import kotlin.math.hypot

/**
 * INTERIM stabiliser for raw finder2 corners (a package helper, NOT the reference's vision layer).
 *
 * The reference never uses raw model corners: it fits them to real edges and follows them by optical flow
 * (02-core-spec/vision/edge-fit.md, tracking.md), and only accepts a model move when two looks agree (steering.md).
 * Until that is ported, this class removes the worst problems seen on device:
 *
 *  1. SANITY   an outline whose proportions are far from the document's is rejected (e.g. aspect 2.84 for a 1.42 page).
 *  2. AGREE    the first outline, and any JUMP away from the current one, must be seen on 2 frames in a row that agree
 *              (every corner within NEAR x frame diagonal of each other) before it is accepted.
 *              A single odd frame (corners on the fold / the other page) is ignored; the previous outline is kept.
 *  3. INFERRED when finder2 completed a hidden corner (3 of 4 sure), the outline is a parallelogram, so lr = tb = 0
 *              exactly: it carries NO tilt information. It is passed on for position, but poseReliable = false
 *              (do not use it for the flat hold or the tilt).
 *
 * Java:
 *   OutlineStabilizer stab = new OutlineStabilizer(1.4204545454545454);   // profile aspect
 *   OutlineStabilizer.Result s = stab.update(quadOrNull, w, h, inferredCorner != null);
 *   s.getQuad()          // the outline to use this frame, or null (= no document this frame)
 *   s.getPoseReliable()  // false: do not use for "hold flat" / tilt
 *   s.getStatus()        // "accepted", "moved", "held-jump", "pending", "rejected-shape", "none"
 *   stab.reset()         // on every restart of the try
 */
class OutlineStabilizer @JvmOverloads constructor(
    private val docAspect: Double,
    private val near: Double = 0.03,          // REF "near": 0.03 x frame diagonal (vision-core.ts:300)
    private val aspectMin: Double = 0.6,      // accept aspect in [0.6, 1.6] x docAspect (tilt changes aspect a lot)
    private val aspectMax: Double = 1.6,
    private val maxHeldFrames: Int = 3,       // keep the old outline over at most this many odd frames in a row
) {
    data class Result(val quad: DoubleArray?, val poseReliable: Boolean, val status: String)

    private var accepted: DoubleArray? = null
    private var candidate: DoubleArray? = null
    private var held = 0

    fun reset() { accepted = null; candidate = null; held = 0 }

    fun update(q: DoubleArray?, w: Double, h: Double, inferred: Boolean): Result {
        if (q == null) { candidate = null; held = 0; accepted = null; return Result(null, false, "none") }

        val a = Geometry.poseOf(q).aspect
        if (!a.isFinite() || a < aspectMin * docAspect || a > aspectMax * docAspect) {
            return keepOrDrop("rejected-shape")
        }

        val tol = near * hypot(w, h)
        val cur = accepted

        if (cur == null) {                                   // first sight: needs two looks that agree
            val c = candidate
            candidate = q.copyOf()
            if (c != null && agree(c, q, tol)) {
                accepted = q.copyOf(); candidate = null; held = 0
                return Result(accepted, !inferred, "accepted")
            }
            return Result(null, false, "pending")
        }

        if (agree(cur, q, tol)) {                            // small move: follow it
            accepted = q.copyOf(); candidate = null; held = 0
            return Result(accepted, !inferred, "accepted")
        }

        // a jump: switch only if the new place is confirmed by the next frame
        val c = candidate
        if (c != null && agree(c, q, tol)) {
            accepted = q.copyOf(); candidate = null; held = 0
            return Result(accepted, !inferred, "moved")
        }
        candidate = q.copyOf()
        return keepOrDrop("held-jump")
    }

    private fun keepOrDrop(status: String): Result {
        val cur = accepted
        if (cur != null && held < maxHeldFrames) {
            held++
            return Result(cur, false, status)                // keep showing it, but not for pose decisions
        }
        accepted = null; held = 0
        return Result(null, false, status)
    }

    private fun agree(p: DoubleArray, q: DoubleArray, tol: Double): Boolean {
        for (i in 0 until 4) {
            if (hypot(p[2 * i] - q[2 * i], p[2 * i + 1] - q[2 * i + 1]) > tol) return false
        }
        return true
    }
}