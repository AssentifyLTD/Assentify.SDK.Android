package com.assentify.sdk.FaceMatch;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelImageProcessing {

    public List<String> processClipsToBase64InParallel(List<Bitmap> livenessCheckArray, Activity activity) {
        List<String> clips = new ArrayList<>();
        int threadCount = calculateThreadCount(activity.getApplicationContext(),livenessCheckArray.size());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<String>> futures = new ArrayList<>();

        for (Bitmap bitmap : livenessCheckArray) {
            Callable<String> task = () -> {
                return ImageUtils.convertClipsBitmapToBase64(bitmap, BlockType.FACE_MATCH, activity);
            };
            futures.add(executor.submit(task));
        }

        for (Future<String> future : futures) {
            try {
                clips.add(future.get());
            } catch (Exception e) {
                e.printStackTrace();
                clips.add(null);
            }
        }

        executor.shutdown();

        return clips;
    }

    private int calculateThreadCount(Context context, int maxThreads) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int safeThreads = Math.max(1, cpuCores / 2);

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean isLowRam = am != null && am.isLowRamDevice();

        return isLowRam ? 1 : Math.min(maxThreads, safeThreads);
    }

}