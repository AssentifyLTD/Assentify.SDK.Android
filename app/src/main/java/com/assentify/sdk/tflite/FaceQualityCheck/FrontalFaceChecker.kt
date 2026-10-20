package com.assentify.sdk.tflite.FaceQualityCheck

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.assentify.sdk.Core.FileUtils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot

data class FrontalFaceResult(
    val face: Face,
    val yaw: Float,           // degrees, 0 = facing camera
    val pitch: Float,         // degrees
    val roll: Float,          // degrees
    val noseOffset: Float,    // landmark-based yaw proxy, 0 = nose centered between eyes
    val mouthOffset: Float,   // landmark-based yaw proxy, 0 = mouth centered between eyes
    val leftEyeOpen: Float,   // 0...1
    val rightEyeOpen: Float   // 0...1
)

/**
 * Returns a result ONLY when exactly one face is looking straight at the screen
 * for [Limits.requiredStableFrames] consecutive frames. Returns null for: no face,
 * multiple faces, small face, turned/tilted head, or eyes closed.
 *
 * Frames that arrive while a previous frame is still being processed are dropped
 * (the callback is not called for them).
 */
class FrontalFaceChecker(val limits: Limits = Limits()) {

    data class Limits(
        val maxYawDegrees: Float = 5f,
        val maxRollDegrees: Float = 5f,
        /** Pitch is measured relative to this value, not 0. Phones sit below eye level,
         *  so a straight head reads a few degrees off. Measured on your device:
         *  looking straight reads ≈ -3…-8°, so the center is -6°. */
        val pitchCenterDegrees: Float = -6f,
        val maxPitchDeviation: Float = 5f,      // accepted pitch = center ± this (-11…-1°)
        val maxNoseOffset: Float = 0.08f,       // nose shift along eye line / inter-eye distance
        val maxMouthOffset: Float = 0.08f,      // mouth shift along eye line / inter-eye distance
        val minEyeOpenProbability: Float = 0.6f,
        val minFaceWidth: Float = 0.20f,        // fraction of upright image width
        val requiredStableFrames: Int = 3
    )

    /** Set to false to silence logs in production. */
    var debugLogging = false

