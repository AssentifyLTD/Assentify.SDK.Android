package com.assentify.sdk.FaceMatch;

import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.FaceEventsKt.getRandomEvents;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.Core.Constants.ActiveLiveEvents;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;


public class FaceMatch extends CameraPreview implements RemoteProcessingCallback {


    private FaceMatchCallback faceMatchCallback;
    private final EnvironmentalConditions environmentalConditions;
    private String secondImage = "";
    private Bitmap croppedBitmap = null;
    private double brightness;
    private List<Bitmap> highQualityBitmaps = new ArrayList<>();

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

    String stepId;
    Boolean showCountDownView = true;
    Boolean isCountDownStarted = true;
    private MotionType motion = MotionType.NO_DETECT;

    private ZoomType zoom = ZoomType.NO_DETECT;
    private RectF rectFCard = new RectF();
    private List<RectF> motionRectF = new ArrayList<>();

    private List<MotionType> sendingFlags = new ArrayList<>();

    private List<ZoomType> sendingFlagsZoom = new ArrayList<>();
    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private String faceMatch = "FaceMatch";
    private int videoCounter = -1;

    private DetectIfRectFInsideTheScreen detectIfInsideTheScreen = new DetectIfRectFInsideTheScreen();
    private boolean isRectFInsideTheScreen = false;

    private FaceQualityCheck faceQualityCheck = null;
    private FaceEvents faceEvent = FaceEvents.NO_DETECT;


    private Map<FaceEvents, Boolean> eventCompletionMap = new EnumMap<>(FaceEvents.class);
   private volatile Boolean startActiveLiveCheck = true;
   private volatile Boolean hasMoved  = true;
   private CountDownTimer activeLiveTimer;

    private AssetsAudioPlayer audioPlayer;

