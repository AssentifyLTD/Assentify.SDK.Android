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
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    private val faceDetectorWink: FaceDetector by lazy {
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
    var throttleDelayMillis: Long = 400L

    private var lastProcessedTimeWink = 0L
    var throttleDelayMillisWink: Long = 400L

    fun checkQualityAction(bitmapImage: Bitmap, callback: FaceEventCallback) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < throttleDelayMillis) {
            return // Skip this frame
        }
        lastProcessedTime = currentTime

        val image = InputImage.fromBitmap(ImageUtils.rotateBitmap(bitmapImage, 270f), 0)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    callback.onFaceEventDetected(FaceEvents.NO_DETECT)
                    return@addOnSuccessListener
                }

                var faceEvent = FaceEvents.Good
                for (face in faces) {
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

                    // Roll (Z)
                    if (face.headEulerAngleZ > ConstantsValues.FaceCheckQualityThresholdPositive) {
                        faceEvent = FaceEvents.RollRight
                    } else if (face.headEulerAngleZ < ConstantsValues.FaceCheckQualityThresholdNegative) {
                        faceEvent = FaceEvents.RollLeft
                    }

                    // Pitch (X)
                    if (face.headEulerAngleX > ConstantsValues.FaceCheckQualityThresholdPositive) {
                        faceEvent = FaceEvents.PitchUp
                    } else if (face.headEulerAngleX < ConstantsValues.FaceCheckQualityThresholdNegative) {
                        faceEvent = FaceEvents.PitchDown
                    }

                    // Yaw (Y)
                    if (face.headEulerAngleY > ConstantsValues.FaceCheckQualityThresholdPositive) {
                        faceEvent = FaceEvents.YawLeft
                    } else if (face.headEulerAngleY < ConstantsValues.FaceCheckQualityThresholdNegative) {
                        faceEvent = FaceEvents.YawRight
                    }

                    callback.onFaceEventDetected(faceEvent)
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener {
                callback.onFaceEventDetected(FaceEvents.NO_DETECT)
            }
    }

    fun checkQualityWink(bitmapImage: Bitmap, callback: FaceEventCallback) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTimeWink < throttleDelayMillisWink) {
            return // Skip this frame
        }
        lastProcessedTimeWink = currentTime
        val image = InputImage.fromBitmap(ImageUtils.rotateBitmap(bitmapImage, 270f), 0)

        faceDetectorWink.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    callback.onFaceEventDetected(FaceEvents.NO_DETECT)
                    return@addOnSuccessListener
                }

                for (face in faces) {
                    val leftProb = face.leftEyeOpenProbability ?: -1f
                    val rightProb = face.rightEyeOpenProbability ?: -1f

                    val isRightWink = leftProb > 0.8f && rightProb < 0.3f
                    val isLeftWink = rightProb > 0.8f && leftProb < 0.3f

                    when {
                        isRightWink -> {
                            callback.onFaceEventDetected(FaceEvents.WinkLeft)
                            return@addOnSuccessListener
                        }
                        isLeftWink -> {
                            callback.onFaceEventDetected(FaceEvents.WinkRight)
                            return@addOnSuccessListener
                        }
                        else -> {
                            callback.onFaceEventDetected(FaceEvents.NO_DETECT)
                            return@addOnSuccessListener
                        }
                    }
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
