package com.assentify.sdk.tflite.FaceQualityCheck

import android.graphics.Bitmap
import android.util.Log
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.FaceEvents
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark


interface FaceEventCallback {
    fun onFaceEventDetected(faceEvent: FaceEvents?)
}

class FaceQualityCheck {

    private var faceDetector: FaceDetector? = null;

    fun checkQuality(bitmapImage: Bitmap, callback: FaceEventCallback) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL).build()
        faceDetector = FaceDetection.getClient(options)

        var faceEvent = FaceEvents.Good
        val image = InputImage.fromBitmap(bitmapImage, 0)

        faceDetector!!.process(image).addOnSuccessListener { faces ->
                for (face in faces) {

                    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                    val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
                    val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

                    if(leftEye != null && rightEye != null && nose != null && mouthLeft != null && mouthRight != null){
                        /** Roll Check  **/
                        if (face.headEulerAngleZ > ConstantsValues.FaceCheckQualityThresholdPositive) {
                            faceEvent = FaceEvents.RollRight
                        } else if (face.headEulerAngleZ < ConstantsValues.FaceCheckQualityThresholdNegative) {
                            faceEvent = FaceEvents.RollLeft
                        }

                        /** Pitch Check  **/
                        if (face.headEulerAngleX >  ConstantsValues.FaceCheckQualityThresholdPositive) {
                            faceEvent = FaceEvents.PitchUp
                        } else if (face.headEulerAngleX < ConstantsValues.FaceCheckQualityThresholdNegative) {
                            faceEvent = FaceEvents.PitchDown
                        }

                        /** Yaw Check  **/
                        if (face.headEulerAngleY >  ConstantsValues.FaceCheckQualityThresholdPositive) {
                            faceEvent = FaceEvents.YawLeft
                        } else if (face.headEulerAngleY < ConstantsValues.FaceCheckQualityThresholdNegative) {
                            faceEvent = FaceEvents.YawRight
                        }
                        callback.onFaceEventDetected(faceEvent)
                    }else{
                        callback.onFaceEventDetected(FaceEvents.NO_DETECT)
                    }
                }
            }.addOnFailureListener { e ->
                callback.onFaceEventDetected(FaceEvents.NO_DETECT)
            }



    }

}