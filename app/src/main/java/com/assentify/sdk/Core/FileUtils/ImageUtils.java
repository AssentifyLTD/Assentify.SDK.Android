package com.assentify.sdk.Core.FileUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.gemalto.jp2.JP2Decoder;
import com.gemalto.jp2.JP2Encoder;

public class ImageUtils {


    /** **/

    public static Bitmap compressAndRotateBitmap(Bitmap inputImage, BlockType blockType, Context context) {
        Bitmap bitmap = inputImage;

        if (blockType == BlockType.FACE_MATCH) {
            bitmap = rotateBitmap(bitmap, 270);
        } else {
            bitmap = rotateBitmap(bitmap, 90);
        }

        return bitmap;
    }

    public static byte[] compressBitmapLossless(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG , 100, outputStream);
        return outputStream.toByteArray();
    }


    /** **/

    public static String convertBitmapToBase64(Bitmap inputImage, BlockType blockType, Context context) {
        Bitmap bitmap = inputImage;
        if (blockType == BlockType.FACE_MATCH) {
            bitmap = rotateBitmap(bitmap, 270);
        } else {
            bitmap = rotateBitmap(bitmap, 90);

        }

        return bitmapToBase64(bitmap,true,0);
    }


    public static String bitmapToBase64(Bitmap bitmap, boolean isLossless, int visualQuality) {
        try {
            byte[] jp2Data;
            if (isLossless) {
                jp2Data = new JP2Encoder(bitmap).encode();
            } else {
                jp2Data = new JP2Encoder(bitmap)
                        .setVisualQuality(visualQuality)
                        .encode();
            }
            return Base64.encodeToString(jp2Data, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertClipsBitmapToBase64(Bitmap inputImage, BlockType blockType, Context context) {
        Bitmap bitmap = inputImage;
       if (blockType == BlockType.FACE_MATCH) {
            bitmap = rotateBitmap(bitmap, 270);
        } else {
            bitmap = rotateBitmap(bitmap, 90);

        }
        return bitmapClipsToBase64(bitmap,false,40);
    }

    public static String bitmapClipsToBase64(Bitmap bitmap, boolean isLossless, int visualQuality) {
        try {
            byte[] jp2Data;
            if (isLossless) {
                jp2Data = new JP2Encoder(bitmap).encode();
            } else {
                jp2Data = new JP2Encoder(bitmap)
                        .setVisualQuality(visualQuality)
                        .encode();
            }
            return Base64.encodeToString(jp2Data, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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


    public static Bitmap resizeBitmap(Bitmap bitmap, int targetWidth) {
        float aspectRatio = (float)bitmap.getHeight() / bitmap.getWidth();
        int targetHeight = Math.round(targetWidth * aspectRatio);
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }


}
