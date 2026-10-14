package com.assentify.sdk.ScanPassport;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;


public final class MrzBitmapScanner {

    private static final String TAG = "AssentifyMRZ";

    /**
     * MRZ line height (px) the crop is enlarged to before the second OCR pass. ML Kit needs
     * characters of at least ~16 px and reads OCR-B (the MRZ font) far better around 40-50 px;
     * small characters are exactly where 5/S, D/0, I/1 and 8/B get confused.
     */
    private static final int TARGET_LINE_HEIGHT_PX = 48;
    private static final float MAX_UPSCALE = 4f;
    private static final int MAX_CROP_WIDTH_PX = 4096;

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
     * (Single full-image pass; use {@link #scanAllBlocking} for the more accurate zoomed passes.)
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
    // blocking API — call from a background thread only (never the main thread)
    // ---------------------------------------------------------------------

    /**
     * Every MRZ reading this frame can give (best quality first), for multi-frame voting:
     *  1. the MRZ crop, enlarged so each line is ~{@value #TARGET_LINE_HEIGHT_PX} px tall
     *  2. the same crop in high-contrast grayscale
     *  3. the full page (lowest resolution, kept as a fallback vote)
     * The printed document number from the full page is added to every pass, so the
     * visual-zone cross-check in {@link Mrz#bestCandidate} works on the zoomed readings too.
     * Must be called off the main thread.
     */
    @NonNull
    public List<Mrz.Result> scanAllBlocking(@NonNull Bitmap upright)
            throws ExecutionException, InterruptedException {
        List<Mrz.Result> out = new ArrayList<>();

        Text full = Tasks.await(recognizer.process(InputImage.fromBitmap(upright, 0)));
        String fullText = full.getText();
        String visualZone = visualZoneText(fullText);

        MrzLocation loc = locateMrz(full);
        if (loc != null) {
            Bitmap crop = cropAndEnlarge(upright, loc);
            if (crop != null) {
                Log.d(TAG, "Frame " + upright.getWidth() + "x" + upright.getHeight()
                        + ", MRZ line height " + loc.lineHeight + " px -> crop "
                        + crop.getWidth() + "x" + crop.getHeight());
                for (Bitmap variant : new Bitmap[]{crop, highContrastGray(crop)}) {
                    try {
                        Text t = Tasks.await(recognizer.process(InputImage.fromBitmap(variant, 0)));
                        Mrz.Result r = Mrz.bestCandidate(t.getText() + "\n" + visualZone);
                        if (r != null) out.add(r);
                    } catch (Exception e) {
                        Log.w(TAG, "Zoomed MRZ pass failed: " + e);
                    }
                }
            }
        } else {
            Log.d(TAG, "Frame " + upright.getWidth() + "x" + upright.getHeight() + ": MRZ not located on the page");
        }

        Mrz.Result fullResult = Mrz.bestCandidate(fullText);
        if (fullResult != null) out.add(fullResult);
        return out;
    }

    /**
     * Blocking version of {@link #scan}. Returns the best {@link Mrz.Result} of all passes, or
     * {@code null} when no MRZ-shaped lines were found. Must be called off the main thread.
     */
    @Nullable
    public Mrz.Result scanBlocking(@NonNull Bitmap bitmap, int rotationDegrees)
            throws ExecutionException, InterruptedException {
        Bitmap upright = rotationDegrees == 0 ? bitmap : rotate(bitmap, rotationDegrees);
        Mrz.Result best = null;
        for (Mrz.Result r : scanAllBlocking(upright)) best = Mrz.better(best, r);
        return best;
    }

    /** Same as {@link #scanBlocking(Bitmap, int)} assuming the bitmap is already upright. */
    @Nullable
    public Mrz.Result scanBlocking(@NonNull Bitmap bitmap) throws ExecutionException, InterruptedException {
        return scanBlocking(bitmap, 0);
    }

    // ---------------------------------------------------------------------
    // static one-shot convenience
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

    // ---------------------------------------------------------------------
    // MRZ location, crop, enhancement
    // ---------------------------------------------------------------------

    private static final class MrzLocation {
        final Rect box;
        final int lineHeight;

        MrzLocation(Rect box, int lineHeight) {
            this.box = box;
            this.lineHeight = lineHeight;
        }
    }

    /** Bounding box of the MRZ lines found by the full-page pass (long lines with "<<") */
    @Nullable
    private static MrzLocation locateMrz(Text text) {
        Rect box = null;
        List<Integer> heights = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect r = line.getBoundingBox();
                String cleaned = Mrz.clean(line.getText());
                if (r == null || cleaned.length() < 25 || !cleaned.contains("<<")) continue;
                if (box == null) box = new Rect(r);
                else box.union(r);
                heights.add(r.height());
            }
        }
        if (box == null || heights.isEmpty()) return null;
        Collections.sort(heights);
        return new MrzLocation(box, Math.max(1, heights.get(heights.size() / 2)));
    }

    /** MRZ area plus a margin (lines often get cut at the edges), enlarged to the target line height */
    @Nullable
    private static Bitmap cropAndEnlarge(Bitmap src, MrzLocation loc) {
        Rect b = loc.box;
        int padY = Math.max(loc.lineHeight, b.height() / 3);
        int padX = Math.max(loc.lineHeight, b.width() / 20);
        int left = Math.max(0, b.left - padX);
        int top = Math.max(0, b.top - padY);
        int right = Math.min(src.getWidth(), b.right + padX);
        int bottom = Math.min(src.getHeight(), b.bottom + padY);
        if (right - left < 20 || bottom - top < 10) return null;

        Bitmap crop = Bitmap.createBitmap(src, left, top, right - left, bottom - top);
        float scale = Math.min(MAX_UPSCALE, TARGET_LINE_HEIGHT_PX / (float) loc.lineHeight);
        if (crop.getWidth() * scale > MAX_CROP_WIDTH_PX) scale = MAX_CROP_WIDTH_PX / (float) crop.getWidth();
        if (scale <= 1.05f) return crop;
        return Bitmap.createScaledBitmap(crop,
                Math.round(crop.getWidth() * scale), Math.round(crop.getHeight() * scale), true);
    }

    /** Grayscale with stronger contrast: separates thin OCR-B strokes from the passport's background print */
    private static Bitmap highContrastGray(Bitmap src) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        float k = 1.6f;                    // contrast factor
        float t = 128f * (1f - k);         // keeps mid-gray in place
        matrix.postConcat(new ColorMatrix(new float[]{
                k, 0, 0, 0, t,
                0, k, 0, 0, t,
                0, 0, k, 0, t,
                0, 0, 0, 1, 0,
        }));
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    /** The page text without the MRZ lines: the printed document number lives here */
    private static String visualZoneText(String fullText) {
        StringBuilder sb = new StringBuilder();
        for (String line : fullText.split("\n")) {
            String cleaned = Mrz.clean(line);
            if (cleaned.indexOf('<') >= 0 || cleaned.length() >= 30) continue;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static Bitmap rotate(Bitmap src, int degrees) {
        Matrix m = new Matrix();
        m.postRotate(degrees);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    }
}