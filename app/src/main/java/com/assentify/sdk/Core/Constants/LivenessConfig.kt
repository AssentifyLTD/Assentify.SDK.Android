package com.assentify.sdk.Core.Constants



import android.content.Context
import org.json.JSONObject

/**
 * Document-liveness thresholds, read from assets/rectifier_config_file.json
 * (the same policy file the web uses).
 *
 *   val cfg = LivenessConfig.load(context, kind)   // kind: "lb_passport", "other_passport", "lb_civil_id", ...
 *   cfg.lossMs, cfg.upDown.tiltMin, cfg.profile.aspect, cfg.laminatePolicy() ...
 *
 * The document kind is always a parameter (no default): it must match a key under "profiles"
 * in the file, otherwise load() throws IllegalArgumentException.
 * The thresholds under "config" have defaults equal to the file (policy 2026-10-22.10).
 *
 * Java callers: fields are @JvmField (cfg.lossMs, cfg.light.settleMs, cfg.profile.aspect) and
 * load / availableKinds / clearCache are static (LivenessConfig.load(ctx, kind)).
 */
class LivenessConfig private constructor() {

    // ---------------------------------------------------------------------------------------------
    // Top-level
    // ---------------------------------------------------------------------------------------------
    @JvmField
    var policyVersion: String = "2026-10-22.10"
    @JvmField
    var packageVariant: String = "rc17+lb-passport-spread"

    @JvmField
    var batteryLowBelow: Double = 0.05
    @JvmField
    var analysisLongEdge: Int = 720
    @JvmField
    var minQuadAreaFrac: Double = 0.08
    @JvmField
    var tiltOverflow: Double = 0.15
    @JvmField
    var frameMarginPx: Double = 4.0
    @JvmField
    var stableMs: Long = 300
    @JvmField
    var baselineHoldMs: Long = 400
    @JvmField
    var finalHoldMs: Long = 400
    @JvmField
    var lossMs: Long = 1500
    @JvmField
    var phaseTimeoutMs: Long = 15000
    @JvmField
    var maxAttempts: Int = 3
    @JvmField
    var maxRestarts: Int = 8
    @JvmField
    var edgeMargin: Double = 0.02
    @JvmField
    var maxEarlyRestarts: Int = 40
    @JvmField
    var frontalMax: Double = 0.16
    @JvmField
    var shapeTol: Double = 0.1
    @JvmField
    var angleMax: Double = 0.17
    @JvmField
    var secondDocFrac: Double = 0.25
    @JvmField
    var samePlaceMax: Double = 0.08
    @JvmField
    var initTimeoutMs: Long = 300000
    @JvmField
    var initStallMs: Long = 60000
    @JvmField
    var loadSlowMs: Long = 20000
    @JvmField
    var stallMs: Long = 6000
    @JvmField
    var tiltHoldMs: Long = 4000
    @JvmField
    var stuckMs: Long = 3000
    @JvmField
    var wrongWayFrames: Int = 4
    @JvmField
    var hintAfterMs: Long = 3000
    @JvmField
    var repeatAfterMs: Long = 6000
    @JvmField
    var autoBackMs: Long = 1800
    @JvmField
    var turnMs: Long = 2500
    @JvmField
    var reviewMs: Long = 10000
    @JvmField
    var turnAfterReviewMs: Long = 900
    @JvmField
    var slowCheckMs: Long = 8000
    @JvmField
    var checkStepsMs: Long = 8000
    @JvmField
    var longerLineMs: Long = 4500
    @JvmField
    var verifyTimeoutMs: Long = 45000
    @JvmField
    var endOnTilt: Boolean = true
    @JvmField
    var sweeps: Int = 1
    @JvmField
    var tiltMin: Double = 0.05
    @JvmField
    var tiltNoiseK: Double = 3.0
    @JvmField
    var sweepFrames: Int = 3
    @JvmField
    var returnTol: Double = 0.05
    @JvmField
    var returnFrames: Int = 2
    @JvmField
    var jitterMax: Double = 0.015
    @JvmField
    var driftMax: Double = 0.03
    @JvmField
    var aspectJumpMax: Double = 0.15
    @JvmField
    var tiltAspectJumpMax: Double = 0.35
    @JvmField
    var tiltStepMin: Double = 0.1
    @JvmField
    var areaJumpMax: Double = 1.6
    @JvmField
    var sharpnessMin: Double = 60.0
    @JvmField
    var glareFracMax: Double = 0.04
    @JvmField
    var otherPassportLaminatePolicy: String = "shadow"
    @JvmField
    var spreadCutMax: Double = 0.05

