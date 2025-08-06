package com.assentify.sdk.Core.Constants



enum class FaceEvents {
    RollLeft,
    RollRight,
    YawLeft,
    YawRight,
    PitchUp,
    PitchDown,
    Good,
    WinkLeft,
    WinkRight,
    BLINK,
    NO_DETECT,
}

enum class ActiveLiveEvents {
    YawLeft,
    YawRight,
    PitchUp,
    PitchDown,
    WinkLeft,
    WinkRight,
    BLINK,
    Good
}

enum class ActiveLiveType{
    Actions,
    Wink,
    BLINK ,
    NONE
}

fun getRandomEvents(
    activeLiveType: ActiveLiveType,
    activeLivenessCheckCount: Int
): List<ActiveLiveEvents> {
    val allEvents = getFilteredEventsByType(activeLiveType)

    // Edge case: empty list
    if (allEvents.isEmpty()) return emptyList()

    val randomEvents = mutableListOf<ActiveLiveEvents>()
    repeat(activeLivenessCheckCount) {
        randomEvents.add(allEvents.random())
    }
    return randomEvents
}


fun getFilteredEventsByType(type: ActiveLiveType): List<ActiveLiveEvents> {
    return when (type) {
        ActiveLiveType.Actions -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good &&
                        it != ActiveLiveEvents.BLINK &&
                        it != ActiveLiveEvents.WinkLeft &&
                        it != ActiveLiveEvents.WinkRight
            }
        }

        ActiveLiveType.Wink -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good &&
                        it != ActiveLiveEvents.BLINK &&
                        it != ActiveLiveEvents.YawLeft &&
                        it != ActiveLiveEvents.YawRight &&
                        it != ActiveLiveEvents.PitchUp &&
                        it != ActiveLiveEvents.PitchDown
            }
        }

        ActiveLiveType.BLINK -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good &&
                        it != ActiveLiveEvents.YawLeft &&
                        it != ActiveLiveEvents.YawRight &&
                        it != ActiveLiveEvents.PitchUp &&
                        it != ActiveLiveEvents.PitchDown &&
                        it != ActiveLiveEvents.WinkLeft &&
                        it != ActiveLiveEvents.WinkRight
            }
        }
        ActiveLiveType.NONE -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good
            }
        }
    }
}
