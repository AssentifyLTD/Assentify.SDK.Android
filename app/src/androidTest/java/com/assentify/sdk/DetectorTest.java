/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.assentify.sdk;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Size;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.assentify.sdk.tflite.Classifier;
import org.junit.Before;
import org.junit.runner.RunWith;
import com.assentify.sdk.Core.FileUtils.ImageUtils;

/** Golden test for Object Detection Reference app. */
@RunWith(AndroidJUnit4.class)
public class DetectorTest {

  private static final int MODEL_INPUT_SIZE = 300;
  private static final boolean IS_MODEL_QUANTIZED = true;
  private static final String MODEL_FILE = "detect.tflite";
  private static final String LABELS_FILE = "file:///android_asset/coco.txt";
  private static final Size IMAGE_SIZE = new Size(640, 480);

  private Classifier detector;
  private Bitmap croppedBitmap;
  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  @Before
  public void setUp() throws IOException {
    AssetManager assetManager =
        InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    detector =
        TFLiteObjectDetectionAPIModel.create(
            assetManager,
            MODEL_FILE,
            LABELS_FILE,
            MODEL_INPUT_SIZE,
            IS_MODEL_QUANTIZED);
    int cropSize = MODEL_INPUT_SIZE;
    int previewWidth = IMAGE_SIZE.getWidth();
    int previewHeight = IMAGE_SIZE.getHeight();
    int sensorOrientation = 0;
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, false);
    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);
  }


  // The format of result:
  // category bbox.left bbox.top bbox.right bbox.bottom confidence
  // ...
  // Example:
  // Apple 99 25 30 75 80 0.99
  // Banana 25 90 75 200 0.98
  // ...
  private static List<Classifier.Recognition> loadRecognitions(String fileName) throws Exception {
    AssetManager assetManager =
        InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    InputStream inputStream = assetManager.open(fileName);
    Scanner scanner = new Scanner(inputStream);
    List<Classifier.Recognition> result = new ArrayList<>();
    while (scanner.hasNext()) {
      String category = scanner.next();
      category = category.replace('_', ' ');
      if (!scanner.hasNextFloat()) {
        break;
      }
      float left = scanner.nextFloat();
      float top = scanner.nextFloat();
      float right = scanner.nextFloat();
      float bottom = scanner.nextFloat();
      RectF boundingBox = new RectF(left, top, right, bottom);
      float confidence = scanner.nextFloat();
        Classifier.Recognition recognition = new Classifier.Recognition(null, category, confidence, boundingBox);
        result.add(recognition);

    }
    return result;
  }
}
