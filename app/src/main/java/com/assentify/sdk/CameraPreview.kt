package com.assentify.sdk

import com.assentify.sdk.R
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import  com.assentify.sdk.Core.Constants.ConstantsValues
import  com.assentify.sdk.Core.FileUtils.ImageUtils
import  com.assentify.sdk.tflite.Classifier
import  com.assentify.sdk.tflite.Classifier.Recognition
import  com.assentify.sdk.tflite.DetectorFactory
import  com.assentify.sdk.tflite.YoloV5Classifier
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.Base64
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class CameraPreview : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var detector: YoloV5Classifier? = null
    private var cropSize = ConstantsValues.InputSize
    private var sensorOrientation: Int? = null
    private var results: List<Recognition> = ArrayList()
    private val listRectF: MutableList<RectFInfo> = mutableListOf()
    private lateinit var rectangleOverlayView: RectangleOverlayView
    private lateinit var cameraSelector: CameraSelector
    private var frontCamera: Boolean = false
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageAnalysisListener: ImageAnalysis.Analyzer? = null
    private var imageCapture: ImageCapture? = null;
    private var videoCapture: VideoCapture<Recorder>? = null;
    private var recording: Recording? = null
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private var counter = 0;


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.camera_preview, container, false)
        previewView = view.findViewById(R.id.previewView)
        rectangleOverlayView = view.findViewById(R.id.rectangleOverlayView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detector = DetectorFactory.getDetector(requireContext().assets)
        startCamera();
        cameraExecutor = Executors.newCachedThreadPool()
    }

    fun frontCamera() {
        frontCamera = true
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture?.addListener({
            cameraProvider = cameraProviderFuture?.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.SD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                    )
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)


            imageAnalysisListener = ImageAnalysis.Analyzer { image ->
                val scaleImage = ImageUtils.scaleBitmap(image.toBitmap(), sensorOrientation!!)
                results = if (frontCamera) {
                    detector!!.recognizeImage(ImageUtils.mirrorBitmap(scaleImage))
                } else {
                    detector!!.recognizeImage(scaleImage)
                }
                listRectF.clear()
                results.forEach { item ->
                    val confidence = (item.confidence * 100).toInt()
                    val className = capitalizeFirstLetter(item.title);
                    listRectF.add(RectFInfo(item.location, confidence.toString(), className))
                }
                processImage(scaleImage!!, image.toBitmap(), results)
                scaleAndDrawLocation(
                    listRectF,
                    image.width,
                    image.height,
                    previewView.width,
                    previewView.height
                )


                image.close()
            }

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, imageAnalysisListener!!)
                }

            cameraSelector = if (frontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis, videoCapture
                )
                camera?.cameraControl?.setZoomRatio(0f)
                sensorOrientation = camera!!.cameraInfo.sensorRotationDegrees
            } catch (exc: Exception) {
            }

        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    fun closeCamera() {
        cameraExecutor.shutdown()
        camera?.let {
            it.cameraControl?.cancelFocusAndMetering()
            it.cameraControl?.enableTorch(false)
            it.cameraControl?.setZoomRatio(0f)
            cameraProvider?.unbindAll()
            camera = null
        }
        imageAnalysisListener = null
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
    }


    fun scaleAndDrawLocation(
        listRectF: MutableList<RectFInfo>,
        width: Int,
        height: Int,
        previewWidth: Int,
        previewHeight: Int
    ) {
        val listScaleRectF: MutableList<Pair<RectF, String>> = mutableListOf()
        listRectF.forEach { rectInfo ->
            val location = rectInfo.rectF
            val widthImageScale: Float = (width.toFloat() / cropSize)
            val heightImageScale: Float = (height.toFloat() / cropSize)
            var imageLeft = location.left * widthImageScale
            val imageTop = location.top * heightImageScale
            val imageRight = location.right * widthImageScale
            val imageBottom = location.bottom * heightImageScale
            val widthFullScreenScale: Float = (previewWidth / width.toFloat())
            val heightFullScreenScale: Float = (previewHeight / height.toFloat())

            if (cameraSelector != CameraSelector.DEFAULT_FRONT_CAMERA) {
                imageLeft = imageLeft * widthFullScreenScale - (previewWidth / 40)
            } else {
                imageLeft *= widthFullScreenScale
            }
            listScaleRectF.add(
                Pair(
                    RectF(
                        imageLeft,
                        imageTop * heightFullScreenScale,
                        imageRight * widthFullScreenScale,
                        imageBottom * heightFullScreenScale
                    ),
                    "${rectInfo.className}:${rectInfo.confidence}"
                )
            )
        }
        rectangleOverlayView.setListRect(listScaleRectF)
    }


    protected abstract fun processImage(
        croppedBitmap: Bitmap,
        normalImage: Bitmap,
        results: List<Classifier.Recognition>
    )

    fun setRectFCustomColor(color: String) {
        rectangleOverlayView.setCustomColor(color)
    }


    fun capitalizeFirstLetter(input: String): String {
        if (input.isEmpty()) return input
        return input.substring(0, 1).toUpperCase() + input.substring(1)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        if (recording != null) {
            return
        }

        val cacheDir = requireContext().cacheDir
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val file = File(cacheDir, fileName)

        recording = videoCapture.output
            .prepareRecording(requireContext(), FileOutputOptions.Builder(file).build())
            .apply {
                // Enable Audio for recording

            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                    }

                    is VideoRecordEvent.Finalize -> {

                        if (!recordEvent.hasError()) {
                            val videoBytes = file.readBytes()
                            val base64Encoded = Base64.getEncoder().encodeToString(videoBytes)
                            onStopRecordVideo(base64Encoded, file)
                        } else {
                            recording?.close()
                            recording = null
                        }
                    }
                }
            }
    }


    fun stopRecording() {

        val curRecording = recording ?: return
        curRecording.stop()
        recording = null
    }

    protected abstract fun onStopRecordVideo(videoBase64: String, video: File)


}
