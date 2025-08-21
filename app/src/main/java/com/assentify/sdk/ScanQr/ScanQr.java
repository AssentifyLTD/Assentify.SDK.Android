package com.assentify.sdk.ScanQr;


import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.getIgnoredProperties;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.preparePropertiesToTranslate;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.FullNameKey;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getRemainingWords;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getSelectedWords;

import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKeys;
import com.assentify.sdk.Core.Constants.Language;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.LanguageTransformation.LanguageTransformation;
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.ScanIDCard.IDExtractedModel;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails;
import com.assentify.sdk.ScanIDCard.IDResponseModel;
import com.assentify.sdk.tflite.Classifier;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import kotlin.Pair;


public class ScanQr extends CameraPreview implements RemoteProcessingCallback , LanguageTransformationCallback {


    private ScanQrCallback scanQrCallback;

    private boolean start = true;

    private RemoteProcessing remoteProcessing;

    private List<KycDocumentDetails> kycDocumentDetails = new ArrayList<>();
    private ConfigModel configModel;

    private BarcodeScanner qrScanner;
    private String language;

    private String apiKey;

    private IDResponseModel idResponseModel;
    private EnvironmentalConditions environmentalConditions;
    Boolean performLivenessDocument;
    Boolean saveCapturedVideoFace;
    Boolean storeCapturedDocument;
    Boolean storeImageStream;

    String stepId;

    public ScanQr() {
    }

    public ScanQr(
            List<KycDocumentDetails> kycDocumentDetails,
            String apiKey,
            String language,
            ConfigModel configModel,
            EnvironmentalConditions environmentalConditions,
            Boolean performLivenessDocument,
            Boolean saveCapturedVideoFace,
            Boolean storeCapturedDocument,
            Boolean storeImageStream

    ) {
        this.language = language;
        this.apiKey = apiKey;
        this.kycDocumentDetails = kycDocumentDetails;
        this.configModel = configModel;
        this.environmentalConditions = environmentalConditions;
        this.qrScanner = BarcodeScanning.getClient();
        this.performLivenessDocument = performLivenessDocument;
        this.saveCapturedVideoFace = saveCapturedVideoFace;
        this.storeCapturedDocument = storeCapturedDocument;
        this.storeImageStream = storeImageStream;
    }

    public void setScanQrCallback(ScanQrCallback scanQrCallback) {
        this.scanQrCallback = scanQrCallback;
        try {
            remoteProcessing = new RemoteProcessing();
            remoteProcessing.setCallback(this);
        } catch (Exception e) {
        }
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
    }

    @Override
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {


        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  changeCardWeightLayout();
                }
            });
        }
        setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);

        if (this.start) {
            this.start = false;
            InputImage image = InputImage.fromBitmap(normalImage, 0);
            this.qrScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes != null && !barcodes.isEmpty()) {
                            Barcode barcode = barcodes.get(0);
                       if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scanQrCallback.onStartQrScan();
                                }
                            });
                        }
                            remoteProcessing.starQrProcessing(
                                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.QR),
                                    ImageUtils.convertBitmapToBase64(normalImage, BlockType.FACE_MATCH, getActivity()),
                                    configModel,
                                    kycDocumentDetails.get(0).getTemplateProcessingKeyInformation(),
                                    "ConnectionId",
                                    this.stepId,
                                    barcode.getRawValue()

                            );
                        }else {
                            this.start = true;
                        }
                    });
        }


    }

    @Override
    protected void onStopRecordVideo() {
        //
    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        Map<String, String> transformedProperties = new HashMap<>();
                        IDExtractedModel idExtractedModel = IDExtractedModel.Companion.fromQrJsonString(BaseResponseDataModel.getResponse(), transformedProperties);
                        idResponseModel = new IDResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                idExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );

                        if (Objects.equals(language, Language.NON)) {
                            scanQrCallback.onCompleteQrScan(idResponseModel);
                        } else {
                            LanguageTransformation translated = new LanguageTransformation(apiKey);
                            translated.setCallback(ScanQr.this);
                            translated.languageTransformation(
                                    language,
                                    preparePropertiesToTranslate(language, idExtractedModel.getOutputProperties())
                            );
                        }


                    } else {
                         start = true;
                         scanQrCallback.onErrorQrScan(BaseResponseDataModel.getError());
                    }

                }
            });
        }
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
                    if(key.contains(IdentificationDocumentCaptureKeys.name)){
                        nameKey = key;
                        nameWordCount = value.toString().trim().isEmpty() ? 0 : value.toString().trim().split("\\s+").length;
                    }
                    if(key.contains(IdentificationDocumentCaptureKeys.surname)){
                        surnameKey = key;
                    }
                }
        );

        idResponseModel.getIDExtractedModel().getTransformedProperties().clear();
        idResponseModel.getIDExtractedModel().getExtractedData().clear();
        properties.forEach((key, value) -> {

            if (key.equals(FullNameKey)) {
                if(!nameKey.isEmpty()){
                    idResponseModel.getIDExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(),nameWordCount));
                    idResponseModel.getIDExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(),nameWordCount));
                }
                if(!surnameKey.isEmpty()){
                    idResponseModel.getIDExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(),nameWordCount));
                    idResponseModel.getIDExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(),nameWordCount));
                }
            }else {
                idResponseModel.getIDExtractedModel().getTransformedProperties().put(key, value);
                String newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length())
                        .replace("_", " ");
                idResponseModel.getIDExtractedModel().getExtractedData().put(newKey, value);
            }

        });

        scanQrCallback.onCompleteQrScan(idResponseModel);
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        scanQrCallback.onCompleteQrScan(idResponseModel);
    }
}