package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.Flow.BlockLoader.BaseTheme

@Composable
fun SecureCheckbox(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultValue = remember(field.inputKey, page) {
        val raw = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page)
        if (raw.isNullOrEmpty()) false else raw.trim().lowercase() == "true"
    }


    var isReadOnly = false

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        isReadOnly = (field.isLocked == true) && identifiers.isNotEmpty()
        return isReadOnly;
    }

    var isChecked by rememberSaveable(field.inputKey, page) { mutableStateOf(defaultValue) }
    LaunchedEffect(defaultValue) {
        isChecked = defaultValue
        onValueChange(defaultValue)
    }


    val accentColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))

    if (!field.isHidden!!) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = isChecked,
                    enabled = !getIsLocked(),
                    role = Role.Checkbox,
                    onValueChange = { checked ->
                        if (!isReadOnly) {
                            isChecked = checked
                            onValueChange(checked)
                        }
                    }
                )
        ) {
            Box(modifier = Modifier.padding(top = 2.dp)) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        if (!isReadOnly) {
                            isChecked = checked
                            onValueChange(checked)
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
                text = title,
                color = BaseTheme.BaseTextColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}