package com.assentify.sdk.ScanPassport;

import static  com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.NonNull;

import  com.assentify.sdk.CameraPreview;
import  com.assentify.sdk.CheckEnvironment.DetectZoom;
import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;
import  com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import  com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import  com.assentify.sdk.Core.Constants.HubConnectionTargets;
import  com.assentify.sdk.Core.Constants.MotionType;
import  com.assentify.sdk.Core.Constants.RemoteProcessing;
import  com.assentify.sdk.Core.Constants.Routes.EndPointsUrls;
import com.assentify.sdk.Core.Constants.SentryKeys;
import com.assentify.sdk.Core.Constants.SentryManager;
import  com.assentify.sdk.Core.Constants.ZoomType;
import  com.assentify.sdk.Core.FileUtils.ImageUtils;
import  com.assentify.sdk.Core.FileUtils.StorageUtils;
import  com.assentify.sdk.Models.BaseResponseDataModel;
import  com.assentify.sdk.CheckEnvironment.DetectMotion;
import  com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import  com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.ScanPassport.ScanPassportCallback;
import  com.assentify.sdk.tflite.Classifier;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.sentry.Sentry;
import io.sentry.SentryLevel;


public class ScanPassport extends CameraPreview implements RemoteProcessingCallback {


    ///

    private ScanPassportCallback scanPassportCallback;
    private final EnvironmentalConditions environmentalConditions;

    private RectF rectFCard = new RectF();
    private double brightness;
    private MotionType motion = MotionType.NO_DETECT;
    private ZoomType zoom = ZoomType.NO_DETECT;
    private List<Bitmap> highQualityBitmaps = new ArrayList<>();
    private Bitmap croppedBitmap;
    private List<RectF> motionRectF = new ArrayList<>();
    private List<MotionType> sendingFlagsMotion = new ArrayList<>();
    private List<ZoomType> sendingFlagsZoom = new ArrayList<>();
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

    private String readPassport = "ReadPassport";
    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private int videoCounter = -1;

    public ScanPassport(
            ConfigModel configModel,
            EnvironmentalConditions environmentalConditions, String apiKey,
            Boolean processMrz,
            Boolean performLivenessDetection,
            Boolean saveCapturedVideo,
            Boolean storeCapturedDocument,
            Boolean storeImageStream
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDetection = performLivenessDetection;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;

    }

