package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.FlowEnvironmentalConditionsObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureCheckboxGroup(
    title: String,
    options: List<String>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    // Default value is a comma-separated string of pre-selected options
    val defaultValue = remember(field.inputKey, page) {
        AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
    }

    var isReadOnly = false

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        isReadOnly =  (field.isLocked == true) && identifiers.isNotEmpty()
        return  isReadOnly;
    }
    // Parse comma-separated default into a Set
    var selected by rememberSaveable(field.inputKey, page) {
        mutableStateOf(
            defaultValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        )
    }

    LaunchedEffect(defaultValue) {
        selected = defaultValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val err by remember(field.inputKey, page, selected) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    val containerColor = BaseTheme.FieldColor
    val accentColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                options.forEach { option ->
                    val isChecked = selected.contains(option)

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = isChecked,
                                enabled = !getIsLocked(),
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    if (!isReadOnly) {
                                        selected = if (checked) {
                                            selected + option
                                        } else {
                                            selected - option
                                        }
                                        onValueChange(selected.toList())
                                    }
                                }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.padding(top = 2.dp)) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (!isReadOnly) {
                                        selected = if (checked) {
                                            selected + option
                                        } else {
                                            selected - option
                                        }
                                        onValueChange(selected.toList())
                                    }
                                },
                                enabled = !isReadOnly,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = BaseTheme.BaseTextColor.copy(alpha = 0.8f),
                                    checkmarkColor = Color.White,
                                    disabledCheckedColor = accentColor.copy(alpha = 0.4f),
                                    disabledUncheckedColor = BaseTheme.BaseTextColor.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = option,
                            color = BaseTheme.BaseTextColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 12.dp),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (option != options.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 48.dp),
                            thickness = 1.dp,
                            color = BaseTheme.FieldColor.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            if (err.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    err,
                    color = BaseTheme.BaseRedColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}