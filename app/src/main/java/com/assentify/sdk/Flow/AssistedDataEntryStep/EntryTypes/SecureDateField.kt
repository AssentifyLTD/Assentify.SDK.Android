package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import android.app.DatePickerDialog
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.NumberPicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import com.assentify.sdk.FlowEnvironmentalConditionsObject

@Composable
fun SecureDateField(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onDateChange: (String) -> Unit = {},   // 🔹 callback when user picks new date
    dateFormat: String = "yyyy-MM-dd",
    modifier: Modifier = Modifier
) {

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

    fun openPicker(
        current: String,
        onPicked: (String) -> Unit,
        dateFormat: String = "yyyy-MM-dd",
        minDateStr: String? = null,
        maxDateStr: String? = null,
        primaryHex: String = flowEnv.clicksHexColor
    ) {
        fun parseToMillisOrNull(s: String?): Long? = try {
            if (s.isNullOrBlank()) null else
                java.text.SimpleDateFormat(dateFormat, java.util.Locale.US)
                    .apply { isLenient = false }
                    .parse(s)?.time
        } catch (_: Exception) { null }

        val colorInt = android.graphics.Color.parseColor(primaryHex)

        val cal = java.util.Calendar.getInstance()
        if (current.isNotBlank()) {
            try {
                java.text.SimpleDateFormat(dateFormat, java.util.Locale.US).apply { isLenient = false }
                    .parse(current)?.let { cal.time = it }
            } catch (_: Exception) { }
        }

        val dlg = android.app.DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val mm = (m + 1).toString().padStart(2, '0')
                val dd = d.toString().padStart(2, '0')
                onPicked("$y-$mm-$dd")
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )

        parseToMillisOrNull(minDateStr)?.let { dlg.datePicker.minDate = it }
        parseToMillisOrNull(maxDateStr)?.let { dlg.datePicker.maxDate = it }

        dlg.setOnShowListener {
            dlg.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(colorInt)
            dlg.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(colorInt)
            val daySpinnerId = ctx.resources.getIdentifier("android:id/day", null, null)
            val monthSpinnerId = ctx.resources.getIdentifier("android:id/month", null, null)
            val yearSpinnerId = ctx.resources.getIdentifier("android:id/year", null, null)
            listOf(daySpinnerId, monthSpinnerId, yearSpinnerId).forEach { id ->
                (dlg.datePicker.findViewById<View>(id) as? NumberPicker)?.let { picker ->
                    val fields = NumberPicker::class.java.declaredFields
                    fields.find { it.name == "mSelectionDivider" }?.apply {
                        isAccessible = true
                        set(picker, ColorDrawable(colorInt))
                    }
                }
            }
            val headerId = ctx.resources.getIdentifier("android:id/date_picker_header", null, null)
            dlg.datePicker.findViewById<View>(headerId)?.setBackgroundColor(colorInt)
        }



        dlg.show()
    }

    fun persist(newValue: String) {
        AssistedFormHelper.changeValue(field.inputKey!!, newValue, page)
        err = AssistedFormHelper.validateField(field.inputKey!!, page) ?: ""
        onDateChange(newValue)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        val canPick = (field.readOnly != true && field.isLocked != true)

        Box(Modifier.fillMaxWidth()) {
            TextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {  },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                    unfocusedContainerColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                    cursorColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor)),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
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
                        openPicker(
                            current = value,
                            onPicked = { picked ->
                                value = picked
                                persist(picked)
                            },
                            minDateStr = field.from,
                            maxDateStr = field.to
                        )

                    }
            )
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
