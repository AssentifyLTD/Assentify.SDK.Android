package com.assentify.sdk.tflite;

import android.content.res.AssetManager;

import  com.assentify.sdk.Core.Constants.ConstantsValues;

import java.io.IOException;

public class DetectorFactory {
    public static YoloV5Classifier getDetector(
            final AssetManager assetManager)
            throws IOException {
        return YoloV5Classifier.create(assetManager, ConstantsValues.ModelFileName, ConstantsValues.LabelFileName, ConstantsValues.IsQuantized,
                ConstantsValues.InputSize);
    }

}