    public void setScanPassportCallback(ScanPassportCallback scanPassportCallback) {
        SentryManager.INSTANCE.registerEvent(SentryKeys.Passport, SentryLevel.INFO);
        this.scanPassportCallback = scanPassportCallback;
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
            highQualityBitmaps.add(normalImage);
        }
        this.croppedBitmap = croppedBitmap;
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (result.getDetectedClass() == 0) {
                rectFCard = new RectF(location.left, location.top, location.right, location.bottom);
                motionRectF.add(rectFCard);
            }
        }
        if (motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
            setRectFCustomColor(environmentalConditions.getCustomColor(),environmentalConditions.getEnableDetect(),environmentalConditions.getEnableGuide());
        } else {
            setRectFCustomColor(environmentalConditions.getHoldHandColor(),environmentalConditions.getEnableDetect(),environmentalConditions.getEnableGuide());
        }

        checkEnvironment();


    }


    protected void checkEnvironment() {
        if (getActivity() != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (hasFaceOrCard() && start) {
                startRecording();
            }
        }
        ImageBrightnessChecker imageBrightnessChecker = new ImageBrightnessChecker();
        DetectMotion detectMotion = new DetectMotion();
        DetectZoom detectZoom = new DetectZoom();
        brightness = imageBrightnessChecker.getAverageBrightness(croppedBitmap);
        if (motionRectF.size() >= 2) {
            if (results.isEmpty()) {
                motionRectF.clear();
                sendingFlagsMotion.clear();
                sendingFlagsZoom.clear();
                motion = MotionType.NO_DETECT;
                zoom = ZoomType.NO_DETECT;
            } else {
                motion = detectMotion.calculatePercentageChangePassport(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
                zoom = detectZoom.calculatePercentageChangeWidth(motionRectF.get(motionRectF.size() - 1));
                if (motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
                    sendingFlagsMotion.add(MotionType.SENDING);
                    sendingFlagsZoom.add(ZoomType.SENDING);
                } else {
                    sendingFlagsMotion.clear();
                    sendingFlagsZoom.clear();
                }
            }
        }
        if (environmentalConditions.checkConditions(
                brightness) && motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (start && highQualityBitmaps.size() != 0 && sendingFlagsMotion.size() > 1 && sendingFlagsZoom.size() > 1) {
                    if (hasFaceOrCard()) {
                        stopRecording();
                    }
                }
            }
        }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanPassportCallback.onEnvironmentalConditionsChange(
                            brightness,
                            sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT : sendingFlagsMotion.size() > 5 ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            zoom);
                }
            });
        }


    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {

        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.Passport,eventName, Objects.requireNonNull(BaseResponseDataModel.getResponse()));
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    highQualityBitmaps.clear();
                    motionRectF.clear();
                    sendingFlagsMotion.clear();
                    sendingFlagsZoom.clear();
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        storageUtils.deleteFolderContents(storageUtils.getImageFolder(getActivity().getApplicationContext()));
                        storageUtils.deleteFolderContents(storageUtils.getVideosFolder(getActivity().getApplicationContext()));

                        PassportExtractedModel passportExtractedModel = PassportExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                        PassportResponseModel passportResponseModel = new PassportResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                passportExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );

                        scanPassportCallback.onComplete(passportResponseModel);
                        start = false;
                    } else
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED);
                    switch (eventName) {
                        case HubConnectionTargets.ON_ERROR:
                            scanPassportCallback.onError(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_RETRY:
                            scanPassportCallback.onRetry(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                            scanPassportCallback.onClipPreparationComplete(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_STATUS_UPDATE:
                            scanPassportCallback.onStatusUpdated(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_UPDATE:
                            scanPassportCallback.onUpdated(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_LIVENESS_UPDATE:
                            scanPassportCallback.onLivenessUpdate(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_CARD_DETECTED:
                            scanPassportCallback.onCardDetected(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_MRZ_EXTRACTED:
                            scanPassportCallback.onMrzExtracted(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_MRZ_DETECTED:
                            scanPassportCallback.onMrzDetected(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                            scanPassportCallback.onNoMrzDetected(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_FACE_DETECTED:
                            scanPassportCallback.onFaceDetected(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_NO_FACE_DETECTED:
                            scanPassportCallback.onNoFaceDetected(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_FACE_EXTRACTED:
                            scanPassportCallback.onFaceExtracted(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                            scanPassportCallback.onQualityCheckAvailable(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                            scanPassportCallback.onDocumentCaptured(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                            scanPassportCallback.onDocumentCropped(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_UPLOAD_FAILED:
                            scanPassportCallback.onUploadFailed(BaseResponseDataModel);
                            break;
                        default:

                    }
                }
            });
        }
    }


    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        this.createBase64.shutdown();
    }


    public boolean hasFaceOrCard() {
        return hasFaceAndCard();
    }

    public boolean hasFaceAndCard() {
        boolean hasFace = true;
        boolean hasCard = false;
        for (Classifier.Recognition item : results) {
            /* if (item.getDetectedClass() == 1 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasFace = true;
            }*/
            if (item.getDetectedClass() == 0 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasCard = true;
            }
        }
        return hasFace && hasCard;
    }


    @Override
    protected void onStopRecordVideo(@NonNull String videoBase64, @NonNull File video) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanPassportCallback.onSend();
                }
            });
        }
        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.Passport,"onSend", "");
        start = false;
        videoCounter = videoCounter + 1;
        createBase64.execute(() -> {
            remoteProcessing.starProcessing(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.READ_PASSPORT),
                    ImageUtils.convertBitmapToBase64(highQualityBitmaps.get(highQualityBitmaps.size() - 1), BlockType.READ_PASSPORT, getActivity()),
                    configModel,
                    "",
                    "",
                    "ConnectionId",
                    getVideoPath(configModel, readPassport, videoCounter),
                    true,
                    processMrz,
                    performLivenessDetection,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    false,
                    storeImageStream,
                    "IdentificationDocumentCapture"
            );
        });

        remoteProcessing.uploadVideo(videoCounter, video, configModel, readPassport);
    }


}
