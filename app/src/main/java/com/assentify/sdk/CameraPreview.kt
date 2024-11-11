package com.assentify.sdk

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.LivenessType
import com.assentify.sdk.Core.FileUtils.ImageUtils
import com.assentify.sdk.FaceMatch.CountDownCallback
import com.assentify.sdk.tflite.Classifier
import com.assentify.sdk.tflite.Classifier.Recognition
import com.assentify.sdk.tflite.DetectorFactory
import com.assentify.sdk.tflite.Liveness.CheckLiveness
import com.assentify.sdk.tflite.YoloV5Classifier
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.Base64
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class CameraPreview : Fragment() {

    private var enableDetect: Boolean = false;
    private var enableGuide: Boolean = false;

    private lateinit var cameraExecutor: ExecutorService
    private var cardBackground: ImageView? = null;
    private var cardContainer: LinearLayout? = null;
    private var faceContainer: LinearLayout? = null;
    private var faceBackground: LinearLayout? = null;
    private var transmittingContainer: LinearLayout? = null;

    private lateinit var previewView: PreviewView
    private var detector: YoloV5Classifier? = null
    private var cropSize = ConstantsValues.InputSize
    private var sensorOrientation: Int? = null
    private var results: List<Recognition> = ArrayList()
    private val listRectF: MutableList<RectFInfo> = mutableListOf()
    private lateinit var rectangleOverlayView: RectangleOverlayView
    private var cameraSelector: CameraSelector? = null;
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


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

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


    fun closeCamera() {
        imageAnalysisListener = null

        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null

        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }

        camera?.let {
            it.cameraControl.cancelFocusAndMetering()
            it.cameraControl.enableTorch(false)
            try {
                it.cameraControl.setZoomRatio(1.0f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cameraProvider?.unbindAll()
            camera = null
        }

        cameraProvider = null
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
                        Quality.UHD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.UHD)
                    )
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)





            imageAnalysisListener = ImageAnalysis.Analyzer { image ->
                val scaleImage = ImageUtils.scaleBitmap(image.toBitmap(), sensorOrientation!!)
                requireActivity().runOnUiThread {
                    if (transmittingContainer == null) {
                        transmittingContainer =
                            requireActivity().findViewById(R.id.transmitting_container)
                    }
                    if (enableGuide) {
                        if (frontCamera) {
                            if (faceContainer == null) {
                                faceContainer =
                                    requireActivity().findViewById(R.id.face_container)
                                faceContainer!!.visibility = View.VISIBLE
                            } else {
                                faceContainer!!.visibility = View.VISIBLE
                            }
                            if (faceBackground == null) {
                                faceBackground =
                                    requireActivity().findViewById(R.id.face_background)
                                faceBackground!!.visibility = View.VISIBLE
                            } else {
                                faceBackground!!.visibility = View.VISIBLE
                            }

                        } else {
                            if (cardContainer == null) {
                                cardContainer =
                                    requireActivity().findViewById(R.id.card_container)
                                cardContainer!!.visibility = View.VISIBLE
                            } else {
                                cardContainer!!.visibility = View.VISIBLE
                            }
                            if (cardBackground == null) {
                                cardBackground =
                                    requireActivity().findViewById(R.id.card_background)
                                cardBackground!!.visibility = View.VISIBLE
                            } else {
                                cardBackground!!.visibility = View.VISIBLE
                            }


                        }
                    }

                }


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
                val listScaleRectF = scaleAndDrawLocation(
                    listRectF,
                    image.width,
                    image.height,
                    previewView.width,
                    previewView.height,
                    enableDetect
                );


                processImage(
                    scaleImage!!,
                    image.toBitmap(),
                    results,
                    listScaleRectF,
                    previewView.width,
                    previewView.height,
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
                    this, cameraSelector!!, preview, imageCapture, imageAnalysis, videoCapture
                )
                camera?.cameraControl?.setZoomRatio(0f)
                sensorOrientation = camera!!.cameraInfo.sensorRotationDegrees
            } catch (exc: Exception) {
            }

        }, ContextCompat.getMainExecutor(requireActivity()))

    }


    fun scaleAndDrawLocation(
        listRectF: MutableList<RectFInfo>,
        width: Int,
        height: Int,
        previewWidth: Int,
        previewHeight: Int,
        enableDetect: Boolean
    ): MutableList<Pair<RectF, String>> {

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
        if (enableDetect) {
            rectangleOverlayView.setListRect(listScaleRectF)
        } else {
            listScaleRectF.clear();
            rectangleOverlayView.setListRect(listScaleRectF)
        }
        return listScaleRectF;

    }


    protected abstract fun processImage(
        croppedBitmap: Bitmap,
        normalImage: Bitmap,
        results: List<Classifier.Recognition>,
        listScaleRectF: MutableList<Pair<RectF, String>>,
        previewWidth: Int,
        previewHeight: Int,
    )

    fun setRectFCustomColor(
        color: String,
        enableDetect: Boolean,
        enableGuide: Boolean,
        notTransmitting: Boolean,
    ) {
        requireActivity().runOnUiThread {
            if (this.isVisible) {
                rectangleOverlayView.setCustomColor(color)
                if (notTransmitting) {
                    this.enableDetect = enableDetect;
                    this.enableGuide = enableGuide;
                    if (transmittingContainer != null) {
                        transmittingContainer!!.visibility = View.GONE
                    }
                } else {
                    this.enableDetect = false;
                    this.enableGuide = false;
                    if (faceBackground != null && faceBackground!!.visibility == View.VISIBLE) {
                        faceBackground!!.visibility = View.GONE
                    }
                    if (faceContainer != null && faceContainer!!.visibility == View.VISIBLE) {
                        faceContainer!!.visibility = View.GONE
                    }
                    if (cardBackground != null && cardBackground!!.visibility == View.VISIBLE) {
                        cardBackground!!.visibility = View.GONE
                    }
                    if (cardContainer != null && cardContainer!!.visibility == View.VISIBLE) {
                        cardContainer!!.visibility = View.GONE
                    }
                    if (transmittingContainer != null) {
                        transmittingContainer!!.visibility = View.VISIBLE
                    }
                }

                if (this.enableGuide) {
                    if (faceBackground != null && faceBackground!!.visibility == View.VISIBLE) {
                        val layerDrawable = ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.face_background,
                            null
                        ) as LayerDrawable
                        val shapeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
                        shapeDrawable.setStroke(10, Color.parseColor(color))
                        faceBackground!!.setBackground(layerDrawable)
                    }

                    if (cardBackground != null && cardBackground!!.visibility == View.VISIBLE) {
                        val drawableCard = ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.card_background,
                            null
                        ) as VectorDrawable
                        val wrappedDrawableCard = DrawableCompat.wrap(drawableCard!!)
                        DrawableCompat.setTint(wrappedDrawableCard, Color.parseColor(color))
                        cardBackground!!.setImageDrawable(wrappedDrawableCard)
                    }
                }
            }
        }

    }


    fun capitalizeFirstLetter(input: String): String {
        if (input.isEmpty()) return input
        return input.substring(0, 1).toUpperCase() + input.substring(1)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startRecording() {
        // TODO Later
//        val videoCapture = this.videoCapture ?: return
//        if (recording != null) {
//            return
//        }
//
//        val cacheDir = requireContext().cacheDir
//        val fileName = "video_${System.currentTimeMillis()}.mp4"
//        val file = File(cacheDir, fileName)
//
//        recording = videoCapture.output
//            .prepareRecording(requireContext(), FileOutputOptions.Builder(file).build())
//            .apply {
//                // Enable Audio for recording
//
//            }
//            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
//                when (recordEvent) {
//                    is VideoRecordEvent.Start -> {
//                    }
//
//                    is VideoRecordEvent.Finalize -> {
//
//                        if (!recordEvent.hasError()) {
//                            val videoBytes = file.readBytes()
//                            val base64Encoded = Base64.getEncoder().encodeToString(videoBytes)
//                           // onStopRecordVideo(base64Encoded, file)
//                            onStopRecordVideo()
//                        } else {
//                            recording?.close()
//                            recording = null
//                        }
//                    }
//                }
//            }
    }


    fun stopRecording() {
        // TODO Later
        onStopRecordVideo()
//
//        val curRecording = recording ?: return
//        curRecording.stop()
//        recording = null
    }

    // TODO Later
    //  protected abstract fun onStopRecordVideo(videoBase64: String, video: File)
    protected abstract fun onStopRecordVideo()
    protected fun showCountDown(
        callback: CountDownCallback,
        color: String,
        isCountDownStarted: Boolean,
    ) {
        if (isCountDownStarted) {
            requireActivity().runOnUiThread {
                val countDownContainer =
                    requireActivity().findViewById<View>(R.id.countDownContainer) as LinearLayout
                countDownContainer.visibility = View.VISIBLE
                val countDownText =
                    requireActivity().findViewById<View>(R.id.countDownText) as TextView
                countDownText.visibility = View.VISIBLE
                countDownText.setTextColor(Color.parseColor(color))
                var counter = 3;
                Handler(Looper.getMainLooper()).postDelayed({
                    object : CountDownTimer(3000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            countDownText.text = counter.toString()
                            counter--;
                        }

                        override fun onFinish() {
                            countDownText.visibility = View.GONE
                            // Call the callback's method
                            callback.onCountDownFinished()
                        }
                    }.start()
                }, 1000)
            }
        }

    }


}
