package com.assentify.sdk.ScanPassport;

import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomPassportLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getIDTag;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.getIgnoredProperties;
import static com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKt.preparePropertiesToTranslate;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.FullNameKey;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getRemainingWords;
import static com.assentify.sdk.Core.Constants.SupportedLanguageKt.getSelectedWords;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.Core.Constants.EventsErrorMessages;
import com.assentify.sdk.Core.Constants.LivenessConfig;
import com.assentify.sdk.Core.Constants.PassportLivenessInstruction;
import com.assentify.sdk.doccapture.Geometry;
import com.assentify.sdk.doccapture.Pose;
import com.assentify.sdk.logging.BugsnagObject;

import com.assentify.sdk.Core.Constants.BlockType;
import com.assentify.sdk.Core.Constants.BrightnessEvents;
import com.assentify.sdk.Core.Constants.ConstantsValues;
import com.assentify.sdk.Core.Constants.EnvironmentalConditions;
import com.assentify.sdk.Core.Constants.HubConnectionFunctions;
import com.assentify.sdk.Core.Constants.HubConnectionTargets;
import com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKeys;
import com.assentify.sdk.Core.Constants.Language;
import com.assentify.sdk.Core.Constants.MotionType;
import com.assentify.sdk.Core.Constants.RemoteProcessing;
import com.assentify.sdk.Core.Constants.ZoomType;
import com.assentify.sdk.Core.FileUtils.AssetsAudioPlayer;
import com.assentify.sdk.Core.FileUtils.ImageUtils;
import com.assentify.sdk.LanguageTransformation.LanguageTransformation;
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback;
import com.assentify.sdk.Models.BaseResponseDataModel;
import com.assentify.sdk.CheckEnvironment.DetectMotion;
import com.assentify.sdk.CheckEnvironment.ImageBrightnessChecker;
import com.assentify.sdk.ProcessingRHub.RemoteProcessingCallback;
import com.assentify.sdk.RemoteClient.Models.ConfigModel;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.tflite.Classifier;
import com.assentify.sdk.tflite.LivenessDetection.Finder.DocHead;
import com.assentify.sdk.tflite.LivenessDetection.Finder.Finder;
import com.assentify.sdk.tflite.LivenessDetection.Finder.FinderResult;
import com.assentify.sdk.tflite.LivenessDetection.Finder.LivenessGate;
import com.assentify.sdk.tflite.LivenessDetection.Finder.LivenessVision;
import com.assentify.sdk.tflite.LivenessDetection.Finder.OutlineStabilizer;
import com.assentify.sdk.tflite.LivenessDetection.Finder.SpreadFix;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class ScanPassport extends CameraPreview implements RemoteProcessingCallback, LanguageTransformationCallback {


    ///

    private ScanPassportCallback scanPassportCallback;
    private  EnvironmentalConditions environmentalConditions;

    private RectF rectFCard = new RectF();
    private double brightness;
    private MotionType motion = MotionType.NO_DETECT;
    private ZoomType zoom = ZoomType.NO_DETECT;
    private List<Bitmap> highQualityBitmaps = new ArrayList<>();
    private Bitmap croppedBitmap;
    private List<RectF> motionRectF = new ArrayList<>();
    private List<MotionType> sendingFlagsMotion = new ArrayList<>();
    private List<ZoomType> sendingFlagsZoom = new ArrayList<>();
    private RemoteProcessing remoteProcessing;
    private boolean start = true;
    private String apiKey = "";
    private List<? extends Classifier.Recognition> results = new ArrayList<>();

    Boolean processMrz;
    Boolean performLivenessDocument;
    Boolean saveCapturedVideo;
    Boolean storeCapturedDocument;
    ConfigModel configModel;
    String language;
    String stepId;

    private String readPassport = "ReadPassport";
    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private int videoCounter = -1;

    private PassportResponseModel passportResponseModel;

    private DetectIfRectFInsideTheScreen detectIfInsideTheScreen = new DetectIfRectFInsideTheScreen();
    private boolean isRectFInsideTheScreen = false;

    private AssetsAudioPlayer audioPlayer;

    int retryCount = 0;


    ///
    private Finder finder ;
    private final Object finderLock = new Object();      // shared by detect() and close()
    private boolean finderClosed = false;                // true once onDestroy closed the finder

    // ---- config (assets/rectifier_config_file.json) ----
    private LivenessConfig cfg;                                     // loaded on the first frame
    private String documentKind = LivenessConfig.KIND_OTHER_PASSPORT;   // lb_passport or other_passport (default)

    // thresholds, copied from the config in ensureConfig()
    private double PASSPORT_ASPECT = 1.4204545454545454;           // profiles[kind].aspect
    private OutlineStabilizer stab = new OutlineStabilizer(PASSPORT_ASPECT);   // re-created on config + restart
    private int RECT_H = 338;                                       // straightened page: 480 x round(480 / aspect)
    private double FRAME_MARGIN_PX = 4;                            // frameMarginPx
    private double MIN_QUAD_AREA_FRAC = 0.08;                      // minQuadAreaFrac
    private double FRONTAL_MAX = 0.16;                             // frontalMax
    private double SHAPE_TOL = 0.1;                                // shapeTol
    private long STABLE_MS = 300;                                  // stableMs
    private int ANALYSIS_LONG_EDGE = 720;                          // analysisLongEdge
    private long LOSS_MS = 1500;                                   // lossMs
    private long PHASE_TIMEOUT_MS = 15000;                         // phaseTimeoutMs
    private int MAX_ATTEMPTS = 3;                                  // maxAttempts
    private int MAX_RESTARTS = 8;                                  // maxRestarts (from the tilt on)
    private int MAX_EARLY_RESTARTS = 40;                           // maxEarlyRestarts (position / baseline / light)
    private long BASELINE_HOLD_MS = 400;                           // baselineHoldMs
    private double SHARPNESS_MIN = 60;                             // sharpnessMin (Laplacian variance at analysis size)
    private double GLARE_MAX = 0.04;                               // glareFracMax
    private double JITTER_MAX = 0.015;                             // jitterMax (steady: pose change per frame)
    private double DRIFT_MAX = 0.03;                               // driftMax (steady: centre move per frame)
    private long LIGHT_SETTLE_MS = 300;                            // light.settleMs (counted from the request)
    private long LIGHT_MAX_MS = 2500;                              // light.maxMs
    private int LIGHT_FRAMES = 5;                                  // light.frames
    private long OFF_SETTLE_MS = 500;                              // light.offSettleMs (light off -> photo allowed)
    private double BATTERY_LOW = 0.05;                             // batteryLowBelow
    private double TILT_MIN = 0.08;                                // upDown.tiltMin (passport = bottom)
    private int SWEEP_FRAMES = 6;                                  // upDown.holdFrames
    private int WRONG_WAY_FRAMES = 4;                              // wrongWayFrames
    private long STUCK_MS = 3000;                                  // stuckMs
    private long TILT_HOLD_MS = 4000;                              // tiltHoldMs (tilt kept open until the colours are strong)
    private double TILT_NOISE_K = 3;                               // tiltNoiseK
    private double SAME_PLACE_MAX = 0.08;                          // samePlaceMax
    private double AREA_JUMP_MAX = 1.6;                            // areaJumpMax

    // native-only (not in the config)
    private static final int BASELINE_MIN_POSES = 3;               // spec: ">= 3 poses"
    // native: our outline comes from the finder on every frame (no point tracking), so it jitters more than the web's
    // followed outline; frame-to-frame jitterMax (0.015) reset the hold on most frames. The hold is instead judged
    // against the pose where it started (the rule that worked on device), plus driftMax per frame for the centre.
    private static final double HOLD_STILL_TOL = 0.05;
    // native: the flat hold accepts a pose up to this (spec frontalMax 0.16). The tilt is measured as a CHANGE from this
    // pose, so slack here costs the tilt nothing (INSIGHTS, frontalMax); device log: a still hand read lr -0.17 .. -0.23.
    private static final double HOLD_FRONTAL_MAX = 0.25;
    // native: glare allowed on the flat photo (spec glareFracMax 0.04); device log: 0.040 .. 0.046 kept resetting the hold
    private static final double HOLD_GLARE_MAX = 0.06;
    private static final double LIGHT_LOSS_FACTOR = 2.5;           // spec: loss grace x2.5 during the light step
    private static final long TORCH_CONFIRM_MS = 1000;             // the torch must report ON within this time
    private static final double RAW_LR_MAX = 0.45;                 // raw lr beyond this = broken outline (log: 0.66, -0.78)
    private static final double RAW_TB_MAX = 0.80;                 // tb can be large: that IS the lift
    private static final double MOVE_EPS = 0.01;                   // pose change that counts as movement
    private static final int BOTTOM_LIFT_SIGN = -1;                // lifting the bottom makes tb go NEGATIVE (log + spec sign)
    private static final double SWEEP_FLAT_EPS = 0.04;             // |delta| below this = still flat
    private static final double RAW_PAGE_ASPECT_MIN = 1.0;         // raw outline: >= 1.0 photo page, < 1.0 whole booklet

    // ---- orientation check (normal only: not upside down, not MRZ left / right) ----
    private static final double MAX_ROLL_DEG = 30;     // max in-plane rotation of the page
    private static final int WARP_W = 400;              // straightened page size
    private static final int WARP_H = 282;              // 400 / 1.42
    private static final int CELL = 16;                 // row is split into 400/16 = 25 cells
    private static final int EDGE_MIN = 40;             // brightness jump that counts as a text edge (calibrate)
    private static final double MRZ_ROW_FILL = 0.60;    // row is "MRZ-like" if >= 60% of its cells have text
    private static final double MRZ_MIN = 0.06;         // log: correct passports at 0.07-0.14 were rejected with 0.12
    private static final double MRZ_MARGIN = 2.0;       // MRZ side must score 2x the opposite side

    // ---- this frame (set by checkDocument) ----
    private Bitmap lastUpright;
    private double[] lastQuad;             // outline used for the pose (stabilised photo page, or raw in the sweep)
    private double[] lastPixelQuad;        // unsmoothed PHOTO-PAGE outline used for pixel checks (null if unknown)
    private Pose lastPose;
    private Pose lastRawPose;              // pose of the raw finder outline
    private double[] lastRawQuad;          // the raw finder outline itself
    private boolean lastIsRaw;             // true = only the raw outline (SpreadFix / stabilizer failed)
    private boolean lastFlat;
    private boolean lastBothPages;
    private boolean lastSharp, lastGlare;  // checks/quality.md
    private double lastSharpness, lastGlareFrac;
    private boolean tiltStart = false;
    private long steadySince = 0;
    private volatile boolean pendingReset = false;
    // native (spec: one odd outline is passed over): Position and the flat hold restart only after TWO bad frames
    // in a row. Screen recording: at ~5 fps one misplaced corner (a thumb on the page edge) restarted them every time.
    private int positionMisses = 0;
    private boolean holdGraceUsed = false;
    private String orientWhy = "";                  // debug: why the orientation check failed

    // ---- phases (rc17: ends on the tilt, endOnTilt = true) ----
    private enum Phase { POSITION, BASELINE, LIGHT, SWEEP, DONE }
    private Phase phase = Phase.POSITION;
    private long phaseStart = 0;
    private long lastSeen = 0;
    private boolean bugsnagStarted = false;

    // ---- per try: budgets and reasons (challenge-engine) ----
    private int engineAttempts = 0, restarts = 0, earlyRestarts = 0;
    private final List<String> tryReasons = new ArrayList<>();

    // ---- per visit (survive a retry of the same document) ----
    private int retriesThisVisit = 0;
    private boolean matteBefore = false;   // the try before ended on a "_first" reason (two misses in a row)

    // ---- baseline ----
    private int baseCount = 0;
    private long baseHoldSince = 0;
    private boolean havePrev = false;
    private double prevLr, prevTb, prevCx, prevCy;
    private double holdLr, holdTb;                                 // pose where the current flat hold started
    private final List<Double> baselineRawTbPage = new ArrayList<>();
    private final List<Double> baselineRawTbSpread = new ArrayList<>();
    private Bitmap flatPhoto = null;       // THE photo: flat and unlit, taken when the baseline ends (output-contract)

    // ---- light ----
    private int baseK = 0;                 // which corner is the document's top-left (makeBaseline)
    private int[] unlitImg, litImg;        // straightened references: before the light / with the light
    private LivenessVision.IridBase iridBase;
    private boolean lightAvailable = false, lightEnded = false;
    private long lightRequestedAt = 0;
    // light off is CONFIRMED (camera-torch.md): asked off, re-asked every 250 ms up to light.offMaxMs, then offSettleMs
    private enum Light { OFF, ON, GOING_OFF }
    private Light lightState = Light.OFF;
    private long offAskedAt = 0, offLastAskAt = 0, darkAt = -1_000_000L;
    private long OFF_MAX_MS = 4000;                                // light.offMaxMs
    private static final long OFF_REASK_MS = 250;
    private int lightSharp = 0;
    private LivenessGate.ReflectionAnalyzer reflection;
    private final List<Double> lightRawTbPage = new ArrayList<>();
    private final List<Double> lightRawTbSpread = new ArrayList<>();
    private double baseRawTbPage = Double.NaN, baseRawTbSpread = Double.NaN;
    private double tiltMinEffective = 0.08;

    // ---- sweep ----
    private double ANGLE_MAX = 0.17;                               // angleMax: an in-plane turn over this (rad) is not a tilt
    private double TILT_OVERFLOW = 0.15;                           // tiltOverflow: corners may leave the picture by this x width
    private double EDGE_MARGIN = 0.02;                             // edgeMargin: "Hold it there" near the picture's edge
    private double baseAngle = Double.NaN;                         // bottom-edge angle while held flat (baseline)
    private final List<Double> baselineAngles = new ArrayList<>();
    private double sweepFromPage = Double.NaN, sweepFromSpread = Double.NaN;   // where the tilt was asked (wrong-way judge)
    private double sweepMoved = 0;                                 // the largest |tilt| seen (pick_up)
    private final List<double[]> iridTilt = new ArrayList<>();    // laminate colours per tilted frame
    private int sweepGood = 0, sweepWrong = 0;
    private double prevDelta = 0, tiltPeak = 0;
    private long lastMoveAt = 0, reachedAt = 0;
    private boolean sweepSawFlat = false;
    private boolean sweepRefSet = false;                           // anti-swap after a loss
    private long sweepLastSeenAt = 0;
    private double sweepRefX, sweepRefY, sweepRefLen;

    // ---- result ----
    private boolean finalReady = false;                            // processImage sends the flat photo
    private volatile Bitmap captureBitmap = null;

    // ---- UI ----
    private PassportLivenessInstruction shownInstruction = null;
    private int livenessProgress = 0;                              // tilt meter 0..100 sent with the instruction
    private int shownProgress = -1;
    private boolean livenessFrameGood = false;                     // this frame meets the current phase's requirement

    // ---- hand: the passport held in the hand, not lying on a table (checks/hand-and-sun-pauses.md) ----
    private double FLAT_DEG = 10;                                  // hands.flatDeg: phone within this of level = flat
    private long FLAT_MS = 800;                                    // hands.flatMs
    private int MAX_HAND_PAUSES = 1;                               // hands.maxPauses per document kind per visit
    private boolean TABLE_CARD = true;                             // hands.tableCard (false = record only)
    private static final long MOTION_FRESH_MS = 1000;              // older readings = unknown (no pause)
    private double TURN_DEG = 20;                                  // hands.turnDeg: the PHONE turned in the tilt
    private SensorManager sensorManager;
    private volatile double phoneTiltDeg = Double.NaN;             // 0 = phone lying flat, 90 = upright
    private volatile long phoneTiltAt = 0;
    private volatile double turnedTotal = 0;                       // degrees the phone turned (gyroscope x, y), ever
    private double turnedMark = 0;                                 // turnedTotal when the tilt started / a pause ended
    // native: the phone turn is the NET angle between the gravity direction now and when the tilt started. The summed
    // gyroscope turn adds up hand tremor (screen recording: an 8 s tilt reached 20 deg without the phone being turned).
    private volatile double gravX = Double.NaN, gravY = Double.NaN, gravZ = Double.NaN;
    private double markX = Double.NaN, markY = Double.NaN, markZ = Double.NaN;
    private long lastGyroNs = 0;
    private long flatSince = 0;
    private int handPauses = 0;                                    // per visit, table and phone together
    private final List<String> flags = new ArrayList<>();          // flag_on_table / flag_phone_tilted (recorded)
    private final SensorEventListener gyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            // spec: turned += hypot(x, y) in deg/s x dt, dt capped at 0.2 s, 0 on the first event; z (in-plane) ignored
            double dt = lastGyroNs == 0 ? 0 : Math.min(0.2, (e.timestamp - lastGyroNs) / 1e9);
            lastGyroNs = e.timestamp;
            turnedTotal += Math.toDegrees(Math.hypot(e.values[0], e.values[1])) * dt;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    // ---- pauses (hand-and-sun-pauses.md) ----
    // HOLD_IN_HAND (on a table), PHONE_TURNED (the phone tilted, not the passport), TOO_BRIGHT (sun).
    // While shown every frame is dropped; the try has already restarted; it continues by itself (pauseDone).
    private volatile PassportLivenessInstruction pause = null;
    private long pauseAt = 0;
    // native-only: the web waits for a Continue tap; here the SDK continues by itself
    private static final long PAUSE_SHOW_MS = 3000;                // a card stays at least this long (time to read it)
    private static final long PAUSE_MAX_MS = 8000;                 // and continues at the latest after this
    private int sunPauses = 0;                                     // per visit
    private boolean brightMiss = false;                            // last try missed with the light barely reaching the page
    private double BRIGHT_LIFT = 1.25;                             // reflection.brightLift
    private int SUN_MAX = 3;                                       // reflection.sunMax

    // ---- stall: no analysed frame for stallMs ends the capture (camera-torch.md) ----
    private long STALL_MS = 6000;                                  // stallMs
    private volatile long lastFrameAt = 0;
    private final Handler watchdog = new Handler(Looper.getMainLooper());
    private final Runnable stallCheck = new Runnable() {
        @Override
        public void run() {
            checkStall();
            watchdog.postDelayed(this, 1000);
        }
    };

    // ---- photo sent (sendPhoto) ----
    private int SEND_MAX_SIDE = 1920;                              // sendPhoto.maxSide (long side, px)
    private final SensorEventListener gravityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            double gx = e.values[0], gy = e.values[1], gz = e.values[2];
            double g = Math.sqrt(gx * gx + gy * gy + gz * gz);
            if (g <= 4) return;                                     // spec: only when |g| > 4 m/s^2
            phoneTiltDeg = Math.toDegrees(Math.acos(Math.min(1, Math.abs(gz) / g)));
            phoneTiltAt = SystemClock.elapsedRealtime();
            gravX = gx / g;
            gravY = gy / g;
            gravZ = gz / g;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    // ---- debug ----
    private static final boolean DEBUG_LOGS = false;                // TEMP: set false for release

    public ScanPassport() {
    }

    public ScanPassport(
            ConfigModel configModel,
            EnvironmentalConditions environmentalConditions, String apiKey,
            String language
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.configModel = configModel;
        this.language = language;
        setIsPassport();
        setEnvironmentalConditions(this.environmentalConditions);
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
        if(this.stepId==null){
            long stepsCount = this.configModel.getStepDefinitions().stream()
                    .filter(item -> item.getStepDefinition().equals("IdentificationDocumentCapture"))
                    .count();

            if(stepsCount==1){
                for (StepDefinitions item : this.configModel.getStepDefinitions()) {
                    if (item.getStepDefinition().equals("IdentificationDocumentCapture")) {
                        this.stepId = String.valueOf(item.getStepId());
                        break;
                    }
                }
            }else {
                if(this.stepId==null){
                    throw new IllegalArgumentException("Step ID is required because multiple 'Identification Document Capture' steps are present.");
                }
            }
        }

        for (StepDefinitions item : configModel.getStepDefinitions()) {
            if (Integer.parseInt(this.stepId) == item.getStepId()) {
                if (performLivenessDocument == null) {
                    performLivenessDocument = item.getCustomization().getDocumentLiveness();
                }
                if (processMrz == null) {
                    processMrz = item.getCustomization().getProcessMrz();
                }
                if (storeCapturedDocument == null) {
                    storeCapturedDocument = item.getCustomization().getStoreCapturedDocument();
                }
                if (saveCapturedVideo == null) {
                    saveCapturedVideo = item.getCustomization().getSaveCapturedVideo();
                }


            }
        }


    }


    public void setScanPassportCallback(ScanPassportCallback scanPassportCallback) {
        this.scanPassportCallback = scanPassportCallback;
        try {
            remoteProcessing = new RemoteProcessing();
            remoteProcessing.setCallback(this);
        } catch (Exception e) {
        }

    }


    @Override
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {


        if (!bugsnagStarted && getActivity() != null) {             // once, not every frame
            BugsnagObject.INSTANCE.initialize(getActivity().getApplicationContext(),configModel);
            bugsnagStarted = true;
        }

        boolean configReady = ensureConfig();                       // thresholds from assets/rectifier_config_file.json

        if (pendingReset) {                                         // set by onMessageReceived after a retry
            pendingReset = false;
            resetLiveness();
        }


        if (audioPlayer == null) {
            if (getActivity() != null) {
                audioPlayer = new AssetsAudioPlayer(getActivity());
            }
        }

        this.results = results;

        boolean livenessOn = Boolean.TRUE.equals(performLivenessDocument) && configReady;

        boolean accepted = false;
        if (livenessOn) {
            synchronized (finderLock) {
                if (!finderClosed) {
                    if (finder == null && getActivity() != null) {
                        finder = new Finder(getActivity(), ConstantsValues.FinderFileName, 4);
                    }
                    if (finder != null) {
                        accepted = checkDocument(normalImage);
                    }
                }
            }
        }

        if(accepted){
            highQualityBitmaps.clear();
            highQualityBitmaps.add(normalImage);
        }else {
            if (hasFaceOrCard()) {
                highQualityBitmaps.clear();                             // keep only the latest frame (was a memory leak)
                highQualityBitmaps.add(normalImage);
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
                    if (motionRectF.size() > 10) motionRectF.remove(0);   // only the last frames are used
                }
            }
            if (!livenessOn) {                                          // liveness mode: updateLivenessColor() owns the colour
                if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && environmentalConditions.checkConditions(brightness,environmentalConditions) == BrightnessEvents.Good) {
                    if (isRectFInsideTheScreen) {
                        setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(),start);
                    }else {
                        setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
                    }
                } else {
                    setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start );
                }
            }

        }


        if (livenessOn) {
            lastFrameAt = SystemClock.elapsedRealtime();            // stall watchdog
            pollLight(lastFrameAt);                                 // confirm the light going off
            if (start) {
                runLiveness(accepted, normalImage);
                if (finalReady) {                                   // verdict "continue": send the flat, unlit photo
                    finalReady = false;
                    captureBitmap = flatPhoto;
                    stopRecording();
                }
            }
            updateLivenessColor();
        } else {
            checkEnvironment();
        }


    }


    protected void checkEnvironment() {
        if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (hasFaceOrCard() && start) {
                    startRecording();
                }
            }
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
                    brightness,environmentalConditions)== BrightnessEvents.Good && motion == MotionType.SENDING && zoom == ZoomType.SENDING && isRectFInsideTheScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (start && highQualityBitmaps.size() != 0 && sendingFlagsMotion.size() > environmentalConditions.getMotionPassportLimit() && sendingFlagsZoom.size() > ZoomPassportLimit) {
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
                                    brightness,environmentalConditions),
                            sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT : sendingFlagsMotion.size() > environmentalConditions.getMotionPassportLimit() ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,
                            zoom,isRectFInsideTheScreen);
                }
            });
        }


    }

    @Override
    public void onMessageReceived(@NonNull String eventName, @NonNull BaseResponseDataModel BaseResponseDataModel) {

        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    highQualityBitmaps.clear();
                    motionRectF.clear();
                    sendingFlagsMotion.clear();
                    sendingFlagsZoom.clear();
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {

                        Map<String, String> transformedProperties = new HashMap<>();
                        PassportExtractedModel passportExtractedModel = PassportExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties);
                        passportResponseModel = new PassportResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                passportExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );
                        Boolean expired = (Boolean) Objects.requireNonNull(Objects.requireNonNull(passportResponseModel
                                                .getPassportExtractedModel())
                                        .getIdentificationDocumentCapture())
                                .isExpired();
                        if (Boolean.TRUE.equals(expired)) {
                            retryCount++;
                            start = true;
                            BaseResponseDataModel.setError(EventsErrorMessages.OnExpiredPassportMessage);
                            scanPassportCallback.onRetry(BaseResponseDataModel);
                        }else {
                            start = false;
                            if (Objects.equals(language, Language.NON)) {
                                scanPassportCallback.onComplete(passportResponseModel);
                            } else {
                                LanguageTransformation translated = new LanguageTransformation(apiKey);
                                translated.setCallback(ScanPassport.this);
                                translated.languageTransformation(
                                        language,
                                        preparePropertiesToTranslate(language, passportExtractedModel.getOutputProperties())
                                );
                            }
                        }

                    } else if (eventName.equals(HubConnectionTargets.ON_RETRY)  ) {
                        retryCount++;
                        start = true;
                        BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                        scanPassportCallback.onRetry(BaseResponseDataModel);

                    } else  {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE) || eventName.equals(HubConnectionTargets.ON_WRONG_TEMPLATE) ;
                        switch (eventName) {
                            case HubConnectionTargets.ON_WRONG_TEMPLATE:
                                scanPassportCallback.onWrongTemplate(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_LIVENESS_UPDATE:
                                scanPassportCallback.onLivenessUpdate(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_ERROR:
                                scanPassportCallback.onError(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                                scanPassportCallback.onClipPreparationComplete(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_STATUS_UPDATE:
                                scanPassportCallback.onStatusUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPDATE:
                                scanPassportCallback.onUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CARD_DETECTED:
                                scanPassportCallback.onCardDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_EXTRACTED:
                                scanPassportCallback.onMrzExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_DETECTED:
                                scanPassportCallback.onMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                                scanPassportCallback.onNoMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_DETECTED:
                                scanPassportCallback.onFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_FACE_DETECTED:
                                scanPassportCallback.onNoFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_EXTRACTED:
                                scanPassportCallback.onFaceExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                                scanPassportCallback.onQualityCheckAvailable(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                                scanPassportCallback.onDocumentCaptured(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                                scanPassportCallback.onDocumentCropped(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPLOAD_FAILED:
                                scanPassportCallback.onUploadFailed(BaseResponseDataModel);
                                break;
                            default:
                                start = true;
                                BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                                scanPassportCallback.onRetry(BaseResponseDataModel);
                                break;

                        }
                    }

                    if (start) pendingReset = true;                 // retry -> liveness starts again from Position

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
            /* if (item.getDetectedClass() == 1 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasFace = true;
            }*/
            if (item.getDetectedClass() == 0 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasCard = true;
            }
        }
        return hasFace && hasCard;
    }

    // TODO Later
    @Override
    protected void onStopRecordVideo() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanPassportCallback.onSend();
                }
            });
        }
        start = false;
        videoCounter = videoCounter + 1;
        final Bitmap picked = captureBitmap != null
                ? captureBitmap
                : (highQualityBitmaps.isEmpty() ? null : highQualityBitmaps.get(highQualityBitmaps.size() - 1));
        if (picked == null) {
            start = true;                                           // nothing to send: keep scanning
            return;
        }
        createBase64.execute(() -> {
            // sendPhoto: the long side at most sendPhoto.maxSide px (the liveness already played its done tick)
            final Bitmap toSend = fitLongSide(picked, SEND_MAX_SIDE);
            if (captureBitmap == null) audioPlayer.playAudio(ConstantsValues.AudioCardSuccess);   // old flow only
            remoteProcessing.starProcessingIDs(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.READ_PASSPORT),
                    ImageUtils.convertBitmapToByteArray(toSend, BlockType.READ_PASSPORT, getActivity()),
                    configModel,
                    new ArrayList<>(),
                    "ConnectionId",
                    getVideoPath(configModel, readPassport, 0),
                    true,
                    processMrz,
                    performLivenessDocument,
                    saveCapturedVideo,
                    storeCapturedDocument,
                    true,
                    stepId,
                    false,
                    true,
                    retryCount,
                    getIDTag(configModel,readPassport),
                    false
            );
        });

        // remoteProcessing.uploadVideo(videoCounter, video, configModel, readPassport);
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
                    if(key.contains(IdentificationDocumentCaptureKeys.name)){
                        nameKey = key;
                        nameWordCount = value.toString().trim().isEmpty() ? 0 : value.toString().trim().split("\\s+").length;
                    }
                    if(key.contains(IdentificationDocumentCaptureKeys.surname)){
                        surnameKey = key;
                    }
                }
        );
        passportResponseModel.getPassportExtractedModel().getTransformedProperties().clear();
        passportResponseModel.getPassportExtractedModel().getExtractedData().clear();

        properties.forEach((key, value) -> {
            if (key.equals(FullNameKey)) {
                if(!nameKey.isEmpty()){
                    passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(),nameWordCount));
                    passportResponseModel.getPassportExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(),nameWordCount));
                }
                if(!surnameKey.isEmpty()){
                    passportResponseModel.getPassportExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(),nameWordCount));
                    passportResponseModel.getPassportExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(),nameWordCount));
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
        stopMotion();
        watchdog.removeCallbacks(stallCheck);
        interrupt();                                                // the app went to the background
        super.onPause();
    }

    @Override
    public void onResume() {
        BugsnagObject.INSTANCE.resumeSession();
        super.onResume();
        startMotion();
        lastFrameAt = 0;                                            // the stall clock starts with the first frame
        watchdog.removeCallbacks(stallCheck);
        watchdog.postDelayed(stallCheck, 1000);
    }

    @Override
    public void onDestroy() {                            // FIX 2: no "synchronized" on the method
        BugsnagObject.INSTANCE.pauseSession();
        super.onDestroy();
        this.createBase64.shutdown();
        if (audioPlayer != null) {
            audioPlayer.stopAudio();
        }
        synchronized (finderLock) {                      // FIX 2: waits for a frame inside detect() to finish
            finderClosed = true;
            if (finder != null) {
                finder.close();
                finder = null;
            }
        }

    }

    @Override
    public void onUploadProgress(int progress) {
        scanPassportCallback.onUploadingProgress(progress);

    }


    // #############################################################################################
    // #############################################################################################
    //
    //                                  Local Liveness Check
    //
    //   rc17 spec (mobile-handover/02-core-spec): POSITION -> BASELINE (flat photo) -> LIGHT -> SWEEP -> verdict.
    //   The capture ends on the tilt (endOnTilt); the photo is the flat, unlit frame of the baseline.
    //   Checks: quality, reflection (enforce), laminate colours (enforce for lb_passport), decide().
    //
    // #############################################################################################
    // #############################################################################################

    /**
     * The passport kind: "lb_passport" (or "lb") -> the Lebanese passport rules; anything else, null included ->
     * other_passport. Without a call the default is other_passport.
     */
    public void setDocumentKind(@Nullable String kind) {
        boolean lebanese = kind != null
                && (kind.trim().equalsIgnoreCase(LivenessConfig.KIND_LB_PASSPORT) || kind.trim().equalsIgnoreCase("lb"));
        String k = lebanese ? LivenessConfig.KIND_LB_PASSPORT : LivenessConfig.KIND_OTHER_PASSPORT;
        if (!k.equals(documentKind)) {
            retriesThisVisit = 0;                                    // a new document kind: a new visit
            matteBefore = false;
            handPauses = 0;
            sunPauses = 0;
            brightMiss = false;
        }
        this.documentKind = k;
        this.cfg = null;                                            // reload on the next frame
    }

    /** Loads the config once (needs a Context) and copies every threshold into the fields. */
    private boolean ensureConfig() {
        if (cfg != null) return true;
        if (getActivity() == null) return false;
        cfg = LivenessConfig.load(getActivity().getApplicationContext(), documentKind);

        PASSPORT_ASPECT     = cfg.profile.aspect;
        RECT_H              = LivenessVision.rectHeight(PASSPORT_ASPECT);
        FRAME_MARGIN_PX     = cfg.frameMarginPx;
        MIN_QUAD_AREA_FRAC  = cfg.minQuadAreaFrac;
        FRONTAL_MAX         = cfg.frontalMax;
        SHAPE_TOL           = cfg.shapeTol;
        STABLE_MS           = cfg.stableMs;
        ANALYSIS_LONG_EDGE  = cfg.analysisLongEdge;
        LOSS_MS             = cfg.lossMs;
        PHASE_TIMEOUT_MS    = cfg.phaseTimeoutMs;
        MAX_ATTEMPTS        = cfg.maxAttempts;
        MAX_RESTARTS        = cfg.maxRestarts;
        MAX_EARLY_RESTARTS  = cfg.maxEarlyRestarts;
        BASELINE_HOLD_MS    = cfg.baselineHoldMs;
        SHARPNESS_MIN       = cfg.sharpnessMin;
        GLARE_MAX           = cfg.glareFracMax;
        JITTER_MAX          = cfg.jitterMax;
        DRIFT_MAX           = cfg.driftMax;
        LIGHT_SETTLE_MS     = cfg.light.settleMs;
        LIGHT_MAX_MS        = cfg.light.maxMs;
        LIGHT_FRAMES        = cfg.light.frames;
        OFF_SETTLE_MS       = cfg.light.offSettleMs;
        BATTERY_LOW         = cfg.batteryLowBelow;
        TILT_MIN            = cfg.tiltMinForPage();
        SWEEP_FRAMES        = cfg.holdFramesForPage();
        WRONG_WAY_FRAMES    = cfg.wrongWayFrames;
        STUCK_MS            = cfg.stuckMs;
        TILT_HOLD_MS        = cfg.tiltHoldMs;
        TILT_NOISE_K        = cfg.tiltNoiseK;
        SAME_PLACE_MAX      = cfg.samePlaceMax;
        AREA_JUMP_MAX       = cfg.areaJumpMax;
        FLAT_DEG            = cfg.hands.flatDeg;
        FLAT_MS             = cfg.hands.flatMs;
        MAX_HAND_PAUSES     = cfg.hands.maxPauses;
        TABLE_CARD          = cfg.hands.tableCard;
        TURN_DEG            = cfg.hands.turnDeg;
        OFF_MAX_MS          = cfg.light.offMaxMs;
        ANGLE_MAX           = cfg.angleMax;
        TILT_OVERFLOW       = cfg.tiltOverflow;
        EDGE_MARGIN         = cfg.edgeMargin;
        BRIGHT_LIFT         = cfg.reflection.brightLift;
        SUN_MAX             = (int) Math.round(cfg.reflection.sunMax);   // sunMax is a Double in LivenessConfig
        STALL_MS            = cfg.stallMs;
        SEND_MAX_SIDE       = cfg.sendPhoto.maxSide;

        newTry();
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", "config " + cfg.policyVersion
                + " profile=" + cfg.profile.id + " aspect=" + PASSPORT_ASPECT + " rect=" + LivenessVision.OPT_W + "x" + RECT_H
                + " tiltMin=" + TILT_MIN + " holdFrames=" + SWEEP_FRAMES + " laminate=" + cfg.laminatePolicy()
                + " reflection=" + cfg.reflection.policy + " spread=" + cfg.profile.spread);
        return true;
    }

    /**
     * Detects the passport photo page AND runs the Position check.
     *  - lastQuad != null -> passport found in THIS frame (use this in later phases)
     *  - returns true     -> found in consecutive frames for >= stableMs (Position passed)
     */
    private boolean checkDocument(Bitmap normalImage) {
        long now = SystemClock.elapsedRealtime();
        lastUpright = null; lastQuad = null; lastPixelQuad = null; lastPose = null; lastFlat = false; lastBothPages = false;
        lastRawPose = null; lastRawQuad = null; lastIsRaw = false; lastSharp = false; lastGlare = false;

        // analysis (ANA) size: every pixel check of the spec is measured at this size; the photo stays full size
        Bitmap upright = ImageUtils.rotateBitmap(scaleDown(normalImage, ANALYSIS_LONG_EDGE), 90);
        double w = upright.getWidth(), h = upright.getHeight();

        FinderResult r = finder.detect(upright, DocHead.PASSPORT_PAGE);
        if (!r.getFound()) {
            positionMiss();
            noOutline("finder found no page");
            return false;
        }

        List<PointF> c = r.getCorners();
        double[] model = new double[] {
                c.get(0).x, c.get(0).y, c.get(1).x, c.get(1).y,
                c.get(2).x, c.get(2).y, c.get(3).x, c.get(3).y
        };
        boolean inferred = r.getInferredCorner() != null;

        // ---- SWEEP: track the RAW finder outline (SpreadFix / stabilizer drop tilted frames) ----
        if (phase == Phase.SWEEP) {
            useRawFrame(model, upright, w, h, inferred);
            return false;                                             // Position result not used here
        }

        // The finder outlines either the photo page or the whole open booklet (two pages; device log: as soon as the
        // torch comes on, aspect 0.74). lb_passport: always cut to the photo page (both pages are shown).
        // other_passport: a page-shaped outline IS the page; a booklet-shaped one is cut to the photo page as well.
        double[] raw = photoPageOf(model);
        if (raw == null || !Geometry.isValidQuad(raw, w, h, FRAME_MARGIN_PX, MIN_QUAD_AREA_FRAC)) {
            positionMiss();
            noOutline(raw == null ? "SpreadFix gave no photo page" : "outline outside the picture / too small");
            return false;
        }

        Pose rp = Geometry.poseOf(raw);
        boolean rightShape = rp.getAspect() >= 0.6 * PASSPORT_ASPECT && rp.getAspect() <= 1.6 * PASSPORT_ASPECT;
        boolean notSkewed  = Math.abs(rp.getLr()) <= 0.45 && Math.abs(rp.getTb()) <= 0.45;
        if (!rightShape || !notSkewed) {
            positionMiss();
            noOutline(String.format(Locale.US, "bad shape aspect=%.2f (page %.2f) lr=%.3f tb=%.3f",
                    rp.getAspect(), PASSPORT_ASPECT, rp.getLr(), rp.getTb()));
            return false;
        }

        OutlineStabilizer.Result s = stab.update(raw, w, h, inferred);
        boolean stable = s.getQuad() != null
                && ("accepted".equals(s.getStatus()) || "moved".equals(s.getStatus()));
        if (!stable || !s.getPoseReliable()) {
            positionMiss();
            noOutline("stabiliser " + s.getStatus() + (s.getPoseReliable() ? "" : " (pose unreliable)"));
            return false;
        }

        // ---- normal orientation only (not upside down, not MRZ left / MRZ right), in Position ----
        if (phase == Phase.POSITION) {
            if (!isNormalOrientation(upright, s.getQuad())) {
                positionMiss();
                noOutline("orientation: " + orientWhy);
                return false;
            }
        }

        // ---- found in this frame ----
        lastRawPose = Geometry.poseOf(model);                         // raw outline, for the sweep reference
        lastRawQuad = model;
        lastUpright = upright;
        lastQuad = s.getQuad();                                       // steadied PHOTO page (pose)
        lastPixelQuad = raw;                                          // unsmoothed PHOTO page (pixel checks)
        lastPose = Geometry.poseOf(lastQuad);
        lastFlat = Geometry.looksRight(lastQuad, PASSPORT_ASPECT, FRONTAL_MAX, SHAPE_TOL);
        lastBothPages = isSpread() && SpreadFix.upperPageInView(lastQuad, w, h);

        // quality (checks/quality.md): Laplacian variance and grey > 249 inside the outline shrunk by 4%
        double[] q = LivenessVision.quality(upright, raw);
        lastSharpness = q[0];
        lastGlareFrac = q[1];
        lastSharp = lastSharpness >= SHARPNESS_MIN;
        lastGlare = lastGlareFrac > GLARE_MAX;

        // ---- Position: must stay found for stableMs in a row (one odd frame passed over) ----
        positionMisses = 0;
        if (steadySince == 0) {
            steadySince = now;
        }
        return now - steadySince >= STABLE_MS;
    }

    /** A bad frame in Position: the steady clock restarts only on the second bad frame in a row. */
    private void positionMiss() {
        positionMisses++;
        if (positionMisses >= 2) steadySince = 0;
    }

    /** Debug: why this frame has no outline. */
    private void noOutline(String why) {
        if (DEBUG_LOGS && (phase == Phase.POSITION || phase == Phase.BASELINE || phase == Phase.LIGHT)) {
            android.util.Log.d("PassportLivenessDebug", "no outline (" + phase + "): " + why);
        }
    }

    /** Raw finder outline for this frame (SWEEP). Rejects guessed corners and broken outlines. */
    private void useRawFrame(double[] model, Bitmap upright, double w, double h, boolean inferred) {
        if (inferred) {
            return;                                                 // guessed corner (finger on the edge) fakes a tilt
        }
        // tiltOverflow: tipping the bottom edge toward the camera makes it larger, so its corners may leave the picture
        if (!validQuad(model, w, h, TILT_OVERFLOW)) {
            return;
        }
        Pose p = Geometry.poseOf(model);
        if (Math.abs(p.getLr()) > RAW_LR_MAX || Math.abs(p.getTb()) > RAW_TB_MAX
                || (p.getLr() == 0 && p.getTb() == 0)) {
            return;
        }
        lastUpright = upright;
        lastQuad = model;
        lastPose = p;
        lastRawPose = p;
        lastRawQuad = model;
        lastIsRaw = true;
        // pixel checks need the PHOTO page: the raw outline is it when wide; when it is the whole booklet (either
        // kind), cut it to the photo page
        lastPixelQuad = p.getAspect() >= RAW_PAGE_ASPECT_MIN ? model : SpreadFix.photoPage(model, PASSPORT_ASPECT);
    }

    /**
     * The photo page from the finder's outline. lb_passport (spread): SpreadFix always. other_passport: the outline
     * as it is when page-shaped, SpreadFix when it is the whole open booklet (any passport is a booklet).
     */
    private double[] photoPageOf(double[] model) {
        if (isSpread()) return SpreadFix.photoPage(model, PASSPORT_ASPECT);
        return Geometry.poseOf(model).getAspect() >= RAW_PAGE_ASPECT_MIN ? model : SpreadFix.photoPage(model, PASSPORT_ASPECT);
    }

    /** geometry.ts isValidQuad: finite, inside the picture (less margin, plus `outside` x width), convex, big enough. */
    private boolean validQuad(double[] q, double w, double h, double outside) {
        double m = FRAME_MARGIN_PX - outside * w;
        for (int i = 0; i < 4; i++) {
            double x = q[2 * i], y = q[2 * i + 1];
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) return false;
            if (x < m || y < m || x > w - m || y > h - m) return false;
        }
        boolean pos = true, neg = true;
        for (int i = 0; i < 4; i++) {
            int a = i, b = (i + 1) % 4, c = (i + 2) % 4;
            double t = (q[2 * b] - q[2 * a]) * (q[2 * c + 1] - q[2 * a + 1]) - (q[2 * b + 1] - q[2 * a + 1]) * (q[2 * c] - q[2 * a]);
            if (!(t > 0)) pos = false;
            if (!(t < 0)) neg = false;
        }
        if (!pos && !neg) return false;                             // not convex / self-intersecting
        double area = 0;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            area += q[2 * i] * q[2 * j + 1] - q[2 * j] * q[2 * i + 1];
        }
        return Math.abs(area) / 2 >= MIN_QUAD_AREA_FRAC * w * h;
    }

    /** The profile shows both pages (lb_passport). */
    private boolean isSpread() {
        return cfg != null && cfg.profile.spread;
    }

    /** Angle of the outline's bottom edge (BL -> BR): the same edge for the photo-page and the booklet outline. */
    private static double bottomAngle(double[] q) {
        double[] o = orderCorners(q);
        if (o == null) return Double.NaN;
        return Math.atan2(o[5] - o[7], o[4] - o[6]);
    }

    /** True only if the photo page is landscape, roughly level, and the MRZ is at the bottom. */
    private boolean isNormalOrientation(Bitmap upright, double[] quad) {
        double[] o = orderCorners(quad);                  // TL, TR, BR, BL by image position
        if (o == null) {
            orientWhy = "corners ambiguous";
            return false;
        }

        // 1) landscape: long side horizontal -> rejects MRZ left / MRZ right
        double top = dist(o, 0, 1), right = dist(o, 1, 2), bottom = dist(o, 2, 3), left = dist(o, 3, 0);
        if (left + right >= top + bottom) {
            orientWhy = "page not landscape (MRZ left / right?)";
            return false;
        }

        // 2) not rotated too much in-plane
        double roll = Math.toDegrees(Math.atan2(o[3] - o[1], o[2] - o[0]));
        if (Math.abs(roll) > MAX_ROLL_DEG) {
            orientWhy = String.format(Locale.US, "page rotated %.0f deg", roll);
            return false;
        }

        // 3) MRZ at the bottom -> rejects upside down
        double[] mrz = mrzScores(upright, o);             // {topBand, bottomBand}
        boolean ok = mrz[1] >= MRZ_MIN && mrz[1] >= mrz[0] * MRZ_MARGIN;
        if (!ok) orientWhy = String.format(Locale.US, "MRZ not found at the bottom (bottom=%.3f top=%.3f, need >= %.2f and 2x)",
                mrz[1], mrz[0], MRZ_MIN);
        return ok;
    }

    /** Reorders 4 corners as TL, TR, BR, BL using image position. Null if ambiguous. */
    private static double[] orderCorners(double[] q) {
        int tl = 0, tr = 0, br = 0, bl = 0;
        double minSum = Double.MAX_VALUE, maxSum = -Double.MAX_VALUE;
        double minDiff = Double.MAX_VALUE, maxDiff = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double x = q[2 * i], y = q[2 * i + 1];
            double sum = x + y, diff = x - y;
            if (sum < minSum) { minSum = sum; tl = i; }
            if (sum > maxSum) { maxSum = sum; br = i; }
            if (diff > maxDiff) { maxDiff = diff; tr = i; }
            if (diff < minDiff) { minDiff = diff; bl = i; }
        }
        if (tl == tr || tl == br || tl == bl || tr == br || tr == bl || br == bl) return null;
        return new double[] {
                q[2 * tl], q[2 * tl + 1], q[2 * tr], q[2 * tr + 1],
                q[2 * br], q[2 * br + 1], q[2 * bl], q[2 * bl + 1]
        };
    }

    private static double dist(double[] o, int a, int b) {
        return Math.hypot(o[2 * a] - o[2 * b], o[2 * a + 1] - o[2 * b + 1]);
    }

    /**
     * Straightens the page and measures how "MRZ-like" the top and bottom 25% strips are.
     * Returns {topBand, bottomBand}.
     */
    private static double[] mrzScores(Bitmap bmp, double[] o) {
        double[] rect = {0, 0, WARP_W, 0, WARP_W, WARP_H, 0, WARP_H};
        double[] hm = LivenessVision.homography(rect, o);
        if (hm == null) return new double[] {0, 0};

        int bw = bmp.getWidth(), bh = bmp.getHeight();
        double minX = Math.min(Math.min(o[0], o[2]), Math.min(o[4], o[6]));
        double maxX = Math.max(Math.max(o[0], o[2]), Math.max(o[4], o[6]));
        double minY = Math.min(Math.min(o[1], o[3]), Math.min(o[5], o[7]));
        double maxY = Math.max(Math.max(o[1], o[3]), Math.max(o[5], o[7]));
        int x0 = clamp((int) Math.floor(minX), 0, bw - 1);
        int y0 = clamp((int) Math.floor(minY), 0, bh - 1);
        int x1 = clamp((int) Math.ceil(maxX), x0, bw - 1);
        int y1 = clamp((int) Math.ceil(maxY), y0, bh - 1);
        int rw = x1 - x0 + 1, rh = y1 - y0 + 1;
        int[] px = new int[rw * rh];
        bmp.getPixels(px, 0, rw, x0, y0, rw, rh);

        int[] gray = new int[WARP_W * WARP_H];
        for (int v = 0; v < WARP_H; v++) {
            for (int u = 0; u < WARP_W; u++) {
                double d = hm[6] * u + hm[7] * v + hm[8];
                int x = clamp((int) Math.round((hm[0] * u + hm[1] * v + hm[2]) / d) - x0, 0, rw - 1);
                int y = clamp((int) Math.round((hm[3] * u + hm[4] * v + hm[5]) / d) - y0, 0, rh - 1);
                int p = px[y * rw + x];
                gray[v * WARP_W + u] = (((p >> 16) & 0xff) * 77 + ((p >> 8) & 0xff) * 150 + (p & 0xff) * 29) >> 8;
            }
        }

        int band = (int) (WARP_H * 0.25);
        return new double[] {
                longRowShare(gray, 0, band),
                longRowShare(gray, WARP_H - band, WARP_H)
        };
    }

    /** Share of rows in [y0,y1) where text runs across >= 60% of the width (MRZ lines span the page). */
    private static double longRowShare(int[] g, int y0, int y1) {
        int cells = WARP_W / CELL;
        int longRows = 0;
        for (int y = y0; y < y1; y++) {
            int base = y * WARP_W;
            int filled = 0;
            for (int c = 0; c < cells; c++) {
                int xs = c * CELL, xe = Math.min(xs + CELL, WARP_W - 1);
                for (int x = xs; x < xe; x++) {
                    if (Math.abs(g[base + x + 1] - g[base + x]) >= EDGE_MIN) {
                        filled++;
                        break;
                    }
                }
            }
            if (filled >= MRZ_ROW_FILL * cells) longRows++;
        }
        return longRows / (double) (y1 - y0);
    }

    /** Scales the frame so its long edge is at most longEdge (keeps the aspect ratio). */
    private static Bitmap scaleDown(Bitmap src, int longEdge) {
        int w = src.getWidth(), h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= longEdge) return src;
        double sc = longEdge / (double) max;
        return LivenessVision.areaResize(src, (int) Math.round(w * sc), (int) Math.round(h * sc));   // area averaging
    }

    /** The photo sent: long side at most maxSide (sendPhoto), smooth downscale; unchanged when already small. */
    private static Bitmap fitLongSide(Bitmap src, int maxSide) {
        int w = src.getWidth(), h = src.getHeight();
        int max = Math.max(w, h);
        if (maxSide <= 0 || max <= maxSide) return src;
        double sc = maxSide / (double) max;
        return Bitmap.createScaledBitmap(src, (int) Math.round(w * sc), (int) Math.round(h * sc), true);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** This frame's photo page straightened to 480 x H with the baseline's corner roll (null if not possible). */
    private int[] rectifyNow() {
        if (lastPixelQuad == null || lastUpright == null) return null;
        double[] o = orderCorners(lastPixelQuad);
        if (o == null) return null;
        return LivenessVision.rectify(lastUpright, LivenessVision.roll(o, baseK), LivenessVision.OPT_W, RECT_H);
    }

    // =============================================================================================
    // Liveness phases
    // =============================================================================================

    /** Green when the current frame is good for the liveness step, dynamic "hold" colour otherwise. */
    private void updateLivenessColor() {
        String color = livenessFrameGood
                ? ConstantsValues.DetectColor                       // green: all good
                : environmentalConditions.getHoldHandColor();       // dynamic: not yet
        // sent every frame: the first calls are ignored by the base class until the view is visible
        setRectFCustomColor(color,
                environmentalConditions.getEnableDetect(),
                environmentalConditions.getEnableGuide(),
                start && !tiltStart);
    }

    /** Called once per frame (only while start == true). Runs the current phase. */
    private void runLiveness(boolean accepted, Bitmap normalImage) {
        long now = SystemClock.elapsedRealtime();
        if (phase == Phase.DONE) return;

        // a pause is shown: every frame is dropped until it continues (the try already restarted when it began)
        if (pause != null) {
            if (!pauseDone(now)) {
                hint(pause);
                return;
            }
            pause = null;
            markTurn();
            flatSince = 0;
            hint(PassportLivenessInstruction.PLACE_DOCUMENT);
        }
        if (notInHand(now)) return;

        if (lastQuad != null) lastSeen = now;                       // passport found in THIS frame

        // the frame stays green from the first good frame until the try restarts or the passport is lost
        if (lastQuad == null && lastSeen > 0 && now - lastSeen > LOSS_MS) livenessFrameGood = false;

        // losses restart in baseline / light; the SWEEP keeps waiting for the lift (anti-swap protects it)
        boolean watchedLoss = phase == Phase.BASELINE || phase == Phase.LIGHT;
        // the phase time limit: every phase except position, the tilt included
        boolean watchedTime = watchedLoss || phase == Phase.SWEEP;
        if (phase == Phase.SWEEP && now - lastSeen > LOSS_MS) {
            hint(PassportLivenessInstruction.PLACE_DOCUMENT);       // out of view: bring it back, keep waiting
        }
        long lossGrace = phase == Phase.LIGHT ? (long) (LOSS_MS * LIGHT_LOSS_FACTOR) : LOSS_MS;
        if (watchedLoss && lastSeen > 0 && now - lastSeen > lossGrace) {
            restartTry("tracking_lost", now);
            return;
        }
        if (watchedTime && now - phaseStart > PHASE_TIMEOUT_MS) {
            engineAttempts++;                                       // a phase timeout adds one attempt
            if (engineAttempts >= MAX_ATTEMPTS) {
                tryReasons.add("attempts_exhausted");
                finishTry(true);
                return;
            }
            restartTry("timeout", now);
            return;
        }

        switch (phase) {
            case POSITION:
                hint(PassportLivenessInstruction.PLACE_DOCUMENT);
                if (lastQuad != null) livenessFrameGood = true;     // found, upright, stable
                if (accepted && photoReady(now)) {
                    enterPhase(Phase.BASELINE, now);
                    playSuccess();                                  // seen and steady: the flat hold starts
                }
                break;

            case BASELINE:
                if (checkBaseline(now)) {
                    flatPhoto = normalImage;                        // THE photo: flat, unlit, full camera size
                    unlitImg = null;                                // next frame = the unlit reference
                    enterPhase(Phase.LIGHT, now);
                    hint(PassportLivenessInstruction.DO_NOT_MOVE);
                }
                break;

            case LIGHT:
                checkLight(now);
                break;

            case SWEEP:
                checkSweep(now);
                break;

            default:
                break;
        }
    }

    private void enterPhase(Phase p, long now) {
        phase = p;
        phaseStart = now;
        if (p != Phase.SWEEP && p != Phase.DONE) setProgress(0);
    }

    /** capture.ts photoReady: the light is confirmed off and the picture had offSettleMs to settle. */
    private boolean photoReady(long now) {
        return lightState == Light.OFF && now - darkAt >= OFF_SETTLE_MS;
    }

    /** capture.ts lightOff: ask the light off; pollLight() confirms it (asking is not darkness). */
    private void lightOff(long now) {
        if (lightState == Light.OFF) return;
        if (lightState == Light.ON) offAskedAt = now;
        lightState = Light.GOING_OFF;
        offLastAskAt = now;
        setTorch(false);
    }

    /** Every frame: the camera must report the light off; re-asked every 250 ms, for light.offMaxMs at most. */
    private void pollLight(long now) {
        if (lightState != Light.GOING_OFF) return;
        if (!isTorchOn()) {
            lightState = Light.OFF;
            darkAt = now;
            return;
        }
        if (now - offAskedAt >= OFF_MAX_MS) {                       // still reported on: go on without waiting
            if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", "light still reported on after offMaxMs: going on");
            lightState = Light.OFF;
            darkAt = now;
            return;
        }
        if (now - offLastAskAt >= OFF_REASK_MS) {
            offLastAskAt = now;
            setTorch(false);
        }
    }

    // =============================================================================================
    // Baseline: frontal, sharp, steady, no glare for baselineHoldMs and >= 3 poses -> flat photo
    // =============================================================================================

    private boolean checkBaseline(long now) {
        if (lastQuad == null || lastIsRaw) {                        // not found
            havePrev = false;
            holdMiss();
            return false;
        }

        double lr = lastPose.getLr(), tb = lastPose.getTb();
        double cx = (lastQuad[0] + lastQuad[2] + lastQuad[4] + lastQuad[6]) / 4;
        double cy = (lastQuad[1] + lastQuad[3] + lastQuad[5] + lastQuad[7]) / 4;
        double fw = lastUpright.getWidth();
        // steady: the centre does not drift more than driftMax per frame, and the pose stays within HOLD_STILL_TOL of
        // where the hold started (the first frame of a hold only has to be steady against the frame before)
        boolean centreStill = havePrev && Math.hypot(cx - prevCx, cy - prevCy) < DRIFT_MAX * fw;
        boolean poseStill = baseCount == 0
                ? havePrev && Math.abs(lr - prevLr) < HOLD_STILL_TOL && Math.abs(tb - prevTb) < HOLD_STILL_TOL
                : Math.abs(lr - holdLr) <= HOLD_STILL_TOL && Math.abs(tb - holdTb) <= HOLD_STILL_TOL;
        boolean steady = centreStill && poseStill;
        prevLr = lr; prevTb = tb; prevCx = cx; prevCy = cy; havePrev = true;

        // challenge-engine.ts:205: frontal is the pose only (no shape test: looksRight only decides whether to DRAW it)
        boolean frontal = Math.abs(lr) <= HOLD_FRONTAL_MAX && Math.abs(tb) <= HOLD_FRONTAL_MAX;
        boolean glare = lastGlareFrac > HOLD_GLARE_MAX;
        if (!frontal) hint(PassportLivenessInstruction.KEEP_FLAT);
        else if (glare) hint(PassportLivenessInstruction.ADJUST_POSITION);
        else hint(PassportLivenessInstruction.KEEP_STEADY);

        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", String.format(Locale.US,
                "baseline lr=%.3f tb=%.3f aspect=%.2f (page %.2f, shape ok=%b) steady=%b hold=%d sharp=%.1f(%b) glare=%.3f(%b) frontal=%b",
                lr, tb, lastPose.getAspect(), PASSPORT_ASPECT, lastFlat, steady, baseCount,
                lastSharpness, lastSharp, lastGlareFrac, glare, frontal));

        if (!steady || !lastSharp || !frontal || glare || !photoReady(now)) {
            holdMiss();
            return false;
        }
        livenessFrameGood = true;
        holdGraceUsed = false;                                      // a good frame: the next odd one is passed over again

        if (baseHoldSince == 0) {
            baseHoldSince = now;
            holdLr = lr;                                            // the hold is judged against this pose
            holdTb = tb;
        }
        baseCount++;
        if (lastRawPose.getAspect() >= RAW_PAGE_ASPECT_MIN) baselineRawTbPage.add(lastRawPose.getTb());
        else                                                baselineRawTbSpread.add(lastRawPose.getTb());
        if (lastRawQuad != null) {
            double a = bottomAngle(lastRawQuad);
            if (!Double.isNaN(a)) baselineAngles.add(a);
        }

        if (now - baseHoldSince >= BASELINE_HOLD_MS && baseCount >= BASELINE_MIN_POSES) {
            baseAngle = baselineAngles.isEmpty() ? Double.NaN : median(baselineAngles);
            // tilt bar: max(tiltMin, tiltNoiseK x the hand's jitter while held flat)
            double noise = Math.max(popStd(baselineRawTbPage), popStd(baselineRawTbSpread));
            tiltMinEffective = Math.max(TILT_MIN, TILT_NOISE_K * noise);
            return true;
        }
        return false;
    }

    /** A bad frame in the flat hold: the first one is passed over (the hold keeps its count), the second restarts it. */
    private void holdMiss() {
        if (baseCount > 0 && !holdGraceUsed) {
            holdGraceUsed = true;
            return;
        }
        resetBaselineHold();
    }

    private void resetBaselineHold() {
        holdGraceUsed = false;
        baseCount = 0;
        baseHoldSince = 0;
        baselineRawTbPage.clear();
        baselineRawTbSpread.clear();
        baselineAngles.clear();
    }

    // =============================================================================================
    // Light (state-machine.md "The light step", checks/reflection.md)
    //   1. the next frame = the UNLIT reference, then the light is requested
    //   2. lit frames from settleMs after the request -> reflection.addLit
    //   3. ends after light.frames lit frames (>= reflection.minLitFrames not whited out) or light.maxMs
    //   4. the next frame = the LIT reference for the tilt -> sweep
    // =============================================================================================

    private void checkLight(long now) {
        hint(PassportLivenessInstruction.DO_NOT_MOVE);

        // 1) unlit reference + light request
        if (unlitImg == null) {
            if (lastPixelQuad == null || lastUpright == null) return;
            double[] o = orderCorners(lastPixelQuad);
            if (o == null) return;
            LivenessVision.Baseline b = LivenessVision.makeBaseline(lastUpright, o, RECT_H);
            if (b == null) return;
            baseK = b.k;
            unlitImg = b.img;
            lightRequestedAt = now;                                 // settle is counted from the REQUEST
            lightAvailable = setTorch(true);
            if (lightAvailable) lightState = Light.ON;
            if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug",
                    "light: unlit reference k=" + baseK + " torch=" + lightAvailable);
            return;
        }

        // the torch must really come on, else the step settles at once (lightAvailable = false)
        if (lightAvailable && !isTorchOn() && now - lightRequestedAt > TORCH_CONFIRM_MS) {
            lightAvailable = false;
            lightOff(now);
        }
        if (!lightAvailable) {
            litImg = unlitImg;                                      // no light: the unlit frame stays the reference
            iridBase = LivenessVision.iridBase(litImg, LivenessVision.OPT_W, RECT_H);
            baseRawTbPage   = rawReference(lightRawTbPage, baselineRawTbPage);
            baseRawTbSpread = rawReference(lightRawTbSpread, baselineRawTbSpread);
            goToSweep(now);
            return;
        }

        long waited = now - lightRequestedAt;

        // 2) + 3) lit frames
        if (!lightEnded) {
            if (waited >= LIGHT_SETTLE_MS && lastPixelQuad != null) {
                int[] cur = rectifyNow();
                if (cur != null) {
                    double[] sample = LivenessVision.pageResponse(unlitImg, cur, LivenessVision.OPT_W, RECT_H);
                    reflection.addLit(sample);
                    lightSharp++;
                    livenessFrameGood = true;
                    if (lastRawPose != null) {                      // raw pose with the torch on (tilt reference)
                        if (lastRawPose.getAspect() >= RAW_PAGE_ASPECT_MIN) lightRawTbPage.add(lastRawPose.getTb());
                        else                                                lightRawTbSpread.add(lastRawPose.getTb());
                    }
                    if (DEBUG_LOGS && sample != null) android.util.Log.d("PassportLivenessDebug", String.format(Locale.US,
                            "light frame spec=%.4f chroma=%.4f flare=%.5f veil=%.3f",
                            sample[0], sample[1], sample[2], sample[3]));
                }
            }
            boolean whitedOut = reflection.usable() < cfg.reflection.minLitFrames;
            if ((lightSharp >= LIGHT_FRAMES && !whitedOut) || waited > LIGHT_MAX_MS) {
                lightEnded = true;                                  // the next frame = the lit reference
                tooBright(now);                                     // the sun check, before the tilt
            }
            return;
        }

        // 4) lit reference for the tilt
        if (pause != null || phase != Phase.LIGHT) return;          // the sun pause restarted the try
        int[] lit = rectifyNow();
        if (lit == null) return;
        litImg = lit;
        iridBase = LivenessVision.iridBase(litImg, LivenessVision.OPT_W, RECT_H);
        baseRawTbPage   = rawReference(lightRawTbPage, baselineRawTbPage);
        baseRawTbSpread = rawReference(lightRawTbSpread, baselineRawTbSpread);
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", String.format(Locale.US,
                "light done: lit=%d usable=%d rawBase page=%.3f spread=%.3f tiltMin=%.3f",
                lightSharp, reflection.usable(), baseRawTbPage, baseRawTbSpread, tiltMinEffective));
        goToSweep(now);
    }

    /**
     * capture.ts tooBright, at the end of the light step: the page did not answer the light (emissive), or this
     * passport's last try missed with the light barely reaching it. In direct sun the phone's light adds nothing:
     * light off, TOO_BRIGHT pause, the try starts over after Continue. At most reflection.sunMax times.
     */
    private boolean tooBright(long now) {
        if (sunPauses >= SUN_MAX || !lightAvailable || isBatteryTooLow()) return false;
        LivenessGate.ReflectionResult r = reflection.result();
        double lift = r.litLift != null ? r.litLift : 9;
        boolean brightAgain = brightMiss && lift <= BRIGHT_LIFT;
        if (!"enforce".equals(cfg.reflection.policy) || !("emissive".equals(r.state) || brightAgain)) return false;
        sunPauses++;
        brightMiss = false;
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", "sun pause: " + (brightAgain ? "bright_miss" : "no_response")
                + " lift=" + r.litLift);
        startPause(PassportLivenessInstruction.TOO_BRIGHT, "too_bright", now);
        return true;
    }

    /** Raw reference for one outline shape: LIGHT frames first (torch on, like SWEEP), else BASELINE frames. */
    private static double rawReference(List<Double> light, List<Double> baseline) {
        if (light.size() >= 2) return median(light);
        if (!baseline.isEmpty()) return median(baseline);
        return Double.NaN;
    }

    /** Population standard deviation (the spec's std). */
    private static double popStd(List<Double> values) {
        if (values.size() < 2) return 0;
        double mean = 0;
        for (double v : values) mean += v;
        mean /= values.size();
        double sq = 0;
        for (double v : values) sq += (v - mean) * (v - mean);
        return Math.sqrt(sq / values.size());
    }

    private static double median(List<Double> values) {
        List<Double> v = new ArrayList<>(values);
        java.util.Collections.sort(v);
        int n = v.size();
        return n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2.0;
    }

    // =============================================================================================
    // Sweep: lift the bottom edge (torch on). Laminate colours per frame; tilt held for holdFrames;
    // kept open up to tiltHoldMs while the colours are not strong yet; then the try ends (endOnTilt).
    // =============================================================================================

    /** Light finished (or not available) -> show the tilt screen: lift the bottom edge. */
    private void goToSweep(long now) {
        resetSweep();
        markTurn();                                                 // the phone's own turn is counted from here
        lastMoveAt = now;
        enterPhase(Phase.SWEEP, now);
        tiltStart = true;
        showLbTiltPassportLayout();
        hint(PassportLivenessInstruction.LIFT_BOTTOM);
    }

    private void checkSweep(long now) {
        if (lastQuad == null) return;                               // passport not in view: keep waiting

        // anti-swap: after a loss it must be the same document in the same place
        if (!isSameDocument(now)) {
            if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", "sweep: different document/place after loss -> restart");
            restartTry("continuity_lost", now);
            return;
        }

        // laminate colours of this frame, against the LIT flat reference.
        // native: the tilt reflection sample (pageResponse vs the lit frame) is recorded only and never decides;
        // it is no longer computed on every tilt frame, so the tilt frames come in faster.
        double[] irid = null;
        if (litImg != null) {
            int[] cur = rectifyNow();
            if (cur != null) {
                irid = LivenessVision.iridescence(iridBase, LivenessVision.chromaGrid(cur, LivenessVision.OPT_W, RECT_H));
                if (irid != null) iridTilt.add(irid);
            }
        }

        // tilt: compare page with page, booklet with booklet (raw outline shape changes between frames)
        boolean pageShape = lastPose.getAspect() >= RAW_PAGE_ASPECT_MIN;
        double base = pageShape ? baseRawTbPage : baseRawTbSpread;
        if (Double.isNaN(base)) return;                             // no reference for this outline shape
        double tb = lastPose.getTb();
        double need = tiltMinEffective;

        double d = BOTTOM_LIFT_SIGN * (tb - base);                  // toward: + = bottom edge lifted toward the camera
        if (Math.abs(d) <= SWEEP_FLAT_EPS) sweepSawFlat = true;     // the user started from flat

        if (Math.abs(d - prevDelta) > MOVE_EPS) lastMoveAt = now;
        prevDelta = d;
        tiltPeak = Math.max(tiltPeak, d);
        sweepMoved = Math.max(sweepMoved, Math.abs(d));

        // angleMax: a tilt that comes with an in-plane turn of the page does not count
        double angleNow = lastRawQuad != null ? bottomAngle(lastRawQuad) : Double.NaN;
        boolean turned = false;
        if (!Double.isNaN(angleNow) && !Double.isNaN(baseAngle)) {
            double t = angleNow - baseAngle;
            turned = Math.abs(Math.atan2(Math.sin(t), Math.cos(t))) > ANGLE_MAX;
        }

        // the wrong edge is judged from where the page was when the tilt was asked, and only once it keeps going that way
        if (pageShape && Double.isNaN(sweepFromPage)) sweepFromPage = tb;
        if (!pageShape && Double.isNaN(sweepFromSpread)) sweepFromSpread = tb;
        double from = pageShape ? sweepFromPage : sweepFromSpread;
        sweepWrong = BOTTOM_LIFT_SIGN * (tb - from) <= -need ? sweepWrong + 1 : 0;

        // consecutive frames only, from flat, without an in-plane turn
        boolean hit = !turned && d >= need && sweepSawFlat;
        sweepGood = hit ? sweepGood + 1 : 0;
        if (hit) livenessFrameGood = true;
        if (sweepGood >= SWEEP_FRAMES && reachedAt == 0) reachedAt = now;

        LivenessGate.LaminateResult lam = LivenessGate.iridescenceResult(iridTilt, cfg.laminate);
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", String.format(Locale.US,
                "sweep tb=%.3f base=%.3f toward=%.3f need=%.3f turned=%b good=%d | colours frames=%d lit=%d hues=%d peak=%.4f strong=%b",
                tb, base, d, need, turned, sweepGood,
                lam.frames, lam.litFrames, lam.hues, lam.peakOr0(), lam.strong));

        // tilt meter (capture.ts:331, passports): clamp(toward / (1.4 x need), 0, 1); empty while the tilt is the wrong way
        double meter = sweepSawFlat ? Math.max(0, Math.min(1, d / (1.4 * need))) : 0;
        setProgress((int) Math.round(100 * meter));

        if (reachedAt != 0) {
            // keep_tilting: the reached tilt is held open while the laminate colours are not strong yet (tiltHoldMs).
            // native: only when the colours DECIDE (laminate policy enforce, lb_passport). For other_passport they are
            // recorded only, so waiting up to tiltHoldMs for "strong" colours most foreign passports never show only
            // made every scan 4 s slower.
            boolean coloursDecide = "enforce".equals(lightAvailable ? cfg.laminatePolicy() : "shadow");
            boolean awaiting = cfg.profile.iridescent && coloursDecide && !lam.strong;
            if (awaiting && now - reachedAt < TILT_HOLD_MS) {
                livenessFrameGood = true;
                hint(atEdge() ? PassportLivenessInstruction.HOLD_THERE           // "Hold it there"
                        : PassportLivenessInstruction.MOVE_A_LITTLE_MORE); // "A bit more"
                return;
            }
            playSuccess();                                          // the tilt is done
            finishTry(false);                                       // endOnTilt: the try ends on the tilt
            return;
        }

        // not reached yet: say what is going wrong while it is going wrong (challenge-engine.ts:251)
        if (sweepWrong >= WRONG_WAY_FRAMES) {
            hint(PassportLivenessInstruction.OTHER_SIDE);                       // tilt_other
        } else if (now - phaseStart > STUCK_MS && sweepMoved < need / 2) {
            hint(PassportLivenessInstruction.HOLD_IT_UP);                       // pick_up
        } else if (d >= need && !sweepSawFlat) {
            hint(PassportLivenessInstruction.KEEP_FLAT);                        // "lifted" from the first frame = offset
        } else {
            hint(PassportLivenessInstruction.LIFT_BOTTOM);                      // tilt_bottom (the meter shows how far)
        }
    }

    /** capture.ts atEdge: a corner of the outline within edgeMargin of the picture's edge. */
    private boolean atEdge() {
        if (lastQuad == null || lastUpright == null) return false;
        double w = lastUpright.getWidth(), h = lastUpright.getHeight();
        for (int i = 0; i < 4; i++) {
            double x = lastQuad[2 * i], y = lastQuad[2 * i + 1];
            if (x < EDGE_MARGIN * w || x > (1 - EDGE_MARGIN) * w || y < EDGE_MARGIN * h || y > (1 - EDGE_MARGIN) * h) return true;
        }
        return false;
    }

    /** laminate.md colourProgress: how close the colours are to "strong" (0..1). */
    private double colourProgress(LivenessGate.LaminateResult r) {
        if (r.peak == null) return 0;
        LivenessConfig.Laminate c = cfg.laminate;
        double p = Math.min(Math.min(r.hues / (double) c.strongHues, r.peak / c.strongPeak),
                Math.min(r.litFrames / (double) c.minLitFrames, r.frames / (double) c.minFrames));
        return Math.max(0, Math.min(1, p));
    }

    /** Same document as before a loss? Compares the bottom edge (middle point + length) of the outline. */
    private boolean isSameDocument(long now) {
        double[] o = orderCorners(lastQuad);                        // TL, TR, BR, BL
        if (o == null || lastUpright == null) return true;          // can't tell: don't block
        double fw = lastUpright.getWidth(), fh = lastUpright.getHeight();
        double bx = (o[4] + o[6]) / 2 / fw, by = (o[5] + o[7]) / 2 / fh;
        double len = dist(o, 2, 3) / fw;

        boolean same = true;
        if (sweepRefSet && now - sweepLastSeenAt > LOSS_MS) {      // came back after being lost
            double maxLen = Math.sqrt(AREA_JUMP_MAX);               // area ratio -> edge-length ratio
            double ratio = len / Math.max(sweepRefLen, 1e-6);
            boolean samePlace = Math.hypot(bx - sweepRefX, by - sweepRefY) <= SAME_PLACE_MAX;
            boolean sameSize = ratio <= maxLen && ratio >= 1 / maxLen;
            same = samePlace && sameSize;
        }
        sweepRefX = bx;
        sweepRefY = by;
        sweepRefLen = len;
        sweepRefSet = true;
        sweepLastSeenAt = now;
        return same;
    }

    private void resetSweep() {
        sweepGood = sweepWrong = 0;
        sweepSawFlat = false;
        sweepRefSet = false;
        sweepLastSeenAt = 0;
        prevDelta = tiltPeak = 0;
        lastMoveAt = 0;
        reachedAt = 0;
        sweepFromPage = sweepFromSpread = Double.NaN;
        sweepMoved = 0;
        iridTilt.clear();
        shownInstruction = null;
    }

    // =============================================================================================
    // Verdict (gate-policy.md, pseudocode/capture-loop.md "finish")
    // =============================================================================================

    /** Ends the try: readings -> decide() -> continue (send the flat photo) / retry / suspected_copy / inconclusive. */
    private void finishTry(boolean engineFailed) {
        long now = SystemClock.elapsedRealtime();
        lightOff(now);
        tiltStart = false;
        hideLbTiltPassportLayout();
        phase = Phase.DONE;

        // laminate + the passport rule (capture.ts:1589)
        LivenessGate.LaminateResult lam = LivenessGate.iridescenceResult(iridTilt, cfg.laminate);
        String laminate = lam.state;
        if (cfg.profile.page && cfg.laminate.passportStrong && "present".equals(laminate)
                && (lam.hues < cfg.laminate.passportHues || lam.peakOr0() < cfg.laminate.strongPeak)) {
            laminate = "absent";
        }
        LivenessGate.ReflectionResult refl = reflection.result();
        boolean batteryLow = isBatteryTooLow();

        LivenessGate.GateInput in = new LivenessGate.GateInput();
        in.cameraOk = true;
        in.enginePhase = engineFailed ? "failed" : "done";
        in.attempt = engineAttempts + retriesThisVisit;
        in.challenge = engineFailed ? "not_observed" : "observed";
        in.optical = "not_configured";                             // optical is shadow (recorded only) for passports
        in.opticalPolicy = "shadow";
        in.reflection = refl.state;
        in.reflectionPolicy = cfg.reflection.policy;
        in.matteBefore = matteBefore;
        in.lightAvailable = lightAvailable;
        in.laminate = laminate;
        in.laminatePolicy = lightAvailable ? cfg.laminatePolicy() : "shadow";
        in.laminateStrong = lam.strong;
        in.laminateRequired = cfg.profile.iridescent;
        in.matteNotEvidence = cfg.profile.page;
        in.batteryLow = batteryLow;
        in.photoOk = engineFailed ? null : flatPhoto != null;

        LivenessGate.Decision decision = LivenessGate.decide(in, MAX_ATTEMPTS);
        List<String> reasons = new ArrayList<>(decision.reasons);
        reasons.addAll(tryReasons);                                 // engine restarts of this try (recorded)
        reasons.addAll(flags);                                      // flag_on_table (recorded, not judged)

        android.util.Log.d("PassportLiveness", String.format(Locale.US,
                "VERDICT %s %s | reflection=%s (lit=%d flare=%s lift=%s) laminate=%s (frames=%d lit=%d hues=%d peak=%.4f strong=%b) attempt=%d",
                decision.verdict, reasons, refl.state, refl.usable, refl.litFlare, refl.litLift,
                laminate, lam.frames, lam.litFrames, lam.hues, lam.peakOr0(), lam.strong, in.attempt));

        // capture.ts:1612 two misses in a row: a "_first" reason sets it; the neutral reasons keep it; else cleared
        boolean first = false, neutral = false;
        for (String r : decision.reasons) {
            if (r.endsWith("_first")) first = true;
            if (r.equals("emblem_untested") || r.equals("no_response_to_light") || r.equals("reflection_matte_unproven")) neutral = true;
        }
        matteBefore = first || (matteBefore && neutral);
        // capture.ts:1615 a passport that missed with the light barely reaching it: the next try checks the sun first
        double lift = refl.litLift != null ? refl.litLift : 9;
        brightMiss = LivenessGate.RETRY.equals(decision.verdict) && lift <= BRIGHT_LIFT && cfg.profile.page;
        if (!LivenessGate.CONTINUE.equals(decision.verdict)) livenessFrameGood = false;

        switch (decision.verdict) {
            case LivenessGate.CONTINUE:
                setProgress(100);
                hint(PassportLivenessInstruction.KEEP_HOLDING);
                finalReady = true;                                  // processImage sends the flat photo
                break;
            case LivenessGate.RETRY:
                retriesThisVisit++;
                newTry();                                           // the camera keeps running: start again
                break;
            default:                                                // suspected_copy / inconclusive
                start = false;                                      // stop: the app shows its retry screen
                notifyLivenessFailed();
                break;
        }
    }

    /**
     * suspected_copy / inconclusive / the camera stalled: reported through onLivenessUpdate (the liveness screen).
     * The screen's retry button calls retryLiveness(). The model has no server response (response = null).
     */
    private void notifyLivenessFailed() {
        if (getActivity() == null || scanPassportCallback == null) return;
        getActivity().runOnUiThread(() -> scanPassportCallback.onLivenessUpdate(new BaseResponseDataModel(
                readPassport,
                "",
                EventsErrorMessages.OnLivenessCardUpdateMessage,
                false,
                "",
                null
        )));
    }

    // =============================================================================================
    // Restarts and tries
    // =============================================================================================

    /** A restart inside the try (lost, timeout, swap): the readings of this presentation are thrown away. */
    private void restartTry(String reason, long now) {
        tryReasons.add(reason);
        boolean early = phase == Phase.POSITION || phase == Phase.BASELINE || phase == Phase.LIGHT;
        if (early) earlyRestarts++;
        else restarts++;
        if (restarts > MAX_RESTARTS || earlyRestarts > MAX_EARLY_RESTARTS) {
            tryReasons.add("restarts_exhausted");
            finishTry(true);
            return;
        }
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", "restart: " + reason
                + " (early=" + earlyRestarts + " late=" + restarts + ")");
        clearReadings(now);
    }

    /** A new try (start, a retry verdict, or a server retry): budgets and readings start from zero. */
    private void newTry() {
        engineAttempts = 0;
        restarts = 0;
        earlyRestarts = 0;
        tryReasons.clear();
        flags.clear();
        pause = null;
        flatSince = 0;
        clearReadings(SystemClock.elapsedRealtime());
    }

    /** Back to Position with nothing measured. */
    private void clearReadings(long now) {
        lightOff(now);
        livenessFrameGood = false;                                  // the frame turns back to the "hold" colour
        steadySince = 0;
        positionMisses = 0;
        lastSeen = 0;
        tiltStart = false;
        hideLbTiltPassportLayout();
        havePrev = false;
        resetBaselineHold();
        baseAngle = Double.NaN;
        flatPhoto = null;
        captureBitmap = null;
        finalReady = false;
        unlitImg = null;
        litImg = null;
        iridBase = null;
        baseK = 0;
        lightAvailable = false;
        lightEnded = false;
        lightRequestedAt = 0;
        lightSharp = 0;
        lightRawTbPage.clear();
        lightRawTbSpread.clear();
        baseRawTbPage = baseRawTbSpread = Double.NaN;
        tiltMinEffective = TILT_MIN;
        reflection = cfg != null ? new LivenessGate.ReflectionAnalyzer(cfg.reflection) : null;
        resetSweep();
        shownInstruction = null;
        stab = new OutlineStabilizer(PASSPORT_ASPECT);
        enterPhase(Phase.POSITION, now);
    }

    /** Kept for the callers that start over (server retry). */
    private void resetLiveness() {
        newTry();
    }

    /**
     * Starts the liveness again after a "suspected_copy" / "inconclusive" verdict (the scan stopped and
     * onLivenessUpdate was sent): call it from the liveness screen's retry button. Safe to call at any time.
     */
    public void retryLiveness() {
        pendingReset = true;                                        // processImage runs newTry() on the next frame
        start = true;
    }

    /** Sends what the user should do + the tilt progress to the app, only when one of them changes. */
    private void hint(PassportLivenessInstruction instruction) {
        if (instruction == shownInstruction && livenessProgress == shownProgress) return;
        shownInstruction = instruction;
        shownProgress = livenessProgress;
        final int progress = livenessProgress;
        if (getActivity() != null && scanPassportCallback != null) {
            getActivity().runOnUiThread(() -> scanPassportCallback.onLivenessInstruction(instruction, progress));
        }
    }

    /** Tilt progress 0..100 (0 before the tilt, 100 when the check passed); re-sends the current instruction. */
    private void setProgress(int p) {
        livenessProgress = Math.max(0, Math.min(100, p));
        if (shownInstruction != null && livenessProgress != shownProgress) hint(shownInstruction);
    }

    // =============================================================================================
    // Sound: AudioCardSuccess where the web plays its tick (capture.ts onPhaseChange):
    // when the passport is seen and steady (the flat hold starts), and when the tilt is done.
    // =============================================================================================

    private void playSuccess() {
        if (audioPlayer != null) audioPlayer.playAudio(ConstantsValues.AudioCardSuccess);
    }

    // =============================================================================================
    // Pauses, stall, background
    // =============================================================================================

    /** Shows a pause: light off, the try restarts (reason recorded), every frame dropped until it continues. */
    private void startPause(PassportLivenessInstruction card, String reason, long now) {
        restartTry(reason, now);
        if (phase == Phase.DONE) return;                            // the restart budget ended the try
        pause = card;
        pauseAt = now;
        hint(card);
    }

    /**
     * When the pause continues by itself (the try starts again from Position):
     *  - HOLD_IN_HAND: shown PAUSE_SHOW_MS, then as soon as the phone is no longer lying flat (picked up)
     *  - PHONE_TURNED, TOO_BRIGHT: after PAUSE_SHOW_MS (time to read it and move)
     *  - always after PAUSE_MAX_MS
     */
    private boolean pauseDone(long now) {
        long shown = now - pauseAt;
        if (shown >= PAUSE_MAX_MS) return true;
        if (shown < PAUSE_SHOW_MS) return false;
        if (pause == PassportLivenessInstruction.HOLD_IN_HAND) {
            Boolean flat = phoneFlat(now);
            return flat == null || !flat;                           // picked up (or no reading): go on
        }
        return true;
    }

    /** camera-torch.md: no analysed frame for stallMs ends the capture (vision_stalled). Runs on the main thread. */
    private void checkStall() {
        if (cfg == null || !Boolean.TRUE.equals(performLivenessDocument) || !start || phase == Phase.DONE) return;
        long last = lastFrameAt;
        if (last == 0 || SystemClock.elapsedRealtime() - last <= STALL_MS) return;
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", "vision_stalled: no frame for " + STALL_MS + " ms");
        setTorch(false);
        lightState = Light.OFF;
        darkAt = SystemClock.elapsedRealtime();
        tiltStart = false;
        hideLbTiltPassportLayout();
        phase = Phase.DONE;
        start = false;
        notifyLivenessFailed();
    }

    /** The app went to the background: everything of this try is discarded and the light switched off. */
    private void interrupt() {
        if (cfg == null) return;
        try {
            setTorch(false);
        } catch (Exception ignored) {
        }
        lightState = Light.OFF;
        darkAt = SystemClock.elapsedRealtime();
        pendingReset = true;                                        // processImage starts a new try on the next frame
    }

    private void markTurn() {
        turnedMark = turnedTotal;
        markX = gravX;
        markY = gravY;
        markZ = gravZ;
    }

    /** Degrees the phone is turned NOW compared with the mark (gravity direction); 0 when unknown. */
    private double turnedSince() {
        double x = gravX, y = gravY, z = gravZ;
        if (Double.isNaN(x) || Double.isNaN(markX)) return 0;
        double dot = Math.max(-1, Math.min(1, x * markX + y * markY + z * markZ));
        return Math.toDegrees(Math.acos(dot));
    }

    // =============================================================================================
    // Hand: the passport must be held in the hand, not scanned from a table (hand-and-sun-pauses.md)
    // =============================================================================================

    /**
     * capture.ts notInHand, first on every observation. At most hands.maxPauses per document kind per visit, either kind:
     *  - table: in position / baseline / light, with an outline in view, the phone within hands.flatDeg of level
     *    for hands.flatMs (camera down over a table) -> HOLD_IN_HAND
     *  - phone: in the tilt, the phone itself turned by hands.turnDeg since the tilt began -> PHONE_TURNED
     * Each pause records flag_on_table / flag_phone_tilted, switches the light off and restarts the try on Continue.
     */
    private boolean notInHand(long now) {
        if (handPauses >= MAX_HAND_PAUSES) return false;
        String why = null;
        boolean early = phase == Phase.POSITION || phase == Phase.BASELINE || phase == Phase.LIGHT;
        if (lastQuad != null && early) {
            Boolean flat = phoneFlat(now);
            if (Boolean.TRUE.equals(flat)) {
                if (flatSince == 0) flatSince = now;
                if (now - flatSince >= FLAT_MS) why = "table";
            } else {
                flatSince = 0;
            }
        } else if (phase == Phase.SWEEP && turnedSince() >= TURN_DEG) {
            why = "phone";
        }
        if (why == null) return false;

        boolean table = why.equals("table");
        if (table && !TABLE_CARD) {                                 // record only
            if (!flags.contains("flag_on_table")) flags.add("flag_on_table");
            flatSince = 0;
            return false;
        }
        handPauses++;
        flatSince = 0;
        String flag = table ? "flag_on_table" : "flag_phone_tilted";
        if (!flags.contains(flag)) flags.add(flag);
        if (DEBUG_LOGS) android.util.Log.d("PassportLivenessDebug", String.format(Locale.US,
                "hand pause: %s (phone %.1f deg from level, turned %.1f deg)", why, phoneTiltDeg, turnedSince()));
        markTurn();
        startPause(table ? PassportLivenessInstruction.HOLD_IN_HAND : PassportLivenessInstruction.PHONE_TURNED,
                table ? "on_table" : "phone_tilted", now);
        return true;
    }

    /** Phone within hands.flatDeg of level? null when the reading is unknown or older than 1 s. */
    private Boolean phoneFlat(long now) {
        double t = phoneTiltDeg;
        if (Double.isNaN(t) || now - phoneTiltAt > MOTION_FRESH_MS) return null;
        return t < FLAT_DEG;
    }

    private void startMotion() {
        if (getActivity() == null) return;
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;
        Sensor s = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (s == null) s = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);   // low-end phones
        if (s != null) sensorManager.registerListener(gravityListener, s, SensorManager.SENSOR_DELAY_GAME);
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        lastGyroNs = 0;
        if (gyro != null) sensorManager.registerListener(gyroListener, gyro, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopMotion() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(gravityListener);
            sensorManager.unregisterListener(gyroListener);
        }
    }

    /** Battery below batteryLowBelow and not charging. */
    private boolean isBatteryTooLow() {
        if (getActivity() == null) return false;
        Intent b = getActivity().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return false;
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        if (level < 0 || scale <= 0) return false;
        return plugged == 0 && level / (double) scale < BATTERY_LOW;
    }

}