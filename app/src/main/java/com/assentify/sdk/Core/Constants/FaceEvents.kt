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
    Good
}

fun getRandomEvents(): Set<ActiveLiveEvents> {
    val allEvents = ActiveLiveEvents.values().filter { it != ActiveLiveEvents.Good }
    val randomEvents = mutableSetOf<ActiveLiveEvents>()

    while (randomEvents.size < 3 && randomEvents.size < allEvents.size) {
        randomEvents.add(allEvents.random())
    }

    return randomEvents
}