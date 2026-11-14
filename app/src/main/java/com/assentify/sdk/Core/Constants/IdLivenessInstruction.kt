package com.assentify.sdk.Core.Constants

/**
 * What the user should do now during the local liveness of a CARD (Lebanese civil ID front / back, driving licence,
 * residence permit), sent with the tilt meter (0..100) through IDCardCallback.onLivenessInstruction(instruction, progress).
 * The three pause items are shown for a few seconds; the SDK continues by itself.
 */
enum class IdLivenessInstruction {
    PLACE_CARD,          // position: show the card in the frame
    KEEP_STEADY,         // baseline: hold still
    KEEP_FLAT,           // baseline: hold it flat, facing the camera
    ADJUST_POSITION,     // baseline: glare on the card, tilt it slightly
    DO_NOT_MOVE,         // light: the light is on for a few seconds (ID front: the cedar is being read)
    LIFT_RIGHT,          // tilt, ID front: lift the RIGHT edge toward you
    LIFT_TOP,            // tilt, licence / permit (random): lift the TOP edge toward you
    LIFT_BOTTOM,         // tilt, licence / permit (random): lift the BOTTOM edge toward you
    A_BIT_MORE,          // tilt reached, held open: ID front a little further (cedar), licence rock it gently (colours)
    HOLD_THERE,          // tilt held open, the card at the picture's edge
    OTHER_EDGE,          // tilted the wrong edge
    PICK_UP,             // not moving: hold the card in your hand
    KEEP_HOLDING,        // done: the check passed, the photo is being sent
    READING_BARCODE,     // ID back, after its photo: reading the barcode (msg:backBarcode); goes on read or not
    BARCODE_READ,        // ID back: the barcode was read (msg:barcodeRead)

    // ---- pauses (the SDK continues by itself) ----
    HOLD_IN_HAND,        // on a table: hold the card in your hand
    PHONE_TURNED,        // the phone turned in the tilt: move the card, not the phone
    TOO_BRIGHT ,
    SCAN_QR         // sun: too much light here

}