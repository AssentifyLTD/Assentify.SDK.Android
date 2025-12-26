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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDropdown(
    title: String,
    options: List<String>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    /** Default Value **/
    var selected by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(field.inputKey, field.languageTransformation) {
        if (field.languageTransformation == 0) {
            selected =  AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
            onValueChange(selected)
        } else {
            if (selected.isEmpty()) {
                val dataList = listOf(
                    LanguageTransformationModel(
                        language = field.targetOutputLanguage!!,
                        languageTransformationEnum = field.languageTransformation!!,
                        value = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page)
                            ?: "",
                        key = field.inputKey!!,
                        dataType = field.inputType
                    )
                )
                AssistedFormHelper.valueTransformation(
                    field.targetOutputLanguage,
                    TransformationModel(LanguageTransformationModels = dataList)
                ) { data ->
                    if (data != null) {
                        selected = data.value
                        AssistedFormHelper.changeValue(field.inputKey, data.value, page);
                        onValueChange(selected)
                    } else {
                        selected =
                            AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
                        onValueChange(selected)
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


        ExposedDropdownMenuBox(
            expanded = expanded && !isReadOnly,
            onExpandedChange = { if (!isReadOnly) expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)),),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown Arrow",
                        tint = Color.White.copy(alpha = 0.8f),
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
                    cursorColor = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                ),
                shape = RoundedCornerShape(16.dp), // keep only one
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true)
                    .fillMaxWidth()
                    .height(55.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded && !isReadOnly,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)))
            ) {
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .background(Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)))
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = { Text(option,color = Color.White) },
                                onClick = {
                                    selected = option
                                    expanded = false
                                    onValueChange(option)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                err,
                color = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                fontSize = 12.sp
            )
        }
    }
}
