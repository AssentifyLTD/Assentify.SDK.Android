package com.assentify.sdk.ScanIDCard;


import static com.assentify.sdk.CheckEnvironment.DetectZoomKt.ZoomLimit;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getIDTag;
import static com.assentify.sdk.Core.Constants.ConstantsValuesKt.getVideoPath;
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
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.assentify.sdk.CameraPreview;
import com.assentify.sdk.CheckEnvironment.DetectIfRectFInsideTheScreen;
import com.assentify.sdk.CheckEnvironment.DetectZoom;
import com.assentify.sdk.Core.Constants.EventsErrorMessages;
import com.assentify.sdk.Core.Constants.IdLivenessInstruction;
import com.assentify.sdk.Core.Constants.LivenessConfig;
import com.assentify.sdk.RemoteClient.Models.Templates;
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry;
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
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails;
import com.assentify.sdk.RemoteClient.Models.StepDefinitions;
import com.assentify.sdk.tflite.Classifier;
import com.assentify.sdk.tflite.LivenessDetection.Finder.DocHead;
import com.assentify.sdk.tflite.LivenessDetection.Finder.DocumentClassifier;
import com.assentify.sdk.tflite.LivenessDetection.Finder.Finder;
import com.assentify.sdk.tflite.LivenessDetection.Finder.FinderResult;
import com.assentify.sdk.tflite.LivenessDetection.Finder.IdBackBarcode;
import com.assentify.sdk.tflite.LivenessDetection.Finder.LivenessGate;
import com.assentify.sdk.tflite.LivenessDetection.Finder.LivenessVision;
import com.assentify.sdk.tflite.LivenessDetection.Finder.OutlineStabilizer;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;


public class ScanIDCard extends CameraPreview implements RemoteProcessingCallback, LanguageTransformationCallback {


    private IDCardCallback idCardCallback;
    private EnvironmentalConditions environmentalConditions;
    private List<String> selectedTemplates = new ArrayList<>();
    private boolean isLastPage = false;
    private String classifiedTemplate = "";

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

    private String language;

    String stepId;
    private ExecutorService createBase64 = Executors.newSingleThreadExecutor();

    private int videoCounter = -1;


    private TemplatesByCountry templatesByCountry;

    private IDResponseModel idResponseModel;

    private DetectIfRectFInsideTheScreen detectIfInsideTheScreen = new DetectIfRectFInsideTheScreen();
    private boolean isRectFInsideTheScreen = false;


    private AssetsAudioPlayer audioPlayer;

    // =============================================================================================
    // Local card liveness: fields (the section itself is at the bottom of the class)
    // =============================================================================================

    // ---- which card (model_A_all25_dynamic.tflite), decided before the liveness of each side ----
    private static final String KIND_ID_FRONT = "lb_civil_id";
    private static final String KIND_ID_BACK = "lb_civil_id_back";
    private static final String KIND_LICENCE = "lb_driving_licence";
    private static final String KIND_PERMIT = "lb_residence_permit";
    private DocumentClassifier docClassifier;
    private boolean classifierFailed = false;
    private volatile boolean kindDecided = false;                   // the classifier answered for this side
    private volatile String livenessKind = null;                    // null = no local liveness for this card
    private volatile boolean backShownLayout = false;               // the classifier saw a "back" class: back layout
    private long classifyStart = 0;                                 // when a card was first seen for this side
    // The FIRST frame whose top class is one of the four supported cards decides (any confidence; "back" needs
    // CLASSIFY_BACK_MIN_CONF, because a front taken as the back would switch the front's checks off).
    // No supported card within CLASSIFY_MAX_MS of the first card seen: no local liveness for this side.
    private static final float CLASSIFY_MIN_CONF = 0.20f;           // any confidence above this
    private static final float CLASSIFY_BACK_MIN_CONF = 0.50f;      // safeguard for "back"; 0 to turn it off
    private static final long CLASSIFY_MAX_MS = 6000;

    // ---- finder ----
    private Finder finder;
    private final Object finderLock = new Object();
    private boolean finderClosed = false;

    // ---- config (assets/rectifier_config_file.json) ----
    private LivenessConfig cfg;
    private String cfgKind = null;                                  // the kind cfg was loaded for
    private boolean isFront, isBack, isLaminateCard;                // ID front / ID back / licence or permit

    private double CARD_ASPECT = 1.585185185185185;
    private OutlineStabilizer stab = new OutlineStabilizer(CARD_ASPECT);
    private int RECT_H = 303;
    private double FRAME_MARGIN_PX = 4, MIN_QUAD_AREA_FRAC = 0.08;
    private long STABLE_MS = 300;
    private int ANALYSIS_LONG_EDGE = 720;
    private long LOSS_MS = 1500, PHASE_TIMEOUT_MS = 15000;
    private int MAX_ATTEMPTS = 3, MAX_RESTARTS = 8, MAX_EARLY_RESTARTS = 40;
    private long BASELINE_HOLD_MS = 400;
    private double SHARPNESS_MIN = 60, DRIFT_MAX = 0.03;
    private long LIGHT_SETTLE_MS = 300, LIGHT_MAX_MS = 2500, OFF_SETTLE_MS = 500, OFF_MAX_MS = 4000;
    private int LIGHT_FRAMES = 5;
    private double BATTERY_LOW = 0.05;
    private double TILT_MIN = 0.05;                                 // ID front: tiltMin (lr); licence: upDown.tiltMin (tb)
    private int SWEEP_FRAMES = 3;                                   // ID front: sweepFrames; licence: upDown.holdFrames
    private long HOLD_MS = 8000;                                    // ID front: emblem.holdMs; licence: drivingLicence.holdMs
    private double TILT_SHARE = 0.6;                                // licence tilt meter: tilt part
    private int WRONG_WAY_FRAMES = 4;
    private long STUCK_MS = 3000;
    private double TILT_NOISE_K = 3, SAME_PLACE_MAX = 0.08, AREA_JUMP_MAX = 1.6;
    private double ANGLE_MAX = 0.17, TILT_OVERFLOW = 0.15, EDGE_MARGIN = 0.02;
    private double BRIGHT_LIFT = 1.25;
    private int SUN_MAX = 3;
    private long STALL_MS = 6000;
    private int SEND_MAX_SIDE = 1920;
    // the ID front's rectangles (fractions of the upright card)
    private double[] emblemCell, emblemInner, emblemOuter;          // profile.emblem
    private double[] orientRoi;                                     // {x, y, w, h, hueLo, hueHi, minSat}
    private double[] staticFeature;                                 // {x, y, w, h, hueLo, hueHi, minSat} or null

    // native-only (same values as the passport, from the device logs)
    private static final int BASELINE_MIN_POSES = 3;
    private static final double HOLD_STILL_TOL = 0.05;
    private static final double HOLD_FRONTAL_MAX = 0.25;
    private static final double HOLD_GLARE_MAX = 0.06;
    private static final double LIGHT_LOSS_FACTOR = 2.5;
    private static final long TORCH_CONFIRM_MS = 1000;
    private static final double RAW_SKEW_MAX = 0.80;
    private static final double MOVE_EPS = 0.01;
    private static final double SWEEP_FLAT_EPS = 0.04;
    private static final long OFF_REASK_MS = 250;

    // ---- this frame ----
    private Bitmap lastUpright;
    private Bitmap curFrame;                                        // the full camera frame (overlay cut edge)
    private double[] lastQuad, lastPixelQuad, lastRawQuad;
    private Pose lastPose, lastRawPose;
    private boolean lastIsRaw, lastSharp;
    private double lastSharpness, lastGlareFrac;
    private long steadySince = 0;
    private int positionMisses = 0;
    private volatile boolean pendingReset = false;

    // ---- phases ----
    private enum Phase { POSITION, BASELINE, LIGHT, SWEEP, BARCODE, DONE }
    private Phase phase = Phase.POSITION;
    private long phaseStart = 0, lastSeen = 0;
    private boolean bugsnagStarted = false;

    // ---- per try / per visit ----
    private int engineAttempts = 0, restarts = 0, earlyRestarts = 0;
    private final List<String> tryReasons = new ArrayList<>();
    private int retriesThisVisit = 0;
    private boolean matteBefore = false;

    // ---- baseline ----
    private int baseCount = 0;
    private long baseHoldSince = 0;
    private boolean havePrev = false, holdGraceUsed = false;
    private double prevLr, prevTb, prevCx, prevCy, holdLr, holdTb;
    private final List<Double> baselineAxis = new ArrayList<>();    // raw pose on the tilt axis while held flat
    private final List<Double> baselineAngles = new ArrayList<>();
    private double baseAngle = Double.NaN;
    private Bitmap flatPhoto = null;                                // THE photo: flat, unlit, full camera size

    // ---- light ----
    private int baseK = 0;
    private int[] unlitImg, litImg;
    private LivenessVision.IridBase iridBase;
    private boolean lightAvailable = false, lightEnded = false, litEarlyCut = false;
    private long lightRequestedAt = 0;
    private enum Light { OFF, ON, GOING_OFF }
    private Light lightState = Light.OFF;
    private long offAskedAt = 0, offLastAskAt = 0, darkAt = -1_000_000L;
    private int lightSharp = 0;
    private LivenessGate.ReflectionAnalyzer reflection;
    private final List<Double> lightAxis = new ArrayList<>();       // raw pose on the tilt axis with the torch on
    private final List<double[]> emblemLit = new ArrayList<>();     // ID front: {now, base} per lit frame
    private final List<Double> cutReadings = new ArrayList<>();     // ID front: overlay cut-edge scores
    private double axisRef = Double.NaN, tiltMinEffective = 0.05;

    // ---- sweep ----
    private final Random rng = new Random();
    private String direction = "right";                             // right (ID front) | top | bottom (licence, random)
    private boolean axisLr = true;
    private int sign = -1;
    private double sweepFrom = Double.NaN, sweepMoved = 0, prevDelta = 0, meterFrom = Double.NaN;
    private int sweepGood = 0, sweepWrong = 0;
    private long lastMoveAt = 0, reachedAt = 0;
    private boolean sweepSawFlat = false;
    private final List<double[]> iridTilt = new ArrayList<>();      // licence / permit: laminate colours per frame
    private final List<double[]> emblemTilt = new ArrayList<>();    // ID front: {absolute lr, emblem now} per frame
    private boolean sweepRefSet = false;
    private long sweepLastSeenAt = 0;
    private double sweepRefX, sweepRefY, sweepRefLen;

    // ---- ID back: the PDF417 barcode (checks/barcode-qr.md), RECORDED ONLY: printed, never a verdict ----
    // Read every idBarcode.everyMs while the back is held still (flat hold, then after its photo), from the FULL camera
    // frame: the card straightened, the barcode band 1712 px wide, plain / sharpened / thresholded, lower band then upper.
    // After the photo, if not read yet: the light comes on (up to light.maxMs + light.barcodeMs) and the capture waits up
    // to idBarcode.waitMs, then goes on, read or not.
    private final ExecutorService barcodeExecutor = Executors.newSingleThreadExecutor();
    private BarcodeScanner barcodeScanner;
    private volatile boolean barcodeBusy = false;
    private volatile boolean barcodeRead = false;
    private volatile int barcodeToken = 0;                          // a late read of an earlier try is ignored
    private long lastBarcodeAt = 0;
    private int barcodeAttempts = 0;
    private long BARCODE_EVERY_MS = 250, BARCODE_WAIT_MS = 8000, BARCODE_LIGHT_MS = 3000;
    // the ID number check: the back's barcode number against the front's IdentificationDocumentCapture.documentNumber.
    // Either side can come first: each number is kept until the other side is there, then they are compared.
    private volatile String readNumber = null;                      // the barcode's number in THIS try of the back
    private volatile String backNumber = null;                      // the number of the back that passed (was sent)
    private volatile String frontNumber = null;                     // the front's documentNumber from the server
    private volatile String sentKind = null;                        // the liveness kind of the side just sent

