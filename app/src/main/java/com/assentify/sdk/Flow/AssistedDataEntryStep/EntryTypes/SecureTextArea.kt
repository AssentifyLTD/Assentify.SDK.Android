package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement

import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel

@Composable
fun SecureTextArea(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {


    /** Default Value **/
    var value by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(field.inputKey, field.languageTransformation) {
        if (field.languageTransformation == 0) {
            value = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
        } else {
            if(value.isEmpty()) {
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
                        value = data.value
                        AssistedFormHelper.changeValue(field.inputKey, data.value, page);
                    } else {
                        value =
                            AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
                    }
                }
            }
        }
    }

    /****/

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        return (field.isLocked == true) && identifiers.isNotEmpty()
    }
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()


    val err by remember(field.inputKey, page, value) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color =   BaseTheme.BaseTextColor,
            fontSize = 14.sp,
            fontFamily = InterFont,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        TextField(
            value = value,
            onValueChange = {
                value = it
                onValueChange(it)
            },
            readOnly = (field.readOnly == true) || getIsLocked(),
            singleLine = false,
            minLines = field.sizeByRows ?: 3,
            maxLines = Int.MAX_VALUE,
            modifier = Modifier
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BaseTheme.FieldColor,
                unfocusedContainerColor = BaseTheme.FieldColor,
                cursorColor = BaseTheme.FieldColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = BaseTheme.BaseTextColor,
                unfocusedTextColor = BaseTheme.BaseTextColor,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            ),
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(lineHeight = 20.sp),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            )
        )

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