package com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes

import AssistedFormHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.assentify.sdk.AssistedDataEntry.Models.DataEntryPageElement
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel

@Composable
fun SecureDropdown(
    title: String,
    options: List<String>,
    page: Int,
    field: DataEntryPageElement,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var userStartedTyping by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(field.inputKey, field.languageTransformation) {
        if (field.languageTransformation == 0) {
            selected = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
            onValueChange(selected)
        } else {
            if (selected.isEmpty()) {
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
                        selected = data.value
                        AssistedFormHelper.changeValue(field.inputKey, data.value, page)
                        onValueChange(selected)
                    } else {
                        selected = AssistedFormHelper.getDefaultValueValue(field.inputKey!!, page) ?: ""
                        onValueChange(selected)
                    }
                }
            }
        }
    }

    fun getIsLocked(): Boolean {
        val identifiers = field.inputPropertyIdentifierList ?: emptyList()
        return (field.isLocked == true) && identifiers.isNotEmpty()
    }

    val isReadOnly = (field.readOnly == true) || getIsLocked()

    val err by remember(field.inputKey, page, selected) {
        mutableStateOf(AssistedFormHelper.validateField(field.inputKey!!, page) ?: "")
    }

    val filteredOptions = remember(searchQuery, options, userStartedTyping) {
        if (!userStartedTyping) {
            options
        } else {
            options.filter { it.contains(searchQuery, ignoreCase = true) }
        }
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
                color = BaseTheme.FieldColor
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selected,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 17.dp),
                        color = BaseTheme.BaseTextColor,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown Arrow",
                        tint = BaseTheme.BaseTextColor.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(30.dp)
                            .padding(top = 12.dp)
                    )
                }
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

    if (showSearchDialog && !isReadOnly) {
        Dialog(
            onDismissRequest = {
                showSearchDialog = false
                searchQuery = ""
                userStartedTyping = false
            }
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
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
                        if (filteredOptions.isEmpty()) {
                            item {
                                Text(
                                    text = "No results found",
                                    color = BaseTheme.BaseTextColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(filteredOptions) { option ->
                                Text(
                                    text = option,
                                    color = BaseTheme.BaseTextColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = option
                                            showSearchDialog = false
                                            searchQuery = ""
                                            userStartedTyping = false
                                            onValueChange(option)
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