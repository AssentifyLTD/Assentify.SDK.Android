package com.assentify.sdk.Core.Constants



enum class FaceEvents {
    RollLeft,
    RollRight,
    YawLeft,
    YawRight,
    PitchUp,
    PitchDown,
    Good,
    Wink,
    WinkLeft,
    WinkRight,
    NO_DETECT,
}

enum class ActiveLiveEvents {
    YawLeft,
    YawRight,
    PitchUp,
    PitchDown,
    Wink,
    WinkLeft,
    WinkRight,
    Good
}

enum class ActiveLiveType{
    Actions,
    Wink,
    NON
}

fun getRandomEvents(activeLiveType: ActiveLiveType): Set<ActiveLiveEvents> {
    val allEvents  = getFilteredEventsByType(activeLiveType)
    val randomEvents = mutableSetOf<ActiveLiveEvents>()

    while (randomEvents.size < 3 && randomEvents.size < allEvents.size) {
        randomEvents.add(allEvents.random())
    }
    return randomEvents
}

fun getFilteredEventsByType(type: ActiveLiveType): List<ActiveLiveEvents> {
    return when (type) {
        ActiveLiveType.Actions -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good &&
                        it != ActiveLiveEvents.Wink &&
                        it != ActiveLiveEvents.WinkLeft &&
                        it != ActiveLiveEvents.WinkRight
            }
        }

        ActiveLiveType.Wink -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good &&
                        it != ActiveLiveEvents.YawLeft &&
                        it != ActiveLiveEvents.YawRight &&
                        it != ActiveLiveEvents.PitchUp &&
                        it != ActiveLiveEvents.PitchDown
            }
        }

        ActiveLiveType.NON -> {
            ActiveLiveEvents.values().filter {
                it != ActiveLiveEvents.Good
            }
        }
    }
}