    public FaceMatch(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                     Boolean processMrz,
                     Boolean performLivenessDocument,
                     Boolean performLivenessFace,
                     Boolean saveCapturedVideo,
                     Boolean storeCapturedDocument,
                     Boolean storeImageStream,
                     Boolean showCountDownView
    ) {
        this.apiKey = apiKey;
        this.performLivenessFace = performLivenessFace;
        if (this.performLivenessFace) {
            fillCompletionMap();
            enableActiveLive(true);
        } else {
            startActiveLiveCheck = false;
            eventCompletionMap = new HashMap<>();
        }
        frontCamera();
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDocument = performLivenessDocument;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;
        this.showCountDownView = showCountDownView;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
        if(this.stepId==null){
            long stepsCount = this.configModel.getStepDefinitions().stream()
                    .filter(item -> item.getStepDefinition().equals("FaceImageAcquisition"))
                    .count();

            if(stepsCount==1){
                for (StepDefinitions item : this.configModel.getStepDefinitions()) {
                    if (item.getStepDefinition().equals("FaceImageAcquisition")) {
                        this.stepId = String.valueOf(item.getStepId());
                        break;
                    }
                }
            }else {
                if(this.stepId==null){
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
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {
        if (startActiveLiveCheck && performLivenessFace && hasMoved) {
            hasMoved = false;
            nextMove();
        }else {
            faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.Good);

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
        if (hasFaceOrCard()) {
            faceQualityCheck.checkQuality(normalImage, new FaceEventCallback() {
                @Override
                public void onFaceEventDetected(FaceEvents result) {
                    faceEvent = result;
                    if (startActiveLiveCheck && performLivenessFace) {
                        isSpecificItemFlagEqualTo(faceEvent);
                    }
                }
            });
            listScaleRectF.forEach((item) -> {
                if (item.component2().contains(ConstantsValues.FaceName)) {
                    isRectFInsideTheScreen = detectIfInsideTheScreen.isRectFWithinMargins(item.component1(), previewWidth, previewHeight);
                }
            });
        } else {
            if (start) {
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
            if (isRectFInsideTheScreen && faceEvent == FaceEvents.Good && zoom == ZoomType.SENDING && environmentalConditions.checkConditions(brightness) == BrightnessEvents.Good) {
                highQualityBitmaps.add(normalImage);
                setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            } else {
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
                    sendingFlags.clear();
                    sendingFlagsZoom.clear();
                    motion = MotionType.NO_DETECT;
                    zoom = ZoomType.NO_DETECT;
                } else {
                    motion = detectMotion.calculatePercentageChange(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
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

            if (this.showCountDownView) {
                if (!hasFaceOrCard() || !isRectFInsideTheScreen || faceEvent != FaceEvents.Good) {
                    stopCountDown();
                    isCountDownStarted = true;
                }
            }
            if (environmentalConditions.checkConditions(
                    brightness
            ) == BrightnessEvents.Good && motion == MotionType.SENDING && zoom == ZoomType.SENDING && faceEvent == FaceEvents.Good) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (start && highQualityBitmaps.size() != 0 && sendingFlags.size() > 2 && isRectFInsideTheScreen && sendingFlagsZoom.size() > ZoomLimit) {
                        if (hasFaceOrCard()) {
                            if (this.showCountDownView) {
                                showCountDown(new CountDownCallback() {
                                    @Override
                                    public void onCountDownFinished() {
                                        if (start) {
                                            isCountDownStarted = true;
                                            stopRecording();
                                        }
                                    }
                                }, environmentalConditions.getHoldHandColor(), isCountDownStarted);
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
                                    brightness
                            ),
                            sendingFlags.isEmpty() ? MotionType.NO_DETECT : sendingFlags.size() > 5 ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            !start && areAllEventsDone() ? FaceEvents.Good : faceEvent, zoom
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
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        FaceExtractedModel faceExtractedModel = FaceExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                        FaceResponseModel faceResponseModel = new FaceResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                faceExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );


                        faceMatchCallback.onComplete(faceResponseModel);
                        start = false;
                    } else
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE);
                    switch (eventName) {
                        case HubConnectionTargets.ON_ERROR:
                            faceMatchCallback.onError(BaseResponseDataModel);
                            break;
                        case HubConnectionTargets.ON_RETRY:
                            faceMatchCallback.onRetry(BaseResponseDataModel);
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
                        case HubConnectionTargets.ON_LIVENESS_UPDATE:
                            faceMatchCallback.onLivenessUpdate(BaseResponseDataModel);
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


    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        this.createBase64.shutdown();
        if (audioPlayer != null) {
            audioPlayer.stopAudio();
        }
        faceQualityCheck.stop();
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
        createBase64.execute(() -> {
            remoteProcessing.starProcessing(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.FACE_MATCH),
                    "",
                    ImageUtils.convertBitmapToBase64(highQualityBitmaps.get(highQualityBitmaps.size() - 1), BlockType.FACE_MATCH, getActivity()),
                    configModel,
                    "",
                    this.secondImage,
                    "ConnectionId",
                    getVideoPath(configModel, faceMatch, videoCounter),
                    hasFace(),
                    processMrz,
                    performLivenessDocument,
                    performLivenessFace,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    true,
                    storeImageStream,
                    stepId,
                    new ArrayList<>()
            );
        });

        // remoteProcessing.uploadVideo(videoCounter, video, configModel, faceMatch);
    }

    public void stopScanning() {
        closeCamera();
    }


    private void fillCompletionMap() {
        showInitLayoutFace();
        activeLiveTimer = new CountDownTimer(4000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                start = false;
                eventCompletionMap = new HashMap<>();
                for (ActiveLiveEvents event : getRandomEvents()) {
                    if (event == ActiveLiveEvents.PitchUp) {
                        eventCompletionMap.put(FaceEvents.PitchUp, false);
                    }
                    if (event == ActiveLiveEvents.PitchDown) {
                        eventCompletionMap.put(FaceEvents.PitchDown, false);
                    }
                    if (event == ActiveLiveEvents.YawRight) {
                        eventCompletionMap.put(FaceEvents.YawRight, false);
                    }
                    if (event == ActiveLiveEvents.YawLeft) {
                        eventCompletionMap.put(FaceEvents.YawLeft, false);
                    }
                }
            }
        };

    }



    private void isSpecificItemFlagEqualTo(
            FaceEvents targetEvent) {
        eventCompletionMap.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .findFirst()
                .ifPresent(entry -> {
                    if (entry.getKey() == targetEvent) {
                        eventCompletionMap.put(entry.getKey(), true);
                        successActiveLive();
                    } else {
                        if (targetEvent != FaceEvents.NO_DETECT &&
                                targetEvent != FaceEvents.Good &&
                                targetEvent != FaceEvents.RollRight &&
                                targetEvent != FaceEvents.RollLeft) {
                            resetActiveLive();
                        }
                    }
                });
    }

    private void successActiveLive() {
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
            activeLiveTimer = new CountDownTimer(4000, 1000) {
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

    private void resetActiveLive() {
      if (activeLiveTimer != null) {
            activeLiveTimer.cancel();
        }
       audioPlayer.playAudio(ConstantsValues.AudioWrong);
        startActiveLiveCheck = false;
        showErrorLiveCheck();
       activeLiveTimer = new CountDownTimer(4000, 1000) {
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



    private void nextMove() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    eventCompletionMap.entrySet().stream()
                            .filter(entry -> !entry.getValue())
                            .findFirst()
                            .ifPresent(entry -> {

                                if (entry.getKey() == FaceEvents.PitchUp) {
                                    faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.PitchUp);
                                }
                                if (entry.getKey() == FaceEvents.PitchDown) {
                                    faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.PitchDown);
                                }
                                if (entry.getKey() == FaceEvents.YawRight) {
                                    faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.YawRight);
                                }
                                if (entry.getKey() == FaceEvents.YawLeft) {
                                    faceMatchCallback.onCurrentLiveMoveChange(ActiveLiveEvents.YawLeft);
                                }
                                setActiveLiveMove(entry.getKey());
                            });

                }
            });
        }
    }

    private boolean areAllEventsDone() {
        for (boolean isDone : eventCompletionMap.values()) {
            if (!isDone) {
                return false;
            }
        }
        return true;
    }


}
