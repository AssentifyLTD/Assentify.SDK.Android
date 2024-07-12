package com.assentify.sdk.Core.FileUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Base64;

import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

import kotlin.text.Regex;


public class ImageUtils {

    static final int kMaxChannelValue = 262143;


    public static String convertBitmapToBase64(Bitmap inputImage, BlockType blockType, Context context) {
        Bitmap bitmap = inputImage;
        if (blockType == BlockType.FACE_MATCH) {
            bitmap = rotateBitmap(bitmap, 270);
        } else {
            bitmap = rotateBitmap(bitmap, 90);

        }

        return bitmapToBase64(bitmap);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }




    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
            }

            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            matrix.postRotate(applyRotation);
        }

        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {

                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }





    public static Bitmap scaleBitmap(Bitmap src,int sensorOrientation) {
        Bitmap croppedBitmap = Bitmap.createBitmap(ConstantsValues.InputSize, ConstantsValues.InputSize, Bitmap.Config.ARGB_8888);
        Matrix frameToCropTransform = ImageUtils.getTransformationMatrix(
                src.getWidth(),
                src.getHeight(),
                ConstantsValues.InputSize, ConstantsValues.InputSize,
                sensorOrientation, false
        );
        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(src, frameToCropTransform, null);

        return croppedBitmap;
    }






    public static Bitmap mirrorBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1f, 1f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }



}