    // ---------------------------------------------------------------------------------------------
    // Sections
    // ---------------------------------------------------------------------------------------------
    @JvmField
    val hands = Hands()
    @JvmField
    val sendPhoto = SendPhoto()
    @JvmField
    val back = Back()
    @JvmField
    val upDown = UpDown()
    @JvmField
    val laminate = Laminate()
    @JvmField
    val civilExtract = CivilExtract()
    @JvmField
    val overlay = Overlay()
    @JvmField
    val backShown = BackShown()
    @JvmField
    val idBarcode = IdBarcode()
    @JvmField
    val drivingLicence = LaminateDoc()
    @JvmField
    val residencePermit = LaminateDoc(evidence = "licence trials")
    @JvmField
    val optical = Optical()
    @JvmField
    val reflection = Reflection()
    @JvmField
    val light = Light()
    @JvmField
    val emblem = Emblem()
    @JvmField
    val face = Face()
    @JvmField
    val evidence = Evidence()

    /** The profile for the requested document kind (lb_passport, other_passport, ...). */
    @JvmField
    var profile: Profile = Profile()

    class Hands {
        @JvmField
        var flatDeg: Double = 10.0
        @JvmField
        var flatMs: Long = 800
        @JvmField
        var tableCard: Boolean = true
        @JvmField
        var turnDeg: Double = 20.0
        @JvmField
        var maxPauses: Int = 1
    }

    class SendPhoto {
        @JvmField
        var maxSide: Int = 1920
        @JvmField
        var quality: Double = 0.9
    }

    class Back {
        @JvmField
        var sweeps: Int = 0
        @JvmField
        var policy: String = "shadow"
        @JvmField
        var glareWaitMs: Long = 3000
    }

    class UpDown {
        @JvmField
        var tiltMin: Double = 0.08
        @JvmField
        var holdFrames: Int = 6
        @JvmField
        var pageDirection: String = "bottom"
    }

    class Laminate {
        @JvmField
        var minLitFrames: Int = 3
        @JvmField
        var minHues: Int = 3
        @JvmField
        var minPeak: Double = 0.001
        @JvmField
        var minFrames: Int = 6
        @JvmField
        var strongHues: Int = 5
        @JvmField
        var strongPeak: Double = 0.003
        @JvmField
        var passportStrong: Boolean = true
        @JvmField
        var passportHues: Int = 4
        @JvmField
        var policy: String = "enforce"
    }

    class CivilExtract {
        @JvmField
        var holdMs: Long = 300
        @JvmField
        var lightMs: Long = 900
        @JvmField
        var minFill: Double = 0.18
        @JvmField
        var steadyMs: Long = 700
        @JvmField
        var steadyMove: Double = 0.02
        @JvmField
        var buttonAfterMs: Long = 10000
        @JvmField
        var codeMs: Long = 25000
        @JvmField
        var codeEveryMs: Long = 350
        @JvmField
        var maxRestarts: Int = 2
    }

    class Overlay {
        @JvmField
        var policy: String = "enforce"
        @JvmField
        var bar: Double = 4.5
        @JvmField
        var barSeveral: Double = 3.5
        @JvmField
        var waitMs: Long = 1500
    }

    class BackShown {
        @JvmField
        var policy: String = "shadow"
        @JvmField
        var bar: Double = 0.3
        @JvmField
        var everyMs: Long = 300
        @JvmField
        var withinMs: Long = 12000
        @JvmField
        var hintAfterMs: Long = 1500
    }

    class IdBarcode {
        @JvmField
        var everyMs: Long = 250
        @JvmField
        var waitMs: Long = 8000
    }

    class LaminateDoc(evidence: String = "") {
        @JvmField
        var laminatePolicy: String = "enforce"
        @JvmField
        var holdMs: Long = 6000
        @JvmField
        var tiltShare: Double = 0.6
        @JvmField
        var evidence: String = evidence
    }

