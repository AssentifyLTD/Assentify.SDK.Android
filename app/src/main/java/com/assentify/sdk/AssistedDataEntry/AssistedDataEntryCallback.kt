package com.assentify.sdk.AssistedDataEntry

import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel


public interface AssistedDataEntryCallback {
    fun onAssistedDataEntryError(message: String)
    fun onAssistedDataEntrySuccess(assistedDataEntryModel: AssistedDataEntryModel)
}