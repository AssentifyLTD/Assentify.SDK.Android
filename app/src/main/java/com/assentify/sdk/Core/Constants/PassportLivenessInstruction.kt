package com.assentify.sdk.Core.Constants

/**
 * What the user should do now, sent with the tilt meter (0..100) through
 * ScanPassportCallback.onLivenessInstruction(instruction, progress).
 *
 * The three PAUSE items are full-screen cards with a Continue button: every frame is dropped while one is shown,
 * and the app's Continue button must call scanPassport.continueAfterPause().
 */
enum class PassportLivenessInstruction {
    PLACE_DOCUMENT,      // position: show_document (lb: "Open the passport", other: "Show the photo page")
    KEEP_STEADY,         // baseline: hold_steady ("Hold still")
    KEEP_FLAT,           // baseline: face_camera ("Hold it flat")
    ADJUST_POSITION,     // baseline: reduce_glare ("Tilt it slightly")
    DO_NOT_MOVE,         // light: light_on ("Hold still" / "The light is on for a few seconds")
    LIFT_BOTTOM,         // tilt: tilt_bottom ("Lift bottom edge"); the meter shows how far
    MOVE_A_LITTLE_MORE,  // tilt reached, colours awaited: keep_tilting ("A bit more")
    HOLD_THERE,          // tilt reached, page at the picture's edge: holdThere ("Hold it there")
    OTHER_SIDE,          // tilt: tilt_other ("The other edge")
    HOLD_IT_UP,          // tilt: pick_up (line "Lift bottom edge", hint "Hold the open passport in your hand…")
    PLACE_FLAT_AGAIN,    // (not sent by rc17: the capture ends on the tilt; kept for compatibility)
    KEEP_HOLDING,        // done: the check passed, the photo is being sent

    // ---- pause cards (Continue button -> continueAfterPause()) ----
    HOLD_IN_HAND,        // on a table: handTablePage ("Hold the passport in your hand")
    PHONE_TURNED,        // the phone turned in the tilt: handPhone ("Move the card, not the phone")
    TOO_BRIGHT           // sun: sunPause ("Too much light here")
}