    class Optical {
        @JvmField
        var minFrames: Int = 6
        @JvmField
        var supportRatio: Double = 2.0
        @JvmField
        var staticRatio: Double = 1.2
        @JvmField
        var controlMax: Double = 20.0
        @JvmField
        var policy: String = "shadow"
    }

    class Reflection {
        @JvmField
        var flareMin: Double = 0.0002
        @JvmField
        var whiteoutMax: Double = 0.3
        @JvmField
        var chromaMin: Double = 0.03
        @JvmField
        var minLitFrames: Int = 2
        @JvmField
        var liftMin: Double = 1.02
        @JvmField
        var brightLift: Double = 1.25
        @JvmField
        var sunMax: Double = 3.0
        @JvmField
        var policy: String = "enforce"
    }

    class Light {
        @JvmField
        var frames: Int = 5
        @JvmField
        var settleMs: Long = 300
        @JvmField
        var maxMs: Long = 2500
        @JvmField
        var offSettleMs: Long = 500
        @JvmField
        var offMaxMs: Long = 4000
        @JvmField
        var barcodeMs: Long = 3000
    }

    class Emblem {
        @JvmField
        var litMin: Double = 7.0
        @JvmField
        var jumpMin: Double = 8.0
        @JvmField
        var strongLit: Double = 16.0
        @JvmField
        var testTilt: Double = 0.12
        @JvmField
        var tiltTarget: Double = 0.15
        @JvmField
        var holdMs: Long = 8000
        @JvmField
        var minFrames: Int = 2
        @JvmField
        var extraMs: Long = 3000
        @JvmField
        var darkMax: Double = 0.6
        @JvmField
        var minTiltFrames: Int = 3
        @JvmField
        var tiltSide: Double = 0.05
        @JvmField
        var direction: String = "right"
        @JvmField
        var policy: String = "enforce"
    }

    class Face {
        @JvmField
        var strongChroma: Double = 50.0
        @JvmField
        var strongColourMax: Double = 0.7
        @JvmField
        var inset: Double = 0.06
        @JvmField
        var policy: String = "enforce"
    }

    class Evidence {
        @JvmField
        var maxFrames: Int = 80
        @JvmField
        var maxBytes: Long = 20971520
    }

    /** Normalised rectangle on the straightened document (0..1). */
    data class Roi(
        @JvmField val x: Double,
        @JvmField val y: Double,
        @JvmField val w: Double,
        @JvmField val h: Double,
    )

    class Profile {
        @JvmField
        var id: String = ""
        @JvmField
        var kind: String = ""
        @JvmField
        var aspect: Double = 0.0
        @JvmField
        var featureKind: String = "none"          // laminate_hologram / cedar / none
        @JvmField
        var featureRoi: Roi = Roi(0.35, 0.3, 0.3, 0.3)
        @JvmField
        var controls: List<Roi> = emptyList()
        @JvmField
        var iridescent: Boolean = false
        @JvmField
        var page: Boolean = false                  // passport data page
        @JvmField
        var spread: Boolean = false                // open booklet (two pages) may be in view
        @JvmField
        var sheet: Boolean = false                 // civil extract
        @JvmField
        var minFill: Double = 0.0                  // lb_civil_id only (0 when absent)
        @JvmField
        var raw: JSONObject =
            JSONObject()         // full profile JSON (emblem, orient, ... for later use)
    }

    /** Laminate policy for this document kind ("enforce" / "shadow"). */
    fun laminatePolicy(): String = when (profile.kind) {
        KIND_OTHER_PASSPORT -> otherPassportLaminatePolicy
        KIND_LB_DRIVING_LICENCE -> drivingLicence.laminatePolicy
        KIND_LB_RESIDENCE_PERMIT -> residencePermit.laminatePolicy
        else -> laminate.policy
    }

    /** True when the tilt is up/down (passport: lift the bottom edge). */
    fun isUpDown(): Boolean = upDown.pageDirection == "bottom" || upDown.pageDirection == "top"

    /** Tilt threshold for the configured page direction (passport: up/down -> bottom). */
    fun tiltMinForPage(): Double = if (isUpDown()) upDown.tiltMin else tiltMin

    /** Frames the tilt must be held for (up/down uses its own count). */
    fun holdFramesForPage(): Int = if (isUpDown()) upDown.holdFrames else sweepFrames

