package com.assentify.sdk.Flow.IDStep

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDStepScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDocumentSelected: (Templates) -> Unit,
    modifier: Modifier = Modifier
) {
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }
    val countries = remember {
        AssentifySdkObject.getAssentifySdkObject().getTemplates(
            FlowController.getCurrentStep()!!.stepDefinition!!.stepId
        )
    }




    var selectedCountry by remember { mutableStateOf<TemplatesByCountry?>(countries.first()) }
    var selectedTemplate by remember { mutableStateOf<Templates?>(null) }


    val countryList = remember(countries) { countries }

    val context = LocalContext.current

    val iconPainterPassport = remember("ic_passport.svg") {
        loadSvgFromAssets(context, "ic_passport.svg")
    }

    val iconPainterID = remember("id_card.svg") {
        loadSvgFromAssets(context, "id_card.svg")
    }

    BaseBackgroundContainer(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .systemBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP + MIDDLE
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BaseTheme.BaseTextColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(BaseTheme.BaseLogo)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.CenterVertically),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(Modifier.height(10.dp))

                ProgressStepper(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )

                // MIDDLE CONTENT – takes remaining space between top and bottom
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        "Choose your country of residence",
                        color = BaseTheme.BaseTextColor,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    CountryDropdownStyled(
                        countryList = countryList,
                        selectedCountry = selectedCountry,
                        onCountrySelected = {
                            selectedTemplate = null;
                            selectedCountry = it
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Select type of document",
                        color = BaseTheme.BaseTextColor,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Template list (scrolls inside the middle area)
                    selectedCountry?.let { country ->


                        val selectedBg =
                            Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor))
                        val unselectedBg =
                            BaseTheme.FieldColor

                        DocumentPicker(
                            country = country,   // your country model
                            iconPainterPassport = iconPainterPassport,
                            iconPainterIDCard = iconPainterID,
                            selectedBg = selectedBg,
                            unselectedBg = unselectedBg,
                            selectedTemplate = selectedTemplate,
                            onDocumentSelected = { template ->
                                selectedTemplate = template
                                onDocumentSelected(template);
                            }
                        )

                    }
                }
            }

            // BOTTOM (always at end of screen)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(5.dp))
                Text(
                    "Only the presented IDs are supported and accepted by NXT Finance. Make sure to provide one of them.",
                    color = BaseTheme.BaseTextColor,
                    fontSize = 12.sp,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Thin,
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp)
                )
                Spacer(Modifier.height(5.dp))
                Button(
                    onClick = {
                        if (selectedTemplate != null) {
                            onNext()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),

                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 25.dp, horizontal = 25.dp)
                        .background(
                            brush = if (selectedTemplate != null)
                                BaseTheme.BaseClickColor!!.toBrush()
                            else
                                SolidColor(BaseTheme.FieldColor),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Text(
                        "Next",
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 7.dp),
                        color = BaseTheme.BaseSecondaryTextColor

                    )
                }
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDropdownStyled(
    countryList: List<TemplatesByCountry>,
    selectedCountry: TemplatesByCountry?,
    onCountrySelected: (TemplatesByCountry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }

    val pillColor = BaseTheme.FieldColor
    val textColor = BaseTheme.BaseTextColor

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            singleLine = true,
            value = selectedCountry?.name ?: "Select country",
            onValueChange = { },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontFamily = InterFont,
                fontWeight = FontWeight.Light,
            ),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown Arrow",
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(30.dp)
                )
            },
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = pillColor,
                unfocusedContainerColor = pillColor,
                disabledContainerColor = pillColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = textColor
            ),
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth()
                .height(60.dp) // pill height
                .clip(RoundedCornerShape(28.dp))
                .then(
                    Modifier // subtle outline, optional
                        .background(pillColor, RoundedCornerShape(28.dp))
                )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(BaseTheme.FieldColor)
        ) {
            Box(
                modifier = Modifier
                    .background(BaseTheme.FieldColor)
            ) {
                Column {
                    countryList.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    country.name, color = textColor,
                                    fontFamily = InterFont,
                                    fontWeight = FontWeight.Light,
                                )
                            },
                            onClick = {
                                onCountrySelected(country)
                                expanded = false
                            }
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentPicker(
    country: TemplatesByCountry,
    iconPainterPassport: Painter?,
    iconPainterIDCard: Painter?,
    selectedBg: Color,
    unselectedBg: Color,
    selectedTemplate: Templates?,
    onDocumentSelected: (Templates) -> Unit,
) {
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }

    fun isSelectedPassport(): Boolean = selectedTemplate?.id == -1
    fun isSelectedIDs(): Boolean = selectedTemplate != null && selectedTemplate.id != -1

    var showIdsSheet by remember { mutableStateOf(false) }


    // ✅ Bottom sheet for templates list
    if (showIdsSheet) {
        TemplatesBottomSheet(
            title = "Supported ${
                country.templates.first().kycDocumentType.trim().split(" ")
                    .firstOrNull() ?: ""
            } IDs",
            templates = country.templates,
            onDismiss = { showIdsSheet = false },

            )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {

        // ===== 1) Passport card =====
        item(key = "default_passport") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        onDocumentSelected(
                            Templates(
                                id = -1,
                                sourceCountryFlag = "",
                                sourceCountryCode = "",
                                kycDocumentType = "Passport",
                                sourceCountry = "",
                                kycDocumentDetails = emptyList()
                            )!!
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelectedPassport()) selectedBg else unselectedBg
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelectedPassport()) 6.dp else 2.dp
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 35.dp, vertical = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    iconPainterPassport?.let {
                        Image(
                            painter = it,
                            contentDescription = "passport",
                            modifier = Modifier.size(60.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(
                                if (isSelectedPassport())
                                    BaseTheme.BaseSecondaryTextColor
                                else
                                    BaseTheme.BaseTextColor,
                      )
                        )
                    }
                    Spacer(Modifier.width(40.dp))
                    Text(
                        text = "${
                            country.templates.first().kycDocumentType.trim().split(" ")
                                .firstOrNull() ?: ""
                        } Passport",
                        color = if (isSelectedPassport())
                            BaseTheme.BaseSecondaryTextColor
                        else
                            BaseTheme.BaseTextColor,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // ===== 2) Supported IDs card =====
        item(key = "supported_ids") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        onDocumentSelected(
                            Templates(
                                id = 1,
                                sourceCountryFlag = "",
                                sourceCountryCode = country.sourceCountryCode,
                                kycDocumentType = "All IDs",
                                sourceCountry = "",
                                kycDocumentDetails = emptyList()
                            )
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelectedIDs()) selectedBg else unselectedBg
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelectedIDs()) 6.dp else 2.dp
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 35.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    iconPainterIDCard?.let {
                        Image(
                            painter = it,
                            contentDescription = "id_card",
                            modifier = Modifier.size(60.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(
                                if (isSelectedIDs())
                                    BaseTheme.BaseSecondaryTextColor
                                else
                                    BaseTheme.BaseTextColor,
                            )
                        )
                    }

                    Spacer(Modifier.width(40.dp))

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Supported\n${
                                country.templates.first().kycDocumentType.trim().split(" ")
                                    .firstOrNull() ?: ""
                            } IDs",
                            color =     if (isSelectedIDs())
                                BaseTheme.BaseSecondaryTextColor
                            else
                                BaseTheme.BaseTextColor,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )

                        // ✅ View more => open list
                        Text(
                            text = "View more",
                            color =     if (isSelectedIDs())
                                BaseTheme.BaseSecondaryTextColor
                            else
                                BaseTheme.BaseTextColor,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { showIdsSheet = true },
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesBottomSheet(
    title: String,
    templates: List<Templates>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val flowEnv = remember { FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions() }

    ModalBottomSheet(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 150.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.Transparent
    ) {
        BaseBackgroundContainer(
            modifier = Modifier.fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = title,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                color = BaseTheme.BaseTextColor,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 15.dp)
            ) {
                items(
                    items = templates,
                    key = { it.id }
                ) { template ->
                    val iconUrl = template.kycDocumentDetails
                        .firstOrNull()
                        ?.templateSpecimen
                        ?.takeIf { it.isNotBlank() }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { },
                        colors = CardDefaults.cardColors(
                            containerColor = BaseTheme.FieldColor
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon (from URL)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(0.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = iconUrl,
                                    contentDescription = "template_icon",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = template.kycDocumentType,
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Bold,
                                color = BaseTheme.BaseTextColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }}
    }
}

