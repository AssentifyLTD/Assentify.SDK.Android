package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.AssistedDataEntryStep.FieldsControllers.FilterManager
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.Models.DataSourceAttribute
import com.assentify.sdk.Flow.Models.DataSourceData
import com.assentify.sdk.Flow.Models.DataSourceResponse
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun SecureDropdownWithDataSource(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (List<DataSourceAttribute>, outputKeys: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    loadedMap: MutableMap<String, DataSourceData?>,
    loadingMap: MutableMap<String, Boolean>,
    filterMap: MutableMap<String, Map<String, String>?>,
) {
    val configModelObject = ConfigModelObject.getConfigModelObject()
    val currentKey = field.inputKey ?: ""

    var dataSourceData by remember(currentKey) {
        mutableStateOf<DataSourceData?>(loadedMap[currentKey])
    }

    val triggerFilter by FilterManager.triggerFilter.collectAsState()

    var filterKeyValues by remember(currentKey) {
        mutableStateOf(filterMap[currentKey] ?: emptyMap())
    }

    var selected by rememberSaveable(currentKey) {
        mutableStateOf<List<DataSourceAttribute>>(emptyList())
    }

    var searchQuery by rememberSaveable(currentKey) {
        mutableStateOf("")
    }

    var userStartedTyping by rememberSaveable(currentKey) {
        mutableStateOf(false)
    }

    var showSearchDialog by remember(currentKey) {
        mutableStateOf(false)
    }

    var filteredItems by remember(currentKey) {
        mutableStateOf(dataSourceData?.items ?: emptyList())
    }

    LaunchedEffect(triggerFilter) {
        val newFilterKeyValues = withContext(Dispatchers.Default) {
            AssistedFormHelper.getFilterValue(dataSourceData)
        }

        filterMap[currentKey] = newFilterKeyValues

        if (newFilterKeyValues != filterKeyValues) {
            filterKeyValues = newFilterKeyValues ?: emptyMap()
            loadedMap[currentKey] = null
            dataSourceData = null
            selected = emptyList()
            searchQuery = ""
            userStartedTyping = false
            showSearchDialog = false
            filteredItems = emptyList()
            loadingMap[currentKey] = false
        }
    }

    LaunchedEffect(field.inputKey, field.languageTransformation, filterKeyValues) {
        if (currentKey.isBlank()) return@LaunchedEffect
        if (configModelObject == null) return@LaunchedEffect
        if (field.endpointId == null) return@LaunchedEffect

        val cachedData = loadedMap[currentKey]
        if (cachedData != null) {
            dataSourceData = cachedData
            filteredItems = cachedData.items
            loadingMap[currentKey] = false
            return@LaunchedEffect
        }

        if (loadingMap[currentKey] == true && loadedMap[currentKey] == null) {
            loadingMap[currentKey] = false
        }

        loadingMap[currentKey] = true

        try {
            val response = loadDataSourceSuspend(
                field = field,
                filterKeyValues = filterKeyValues ?: emptyMap()
            )

            val loadedData = response?.data ?: return@LaunchedEffect

            dataSourceData = loadedData
            loadedMap[currentKey] = loadedData
            filteredItems = loadedData.items

            if (field.languageTransformation == 0) {
                val defaultValue = AssistedFormHelper.getDefaultValueValue(
                    field.inputKey!!,
                    page
                )

                val defaultSelected = withContext(Dispatchers.Default) {
                    findSelectedByDisplayValue(loadedData, defaultValue)
                }

                if (defaultSelected.isNotEmpty()) {
                    selected = defaultSelected
                    onValueChange(defaultSelected, loadedData.outputKeys)
                }
            } else {
                if (selected.isEmpty()) {
                    val dataList = listOf(
                        LanguageTransformationModel(
                            language = field.targetOutputLanguage!!,
                            languageTransformationEnum = field.languageTransformation!!,
                            value = AssistedFormHelper.getDefaultValueValue(
                                field.inputKey!!,
                                page
                            ) ?: "",
                            key = field.inputKey!!,
                            dataType = field.inputType
                        )
                    )

                    val transformationData = valueTransformationSuspend(
                        field.targetOutputLanguage,
                        TransformationModel(LanguageTransformationModels = dataList)
                    )

                    val valueToCompare = transformationData?.value
                        ?: AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page)

                    val transformedSelected = withContext(Dispatchers.Default) {
                        findSelectedByDisplayValue(loadedData, valueToCompare)
                    }

                    if (transformedSelected.isNotEmpty()) {
                        selected = transformedSelected

                        if (transformationData != null) {
                            AssistedFormHelper.changeValueSecureDropdownWithDataSource(
                                field.inputKey,
                                transformedSelected,
                                loadedData.outputKeys,
                                page
                            )
                        }

                        onValueChange(transformedSelected, loadedData.outputKeys)
                    }
                }
            }
        } finally {
            loadingMap[currentKey] = false
        }
    }

    LaunchedEffect(searchQuery, dataSourceData, userStartedTyping) {
        val currentData = dataSourceData
        val query = searchQuery

        filteredItems = withContext(Dispatchers.Default) {
            val items = currentData?.items ?: emptyList()

            if (!userStartedTyping || query.isBlank()) {
                items
            } else {
                items.filter { item ->
                    item.dataSourceAttributes
                        .firstOrNull { it.mappedKey == "Display Value" }
                        ?.value
                        .orEmpty()
                        .contains(query, ignoreCase = true)
                }
            }
        }
    }

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        return field.isLocked == true && identifiers.isNotEmpty()
    }

    val isReadOnly = field.readOnly == true || getIsLocked()

    val err by remember(field.inputKey, page, selected) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    val selectedDisplayValue = selected
        .firstOrNull { it.mappedKey == "Display Value" }
        ?.value
        .orEmpty()

    if (field.isHidden != true) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = BaseTheme.BaseTextColor,
                fontSize = 14.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal
            )

            Spacer(Modifier.height(6.dp))

            if (dataSourceData != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clickable(enabled = !isReadOnly) {
                            searchQuery = ""
                            userStartedTyping = false
                            showSearchDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = BaseTheme.FieldColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDisplayValue,
                            modifier = Modifier.weight(1f),
                            color = BaseTheme.BaseTextColor,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown Arrow",
                            tint = BaseTheme.BaseTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.CenterHorizontally),
                    color = BaseTheme.BaseTextColor,
                    strokeWidth = 2.dp
                )
            }

            if (err.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = err,
                    color = BaseTheme.BaseRedColor,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showSearchDialog && !isReadOnly && dataSourceData != null) {
        Dialog(
            onDismissRequest = {
                showSearchDialog = false
                searchQuery = ""
                userStartedTyping = false
            }
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BaseTheme.FieldColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = title,
                        color = BaseTheme.BaseTextColor,
                        fontSize = 16.sp,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(12.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            userStartedTyping = true
                        },
                        placeholder = {
                            Text(
                                text = "Search...",
                                color = BaseTheme.BaseTextColor.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = BaseTheme.BaseTextColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BaseTheme.FieldColor,
                            unfocusedContainerColor = BaseTheme.FieldColor,
                            disabledContainerColor = BaseTheme.FieldColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = BaseTheme.BaseTextColor,
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item {
                                Text(
                                    text = "No results found",
                                    color = BaseTheme.BaseTextColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(
                                items = filteredItems,
                                key = { option ->
                                    option.dataSourceAttributes
                                        .firstOrNull { it.mappedKey == "Display Value" }
                                        ?.value
                                        ?: option.hashCode()
                                }
                            ) { option ->
                                val displayValue = option.dataSourceAttributes
                                    .firstOrNull { it.mappedKey == "Display Value" }
                                    ?.value
                                    .orEmpty()

                                Text(
                                    text = displayValue,
                                    color = BaseTheme.BaseTextColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = option.dataSourceAttributes
                                            showSearchDialog = false
                                            searchQuery = ""
                                            userStartedTyping = false

                                            onValueChange(
                                                option.dataSourceAttributes,
                                                dataSourceData!!.outputKeys
                                            )
                                        }
                                        .padding(vertical = 14.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadDataSourceSuspend(
    field: DataEntryPageElement,
    filterKeyValues: Map<String, String>
): DataSourceResponse? {
    return withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val configModelObject = ConfigModelObject.getConfigModelObject()

            AssistedFormHelper.getDataSourceValues(
                configModelObject!!,
                field.elementIdentifier,
                FlowController.getCurrentStep()!!.stepDefinition!!.stepId,
                field.endpointId!!,
                filterKeyValues = filterKeyValues
            ) { data ->
                if (continuation.isActive) {
                    continuation.resume(data)
                }
            }
        }
    }
}

private suspend fun valueTransformationSuspend(
    targetOutputLanguage: String?,
    transformationModel: TransformationModel
): LanguageTransformationModel? {
    return withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            AssistedFormHelper.valueTransformation(
                targetOutputLanguage!!,
                transformationModel
            ) { transformationData ->
                if (continuation.isActive) {
                    continuation.resume(transformationData)
                }
            }
        }
    }
}

private fun findSelectedByDisplayValue(
    dataSourceData: DataSourceData,
    value: String?
): List<DataSourceAttribute> {
    if (value.isNullOrEmpty()) return emptyList()

    return dataSourceData.items.firstOrNull { item ->
        item.dataSourceAttributes
            .firstOrNull { it.mappedKey == "Display Value" }
            ?.value == value
    }?.dataSourceAttributes ?: emptyList()
}