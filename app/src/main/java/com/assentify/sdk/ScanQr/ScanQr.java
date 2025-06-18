package com.assentify.sdk.ScanQr;


import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
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
import java.util.List;


import kotlin.Pair;


public class ScanQr extends CameraPreview implements RemoteProcessingCallback {

    private IDResponseModel dataModel;

    private ScanQrCallback scanQrCallback;

    private boolean start = true;

    private RemoteProcessing remoteProcessing;

    private List<KycDocumentDetails> kycDocumentDetails = new ArrayList<>();
    private ConfigModel configModel;

    private BarcodeScanner qrScanner;

    private EnvironmentalConditions environmentalConditions;
    public ScanQr(
          /*  IDResponseModel dataModel,
            List<KycDocumentDetails> kycDocumentDetails,
            ConfigModel configModel*/
            EnvironmentalConditions environmentalConditions

    ) {
 /*       this.dataModel = dataModel;
        this.kycDocumentDetails = kycDocumentDetails;
        this.configModel = configModel;*/
        this.environmentalConditions = environmentalConditions;
        this.qrScanner = BarcodeScanning.getClient();
    }

    public void setScanQrCallback(ScanQrCallback scanQrCallback) {
        this.scanQrCallback = scanQrCallback;
        try {
            remoteProcessing = new RemoteProcessing();
            remoteProcessing.setCallback(this);
        } catch (Exception e) {
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
                            Log.e("IDSCAN", barcode.getRawValue());
                      /*  if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scanQrCallback.onStartQrScan();
                                }
                            });
                        }
                        remoteProcessing.starQrProcessing(
                                HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.QR),
                                "url", this.kycDocumentDetails.get(0).getTemplateProcessingKeyInformation(), this.configModel
                        );*/
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
                        //  scanQrCallback.onCompleteQrScan();
                    } else {
                        start = true;
                        //  scanQrCallback.onErrorQrScan();
                    }

                }
            });
        }
    }
}