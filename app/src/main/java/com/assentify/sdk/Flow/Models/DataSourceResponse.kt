package com.assentify.sdk.Flow.Models

data class DataSourceResponse(
    val message: String?,
    val statusCode: Int,
    val error: String?,
    val errorCode: String?,
    val data: DataSourceData?,
    val isSuccessful: Boolean
)

data class DataSourceData(
    val endpointId: Int,
    val stepId: Int,
    val elementIdentifier: String,
    val items: List<DataSourceItem>,
    val outputKeys: Map<String, String>,
    val inputKeys: Map<String, String>,
    val filterKeys: List<String>,
    val isLoaded: Boolean
)

data class DataSourceItem(
    val dataSourceAttributes: List<DataSourceAttribute>
)

data class DataSourceAttribute(
    val id: Int,
    val propertyIdentifier: String,
    val value: String,
    val displayName: String,
    val mappedKey: String
)

data class DataSourceRequestBody(
    val filterKeyValues: Map<String, String> = emptyMap(),
    val inputKeyValues: Map<String, String> = emptyMap()
)