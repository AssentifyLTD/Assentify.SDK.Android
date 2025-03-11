package com.assentify.sdk.FaceMatch;

import android.app.Activity;
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

        ExecutorService executor = Executors.newFixedThreadPool(livenessCheckArray.size());

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

}