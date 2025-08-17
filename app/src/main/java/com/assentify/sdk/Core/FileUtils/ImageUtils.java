package com.assentify.sdk.Core.FileUtils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;

import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;
import java.io.ByteArrayOutputStream;
import com.gemalto.jp2.JP2Encoder;

public class ImageUtils {



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

    public static int getDynamicDelay(Context context) {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalRamMB = memoryInfo.totalMem / (1024L * 1024L);

        // Very low-end devices: ≤1 GB RAM or ≤2 CPU cores
        if (totalRamMB <= 1024 || cpuCores <= 2) {
            return 700;

            // Low / mid-tier devices: 2 GB RAM or ≤4 cores
        } else if (totalRamMB <= 2048 || cpuCores <= 4) {
            return 400;
         // Mid-range / decent devices: 4 GB RAM or ≤6 cores
        } else if (totalRamMB <= 4096 || cpuCores <= 6) {
            return 250;

        // High-end devices: >4 GB RAM and >6 cores
        } else {
            return 100;
        }
    }

    public static long getRecommendedDelayForFaceCheck(Context context) {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalRamMB = memoryInfo.totalMem / (1024L * 1024L);
        double ramGB = totalRamMB / 1024.0;

        // Performance score = cores × GB RAM
        double score = cpuCores * ramGB;

        // Map score to delay (step-based for simplicity)
        if (score >= 24) {
            return 400L;  // Flagship (8 cores × 3GB+)
        } else if (score >= 16) {
            return 600L;  // High-mid (4 cores × 4GB)
        } else if (score >= 8) {
            return 700L;  // Mid (4 cores × 2GB)
        } else if (score >= 4) {
            return 800L;  // Low (2 cores × 2GB)
        } else {
            return 800L;  // Very low end
        }
    }



}