    companion object {
        const val ASSET_FILE = "rectifier_config_file.json"

        // profile kinds in the file (convenience names; any key under "profiles" works)
        const val KIND_LB_PASSPORT = "lb_passport"
        const val KIND_OTHER_PASSPORT = "other_passport"
        const val KIND_LB_CIVIL_ID = "lb_civil_id"
        const val KIND_LB_CIVIL_ID_BACK = "lb_civil_id_back"
        const val KIND_LB_DRIVING_LICENCE = "lb_driving_licence"
        const val KIND_LB_CIVIL_EXTRACT = "lb_civil_extract"
        const val KIND_LB_RESIDENCE_PERMIT = "lb_residence_permit"

        private val cache = HashMap<String, LivenessConfig>()

        /**
         * Loads (once, cached) the config for the given document kind from assets.
         *
         * @param kind a key under "profiles" in the file: "lb_passport", "other_passport", "lb_civil_id", ...
         * @throws IllegalArgumentException if kind is empty or not in the file
         * @throws IllegalStateException    if the asset can't be read or parsed
         */
        @JvmStatic
        @Synchronized
        fun load(context: Context, kind: String): LivenessConfig {
            require(kind.isNotEmpty()) { "document kind is required (e.g. \"lb_passport\")" }
            cache[kind]?.let { return it }
            val cfg = parse(readRoot(context), kind)
            cache[kind] = cfg
            return cfg
        }

        /** All document kinds available in the file (the keys under "profiles"). */
        @JvmStatic
        fun availableKinds(context: Context): List<String> =
            readRoot(context).obj("profiles").keys().asSequence().toList()

        /** Drops the cache (e.g. after downloading a newer policy file). */
        @JvmStatic
        @Synchronized
        fun clearCache() = cache.clear()

        private fun readRoot(context: Context): JSONObject = try {
            val text =
                context.assets.open(ASSET_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalStateException("could not read assets/$ASSET_FILE", e)
        }

        /** Child object or an empty one (so every opt*() call falls back to its default). */
        private fun JSONObject.obj(name: String): JSONObject = optJSONObject(name) ?: JSONObject()

        private fun parse(
            root: JSONObject,
            kind: String
        ): LivenessConfig {
            val c = LivenessConfig()
            val j = root.obj("config")

            c.packageVariant = root.optString("packageVariant", c.packageVariant)
            c.policyVersion =
                j.optString("policyVersion", root.optString("policyVersion", c.policyVersion))

            c.batteryLowBelow = j.optDouble("batteryLowBelow", c.batteryLowBelow)
            c.analysisLongEdge = j.optInt("analysisLongEdge", c.analysisLongEdge)
            c.minQuadAreaFrac = j.optDouble("minQuadAreaFrac", c.minQuadAreaFrac)
            c.tiltOverflow = j.optDouble("tiltOverflow", c.tiltOverflow)
            c.frameMarginPx = j.optDouble("frameMarginPx", c.frameMarginPx)
            c.stableMs = j.optLong("stableMs", c.stableMs)
            c.baselineHoldMs = j.optLong("baselineHoldMs", c.baselineHoldMs)
            c.finalHoldMs = j.optLong("finalHoldMs", c.finalHoldMs)
            c.lossMs = j.optLong("lossMs", c.lossMs)
            c.phaseTimeoutMs = j.optLong("phaseTimeoutMs", c.phaseTimeoutMs)
            c.maxAttempts = j.optInt("maxAttempts", c.maxAttempts)
            c.maxRestarts = j.optInt("maxRestarts", c.maxRestarts)
            c.edgeMargin = j.optDouble("edgeMargin", c.edgeMargin)
            c.maxEarlyRestarts = j.optInt("maxEarlyRestarts", c.maxEarlyRestarts)
            c.frontalMax = j.optDouble("frontalMax", c.frontalMax)
            c.shapeTol = j.optDouble("shapeTol", c.shapeTol)
            c.angleMax = j.optDouble("angleMax", c.angleMax)
            c.secondDocFrac = j.optDouble("secondDocFrac", c.secondDocFrac)
            c.samePlaceMax = j.optDouble("samePlaceMax", c.samePlaceMax)
            c.initTimeoutMs = j.optLong("initTimeoutMs", c.initTimeoutMs)
            c.initStallMs = j.optLong("initStallMs", c.initStallMs)
            c.loadSlowMs = j.optLong("loadSlowMs", c.loadSlowMs)
            c.stallMs = j.optLong("stallMs", c.stallMs)
            c.tiltHoldMs = j.optLong("tiltHoldMs", c.tiltHoldMs)
            c.stuckMs = j.optLong("stuckMs", c.stuckMs)
            c.wrongWayFrames = j.optInt("wrongWayFrames", c.wrongWayFrames)
            c.hintAfterMs = j.optLong("hintAfterMs", c.hintAfterMs)
            c.repeatAfterMs = j.optLong("repeatAfterMs", c.repeatAfterMs)
            c.autoBackMs = j.optLong("autoBackMs", c.autoBackMs)
            c.turnMs = j.optLong("turnMs", c.turnMs)
            c.reviewMs = j.optLong("reviewMs", c.reviewMs)
            c.turnAfterReviewMs = j.optLong("turnAfterReviewMs", c.turnAfterReviewMs)
            c.slowCheckMs = j.optLong("slowCheckMs", c.slowCheckMs)
            c.checkStepsMs = j.optLong("checkStepsMs", c.checkStepsMs)
            c.longerLineMs = j.optLong("longerLineMs", c.longerLineMs)
            c.verifyTimeoutMs = j.optLong("verifyTimeoutMs", c.verifyTimeoutMs)
            c.endOnTilt = j.optBoolean("endOnTilt", c.endOnTilt)
            c.sweeps = j.optInt("sweeps", c.sweeps)
            c.tiltMin = j.optDouble("tiltMin", c.tiltMin)
            c.tiltNoiseK = j.optDouble("tiltNoiseK", c.tiltNoiseK)
            c.sweepFrames = j.optInt("sweepFrames", c.sweepFrames)
            c.returnTol = j.optDouble("returnTol", c.returnTol)
            c.returnFrames = j.optInt("returnFrames", c.returnFrames)
            c.jitterMax = j.optDouble("jitterMax", c.jitterMax)
            c.driftMax = j.optDouble("driftMax", c.driftMax)
            c.aspectJumpMax = j.optDouble("aspectJumpMax", c.aspectJumpMax)
            c.tiltAspectJumpMax = j.optDouble("tiltAspectJumpMax", c.tiltAspectJumpMax)
            c.tiltStepMin = j.optDouble("tiltStepMin", c.tiltStepMin)
            c.areaJumpMax = j.optDouble("areaJumpMax", c.areaJumpMax)
            c.sharpnessMin = j.optDouble("sharpnessMin", c.sharpnessMin)
            c.glareFracMax = j.optDouble("glareFracMax", c.glareFracMax)

            j.obj("hands").let { o ->
                c.hands.flatDeg = o.optDouble("flatDeg", c.hands.flatDeg)
                c.hands.flatMs = o.optLong("flatMs", c.hands.flatMs)
                c.hands.tableCard = o.optBoolean("tableCard", c.hands.tableCard)
                c.hands.turnDeg = o.optDouble("turnDeg", c.hands.turnDeg)
                c.hands.maxPauses = o.optInt("maxPauses", c.hands.maxPauses)
            }
            j.obj("sendPhoto").let { o ->
                c.sendPhoto.maxSide = o.optInt("maxSide", c.sendPhoto.maxSide)
                c.sendPhoto.quality = o.optDouble("quality", c.sendPhoto.quality)
            }
            j.obj("back").let { o ->
                c.back.sweeps = o.optInt("sweeps", c.back.sweeps)
                c.back.policy = o.optString("policy", c.back.policy)
                c.back.glareWaitMs = o.optLong("glareWaitMs", c.back.glareWaitMs)
            }
            j.obj("upDown").let { o ->
                c.upDown.tiltMin = o.optDouble("tiltMin", c.upDown.tiltMin)
                c.upDown.holdFrames = o.optInt("holdFrames", c.upDown.holdFrames)
                c.upDown.pageDirection = o.optString("pageDirection", c.upDown.pageDirection)
            }
            j.obj("laminate").let { o ->
                c.laminate.minLitFrames = o.optInt("minLitFrames", c.laminate.minLitFrames)
                c.laminate.minHues = o.optInt("minHues", c.laminate.minHues)
                c.laminate.minPeak = o.optDouble("minPeak", c.laminate.minPeak)
                c.laminate.minFrames = o.optInt("minFrames", c.laminate.minFrames)
                c.laminate.strongHues = o.optInt("strongHues", c.laminate.strongHues)
                c.laminate.strongPeak = o.optDouble("strongPeak", c.laminate.strongPeak)
                c.laminate.passportStrong =
                    o.optBoolean("passportStrong", c.laminate.passportStrong)
                c.laminate.passportHues = o.optInt("passportHues", c.laminate.passportHues)
                c.laminate.policy = o.optString("policy", c.laminate.policy)
            }
            c.otherPassportLaminatePolicy =
                j.obj("otherPassport").optString("laminatePolicy", c.otherPassportLaminatePolicy)
            j.obj("civilExtract").let { o ->
                c.civilExtract.holdMs = o.optLong("holdMs", c.civilExtract.holdMs)
                c.civilExtract.lightMs = o.optLong("lightMs", c.civilExtract.lightMs)
                c.civilExtract.minFill = o.optDouble("minFill", c.civilExtract.minFill)
                c.civilExtract.steadyMs = o.optLong("steadyMs", c.civilExtract.steadyMs)
                c.civilExtract.steadyMove = o.optDouble("steadyMove", c.civilExtract.steadyMove)
                c.civilExtract.buttonAfterMs =
                    o.optLong("buttonAfterMs", c.civilExtract.buttonAfterMs)
                c.civilExtract.codeMs = o.optLong("codeMs", c.civilExtract.codeMs)
                c.civilExtract.codeEveryMs = o.optLong("codeEveryMs", c.civilExtract.codeEveryMs)
                c.civilExtract.maxRestarts = o.optInt("maxRestarts", c.civilExtract.maxRestarts)
            }
            j.obj("overlay").let { o ->
                c.overlay.policy = o.optString("policy", c.overlay.policy)
                c.overlay.bar = o.optDouble("bar", c.overlay.bar)
                c.overlay.barSeveral = o.optDouble("barSeveral", c.overlay.barSeveral)
                c.overlay.waitMs = o.optLong("waitMs", c.overlay.waitMs)
            }
            j.obj("backShown").let { o ->
                c.backShown.policy = o.optString("policy", c.backShown.policy)
                c.backShown.bar = o.optDouble("bar", c.backShown.bar)
                c.backShown.everyMs = o.optLong("everyMs", c.backShown.everyMs)
                c.backShown.withinMs = o.optLong("withinMs", c.backShown.withinMs)
                c.backShown.hintAfterMs = o.optLong("hintAfterMs", c.backShown.hintAfterMs)
            }
            j.obj("idBarcode").let { o ->
                c.idBarcode.everyMs = o.optLong("everyMs", c.idBarcode.everyMs)
                c.idBarcode.waitMs = o.optLong("waitMs", c.idBarcode.waitMs)
            }
            readLaminateDoc(j.obj("drivingLicence"), c.drivingLicence)
            readLaminateDoc(j.obj("residencePermit"), c.residencePermit)
            c.spreadCutMax = j.obj("spread").optDouble("cutMax", c.spreadCutMax)
            j.obj("optical").let { o ->
                c.optical.minFrames = o.optInt("minFrames", c.optical.minFrames)
                c.optical.supportRatio = o.optDouble("supportRatio", c.optical.supportRatio)
                c.optical.staticRatio = o.optDouble("staticRatio", c.optical.staticRatio)
                c.optical.controlMax = o.optDouble("controlMax", c.optical.controlMax)
                c.optical.policy = o.optString("policy", c.optical.policy)
            }
            j.obj("reflection").let { o ->
                c.reflection.flareMin = o.optDouble("flareMin", c.reflection.flareMin)
                c.reflection.whiteoutMax = o.optDouble("whiteoutMax", c.reflection.whiteoutMax)
                c.reflection.chromaMin = o.optDouble("chromaMin", c.reflection.chromaMin)
                c.reflection.minLitFrames = o.optInt("minLitFrames", c.reflection.minLitFrames)
                c.reflection.liftMin = o.optDouble("liftMin", c.reflection.liftMin)
                c.reflection.brightLift = o.optDouble("brightLift", c.reflection.brightLift)
                c.reflection.sunMax = o.optDouble("sunMax", c.reflection.sunMax)
                c.reflection.policy = o.optString("policy", c.reflection.policy)
            }
            j.obj("light").let { o ->
                c.light.frames = o.optInt("frames", c.light.frames)
                c.light.settleMs = o.optLong("settleMs", c.light.settleMs)
                c.light.maxMs = o.optLong("maxMs", c.light.maxMs)
                c.light.offSettleMs = o.optLong("offSettleMs", c.light.offSettleMs)
                c.light.offMaxMs = o.optLong("offMaxMs", c.light.offMaxMs)
                c.light.barcodeMs = o.optLong("barcodeMs", c.light.barcodeMs)
            }
            j.obj("emblem").let { o ->
                c.emblem.litMin = o.optDouble("litMin", c.emblem.litMin)
                c.emblem.jumpMin = o.optDouble("jumpMin", c.emblem.jumpMin)
                c.emblem.strongLit = o.optDouble("strongLit", c.emblem.strongLit)
                c.emblem.testTilt = o.optDouble("testTilt", c.emblem.testTilt)
                c.emblem.tiltTarget = o.optDouble("tiltTarget", c.emblem.tiltTarget)
                c.emblem.holdMs = o.optLong("holdMs", c.emblem.holdMs)
                c.emblem.minFrames = o.optInt("minFrames", c.emblem.minFrames)
                c.emblem.extraMs = o.optLong("extraMs", c.emblem.extraMs)
                c.emblem.darkMax = o.optDouble("darkMax", c.emblem.darkMax)
                c.emblem.minTiltFrames = o.optInt("minTiltFrames", c.emblem.minTiltFrames)
                c.emblem.tiltSide = o.optDouble("tiltSide", c.emblem.tiltSide)
                c.emblem.direction = o.optString("direction", c.emblem.direction)
                c.emblem.policy = o.optString("policy", c.emblem.policy)
            }
            j.obj("face").let { o ->
                c.face.strongChroma = o.optDouble("strongChroma", c.face.strongChroma)
                c.face.strongColourMax = o.optDouble("strongColourMax", c.face.strongColourMax)
                c.face.inset = o.optDouble("inset", c.face.inset)
                c.face.policy = o.optString("policy", c.face.policy)
            }
            j.obj("evidence").let { o ->
                c.evidence.maxFrames = o.optInt("maxFrames", c.evidence.maxFrames)
                c.evidence.maxBytes = o.optLong("maxBytes", c.evidence.maxBytes)
            }

            c.profile = parseProfile(root.obj("profiles"), kind)
            return c
        }

        private fun readLaminateDoc(o: JSONObject, d: LaminateDoc) {
            d.laminatePolicy = o.optString("laminatePolicy", d.laminatePolicy)
            d.holdMs = o.optLong("holdMs", d.holdMs)
            d.tiltShare = o.optDouble("tiltShare", d.tiltShare)
            d.evidence = o.optString("evidence", d.evidence)
        }

        private fun parseProfile(profiles: JSONObject, profileKind: String): Profile {
            val p = profiles.optJSONObject(profileKind)
                ?: throw IllegalArgumentException(
                    "no profile '$profileKind' in $ASSET_FILE (available: ${profiles.names()})"
                )

            val feature = p.optJSONObject("feature")
            val defaultRoi = Roi(0.35, 0.3, 0.3, 0.3)
            val controlList = p.optJSONArray("controls")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.toRoi(Roi(0.0, 0.0, 0.0, 0.0))
                }
            } ?: emptyList()

            val pr = Profile()
            pr.raw = p
            pr.id = p.optString("id", profileKind)
            pr.kind = p.optString("kind", profileKind)
            pr.aspect = p.optDouble("aspect", 0.0)
            pr.featureKind = feature?.optString("kind", "none") ?: "none"
            pr.featureRoi = feature?.optJSONObject("roi")?.toRoi(defaultRoi) ?: defaultRoi
            pr.controls = controlList
            pr.iridescent = p.optBoolean("iridescent", false)
            pr.page = p.optBoolean("page", false)
            pr.spread = p.optBoolean("spread", false)
            pr.sheet = p.optBoolean("sheet", false)
            pr.minFill = p.optDouble("minFill", 0.0)
            return pr
        }

        private fun JSONObject.toRoi(def: Roi) = Roi(
            optDouble("x", def.x), optDouble("y", def.y),
            optDouble("w", def.w), optDouble("h", def.h),
        )
    }
}
