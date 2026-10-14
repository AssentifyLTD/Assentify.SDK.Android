package com.assentify.sdk.ScanPassport;


import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.getIgnoredProperties;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.preparePropertiesToTranslate;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.FullNameKey;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getRemainingWords;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getSelectedWords;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CameraPreviewLocalMrz;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.EventsErrorMessages;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKeys;
import com.assentify.sdk.Core.Constants.Language;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.LanguageTransformation.LanguageTransformation;
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.RemoteClient.RemoteClient;
import com.assentify.sdk.logging.BugsnagObject;
import com.assentify.sdk.tflite.Classifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocalScanPassport extends CameraPreviewLocalMrz implements LanguageTransformationCallback {

    private static final String TAG = "AssentifyMRZ";

    /**
     * How many of the most recent captured frames are OCR'd. Each frame gives up to 3 votes
     * (zoomed MRZ, high-contrast MRZ, full page) and costs ~3 ML Kit passes (~100-300 ms each).
     */
    private static final int MAX_FRAMES_TO_SCAN = 5;

    /**
     * Votes per reading of the full-resolution still photo. Its MRZ characters are several times
     * larger than in the analysis frames, so it must not be outvoted by many lower-quality reads.
     */
    private static final int STILL_VOTE_WEIGHT = MAX_FRAMES_TO_SCAN;

    private final ExecutorService mrzExecutor = Executors.newSingleThreadExecutor();
    private final MrzBitmapScanner mrzScanner = new MrzBitmapScanner();


    private ScanPassportCallback scanPassportCallback;

    private EnvironmentalConditions environmentalConditions;


    private RectF rectFCard = new RectF();
    private double brightness;
    private MotionType motion = MotionType.NO_DETECT;
    private ZoomType zoom = ZoomType.NO_DETECT;
    private final List<Bitmap> highQualityBitmaps = new ArrayList<>();
    private Bitmap croppedBitmap;
    private List<RectF> motionRectF = new ArrayList<>();
    private List<MotionType> sendingFlagsMotion = new ArrayList<>();
    private List<ZoomType> sendingFlagsZoom = new ArrayList<>();
    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();

    ConfigModel configModel;
    String language;
    Integer stepId;

    private String readPassport = "ReadPassport";


    private PassportResponseModel passportResponseModel;

    private DetectIfRectFInsideTheScreen detectIfInsideTheScreen = new DetectIfRectFInsideTheScreen();
    private boolean isRectFInsideTheScreen = false;


    public LocalScanPassport() {
    }

    public LocalScanPassport(
            ConfigModel configModel,
            EnvironmentalConditions environmentalConditions, String apiKey,
            String language,
            Integer stepId
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.configModel = configModel;
        this.language = language;
        this.stepId = stepId;
        setIsPassport();
        setEnvironmentalConditions(environmentalConditions);
    }

    public void setScanPassportCallback(ScanPassportCallback scanPassportCallback) {
        this.scanPassportCallback = scanPassportCallback;
    }


    @Override
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {


        if (getActivity() != null) {
            BugsnagObject.INSTANCE.initialize(getActivity().getApplicationContext(), configModel);
        }


        this.results = results;
        if (hasFaceOrCard()) {
            synchronized (highQualityBitmaps) {
                highQualityBitmaps.add(normalImage);
                // Frames are high resolution now: keep only the ones that will be OCR'd
                while (highQualityBitmaps.size() > MAX_FRAMES_TO_SCAN) highQualityBitmaps.remove(0);
            }
            listScaleRectF.forEach((item) -> {
                if (item.component2().contains(ConstantsValues.CardName)) {
                    isRectFInsideTheScreen = detectIfInsideTheScreen.isRectFWithinMargins(item.component1(), previewWidth, previewHeight);
                }
            });
        }

        this.croppedBitmap = croppedBitmap;
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (result.getDetectedClass() == 0) {
                rectFCard = new RectF(location.left, location.top, location.right, location.bottom);
                motionRectF.add(rectFCard);
            }
        }
        if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && environmentalConditions.checkConditions(brightness, environmentalConditions) == BrightnessEvents.Good) {
            if (isRectFInsideTheScreen) {
                setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), true);
            } else {
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), true);
            }
        } else {
            setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), true);
        }
        checkEnvironment();


    }


    protected void checkEnvironment() {
        if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
            ImageBrightnessChecker imageBrightnessChecker = new ImageBrightnessChecker();
            DetectMotion detectMotion = new DetectMotion();
            DetectZoom detectZoom = new DetectZoom();
            brightness = imageBrightnessChecker.getAverageBrightness(croppedBitmap);
            if (motionRectF.size() >= 2) {
                if (results.isEmpty()) {
                    motionRectF.clear();
                    sendingFlagsMotion.clear();
                    sendingFlagsZoom.clear();
                    motion = MotionType.NO_DETECT;
                    zoom = ZoomType.NO_DETECT;
                } else {
                    motion = detectMotion.calculatePercentageChangePassport(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
                    zoom = detectZoom.calculatePercentageChangeWidth(motionRectF.get(motionRectF.size() - 1));
                    if (motion == MotionType.SENDING && zoom == ZoomType.SENDING) {
                        sendingFlagsMotion.add(MotionType.SENDING);
                        sendingFlagsZoom.add(ZoomType.SENDING);
                    } else {
                        sendingFlagsMotion.clear();
                        sendingFlagsZoom.clear();
                    }
                }
            }
            if (environmentalConditions.checkConditions(
                    brightness, environmentalConditions) == BrightnessEvents.Good && motion == MotionType.SENDING && zoom == ZoomType.SENDING && isRectFInsideTheScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (start) {
                        if (hasFaceOrCard()) {
                            stopRecording();
                        }
                    }
                }
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanPassportCallback.onEnvironmentalConditionsChange(
                            environmentalConditions.checkConditions(
                                    brightness, environmentalConditions),
                            sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT : sendingFlagsMotion.size() > environmentalConditions.getMotionPassportLimit() ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            zoom, isRectFInsideTheScreen);
                }
            });
        }


    }


    public boolean hasFaceOrCard() {
        return hasFaceAndCard();
    }

    public boolean hasFaceAndCard() {
        boolean hasFace = true;
        boolean hasCard = false;
        for (Classifier.Recognition item : results) {
            if (item.getDetectedClass() == 0 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasCard = true;
            }
        }
        return hasFace && hasCard;
    }

    @Override
    protected void onStopRecordVideo() {
        start = false;
        // The passport is detected and steady: take one full-resolution photo for the MRZ, then OCR
        // it together with the recent analysis frames (the callback fires once, also on failure)
        captureStillFrame(still -> extractMrzFromCapturedFrames(still));
    }

    /**
     * OCRs the most recent frames (not just until the first "valid" one) and combines them:
     * a single frame can pass every check digit and still be wrong, but OCR mistakes rarely
     * repeat in the same position across frames.
     */
    private void extractMrzFromCapturedFrames(@Nullable Bitmap still) {
        final List<Bitmap> frames;
        synchronized (highQualityBitmaps) {
            frames = new ArrayList<>(highQualityBitmaps);
            // Frames of this attempt only: a retry must not mix in old frames (or another passport)
            highQualityBitmaps.clear();
        }

        mrzExecutor.execute(() -> {
            List<Mrz.Result> readings = new ArrayList<>();
            List<Bitmap> readingFrames = new ArrayList<>();
            Mrz.Result bestAny = null;
            Bitmap bestAnyFrame = null;

            // Full-resolution still first (already upright), then the analysis frames (rotated)
            List<Bitmap> targets = new ArrayList<>();
            List<String> names = new ArrayList<>();
            if (still != null) {
                targets.add(still);
                names.add("Still " + still.getWidth() + "x" + still.getHeight());
            } else {
                Log.d(TAG, "No still photo: using analysis frames only");
            }
            int oldest = Math.max(0, frames.size() - MAX_FRAMES_TO_SCAN);
            for (int i = frames.size() - 1; i >= oldest; i--) {
                targets.add(null);              // rotated lazily below
                names.add("Frame " + i);
            }

            for (int t = 0; t < targets.size(); t++) {
                int i = frames.size() - 1 - (still != null ? t - 1 : t); // index into frames for analysis frames
                try {
                    Bitmap upright = targets.get(t) != null
                            ? targets.get(t)
                            : ImageUtils.rotateBitmap(frames.get(i), 90);
                    // Zoomed MRZ, high-contrast MRZ and full page: every pass is one vote
                    List<Mrz.Result> passes = mrzScanner.scanAllBlocking(upright);
                    for (int p = 0; p < passes.size(); p++) {
                        Mrz.Result r = passes.get(p);
                        Log.d(TAG, names.get(t) + " pass " + p + ": " + r.docType + ", " + r.failSummary());
                        if (r.bacReady()) {
                            boolean isStill = still != null && t == 0;
                            int weight = isStill ? STILL_VOTE_WEIGHT : 1;
                            for (int w = 0; w < weight; w++) {
                                readings.add(r);
                                readingFrames.add(upright);
                            }
                        }
                        if (bestAny == null || r.score() > bestAny.score()) {
                            bestAny = r;
                            bestAnyFrame = upright;
                        }
                    }
                } catch (Exception e) {
                    // One unreadable frame must not abort the scan: just skip it
                    Log.w(TAG, names.get(t) + " OCR failed: " + e);
                }
            }

            MrzConsensus.Outcome outcome = MrzConsensus.decide(readings);
            if (outcome != null) {
                Log.d(TAG, "MRZ consensus: " + outcome.summary);
                onMrzExtracted(outcome.result, readingFrames.get(outcome.frameIndex));
            } else {
                // No frame had valid document number + dates: keep the old behaviour (best single frame)
                Log.d(TAG, "MRZ consensus: no BAC-ready frame, best single frame used"
                        + (bestAny != null ? " (" + bestAny.failSummary() + ")" : ""));
                onMrzExtracted(bestAny, bestAnyFrame);
            }
        });
    }

    private void onMrzExtracted(@Nullable Mrz.Result result, Bitmap passportImage) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result != null) {
                    scanPassportCallback.onSend();
                    uploadImage(passportImage, result);
                } else {
                    start = true;
                    BaseResponseDataModel baseResponseDataModel = new BaseResponseDataModel(
                            readPassport,
                            null,
                            EventsErrorMessages.OnRetryCardMessage,
                            false,
                            "",
                            null
                    );
                    scanPassportCallback.onRetry(baseResponseDataModel);
                }

            }
        });

    }

    private void uploadImage(Bitmap bitmap, Mrz.Result mrzResult) {
        if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
            Pair<File, String> result = createTimestampedTempFile(bitmap);
            if (result == null) {
                buildData("", mrzResult);
                return;
            }
            File image = result.getFirst();
            String fileName = result.getSecond();

            RequestBody fileRequestBody = RequestBody.create(image, null);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("asset", fileName, fileRequestBody);

            String path;
            try {
                path = URLEncoder.encode(
                        configModel.getTenantIdentifier() + "/" +
                                configModel.getBlockIdentifier() + "/" +
                                configModel.getInstanceId() + "/" +
                                fileName,
                        "UTF-8"
                );
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            Call<ResponseBody> call = RemoteClient.INSTANCE.getRemoteBlobStorageService().uploadImageFile(
                    apiKey,
                    configModel.getTenantIdentifier(),
                    configModel.getBlockIdentifier(),
                    configModel.getInstanceId(),
                    "text/plain",
                    path,
                    filePart
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            try {
                                String responseBodyString = responseBody.string();
                                JSONObject jsonObject = new JSONObject(responseBodyString);
                                String uploadedUrl = jsonObject.getString("url");
                                buildData(uploadedUrl, mrzResult);
                            } catch (IOException | JSONException e) {
                                buildData("", mrzResult);
                            }
                        } else {
                            buildData("", mrzResult);
                        }
                    } else {
                        buildData("", mrzResult);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    buildData("", mrzResult);
                }
            });
        } else {
            buildData("", mrzResult);
        }
    }

    private void buildData(String imageUrl, Mrz.Result mrzResult) {
        StepDefinitions passportStep = configModel.getStepDefinitions().stream()
                .filter(stepDefinitions -> stepDefinitions.getStepId() == this.stepId)
                .findFirst().orElseThrow(() -> new IllegalStateException("No step definition found for stepId=" + this.stepId));

        Map<String, String> transformedProperties = new HashMap<>();
        try {
            PassportExtractedModel passportExtractedModel = PassportExtractedModel.Companion.fromOutputProperties(imageUrl, transformedProperties, passportStep.getOutputProperties(), mrzResult.toOutputProperties());
            passportResponseModel = new PassportResponseModel(
                    HubConnectionTargets.ON_COMPLETE,
                    passportExtractedModel,
                    "",
                    true
            );
            String rawExpiryDate = Objects.requireNonNull(
                            Objects.requireNonNull(passportResponseModel.getPassportExtractedModel())
                                    .getIdentificationDocumentCapture())
                    .getExpiryDate().toString();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            boolean expired = false;

            try {
                Date expiryDate = sdf.parse(rawExpiryDate);
                if (expiryDate != null) {
                    expired = expiryDate.before(new Date());
                }
            } catch (ParseException ignored) {
            }
            if (expired) {
                start = true;
                BaseResponseDataModel baseResponseDataModel = new BaseResponseDataModel(
                        readPassport,
                        null,
                        EventsErrorMessages.OnExpiredPassportMessage,
                        false,
                        "",
                        null
                );
                scanPassportCallback.onRetry(baseResponseDataModel);
            } else {
                start = false;
                if (Objects.equals(language, Language.NON)) {
                    scanPassportCallback.onComplete(passportResponseModel);
                } else {
                    LanguageTransformation translated = new LanguageTransformation(apiKey);
                    translated.setCallback(LocalScanPassport.this);
                    translated.languageTransformation(
                            language,
                            preparePropertiesToTranslate(language, passportExtractedModel.getOutputProperties())
                    );
                }
            }
        } catch (Exception e) {
            start = true;
            BaseResponseDataModel baseResponseDataModel = new BaseResponseDataModel(
                    readPassport,
                    null,
                    EventsErrorMessages.OnRetryCardMessage,
                    false,
                    "",
                    null
            );
            scanPassportCallback.onRetry(baseResponseDataModel);
        }

    }


    private Pair<File, String> createTimestampedTempFile(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "IMG_" + timeStamp + ".jpg";
            File tempFile = new File(getActivity().getCacheDir(), fileName);

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos); // 85% quality
                fos.flush();
            }

            return new Pair<>(tempFile, fileName);
        } catch (IOException e) {
            Log.w(TAG, "createTimestampedTempFile failed", e);
            return null;
        }
    }

    String nameKey = "";
    int nameWordCount = 0;
    String surnameKey = "";

    @Override
    public void onTranslatedSuccess(@Nullable Map<String, String> properties) {

        getIgnoredProperties(Objects.requireNonNull(passportResponseModel.getPassportExtractedModel().getOutputProperties())).forEach((key, value) -> {
            properties.put(key, value);
        });

        Objects.requireNonNull(passportResponseModel.getPassportExtractedModel().getOutputProperties()).forEach(
                (key, value) -> {
                    if (key.contains(IdentificationDocumentCaptureKeys.name)) {
                        nameKey = key;
                        nameWordCount = value.toString().trim().isEmpty() ? 0 : value.toString().trim().split("\\s+").length;
                    }
                    if (key.contains(IdentificationDocumentCaptureKeys.surname)) {
                        surnameKey = key;
                    }
                }
        );
        passportResponseModel.getPassportExtractedModel().getTransformedProperties().clear();
        passportResponseModel.getPassportExtractedModel().getExtractedData().clear();

        properties.forEach((key, value) -> {
            if (key.equals(FullNameKey)) {
                if (!nameKey.isEmpty()) {
                    passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(), nameWordCount));
                    passportResponseModel.getPassportExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(), nameWordCount));
                }
                if (!surnameKey.isEmpty()) {
                    passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(), nameWordCount));
                    passportResponseModel.getPassportExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(), nameWordCount));
                }
            } else {
                passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(key, value);
                String newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length())
                        .replace("_", " ");
                passportResponseModel.getPassportExtractedModel().getExtractedData().put(newKey, value);
            }
        });
        scanPassportCallback.onComplete(passportResponseModel);
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        scanPassportCallback.onComplete(passportResponseModel);
    }

    public void stopScanning() {
        closeCamera();
    }

    @Override
    public void onPause() {
        BugsnagObject.INSTANCE.pauseSession();
        super.onPause();
    }

    @Override
    public void onResume() {
        BugsnagObject.INSTANCE.resumeSession();
        super.onResume();
    }

    @Override
    public synchronized void onDestroy() {
        BugsnagObject.INSTANCE.pauseSession();
        mrzScanner.close();
        mrzExecutor.shutdown();
        super.onDestroy();
    }

}