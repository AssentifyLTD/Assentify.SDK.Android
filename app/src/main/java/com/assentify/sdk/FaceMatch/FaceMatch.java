package com.assentify.sdk.FaceMatch;

import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.FaceEventsKt.getRandomEvents;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.Core.Constants.DoneFlags;
import com.assentify.sdk.Core.Constants.EventsErrorMessages;
import com.assentify.sdk.Core.Constants.FaceEventStatus;
import com.assentify.sdk.logging.BugsnagObject;
import com.assentify.sdk.Core.Constants.ActiveLiveEvents;
import com.assentify.sdk.Core.Constants.ActiveLiveType;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.FaceEvents;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.AssetsAudioPlayer;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.tflite.Classifier;
import com.assentify.sdk.tflite.FaceQualityCheck.FaceEventCallback;
import com.assentify.sdk.tflite.FaceQualityCheck.FaceQualityCheck;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;


public class FaceMatch extends CameraPreview implements RemoteProcessingCallback {


    private FaceMatchCallback faceMatchCallback;
    private  EnvironmentalConditions environmentalConditions;
    private String secondImage = "";
    private Bitmap croppedBitmap = null;
    private double brightness;
    private List<Bitmap> highQualityBitmaps = new ArrayList<>();

    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();

    Boolean performLivenessFace;

    Boolean performPassiveLivenessFace;
    Boolean saveCapturedVideo;
    Boolean storeImageStream;
    ConfigModel configModel;

    String stepId;
    Boolean showCountDownView = true;
    Boolean isCountDownStarted = true;
    private MotionType motion = MotionType.NO_DETECT;

    private ZoomType zoom = ZoomType.NO_DETECT;
    private RectF rectFCard = new RectF();
    private List<RectF> motionRectF = new ArrayList<>();

    private List<MotionType> sendingFlags = new ArrayList<>();

    private List<ZoomType> sendingFlagsZoom = new ArrayList<>();
    private ExecutorService createByteArray = Executors.newSingleThreadExecutor();

    private String faceMatch = "FaceMatch";
    private int videoCounter = -1;

    private DetectIfRectFInsideTheScreen detectIfInsideTheScreen = new DetectIfRectFInsideTheScreen();
    private boolean isRectFInsideTheScreen = false;

    private FaceQualityCheck faceQualityCheck = null;
    private FaceEvents faceEvent = FaceEvents.NO_DETECT;


    private List<FaceEventStatus> eventCompletionList = new ArrayList<>();

    private volatile Boolean startActiveLiveCheck = true;
    private volatile Boolean hasMoved = true;
    private CountDownTimer activeLiveTimer;

    private AssetsAudioPlayer audioPlayer;

    List<byte[]> clips = new ArrayList<>();

    private List<Bitmap> livenessCheckArray = new ArrayList<>();

    private int localLivenessLimit;

    private long lastProcessedTime = 0L;

    public FaceMatch() {
    }

