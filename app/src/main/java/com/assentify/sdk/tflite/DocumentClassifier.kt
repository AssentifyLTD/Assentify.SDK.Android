package com.assentify.sdk.tflite.LivenessDetection.Finder

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Which document is in front of the camera: the 25-class model `model_A_all25_dynamic.tflite` (MobileNetV3-Small).
 *
 *  - input  [1, 256, 256, 3] float32 RGB, values 0..255 (the model rescales itself: x / 127.5 - 1; do NOT normalise)
 *  - output [1, 25] softmax, classes in alphabetical folder order (LABELS below)
 *
 * The whole upright camera frame is squashed to 256 x 256 (bilinear), as image_dataset_from_directory does when training.
 * Not thread-safe: use one instance from the analysis thread only, and close() it when the screen goes away.
 */
class DocumentClassifier @JvmOverloads constructor(
    context: Context,
    assetName: String = MODEL_FILE,
    threads: Int = 2,
) : AutoCloseable {

    class Result(
        @JvmField val label: String,          // e.g. "lebanese-passport"
        @JvmField val index: Int,
        @JvmField val confidence: Float,      // softmax probability of the label, 0..1
        @JvmField val scores: FloatArray,     // all 25 probabilities, in LABELS order
    ) {
        override fun toString(): String = "$label (%.2f)".format(confidence)
    }

    private val interpreter: Interpreter
    private val input: ByteBuffer = ByteBuffer.allocateDirect(4 * SIZE * SIZE * 3).order(ByteOrder.nativeOrder())
    private val output = Array(1) { FloatArray(LABELS.size) }
    private val pixels = IntArray(SIZE * SIZE)

    init {
        interpreter = Interpreter(loadModel(context, assetName), Interpreter.Options().setNumThreads(threads))
        val inShape = interpreter.getInputTensor(0).shape()
        val outShape = interpreter.getOutputTensor(0).shape()
        require(inShape.contentEquals(intArrayOf(1, SIZE, SIZE, 3))) {
            "unexpected classifier input ${inShape.contentToString()} (want [1, $SIZE, $SIZE, 3])"
        }
        require(outShape.contentEquals(intArrayOf(1, LABELS.size))) {
            "unexpected classifier output ${outShape.contentToString()} (want [1, ${LABELS.size}])"
        }
    }

    /** Classifies an UPRIGHT frame (the same orientation the person sees). */
    fun classify(frame: Bitmap): Result {
        val scaled = if (frame.width == SIZE && frame.height == SIZE) frame
        else Bitmap.createScaledBitmap(frame, SIZE, SIZE, true)
        scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        if (scaled !== frame) scaled.recycle()

        input.rewind()
        for (p in pixels) {                                           // raw RGB 0..255: the model rescales itself
            input.putFloat(((p shr 16) and 255).toFloat())
            input.putFloat(((p shr 8) and 255).toFloat())
            input.putFloat((p and 255).toFloat())
        }
        input.rewind()
        interpreter.run(input, output)

        val scores = output[0].copyOf()
        var best = 0
        for (i in 1 until scores.size) if (scores[i] > scores[best]) best = i
        return Result(LABELS[best], best, scores[best], scores)
    }

    override fun close() = interpreter.close()

    companion object {
        const val MODEL_FILE = "classification.tflite"
        const val SIZE = 256

        /** The model's classes, in output order (alphabetical folder order of the training set). */
        @JvmField
        val LABELS: List<String> = listOf(
            "Car-Registration-New-Front",
            "Car-Registration-New-Rear",
            "Car-Registration-Old-Front",
            "Car-Registration-Old-Rear",
            "Old-Lawyer-Card",
            "lebanese-army-card",
            "lebanese-army-services",
            "lebanese-civil-extract",
            "lebanese-civil-extract-old",
            "lebanese-civil-id-back",
            "lebanese-civil-id-front",
            "lebanese-driving-license",
            "lebanese-form-2",
            "lebanese-gdgs-card",
            "lebanese-isf-card",
            "lebanese-lawyer-card",
            "lebanese-passport",
            "lebanese-resident-permit-back",
            "lebanese-resident-permit-front",
            "lebanese-temporary-resident-permit-back",
            "lebanese-temporary-resident-permit-front",
            "palestinian-id-back",
            "palestinian-id-front",
            "syrian-civil-id-back",
            "syrian-civil-id-front",
        )

        /**
         * Model class -> profile kind in rectifier_config_file.json. Classes with no profile in the config are absent:
         * for them kindFor() returns null and the local liveness check is skipped.
         */
        @JvmField
        val LABEL_TO_KIND: Map<String, String> = mapOf(
            "lebanese-passport" to "lb_passport",
            "lebanese-civil-id-front" to "lb_civil_id",
            "lebanese-civil-id-back" to "lb_civil_id_back",
            "lebanese-driving-license" to "lb_driving_licence",
            "lebanese-civil-extract" to "lb_civil_extract",
            "lebanese-civil-extract-old" to "lb_civil_extract",
            "lebanese-resident-permit-front" to "lb_residence_permit",
        )

        /** The config kind for a model class, or null when the config has no profile for it. */
        @JvmStatic
        fun kindFor(label: String): String? = LABEL_TO_KIND[label]

        private fun loadModel(context: Context, name: String): MappedByteBuffer =
            context.assets.openFd(name).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { ch ->
                    ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
    }
}