package com.assentify.sdk.tflite.LivenessDetection.Finder

import com.assentify.sdk.Core.Constants.LivenessConfig

/**
 * From readings to a verdict (rc17 spec, 02-core-spec):
 *
 *  - iridescenceResult ... checks/laminate.md   (core/iridescence.ts:24)
 *  - ReflectionAnalyzer .. checks/reflection.md (core/reflection.ts:34)
 *  - decide .............. gate-policy.md + pseudocode/decide.md (core/gate-policy.ts:44)
 *
 * States and reasons are the spec's strings, so they can be compared with the web's results.
 * Java callers: functions are @JvmStatic, fields are @JvmField, verdicts are constants usable in a switch.
 */
object LivenessGate {

    const val CONTINUE = "continue"
    const val RETRY = "retry"
    const val SUSPECTED_COPY = "suspected_copy"
    const val INCONCLUSIVE = "inconclusive"

    // =============================================================================================
    // Laminate (iridescence.ts:24)
    // =============================================================================================

    class LaminateResult {
        @JvmField var state: String = "unassessable"      // present | absent | unassessable
        @JvmField var strong: Boolean = false
        @JvmField var frames: Int = 0
        @JvmField var litFrames: Int = 0
        @JvmField var hues: Int = 0
        @JvmField var peak: Double? = null                // null when there were no frames

        fun peakOr0(): Double = peak ?: 0.0
    }

    @JvmStatic
    fun iridescenceResult(frames: List<DoubleArray>, c: LivenessConfig.Laminate): LaminateResult {
        val r = LaminateResult()
        r.frames = frames.size
        if (frames.isEmpty()) return r                    // unassessable, not strong

        val best = DoubleArray(12)
        var peak = 0.0
        var lit = 0
        for (f in frames) {
            var total = 0.0
            for (i in 0 until 12) {
                total += f[i]
                best[i] = maxOf(best[i], f[i])
            }
            if (total >= 0.001) lit++
            peak = maxOf(peak, total)
        }
        r.litFrames = lit
        r.hues = best.count { it >= 0.0005 }
        r.peak = peak

        val enough = r.frames >= c.minFrames && lit >= c.minLitFrames
        r.state = when {
            r.frames < c.minFrames -> "unassessable"
            enough && r.hues >= c.minHues && peak >= c.minPeak -> "present"
            else -> "absent"
        }
        r.strong = enough && r.hues >= c.strongHues && peak >= c.strongPeak
        return r
    }

    // =============================================================================================
    // Emblem (core/emblem.ts:30): the ID front's cedar under the light and the right-edge tilt
    // =============================================================================================

    class EmblemResult {
        @JvmField var state: String = "not_configured"   // present | absent | untested | unassessable | not_configured
        @JvmField var lit: Double? = null
        @JvmField var jump: Double? = null
        @JvmField var frames: Int = 0
        @JvmField var low: Double? = null
        @JvmField var dark: Boolean? = null
    }

    /**
     * readings: lit frames {now, base}; tilt: the emblem per tilted frame (NaN where the frame does not count), or null
     * before the tilt; tested: the right edge came far enough (testTilt) for minTiltFrames frames in a row.
     */
    @JvmStatic
    fun emblemResult(readings: List<DoubleArray>, configured: Boolean, tilt: DoubleArray?, tested: Boolean,
                     c: LivenessConfig.Emblem): EmblemResult {
        val r = EmblemResult()
        if (!configured) return r
        val ok = readings.filter { it[0].isFinite() && it[1].isFinite() }
        r.frames = ok.size
        if (ok.size < c.minFrames) { r.state = "unassessable"; return r }
        val lit = ok.map { it[0] }.sortedDescending()[1]
        val bases = ok.map { it[1] }.sorted()
        val jump = lit - maxOf(0.0, bases[bases.size shr 1])
        r.lit = lit
        r.jump = jump
        val jumped = lit >= c.litMin && (jump >= c.jumpMin || lit >= c.strongLit)
        if (tilt == null) { r.state = if (jumped) "present" else "absent"; return r }
        val n = c.minTiltFrames
        var low: Double? = null
        var i = 0
        while (i + n <= tilt.size) {
            var allFinite = true
            var top = -Double.MAX_VALUE
            for (j in i until i + n) {
                if (!tilt[j].isFinite()) { allFinite = false; break }
                top = maxOf(top, tilt[j])
            }
            if (allFinite && (low == null || top < low)) low = top
            i++
        }
        r.low = low
        val dark = low != null && low <= c.darkMax * lit
        r.dark = dark
        r.state = if (jumped && dark) "present" else if (jumped && !tested) "untested" else "absent"
        return r
    }

