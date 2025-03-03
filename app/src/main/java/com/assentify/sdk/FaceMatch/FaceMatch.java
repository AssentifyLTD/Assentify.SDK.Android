package com.assentify.sdk.FaceMatch;

import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.FaceEvents;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.LivenessType;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.SentryKeys;
import com.assentify.sdk.Core.Constants.SentryManager;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.tflite.Classifier;
import com.assentify.sdk.tflite.FaceQualityCheck.FaceEventCallback;
import com.assentify.sdk.tflite.FaceQualityCheck.FaceQualityCheck;
import com.assentify.sdk.tflite.Liveness.CheckLiveness;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.sentry.SentryLevel;
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

    private CheckLiveness checkIsLive = null;
    private LivenessType livenessType = LivenessType.NON;
    private FaceQualityCheck faceQualityCheck = null;
    private FaceEvents faceEvent = FaceEvents.NO_DETECT;

    private List<Bitmap> livenessCheckArray = new ArrayList<>();

    List<String> clips = new ArrayList<>();
    List<LivenessType> livenessTypeResults = new ArrayList<>();

    private int localLivenessLimit;



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
        frontCamera();
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDocument = performLivenessDocument;
        this.performLivenessFace = performLivenessFace;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;
        this.showCountDownView = showCountDownView;
        if (this.performLivenessFace) {
            localLivenessLimit = 12;
        } else {
            localLivenessLimit = 0;
        }

    }


    public void setFaceMatchCallback(FaceMatchCallback faceMatchCallback) {
        SentryManager.INSTANCE.registerEvent(SentryKeys.Face, SentryLevel.INFO);
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

        if (checkIsLive == null) {
            checkIsLive = new CheckLiveness();
            checkIsLive.loadTfliteModel(requireContext());
        }
        if (faceQualityCheck == null) {
            faceQualityCheck = new FaceQualityCheck();
        }


        this.results = results;
        if (hasFaceOrCard()) {
            faceQualityCheck.checkQuality(croppedBitmap, new FaceEventCallback() {
                @Override
                public void onFaceEventDetected(FaceEvents result) {
                    faceEvent = result;
                }
            });
            listScaleRectF.forEach((item) -> {
                if (item.component2().contains(ConstantsValues.FaceName)) {
                    isRectFInsideTheScreen = detectIfInsideTheScreen.isRectFWithinMargins(item.component1(), previewWidth, previewHeight);
                }
            });
        } else {
            if (start) {
                livenessCheckArray.clear();
                livenessTypeResults.clear();
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
                if (performLivenessFace && start) {
                    if(livenessCheckArray.size()<localLivenessLimit){
                        livenessCheckArray.add(normalImage);
                        if (checkIsLive.preprocessAndPredict(normalImage) == LivenessType.LIVE) {
                            livenessTypeResults.add(LivenessType.LIVE);
                        }
                    }
                }
                highQualityBitmaps.add(normalImage);
                setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            } else {
                if (start) {
                    livenessCheckArray.clear();
                    livenessTypeResults.clear();
                }
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            }
        } else {
            if (start) {
                livenessCheckArray.clear();
                livenessTypeResults.clear();
            }
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
                    if (start && highQualityBitmaps.size() != 0 && sendingFlags.size() > 2 && isRectFInsideTheScreen && sendingFlagsZoom.size() > ZoomLimit && livenessCheckArray.size() == localLivenessLimit) {
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
                            !start ? FaceEvents.Good : faceEvent, zoom
                    );
                }
            });
        }

    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {
        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.Face, eventName, Objects.requireNonNull(BaseResponseDataModel.getResponse()));
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    highQualityBitmaps.clear();
                    livenessCheckArray.clear();
                    livenessTypeResults.clear();
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
        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.ID, "onSend", "");
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
        livenessType = LivenessType.NON;
        createBase64.execute(() -> {
            if (performLivenessFace) {
                runLivenessCheck();
            }
            if (performLivenessFace) {
                if ((double) livenessTypeResults.size() / livenessCheckArray.size() > ConstantsValues.LIVENESS_THRESHOLD) {
                    livenessType = LivenessType.LIVE;
                }
            } else {
                livenessType = LivenessType.LIVE;
            }
            if (livenessType == LivenessType.LIVE) {
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
                        "FaceImageAcquisition", clips
                );
            } else {
                this.onMessageReceived(HubConnectionTargets.ON_LIVENESS_UPDATE, new BaseResponseDataModel(
                        HubConnectionTargets.ON_LIVENESS_UPDATE,
                        "",
                        "",
                        false
                ));
            }
        });

        // remoteProcessing.uploadVideo(videoCounter, video, configModel, faceMatch);
    }

    public void stopScanning() {
        closeCamera();
    }

    private void runLivenessCheck() {
        ParallelImageProcessing processor = new ParallelImageProcessing();
        clips = processor.processClipsToBase64InParallel(livenessCheckArray, getActivity());
    }

}
