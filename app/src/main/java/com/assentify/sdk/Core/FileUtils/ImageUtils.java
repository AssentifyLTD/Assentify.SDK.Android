package com.assentify.sdk.Core.FileUtils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import  com.assentify.sdk.Core.Constants.BlockType;
import  com.assentify.sdk.Core.Constants.ConstantsValues;
import android.os.Build;
import android.graphics.Bitmap;
import android.content.Context;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Base64;
import android.util.Log;

import com.assentify.sdk.Core.Constants.EnvironmentalConditions;

public class ImageUtils {


    /** Byte Array **/

    public static  byte[] convertBitmapToByteArray(Bitmap inputImage, BlockType blockType, Context context) {
        Bitmap bitmap = inputImage;
        if (blockType == BlockType.FACE_MATCH) {
            bitmap = rotateBitmap(bitmap, 270);
        } else {
            bitmap = rotateBitmap(bitmap, 90);

        }

        return bitmapToByteArray(bitmap,60,context);
    }


    public static byte[]  convertClipsBitmapByteArray(Bitmap inputImage, BlockType blockType, Context context) {
        Bitmap bitmap = inputImage;
        if (blockType == BlockType.FACE_MATCH) {
            bitmap = rotateBitmap(bitmap, 270);
        } else {
            bitmap = rotateBitmap(bitmap, 90);

        }

        return bitmapToByteArray(bitmap,60,context);
    }

   public static  byte[] bitmapToByteArray(Bitmap bitmap, int visualQuality,Context context) {
        try {
            // 1️⃣ Create temp file
            File outputFile = File.createTempFile("compressed_", ".jpg", context.getCacheDir());

            // 2️⃣ Compress bitmap to file
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, visualQuality, fos);
            fos.flush();
            fos.close();

            // 3️⃣ Add EXIF AFTER compress
            ExifInterface exif = new ExifInterface(outputFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER);
            exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL);
            exif.setAttribute(
                    ExifInterface.TAG_DATETIME,
                    new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(new Date())
            );
            exif.setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    String.valueOf(ExifInterface.ORIENTATION_NORMAL)
            );
            exif.saveAttributes();

            // 4️⃣ Read file back to byte[]
            byte[] result;
            try (FileInputStream fis = new FileInputStream(outputFile)) {
                result = new byte[(int) outputFile.length()];
                fis.read(result);
            }

            ExifInterface exifA = new ExifInterface(outputFile.getAbsolutePath());
            String make  = exifA.getAttribute(ExifInterface.TAG_MAKE);
            String model = exifA.getAttribute(ExifInterface.TAG_MODEL);

            // Optional: delete temp file
            outputFile.delete();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /****/

    public static byte[] base64ToByteArray(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return new byte[0];
        }
        if (base64String.contains(",")) {
            base64String = base64String.substring(base64String.indexOf(",") + 1);
        }
        return Base64.decode(base64String, Base64.DEFAULT);
    }



    /**  **/

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

    public static boolean isLowCapabilities(Context context, EnvironmentalConditions environmentalConditions) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalRamGB = memoryInfo.totalMem / (1024L * 1024L * 1024L);

        int cores = Runtime.getRuntime().availableProcessors();

        return (totalRamGB < environmentalConditions.getMinRam()) || (cores < environmentalConditions.getMinCPUCores());
    }

    public static Bitmap cropFromMiddle(Bitmap source, int cropWidth, int cropHeight) {
        if (source == null) return null;

        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        // Ensure crop size does not exceed the bitmap size
        cropWidth = Math.min(cropWidth, srcWidth);
        cropHeight = Math.min(cropHeight, srcHeight);

        // Calculate the top-left point (x, y) to start cropping from the center
        int x = (srcWidth - cropWidth) / 2;
        int y = (srcHeight - cropHeight) / 2;

        return Bitmap.createBitmap(source, x, y, cropWidth, cropHeight);
    }


}