    // =============================================================================================
    // Reflection (reflection.ts:34)
    // =============================================================================================

    class ReflectionResult {
        @JvmField var state: String = "unassessable"      // reflective | matte | emissive | unassessable
        @JvmField var usable: Int = 0
        @JvmField var litFlare: Double? = null
        @JvmField var litLift: Double? = null
        @JvmField var iridescent: Boolean = false         // recorded only
    }

    /** Samples are LivenessVision.pageResponse arrays {specFrac, chromaFrac, flareFrac, veilLift}. */
    class ReflectionAnalyzer(private val c: LivenessConfig.Reflection) {
        private val lit = ArrayList<DoubleArray>()
        private val tilt = ArrayList<DoubleArray>()

        /** A lit frame of the light step (unlit reference). Whited-out frames are not a reflection. */
        fun addLit(s: DoubleArray?) {
            if (s != null && s[LivenessVision.FLARE].isFinite() && s[LivenessVision.FLARE] <= c.whiteoutMax) lit.add(s)
        }

        /** A tilted frame (lit reference): recorded only. */
        fun add(s: DoubleArray?) {
            if (s != null && s[LivenessVision.SPEC].isFinite() && s[LivenessVision.CHROMA].isFinite()) tilt.add(s)
        }

        fun usable(): Int = lit.size

        fun result(): ReflectionResult {
            val r = ReflectionResult()
            r.usable = lit.size
            r.litFlare = steadyMax(lit.map { it[LivenessVision.FLARE] })
            val lifts = lit.map { it[LivenessVision.VEIL] }.filter { it.isFinite() }.sorted()
            r.litLift = if (lifts.isEmpty()) null else lifts[lifts.size shr 1]
            r.state = when {
                r.usable < c.minLitFrames -> "unassessable"
                r.litLift != null && r.litLift!! < c.liftMin -> "emissive"
                (r.litFlare ?: 0.0) >= c.flareMin -> "reflective"
                else -> "matte"
            }
            val chromas = tilt.map { it[LivenessVision.CHROMA] }
            val tiltChroma = if (chromas.size >= 2) steadyMax(chromas) else null
            r.iridescent = tiltChroma != null && tiltChroma >= c.chromaMin
            return r
        }

        /** Second largest, so one stray frame cannot decide (the only one if there is one; null if none). */
        private fun steadyMax(xs: List<Double>): Double? = when (xs.size) {
            0 -> null
            1 -> xs[0]
            else -> xs.sorted()[xs.size - 2]
        }
    }

    // =============================================================================================
    // decide (gate-policy.ts:44)
    // =============================================================================================

    /** Null fields are "not given" (not the same as false), exactly as in the spec. */
    class GateInput {
        @JvmField var cameraOk: Boolean = true
        @JvmField var enginePhase: String? = null         // done | failed | other
        @JvmField var attempt: Int = 0
        @JvmField var challenge: String? = null           // observed | not_observed | unassessable
        @JvmField var optical: String = "not_configured"
        @JvmField var opticalPolicy: String = "shadow"
        @JvmField var reflection: String? = null          // reflective | matte | emissive | unassessable
        @JvmField var reflectionPolicy: String? = null
        @JvmField var matteBefore: Boolean? = null
        @JvmField var lightAvailable: Boolean? = null
        @JvmField var emblem: String? = null
        @JvmField var emblemPolicy: String? = null
        @JvmField var laminate: String? = null            // present | absent | unassessable | not_configured
        @JvmField var laminatePolicy: String? = null
        @JvmField var laminateStrong: Boolean? = null
        @JvmField var matteNotEvidence: Boolean? = null
        @JvmField var batteryLow: Boolean? = null
        @JvmField var emblemRequired: Boolean? = null
        @JvmField var laminateRequired: Boolean? = null
        @JvmField var cutEdge: String? = null
        @JvmField var cutEdgePolicy: String? = null
        @JvmField var backShown: Boolean? = null
        @JvmField var photoOk: Boolean? = null
    }

    class Decision(@JvmField val verdict: String, @JvmField val reasons: List<String>)