    // ---- result / UI ----
    private boolean finalReady = false;
    private volatile Bitmap captureBitmap = null;
    private volatile Bitmap lastFrame = null;                       // the latest camera frame (liveness screen image)
    private static final int LIVENESS_IMAGE_MAX_SIDE = 1080;        // the liveness screen's image: long side at most
    private IdLivenessInstruction shownInstruction = null;
    private int livenessProgress = 0, shownProgress = -1;
    private boolean livenessFrameGood = false;
    private boolean tiltStart = false;                              // a tilt drawing is shown: the frame is hidden
    private volatile boolean barcodeLayoutShown = false;            // the barcode drawing is shown: the frame is hidden

    // ---- hand / sun / stall ----
    private double FLAT_DEG = 10, TURN_DEG = 20;
    private long FLAT_MS = 800;
    private int MAX_HAND_PAUSES = 1;
    private boolean TABLE_CARD = true;
    private static final long MOTION_FRESH_MS = 1000;
    private SensorManager sensorManager;
    private volatile double phoneTiltDeg = Double.NaN;
    private volatile long phoneTiltAt = 0;
    private volatile double gravX = Double.NaN, gravY = Double.NaN, gravZ = Double.NaN;
    private double markX = Double.NaN, markY = Double.NaN, markZ = Double.NaN;
    private long flatSince = 0;
    private int handPauses = 0, sunPauses = 0;
    private boolean brightMiss = false;
    private final List<String> flags = new ArrayList<>();
    private volatile IdLivenessInstruction pause = null;
    private long pauseAt = 0;
    private static final long PAUSE_SHOW_MS = 3000, PAUSE_MAX_MS = 8000;
    private volatile long lastFrameAt = 0;
    private final Handler watchdog = new Handler(Looper.getMainLooper());
    private final Runnable stallCheck = new Runnable() {
        @Override
        public void run() {
            checkStall();
            watchdog.postDelayed(this, 1000);
        }
    };
    private final SensorEventListener gravityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            double gx = e.values[0], gy = e.values[1], gz = e.values[2];
            double g = Math.sqrt(gx * gx + gy * gy + gz * gz);
            if (g <= 4) return;
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

    private static final boolean DEBUG_LOGS = true;                // TEMP: set false for release

    public ScanIDCard() {
    }

    int retryCount = 0;

