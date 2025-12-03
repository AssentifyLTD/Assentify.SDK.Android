package com.assentify.sdk.ScanIDCard;


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
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails;
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


public class ScanIDCardManual extends CameraPreview implements RemoteProcessingCallback, LanguageTransformationCallback {


    private IDCardCallback idCardCallback;
    private EnvironmentalConditions environmentalConditions;
    private String templateId;


    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();

    Boolean processMrz;
    Boolean performLivenessDocument;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    ConfigModel configModel;

    private String language;

    String stepId;
    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();


    private int order = 0;

    private List<KycDocumentDetails> kycDocumentDetails = new ArrayList<>();


    private IDResponseModel idResponseModel;

    private Bitmap normalImage;

    public ScanIDCardManual() {
    }

    int retryCount = 0;

    private String templateName;

    public ScanIDCardManual(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                            IDCardCallback idCardCallback,
                            List<KycDocumentDetails> kycDocumentDetails,
                            String language
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.configModel = configModel;
        this.idCardCallback = idCardCallback;
        this.kycDocumentDetails = kycDocumentDetails;
        this.language = language;
        setEnvironmentalConditions(this.environmentalConditions);
        if (!this.kycDocumentDetails.isEmpty()) {
            KycDocumentDetails firstKycDocument = kycDocumentDetails.get(0);
            this.changeTemplateId(firstKycDocument.getTemplateProcessingKeyInformation());
        }

    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
        if (this.stepId == null) {
            long stepsCount = this.configModel.getStepDefinitions().stream()
                    .filter(item -> item.getStepDefinition().equals("IdentificationDocumentCapture"))
                    .count();

            if (stepsCount == 1) {
                for (StepDefinitions item : this.configModel.getStepDefinitions()) {
                    if (item.getStepDefinition().equals("IdentificationDocumentCapture")) {
                        this.stepId = String.valueOf(item.getStepId());
                        break;
                    }
                }
            } else {
                if (this.stepId == null) {
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


    public void changeTemplateId(String templateId) {
        this.retryCount = 0;
        this.templateId = templateId;
        kycDocumentDetails.forEach((item) -> {
            if (Objects.equals(this.templateId, item.getTemplateProcessingKeyInformation())) {
                this.templateName = item.getName();
            }
        });
        createBase64 = Executors.newSingleThreadExecutor();
        start = true;
        remoteProcessing = new RemoteProcessing();
        remoteProcessing.setCallback(this);

    }

    @Override
    protected void processManualImage(@NonNull Bitmap normalImage) {
        if (getActivity() != null) {
            BugsnagObject.INSTANCE.initialize(getActivity().getApplicationContext(), configModel);
        }
        this.normalImage = normalImage;
        if (this.start) {
            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
        }
    }

    public void takePicture() {
        if (this.start) {
            this.results = detectCardAndFace(normalImage);
            if (hasFaceOrCard()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            idCardCallback.onSend();
                        }
                    });
                }

                start = false;
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
                createBase64.execute(() -> {
                    remoteProcessing.starProcessingIDs(
                            HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.ID_CARD),
                            ImageUtils.convertBitmapToByteArray(normalImage, BlockType.READ_PASSPORT, getActivity()),
                            configModel,
                            this.templateId,
                            "ConnectionId",
                            getVideoPath(configModel, this.templateId, 0),
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
                            getIDTag(configModel, this.templateName),
                            false
                    );

                });
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            idCardCallback.onRetry(
                                    new BaseResponseDataModel(
                                            "onRetry",
                                            "",
                                            "",
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
                        Map<String, String> transformedProperties = new HashMap<>();
                        start = false;
                        IDExtractedModel idExtractedModel = IDExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties);
                        idResponseModel = new IDResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                idExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );

                        if (Objects.equals(language, Language.NON)) {
                            idCardCallback.onComplete(idResponseModel, order, DoneFlags.Success);
                            order = order + 1;
                        } else {
                            LanguageTransformation translated = new LanguageTransformation(apiKey);
                            translated.setCallback(ScanIDCardManual.this);
                            translated.languageTransformation(
                                    language,
                                    preparePropertiesToTranslate(language, idExtractedModel.getOutputProperties())
                            );
                        }


                    } else if (eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE) || eventName.equals(HubConnectionTargets.ON_WRONG_TEMPLATE)) {
                        retryCount++;
                        if (retryCount == environmentalConditions.getRetryCount()) {
                            Map<String, String> transformedProperties = new HashMap<>();
                            IDExtractedModel idExtractedModel = IDExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties);
                            idResponseModel = new IDResponseModel(
                                    BaseResponseDataModel.getDestinationEndpoint(),
                                    idExtractedModel,
                                    BaseResponseDataModel.getError(),
                                    BaseResponseDataModel.getSuccess()
                            );

                            if (eventName.equals(HubConnectionTargets.ON_RETRY)) {
                                idCardCallback.onComplete(idResponseModel, order, DoneFlags.ExtractFailed);
                            } else if (eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)) {
                                idCardCallback.onComplete(idResponseModel, order, DoneFlags.LivenessFailed);
                            } else {
                                idCardCallback.onComplete(idResponseModel, order, DoneFlags.WrongTemplate);
                            }
                            order = order + 1;
                            start = false;
                        } else {
                            start = true;
                            if (eventName.equals(HubConnectionTargets.ON_RETRY)) {
                                idCardCallback.onRetry(BaseResponseDataModel);

                            } else if (eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)) {
                                idCardCallback.onLivenessUpdate(BaseResponseDataModel);

                            } else {
                                idCardCallback.onWrongTemplate(BaseResponseDataModel);

                            }
                        }
                    } else {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE);
                        if (start) {
                            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
                        }
                        switch (eventName) {
                            case HubConnectionTargets.ON_ERROR:
                                idCardCallback.onError(BaseResponseDataModel);
                                break;

                            case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                                idCardCallback.onClipPreparationComplete(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_STATUS_UPDATE:
                                idCardCallback.onStatusUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPDATE:
                                idCardCallback.onUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CARD_DETECTED:
                                idCardCallback.onCardDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_EXTRACTED:
                                idCardCallback.onMrzExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_DETECTED:
                                idCardCallback.onMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                                idCardCallback.onNoMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_DETECTED:
                                idCardCallback.onFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_FACE_DETECTED:
                                idCardCallback.onNoFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_EXTRACTED:
                                idCardCallback.onFaceExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                                idCardCallback.onQualityCheckAvailable(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                                idCardCallback.onDocumentCaptured(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                                idCardCallback.onDocumentCropped(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPLOAD_FAILED:
                                idCardCallback.onUploadFailed(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_WRONG_TEMPLATE:
                                idCardCallback.onWrongTemplate(BaseResponseDataModel);
                                break;
                            default:
                                start = true;
                                idCardCallback.onRetry(BaseResponseDataModel);
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


    String nameKey = "";
    int nameWordCount = 0;
    String surnameKey = "";

    @Override
    public void onTranslatedSuccess(@Nullable Map<String, String> properties) {
        getIgnoredProperties(Objects.requireNonNull(idResponseModel.getIDExtractedModel().getOutputProperties())).forEach((key, value) -> {
            properties.put(key, value);
        });

        Objects.requireNonNull(idResponseModel.getIDExtractedModel().getOutputProperties()).forEach(
                (key, value) -> {
                    if (key.contains(IdentificationDocumentCaptureKeys.name)) {
                        nameKey = key;
                        nameWordCount = value.toString().trim().isEmpty() ? 0 : value.toString().trim().split("\\s+").length;
                    }
                    if (key.contains(IdentificationDocumentCaptureKeys.surname)) {
                        surnameKey = key;
                    }
                }
        );

        idResponseModel.getIDExtractedModel().getTransformedProperties().clear();
        idResponseModel.getIDExtractedModel().getExtractedData().clear();
        properties.forEach((key, value) -> {

            if (key.equals(FullNameKey)) {
                if (!nameKey.isEmpty()) {
                    idResponseModel.getIDExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(), nameWordCount));
                    idResponseModel.getIDExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(), nameWordCount));
                }
                if (!surnameKey.isEmpty()) {
                    idResponseModel.getIDExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(), nameWordCount));
                    idResponseModel.getIDExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(), nameWordCount));
                }
            } else {
                idResponseModel.getIDExtractedModel().getTransformedProperties().put(key, value);
                String newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length())
                        .replace("_", " ");
                idResponseModel.getIDExtractedModel().getExtractedData().put(newKey, value);
            }

        });


        idCardCallback.onComplete(idResponseModel, order, DoneFlags.Success);
        order = order + 1;
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        idCardCallback.onComplete(idResponseModel, order, DoneFlags.Success);
        order = order + 1;
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
    protected void onStopRecordVideo() {
    }

    @Override
    public void onUploadProgress(int progress) {
        idCardCallback.onUploadingProgress(progress);

    }
}
