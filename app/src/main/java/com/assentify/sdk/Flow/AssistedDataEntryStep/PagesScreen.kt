package com.assentify.sdk.Flow.AssistedDataEntryStep

import AssistedFormHelper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.assentify.sdk.AssistedDataEntryPagesObject
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureCheckbox
import com.assentify.sdk.Flow.AssistedDataEntryStep.EntryTypes.SecureCheckboxGroup
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
import com.assentify.sdk.Flow.AssistedDataEntryStep.FieldsControllers.FilterManager
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.Models.DataSourceData
import com.assentify.sdk.Flow.ReusableComposable.LogoSvgUrl
import com.assentify.sdk.FlowEnvironmentalConditionsObject

class DataSourceCache {
    val loadedMap = mutableStateMapOf<String, DataSourceData?>()
    val loadingMap = mutableStateMapOf<String, Boolean>()
    val filterMap = mutableStateMapOf<String, Map<String, String>?>()

    fun clear() {
        loadedMap.clear()
        loadingMap.clear()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistedDataEntryPager(
    assistedDataEntryModel: AssistedDataEntryModel?,
    pagerState: PagerState,
    cache: DataSourceCache,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier
) {


    val assistedDataEntryPages = remember { mutableStateOf(assistedDataEntryModel!!.assistedDataEntryPages) }
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val loadedMap = cache.loadedMap
    val loadingMap = cache.loadingMap
    val filterMap = remember { mutableStateMapOf<String, Map<String, String>?>() }
    var rebuildTick by remember { mutableStateOf(0) }
    fun onFieldChanged() {
        val newModel = AssistedDataEntryPagesObject.getAssistedDataEntryModelObject() ?: return

        assistedDataEntryPages.value = newModel.assistedDataEntryPages.map { page ->
            page.copy(
                dataEntryPageElements = page.dataEntryPageElements.toList()
            )
        }
        rebuildTick++
        onChanged()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // not rebuild the screen at all
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth()
        ) { page ->

            val pageModel =  assistedDataEntryPages.value[page]

            // One scroll state per page. A plain (non-lazy) Column is used on purpose:
            // every field widget is composed as soon as the page opens, so each one
            // writes its default value (from earlier steps) into the model even if the
            // user never scrolls to it. A LazyColumn only composes visible rows, which
            // left off-screen prefilled fields out of the submitted data.
            val scrollState = remember(page) { ScrollState(0) }

            val reachedEnd by remember(page) {
                derivedStateOf { scrollState.value >= scrollState.maxValue }
            }

            LaunchedEffect(page, reachedEnd) {
                if (reachedEnd) {
                    onFieldChanged()
                }
            }

            ScrollColumnWithScrollbar(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                trackColor = BaseTheme.FieldColor,
                thumbColor = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                thumbMinHeight = 60.dp,
                thumbMaxHeight = 140.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!pageModel.svgLogoUrl.isNullOrEmpty() && !pageModel.title.isNullOrEmpty() && !pageModel.subTitle.isNullOrEmpty()) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            LogoSvgUrl(
                                url = pageModel.svgLogoUrl!!,
                                modifier = Modifier.size(width = 70.dp, height = 70.dp)
                            )
                        }
                        Text(
                            text = pageModel.title,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            color = BaseTheme.BaseTextColor,
                            fontSize = 23.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                        )
                        Text(
                            text = pageModel.subTitle,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Medium,
                            color = BaseTheme.BaseTextColor.copy(0.5F),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp, start = 20.dp, end = 20.dp)
                        )
                    }else{
                        Text(
                            text = pageModel.title ?: "",
                            color =   BaseTheme.BaseTextColor,
                            fontSize = 22.sp,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp)
                        )
                    }
                }
                val currentTick = rebuildTick
                pageModel.dataEntryPageElements.forEachIndexed { i, field ->
                    // Key by the element's identity (not its index) so that inserting or
                    // removing child fields above does not reset the state of fields below.
                    // `as String?` guards against Gson leaving the non-null field null.
                    val stableKey: Any = (field.elementIdentifier as String?)
                        ?: field.inputKey
                        ?: i
                    key(stableKey) {
                    Box(modifier = Modifier.padding(end = 10.dp)) {
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
                                        FilterManager.updateFilter();
                                    },
                                    page = page,
                                    field = field,
                                    loadedMap = loadedMap,
                                    loadingMap = loadingMap,
                                    filterMap= filterMap,
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
                                options = allCountries
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
                        InputTypes.CheckboxGroup -> {
                            SecureCheckboxGroup(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    val result = new.joinToString(",")
                                    AssistedFormHelper.changeValue(field.inputKey!!, result, page)
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
                        InputTypes.Checkbox -> {
                            SecureCheckbox(
                                title = field.textTitle!!,
                                onValueChange = { new ->
                                    AssistedFormHelper.changeValue(field.inputKey!!, new.toString(), page)
                                    onFieldChanged()
                                },
                                page = page,
                                field = field,
                            )
                        }
                    }
                }}}

                Spacer(Modifier.height(300.dp))
            }

        }

        Spacer(Modifier.height(500.dp))
    }
}



/**
 * Scrollable (non-lazy) column with a custom scrollbar.
 * All children are composed up front — see AssistedDataEntryPager for why.
 */
@Composable
fun ScrollColumnWithScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    scrollbarWidth: Dp = 8.dp,
    trackColor: Color,
    thumbColor: Color,
    thumbMinHeight: Dp = 48.dp,
    thumbMaxHeight: Dp = 160.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state)
                .padding(contentPadding),
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
    state: ScrollState,
    modifier: Modifier = Modifier,
    width: Dp,
    trackColor: Color,
    thumbColor: Color,
    thumbMinHeight: Dp,
    thumbMaxHeight: Dp = 180.dp,
) {
    // Hide when content doesn't scroll
    val showScrollbar by remember {
        derivedStateOf { state.maxValue > 0 }
    }

    if (!showScrollbar) return

    Canvas(modifier = modifier.width(width)) {

        // Track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(size.width / 2f, size.width / 2f)
        )

        // The scrollbar fills the viewport height, so the canvas height is the viewport.
        // ScrollState gives exact numbers (no per-item size estimation needed).
        val viewportPx = size.height.coerceAtLeast(1f)
        val maxScrollPx = state.maxValue.toFloat().coerceAtLeast(1f)
        val totalContentPx = viewportPx + maxScrollPx
        val fraction = (state.value / maxScrollPx).coerceIn(0f, 1f)

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

