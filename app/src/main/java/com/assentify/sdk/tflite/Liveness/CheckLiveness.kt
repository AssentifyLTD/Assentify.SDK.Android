package com.assentify.sdk.tflite.Liveness

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.assentify.sdk.Core.Constants.ConstantsValues.InputFaceModelsSize
import com.assentify.sdk.Core.Constants.ConstantsValues.ModelLiveModelFileName
import com.assentify.sdk.Core.Constants.LivenessType
import com.assentify.sdk.Core.FileUtils.FileUtils
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import java.nio.ByteBuffer


class CheckLiveness {

    private var interpreter: Interpreter? = null

    fun loadTfliteModel(context: Context) {
            val fileUtils = FileUtils()
            val tfliteModel = fileUtils.loadModelFile(context.assets, ModelLiveModelFileName)
            interpreter = Interpreter(tfliteModel)
    }

    private fun preprocessBitmap(bitmap: Bitmap): TensorImage {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, InputFaceModelsSize, InputFaceModelsSize, true)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val imageProcessor = ImageProcessor.Builder()
            .build()

        return imageProcessor.process(tensorImage)
    }


    private fun predictImage(inputArray: ByteBuffer): Array<FloatArray> {
        val outputData = Array(1) { FloatArray(1) }
        interpreter!!.run(inputArray, outputData)
        return outputData;
    }

    fun preprocessAndPredict(bitmap: Bitmap): LivenessType {
        val tensorImage = preprocessBitmap(bitmap)
        val outputData = interpreter?.let { predictImage(tensorImage.buffer) }
        return if (outputData != null && outputData!![0][0] > 0.5f) LivenessType.NOT_LIVE else LivenessType.LIVE
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

}
