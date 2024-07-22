package com.assentify.sdk.ScanOther;

import static  com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.getIgnoredProperties;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.preparePropertiesToTranslate;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import  com.assentify.sdk.CameraPreview;
import  com.assentify.sdk.CheckEnvironment.DetectZoom;
import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;
import  com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import  com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import  com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.Language;
import  com.assentify.sdk.Core.Constants.MotionType;
import  com.assentify.sdk.Core.Constants.RemoteProcessing;
import  com.assentify.sdk.Core.Constants.Routes.EndPointsUrls;
import com.assentify.sdk.Core.Constants.SentryKeys;
import com.assentify.sdk.Core.Constants.SentryManager;
import  com.assentify.sdk.Core.Constants.ZoomType;
import  com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.LanguageTransformation.LanguageTransformation;
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback;
import  com.assentify.sdk.Models.BaseResponseDataModel;
import  com.assentify.sdk.CheckEnvironment.DetectMotion;
import  com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import  com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.ScanPassport.PassportExtractedModel;
import com.assentify.sdk.ScanPassport.PassportResponseModel;
import com.assentify.sdk.ScanPassport.ScanPassport;
import  com.assentify.sdk.tflite.Classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.sentry.SentryLevel;

public class ScanOther  extends CameraPreview implements RemoteProcessingCallback, LanguageTransformationCallback {


    private ScanOtherCallback scanOtherCallback;
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

    Boolean processMrz;
    Boolean performLivenessDetection;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    Boolean storeImageStream;
    ConfigModel configModel;

    String language;

    private  String other = "Other";

    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private  int videoCounter = -1;

    private OtherResponseModel otherResponseModel;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScanOther(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                     Boolean processMrz,
                     Boolean performLivenessDetection,
                     Boolean saveCapturedVideo,
                     Boolean storeCapturedDocument,
                     Boolean storeImageStream,
                     String language
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDetection = performLivenessDetection;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;
        this.language = language;
    }

