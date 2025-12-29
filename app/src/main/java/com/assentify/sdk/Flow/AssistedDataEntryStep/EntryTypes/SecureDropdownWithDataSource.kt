package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.Models.DataSourceAttribute
import com.assentify.sdk.Flow.Models.DataSourceData
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDropdownWithDataSource(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (List<DataSourceAttribute>, outputKeys: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val configModelObject = ConfigModelObject.getConfigModelObject()
    var dataSourceData by remember { mutableStateOf<DataSourceData?>(null) }

    /** Default Value **/
    var selected by rememberSaveable { mutableStateOf<List<DataSourceAttribute>>(emptyList()) }
    LaunchedEffect(field.inputKey, field.languageTransformation) {
        AssistedFormHelper.getDataSourceValues(
            configModelObject,
            field.elementIdentifier,
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId,
            field.endpointId!!,
        ) { data ->
            if (data != null) {
                dataSourceData = data.data;
                if (field.languageTransformation == 0) {
                    dataSourceData!!.items.forEach {
                        if (it.dataSourceAttributes.isNotEmpty() && it.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value == AssistedFormHelper.getDefaultValueValue(
                                field.inputKey!!,
                                page
                            )
                        ) {
                            selected = it.dataSourceAttributes;
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
                                )
                                    ?: "",
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
                                    if (it.dataSourceAttributes.isNotEmpty() && it.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value == transformationData.value) {
                                        selected = it.dataSourceAttributes;
                                    }
                                }
                                if (selected.isNotEmpty()) {
                                    AssistedFormHelper.changeValueSecureDropdownWithDataSource(
                                        field.inputKey,
                                        selected,
                                        dataSourceData!!.outputKeys,
                                        page
                                    );
                                    onValueChange(selected, dataSourceData!!.outputKeys)
                                }

                            } else {
                                dataSourceData!!.items.forEach {
                                    if (it.dataSourceAttributes.isNotEmpty() && it.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value == AssistedFormHelper.getDefaultValueValue(
                                            field.inputKey!!,
                                            page
                                        )
                                    ) {
                                        selected = it.dataSourceAttributes;
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

    var expanded by remember { mutableStateOf(false) }


    val isReadOnly = (field.readOnly == true) || (field.isLocked == true)

    val err by remember(field.inputKey, page, selected) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }


    val pillColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor))


    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))


        if (dataSourceData != null) {
            ExposedDropdownMenuBox(
                expanded = expanded && !isReadOnly,
                onExpandedChange = { if (!isReadOnly) expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = if (selected.isNotEmpty()) selected.first { i -> i.mappedKey == "Display Value" }.value else "",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown Arrow",
                            tint = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)).copy(alpha = 0.8f),
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = pillColor,
                        unfocusedContainerColor = pillColor,
                        disabledContainerColor = pillColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor))
                    ),
                    shape = RoundedCornerShape(16.dp), // keep only one
                    modifier = Modifier
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                        .fillMaxWidth()
                        .height(55.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded && !isReadOnly,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)))
                ) {
                    dataSourceData!!.items.forEach { option ->
                        Box(
                            modifier = Modifier
                                .background(Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)))
                        ) {
                            Column {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.dataSourceAttributes.first { i -> i.mappedKey == "Display Value" }.value,
                                            color = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor))
                                        )
                                    },
                                    onClick = {
                                        selected = option.dataSourceAttributes
                                        expanded = false
                                        onValueChange(
                                            option.dataSourceAttributes,
                                            dataSourceData!!.outputKeys,
                                        )
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.CenterHorizontally),
                color = Color(android.graphics.Color.parseColor(flowEnv.listItemsTextUnSelectedHexColor)),
                strokeWidth = 2.dp
            )
        }


        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                err,
                color = Color.Red,
                fontSize = 12.sp
            )
        }
    }
}
