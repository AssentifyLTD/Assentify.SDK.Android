package com.assentify.sdk.Core.Constants

enum class FaceEvents {
    RollLeft,
    RollRight,
    YawLeft,
    YawRight,
    PitchUp,
    PitchDown,
    Good,
    NO_DETECT,
}

enum class ActiveLiveEvents {
    YawLeft,
    YawRight,
    PitchUp,
    PitchDown,
}

fun getRandomEvents(): Set<ActiveLiveEvents> {
    val allEvents = ActiveLiveEvents.values().toList()
    return allEvents.shuffled().take(3).toSet()
}