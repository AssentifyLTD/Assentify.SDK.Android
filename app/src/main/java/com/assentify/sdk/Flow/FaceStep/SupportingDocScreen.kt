package com.assentify.sdk.Flow.FaceStep

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.assentify.sdk.ConfigModelObject
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper.ProgressStepper
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.Customization
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportingDocScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDocumentSelected: (Templates) -> Unit,
    onTemplateClick: (Templates) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = flowStrings()
    val context = LocalContext.current
    val  faceCurrentStep = FlowController.getCurrentStep()!!.stepDefinition!!.customization;

    // Supporting-document templates (no country selection)
    val supportingDocTemplates = remember(faceCurrentStep) {
        AssentifySdkObject.getAssentifySdkObject()
            .getTemplatesForSupportingDoc(
                faceCurrentStep.supportingDocumentAcceptedDocuments.orEmpty()
            )
    }

    val hasPassport = true
    val hasID = supportingDocTemplates.templates.isNotEmpty()

    var selectedTemplate by remember { mutableStateOf<Templates?>(null) }

    val iconPainterPassport = remember("ic_passport.svg") {
        loadSvgFromAssets(context, "ic_passport.svg")
    }
    val iconPainterID = remember("id_card.svg") {
        loadSvgFromAssets(context, "id_card.svg")
    }


    BaseBackgroundContainer(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP + MIDDLE
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                if (BaseTheme.StepperType == StepperType.Normal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                .size(40.dp)
                                .align(Alignment.CenterVertically),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                ProgressStepper(
                    onBack = { onBack() },
                    normalModifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    percentageBased = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 20.dp)
                )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = s.supportingDocumentTitle ,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        color = BaseTheme.BaseTextColor,
                        fontSize = 23.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = s.supportingDocumentTMsg  ,
                        fontFamily = InterFont,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = BaseTheme.BaseTextColor.copy(0.5F),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp, start = 20.dp, end = 20.dp)
                    )


                // MIDDLE CONTENT
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        s.selectDocument,
                        color = BaseTheme.BaseTextColor,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DocumentPicker(
                        minVerticalPadding = false,
                        country = supportingDocTemplates,
                        hasPassport = hasPassport,
                        hasID = hasID,
                        iconPainterPassport = iconPainterPassport,
                        iconPainterIDCard = iconPainterID,
                        selectedBg = Color(android.graphics.Color.parseColor(BaseTheme.BaseAccentColor)),
                        unselectedBg = BaseTheme.FieldColor,
                        selectedTemplate = selectedTemplate,
                        onDocumentSelected = { template ->
                            selectedTemplate = template
                            onDocumentSelected(template)
                        },
                        onTemplateClick = { template ->
                            selectedTemplate = template
                            onTemplateClick(template)
                        },
                    )
                }
            }

            // BOTTOM
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    s.idDisclaimer(ConfigModelObject.getConfigModelObject()!!.flowName),
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
                Button(
                    onClick = {
                        if (selectedTemplate != null) onNext()
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
                        s.next,
                        fontFamily = InterFont,
                        fontWeight = BaseTheme.BaseClickFontWeight,
                        modifier = Modifier.padding(vertical = 7.dp),
                        color = BaseTheme.BaseSecondaryTextColor
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentPicker(
    country: TemplatesByCountry,
    hasPassport: Boolean,
    hasID: Boolean,
    iconPainterPassport: Painter?,
    iconPainterIDCard: Painter?,
    selectedBg: Color,
    unselectedBg: Color,
    selectedTemplate: Templates?,
    minVerticalPadding: Boolean,
    onDocumentSelected: (Templates) -> Unit,
    onTemplateClick: (Templates) -> Unit,
) {
    val s = flowStrings()

    fun isSelectedPassport(): Boolean = selectedTemplate?.id == -1
    fun isSelectedIDs(): Boolean = selectedTemplate != null && selectedTemplate.id != -1

    var showIdsSheet by remember { mutableStateOf(false) }

    // Bottom sheet with the accepted supporting documents
    if (showIdsSheet) {
        TemplatesBottomSheet(
            title = s.supportedIds,
            templates = country.templates,
            onDismiss = { showIdsSheet = false },
            onTemplateClick = { template -> onTemplateClick(template) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {

        if (hasPassport) {
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
                                    sourceCountryCode = country.sourceCountryCode,
                                    kycDocumentType = "Passport",
                                    sourceCountry = "",
                                    kycDocumentDetails = emptyList()
                                )
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
                            .padding(
                                horizontal = 35.dp,
                                vertical = if (minVerticalPadding) 18.dp else 28.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        iconPainterPassport?.let {
                            Image(
                                painter = it,
                                contentDescription = "passport",
                                modifier = Modifier.size(if (minVerticalPadding) 40.dp else 60.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(
                                    if (isSelectedPassport()) BaseTheme.BaseSecondaryTextColor
                                    else BaseTheme.BaseTextColor
                                )
                            )
                        }
                        Spacer(Modifier.width(40.dp))
                        Text(
                            text = s.passport,
                            color = if (isSelectedPassport()) BaseTheme.BaseSecondaryTextColor
                            else BaseTheme.BaseTextColor,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        if (hasID) {
            item(key = "supported_ids") {
                Spacer(Modifier.height(10.dp))
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
                            .padding(
                                horizontal = 35.dp,
                                vertical = if (minVerticalPadding) 10.dp else 28.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        iconPainterIDCard?.let {
                            Image(
                                painter = it,
                                contentDescription = "id_card",
                                modifier = Modifier.size(if (minVerticalPadding) 40.dp else 60.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(
                                    if (isSelectedIDs()) BaseTheme.BaseSecondaryTextColor
                                    else BaseTheme.BaseTextColor
                                )
                            )
                        }

                        Spacer(Modifier.width(40.dp))

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = s.supportedIds,
                                color = if (isSelectedIDs()) BaseTheme.BaseSecondaryTextColor
                                else BaseTheme.BaseTextColor,
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            )

                            // View more => open list
                            Text(
                                text = s.viewMore,
                                color = if (isSelectedIDs()) BaseTheme.BaseSecondaryTextColor
                                else BaseTheme.BaseTextColor,
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesBottomSheet(
    title: String,
    templates: List<Templates>,
    onDismiss: () -> Unit,
    onTemplateClick: (Templates) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val headerHeight = 52.dp
    val itemHeight = 88.dp
    val verticalPadding = 32.dp
    val maxSheetHeight = screenHeight * 0.75f

    val sheetHeight = remember(templates.size) {
        val calculated = headerHeight + verticalPadding + (itemHeight * templates.size.toFloat())
        calculated.coerceAtMost(maxSheetHeight)
    }

    ModalBottomSheet(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 150.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.Transparent,
        dragHandle = null,
        sheetGesturesEnabled = false
    ) {
        BaseBackgroundContainer(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    modifier = Modifier.fillMaxSize(),
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
                                .clickable {
                                    onDismiss()
                                    onTemplateClick(template)
                                },
                            colors = CardDefaults.cardColors(containerColor = BaseTheme.FieldColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
            }
        }
    }
}

fun getIDStepFromConfigFile(
    configModel: ConfigModel,
    id: Int,
): Customization? = configModel.stepDefinitions
    .firstOrNull { it.stepId == id }
    ?.customization