    public ScanIDCard(ConfigModel configModel, EnvironmentalConditions environmentalConditions, String apiKey,
                      IDCardCallback idCardCallback,
                      TemplatesByCountry templatesByCountry,
                      String language
    ) {
        this.apiKey = apiKey;
        this.environmentalConditions = environmentalConditions;
        this.configModel = configModel;
        this.idCardCallback = idCardCallback;
        this.templatesByCountry = templatesByCountry;
        for (Templates template : this.templatesByCountry.getTemplates()) {
            for (KycDocumentDetails kycDocumentDetails : template.getKycDocumentDetails()) {
                this.selectedTemplates.add(kycDocumentDetails.getTemplateProcessingKeyInformation());
            }
        }
        changeTemplateId();
        this.language = language;
        setEnvironmentalConditions(this.environmentalConditions);

    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
        if (this.stepId == null) {
            long stepsCount = this.configModel.getStepDefinitions().stream()
                    .filter(item -> item.getStepDefinition().equals("IdentificationDocumentCapture"))
                    .count();

            if (stepsCount == 1) {
                for (StepDefinitions item : this.configModel.getStepDefinitions()) {
                    if (item.getStepDefinition().equals("IdentificationDocumentCapture")) {
                        this.stepId = String.valueOf(item.getStepId());
                        break;
                    }
                }
            } else {
                if (this.stepId == null) {
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


    public void changeTemplateId() {
        this.retryCount = 0;
        if (!classifiedTemplate.isEmpty()) {
            for (Templates template : this.templatesByCountry.getTemplates()) {
                for (KycDocumentDetails kycDocumentDetails : template.getKycDocumentDetails()) {
                    if (kycDocumentDetails.getTemplateProcessingKeyInformation().equals(classifiedTemplate)) {
                        if (template.getKycDocumentDetails().size() == 2) {
                            for (KycDocumentDetails item : template.getKycDocumentDetails()) {
                                if (!item.getTemplateProcessingKeyInformation().equals(classifiedTemplate)) {
                                    this.selectedTemplates.clear();
                                    this.selectedTemplates.add(item.getTemplateProcessingKeyInformation());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            isLastPage = true;
        }
        createBase64 = Executors.newSingleThreadExecutor();
        highQualityBitmaps.clear();
        motionRectF.clear();
        sendingFlagsZoom.clear();
        sendingFlagsMotion.clear();
        videoCounter = -1;
        start = true;
        remoteProcessing = new RemoteProcessing();
        remoteProcessing.setCallback(this);

        resetKind();                                                // the next side: the classifier decides again
        pendingReset = true;                                        // and the liveness starts from Position

    }

    private boolean isFrontPage() {
        boolean result = false;
        if (!classifiedTemplate.isEmpty()) {
            for (Templates template : this.templatesByCountry.getTemplates()) {
                for (KycDocumentDetails kycDocumentDetails : template.getKycDocumentDetails()) {
                    if (kycDocumentDetails.getTemplateProcessingKeyInformation().equals(classifiedTemplate)) {
                        if (template.getKycDocumentDetails().size() > 1) {
                            result = template.getKycDocumentDetails().get(0).getTemplateProcessingKeyInformation().equals(classifiedTemplate);
                        } else {
                            isLastPage = true;
                            result = true;
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }


    @Override
    protected void processImage(@NonNull Bitmap croppedBitmap, @NonNull Bitmap normalImage, @NonNull List<? extends Classifier.Recognition> results, @NonNull List<Pair<RectF, String>> listScaleRectF, int previewWidth, int previewHeight) {

        if (getActivity() != null) {
            final boolean back = backShownLayout;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (back) changeToBackLayout(environmentalConditions.getHoldHandColor());                 // the classifier saw the back of a card
                    else changeCardWeightLayoutID();
                }
            });
        }
        if (!bugsnagStarted && getActivity() != null) {             // once, not every frame
            BugsnagObject.INSTANCE.initialize(getActivity().getApplicationContext(), configModel);
            bugsnagStarted = true;
        }


        if (audioPlayer == null) {
            if (getActivity() != null) {
                audioPlayer = new AssetsAudioPlayer(getActivity());
            }
        }

        this.results = results;
        this.croppedBitmap = croppedBitmap;
        lastFrame = normalImage;                                    // a reference only (CameraX makes a new bitmap per frame)

        if (pendingReset) {                                         // a new side, a retry, or back from the background
            pendingReset = false;
            resetKind();
            resetLiveness();
        }

        // local liveness only when the step's document liveness is on, and only for the cards it is defined for
        boolean localAllowed = Boolean.TRUE.equals(performLivenessDocument);
        if (localAllowed && start && !kindDecided) {
            decideKind(normalImage);
            if (!kindDecided) {                                     // still looking: no capture of any kind yet
                setRectFCustomColor(environmentalConditions.getHoldHandColor(),
                        environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
                return;
            }
        }
        boolean livenessOn = localAllowed && livenessKind != null && ensureConfig();
        if (!livenessOn) {
            processImageWithoutLiveness(normalImage, listScaleRectF, previewWidth, previewHeight);
            return;
        }

        // ---- local card liveness ----
        curFrame = normalImage;
        boolean accepted = false;
        synchronized (finderLock) {
            if (!finderClosed) {
                if (finder == null && getActivity() != null) {
                    finder = new Finder(getActivity(), ConstantsValues.FinderFileName, 4);
                }
                if (finder != null) {
                    accepted = checkCard(normalImage);
                }
            }
        }
        lastFrameAt = SystemClock.elapsedRealtime();
        pollLight(lastFrameAt);
        if (start) {
            runLiveness(accepted, normalImage);
            if (finalReady) {                                       // verdict "continue": send the flat, unlit photo
                finalReady = false;
                captureBitmap = flatPhoto;
                stopRecording();
            }
        }
        updateLivenessColor();
    }

    /** The capture as before (YOLO + motion / zoom / brightness): no local liveness for this card. */
    private void processImageWithoutLiveness(Bitmap normalImage, List<Pair<RectF, String>> listScaleRectF,
                                             int previewWidth, int previewHeight) {
        if (hasFaceOrCard()) {
            highQualityBitmaps.add(normalImage);
            listScaleRectF.forEach((item) -> {
                if (item.component2().contains(ConstantsValues.CardName)) {
                    isRectFInsideTheScreen = detectIfInsideTheScreen.isRectFWithinMargins(item.component1(), previewWidth, previewHeight);
                }
            });
        }
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (result.getDetectedClass() == 0) {
                rectFCard = new RectF(location.left, location.top, location.right, location.bottom);
                motionRectF.add(rectFCard);
            }
        }
        if (motion == MotionType.SENDING && zoom == ZoomType.SENDING && environmentalConditions.checkConditions(brightness, environmentalConditions) == BrightnessEvents.Good) {
            if (isRectFInsideTheScreen) {
                setRectFCustomColor(ConstantsValues.DetectColor, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            } else {
                setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
            }

        } else {
            setRectFCustomColor(environmentalConditions.getHoldHandColor(), environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(), start);
        }

        checkEnvironment();
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
                    sendingFlagsZoom.clear();
                    sendingFlagsMotion.clear();
                    motion = MotionType.NO_DETECT;
                    zoom = ZoomType.NO_DETECT;
                } else {
                    motion = detectMotion.calculatePercentageChange(motionRectF.get(motionRectF.size() - 2), motionRectF.get(motionRectF.size() - 1));
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
                    brightness, environmentalConditions) == BrightnessEvents.Good && motion == MotionType.SENDING && isRectFInsideTheScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (start && highQualityBitmaps.size() != 0 && sendingFlagsZoom.size() > ZoomLimit && sendingFlagsMotion.size() > environmentalConditions.getMotionCardLimit()) {
                        if (hasFaceOrCard()) {
                            stopRecording();
                        }
                    }

                }
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    idCardCallback.onEnvironmentalConditionsChange(
                            environmentalConditions.checkConditions(
                                    brightness, environmentalConditions),
                            sendingFlagsMotion.size() == 0 ? MotionType.NO_DETECT : sendingFlagsMotion.size() > environmentalConditions.getMotionCardLimit() ? MotionType.SENDING : MotionType.HOLD_YOUR_HAND,

                            zoom, isRectFInsideTheScreen);
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
                    if (!BaseResponseDataModel.getClassifiedTemplate().isEmpty()) {
                        classifiedTemplate = BaseResponseDataModel.getClassifiedTemplate();
                    }
                    highQualityBitmaps.clear();
                    motionRectF.clear();
                    sendingFlagsZoom.clear();
                    sendingFlagsMotion.clear();
                    if (eventName.equals(HubConnectionTargets.ON_COMPLETE)) {
                        Map<String, String> transformedProperties = new HashMap<>();
                        start = false;
                        IDExtractedModel idExtractedModel = IDExtractedModel.Companion.fromJsonString(BaseResponseDataModel.getResponse(), transformedProperties);
                        idResponseModel = new IDResponseModel(
                                BaseResponseDataModel.getDestinationEndpoint(),
                                idExtractedModel,
                                BaseResponseDataModel.getError(),
                                BaseResponseDataModel.getSuccess()
                        );

                        // the ID number check, when the side that just passed is the civil ID front
                        if (KIND_ID_FRONT.equals(sentKind)) {
                            String number = documentNumberOf(idExtractedModel);
                            if (number == null) {
                                Log.d("IdBarcode", "front done: no documentNumber in the answer (not checked)");
                            } else if (backNumber != null && !sameIdNumber(backNumber, number)) {
                                // the back came first and its barcode holds another number: scan the front again
                                Log.d("IdBarcode", "front REJECTED: documentNumber " + number + " is not the back's barcode number "
                                        + backNumber + " -> onRetry");
                                frontNumber = null;
                                retryCount++;
                                start = true;
                                pendingReset = true;
                                BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                                idCardCallback.onRetry(BaseResponseDataModel);
                                return;
                            } else {
                                frontNumber = number;               // kept for the back (or already matches it)
                                Log.d("IdBarcode", "front done: documentNumber " + number
                                        + (backNumber != null ? " = the back's barcode number" : " (the back is checked against it)"));
                            }
                        }

                        if (Objects.equals(language, Language.NON)) {
                            idCardCallback.onComplete(idResponseModel, isFrontPage(), isLastPage, classifiedTemplate);
                        } else {
                            LanguageTransformation translated = new LanguageTransformation(apiKey);
                            translated.setCallback(ScanIDCard.this);
                            translated.languageTransformation(
                                    language,
                                    preparePropertiesToTranslate(language, idExtractedModel.getOutputProperties())
                            );
                        }


                    } else if (eventName.equals(HubConnectionTargets.ON_RETRY)) {
                        retryCount++;
                        start = true;
                        BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                        idCardCallback.onRetry(BaseResponseDataModel);

                    } else {
                        start = eventName.equals(HubConnectionTargets.ON_ERROR) || eventName.equals(HubConnectionTargets.ON_UPLOAD_FAILED) || eventName.equals(HubConnectionTargets.ON_LIVENESS_UPDATE) || eventName.equals(HubConnectionTargets.ON_WRONG_TEMPLATE);
                        switch (eventName) {
                            case HubConnectionTargets.ON_WRONG_TEMPLATE:
                                idCardCallback.onWrongTemplate(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_LIVENESS_UPDATE:
                                idCardCallback.onLivenessUpdate(BaseResponseDataModel, null);   // server: image URL in the response
                                break;
                            case HubConnectionTargets.ON_ERROR:
                                idCardCallback.onError(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CLIP_PREPARATION_COMPLETE:
                                idCardCallback.onClipPreparationComplete(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_STATUS_UPDATE:
                                idCardCallback.onStatusUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPDATE:
                                idCardCallback.onUpdated(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_CARD_DETECTED:
                                idCardCallback.onCardDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_EXTRACTED:
                                idCardCallback.onMrzExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_MRZ_DETECTED:
                                idCardCallback.onMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_MRZ_EXTRACTED:
                                idCardCallback.onNoMrzDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_DETECTED:
                                idCardCallback.onFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_NO_FACE_DETECTED:
                                idCardCallback.onNoFaceDetected(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_FACE_EXTRACTED:
                                idCardCallback.onFaceExtracted(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_QUALITY_CHECK_AVAILABLE:
                                idCardCallback.onQualityCheckAvailable(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CAPTURED:
                                idCardCallback.onDocumentCaptured(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_DOCUMENT_CROPPED:
                                idCardCallback.onDocumentCropped(BaseResponseDataModel);
                                break;
                            case HubConnectionTargets.ON_UPLOAD_FAILED:
                                idCardCallback.onUploadFailed(BaseResponseDataModel);
                                break;

                            default:
                                start = true;
                                BaseResponseDataModel.setError(EventsErrorMessages.OnRetryCardMessage);
                                idCardCallback.onRetry(BaseResponseDataModel);
                                break;
                        }
                    }

                    if (start) pendingReset = true;                 // a server retry: the side starts again

                }
            });
        }
    }


    public boolean hasFaceOrCard() {
        return hasCard();
    }


    public boolean hasCard() {
        boolean hasCard = false;
        for (Classifier.Recognition item : results) {
            if (item.getDetectedClass() == 0 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasCard = true;
            }
        }
        return hasCard;
    }

    public boolean hasFace() {
        boolean hasFace = false;
        for (Classifier.Recognition item : results) {
            if (item.getDetectedClass() == 1 && environmentalConditions.isPredictionValid(item.getConfidence())) {
                hasFace = true;
            }
        }
        return hasFace;
    }


    @Override
    protected void onStopRecordVideo() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    idCardCallback.onSend();
                }
            });
        }

        start = false;
        videoCounter = videoCounter + 1;
        sentKind = captureBitmap != null ? cfgKind : null;          // the side sent (for the ID number check)
        // local liveness passed: the flat, unlit photo; otherwise (no local liveness) the latest frame, as before
        final Bitmap liveness = captureBitmap;
        final Bitmap picked = liveness != null
                ? liveness
                : (highQualityBitmaps.isEmpty() ? null : highQualityBitmaps.get(highQualityBitmaps.size() - 1));
        if (picked == null) {
            start = true;                                           // nothing to send: keep scanning
            return;
        }
        createBase64.execute(() -> {
            final Bitmap toSend = liveness != null ? fitLongSide(picked, SEND_MAX_SIDE) : picked;
            if (liveness == null) audioPlayer.playAudio(ConstantsValues.AudioCardSuccess);   // the liveness played its sound
            remoteProcessing.starProcessingIDs(
                    HubConnectionFunctions.INSTANCE.etHubConnectionFunction(BlockType.ID_CARD),
                    ImageUtils.convertBitmapToByteArray(toSend, BlockType.READ_PASSPORT, getActivity()),
                    configModel,
                    selectedTemplates,
                    "ConnectionId",
                    getVideoPath(configModel, this.templatesByCountry.getName(), 0),
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
                    getIDTag(configModel, this.templatesByCountry.getName()),
                    false
            );

        });
    }

    String nameKey = "";
    int nameWordCount = 0;
    String surnameKey = "";

    @Override
    public void onTranslatedSuccess(@Nullable Map<String, String> properties) {
        getIgnoredProperties(Objects.requireNonNull(idResponseModel.getIDExtractedModel().getOutputProperties())).forEach((key, value) -> {
            properties.put(key, value);
        });

        Objects.requireNonNull(idResponseModel.getIDExtractedModel().getOutputProperties()).forEach(
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

        idResponseModel.getIDExtractedModel().getTransformedProperties().clear();
        idResponseModel.getIDExtractedModel().getExtractedData().clear();
        properties.forEach((key, value) -> {

            if (key.equals(FullNameKey)) {
                if (!nameKey.isEmpty()) {
                    idResponseModel.getIDExtractedModel().getTransformedProperties().put(nameKey, getSelectedWords(value.toString(), nameWordCount));
                    idResponseModel.getIDExtractedModel().getExtractedData().put("name", getSelectedWords(value.toString(), nameWordCount));
                }
                if (!surnameKey.isEmpty()) {
                    idResponseModel.getIDExtractedModel().getTransformedProperties().put(surnameKey, getRemainingWords(value.toString(), nameWordCount));
                    idResponseModel.getIDExtractedModel().getExtractedData().put("surname", getRemainingWords(value.toString(), nameWordCount));
                }
            } else {
                idResponseModel.getIDExtractedModel().getTransformedProperties().put(key, value);
                String newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length())
                        .replace("_", " ");
                idResponseModel.getIDExtractedModel().getExtractedData().put(newKey, value);
            }

        });


        idCardCallback.onComplete(idResponseModel, isFrontPage(), isLastPage, classifiedTemplate);
    }

    @Override
    public void onTranslatedError(@Nullable Map<String, String> properties) {
        idCardCallback.onComplete(idResponseModel, isFrontPage(), isLastPage, classifiedTemplate);
    }

    public void stopScanning() {
        hideBarcodeLayout();
        closeCamera();
    }


    @Override
    public void onPause() {
        BugsnagObject.INSTANCE.pauseSession();
        stopMotion();
        watchdog.removeCallbacks(stallCheck);
        interrupt();
        super.onPause();
    }

    @Override
    public void onResume() {
        BugsnagObject.INSTANCE.resumeSession();
        super.onResume();
        startMotion();
        lastFrameAt = 0;
        watchdog.removeCallbacks(stallCheck);
        watchdog.postDelayed(stallCheck, 1000);
    }

    @Override
    public void onDestroy() {                                       // no "synchronized" on the method: finderLock below
        BugsnagObject.INSTANCE.pauseSession();
        super.onDestroy();
        this.createBase64.shutdown();
        barcodeToken++;
        barcodeExecutor.shutdownNow();
        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }
        if (audioPlayer != null) {
            audioPlayer.stopAudio();
        }
        synchronized (finderLock) {
            finderClosed = true;
            if (finder != null) {
                finder.close();
                finder = null;
            }
            if (docClassifier != null) {
                docClassifier.close();
                docClassifier = null;
            }
        }
    }

    @Override
    public void onUploadProgress(int progress) {
        idCardCallback.onUploadingProgress(progress);

    }


    // #############################################################################################
    // #############################################################################################
    //
    //                                  Local Card Liveness Check
    //
    //   rc17 spec (mobile-handover/02-core-spec, 03-documents/civil-id, driving-licence-residence-permit):
    //    - lb_civil_id (front): POSITION -> BASELINE (flat photo) -> LIGHT -> lift the RIGHT edge -> verdict.
    //      Enforced: cedar emblem (lit, then dark on the tilt), overlay cut edge, reflection.
    //    - lb_civil_id_back: POSITION -> BASELINE (flat photo) -> [BARCODE: light on, wait up to idBarcode.waitMs] -> done.
    //      No tilt (back.sweeps = 0). The PDF417 barcode is read and PRINTED (tag IdBarcode); recorded only, never a verdict.
    //    - lb_driving_licence / lb_residence_permit: POSITION -> BASELINE (flat photo) -> LIGHT -> lift the TOP or
    //      BOTTOM edge (random) -> held open while the laminate colours are read -> verdict. Enforced: laminate, reflection.
    //   Run only when the step's document liveness is on, and only for those four cards (DocumentClassifier).
    //
    // #############################################################################################
    // #############################################################################################

    // =============================================================================================
    // Which card: DocumentClassifier
    // =============================================================================================

    /**
     * The classifier on frames with a card in view. The FIRST frame whose top class is one of the four supported cards
     * (civil ID front / back, driving licence, residence permit) decides, at any confidence ("back" needs
     * CLASSIFY_BACK_MIN_CONF). Nothing supported within CLASSIFY_MAX_MS of the first card seen: no local liveness.
     */
    private void decideKind(Bitmap frame) {
        if (getActivity() == null) return;
        if (!hasFaceOrCard()) return;                               // no card in view: nothing to classify yet
        long now = SystemClock.elapsedRealtime();
        if (classifyStart == 0) classifyStart = now;
        if (docClassifier == null && !classifierFailed) {
            try {
                docClassifier = new DocumentClassifier(getActivity().getApplicationContext());
            } catch (Exception e) {
                classifierFailed = true;
                Log.e("IdLiveness", "document classifier could not be loaded: " + e);
            }
        }
        if (docClassifier == null) {
            applyKind(null, "classifier unavailable");
            return;
        }
        DocumentClassifier.Result r;
        synchronized (finderLock) {
            if (finderClosed || docClassifier == null) return;
            r = docClassifier.classify(ImageUtils.rotateBitmap(scaleDown(frame, ANALYSIS_LONG_EDGE), frameRotationDegrees()));
        }

        String kind = DocumentClassifier.kindFor(r.label);
        boolean supported = KIND_ID_FRONT.equals(kind) || KIND_ID_BACK.equals(kind)
                || KIND_LICENCE.equals(kind) || KIND_PERMIT.equals(kind);
        boolean back = r.label.toLowerCase(Locale.US).contains("back");
        float bar = back ? CLASSIFY_BACK_MIN_CONF : CLASSIFY_MIN_CONF;
        long waited = now - classifyStart;
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", "classifier: " + r + " supported=" + supported
                + " bar=" + bar + " waited=" + waited + "ms");

        if (supported && r.confidence >= bar) {                     // one of the four: decide now
            backShownLayout = back;
            applyKind(kind, r + " after " + waited + " ms");
        } else if (waited > CLASSIFY_MAX_MS) {                      // nothing supported in time: no local liveness
            applyKind(null, "no supported card after " + CLASSIFY_MAX_MS + " ms (last " + r + ")");
        }
        // otherwise: keep trying on the next frame
    }

    /** Only the civil ID (front / back), the driving licence and the residence permit get the local liveness. */
    private void applyKind(@Nullable String kind, String why) {
        boolean supported = KIND_ID_FRONT.equals(kind) || KIND_ID_BACK.equals(kind)
                || KIND_LICENCE.equals(kind) || KIND_PERMIT.equals(kind);
        livenessKind = supported ? kind : null;
        kindDecided = true;
        Log.d("IdLiveness", "card: " + why + " -> " + (supported ? "kind " + kind + ", local liveness ON"
                : "local liveness SKIPPED" + (kind != null ? " (" + kind + ")" : "")));
    }

    private void resetKind() {
        kindDecided = false;
        livenessKind = null;
        backShownLayout = false;
        classifyStart = 0;
    }

    // =============================================================================================
    // Config
    // =============================================================================================

    /** Loads (or reloads, for another card) the config and copies the thresholds of this card's kind. */
    private boolean ensureConfig() {
        String kind = livenessKind;
        if (kind == null) return false;
        if (cfg != null && kind.equals(cfgKind)) return true;
        if (getActivity() == null) return false;
        if (cfgKind != null && !kind.equals(cfgKind)) {             // another card: a new visit
            retriesThisVisit = 0;
            matteBefore = false;
            handPauses = 0;
            sunPauses = 0;
            brightMiss = false;
        }
        cfg = LivenessConfig.load(getActivity().getApplicationContext(), kind);
        cfgKind = kind;
        isFront = KIND_ID_FRONT.equals(kind);
        isBack = KIND_ID_BACK.equals(kind);
        isLaminateCard = KIND_LICENCE.equals(kind) || KIND_PERMIT.equals(kind);

        CARD_ASPECT         = cfg.profile.aspect;
        RECT_H              = LivenessVision.rectHeight(CARD_ASPECT);
        FRAME_MARGIN_PX     = cfg.frameMarginPx;
        MIN_QUAD_AREA_FRAC  = cfg.minQuadAreaFrac;
        STABLE_MS           = cfg.stableMs;
        ANALYSIS_LONG_EDGE  = cfg.analysisLongEdge;
        LOSS_MS             = cfg.lossMs;
        PHASE_TIMEOUT_MS    = cfg.phaseTimeoutMs;
        MAX_ATTEMPTS        = cfg.maxAttempts;
        MAX_RESTARTS        = cfg.maxRestarts;
        MAX_EARLY_RESTARTS  = cfg.maxEarlyRestarts;
        BASELINE_HOLD_MS    = cfg.baselineHoldMs;
        SHARPNESS_MIN       = cfg.sharpnessMin;
        DRIFT_MAX           = cfg.driftMax;
        LIGHT_SETTLE_MS     = cfg.light.settleMs;
        LIGHT_MAX_MS        = cfg.light.maxMs;
        LIGHT_FRAMES        = cfg.light.frames;
        OFF_SETTLE_MS       = cfg.light.offSettleMs;
        OFF_MAX_MS          = cfg.light.offMaxMs;
        BATTERY_LOW         = cfg.batteryLowBelow;
        WRONG_WAY_FRAMES    = cfg.wrongWayFrames;
        STUCK_MS            = cfg.stuckMs;
        TILT_NOISE_K        = cfg.tiltNoiseK;
        SAME_PLACE_MAX      = cfg.samePlaceMax;
        AREA_JUMP_MAX       = cfg.areaJumpMax;
        ANGLE_MAX           = cfg.angleMax;
        TILT_OVERFLOW       = cfg.tiltOverflow;
        EDGE_MARGIN         = cfg.edgeMargin;
        BRIGHT_LIFT         = cfg.reflection.brightLift;
        SUN_MAX             = (int) Math.round(cfg.reflection.sunMax);
        STALL_MS            = cfg.stallMs;
        SEND_MAX_SIDE       = cfg.sendPhoto.maxSide;
        BARCODE_EVERY_MS    = cfg.idBarcode.everyMs;
        BARCODE_WAIT_MS     = cfg.idBarcode.waitMs;
        BARCODE_LIGHT_MS    = cfg.light.barcodeMs;
        FLAT_DEG            = cfg.hands.flatDeg;
        FLAT_MS             = cfg.hands.flatMs;
        MAX_HAND_PAUSES     = cfg.hands.maxPauses;
        TABLE_CARD          = cfg.hands.tableCard;
        TURN_DEG            = cfg.hands.turnDeg;

        // the tilt (capture.ts:773): ID front = right edge on lr, tiltMin / sweepFrames, held open emblem.holdMs;
        // licence / permit = top or bottom (random) on tb, upDown.tiltMin / upDown.holdFrames, held open holdMs
        if (isFront) {
            TILT_MIN = cfg.tiltMin;
            SWEEP_FRAMES = cfg.sweepFrames;
            HOLD_MS = cfg.emblem.holdMs;
        } else {
            TILT_MIN = cfg.upDown.tiltMin;
            SWEEP_FRAMES = cfg.upDown.holdFrames;
            HOLD_MS = KIND_LICENCE.equals(kind) ? cfg.drivingLicence.holdMs : cfg.residencePermit.holdMs;
            TILT_SHARE = KIND_LICENCE.equals(kind) ? cfg.drivingLicence.tiltShare : cfg.residencePermit.tiltShare;
        }

        // the ID front's rectangles from the profile
        emblemCell = emblemInner = emblemOuter = null;
        orientRoi = staticFeature = null;
        JSONObject raw = cfg.profile.raw;
        JSONObject em = raw.optJSONObject("emblem");
        if (em != null) {
            emblemCell = rect(em.optJSONObject("cell"));
            emblemInner = rect(em.optJSONObject("inner"));
            emblemOuter = rect(em.optJSONObject("outer"));
        }
        JSONObject or = raw.optJSONObject("orient");
        if (or != null) orientRoi = hueRect(or.optJSONObject("roi"), or.optJSONArray("hue"), or.optDouble("minSaturation", 0));
        JSONObject fe = raw.optJSONObject("feature");
        if (fe != null && fe.optJSONArray("staticHue") != null) {
            staticFeature = hueRect(fe.optJSONObject("roi"), fe.optJSONArray("staticHue"), fe.optDouble("minSaturation", 0));
        }

        newTry();
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", "config " + cfg.policyVersion + " profile=" + cfg.profile.id
                + " aspect=" + CARD_ASPECT + " tilt=" + (isBack ? "none" : isFront ? "right" : "top/bottom")
                + " tiltMin=" + TILT_MIN + " frames=" + SWEEP_FRAMES + " holdMs=" + HOLD_MS
                + " emblem=" + (emblemCell != null) + " laminate=" + cfg.laminatePolicy());
        return true;
    }

    private static double[] rect(@Nullable JSONObject o) {
        if (o == null) return null;
        return new double[] {o.optDouble("x", 0), o.optDouble("y", 0), o.optDouble("w", 0), o.optDouble("h", 0)};
    }

    private static double[] hueRect(@Nullable JSONObject roi, @Nullable JSONArray hue, double minSat) {
        double[] r = rect(roi);
        if (r == null || hue == null || hue.length() < 2) return null;
        return new double[] {r[0], r[1], r[2], r[3], hue.optDouble(0, 0), hue.optDouble(1, 0), minSat};
    }

    // =============================================================================================
    // The card in this frame (finder head "card")
    // =============================================================================================

    /** Finds the card AND runs the Position check. lastQuad != null: found in this frame; true: steady for stableMs. */
    private boolean checkCard(Bitmap normalImage) {
        long now = SystemClock.elapsedRealtime();
        lastUpright = null; lastQuad = null; lastPixelQuad = null; lastRawQuad = null; lastPose = null; lastRawPose = null;
        lastIsRaw = false; lastSharp = false;

        Bitmap upright = ImageUtils.rotateBitmap(scaleDown(normalImage, ANALYSIS_LONG_EDGE), frameRotationDegrees());
        double w = upright.getWidth(), h = upright.getHeight();

        FinderResult r = finder.detect(upright, DocHead.CARD);
        if (!r.getFound()) {
            positionMiss();
            noOutline("finder found no card");
            return false;
        }
        List<PointF> c = r.getCorners();
        double[] model = new double[] {
                c.get(0).x, c.get(0).y, c.get(1).x, c.get(1).y,
                c.get(2).x, c.get(2).y, c.get(3).x, c.get(3).y
        };
        boolean inferred = r.getInferredCorner() != null;

        // tilt: the raw outline (the stabilizer drops tilted frames)
        if (phase == Phase.SWEEP) {
            useRawFrame(model, upright, w, h, inferred);
            return false;
        }

        if (!Geometry.isValidQuad(model, w, h, FRAME_MARGIN_PX, MIN_QUAD_AREA_FRAC)) {
            positionMiss();
            noOutline("outline outside the picture / too small");
            return false;
        }
        Pose rp = Geometry.poseOf(model);
        boolean rightShape = rp.getAspect() >= 0.6 * CARD_ASPECT && rp.getAspect() <= 1.6 * CARD_ASPECT;
        boolean notSkewed = Math.abs(rp.getLr()) <= 0.45 && Math.abs(rp.getTb()) <= 0.45;
        if (!rightShape || !notSkewed) {
            positionMiss();
            noOutline(String.format(Locale.US, "bad shape aspect=%.2f (card %.2f) lr=%.3f tb=%.3f",
                    rp.getAspect(), CARD_ASPECT, rp.getLr(), rp.getTb()));
            return false;
        }
        OutlineStabilizer.Result s = stab.update(model, w, h, inferred);
        boolean stable = s.getQuad() != null && ("accepted".equals(s.getStatus()) || "moved".equals(s.getStatus()));
        if (!stable || !s.getPoseReliable()) {
            positionMiss();
            noOutline("stabiliser " + s.getStatus() + (s.getPoseReliable() ? "" : " (pose unreliable)"));
            return false;
        }

        lastRawPose = rp;
        lastRawQuad = model;
        lastUpright = upright;
        lastQuad = s.getQuad();
        lastPixelQuad = model;
        lastPose = Geometry.poseOf(lastQuad);
        double[] q = LivenessVision.quality(upright, model);
        lastSharpness = q[0];
        lastGlareFrac = q[1];
        lastSharp = lastSharpness >= SHARPNESS_MIN;

        positionMisses = 0;
        if (steadySince == 0) steadySince = now;
        return now - steadySince >= STABLE_MS;
    }

    private void positionMiss() {
        positionMisses++;
        if (positionMisses >= 2) steadySince = 0;
    }

    private void noOutline(String why) {
        if (DEBUG_LOGS && phase != Phase.SWEEP && phase != Phase.DONE) Log.d("IdLivenessDebug", "no outline (" + phase + "): " + why);
    }

    private void useRawFrame(double[] model, Bitmap upright, double w, double h, boolean inferred) {
        if (inferred) return;                                       // a guessed corner (finger on the edge) fakes a tilt
        if (!validQuad(model, w, h, TILT_OVERFLOW)) return;
        Pose p = Geometry.poseOf(model);
        if (Math.abs(p.getLr()) > RAW_SKEW_MAX || Math.abs(p.getTb()) > RAW_SKEW_MAX || (p.getLr() == 0 && p.getTb() == 0)) return;
        lastUpright = upright;
        lastQuad = model;
        lastPixelQuad = model;
        lastRawQuad = model;
        lastPose = p;
        lastRawPose = p;
        lastIsRaw = true;
    }

    /** geometry.ts isValidQuad with `outside` (tiltOverflow during the tilt). */
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
        if (!pos && !neg) return false;
        double area = 0;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            area += q[2 * i] * q[2 * j + 1] - q[2 * j] * q[2 * i + 1];
        }
        return Math.abs(area) / 2 >= MIN_QUAD_AREA_FRAC * w * h;
    }

    /** The raw pose on the tilt axis of this card (lr for the ID front, tb for the licence / permit). */
    private double axisOf(Pose p) {
        return axisLr ? p.getLr() : p.getTb();
    }

    private static double bottomAngle(double[] q) {
        double[] o = orderCorners(q);
        if (o == null) return Double.NaN;
        return Math.atan2(o[5] - o[7], o[4] - o[6]);
    }

    private static double[] orderCorners(double[] q) {
        int tl = 0, tr = 0, br = 0, bl = 0;
        double minSum = Double.MAX_VALUE, maxSum = -Double.MAX_VALUE, minDiff = Double.MAX_VALUE, maxDiff = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double x = q[2 * i], y = q[2 * i + 1], sum = x + y, diff = x - y;
            if (sum < minSum) { minSum = sum; tl = i; }
            if (sum > maxSum) { maxSum = sum; br = i; }
            if (diff > maxDiff) { maxDiff = diff; tr = i; }
            if (diff < minDiff) { minDiff = diff; bl = i; }
        }
        if (tl == tr || tl == br || tl == bl || tr == br || tr == bl || br == bl) return null;
        return new double[] {q[2 * tl], q[2 * tl + 1], q[2 * tr], q[2 * tr + 1], q[2 * br], q[2 * br + 1], q[2 * bl], q[2 * bl + 1]};
    }

    private static double dist(double[] o, int a, int b) {
        return Math.hypot(o[2 * a] - o[2 * b], o[2 * a + 1] - o[2 * b + 1]);
    }

    private static Bitmap scaleDown(Bitmap src, int longEdge) {
        int w = src.getWidth(), h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= longEdge) return src;
        double sc = longEdge / (double) max;
        return LivenessVision.areaResize(src, (int) Math.round(w * sc), (int) Math.round(h * sc));
    }

    private static Bitmap fitLongSide(Bitmap src, int maxSide) {
        int w = src.getWidth(), h = src.getHeight();
        int max = Math.max(w, h);
        if (maxSide <= 0 || max <= maxSide) return src;
        double sc = maxSide / (double) max;
        return Bitmap.createScaledBitmap(src, (int) Math.round(w * sc), (int) Math.round(h * sc), true);
    }

    /** This frame's card straightened to 480 x H with the baseline's corner roll. */
    private int[] rectifyNow() {
        if (lastPixelQuad == null || lastUpright == null) return null;
        double[] o = orderCorners(lastPixelQuad);
        if (o == null) return null;
        return LivenessVision.rectify(lastUpright, LivenessVision.roll(o, baseK), LivenessVision.OPT_W, RECT_H);
    }

    /** checks/overlay-cut-edge.md: the cut-edge score of the card in this FULL camera frame (ID front only). */
    private void measureCut(String when) {
        if (!isFront || curFrame == null || lastPixelQuad == null || lastUpright == null) return;
        try {
            double[] o = orderCorners(lastPixelQuad);
            if (o == null) return;
            double[] q = LivenessVision.roll(o, baseK);               // top left (the emblem's corner) first, clockwise
            Bitmap full = ImageUtils.rotateBitmap(curFrame, frameRotationDegrees());   // upright, full size
            double sc = full.getWidth() / (double) lastUpright.getWidth();
            double[] fq = new double[8];
            for (int i = 0; i < 8; i++) fq[i] = q[i] * sc;
            double score = LivenessVision.cutEdgeOf(full, fq);
            cutReadings.add(score);
            if (DEBUG_LOGS) Log.d("IdLivenessDebug", String.format(Locale.US, "overlay cut edge (%s): %.2f", when, score));
        } catch (Exception e) {
            if (DEBUG_LOGS) Log.d("IdLivenessDebug", "overlay cut edge (" + when + ") failed: " + e);
        }
    }

    // =============================================================================================
    // Phases
    // =============================================================================================

    private void updateLivenessColor() {
        String color = livenessFrameGood ? ConstantsValues.DetectColor : environmentalConditions.getHoldHandColor();
        setRectFCustomColor(color, environmentalConditions.getEnableDetect(), environmentalConditions.getEnableGuide(),
                start && !tiltStart && !barcodeLayoutShown);        // hidden while a drawing (tilt / barcode) is shown
    }

    /** The tilt drawing for the asked edge: right (ID front), top or bottom (licence / permit). */
    private void showTiltLayout() {
        hideTiltLayouts();
        if ("right".equals(direction)) {
            showTiltIDRightLayout();
            tiltStart = true;
        } else if ("top".equals(direction)) {
            showTiltIDTopLayout();
            tiltStart = true;
        } else if ("bottom".equals(direction)) {
            showTiltIDBottomLayout();
            tiltStart = true;
        }
    }

    private void hideTiltLayouts() {
        hideTiltIDTopLayout();
        hideTiltIDRightLayout();
        hideTiltIDBottomLayout();
        tiltStart = false;
    }

    /**
     * ID back: the barcode drawing (showBarCodeGifLayout) while the barcode is being read after the photo.
     * Shown once per BARCODE phase; the frame is hidden under it (updateLivenessColor).
     */
    private void showBarcodeLayout() {
        if (barcodeLayoutShown) return;                             // already on screen
        barcodeLayoutShown = true;
        showBarCodeGifLayout();
    }

    /** Removes the barcode drawing: read / not read in time / a restart / a retry / the background / a stall / close. */
    private void hideBarcodeLayout() {
        barcodeLayoutShown = false;
        hideBarCodeGifLayout();                                     // safe when it is not shown
    }

    private void runLiveness(boolean accepted, Bitmap normalImage) {
        long now = SystemClock.elapsedRealtime();
        if (phase == Phase.DONE) return;

        if (pause != null) {
            if (!pauseDone(now)) {
                hint(pause);
                return;
            }
            pause = null;
            markTurn();
            flatSince = 0;
            hint(IdLivenessInstruction.PLACE_CARD);
        }
        if (notInHand(now)) return;

        if (lastQuad != null) lastSeen = now;
        if (lastQuad == null && lastSeen > 0 && now - lastSeen > LOSS_MS) livenessFrameGood = false;

        boolean watchedLoss = phase == Phase.BASELINE || phase == Phase.LIGHT;
        boolean watchedTime = watchedLoss || phase == Phase.SWEEP;
        if (phase == Phase.SWEEP && now - lastSeen > LOSS_MS) hint(IdLivenessInstruction.PLACE_CARD);
        long lossGrace = phase == Phase.LIGHT ? (long) (LOSS_MS * LIGHT_LOSS_FACTOR) : LOSS_MS;
        if (watchedLoss && lastSeen > 0 && now - lastSeen > lossGrace) {
            restartTry("tracking_lost", now);
            return;
        }
        if (watchedTime && now - phaseStart > PHASE_TIMEOUT_MS) {
            engineAttempts++;
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
                hint(IdLivenessInstruction.PLACE_CARD);
                if (lastQuad != null) livenessFrameGood = true;
                if (accepted && photoReady(now)) {
                    enterPhase(Phase.BASELINE, now);
                    playSuccess();                                  // seen and steady: the flat hold starts
                }
                break;
            case BASELINE:
                if (isBack) readBarcode(now);                       // the back held still: try the barcode
                if (checkBaseline(now)) baselineDone(normalImage, now);
                break;
            case BARCODE:
                checkBarcodeWait(now);
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

    /** The flat hold ended: THE photo, the unlit reference (and its corner roll), the flat cut-edge reading. */
    private void baselineDone(Bitmap normalImage, long now) {
        flatPhoto = normalImage;                                    // flat, unlit, full camera size
        if (isBack) {                                               // back: no tilt; the photo is taken before the light
            if (barcodeRead) {
                playSuccess();
                finishTry(false);
                return;
            }
            // not read yet: the light on (it helps the barcode), wait up to idBarcode.waitMs while it is read
            enterPhase(Phase.BARCODE, now);
            lightRequestedAt = now;
            lightAvailable = setTorch(true);
            if (lightAvailable) lightState = Light.ON;
            showBarcodeLayout();                                    // the barcode drawing while it is read
            hint(IdLivenessInstruction.READING_BARCODE);
            if (DEBUG_LOGS) Log.d("IdBarcode", "photo taken, barcode not read yet: light on, waiting up to "
                    + BARCODE_WAIT_MS + " ms");
            return;
        }
        double[] o = lastPixelQuad != null ? orderCorners(lastPixelQuad) : null;
        LivenessVision.Baseline b = null;
        if (o != null) {
            b = isFront && orientRoi != null
                    ? LivenessVision.makeBaselineOriented(lastUpright, o, RECT_H, orientRoi, staticFeature)
                    : LivenessVision.makeBaseline(lastUpright, o, RECT_H);
        }
        if (b == null) {                                            // no usable outline on this frame: hold again
            resetBaselineHold();
            return;
        }
        baseK = b.k;
        unlitImg = b.img;
        measureCut("flat");
        enterPhase(Phase.LIGHT, now);
        lightRequestedAt = now;
        lightAvailable = setTorch(true);
        if (lightAvailable) lightState = Light.ON;
        hint(IdLivenessInstruction.DO_NOT_MOVE);
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", "baseline done: k=" + baseK + " torch=" + lightAvailable
                + String.format(Locale.US, " tiltMin=%.3f", tiltMinEffective));
    }

    // ---- ID back: the PDF417 barcode (recorded only) ----

    /** After the back's photo: keep reading while the card is held, the light on for a while, then go on read or not. */
    private void checkBarcodeWait(long now) {
        hint(barcodeRead ? IdLivenessInstruction.BARCODE_READ : IdLivenessInstruction.READING_BARCODE);
        if (lastQuad != null) livenessFrameGood = true;
        readBarcode(now);
        long waited = now - phaseStart;
        if (lightState == Light.ON && waited > LIGHT_MAX_MS + BARCODE_LIGHT_MS) lightOff(now);   // light.barcodeMs
        if (barcodeRead || waited > BARCODE_WAIT_MS) {
            if (DEBUG_LOGS && !barcodeRead) Log.d("IdBarcode", "not read after " + waited + " ms (" + barcodeAttempts
                    + " attempts): going on (recorded only)");
            hideBarcodeLayout();                                    // read or not: the drawing goes
            playSuccess();
            finishTry(false);
        }
    }

    /** One reading of the barcode every idBarcode.everyMs, in the background, on the card in the FULL frame. */
    private void readBarcode(long now) {
        if (!isBack || barcodeRead || barcodeBusy || now - lastBarcodeAt < BARCODE_EVERY_MS) return;
        if (curFrame == null || lastPixelQuad == null || lastUpright == null || lastIsRaw) return;   // held still only
        final double[] o = orderCorners(lastPixelQuad);
        if (o == null) return;
        lastBarcodeAt = now;
        barcodeBusy = true;
        barcodeAttempts++;
        final Bitmap frame = curFrame;
        final double anaW = lastUpright.getWidth();
        final int token = barcodeToken;
        final int rotation = frameRotationDegrees();
        try {
            barcodeExecutor.execute(() -> {
                try {
                    Bitmap full = ImageUtils.rotateBitmap(frame, rotation);      // upright, full size
                    double sc = full.getWidth() / anaW;
                    double[] q = new double[8];
                    for (int i = 0; i < 8; i++) q[i] = o[i] * sc;                // the card in the full frame, TL first
                    IdBackBarcode.Gray g = IdBackBarcode.toGray(full);
                    List<IdBackBarcode.Gray> tries = IdBackBarcode.tries(g, q, IdBackBarcode.TRY_WIDTH);
                    String[] names = {"lower band", "lower band sharpened", "lower band thresholded",
                            "upper band", "upper band sharpened", "upper band thresholded"};
                    for (int t = 0; t < tries.size(); t++) {
                        if (token != barcodeToken || barcodeRead) return;
                        byte[] raw = scanPdf417(IdBackBarcode.toBitmap(tries.get(t)));
                        if (raw != null && raw.length > 0) {
                            if (token != barcodeToken) return;
                            barcodeRead = true;
                            printBarcode(raw, names[t]);
                            return;
                        }
                    }
                } catch (Exception e) {
                    if (DEBUG_LOGS) Log.d("IdBarcode", "reading failed: " + e);
                } finally {
                    barcodeBusy = false;
                }
            });
        } catch (Exception e) {                                     // the executor is shut down (screen closed)
            barcodeBusy = false;
        }
    }

    /** ML Kit PDF417 on one image (runs on the barcode thread): the raw bytes of the first non-empty code, or null. */
    @Nullable
    private byte[] scanPdf417(Bitmap image) throws Exception {
        if (barcodeScanner == null) {
            barcodeScanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_PDF417).build());
        }
        List<Barcode> found = Tasks.await(barcodeScanner.process(InputImage.fromBitmap(image, 0)));
        for (Barcode b : found) {
            byte[] raw = b.getRawBytes();
            if (raw != null && raw.length > 0) return raw;
        }
        return null;
    }

    /** Prints what the back's barcode holds (id-back.ts): the card number and the text fields. */
    private void printBarcode(byte[] raw, String fromTry) {
        String text = IdBackBarcode.decode(raw);
        IdBackBarcode.Content c = IdBackBarcode.read(text);
        readNumber = c != null ? c.number : null;                   // for the check against the front's number
        Log.d("IdBarcode", "===== ID back barcode READ (" + fromTry + ", attempt " + barcodeAttempts + ", "
                + raw.length + " bytes) =====");
        if (c == null) {
            Log.d("IdBarcode", "no comma-separated fields; text: " + text);
        } else {
            Log.d("IdBarcode", "number: " + c.number);
            List<String> textFields = c.textFields();
            for (int i = 0; i < textFields.size(); i++) Log.d("IdBarcode", "field " + (i + 1) + ": " + textFields.get(i));
            Log.d("IdBarcode", "fields: " + c.fields.size() + " in total, " + textFields.size()
                    + " text (the others are binary data)");
        }
        Log.d("IdBarcode", "raw (base64): " + IdBackBarcode.toBase64(raw));
    }

    private void enterPhase(Phase p, long now) {
        phase = p;
        phaseStart = now;
        if (p != Phase.SWEEP && p != Phase.DONE) setProgress(0);
    }

    private boolean photoReady(long now) {
        return lightState == Light.OFF && now - darkAt >= OFF_SETTLE_MS;
    }

    private void lightOff(long now) {
        if (lightState == Light.OFF) return;
        if (lightState == Light.ON) offAskedAt = now;
        lightState = Light.GOING_OFF;
        offLastAskAt = now;
        setTorch(false);
    }

    private void pollLight(long now) {
        if (lightState != Light.GOING_OFF) return;
        if (!isTorchOn() || now - offAskedAt >= OFF_MAX_MS) {
            lightState = Light.OFF;
            darkAt = now;
            return;
        }
        if (now - offLastAskAt >= OFF_REASK_MS) {
            offLastAskAt = now;
            setTorch(false);
        }
    }

    // ---- baseline: frontal, sharp, steady, no glare for baselineHoldMs and >= 3 poses ----

    private boolean checkBaseline(long now) {
        if (lastQuad == null || lastIsRaw) {
            havePrev = false;
            holdMiss();
            return false;
        }
        double lr = lastPose.getLr(), tb = lastPose.getTb();
        double cx = (lastQuad[0] + lastQuad[2] + lastQuad[4] + lastQuad[6]) / 4;
        double cy = (lastQuad[1] + lastQuad[3] + lastQuad[5] + lastQuad[7]) / 4;
        double fw = lastUpright.getWidth();
        boolean centreStill = havePrev && Math.hypot(cx - prevCx, cy - prevCy) < DRIFT_MAX * fw;
        boolean poseStill = baseCount == 0
                ? havePrev && Math.abs(lr - prevLr) < HOLD_STILL_TOL && Math.abs(tb - prevTb) < HOLD_STILL_TOL
                : Math.abs(lr - holdLr) <= HOLD_STILL_TOL && Math.abs(tb - holdTb) <= HOLD_STILL_TOL;
        boolean steady = centreStill && poseStill;
        prevLr = lr; prevTb = tb; prevCx = cx; prevCy = cy; havePrev = true;

        boolean frontal = Math.abs(lr) <= HOLD_FRONTAL_MAX && Math.abs(tb) <= HOLD_FRONTAL_MAX;
        boolean glare = lastGlareFrac > HOLD_GLARE_MAX;
        if (!frontal) hint(IdLivenessInstruction.KEEP_FLAT);
        else if (glare) hint(IdLivenessInstruction.ADJUST_POSITION);
        else hint(IdLivenessInstruction.KEEP_STEADY);

        if (DEBUG_LOGS) Log.d("IdLivenessDebug", String.format(Locale.US,
                "baseline lr=%.3f tb=%.3f steady=%b hold=%d sharp=%.1f(%b) glare=%.3f(%b) frontal=%b",
                lr, tb, steady, baseCount, lastSharpness, lastSharp, lastGlareFrac, glare, frontal));

        if (!steady || !lastSharp || !frontal || glare || !photoReady(now)) {
            holdMiss();
            return false;
        }
        livenessFrameGood = true;
        holdGraceUsed = false;
        if (baseHoldSince == 0) {
            baseHoldSince = now;
            holdLr = lr;
            holdTb = tb;
        }
        baseCount++;
        if (lastRawPose != null) baselineAxis.add(axisOf(lastRawPose));
        if (lastRawQuad != null) {
            double a = bottomAngle(lastRawQuad);
            if (!Double.isNaN(a)) baselineAngles.add(a);
        }
        if (now - baseHoldSince >= BASELINE_HOLD_MS && baseCount >= BASELINE_MIN_POSES) {
            baseAngle = baselineAngles.isEmpty() ? Double.NaN : median(baselineAngles);
            tiltMinEffective = Math.max(TILT_MIN, TILT_NOISE_K * popStd(baselineAxis));
            return true;
        }
        return false;
    }

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
        baselineAxis.clear();
        baselineAngles.clear();
    }

    // ---- light: lit frames against the unlit reference; the ID front's emblem is read here; then the lit reference ----

    private void checkLight(long now) {
        hint(IdLivenessInstruction.DO_NOT_MOVE);
        if (lightAvailable && !isTorchOn() && now - lightRequestedAt > TORCH_CONFIRM_MS) {
            lightAvailable = false;
            lightOff(now);
        }
        if (!lightAvailable) {                                      // no light: the unlit frame stays the reference
            litImg = unlitImg;
            iridBase = LivenessVision.iridBase(litImg, LivenessVision.OPT_W, RECT_H);
            axisRef = rawReference(lightAxis, baselineAxis);
            goToSweep(now);
            return;
        }
        long waited = now - lightRequestedAt;
        if (!lightEnded) {
            if (waited >= LIGHT_SETTLE_MS && lastPixelQuad != null) {
                int[] cur = rectifyNow();
                if (cur != null) {
                    double[] sample = LivenessVision.pageResponse(unlitImg, cur, LivenessVision.OPT_W, RECT_H);
                    reflection.addLit(sample);
                    lightSharp++;
                    livenessFrameGood = true;
                    if (lastRawPose != null) lightAxis.add(axisOf(lastRawPose));
                    if (isFront && emblemCell != null) {
                        double[] e = LivenessVision.emblemReading(unlitImg, cur, LivenessVision.OPT_W, RECT_H,
                                emblemCell, emblemInner, emblemOuter);
                        if (e != null) emblemLit.add(e);
                    }
                    if (!litEarlyCut) {                             // overlay: the first lit frame ("lit early")
                        litEarlyCut = true;
                        measureCut("lit early");
                    }
                    if (DEBUG_LOGS && sample != null) Log.d("IdLivenessDebug", String.format(Locale.US,
                            "light frame spec=%.4f flare=%.5f veil=%.3f emblem=%s", sample[0], sample[2], sample[3],
                            emblemLit.isEmpty() ? "-" : String.format(Locale.US, "%.1f/%.1f",
                                    emblemLit.get(emblemLit.size() - 1)[0], emblemLit.get(emblemLit.size() - 1)[1])));
                }
            }
            boolean whitedOut = reflection.usable() < cfg.reflection.minLitFrames;
            boolean normalEnd = (lightSharp >= LIGHT_FRAMES && !whitedOut) || waited > LIGHT_MAX_MS;
            // capture.ts:1386: the ID front keeps the light on while the emblem has not answered (up to emblem.extraMs)
            boolean waitEmblem = isFront && emblemCell != null && lightSharp >= LIGHT_FRAMES
                    && waited <= LIGHT_MAX_MS + cfg.emblem.extraMs
                    && !"present".equals(LivenessGate.emblemResult(emblemLit, true, null, true, cfg.emblem).state);
            if (normalEnd && !waitEmblem) {
                lightEnded = true;
                tooBright(now);
            }
            return;
        }
        if (pause != null || phase != Phase.LIGHT) return;
        int[] lit = rectifyNow();
        if (lit == null) return;
        litImg = lit;
        iridBase = LivenessVision.iridBase(litImg, LivenessVision.OPT_W, RECT_H);
        axisRef = rawReference(lightAxis, baselineAxis);
        measureCut("light end");
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", String.format(Locale.US, "light done: lit=%d usable=%d axisRef=%.3f",
                lightSharp, reflection.usable(), axisRef));
        goToSweep(now);
    }

    private boolean tooBright(long now) {
        if (sunPauses >= SUN_MAX || !lightAvailable || isBatteryTooLow()) return false;
        LivenessGate.ReflectionResult r = reflection.result();
        double lift = r.litLift != null ? r.litLift : 9;
        boolean brightAgain = brightMiss && lift <= BRIGHT_LIFT;
        if (!"enforce".equals(cfg.reflection.policy) || !("emissive".equals(r.state) || brightAgain)) return false;
        sunPauses++;
        brightMiss = false;
        startPause(IdLivenessInstruction.TOO_BRIGHT, "too_bright", now);
        return true;
    }

    private static double rawReference(List<Double> light, List<Double> baseline) {
        if (light.size() >= 2) return median(light);
        if (!baseline.isEmpty()) return median(baseline);
        return Double.NaN;
    }

    private static double popStd(List<Double> v) {
        if (v.size() < 2) return 0;
        double m = 0;
        for (double x : v) m += x;
        m /= v.size();
        double sq = 0;
        for (double x : v) sq += (x - m) * (x - m);
        return Math.sqrt(sq / v.size());
    }

    private static double median(List<Double> values) {
        List<Double> v = new ArrayList<>(values);
        java.util.Collections.sort(v);
        int n = v.size();
        return n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2.0;
    }

    // ---- sweep: ID front the RIGHT edge (lr, sign -1); licence / permit top (+1) or bottom (-1) on tb ----

    private void goToSweep(long now) {
        resetSweep();
        markTurn();
        if (isFront) {
            direction = "right";                                    // emblem.direction, forced
            axisLr = true;
            sign = -1;
        } else {
            direction = rng.nextDouble() < 0.5 ? "top" : "bottom";  // challenge-engine.ts:313
            axisLr = false;
            sign = "top".equals(direction) ? 1 : -1;
        }
        lastMoveAt = now;
        enterPhase(Phase.SWEEP, now);
        showTiltLayout();
        hint(liftHint());
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", "tilt: lift the " + direction + " edge");
    }

    private IdLivenessInstruction liftHint() {
        return "right".equals(direction) ? IdLivenessInstruction.LIFT_RIGHT
                : "top".equals(direction) ? IdLivenessInstruction.LIFT_TOP : IdLivenessInstruction.LIFT_BOTTOM;
    }

    private void checkSweep(long now) {
        if (lastQuad == null) return;
        if (!isSameDocument(now)) {
            restartTry("continuity_lost", now);
            return;
        }
        double v = axisOf(lastPose);
        if (Double.isNaN(axisRef)) axisRef = v;                     // no reference yet: this frame is it
        double need = tiltMinEffective;
        double d = sign * (v - axisRef);                            // toward: + = the asked edge lifted toward the camera
        if (Math.abs(d) <= SWEEP_FLAT_EPS) sweepSawFlat = true;
        if (Math.abs(d - prevDelta) > MOVE_EPS) lastMoveAt = now;
        prevDelta = d;
        sweepMoved = Math.max(sweepMoved, Math.abs(d));
        if (Double.isNaN(meterFrom)) meterFrom = lastPose.getLr();

        // this frame's card readings
        int[] cur = litImg != null ? rectifyNow() : null;
        if (cur != null && isFront && emblemCell != null) {
            double[] e = LivenessVision.emblemReading(litImg, cur, LivenessVision.OPT_W, RECT_H, emblemCell, emblemInner, emblemOuter);
            emblemTilt.add(new double[] {lastPose.getLr(), e != null ? e[0] : Double.NaN});
        }
        if (cur != null && isLaminateCard) {
            double[] irid = LivenessVision.iridescence(iridBase, LivenessVision.chromaGrid(cur, LivenessVision.OPT_W, RECT_H));
            if (irid != null) iridTilt.add(irid);
        }

        double angleNow = lastRawQuad != null ? bottomAngle(lastRawQuad) : Double.NaN;
        boolean turned = false;
        if (!Double.isNaN(angleNow) && !Double.isNaN(baseAngle)) {
            double t = angleNow - baseAngle;
            turned = Math.abs(Math.atan2(Math.sin(t), Math.cos(t))) > ANGLE_MAX;
        }
        if (Double.isNaN(sweepFrom)) sweepFrom = v;
        sweepWrong = sign * (v - sweepFrom) <= -need ? sweepWrong + 1 : 0;
        boolean hit = !turned && d >= need && sweepSawFlat;
        sweepGood = hit ? sweepGood + 1 : 0;
        if (hit) livenessFrameGood = true;
        if (sweepGood >= SWEEP_FRAMES && reachedAt == 0) reachedAt = now;

        // tilt meter (capture.ts:331): ID front from the starting lr to tiltTarget; licence tilt + colours (tiltShare)
        double meter;
        LivenessGate.LaminateResult lam = isLaminateCard ? LivenessGate.iridescenceResult(iridTilt, cfg.laminate) : null;
        if (isFront) {
            meter = (meterFrom - lastPose.getLr()) / Math.max(0.08, meterFrom + cfg.emblem.tiltTarget);
        } else {
            double tiltPart = reachedAt != 0 ? 1 : (sweepSawFlat ? d / need : 0);
            meter = TILT_SHARE * Math.max(0, Math.min(1, tiltPart)) + (1 - TILT_SHARE) * colourProgress(lam);
        }
        setProgress((int) Math.round(100 * Math.max(0, Math.min(1, meter))));

        if (DEBUG_LOGS) Log.d("IdLivenessDebug", String.format(Locale.US,
                "tilt %s v=%.3f ref=%.3f toward=%.3f need=%.3f turned=%b good=%d%s", direction, v, axisRef, d, need, turned,
                sweepGood, lam != null ? String.format(Locale.US, " | colours frames=%d hues=%d peak=%.4f strong=%b",
                        lam.frames, lam.hues, lam.peakOr0(), lam.strong)
                        : isFront && !emblemTilt.isEmpty() ? String.format(Locale.US, " | emblem %.1f", emblemTilt.get(emblemTilt.size() - 1)[1]) : ""));

        if (reachedAt != 0) {
            // held open while the reading is not there yet: the emblem lit but not dark yet / the colours not strong yet
            boolean awaiting;
            if (isFront) {
                awaiting = emblemCell != null && "untested".equals(LivenessGate.emblemResult(
                        emblemLit, true, emblemWhenTilted(), false, cfg.emblem).state);
            } else {
                boolean coloursDecide = "enforce".equals(lightAvailable ? cfg.laminatePolicy() : "shadow");
                awaiting = cfg.profile.iridescent && coloursDecide && lam != null && !lam.strong;
            }
            if (awaiting && now - reachedAt < HOLD_MS) {
                livenessFrameGood = true;
                hint(atEdge() ? IdLivenessInstruction.HOLD_THERE : IdLivenessInstruction.A_BIT_MORE);
                return;
            }
            measureCut("tilt end");
            playSuccess();
            finishTry(false);
            return;
        }

        if (sweepWrong >= WRONG_WAY_FRAMES) hint(IdLivenessInstruction.OTHER_EDGE);
        else if (now - phaseStart > STUCK_MS && sweepMoved < need / 2) hint(IdLivenessInstruction.PICK_UP);
        else if (d >= need && !sweepSawFlat) hint(IdLivenessInstruction.KEEP_FLAT);
        else hint(liftHint());
    }

    /** capture.ts:1318 emblemWhenTilted: the emblem where the right edge is toward the camera (lr <= -tiltSide), else NaN. */
    private double[] emblemWhenTilted() {
        double[] out = new double[emblemTilt.size()];
        for (int i = 0; i < out.length; i++) {
            double[] row = emblemTilt.get(i);
            out[i] = row[0] <= -cfg.emblem.tiltSide ? row[1] : Double.NaN;
        }
        return out;
    }

    /** capture.ts:1321 emblemTested: minTiltFrames rows in a row with lr <= -testTilt. */
    private boolean emblemTested() {
        int run = 0;
        for (double[] row : emblemTilt) {
            run = row[0] <= -cfg.emblem.testTilt ? run + 1 : 0;
            if (run >= cfg.emblem.minTiltFrames) return true;
        }
        return false;
    }

    private double colourProgress(@Nullable LivenessGate.LaminateResult r) {
        if (r == null || r.peak == null) return 0;
        LivenessConfig.Laminate c = cfg.laminate;
        double p = Math.min(Math.min(r.hues / (double) c.strongHues, r.peak / c.strongPeak),
                Math.min(r.litFrames / (double) c.minLitFrames, r.frames / (double) c.minFrames));
        return Math.max(0, Math.min(1, p));
    }

    private boolean atEdge() {
        if (lastQuad == null || lastUpright == null) return false;
        double w = lastUpright.getWidth(), h = lastUpright.getHeight();
        for (int i = 0; i < 4; i++) {
            double x = lastQuad[2 * i], y = lastQuad[2 * i + 1];
            if (x < EDGE_MARGIN * w || x > (1 - EDGE_MARGIN) * w || y < EDGE_MARGIN * h || y > (1 - EDGE_MARGIN) * h) return true;
        }
        return false;
    }

    private boolean isSameDocument(long now) {
        double[] o = orderCorners(lastQuad);
        if (o == null || lastUpright == null) return true;
        double fw = lastUpright.getWidth(), fh = lastUpright.getHeight();
        double bx = (o[4] + o[6]) / 2 / fw, by = (o[5] + o[7]) / 2 / fh, len = dist(o, 2, 3) / fw;
        boolean same = true;
        if (sweepRefSet && now - sweepLastSeenAt > LOSS_MS) {
            double maxLen = Math.sqrt(AREA_JUMP_MAX), ratio = len / Math.max(sweepRefLen, 1e-6);
            same = Math.hypot(bx - sweepRefX, by - sweepRefY) <= SAME_PLACE_MAX && ratio <= maxLen && ratio >= 1 / maxLen;
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
        prevDelta = 0;
        sweepMoved = 0;
        sweepFrom = Double.NaN;
        meterFrom = Double.NaN;
        lastMoveAt = 0;
        reachedAt = 0;
        iridTilt.clear();
        emblemTilt.clear();
        shownInstruction = null;
    }

    // =============================================================================================
    // Verdict
    // =============================================================================================

    private void finishTry(boolean engineFailed) {
        long now = SystemClock.elapsedRealtime();
        lightOff(now);
        hideTiltLayouts();
        hideBarcodeLayout();
        phase = Phase.DONE;

        LivenessGate.GateInput in = new LivenessGate.GateInput();
        in.cameraOk = true;
        in.enginePhase = engineFailed ? "failed" : "done";
        in.attempt = engineAttempts + retriesThisVisit;
        in.challenge = engineFailed ? "not_observed" : "observed";
        in.optical = "not_configured";                              // optical cedar check: recorded only
        in.opticalPolicy = "shadow";
        in.matteBefore = matteBefore;
        in.matteNotEvidence = cfg.profile.page;
        in.batteryLow = isBatteryTooLow();
        in.photoOk = engineFailed ? null : flatPhoto != null;

        String detail;
        LivenessGate.ReflectionResult refl = reflection != null ? reflection.result() : null;
        if (isBack) {
            // the back: the photo only. Its reflection policy is back.policy (shadow) and no light was used.
            in.reflection = "unassessable";
            in.reflectionPolicy = cfg.back.policy;
            in.lightAvailable = null;
            detail = "back: flat photo only";
        } else {
            in.reflection = refl.state;
            in.reflectionPolicy = cfg.reflection.policy;
            in.lightAvailable = lightAvailable;
            if (isFront) {
                LivenessGate.EmblemResult em = LivenessGate.emblemResult(emblemLit, emblemCell != null,
                        emblemWhenTilted(), emblemTested(), cfg.emblem);
                in.emblem = em.state;
                in.emblemRequired = emblemCell != null;
                in.emblemPolicy = lightAvailable ? cfg.emblem.policy : "shadow";
                // overlay: the second highest of two or more readings against barSeveral, one against bar
                List<Double> all = new ArrayList<>(cutReadings);
                java.util.Collections.sort(all, java.util.Collections.reverseOrder());
                Double cut = all.size() >= 2 ? all.get(1) : all.size() == 1 ? all.get(0) : null;
                in.cutEdge = cut == null ? "unassessable"
                        : cut > (all.size() >= 2 ? cfg.overlay.barSeveral : cfg.overlay.bar) ? "cut" : "clear";
                in.cutEdgePolicy = cfg.overlay.policy;
                detail = String.format(Locale.US, "emblem=%s (lit=%s jump=%s low=%s frames=%d) cut=%s %s",
                        em.state, em.lit, em.jump, em.low, em.frames, in.cutEdge, all);
            } else {
                LivenessGate.LaminateResult lam = LivenessGate.iridescenceResult(iridTilt, cfg.laminate);
                in.laminate = lam.state;
                in.laminatePolicy = lightAvailable ? cfg.laminatePolicy() : "shadow";
                in.laminateStrong = lam.strong;
                in.laminateRequired = cfg.profile.iridescent;
                detail = String.format(Locale.US, "laminate=%s (frames=%d lit=%d hues=%d peak=%.4f strong=%b) tilt=%s",
                        lam.state, lam.frames, lam.litFrames, lam.hues, lam.peakOr0(), lam.strong, direction);
            }
        }

        LivenessGate.Decision decision = LivenessGate.decide(in, MAX_ATTEMPTS);
        List<String> reasons = new ArrayList<>(decision.reasons);
        reasons.addAll(tryReasons);
        reasons.addAll(flags);
        if (isBack) reasons.add(barcodeRead ? "back_barcode_read" : "back_barcode_unread");   // recorded only
        Log.d("IdLiveness", String.format(Locale.US, "VERDICT %s %s %s | kind=%s reflection=%s %s attempt=%d",
                decision.verdict, reasons, "", cfgKind, refl != null ? refl.state + " (lit=" + refl.usable + " flare="
                        + refl.litFlare + " lift=" + refl.litLift + ")" : "-", detail, in.attempt));

        boolean first = false, neutral = false;
        for (String r : decision.reasons) {
            if (r.endsWith("_first")) first = true;
            if (r.equals("emblem_untested") || r.equals("no_response_to_light") || r.equals("reflection_matte_unproven")) neutral = true;
        }
        matteBefore = first || (matteBefore && neutral);
        double lift = refl != null && refl.litLift != null ? refl.litLift : 9;
        brightMiss = LivenessGate.RETRY.equals(decision.verdict) && lift <= BRIGHT_LIFT && cfg.profile.page;
        if (!LivenessGate.CONTINUE.equals(decision.verdict)) livenessFrameGood = false;

        // the ID back: not read -> it goes on (recorded only, as the reference); read -> its number must be the front's
        // (now, when the front is already done; otherwise when the front's answer arrives)
        boolean barcodeShown = false;
        if (isBack && LivenessGate.CONTINUE.equals(decision.verdict)) {
            if (!barcodeRead || readNumber == null) {
                backNumber = null;                                  // nothing to compare with the front
                Log.d("IdBarcode", "back accepted WITHOUT the barcode (not read): no number check");
            } else if (frontNumber != null && !sameIdNumber(readNumber, frontNumber)) {
                Log.d("IdBarcode", "back REJECTED: barcode number " + readNumber + " is not the front's document number "
                        + frontNumber + " -> onRetry");
                livenessFrameGood = false;
                start = false;
                notifyRetry();
                return;
            } else {
                backNumber = readNumber;                            // this back passes: its number is kept
                barcodeShown = true;
                Log.d("IdBarcode", "back accepted, number " + backNumber
                        + (frontNumber != null ? " = the front's " + frontNumber : " (the front is not done yet: checked then)"));
            }
        }

        switch (decision.verdict) {
            case LivenessGate.CONTINUE:
                setProgress(100);
                hint(barcodeShown ? IdLivenessInstruction.BARCODE_READ : IdLivenessInstruction.KEEP_HOLDING);
                livenessFrameGood = true;
                finalReady = true;                                  // processImage sends the flat photo
                break;
            case LivenessGate.RETRY:
                retriesThisVisit++;
                newTry();
                break;
            default:                                                // suspected_copy / inconclusive
                start = false;
                notifyLivenessFailed();
                break;
        }
    }

    /** The ID back failed its barcode check (not read, or not the front's number): the retry screen (onRetry). */
    private void notifyRetry() {
        if (getActivity() == null || idCardCallback == null) return;
        getActivity().runOnUiThread(() -> idCardCallback.onRetry(new BaseResponseDataModel(
                "IdCard", null, EventsErrorMessages.OnRetryCardMessage, false, "", null)));
    }

    /**
     * The same ID number? Digits only (Arabic-Indic digits read as 0-9, spaces and separators dropped), and leading zeros
     * ignored: the barcode holds 12 digits ("000012345678"), the server may give the number without its zeros.
     */
    private static boolean sameIdNumber(String a, String b) {
        String x = idDigits(a), y = idDigits(b);
        return !x.isEmpty() && x.equals(y);
    }

    private static String idDigits(String s) {
        if (s == null) return "";
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') d.append(c);
            else if (c >= '\u0660' && c <= '\u0669') d.append((char) ('0' + (c - '\u0660')));   // Arabic-Indic
            else if (c >= '\u06F0' && c <= '\u06F9') d.append((char) ('0' + (c - '\u06F0')));   // Extended Arabic-Indic
        }
        int z = 0;
        while (z < d.length() - 1 && d.charAt(z) == '0') z++;
        return d.substring(z);
    }

    /** The front's IdentificationDocumentCapture.documentNumber from the server's answer, or null. */
    @Nullable
    private static String documentNumberOf(@Nullable IDExtractedModel m) {
        try {
            if (m == null || m.getIdentificationDocumentCapture() == null) return null;
            Object n = m.getIdentificationDocumentCapture().getDocumentNumber();
            String v = n == null ? null : n.toString().trim();
            return v == null || v.isEmpty() ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * suspected_copy / inconclusive / the camera stalled: the liveness screen (onLivenessUpdate, response = null), with
     * the card as the camera saw it: the flat, unlit photo if the try got that far, else the latest frame. Called
     * before the next try clears them.
     */
    private void notifyLivenessFailed() {
        if (getActivity() == null || idCardCallback == null) return;
        final Bitmap image = livenessScreenImage();
        getActivity().runOnUiThread(() -> idCardCallback.onLivenessUpdate(new BaseResponseDataModel(
                "IdCard", null, EventsErrorMessages.OnRetryCardMessage, false, "", null), image));
    }

    /** The image for the liveness screen: upright, long side at most 1080 px; null when there is no frame at all. */
    @Nullable
    private Bitmap livenessScreenImage() {
        Bitmap src = flatPhoto != null ? flatPhoto : lastFrame;
        if (src == null) return null;
        try {
            Bitmap upright = ImageUtils.rotateBitmap(src, frameRotationDegrees());   // sensor frames are landscape
            return fitLongSide(upright, LIVENESS_IMAGE_MAX_SIDE);
        } catch (Exception e) {
            return null;
        }
    }

    // =============================================================================================
    // Restarts, tries, pauses, stall, background
    // =============================================================================================

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
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", "restart: " + reason + " (early=" + earlyRestarts + " late=" + restarts + ")");
        clearReadings(now);
    }

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

    private void clearReadings(long now) {
        lightOff(now);
        hideTiltLayouts();
        if (barcodeLayoutShown) hideBarcodeLayout();
        livenessFrameGood = false;
        steadySince = 0;
        positionMisses = 0;
        lastSeen = 0;
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
        litEarlyCut = false;
        lightRequestedAt = 0;
        lightSharp = 0;
        lightAxis.clear();
        emblemLit.clear();
        cutReadings.clear();
        barcodeToken++;                                             // a reading still running belongs to the old try
        barcodeRead = false;
        lastBarcodeAt = 0;
        barcodeAttempts = 0;
        axisRef = Double.NaN;
        tiltMinEffective = TILT_MIN;
        // the licence's axis is tb, the ID front's lr: set before the baseline collects its poses
        axisLr = isFront;
        reflection = cfg != null ? new LivenessGate.ReflectionAnalyzer(cfg.reflection) : null;
        resetSweep();
        shownInstruction = null;
        stab = new OutlineStabilizer(CARD_ASPECT);
        enterPhase(Phase.POSITION, now);
    }

    private void resetLiveness() {
        if (cfg != null) newTry();
    }

    /** The liveness screen's retry button: this side starts again (the classifier decides again). */
    public void retryLiveness() {
        pendingReset = true;
        start = true;
    }

    private void hint(IdLivenessInstruction instruction) {
        if (instruction == shownInstruction && livenessProgress == shownProgress) return;
        shownInstruction = instruction;
        shownProgress = livenessProgress;
        final int progress = livenessProgress;
        if (getActivity() != null && idCardCallback != null) {
            getActivity().runOnUiThread(() -> idCardCallback.onLivenessInstruction(instruction, progress));
        }
    }

    private void setProgress(int p) {
        livenessProgress = Math.max(0, Math.min(100, p));
        if (shownInstruction != null && livenessProgress != shownProgress) hint(shownInstruction);
    }

    private void playSuccess() {
        if (audioPlayer != null) audioPlayer.playAudio(ConstantsValues.AudioCardSuccess);
    }

    private void startPause(IdLivenessInstruction card, String reason, long now) {
        restartTry(reason, now);
        if (phase == Phase.DONE) return;
        pause = card;
        pauseAt = now;
        hint(card);
    }

    private boolean pauseDone(long now) {
        long shown = now - pauseAt;
        if (shown >= PAUSE_MAX_MS) return true;
        if (shown < PAUSE_SHOW_MS) return false;
        if (pause == IdLivenessInstruction.HOLD_IN_HAND) {
            Boolean flat = phoneFlat(now);
            return flat == null || !flat;
        }
        return true;
    }

    private void checkStall() {
        if (cfg == null || livenessKind == null || !start || phase == Phase.DONE) return;
        long last = lastFrameAt;
        if (last == 0 || SystemClock.elapsedRealtime() - last <= STALL_MS) return;
        if (DEBUG_LOGS) Log.d("IdLivenessDebug", "vision_stalled: no frame for " + STALL_MS + " ms");
        setTorch(false);
        lightState = Light.OFF;
        darkAt = SystemClock.elapsedRealtime();
        hideTiltLayouts();
        hideBarcodeLayout();
        phase = Phase.DONE;
        start = false;
        notifyLivenessFailed();
    }

    private void interrupt() {
        if (cfg == null) return;
        try {
            setTorch(false);
            hideTiltLayouts();
            hideBarcodeLayout();
        } catch (Exception ignored) {
        }
        lightState = Light.OFF;
        darkAt = SystemClock.elapsedRealtime();
        pendingReset = true;
    }

    private void markTurn() {
        markX = gravX;
        markY = gravY;
        markZ = gravZ;
    }

    private double turnedSince() {
        double x = gravX, y = gravY, z = gravZ;
        if (Double.isNaN(x) || Double.isNaN(markX)) return 0;
        double dot = Math.max(-1, Math.min(1, x * markX + y * markY + z * markZ));
        return Math.toDegrees(Math.acos(dot));
    }

    /** Hand pauses (not on the back): phone flat over a table in position / baseline / light, the phone turned in the tilt. */
    private boolean notInHand(long now) {
        if (isBack || handPauses >= MAX_HAND_PAUSES) return false;
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
        if (table && !TABLE_CARD) {
            if (!flags.contains("flag_on_table")) flags.add("flag_on_table");
            flatSince = 0;
            return false;
        }
        handPauses++;
        flatSince = 0;
        String flag = table ? "flag_on_table" : "flag_phone_tilted";
        if (!flags.contains(flag)) flags.add(flag);
        markTurn();
        startPause(table ? IdLivenessInstruction.HOLD_IN_HAND : IdLivenessInstruction.PHONE_TURNED,
                table ? "on_table" : "phone_tilted", now);
        return true;
    }

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
        if (s == null) s = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (s != null) sensorManager.registerListener(gravityListener, s, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopMotion() {
        if (sensorManager != null) sensorManager.unregisterListener(gravityListener);
    }

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