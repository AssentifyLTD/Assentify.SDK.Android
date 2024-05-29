package com.assentify.sdk.FaceMatch;

import static  com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.NonNull;
import  com.assentify.sdk.CameraPreview;
import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;
import  com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import  com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import  com.assentify.sdk.Core.Constants.HubConnectionTargets;
import  com.assentify.sdk.Core.Constants.MotionType;
import  com.assentify.sdk.Core.Constants.RemoteProcessing;
import  com.assentify.sdk.Core.Constants.Routes.EndPointsUrls;
import  com.assentify.sdk.Core.FileUtils.ImageUtils;
import  com.assentify.sdk.Core.FileUtils.StorageUtils;
import com.assentify.sdk.FaceMatch.FaceMatchCallback;import  com.assentify.sdk.Models.BaseResponseDataModel;
import  com.assentify.sdk.CheckEnvironment.DetectMotion;
import  com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import  com.assentify.sdk.RemoteClient.Models.ConfigModel;
import  com.assentify.sdk.tflite.Classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FaceMatch extends CameraPreview implements RemoteProcessingCallback {


    private FaceMatchCallback faceMatchCallback;
    private final EnvironmentalConditions environmentalConditions;
    private String secondImage = "";
    private Bitmap croppedBitmap = null;
    private double brightness;
    private List<Bitmap> highQualityBitmaps = new ArrayList<>();

    private List<Bitmap> bitmaps = new ArrayList<>();
    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();
    private StorageUtils storageUtils = new StorageUtils(
            ConstantsValues.FolderImagesName,
            ConstantsValues.FolderVideosName,
            ConstantsValues.ImageName,
            ConstantsValues.VideoName
    );
    Boolean processMrz;
    Boolean performLivenessDetection;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    Boolean storeImageStream;
    ConfigModel configModel;
    private MotionType motion = MotionType.NO_DETECT;
    private RectF rectFCard = new RectF();
    private List<RectF> motionRectF = new ArrayList<>();

    private List<MotionType> sendingFlags = new ArrayList<>();

    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService createClipsService = Executors.newSingleThreadScheduledExecutor();

    private String  faceMatch = "FaceMatch";
    private  int videoCounter = -1;
    public FaceMatch(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                     Boolean processMrz,
                     Boolean performLivenessDetection,
                     Boolean saveCapturedVideo,
                     Boolean storeCapturedDocument,
                     Boolean storeImageStream
    ) {
        this.apiKey = apiKey;
        frontCamera();
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDetection = performLivenessDetection;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;

    }


    public void setFaceMatchCallback(FaceMatchCallback faceMatchCallback) {
        this.faceMatchCallback = faceMatchCallback;
    }

    public void setSecondImage(String secondImage) {
        this.secondImage = secondImage;
        try {
            remoteProcessing = new RemoteProcessing();
            remoteProcessing.setCallback(this);
        } catch (Exception e) {
        }

    }


    @Override
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results) {

        this.results = results;
        if (hasFaceOrCard()) {
            bitmaps.add(normalImage);
            highQualityBitmaps.add(normalImage);
        }
        this.croppedBitmap = croppedBitmap;
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (result.getDetectedClass() == 1) {
                rectFCard = new RectF(location.left, location.top, location.right, location.bottom);
                motionRectF.add(rectFCard);
            }
        }
        if (motion == MotionType.SENDING) {
            setRectFCustomColor("#61A03A");
        } else {
            setRectFCustomColor(environmentalConditions.getHoldHandColor());
        }

        checkEnvironment();
    }



    protected void checkEnvironment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (hasFaceOrCard() && start) {
                startRecording();
            }
        }
        ImageBrightnessChecker imageBrightnessChecker = new ImageBrightnessChecker();
        DetectMotion detectMotion = new DetectMotion();
        brightness = imageBrightnessChecker.getAverageBrightness(croppedBitmap);

        if (motionRectF.size() >= 2) {
            if (results.isEmpty()) {
                motionRectF.clear();
                sendingFlags.clear();
                motion = MotionType.NO_DETECT;
            } else {
                motion = detectMotion.calculatePercentageChange(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
                if (motion == MotionType.SENDING) {
                    sendingFlags.add(MotionType.SENDING);
                } else {
                    sendingFlags.clear();
                }
            }
        }


        if (environmentalConditions.checkConditions(
                brightness
                )) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (start && highQualityBitmaps.size() != 0  && sendingFlags.size() > 10) {
                    if (hasFaceOrCard()) {
                        createClipsService.schedule(() -> {
                            stopRecording();
                        }, 250, TimeUnit.MILLISECONDS);
                        start = false;

                    }
                }

            }
        }


        this.faceMatchCallback.onEnvironmentalConditionsChange(
                brightness,
                sendingFlags.size() == 0 ? MotionType.NO_DETECT :  sendingFlags.size() > 5 ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND
                );

    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {
        highQualityBitmaps.clear();
        bitmaps.clear();
        motionRectF.clear();
        sendingFlags.clear();
        if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
            storageUtils.deleteFolderContents(storageUtils.getImageFolder(getActivity().getApplicationContext()));
            storageUtils.deleteFolderContents(storageUtils.getVideosFolder(getActivity().getApplicationContext()));
            this.faceMatchCallback.onComplete(BaseResponseDataModel);
            start = false;
        } else
            start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED);
        switch (eventName) {
            case HubConnectionTargets.ON_ERROR:
                this.faceMatchCallback.onError(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_RETRY:
                this.faceMatchCallback.onRetry(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                this.faceMatchCallback.onClipPreparationComplete(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_STATUS_UPDATE:
                this.faceMatchCallback.onStatusUpdated(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_UPDATE:
                this.faceMatchCallback.onUpdated(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_LIVENESS_UPDATE:
                this.faceMatchCallback.onLivenessUpdate(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_CARD_DETECTED:
                this.faceMatchCallback.onCardDetected(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_MRZ_EXTRACTED:
                this.faceMatchCallback.onMrzExtracted(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_MRZ_DETECTED:
                this.faceMatchCallback.onMrzDetected(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                this.faceMatchCallback.onNoMrzDetected(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_FACE_DETECTED:
                this.faceMatchCallback.onFaceDetected(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_NO_FACE_DETECTED:
                this.faceMatchCallback.onNoFaceDetected(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_FACE_EXTRACTED:
                this.faceMatchCallback.onFaceExtracted(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                this.faceMatchCallback.onQualityCheckAvailable(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                this.faceMatchCallback.onDocumentCaptured(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                this.faceMatchCallback.onDocumentCropped(BaseResponseDataModel);
                break;
            case HubConnectionTargets.ON_UPLOAD_FAILED:
                this.faceMatchCallback.onUploadFailed(BaseResponseDataModel);
                break;
            default:

        }
    }


    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        storageUtils.deleteFolderContents(storageUtils.getImageFolder(getActivity().getApplicationContext()));
        storageUtils.deleteFolderContents(storageUtils.getVideosFolder(getActivity().getApplicationContext()));
    }

    ;


    public boolean hasFaceOrCard() {
        return hasFace();
    }

    public boolean hasFace() {
        boolean hasFace = false;
        for (Classifier.Recognition item : results) {
            if (item.getDetectedClass() == 1 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasFace = true;
            }
        }
        return hasFace;
    }

    @Override
    protected void onStopRecordVideo(@NonNull String videoBase64, @NonNull File video) {
        this.faceMatchCallback.onSend();
        start = false;
        videoCounter = videoCounter + 1;
        createBase64.execute(() -> {
            remoteProcessing.starProcessing(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.FACE_MATCH),
                    ImageUtils.convertBitmapToBase64(highQualityBitmaps.get(highQualityBitmaps.size() - 1), BlockType.FACE_MATCH, getActivity()),
                    configModel,
                    "",
                    this.secondImage,
                    "ConnectionId",
                    getVideoPath(configModel, faceMatch,videoCounter),
                    hasFace(),
                    processMrz,
                    performLivenessDetection,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    true,
                    storeImageStream,
                    "FaceImageAcquisition"
            );
        });

        remoteProcessing.uploadVideo(videoCounter, video, configModel, faceMatch);
    }


}
