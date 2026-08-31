package com.assentify.sdk.ScanPassport;


import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;


public final class MrzBitmapScanner {

    private final TextRecognizer recognizer;

    public MrzBitmapScanner() {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    /**
     * Releases the underlying ML Kit recognizer. Call once when you're completely done scanning
     * with this instance (e.g. {@code Activity#onDestroy}) — not after every individual scan.
     */
    public void close() {
        recognizer.close();
    }

    // ---------------------------------------------------------------------
    // async API — callback fires on the main thread, safe to call from the UI thread
    // ---------------------------------------------------------------------

    public interface Callback {
        /** MRZ-shaped lines were found. Check allValid()/bacReady()/failSummary() before trusting it. */
        void onResult(@NonNull Mrz.Result result);
        /** OCR completed but no MRZ-shaped lines (>=20 usable chars each) were found in the image. */
        void onNotFound();
        /** The OCR step itself failed. */
        void onError(@NonNull Exception e);
    }

    /**
     * Runs OCR + MRZ parsing on {@code bitmap} and reports the outcome on {@code callback}.
     *
     * @param rotationDegrees clockwise rotation to apply before OCR so the MRZ text is upright
     *                        (0 if the bitmap is already correctly oriented; pass the image's
     *                        EXIF/sensor rotation otherwise — mirrors how MainActivity#analyze
     *                        rotates each live CameraX frame before running OCR on it)
     */
    public void scan(@NonNull Bitmap bitmap, int rotationDegrees, @NonNull Callback callback) {
        Bitmap upright = rotationDegrees == 0 ? bitmap : rotate(bitmap, rotationDegrees);
        InputImage image = InputImage.fromBitmap(upright, 0);
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    Mrz.Result r = Mrz.bestCandidate(text.getText());
                    if (r == null) callback.onNotFound();
                    else callback.onResult(r);
                })
                .addOnFailureListener(callback::onError);
    }

    /** Same as {@link #scan(Bitmap, int, Callback)} assuming the bitmap is already upright. */
    public void scan(@NonNull Bitmap bitmap, @NonNull Callback callback) {
        scan(bitmap, 0, callback);
    }

    // ---------------------------------------------------------------------
    // blocking API — call from a background thread only (never the main thread),
    // mirrors the Tasks.await(...) pattern MainActivity#analyze uses per frame
    // ---------------------------------------------------------------------

    /**
     * Blocking version of {@link #scan}. Returns the parsed {@link Mrz.Result}, or {@code null}
     * when OCR found no MRZ-shaped lines. Must be called off the main thread.
     */
    @Nullable
    public Mrz.Result scanBlocking(@NonNull Bitmap bitmap, int rotationDegrees)
            throws ExecutionException, InterruptedException {
        Bitmap upright = rotationDegrees == 0 ? bitmap : rotate(bitmap, rotationDegrees);
        InputImage image = InputImage.fromBitmap(upright, 0);
        Text text = Tasks.await(recognizer.process(image));
        return Mrz.bestCandidate(text.getText());
    }

    /** Same as {@link #scanBlocking(Bitmap, int)} assuming the bitmap is already upright. */
    @Nullable
    public Mrz.Result scanBlocking(@NonNull Bitmap bitmap) throws ExecutionException, InterruptedException {
        return scanBlocking(bitmap, 0);
    }

    // ---------------------------------------------------------------------
    // static one-shot convenience — spins up its own recognizer and tears it down;
    // prefer a long-lived instance (with your own close()) if you'll scan more than once
    // ---------------------------------------------------------------------

    /** Convenience for scanning exactly one bitmap. Must be called off the main thread. */
    @Nullable
    public static Mrz.Result scanOnce(@NonNull Bitmap bitmap, int rotationDegrees)
            throws ExecutionException, InterruptedException {
        MrzBitmapScanner scanner = new MrzBitmapScanner();
        try {
            return scanner.scanBlocking(bitmap, rotationDegrees);
        } finally {
            scanner.close();
        }
    }

    /** Same as {@link #scanOnce(Bitmap, int)} assuming the bitmap is already upright. */
    @Nullable
    public static Mrz.Result scanOnce(@NonNull Bitmap bitmap) throws ExecutionException, InterruptedException {
        return scanOnce(bitmap, 0);
    }

    private static Bitmap rotate(Bitmap src, int degrees) {
        Matrix m = new Matrix();
        m.postRotate(degrees);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    }
}