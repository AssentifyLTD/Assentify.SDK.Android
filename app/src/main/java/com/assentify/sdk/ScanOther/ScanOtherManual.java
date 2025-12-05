package com.assentify.sdk.ScanOther;

import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getIDTag;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.getIgnoredProperties;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.preparePropertiesToTranslate;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.FullNameKey;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getRemainingWords;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getSelectedWords;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.DoneFlags;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.EventsErrorMessages;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKeys;
import com.assentify.sdk.Core.Constants.Language;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.AssetsAudioPlayer;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.LanguageTransformation.LanguageTransformation;
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.ScanPassport.PassportExtractedModel;
import com.assentify.sdk.ScanPassport.PassportResponseModel;
import com.assentify.sdk.logging.BugsnagObject;
import com.assentify.sdk.tflite.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;

public class ScanOtherManual extends CameraPreview implements RemoteProcessingCallback, LanguageTransformationCallback {


    private ScanOtherCallback scanOtherCallback;
    private  EnvironmentalConditions environmentalConditions;


    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();

    Boolean processMrz;
    Boolean performLivenessDocument;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    ConfigModel configModel;

    String language;

    String stepId;

    private String other = "Other";

    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();


    private OtherResponseModel otherResponseModel;

    private Bitmap normalImage;

    int retryCount = 0;
    public ScanOtherManual() {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScanOtherManual(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                           String language
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;

        this.configModel = configModel;
        this.language = language;
        setEnvironmentalConditions(this.environmentalConditions);
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
        if(this.stepId==null){
            long stepsCount = this.configModel.getStepDefinitions().stream()
                    .filter(item -> item.getStepDefinition().equals("IdentificationDocumentCapture"))
                    .count();

            if(stepsCount==1){
                for (StepDefinitions item : this.configModel.getStepDefinitions()) {
                    if (item.getStepDefinition().equals("IdentificationDocumentCapture")) {
                        this.stepId = String.valueOf(item.getStepId());
                        break;
                    }
                }
            }else {
                if(this.stepId==null){
                    throw new IllegalArgumentException("Step ID is required because multiple 'Identification Document Capture' steps are present.");
                }
            }
        }
        for (StepDefinitions item : configModel.getStepDefinitions()) {
            if (Integer.parseInt(this.stepId) == item.getStepId()) {
                if (performLivenessDocument == null) {
                    performLivenessDocument = item.getCustomization().getDocumentLiveness();
                }
                if (processMrz == null) {
                    processMrz = item.getCustomization().getProcessMrz();
                }
                if (storeCapturedDocument == null) {
                    storeCapturedDocument = item.getCustomization().getStoreCapturedDocument();
                }
                if (saveCapturedVideo == null) {
                    saveCapturedVideo = item.getCustomization().getSaveCapturedVideo();
                }


            }
        }
    }

    public void setScanOtherCallback(ScanOtherCallback scanOtherCallback) {
        this.scanOtherCallback = scanOtherCallback;
        try {
            remoteProcessing = new RemoteProcessing();
            remoteProcessing.setCallback(this);
        } catch (Exception e) {
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void processManualImage(@NonNull Bitmap normalImage) {
        if (getActivity() != null) {
            BugsnagObject.INSTANCE.initialize(getActivity().getApplicationContext(), configModel);
        }
        this.normalImage = normalImage;
    }


    public void takePicture(){
        if(this.start){
            this.results = detectCardAndFace(normalImage);
            if(hasFaceOrCard()){
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanOtherCallback.onSend();
                        }
                    });
                }

                start = false;
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
                createBase64.execute(() -> {
                    remoteProcessing.starProcessingIDs(
                            HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.OTHER),
                            ImageUtils.convertBitmapToByteArray(normalImage, BlockType.READ_PASSPORT, getActivity()),
                            configModel,
                            "",
                            "ConnectionId",
                            getVideoPath(configModel, other, 0),
                            true,
                            processMrz,
                            performLivenessDocument,
                            saveCapturedVideo,
                            storeCapturedDocument,
                            true,
                            stepId,
                            true,
                            false,
                            retryCount,
                            getIDTag(configModel,other),
                            false
                    );
                });
            }else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanOtherCallback.onRetry(
                                    new BaseResponseDataModel(
                                            "onRetry",
                                            "",
                                            EventsErrorMessages.OnRetryCardMessage,
                                            false
                                    ));
                        }
                    });
                }
            }
        }

    }


    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        start = false;
                        Map<String, String> transformedProperties = new HashMap<>();
                        Map<String, String> transformedDetails = new HashMap<>();
                        OtherExtractedModel otherExtractedModel = OtherExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties, transformedDetails);
                        otherResponseModel = new OtherResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                otherExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );
                        if (Objects.equals(language, Language.NON)) {
                            scanOtherCallback.onComplete(otherResponseModel,DoneFlags.Success);
                        } else {
                            Map<String, String> propertiesToTranslate = new HashMap<>();
                            otherExtractedModel.getAdditionalDetails().forEach((key, value) -> {
                                propertiesToTranslate.put(key, value.toString());
                            });

                            otherExtractedModel.getOutputProperties().forEach((key, value) -> {
                                propertiesToTranslate.put(key, value.toString());
                            });
                            LanguageTransformation translated = new LanguageTransformation(apiKey);
                            translated.setCallback(ScanOtherManual.this);
                            translated.languageTransformation(
                                    language,
                                    preparePropertiesToTranslate(language, propertiesToTranslate)
                            );
                        }


                    } else if(eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)  || eventName.equals(HubConnectionTargets.ON_WRONG_TEMPLATE) ){
                        retryCount++;
                        if (retryCount == environmentalConditions.getRetryCount()){
                            Map<String, String> transformedProperties = new HashMap<>();
                            Map<String, String> transformedDetails = new HashMap<>();
                            OtherExtractedModel otherExtractedModel = OtherExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties, transformedDetails);
                            otherResponseModel = new OtherResponseModel(
                                    BaseResponseDataModel.getDestinationEndpoint(),
                                    otherExtractedModel,
                                    BaseResponseDataModel.getError(),
                                    BaseResponseDataModel.getSuccess()
                            );
                            if(eventName.equals(HubConnectionTargets.ON_RETRY)){
                                scanOtherCallback.onComplete(otherResponseModel, DoneFlags.ExtractFailed);
                            }else if(eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)) {
                                scanOtherCallback.onComplete(otherResponseModel, DoneFlags.LivenessFailed);
                            }else {
                                scanOtherCallback.onComplete(otherResponseModel, DoneFlags.WrongTemplate);
                            }
                            start = false;
                        }else {
                            start = true;
                            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
                            if(eventName.equals(HubConnectionTargets.ON_RETRY)){
                                BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                                scanOtherCallback.onRetry(BaseResponseDataModel);

                            }else if(eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)) {
                                BaseResponseDataModel.setError(EventsErrorMessages.OnLivenessCardUpdateMessage);
                                scanOtherCallback.onLivenessUpdate(BaseResponseDataModel);

                            }else {
                                BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                                scanOtherCallback.onRetry(BaseResponseDataModel);

                            }
                        }
                    } else {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR)  || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) ;
                        if(start){
                            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
                        }
                        switch (eventName) {
                            case HubConnectionTargets.ON_ERROR:
                                scanOtherCallback.onError(BaseResponseDataModel);
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
                                BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                                scanOtherCallback.onRetry(BaseResponseDataModel);
                                break;
                        }
                    }
                }
            });

        }
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

    // TODO Later
    @Override
    protected void onStopRecordVideo() {

        // remoteProcessing.uploadVideo(videoCounter, video, configModel, other);
    }

    String nameKey = "";
    int nameWordCount = 0;
    String surnameKey = "";
    @Override
    public void onTranslatedSuccess(@Nullable Map<String, String> properties) {
        getIgnoredProperties(Objects.requireNonNull(otherResponseModel.getOtherExtractedModel().getOutputProperties())).forEach((key, value) -> {
            properties.put(key, value);
        });

        Objects.requireNonNull(otherResponseModel.getOtherExtractedModel().getOutputProperties()).forEach(
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

        otherResponseModel.getOtherExtractedModel().getTransformedProperties().clear();
        otherResponseModel.getOtherExtractedModel().getTransformedDetails().clear();
        otherResponseModel.getOtherExtractedModel().getExtractedData().clear();
        properties.forEach((key, value) -> {
            if (key.contains("OnBoardMe_IdentificationDocumentCapture") || key.equals(FullNameKey)) {
                if (key.equals(FullNameKey)) {
                    if(!nameKey.isEmpty()){
                        otherResponseModel.getOtherExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(),nameWordCount));
                        otherResponseModel.getOtherExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(),nameWordCount));
                    }
                    if(!surnameKey.isEmpty()){
                        otherResponseModel.getOtherExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(),nameWordCount));
                        otherResponseModel.getOtherExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(),nameWordCount));
                    }

                } else {
                otherResponseModel.getOtherExtractedModel().getTransformedProperties().put(key, value);
                String newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length())
                        .replace("_", " ");
                otherResponseModel.getOtherExtractedModel().getExtractedData().put(newKey, value);}
            } else {
                otherResponseModel.getOtherExtractedModel().getTransformedDetails().put(key, value);
            }
        });
        scanOtherCallback.onComplete(otherResponseModel,DoneFlags.Success);
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        scanOtherCallback.onComplete(otherResponseModel,DoneFlags.Success);
    }

    public void stopScanning() {
        closeCamera();
    }

    @Override
    public void onPause() {
        BugsnagObject.INSTANCE.pauseSession();
        super.onPause();
    }

    @Override
    public void onResume() {
        BugsnagObject.INSTANCE.resumeSession();
        super.onResume();
    }

    @Override
    public synchronized void onDestroy() {
        BugsnagObject.INSTANCE.pauseSession();
        super.onDestroy();
        this.createBase64.shutdown();
    }

    @Override
    public void onUploadProgress(int progress) {
        scanOtherCallback.onUploadingProgress(progress);

    }
}
