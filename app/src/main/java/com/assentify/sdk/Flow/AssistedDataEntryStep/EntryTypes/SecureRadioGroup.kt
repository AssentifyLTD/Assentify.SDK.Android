package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureRadioGroup(
    title: String,
    options: List<String>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    val defaultValue = remember(field.inputKey, page) {
        AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
    }

    val isReadOnly = false

    var selected by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultValue) }
    LaunchedEffect(defaultValue) { selected = defaultValue }

    val err by remember(field.inputKey, page, selected) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    val containerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor))
    val accentColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        // Pills-style container (to match your dropdown background)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selected == option

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isSelected,
                            enabled = !isReadOnly,
                            role = Role.RadioButton,
                            onClick = {
                                if (!isReadOnly) {
                                    selected = option
                                    onValueChange(option)
                                }
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            if (!isReadOnly) {
                                selected = option
                                onValueChange(option)
                            }
                        },
                        enabled = !isReadOnly,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = accentColor,
                            unselectedColor = Color.White.copy(alpha = 0.8f),
                            disabledSelectedColor = accentColor.copy(alpha = 0.4f),
                            disabledUnselectedColor = Color.White.copy(alpha = 0.4f)
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = option,
                        color = if (isReadOnly) Color.White.copy(alpha = 0.6f) else Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (option != options.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.12f)
                    )
                }
            }
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                err,
                color = accentColor,
                fontSize = 12.sp
            )
        }
    }
}