    int livnessRetryCount = 0;
    int retryCount = 0;
    public FaceMatch(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                     Boolean performLivenessFace,
                     Boolean showCountDownView
    ) {
        this.apiKey = apiKey;
        this.performLivenessFace = performLivenessFace;
        this.environmentalConditions = environmentalConditions;
        if (this.performLivenessFace && this.environmentalConditions.getActiveLiveType() != ActiveLiveType.NONE && environmentalConditions.getActiveLivenessCheckCount() != 0) {
            fillCompletionMap();
            enableActiveLive(true);
        } else {
            startActiveLiveCheck = false;
            eventCompletionList = new ArrayList<>();
        }
        frontCamera();
        this.configModel = configModel;
        this.showCountDownView = showCountDownView;
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
        for (StepDefinitions item : configModel.getStepDefinitions()) {
            if (Integer.parseInt(this.stepId) == item.getStepId()) {
                if (performPassiveLivenessFace == null) {
                  //  performPassiveLivenessFace = item.getCustomization().getPerformLivenessDetection();
                    performPassiveLivenessFace = true;
                }
                if (saveCapturedVideo == null) {
                    saveCapturedVideo = item.getCustomization().getSaveCapturedVideo();
                }
                if (storeImageStream == null) {
                    storeImageStream = item.getCustomization().getStoreImageStream();
                }

            }
        }
        if (this.performPassiveLivenessFace) {
            localLivenessLimit = 12;
        } else {
            localLivenessLimit = 0;
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
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {
        if (getActivity() != null) {
            BugsnagObject.INSTANCE.initialize(getActivity().getApplicationContext(), configModel);
        }


        if (startActiveLiveCheck && performLivenessFace && hasMoved && this.environmentalConditions.getActiveLiveType() != ActiveLiveType.NONE && environmentalConditions.getActiveLivenessCheckCount() != 0) {
            hasMoved = false;
            nextMove();
        } else {
            if (areAllEventsDone()) {
                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.Good);
            }

        }
        if (faceQualityCheck == null) {
            faceQualityCheck = new FaceQualityCheck();
        }
        if (audioPlayer == null) {
            if (getActivity() != null) {
                audioPlayer = new AssetsAudioPlayer(getActivity());
            }
        }


        this.results = results;
        if (hasFace()) {
            faceQualityCheck.checkFaceQualityAndExpressions(normalImage, new FaceEventCallback() {
                        @Override
                        public void onFaceEventDetected(FaceEvents result) {
                            if(areAllEventsDone()){
                                if(result == FaceEvents.BLINK){
                                    faceEvent =  FaceEvents.Good;
                                }else {
                                    faceEvent = result;
                                }
                            }else {
                                faceEvent = result;
                            }
                            if (startActiveLiveCheck && performLivenessFace && environmentalConditions.getActiveLiveType() != ActiveLiveType.NONE && environmentalConditions.getActiveLivenessCheckCount() != 0) {
                                if (environmentalConditions.getActiveLiveType() == ActiveLiveType.Actions) {
                                    isSpecificItemFlagEqualToActions(faceEvent);
                                }
                                if (environmentalConditions.getActiveLiveType() == ActiveLiveType.Wink || environmentalConditions.getActiveLiveType() == ActiveLiveType.BLINK) {
                                    isSpecificItemFlagEqualToWinkAndBLINK(result);
                                }
                            }
                        }
                    }, ImageUtils.getRecommendedDelayForFaceCheck(getActivity())
            );
            listScaleRectF.forEach((item) -> {
                if (item.component2().contains(ConstantsValues.FaceName)) {
                    isRectFInsideTheScreen = detectIfInsideTheScreen.isRectFWithinMargins(item.component1(), previewWidth, previewHeight);
                }
            });
        } else {
            if (start) {
                livenessCheckArray.clear();
                highQualityBitmaps.clear();
                faceEvent = FaceEvents.NO_DETECT;
            }
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
            if (isRectFInsideTheScreen && faceEvent == FaceEvents.Good && zoom == ZoomType.SENDING && environmentalConditions.checkConditions(brightness, environmentalConditions) == BrightnessEvents.Good) {
                if (performPassiveLivenessFace && start) {
                    if (livenessCheckArray.size() < localLivenessLimit) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastProcessedTime > ImageUtils.getDynamicDelay(getActivity())) {
                            livenessCheckArray.add(normalImage);
                            lastProcessedTime = currentTime;
                        }

                    }
                }
                if(start){
                    highQualityBitmaps.add(normalImage);
                }
                setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            } else {
                if (start) {
                    livenessCheckArray.clear();
                    highQualityBitmaps.clear();
                }
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            }
        } else {
            if (start) {
                livenessCheckArray.clear();
                highQualityBitmaps.clear();
            }
            setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
        }

