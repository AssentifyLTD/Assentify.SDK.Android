package com.assentify.sdk.Flow.AssistedDataEntryStep.FieldsControllers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FilterManager {
    private val _updateFilter = MutableStateFlow(0)
    val triggerFilter: StateFlow<Int> = _updateFilter

    fun updateFilter() {
        _updateFilter.value += 1
    }
}