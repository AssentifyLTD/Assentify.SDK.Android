package com.assentify.sdk.Flow.AssistedDataEntryStep

import AssistedFormHelper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.AssistedDataEntry.Models.InputTypes
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureDateField
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureDropdown
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureDropdownWithDataSource
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureEmailWithOtpField
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureNationalityDropdown
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecurePhoneInput
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecurePhoneWithOtpField
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureRadioGroup
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureTextArea
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureTextField
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.allCountries
import com.assentify.sdk.FlowEnvironmentalConditionsObject


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistedDataEntryPager(
    assistedDataEntryModel: AssistedDataEntryModel?,
    pagerState: PagerState,
    onFieldChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistedDataEntryPages = assistedDataEntryModel!!.assistedDataEntryPages

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageModel = assistedDataEntryPages[page]

            // Make page content scrollable
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        text = pageModel.title ?: "",
                        color = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(pageModel.dataEntryPageElements.size) { i ->
                    val field = pageModel.dataEntryPageElements[i]
                    val typeEnum = InputTypes.fromString(field.inputType)


                    when (typeEnum) {
                        InputTypes.Text -> {

                            SecureTextField(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.TextArea -> {
                            SecureTextArea(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                            )

                        }

                        InputTypes.Date -> {

                            SecureDateField(
                                title = field.textTitle!!,
                                onDateChange = { newDate ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, newDate, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.DropDown -> {
                            if(field.endpointId != null){
                                SecureDropdownWithDataSource(
                                    title = field.textTitle!!,
                                    onValueChange = { new ,outputKeys ->
                                        AssistedFormHelper.changeValueSecureDropdownWithDataSource(
                                            field.inputKey!!,new,outputKeys, page);
                                        onFieldChanged();
                                    },
                                    page = page,
                                    field = field,
                                )
                            }else{
                                SecureDropdown(
                                    title = field.textTitle!!,
                                    onValueChange = { new ->
                                        AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                        onFieldChanged();
                                    },
                                    page = page,
                                    field = field,
                                    options = field.dataSourceContent
                                        ?.split(",")
                                        ?.map { it.trim() }
                                        ?: emptyList(),
                                )
                            }

                        }

                        InputTypes.Email -> {
                            SecureTextField(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.RadioButtonGroup -> {
                            SecureRadioGroup(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                                options = field.dataSourceContent
                                    ?.split(",")
                                    ?.map { it.trim() }
                                    ?: emptyList(),
                            )
                        }

                        InputTypes.Nationality -> {
                            SecureNationalityDropdown(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                                options = allCountries
                            )
                        }

                        InputTypes.PhoneNumber -> {
                            SecurePhoneInput(
                                title = field.textTitle!!,
                                onValueChange = {selectedDial, localNumber ,->
                                    AssistedFormHelper.changeValue(field.inputKey!!, localNumber, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                                options = allCountries
                            )
                        }

                        InputTypes.PhoneNumberWithOTP -> {
                            SecurePhoneWithOtpField(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                onValid = {
                                    AssistedFormHelper.changeLocalOtpValid(field.inputKey!!, true, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.EmailWithOTP -> {
                            SecureEmailWithOtpField(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page);
                                    onFieldChanged();
                                },
                                onValid = {
                                    AssistedFormHelper.changeLocalOtpValid(field.inputKey!!, true, page);
                                    onFieldChanged();
                                },
                                page = page,
                                field = field,
                            )
                        }
                    }

                }
                item { Spacer(Modifier.height(300.dp)) }
            }
        }

        Spacer(Modifier.height(500.dp))
    }
}