        checkEnvironment();
    }


    protected void checkEnvironment() {
        if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (hasFace() && start) {
                    startRecording();
                }
            }
            ImageBrightnessChecker imageBrightnessChecker = new ImageBrightnessChecker();
            DetectMotion detectMotion = new DetectMotion();
            DetectZoom detectZoom = new DetectZoom();
            brightness = imageBrightnessChecker.getAverageBrightness(croppedBitmap);
            try {
                if (motionRectF.size() >= 2) {
                    if (results.isEmpty()) {
                        motionRectF.clear();
                        sendingFlags.clear();
                        sendingFlagsZoom.clear();
                        motion = MotionType.NO_DETECT;
                        zoom = ZoomType.NO_DETECT;
                    } else {
                        motion = detectMotion.calculatePercentageChangeFace(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
                        zoom = detectZoom.calculateFacePercentageChangeWidth(motionRectF.get(motionRectF.size() - 1));
                        if (motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
                            sendingFlags.add(MotionType.SENDING);
                            sendingFlagsZoom.add(ZoomType.SENDING);
                        } else {
                            sendingFlags.clear();
                            sendingFlagsZoom.clear();
                        }
                    }
                }
            } catch (Exception e) {
                motionRectF.clear();
                sendingFlags.clear();
                sendingFlagsZoom.clear();
                motion = MotionType.NO_DETECT;
                zoom = ZoomType.NO_DETECT;
            }

            if (this.showCountDownView) {
                if (!hasFace() || !isRectFInsideTheScreen || faceEvent != FaceEvents.Good) {
                    stopCountDown();
                    isCountDownStarted = true;
                }
            }
            if (environmentalConditions.checkConditions(
                    brightness, environmentalConditions
            ) == BrightnessEvents.Good && motion == MotionType.SENDING && zoom == ZoomType.SENDING && faceEvent == FaceEvents.Good) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (start && highQualityBitmaps.size() != 0 && sendingFlags.size() > 2 && isRectFInsideTheScreen && sendingFlagsZoom.size() > ZoomLimit
                            && livenessCheckArray.size() == localLivenessLimit) {
                        if (hasFace()) {
                            if (this.showCountDownView) {
                                showCountDown(new CountDownCallback() {
                                    @Override
                                    public void onCountDownFinished() {
                                        if (start) {
                                            isCountDownStarted = true;
                                            stopRecording();
                                        }
                                    }
                                }, environmentalConditions.getCountdownMumbersColor(), isCountDownStarted);
                                isCountDownStarted = false;
                            } else {
                                if (start) {
                                    stopRecording();
                                }
                            }

                        }


                    }
                }
            }


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    faceMatchCallback.onEnvironmentalConditionsChange(
                            environmentalConditions.checkConditions(
                                    brightness, environmentalConditions
                            ),
                            sendingFlags.isEmpty() ? MotionType.NO_DETECT : sendingFlags.size() > 5 ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            !start && areAllEventsDone() ? FaceEvents.Good : faceEvent, zoom,faces()
                    );
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
                    sendingFlags.clear();
                    sendingFlagsZoom.clear();
                    livenessCheckArray.clear();
                    BugsnagObject.INSTANCE.logInfo("Face match done with event  : " + eventName + BaseResponseDataModel.getResponse(),configModel);
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        FaceExtractedModel faceExtractedModel = FaceExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                        FaceResponseModel faceResponseModel = new FaceResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                faceExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );


                        faceMatchCallback.onComplete(faceResponseModel, DoneFlags.Success);
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
                            BaseResponseDataModel.setError(EventsErrorMessages.OnRetryFaceMessage);
                            faceMatchCallback.onRetry(BaseResponseDataModel);
                        }
                    } else if(eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE)){
                        livnessRetryCount++;
                        if (livnessRetryCount ==  environmentalConditions.getFaceLivenessRetryCount()){
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
                            BaseResponseDataModel.setError(EventsErrorMessages.OnLivenessFaceUpdateMessage);
                            faceMatchCallback.onLivenessUpdate(BaseResponseDataModel);
                        }
                    } else
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) ;
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
            });
        }

    }


    public boolean hasFace() {
        return faces() == 1;
    }

    public int faces() {
        int faces = 0;
        for (Classifier.Recognition item : results) {
            if (item.getDetectedClass() == 1 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                faces = faces+1;
            }
        }
        return faces;
    }

    @Override
    protected void onStopRecordVideo() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    start = false;
                    faceMatchCallback.onSend();

                }
            });
        }
        videoCounter = videoCounter + 1;
        clips = new ArrayList<>();
        createByteArray.execute(() -> {
            BugsnagObject.INSTANCE.logInfo("Face match started : perform passive liveness face : " + performPassiveLivenessFace.toString(),configModel);
            if (performPassiveLivenessFace) {
                runLivenessCheck();
                int middleIndex = clips.size() / 2;
                BugsnagObject.INSTANCE.logInfo("Face match started : clips size : " + clips.size(),configModel);
                if (!clips.isEmpty()) {
                    remoteProcessing.starProcessingFace(
                            HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.FACE_MATCH),
                            configModel,
                            stepId,
                            clips.get(middleIndex),
                            clips,
                            ImageUtils.base64ToByteArray(this.secondImage),
                            performPassiveLivenessFace,
                            livnessRetryCount,
                            true,
                            false,
                            "ConnectionId"
                    );
                } else {
                    BugsnagObject.INSTANCE.logInfo("Face match done with error clips  : ",configModel);
                    start = true;
                    faceMatchCallback.onRetry(new BaseResponseDataModel(
                            HubConnectionTargets.ON_RETRY,
                            "Please hold your hand",
                            EventsErrorMessages.OnRetryFaceMessage,
                            false
                    ));
                }

            } else {
                int middleIndex = highQualityBitmaps.size() / 2;
                BugsnagObject.INSTANCE.logInfo("Face match started : clips size : " + clips.size(),configModel);
                remoteProcessing.starProcessingFace(
                        HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.FACE_MATCH),
                        configModel,
                        stepId,
                        ImageUtils.convertBitmapToByteArray(highQualityBitmaps.get(middleIndex), BlockType.FACE_MATCH, getActivity()),
                        new ArrayList<>(),
                        ImageUtils.base64ToByteArray(this.secondImage),
                        performPassiveLivenessFace,
                        livnessRetryCount,
                        true,
                        false,
                        "ConnectionId"
                );
            }

        });

        // remoteProcessing.uploadVideo(videoCounter, video, configModel, faceMatch);
    }

    private void runLivenessCheck() {
        ParallelImageProcessing processor = new ParallelImageProcessing();
        clips = processor.processClipsToByteArrayInParallel(livenessCheckArray, getActivity());
    }

    public void stopScanning() {
        closeCamera();
    }


    private void fillCompletionMap() {
        start = false;
        eventCompletionList = new ArrayList<>();
        for (ActiveLiveEvents event : getRandomEvents(environmentalConditions.getActiveLiveType(), environmentalConditions.getActiveLivenessCheckCount())) {
            if (event == ActiveLiveEvents.PitchUp) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.PitchUp, false));
            }
            if (event == ActiveLiveEvents.PitchDown) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.PitchDown, false));
            }
            if (event == ActiveLiveEvents.YawRight) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.YawRight, false));
            }
            if (event == ActiveLiveEvents.YawLeft) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.YawLeft, false));
            }
            if (event == ActiveLiveEvents.WinkLeft) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.WinkLeft, false));
            }
            if (event == ActiveLiveEvents.WinkRight) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.WinkRight, false));
            }
            if (event == ActiveLiveEvents.BLINK) {
                eventCompletionList.add(new FaceEventStatus(FaceEvents.BLINK, false));
            }
        }

    }

    private void isSpecificItemFlagEqualToActions(
            FaceEvents targetEvent) {
        for (FaceEventStatus status : eventCompletionList) {
            if (!status.isCompleted()) {
                if (status.getEvent() == targetEvent) {
                    status.setCompleted(true);
                    successActiveLive();
                } else {
                    if (targetEvent != FaceEvents.NO_DETECT &&
                            targetEvent != FaceEvents.Good &&
                            targetEvent != FaceEvents.RollRight &&
                            targetEvent != FaceEvents.RollLeft &&
                            targetEvent != FaceEvents.WinkLeft &&
                            targetEvent != FaceEvents.WinkRight &&
                            targetEvent != FaceEvents.BLINK
                    ) {
                        resetActiveLive();
                    }
                }
                break;
            }
        }

    }

    private void isSpecificItemFlagEqualToWinkAndBLINK(
            FaceEvents targetEvent) {

        for (FaceEventStatus status : eventCompletionList) {
            if (!status.isCompleted()) {
                if (status.getEvent() == targetEvent) {
                    status.setCompleted(true);
                    successActiveLive();
                } else {
                    if (targetEvent != FaceEvents.NO_DETECT &&
                            targetEvent != FaceEvents.Good &&
                            targetEvent != FaceEvents.RollRight &&
                            targetEvent != FaceEvents.RollLeft &&
                            targetEvent != FaceEvents.PitchDown &&
                            targetEvent != FaceEvents.PitchUp &&
                            targetEvent != FaceEvents.YawLeft &&
                            targetEvent != FaceEvents.YawRight
                    ) {
                        resetActiveLive();
                    }
                }
                break;
            }
        }
    }

    private void successActiveLive() {
        if (startActiveLiveCheck) {
            if (activeLiveTimer != null) {
                activeLiveTimer.cancel();
            }
            audioPlayer.playAudio(ConstantsValues.AudioFaceSuccess);
            startActiveLiveCheck = false;
            showSuccessLiveCheck();
            if (areAllEventsDone()) {
                start = true;
                enableActiveLive(false);
            } else {
                activeLiveTimer = new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        hasMoved = true;
                        startActiveLiveCheck = true;
                    }
                };
                activeLiveTimer.start();
            }
        }
    }

    private void resetActiveLive() {
        if (startActiveLiveCheck) {
            if (activeLiveTimer != null) {
                activeLiveTimer.cancel();
            }
            audioPlayer.playAudio(ConstantsValues.AudioWrong);
            startActiveLiveCheck = false;
            showErrorLiveCheck();
            activeLiveTimer = new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    fillCompletionMap();
                    hasMoved = true;
                    startActiveLiveCheck = true;
                }
            };
            activeLiveTimer.start();
        }

    }


    private void nextMove() {
        startActiveLiveCheck = false;
        if (activeLiveTimer != null) {
            activeLiveTimer.cancel();
        }
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (FaceEventStatus status : eventCompletionList) {
                        if (!status.isCompleted()) {
                            if (status.getEvent() == FaceEvents.PitchUp) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.PitchUp);
                            }
                            if (status.getEvent() == FaceEvents.PitchDown) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.PitchDown);
                            }
                            if (status.getEvent() == FaceEvents.YawRight) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.YawRight);
                            }
                            if (status.getEvent() == FaceEvents.YawLeft) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.YawLeft);
                            }
                            if (status.getEvent() == FaceEvents.WinkLeft) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.WinkLeft);
                            }
                            if (status.getEvent() == FaceEvents.WinkRight) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.WinkRight);
                            }
                            if (status.getEvent() == FaceEvents.BLINK) {
                                faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.BLINK);
                            }
                            setActiveLiveMove(status.getEvent());
                            break;
                        }
                    }

                    activeLiveTimer = new CountDownTimer(4000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            startActiveLiveCheck = true;
                        }
                    };
                    activeLiveTimer.start();
                }
            });
        }

    }

    private boolean areAllEventsDone() {
        return eventCompletionList.stream().allMatch(FaceEventStatus::isCompleted);
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
        if (this.createByteArray != null) {
            this.createByteArray.shutdown();
        }
        if (audioPlayer != null) {
            audioPlayer.stopAudio();
        }
        if (faceQualityCheck != null) {
            faceQualityCheck.stop();
        }
    }

    @Override
    public void onUploadProgress(int progress) {
        faceMatchCallback.onUploadingProgress(progress);

    }
}
