package com.assentify.sdk.Flow.AssistedDataEntryStep

import AssistedFormHelper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
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


            val listState = remember(page) { LazyListState() } // ✅ one state per page

            LazyColumnWithScrollbar(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                trackColor = BaseTheme.FieldColor,
                thumbColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                thumbMinHeight = 60.dp,
                thumbMaxHeight = 140.dp
            ) {
                item {
                    Text(
                        text = pageModel.title ?: "",
                        color =   BaseTheme.BaseTextColor,
                        fontSize = 22.sp,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(
                    count = pageModel.dataEntryPageElements.size,
                    key = { index -> pageModel.dataEntryPageElements[index].inputKey ?: index }

                ) { i ->
                    Box(modifier = Modifier.padding(end = 10.dp)) {
                    val field = pageModel.dataEntryPageElements[i]
                    val typeEnum = InputTypes.fromString(field.inputType)

                    when (typeEnum) {
                        InputTypes.Text -> {
                            SecureTextField(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.TextArea -> {
                            SecureTextArea(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.Date -> {
                            SecureDateField(
                                title = field.textTitle!!,
                                onDateChange = { newDate ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, newDate, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.DropDown -> {
                            if (field.endpointId != null) {
                                SecureDropdownWithDataSource(
                                    title = field.textTitle!!,
                                    onValueChange = { new, outputKeys ->
                                        AssistedFormHelper.changeValueSecureDropdownWithDataSource(
                                            field.inputKey!!, new, outputKeys, page
                                        )
                                        onFieldChanged()
                                    },
                                    page = page,
                                    field = field,
                                )
                            } else {
                                SecureDropdown(
                                    title = field.textTitle!!,
                                    onValueChange = { new ->
                                        AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                        onFieldChanged()
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
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.RadioButtonGroup -> {
                            SecureRadioGroup(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
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
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                                options = allCountries
                            )
                        }

                        InputTypes.PhoneNumber -> {
                            SecurePhoneInput(
                                title = field.textTitle!!,
                                onValueChange = { selectedDial, localNumber ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, localNumber, page)
                                    onFieldChanged()
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
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
                                },
                                onValid = {
                                    AssistedFormHelper.changeLocalOtpValid(field.inputKey!!, true, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }

                        InputTypes.EmailWithOTP -> {
                            SecureEmailWithOtpField(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new, page)
                                    onFieldChanged()
                                },
                                onValid = {
                                    AssistedFormHelper.changeLocalOtpValid(field.inputKey!!, true, page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }
                    }
                }}

                item { Spacer(Modifier.height(300.dp)) }
            }
        }

        Spacer(Modifier.height(500.dp))
    }
}



@Composable
fun LazyColumnWithScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    scrollbarWidth: Dp = 8.dp,
    trackColor: Color,
    thumbColor: Color,
    thumbMinHeight: Dp = 48.dp,
    thumbMaxHeight: Dp = 160.dp,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content
        )

        VerticalScrollbar(
            state = state,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 6.dp),
            width = scrollbarWidth,
            trackColor = trackColor,
            thumbColor = thumbColor,
            thumbMinHeight = thumbMinHeight,
            thumbMaxHeight = thumbMaxHeight
        )
    }
}

@Composable
private fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    width: Dp,
    trackColor: Color,
    thumbColor: Color,
    thumbMinHeight: Dp,
    thumbMaxHeight: Dp = 180.dp,
) {
    // ✅ Hide when content doesn’t scroll
    val showScrollbar by remember {
        derivedStateOf { state.canScrollForward || state.canScrollBackward }
    }

    if (!showScrollbar) return

    Canvas(modifier = modifier.width(width)) {

        // Track (always draw if scrollbar is visible)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
        )

        val layoutInfo = state.layoutInfo
        val visible = layoutInfo.visibleItemsInfo
        val totalItems = layoutInfo.totalItemsCount

        // If not ready yet (first frame), draw a default thumb so it doesn't "wait until scroll"
        if (totalItems <= 0 || visible.isEmpty()) {
            val thumbPx = thumbMinHeight.toPx().coerceAtMost(thumbMaxHeight.toPx())
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, thumbPx),
                cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
            )
            return@Canvas
        }

        val firstIndex = state.firstVisibleItemIndex
        val firstOffsetPx = state.firstVisibleItemScrollOffset.toFloat()

        val avgItemPx = visible.map { it.size }.average().toFloat().coerceAtLeast(1f)
        val totalContentPx = (totalItems * avgItemPx).coerceAtLeast(1f)

        val viewportPx = layoutInfo.viewportEndOffset.toFloat().coerceAtLeast(1f)
        val maxScrollPx = (totalContentPx - viewportPx).coerceAtLeast(1f)

        val scrollPx = (firstIndex * avgItemPx + firstOffsetPx).coerceIn(0f, maxScrollPx)
        val fraction = (scrollPx / maxScrollPx).coerceIn(0f, 1f)

        val minThumbPx = thumbMinHeight.toPx()
        val maxThumbPx = thumbMaxHeight.toPx().coerceAtLeast(minThumbPx)

        val rawThumbPx = (viewportPx / totalContentPx) * size.height
        val thumbPx = rawThumbPx.coerceIn(minThumbPx, maxThumbPx)

        val maxTop = (size.height - thumbPx).coerceAtLeast(0f)
        val top = (fraction * maxTop).coerceIn(0f, maxTop)

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, top),
            size = Size(size.width, thumbPx),
            cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
        )
    }
}