    // Created ONCE and reused. Contours are off on purpose: with contours enabled,
    // ML Kit only returns the most prominent face, which would hide a second face.
    // FAST mode: ACCURATE took ~550 ms per frame in your logs.
    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
    )

    private val busy = AtomicBoolean(false)
    @Volatile private var stableCount = 0

    // MARK: - Logging

    private fun log(message: String) {
        if (debugLogging) Log.d("FrontalFace", message)
    }

    private fun f(v: Float) = "%.2f".format(v)

    // MARK: - Public API

    /**
     * The bitmap is rotated by 270° with ImageUtils.rotateBitmap (same as FaceQualityCheck)
     * before detection, so it is already upright and [rotationDegrees] should stay 0.
     */
    fun check(bitmap: Bitmap, rotationDegrees: Int = 0, callback: (FrontalFaceResult?) -> Unit) {
        if (busy.get()) return  // skip the rotation work too while a frame is still running
        val upright = ImageUtils.rotateBitmap(bitmap, 270f)
        check(InputImage.fromBitmap(upright, rotationDegrees), callback)
    }

    fun check(image: InputImage, callback: (FrontalFaceResult?) -> Unit) {
        if (!busy.compareAndSet(false, true)) return  // previous frame still running

        // Face coordinates are in the upright frame, so swap width/height for 90/270.
        val uprightWidth = when (image.rotationDegrees) {
            90, 270 -> image.height
            else -> image.width
        }
        log("---- frame ${image.width}x${image.height} rotation=${image.rotationDegrees} ----")

        detector.process(image)
            .addOnSuccessListener { faces ->
                busy.set(false)
                val result = evaluate(faces, uprightWidth)
                if (result == null) {
                    stableCount = 0
                    callback(null)
                    return@addOnSuccessListener
                }
                stableCount++
                if (stableCount < limits.requiredStableFrames) {
                    log("⏳ frontal $stableCount/${limits.requiredStableFrames} — hold still")
                    callback(null)
                    return@addOnSuccessListener
                }
                log("✅ FRONTAL")
                callback(result)
            }
            .addOnFailureListener { e ->
                busy.set(false)
                stableCount = 0
                log("❌ detector failed: ${e.message}")
                callback(null)
            }
    }

    /** Call when starting a new capture session. */
    fun resetStability() {
        stableCount = 0
    }

    fun stop() {
        detector.close()
    }

    // MARK: - Evaluation

    private fun evaluate(faces: List<Face>, imageWidth: Int): FrontalFaceResult? {
        if (faces.isEmpty()) {
            log("❌ no face detected (if this always happens, check the rotation)")
            return null
        }
        if (faces.size != 1) {
            log("❌ ${faces.size} faces detected, need exactly 1")
            return null
        }
        val face = faces[0]

        // 1. Face size
        val widthRatio = face.boundingBox.width().toFloat() / imageWidth
        log("face box=${face.boundingBox} widthRatio=${f(widthRatio)}")
        if (widthRatio < limits.minFaceWidth) {
            log("❌ face too small: ${f(widthRatio)} < ${f(limits.minFaceWidth)} — move closer")
            return null
        }

        // 2. Head pose (ML Kit: X = pitch, Y = yaw, Z = roll)
        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ
        log("pose yaw=${f(yaw)}° pitch=${f(pitch)}° roll=${f(roll)}°")

        if (abs(yaw) > limits.maxYawDegrees) {
            log("❌ yaw ${f(yaw)}° exceeds ±${f(limits.maxYawDegrees)}°")
            return null
        }
        val pitchMin = limits.pitchCenterDegrees - limits.maxPitchDeviation
        val pitchMax = limits.pitchCenterDegrees + limits.maxPitchDeviation
        if (pitch < pitchMin || pitch > pitchMax) {
            log("❌ pitch ${f(pitch)}° outside ${f(pitchMin)}…${f(pitchMax)}°")
            return null
        }
        if (abs(roll) > limits.maxRollDegrees) {
            log("❌ roll ${f(roll)}° exceeds ±${f(limits.maxRollDegrees)}° (≈90° means wrong rotation)")
            return null
        }

        // 3. Landmarks
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

        log("landmarks: leftEye=${leftEye != null} rightEye=${rightEye != null} " +
                "nose=${nose != null} mouthLeft=${mouthLeft != null} mouthRight=${mouthRight != null}")

        if (leftEye == null || rightEye == null || nose == null ||
            mouthLeft == null || mouthRight == null) {
            log("❌ required landmark missing (see line above)")
            return null
        }

        val dx = rightEye.x - leftEye.x
        val dy = rightEye.y - leftEye.y
        val eyeDistance = hypot(dx, dy)
        if (eyeDistance < 1f || abs(dx) < 1e-3f) {
            log("❌ eye distance too small: ${f(eyeDistance)}px")
            return null
        }

        // Unit vector along the eye line (makes the checks below roll-invariant)
        val ux = dx / eyeDistance
        val uy = dy / eyeDistance

        // 4. Landmark roll: angle of the eye line (cross-checks ML Kit's roll)
        val eyeLineRoll = Math.toDegrees(atan((dy / dx).toDouble())).toFloat()
        log("eyeLineRoll=${f(eyeLineRoll)}° eyeDistance=${f(eyeDistance)}px")
        if (abs(eyeLineRoll) > limits.maxRollDegrees) {
            log("❌ eye-line roll ${f(eyeLineRoll)}° exceeds ±${f(limits.maxRollDegrees)}°")
            return null
        }

        // 5. Landmark yaw: nose and mouth must sit centered between the eyes
        val mid = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        fun offsetAlongEyeLine(p: PointF) =
            ((p.x - mid.x) * ux + (p.y - mid.y) * uy) / eyeDistance

        val noseOffset = offsetAlongEyeLine(nose)
        val mouthCenter = PointF((mouthLeft.x + mouthRight.x) / 2f, (mouthLeft.y + mouthRight.y) / 2f)
        val mouthOffset = offsetAlongEyeLine(mouthCenter)
        log("noseOffset=${f(noseOffset)} (±${f(limits.maxNoseOffset)}) " +
                "mouthOffset=${f(mouthOffset)} (±${f(limits.maxMouthOffset)})")

        if (abs(noseOffset) > limits.maxNoseOffset) {
            log("❌ nose not centered between eyes")
            return null
        }
        if (abs(mouthOffset) > limits.maxMouthOffset) {
            log("❌ mouth not centered between eyes")
            return null
        }

        // 6. Eyes open
        val leftOpen = face.leftEyeOpenProbability
        val rightOpen = face.rightEyeOpenProbability
        log("eyeOpen left=${leftOpen?.let { f(it) }} right=${rightOpen?.let { f(it) }} " +
                "(min ${f(limits.minEyeOpenProbability)})")
        if (leftOpen == null || rightOpen == null ||
            leftOpen < limits.minEyeOpenProbability || rightOpen < limits.minEyeOpenProbability) {
            log("❌ eye(s) closed or unknown")
            return null
        }

        return FrontalFaceResult(
            face = face,
            yaw = yaw,
            pitch = pitch,
            roll = roll,
            noseOffset = noseOffset,
            mouthOffset = mouthOffset,
            leftEyeOpen = leftOpen,
            rightEyeOpen = rightOpen
        )
    }
}