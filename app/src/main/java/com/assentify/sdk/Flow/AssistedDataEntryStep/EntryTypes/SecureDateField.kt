package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.Core.Constants.firstColor
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
private fun dateStringToUtcMillis(dateStr: String, pattern: String): Long? = try {
    if (dateStr.isBlank()) null else {
        val fmt = DateTimeFormatter.ofPattern(pattern)
        val localDate = LocalDate.parse(dateStr, fmt)
        localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
} catch (_: Exception) { null }

@RequiresApi(Build.VERSION_CODES.O)
private fun utcMillisToDateString(utcMillis: Long, pattern: String): String {
    val fmt = DateTimeFormatter.ofPattern(pattern)
    val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.format(fmt)
}

@Composable
fun SecureDateField(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onDateChange: (String) -> Unit = {},   // 🔹 callback when user picks new date
    dateFormat: String = "yyyy-MM-dd",
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }

    val defaultValue = remember(field.inputKey) {
        AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
    }

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val ctx = LocalContext.current

    var value by rememberSaveable(field.inputKey) { mutableStateOf(defaultValue) }

    // Validation state
    var err by remember(field.inputKey, page, value) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }



    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    fun OpenPickerDynamic(
        current: String,
        onPicked: (String) -> Unit,
        dateFormat: String = "yyyy-MM-dd",
        minDateStr: String? = null,
        maxDateStr: String? = null,
        onDismiss: () -> Unit
    ) {
        fun colorFromHex(hex: String): androidx.compose.ui.graphics.Color =
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))

        val background = BaseTheme.BackgroundColor!!
        val text = BaseTheme.BaseTextColor
        val click =  Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))

        fun contentOn(color: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
            val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
            return if (luminance > 0.6f) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
        }

        val clickContent = contentOn(click)

        // ✅ IMPORTANT: use UTC millis for DatePicker
        val minMillis = minDateStr?.let { dateStringToUtcMillis(it, dateFormat) }
        val maxMillis = maxDateStr?.let { dateStringToUtcMillis(it, dateFormat) }
        val initialMillis = dateStringToUtcMillis(current, dateFormat) ?: System.currentTimeMillis()

        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (minMillis != null && utcTimeMillis < minMillis) return false
                    if (maxMillis != null && utcTimeMillis > maxMillis) return false
                    return true
                }
            }
        )

        LaunchedEffect(Unit) {
            state.displayMode = DisplayMode.Picker
        }

        val tfColors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = background.firstColor(),
            unfocusedContainerColor = background.firstColor(),
            disabledContainerColor = background.firstColor(),

            focusedTextColor = text,
            unfocusedTextColor = text,
            disabledTextColor = text.copy(alpha = 0.5f),

            cursorColor = click,
            focusedIndicatorColor = click,
            unfocusedIndicatorColor = click.copy(alpha = 0.5f),
            disabledIndicatorColor = click.copy(alpha = 0.3f),

            focusedLabelColor = text,
            unfocusedLabelColor = text,
            disabledLabelColor = text.copy(alpha = 0.5f),

            focusedPlaceholderColor = text.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = text.copy(alpha = 0.5f),
            disabledPlaceholderColor = text.copy(alpha = 0.3f)
        )

        val colors = androidx.compose.material3.DatePickerDefaults.colors(
            containerColor = background.firstColor(),
            dividerColor = click.copy(alpha = 0.3f),

            titleContentColor = text,
            headlineContentColor = text,
            subheadContentColor = text,
            navigationContentColor = click,

            weekdayContentColor = text,
            dayContentColor = text,
            disabledDayContentColor = text.copy(alpha = 0.3f),

            selectedDayContainerColor = click,
            selectedDayContentColor = clickContent,

            todayContentColor = click,
            todayDateBorderColor = click,

            yearContentColor = text,
            disabledYearContentColor = text.copy(alpha = 0.3f),
            currentYearContentColor = click,
            selectedYearContainerColor = click,
            selectedYearContentColor = clickContent,

            dateTextFieldColors = tfColors
        )

        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = onDismiss,
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = background.firstColor()),
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis ?: return@TextButton
                        // ✅ IMPORTANT: format from UTC millis
                        onPicked(utcMillisToDateString(millis, dateFormat))
                        onDismiss()
                    }
                ) { androidx.compose.material3.Text("OK", color = click) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("CANCEL", color = click)
                }
            }
        ) {
            androidx.compose.material3.DatePicker(
                state = state,
                colors = colors
            )
        }
    }




     @RequiresApi(Build.VERSION_CODES.O)
     fun dateStringToUtcMillis(dateStr: String, pattern: String): Long? = try {
        if (dateStr.isBlank()) null else {
            val fmt = DateTimeFormatter.ofPattern(pattern)
            val localDate = LocalDate.parse(dateStr, fmt)
            localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    } catch (_: Exception) { null }

     @RequiresApi(Build.VERSION_CODES.O)
     fun utcMillisToDateString(utcMillis: Long, pattern: String): String {
        val fmt = DateTimeFormatter.ofPattern(pattern)
        val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return date.format(fmt)
    }


    fun persist(newValue: String) {
        AssistedFormHelper.changeValue(field.inputKey!!, newValue, page)
        err = AssistedFormHelper.validateField(field.inputKey!!, page) ?: ""
        onDateChange(newValue)
    }

     fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        return (field.isLocked == true) && identifiers.isNotEmpty()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = BaseTheme.BaseTextColor,
            fontSize = 14.sp,
            fontFamily = InterFont,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        val canPick = (field.readOnly != true && !getIsLocked())

        Box(Modifier.fillMaxWidth()) {
            TextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {  },

                trailingIcon = {
                    IconButton(onClick = {
                        // TODO: open date picker here
                    }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick date",
                            modifier = Modifier.size(22.dp),
                            tint = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                        )
                    }
                },

                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BaseTheme.FieldColor,
                    unfocusedContainerColor =  BaseTheme.FieldColor,
                    cursorColor = BaseTheme.BaseTextColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = BaseTheme.BaseTextColor,
                    unfocusedTextColor = BaseTheme.BaseTextColor,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        enabled = canPick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                       show = true;

                    }
            )
        }
        if (show) {
            OpenPickerDynamic(
                current = value,
                onPicked = { picked ->
                    value = picked
                    persist(picked)
                },
                minDateStr = field.from,
                maxDateStr = field.to,
                onDismiss = { show = false }
            )
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