    @JvmStatic
    fun decide(i: GateInput, maxAttempts: Int): Decision {
        val reasons = ArrayList<String>()
        fun stop(verdict: String, r: String) = Decision(verdict, reasons + r)
        fun retryOrGiveUp(r: String) = stop(if (i.attempt + 1 >= maxAttempts) INCONCLUSIVE else RETRY, r)

        // 1
        if (!i.cameraOk) return Decision(INCONCLUSIVE, listOf("camera_unavailable"))
        // 2
        if (i.backShown == false) return retryOrGiveUp("back_barcode_not_seen")
        // 3
        if (i.enginePhase == "failed") return Decision(INCONCLUSIVE, listOf("challenge_not_completed"))
        // 4
        if (i.enginePhase != "done" || i.challenge != "observed") return retryOrGiveUp("challenge_incomplete")
        // 5
        if (i.photoOk == false) return retryOrGiveUp("photo_failed")
        // 6
        if (i.emblemRequired == true && i.emblemPolicy == "enforce") {
            if (i.emblem == null || i.emblem == "not_configured") return retryOrGiveUp("emblem_missing")
            if (i.emblem == "unassessable") return retryOrGiveUp("emblem_unassessable")
        }
        // 7
        if (i.laminateRequired == true && i.laminatePolicy == "enforce") {
            if (i.laminate == null || i.laminate == "not_configured") return retryOrGiveUp("laminate_missing")
            if (i.laminate == "unassessable") return retryOrGiveUp("laminate_unassessable")
        }
        // 8
        if (i.emblem == "untested" && i.emblemPolicy == "enforce") return retryOrGiveUp("emblem_untested")
        // 9
        if (i.optical == "inconsistent_response") {
            if (i.opticalPolicy == "enforce") return stop(SUSPECTED_COPY, "optical_inconsistent")
            reasons.add("optical_inconsistent_shadow")
        }
        // 10
        if (i.opticalPolicy == "enforce" && i.optical == "unassessable") return retryOrGiveUp("optical_unassessable")
        // 11
        val matte = i.reflection == "matte"
        val emissive = i.reflection == "emissive"
        val emblemPresent = i.emblem == "present"
        if (matte && (emblemPresent || (i.laminate == "present" && i.laminateStrong != false))) {
            reasons.add("reflection_matte_excused")
        } else if (emissive && emblemPresent) {
            reasons.add("no_response_to_light_excused")
        } else if (matte || emissive) {
            val what = if (matte) "reflection_matte" else "no_response_to_light"
            if (emissive && i.batteryLow == true) return stop(INCONCLUSIVE, "battery_low")
            if (i.reflectionPolicy == "enforce") {
                if (emissive) return retryOrGiveUp("no_response_to_light")
                if (i.matteNotEvidence == true) return retryOrGiveUp("reflection_matte_unproven")
                if (i.matteBefore != true) return stop(RETRY, what + "_first")
                return stop(SUSPECTED_COPY, what)
            }
            reasons.add(what + "_shadow")
        }
        // 12
        if (i.reflectionPolicy == "enforce" && i.reflection == "unassessable") {
            if (i.lightAvailable == false) return stop(INCONCLUSIVE, "no_camera_light")
            return retryOrGiveUp("reflection_unassessable")
        }
        // 13
        if (i.emblem == "absent") {
            if (i.emblemPolicy == "enforce") {
                return if (i.matteBefore != true) stop(RETRY, "emblem_absent_first") else stop(SUSPECTED_COPY, "emblem_absent")
            }
            reasons.add("emblem_absent_shadow")
        }
        // 14
        if (i.laminate == "absent") {
            if (i.laminatePolicy == "enforce") {
                return if (i.matteBefore != true) stop(RETRY, "laminate_absent_first") else stop(SUSPECTED_COPY, "laminate_absent")
            }
            reasons.add("laminate_absent_shadow")
        }
        // 15
        if (i.cutEdge == "cut") {
            if (i.cutEdgePolicy == "enforce") {
                return if (i.matteBefore != true) stop(RETRY, "overlay_cut_first") else stop(SUSPECTED_COPY, "overlay_cut")
            }
            reasons.add("overlay_cut_shadow")
        }
        // end
        reasons.add("capture_completed")
        if (i.batteryLow == true) reasons.add("battery_low_noted")
        return Decision(CONTINUE, reasons)
    }
}