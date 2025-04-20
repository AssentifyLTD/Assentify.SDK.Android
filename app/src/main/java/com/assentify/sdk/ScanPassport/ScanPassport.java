package com.assentify.sdk.ScanPassport;

import static com.assentify.sdk.CheckEnvironment.DetectMotionKt.MotionPassportLimit;
import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomPassportLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.getIgnoredProperties;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.preparePropertiesToTranslate;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.FullNameKey;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getRemainingWords;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getSelectedWords;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKeys;
import com.assentify.sdk.Core.Constants.Language;
import com.assentify.sdk.Core.Constants.LivenessType;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.Routes.EndPointsUrls;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.AssetsAudioPlayer;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.LanguageTransformation.LanguageTransformation;
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.ScanPassport.ScanPassportCallback;
import com.assentify.sdk.tflite.Classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;


public class ScanPassport extends CameraPreview implements RemoteProcessingCallback, LanguageTransformationCallback {


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

    Boolean processMrz;
    Boolean performLivenessDocument;
    Boolean performLivenessFace;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    Boolean storeImageStream;
    ConfigModel configModel;
    String language;

    private String readPassport = "ReadPassport";
    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private int videoCounter = -1;

    private PassportResponseModel passportResponseModel;

    private DetectIfRectFInsideTheScreen detectIfInsideTheScreen = new DetectIfRectFInsideTheScreen();
    private boolean isRectFInsideTheScreen = false;

    private AssetsAudioPlayer audioPlayer;

    public ScanPassport(
            ConfigModel configModel,
            EnvironmentalConditions environmentalConditions, String apiKey,
            Boolean processMrz,
            Boolean performLivenessDocument,
            Boolean performLivenessFace,
            Boolean saveCapturedVideo,
            Boolean storeCapturedDocument,
            Boolean storeImageStream,
            String language
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDocument = performLivenessDocument;
        this.performLivenessFace = performLivenessFace;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;
        this.language = language;

    }

    public void setScanPassportCallback(ScanPassportCallback scanPassportCallback) {
        this.scanPassportCallback = scanPassportCallback;
        try {
            remoteProcessing = new RemoteProcessing();
            remoteProcessing.setCallback(this);
        } catch (Exception e) {
        }

    }


    @Override
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {

        if (audioPlayer == null) {
            if (getActivity() != null) {
                audioPlayer = new AssetsAudioPlayer(getActivity());
            }
        }

        this.results = results;
        if (hasFaceOrCard()) {
            highQualityBitmaps.add(normalImage);
            listScaleRectF.forEach((item) -> {
                if (item.component2().contains(ConstantsValues.CardName)) {
                    isRectFInsideTheScreen = detectIfInsideTheScreen.isRectFWithinMargins(item.component1(), previewWidth, previewHeight);
                }
            });
        }

        this.croppedBitmap = croppedBitmap;
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (result.getDetectedClass() == 0) {
                rectFCard = new RectF(location.left, location.top, location.right, location.bottom);
                motionRectF.add(rectFCard);
            }
        }
        if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && environmentalConditions.checkConditions(brightness) == BrightnessEvents.Good) {
            if (isRectFInsideTheScreen) {
                setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            }else {
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            }
        } else {
            setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
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
                    brightness)== BrightnessEvents.Good && motion == MotionType.SENDING && zoom == ZoomType.SENDING && isRectFInsideTheScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (start && highQualityBitmaps.size() != 0 && sendingFlagsMotion.size() > MotionPassportLimit && sendingFlagsZoom.size() > ZoomPassportLimit) {
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
                            environmentalConditions.checkConditions(
                                    brightness),
                            sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT : sendingFlagsMotion.size() > MotionPassportLimit ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            zoom);
                }
            });
        }


    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {

        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    highQualityBitmaps.clear();
                    motionRectF.clear();
                    sendingFlagsMotion.clear();
                    sendingFlagsZoom.clear();
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        start = false;
                        Map<String, String> transformedProperties = new HashMap<>();
                        PassportExtractedModel passportExtractedModel = PassportExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties);
                        passportResponseModel = new PassportResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                passportExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );
                        if (Objects.equals(language, Language.NON)) {
                            scanPassportCallback.onComplete(passportResponseModel);
                        } else {
                            LanguageTransformation translated = new LanguageTransformation(apiKey);
                            translated.setCallback(ScanPassport.this);
                            translated.languageTransformation(
                                    language,
                                    preparePropertiesToTranslate(language, passportExtractedModel.getOutputProperties())
                            );
                        }


                    } else {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE);
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
                                start = true;
                                scanPassportCallback.onRetry(BaseResponseDataModel);
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
        if (audioPlayer != null) {
            audioPlayer.stopAudio();
        }
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

    // TODO Later
    @Override
    protected void onStopRecordVideo() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanPassportCallback.onSend();
                }
            });
        }
        start = false;
        videoCounter = videoCounter + 1;
        createBase64.execute(() -> {
            audioPlayer.playAudio(ConstantsValues.AudioCardSuccess);
            remoteProcessing.starProcessing(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.READ_PASSPORT),
                    ImageUtils.convertBitmapToBase64(highQualityBitmaps.get(highQualityBitmaps.size() - 1), BlockType.READ_PASSPORT, getActivity()),
                    "",
                    configModel,
                    "",
                    "",
                    "ConnectionId",
                    getVideoPath(configModel, readPassport, videoCounter),
                    true,
                    processMrz,
                    performLivenessDocument,
                    performLivenessFace,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    false,
                    storeImageStream,
                    "IdentificationDocumentCapture",
                    new ArrayList<>()
            );
        });

        // remoteProcessing.uploadVideo(videoCounter, video, configModel, readPassport);
    }
    String nameKey = "";
    int nameWordCount = 0;
    String surnameKey = "";

    @Override
    public void onTranslatedSuccess(@Nullable Map<String, String> properties) {

        getIgnoredProperties(Objects.requireNonNull(passportResponseModel.getPassportExtractedModel().getOutputProperties())).forEach((key, value) -> {
            properties.put(key, value);
        });

        Objects.requireNonNull(passportResponseModel.getPassportExtractedModel().getOutputProperties()).forEach(
                (key, value) -> {
                    if(key.contains(IdentificationDocumentCaptureKeys.name)){
                        nameKey = key;
                        nameWordCount = value.toString().trim().isEmpty() ? 0 : value.toString().trim().split("\\s+").length;
                    }
                    if(key.contains(IdentificationDocumentCaptureKeys.surname)){
                        surnameKey = key;
                    }
                }
        );
        passportResponseModel.getPassportExtractedModel().getTransformedProperties().clear();
        passportResponseModel.getPassportExtractedModel().getExtractedData().clear();

        properties.forEach((key, value) -> {
            if (key.equals(FullNameKey)) {
                if(!nameKey.isEmpty()){
                    passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(),nameWordCount));
                    passportResponseModel.getPassportExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(),nameWordCount));
                }
                if(!surnameKey.isEmpty()){
                    passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(),nameWordCount));
                    passportResponseModel.getPassportExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(),nameWordCount));
                }
            } else {
                passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(key, value);
                String newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length())
                        .replace("_", " ");
                passportResponseModel.getPassportExtractedModel().getExtractedData().put(newKey, value);
            }
        });
        scanPassportCallback.onComplete(passportResponseModel);
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        scanPassportCallback.onComplete(passportResponseModel);
    }

    public void stopScanning() {
        closeCamera();
    }
}