    public void setScanOtherCallback(ScanOtherCallback scanOtherCallback) {
        SentryManager.INSTANCE.registerEvent(SentryKeys.Other, SentryLevel.INFO);
        this.scanOtherCallback = scanOtherCallback;
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
                sendingFlagsZoom.clear();
                sendingFlagsMotion.clear();
                motion = MotionType.NO_DETECT;
                zoom = ZoomType.NO_DETECT;
            } else {
                motion = detectMotion.calculatePercentageChange(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
                zoom = detectZoom.calculatePercentageChangeWidth(motionRectF.get(motionRectF.size() - 1));
                if (motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
                    sendingFlagsMotion.add(MotionType.SENDING);
                    sendingFlagsZoom.add(ZoomType.SENDING);
                } else {
                    sendingFlagsZoom.clear();
                    sendingFlagsMotion.clear();
                }
            }
        }
        if (environmentalConditions.checkConditions(
                brightness) && motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (start && highQualityBitmaps.size() != 0 && sendingFlagsMotion.size() > 1  && sendingFlagsZoom.size() > 1) {
                    if (hasFaceOrCard()) {
                        stopRecording();
                    }
                }

            }
        }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanOtherCallback.onEnvironmentalConditionsChange(
                            brightness,
                            sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT : sendingFlagsMotion.size() > 5 ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            zoom);
                }
            });
        }


    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {
        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.Other,eventName, Objects.requireNonNull(BaseResponseDataModel.getResponse()));
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    highQualityBitmaps.clear();
                    motionRectF.clear();
                    sendingFlagsZoom.clear();
                    sendingFlagsMotion.clear();



                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        start = false;
                        Map<String, String> transformedProperties = new HashMap<>();
                        Map<String, String> transformedDetails = new HashMap<>();
                        OtherExtractedModel otherExtractedModel = OtherExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(),transformedProperties,transformedDetails);
                         otherResponseModel = new OtherResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                otherExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );
                        if (Objects.equals(language, Language.NON)) {
                            scanOtherCallback.onComplete(otherResponseModel);
                        } else {
                            Map<String, String> propertiesToTranslate = new HashMap<>();
                            otherExtractedModel.getAdditionalDetails().forEach((key, value) -> {
                                propertiesToTranslate.put(key,value.toString());
                            });

                            otherExtractedModel.getOutputProperties().forEach((key, value) -> {
                                propertiesToTranslate.put(key,value.toString());
                            });
                            LanguageTransformation translated = new LanguageTransformation(apiKey);
                            translated.setCallback(ScanOther.this);
                            translated.languageTransformation(
                                    language,
                                    preparePropertiesToTranslate(language,propertiesToTranslate)
                            );
                        }


                    } else {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED);
                        switch (eventName) {
                            case HubConnectionTargets.ON_ERROR:
                                scanOtherCallback.onError(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_RETRY:
                                scanOtherCallback.onRetry(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                                scanOtherCallback.onClipPreparationComplete(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_STATUS_UPDATE:
                                scanOtherCallback.onStatusUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPDATE:
                                scanOtherCallback.onUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_LIVENESS_UPDATE:
                                scanOtherCallback.onLivenessUpdate(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CARD_DETECTED:
                                scanOtherCallback.onCardDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_EXTRACTED:
                                scanOtherCallback.onMrzExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_DETECTED:
                                scanOtherCallback.onMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                                scanOtherCallback.onNoMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_DETECTED:
                                scanOtherCallback.onFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_FACE_DETECTED:
                                scanOtherCallback.onNoFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_EXTRACTED:
                                scanOtherCallback.onFaceExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                                scanOtherCallback.onQualityCheckAvailable(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                                scanOtherCallback.onDocumentCaptured(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                                scanOtherCallback.onDocumentCropped(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPLOAD_FAILED:
                                scanOtherCallback.onUploadFailed(BaseResponseDataModel);
                                break;
                            default:
                                start = true;
                                scanOtherCallback.onRetry(BaseResponseDataModel);
                                break;
                        }
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
        return hasCard();
    }


    public boolean hasCard() {
        boolean hasCard = false;
        for (Classifier.Recognition item : results) {
            if (item.getDetectedClass() == 0 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasCard = true;
            }
        }
        return hasCard;
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
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanOtherCallback.onSend();
                }
            });
        }

        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.Other,"onSend", "");

        start = false;
        videoCounter = videoCounter + 1;
        createBase64.execute(() -> {
            remoteProcessing.starProcessing(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.OTHER),
                    ImageUtils.convertBitmapToBase64(highQualityBitmaps.get(highQualityBitmaps.size() - 1), BlockType.OTHER, getActivity()),
                    "",
                    configModel,
                    "",
                    "",
                    "ConnectionId",
                    getVideoPath(configModel, other,videoCounter),
                    hasFace(),
                    processMrz,
                    performLivenessDetection,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    false,
                    storeImageStream,
                    "IdentificationDocumentCapture"
            );
        });

        remoteProcessing.uploadVideo(videoCounter, video, configModel, other);
    }

    @Override
    public void onTranslatedSuccess(@Nullable Map<String, String> properties) {
        getIgnoredProperties(Objects.requireNonNull(otherResponseModel.getOtherExtractedModel().getOutputProperties())).forEach((key, value) -> {
            properties.put(key,value);
        });
        otherResponseModel.getOtherExtractedModel().getTransformedProperties().clear();
        otherResponseModel.getOtherExtractedModel().getTransformedDetails().clear();
        properties.forEach((key, value) -> {
            if(key.contains("OnBoardMe_IdentificationDocumentCapture")){
                otherResponseModel.getOtherExtractedModel().getTransformedProperties().put(key,value);
            }else {
                otherResponseModel.getOtherExtractedModel().getTransformedDetails().put(key,value);
            }
        });

        scanOtherCallback.onComplete(otherResponseModel);
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        scanOtherCallback.onComplete(otherResponseModel);
    }

}
