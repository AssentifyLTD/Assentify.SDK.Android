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
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel

@Composable
fun SecureDropdownWithDataSource(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (List<DataSourceAttribute>, outputKeys: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    loadedMap:MutableMap<String, DataSourceData?>,
    filterMap:MutableMap<String, Map<String, String>?>,
    ) {



    val configModelObject = ConfigModelObject.getConfigModelObject()
    var dataSourceData by remember { mutableStateOf<DataSourceData?>(null) }

    val triggerFilter by FilterManager.triggerFilter.collectAsState()
    var filterKeyValues by remember { mutableStateOf<Map<String, String>?>(emptyMap()) }

    /** Default Value **/
    var selected by rememberSaveable  { mutableStateOf<List<DataSourceAttribute>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var userStartedTyping by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    /** Loaded State **/

    val currentKey = field.inputKey ?: ""
    dataSourceData = loadedMap[currentKey]
    filterKeyValues = filterMap[currentKey]

    LaunchedEffect(triggerFilter) {
        val newFilterKeyValues = AssistedFormHelper.getFilterValue(dataSourceData)
        filterMap[currentKey] = newFilterKeyValues;
        if (newFilterKeyValues != filterKeyValues) {
            filterKeyValues = newFilterKeyValues
            loadedMap[currentKey] = null
            dataSourceData = null
            selected = emptyList()
            searchQuery = ""
            userStartedTyping = false
            showSearchDialog = false
        }
    }




    LaunchedEffect(field.inputKey, field.languageTransformation,filterKeyValues) {
        if (loadedMap[currentKey] != null) return@LaunchedEffect

        AssistedFormHelper.getDataSourceValues(
            configModelObject!!,
            field.elementIdentifier,
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId,
            field.endpointId!!,
            filterKeyValues = filterKeyValues!!
        ) { data ->
            if (data != null) {
                dataSourceData = data.data
                loadedMap[currentKey] = dataSourceData!!;
                if (field.languageTransformation == 0) {
                    dataSourceData!!.items.forEach {
                        if (
                            it.dataSourceAttributes.isNotEmpty() &&
                            it.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value ==
                            AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page)
                        ) {
                            selected = it.dataSourceAttributes
                        }
                    }
                    if (selected.isNotEmpty()) {
                        onValueChange(selected, dataSourceData!!.outputKeys)
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
                        AssistedFormHelper.valueTransformation(
                            field.targetOutputLanguage,
                            TransformationModel(LanguageTransformationModels = dataList)
                        ) { transformationData ->
                            if (transformationData != null) {
                                dataSourceData!!.items.forEach {
                                    if (
                                        it.dataSourceAttributes.isNotEmpty() &&
                                        it.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value ==
                                        transformationData.value
                                    ) {
                                        selected = it.dataSourceAttributes
                                    }
                                }
                                if (selected.isNotEmpty()) {
                                    AssistedFormHelper.changeValueSecureDropdownWithDataSource(
                                        field.inputKey,
                                        selected,
                                        dataSourceData!!.outputKeys,
                                        page
                                    )
                                    onValueChange(selected, dataSourceData!!.outputKeys)
                                }
                            } else {
                                dataSourceData!!.items.forEach {
                                    if (
                                        it.dataSourceAttributes.isNotEmpty() &&
                                        it.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value ==
                                        AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page)
                                    ) {
                                        selected = it.dataSourceAttributes
                                    }
                                }
                                if (selected.isNotEmpty()) {
                                    onValueChange(selected, dataSourceData!!.outputKeys)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /****/
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        return (field.isLocked == true) && identifiers.isNotEmpty()
    }

    val isReadOnly = (field.readOnly == true) || getIsLocked()

    val err by remember(field.inputKey, page, selected) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    val pillColor = BaseTheme.FieldColor

    val filteredItems = remember(searchQuery, dataSourceData, userStartedTyping) {
        val items = dataSourceData?.items ?: emptyList()
        if (!userStartedTyping) {
            items
        } else {
            items.filter { item ->
                val displayValue = item.dataSourceAttributes
                    .firstOrNull { it.mappedKey == "Display Value" }
                    ?.value
                    .orEmpty()

                displayValue.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val selectedDisplayValue = if (selected.isNotEmpty()) {
        selected.firstOrNull { it.mappedKey == "Display Value" }?.value ?: ""
    } else {
        ""
    }

    if (!field.isHidden!!) {
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
                    color = pillColor
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
                            items(filteredItems) { option ->
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