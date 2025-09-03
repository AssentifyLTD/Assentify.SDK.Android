package com.assentify.sdk.FaceMatch;

import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.FaceEventsKt.getRandomEvents;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.Core.Constants.ActiveLiveEvents;
import com.assentify.sdk.Core.Constants.ActiveLiveType;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.DoneFlags;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.FaceEventStatus;
import com.assentify.sdk.Core.Constants.FaceEvents;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.AssetsAudioPlayer;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.logging.BugsnagObject;
import com.assentify.sdk.tflite.Classifier;
import com.assentify.sdk.tflite.FaceQualityCheck.FaceEventCallback;
import com.assentify.sdk.tflite.FaceQualityCheck.FaceQualityCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;


public class FaceMatchManual extends CameraPreview implements RemoteProcessingCallback {


    private FaceMatchCallback faceMatchCallback;
    private  EnvironmentalConditions environmentalConditions;
    private String secondImage = "";

    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();

    Boolean processMrz;
    Boolean performLivenessDocument;
    Boolean performLivenessFace;

    Boolean performPassiveLivenessFace;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    Boolean storeImageStream;
    ConfigModel configModel;

    String stepId;

    int livnessRetryCount = 0;
    int retryCount = 0;

    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private String faceMatch = "FaceMatch";

    private  Bitmap normalImage;

    public FaceMatchManual() {
    }
    public FaceMatchManual(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                           Boolean processMrz,
                           Boolean performLivenessDocument,
                           Boolean performLivenessFace,
                           Boolean performPassiveLivenessFace,
                           Boolean saveCapturedVideo,
                           Boolean storeCapturedDocument,
                           Boolean storeImageStream,
                           Boolean showCountDownView
    ) {
        this.apiKey = apiKey;
        this.performLivenessFace = performLivenessFace;
        this.performPassiveLivenessFace = performPassiveLivenessFace;
        this.environmentalConditions = environmentalConditions;
        frontCamera();
        this.processMrz = processMrz;
        this.performLivenessDocument = performLivenessDocument;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;
        setEnvironmentalConditions(this.environmentalConditions);
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
        if (this.stepId == null) {
            long stepsCount = this.configModel.getStepDefinitions().stream()
                    .filter(item -> item.getStepDefinition().equals("FaceImageAcquisition"))
                    .count();

            if (stepsCount == 1) {
                for (StepDefinitions item : this.configModel.getStepDefinitions()) {
                    if (item.getStepDefinition().equals("FaceImageAcquisition")) {
                        this.stepId = String.valueOf(item.getStepId());
                        break;
                    }
                }
            } else {
                if (this.stepId == null) {
                    throw new IllegalArgumentException("Step ID is required because multiple 'FaceImage Acquisition' steps are present.");
                }
            }
        }
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
                            faceMatchCallback.onSend();
                        }
                    });
                }
                start = false;
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
                createBase64.execute(() -> {
                    remoteProcessing.starProcessing(
                            HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.FACE_MATCH),
                            "",
                            ImageUtils.convertBitmapToBase64(normalImage, BlockType.FACE_MATCH, getActivity()),
                            configModel,
                            "",
                            this.secondImage,
                            "ConnectionId",
                            getVideoPath(configModel, faceMatch, 0),
                            hasFace(),
                            processMrz,
                            performLivenessDocument,
                            performPassiveLivenessFace,
                            saveCapturedVideo,
                            storeCapturedDocument,
                            true,
                            storeImageStream,
                            stepId,
                            new ArrayList<>()
                    );
                });
            }else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            faceMatchCallback.onRetry(
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
                    BugsnagObject.INSTANCE.logInfo("Face match done with event  : " + eventName + BaseResponseDataModel.getResponse(),configModel);
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        FaceExtractedModel faceExtractedModel = FaceExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                        FaceResponseModel faceResponseModel = new FaceResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                faceExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );


                        faceMatchCallback.onComplete(faceResponseModel,DoneFlags.Success);
                        start = false;
                    } else if(eventName.equals(HubConnectionTargets.ON_RETRY)){
                        retryCount++;
                        if (retryCount ==  environmentalConditions.getRetryCount()){
                            FaceExtractedModel faceExtractedModel = FaceExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                            FaceResponseModel faceResponseModel = new FaceResponseModel(
                                    BaseResponseDataModel.getDestinationEndpoint(),
                                    faceExtractedModel,
                                    BaseResponseDataModel.getError(),
                                    BaseResponseDataModel.getSuccess()
                            );
                            faceMatchCallback.onComplete(faceResponseModel, DoneFlags.MatchFailed);
                            start = false;
                        }else {
                            start = true;
                            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
                            faceMatchCallback.onRetry(BaseResponseDataModel);
                        }
                    } else if(eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)){
                        livnessRetryCount++;
                        if (livnessRetryCount == environmentalConditions.getFaceLivenessRetryCount()){
                            FaceExtractedModel faceExtractedModel = FaceExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                            FaceResponseModel faceResponseModel = new FaceResponseModel(
                                    BaseResponseDataModel.getDestinationEndpoint(),
                                    faceExtractedModel,
                                    BaseResponseDataModel.getError(),
                                    BaseResponseDataModel.getSuccess()
                            );
                            faceMatchCallback.onComplete(faceResponseModel, DoneFlags.LivenessFailed);
                            start = false;
                        }else {
                            start = true;
                            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
                            faceMatchCallback.onLivenessUpdate(BaseResponseDataModel);
                        }
                    }else {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) ;
                        if(start){
                            manualCaptureUi((environmentalConditions.getHoldHandColor()), environmentalConditions.getEnableGuide());
                        }
                        switch (eventName) {
                            case HubConnectionTargets.ON_ERROR:
                                faceMatchCallback.onError(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                                faceMatchCallback.onClipPreparationComplete(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_STATUS_UPDATE:
                                faceMatchCallback.onStatusUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPDATE:
                                faceMatchCallback.onUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CARD_DETECTED:
                                faceMatchCallback.onCardDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_EXTRACTED:
                                faceMatchCallback.onMrzExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_DETECTED:
                                faceMatchCallback.onMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                                faceMatchCallback.onNoMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_DETECTED:
                                faceMatchCallback.onFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_FACE_DETECTED:
                                faceMatchCallback.onNoFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_EXTRACTED:
                                faceMatchCallback.onFaceExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                                faceMatchCallback.onQualityCheckAvailable(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                                faceMatchCallback.onDocumentCaptured(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                                faceMatchCallback.onDocumentCropped(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPLOAD_FAILED:
                                faceMatchCallback.onUploadFailed(BaseResponseDataModel);
                                break;
                            default:

                        }
                    }

                }
            });
        }

    }


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

    // TODO Later
    @Override
    protected void onStopRecordVideo() {
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
        if (this.createBase64 != null) {
            this.createBase64.shutdown();
        }
    }
}
