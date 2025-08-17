package com.assentify.sdk.tflite.FaceQualityCheck

import android.graphics.Bitmap
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.FaceEvents
import com.assentify.sdk.Core.FileUtils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

interface FaceEventCallback {
    fun onFaceEventDetected(faceEvent: FaceEvents?)
}

class FaceQualityCheck {

    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }




    private var lastProcessedTime = 0L



    fun checkFaceQualityAndExpressions(bitmapImage: Bitmap, callback: FaceEventCallback,throttleDelayMillis:Long) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < throttleDelayMillis) {
            return // Skip this frame
        }
        lastProcessedTime = currentTime

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // 👈 important for wink/blink
            .build()

        val faceDetector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(ImageUtils.rotateBitmap(bitmapImage, 270f), 0)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    callback.onFaceEventDetected(FaceEvents.NO_DETECT)
                    return@addOnSuccessListener
                }

                for (face in faces) {
                    // 1. Landmark presence check
                    val hasAllLandmarks = listOf(
                        face.getLandmark(FaceLandmark.LEFT_EYE),
                        face.getLandmark(FaceLandmark.RIGHT_EYE),
                        face.getLandmark(FaceLandmark.NOSE_BASE),
                        face.getLandmark(FaceLandmark.MOUTH_LEFT),
                        face.getLandmark(FaceLandmark.MOUTH_RIGHT)
                    ).all { it != null }

                    if (!hasAllLandmarks) {
                        callback.onFaceEventDetected(FaceEvents.NO_DETECT)
                        return@addOnSuccessListener
                    }

                    // 2. Head orientation checks
                    var faceEvent: FaceEvents = FaceEvents.Good
                    if (face.headEulerAngleZ > ConstantsValues.FaceCheckQualityThresholdPositive) {
                        faceEvent = FaceEvents.RollRight
                    } else if (face.headEulerAngleZ < ConstantsValues.FaceCheckQualityThresholdNegative) {
                        faceEvent = FaceEvents.RollLeft
                    }

                    if (face.headEulerAngleX > ConstantsValues.FaceCheckQualityThresholdPositive) {
                        faceEvent = FaceEvents.PitchUp
                    } else if (face.headEulerAngleX < ConstantsValues.FaceCheckQualityThresholdNegative) {
                        faceEvent = FaceEvents.PitchDown
                    }

                    if (face.headEulerAngleY > ConstantsValues.FaceCheckQualityThresholdPositive) {
                        faceEvent = FaceEvents.YawLeft
                    } else if (face.headEulerAngleY < ConstantsValues.FaceCheckQualityThresholdNegative) {
                        faceEvent = FaceEvents.YawRight
                    }

                    // 3. Blink / Wink detection
                    val leftProb = face.leftEyeOpenProbability ?: -1f
                    val rightProb = face.rightEyeOpenProbability ?: -1f

                    val isRightWink = leftProb > 0.8f && rightProb < 0.3f
                    val isLeftWink = rightProb > 0.8f && leftProb < 0.3f
                    val isBlink = leftProb < 0.3f && rightProb < 0.3f

                    when {
                        isBlink -> faceEvent = FaceEvents.BLINK
                        isRightWink -> faceEvent = FaceEvents.WinkLeft
                        isLeftWink -> faceEvent = FaceEvents.WinkRight
                    }

                    callback.onFaceEventDetected(faceEvent)
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener {
                callback.onFaceEventDetected(FaceEvents.NO_DETECT)
            }
    }




    fun stop() {
        faceDetector.close()
    }
}
