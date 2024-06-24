package com.assentify.sdk.ScanIDCard;


import static  com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

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
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails;
import  com.assentify.sdk.RemoteClient.Models.Templates;
import  com.assentify.sdk.RemoteClient.Models.TemplatesByCountry;
import  com.assentify.sdk.RemoteClient.RemoteClient;
import  com.assentify.sdk.RemoteClient.RemoteIdPowerService;
import com.assentify.sdk.ScanIDCard.IDCardCallback;
import  com.assentify.sdk.tflite.Classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.sentry.SentryLevel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ScanIDCard extends CameraPreview implements RemoteProcessingCallback {


    private IDCardCallback idCardCallback;
    private final EnvironmentalConditions environmentalConditions;
    private String templateId;

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

    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private int videoCounter =-1;
    private int Lis =-1;

    private  int order = 0;

    private List<KycDocumentDetails>  kycDocumentDetails = new ArrayList<>();;
    public ScanIDCard(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                      Boolean processMrz,
                      Boolean performLivenessDetection,
                      Boolean saveCapturedVideo,
                      Boolean storeCapturedDocument,
                      Boolean storeImageStream,
                      IDCardCallback idCardCallback,
                      List<KycDocumentDetails>  kycDocumentDetails
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.processMrz = processMrz;
        this.performLivenessDetection = performLivenessDetection;
        this.saveCapturedVideo = saveCapturedVideo;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
        this.configModel = configModel;
        this.idCardCallback = idCardCallback;
        this.kycDocumentDetails = kycDocumentDetails;

        if(!this.kycDocumentDetails.isEmpty()){
            KycDocumentDetails firstKycDocument = kycDocumentDetails.get(0);
            this.changeTemplateId(firstKycDocument.getTemplateProcessingKeyInformation());
        }

    }


    private void changeTemplateId(String templateId) {
        SentryManager.INSTANCE.registerEvent(SentryKeys.ID, SentryLevel.INFO);
        this.templateId = templateId;
        createBase64 = Executors.newSingleThreadExecutor();
        highQualityBitmaps.clear();
        motionRectF.clear();
        sendingFlagsZoom.clear();
        sendingFlagsMotion.clear();
        videoCounter =-1;
        start = true;
        remoteProcessing = new RemoteProcessing();
        remoteProcessing.setCallback(this);

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
                    sendingFlagsMotion.clear();
                    sendingFlagsZoom.clear();
                }
            }
        }
        if (environmentalConditions.checkConditions(
                brightness) && motion == MotionType.SENDING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (start && highQualityBitmaps.size() != 0 && sendingFlagsZoom.size() > 1 && sendingFlagsMotion.size() > 1) {
                    if (hasFaceOrCard()) {
                        stopRecording();
                    }
                }

            }
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                idCardCallback.onEnvironmentalConditionsChange(
                        brightness,
                        sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT :  sendingFlagsMotion.size() > 5 ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND ,

                        zoom);
            }
        });


    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {
        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.ID,eventName, Objects.requireNonNull(BaseResponseDataModel.getResponse()));

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                highQualityBitmaps.clear();
                motionRectF.clear();
                sendingFlagsZoom.clear();
                sendingFlagsMotion.clear();
                if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                    storageUtils.deleteFolderContents(storageUtils.getImageFolder(getActivity().getApplicationContext()));
                    storageUtils.deleteFolderContents(storageUtils.getVideosFolder(getActivity().getApplicationContext()));

                    IDExtractedModel idExtractedModel = IDExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse());
                    IDResponseModel idResponseModel = new IDResponseModel(
                            BaseResponseDataModel.getDestinationEndpoint(),
                            idExtractedModel,
                            BaseResponseDataModel.getError(),
                            BaseResponseDataModel.getSuccess()
                    );


                    idCardCallback.onComplete(idResponseModel,order);
                    start = false;
                    order = order + 1;
                    if(!kycDocumentDetails.isEmpty() && order < kycDocumentDetails.size()){
                        changeTemplateId(kycDocumentDetails.get(order).getTemplateProcessingKeyInformation());
                    }
                } else
                    start = eventName.equals(HubConnectionTargets.ON_WRONG_TEMPLATE) || eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_RETRY) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED);
                switch (eventName) {
                    case HubConnectionTargets.ON_ERROR:
                        idCardCallback.onError(BaseResponseDataModel);
                        break;
                    case HubConnectionTargets.ON_RETRY:
                        idCardCallback.onRetry(BaseResponseDataModel);
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
                    case HubConnectionTargets.ON_LIVENESS_UPDATE:
                       idCardCallback.onLivenessUpdate(BaseResponseDataModel);
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

                }
            }
        });

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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                idCardCallback.onSend();
            }
        });
        SentryManager.INSTANCE.registerCallbackEvent(SentryKeys.ID,"onSend", "");

        start = false;
        videoCounter = videoCounter + 1;
        createBase64.execute(() -> {

            remoteProcessing.starProcessing(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.ID_CARD),
                    ImageUtils.convertBitmapToBase64(highQualityBitmaps.get(highQualityBitmaps.size() - 1), BlockType.ID_CARD, getActivity()),
                    configModel,
                    this.templateId,
                    "",
                    "ConnectionId",
                    getVideoPath(configModel, this.templateId,videoCounter),
                    false,
                    processMrz,
                    performLivenessDetection,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    false,
                    storeImageStream,
                    "IdentificationDocumentCapture"
            );
        });

        remoteProcessing.uploadVideo(videoCounter, video, configModel, this.templateId);
    }


}

