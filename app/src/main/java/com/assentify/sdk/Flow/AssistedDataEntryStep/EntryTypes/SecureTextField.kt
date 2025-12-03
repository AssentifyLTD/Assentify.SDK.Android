package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes


import AssistedFormHelper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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


@Composable
fun SecureTextField(
    title: String,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    /** Default Value **/
    var value by remember { mutableStateOf<String>("") }
    LaunchedEffect(field.inputKey, field.languageTransformation) {
        if (field.languageTransformation == 0) {
            value =  AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
        } else {
            val dataList = listOf(
                LanguageTransformationModel(
                    language = field.targetOutputLanguage!!,
                    languageTransformationEnum = field.languageTransformation!!,
                    value = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: "",
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
                    AssistedFormHelper.changeValue(field.inputKey,data.value,page);
                } else {
                    value = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
                }
            }
        }
    }

    /****/


    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()



    val err by remember(field.inputKey, page, value) {
        mutableStateOf(
            AssistedFormHelper.validateField(field.inputKey!!, page) ?: ""
        )
    }
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(6.dp))

        TextField(
            value = value,
            onValueChange = {
                value = it;
                onValueChange(it)
            },
            readOnly = field.readOnly!! || field.isLocked!!,
            singleLine = true,
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
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        )

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

