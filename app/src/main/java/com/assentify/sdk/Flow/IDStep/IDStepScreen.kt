package com.assentify.sdk.Flow.IDStep

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.FileUtils.loadSvgFromAssets
import com.assentify.sdk.Flow.FlowController.FlowController
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
    val countries = remember { AssentifySdkObject.getAssentifySdkObject().getTemplates(
        FlowController.getCurrentStep()!!.stepDefinition!!.stepId) }


    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    var selectedCountry by remember { mutableStateOf<TemplatesByCountry?>(countries.first()) }
    var selectedTemplate by remember { mutableStateOf<Templates?>(null) }


    val countryList = remember(countries) { countries }

    val context = LocalContext.current

    val iconPainter = remember("ic_passport.svg") {
        loadSvgFromAssets(context, "ic_passport.svg")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
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
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    logoBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
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
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    CountryDropdownStyled(
                        countryList = countryList,
                        selectedCountry = selectedCountry,
                        onCountrySelected = { selectedCountry = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Select type of document",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Template list (scrolls inside the middle area)
                    selectedCountry?.let { country ->

                        var selectedKey by remember(country) { mutableStateOf<String?>(null) }

                        val selectedBg =
                            Color(android.graphics.Color.parseColor(flowEnv.listItemsSelectedHexColor))
                        val unselectedBg =
                            Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 4.dp)
                        ) {
                            // Default passport item
                            item(key = "default_passport") {
                                val key = "default_passport"
                                val isSelected = selectedKey == key

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedKey = key
                                            selectedTemplate = Templates(
                                                id = -1,
                                                sourceCountryFlag = "",
                                                sourceCountryCode = "",
                                                kycDocumentType = "",
                                                sourceCountry = "",
                                                kycDocumentDetails = emptyList()
                                            )
                                            onDocumentSelected(selectedTemplate!!)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) selectedBg else unselectedBg
                                    ),
                                    border = if (isSelected) BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.15f)
                                    ) else null,
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 6.dp else 2.dp
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        iconPainter?.let {
                                            Icon(
                                                painter = it,
                                                contentDescription = "passport",
                                                modifier = Modifier.size(60.dp),
                                                tint = Color.Unspecified
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Passport",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }

                            // Dynamic templates
                            items(
                                items = country.templates,
                                key = { t -> (t.id ?: t.kycDocumentType.hashCode()).toString() }
                            ) { template ->
                                val key = "t_${template.id ?: template.kycDocumentType}"
                                val isSelected = selectedKey == key

                                Spacer(modifier = Modifier.height(4.dp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedKey = key
                                            selectedTemplate = template
                                            onDocumentSelected(template)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) selectedBg else unselectedBg
                                    ),
                                    border = if (isSelected) BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.15f)
                                    ) else null,
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 6.dp else 2.dp
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                template.kycDocumentDetails.first().templateSpecimen
                                            ),
                                            contentDescription = template.kycDocumentType,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = template.kycDocumentType,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
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
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTemplate != null)
                            Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor))
                        else
                            Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Text(
                        "Next",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp)
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

    val pillColor = Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor))
    val textColor = Color.White.copy(alpha = 0.95f)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            singleLine = true,
            value = selectedCountry?.name ?: "Select country",
            onValueChange = { },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown Arrow",
                    tint = Color.White.copy(alpha = 0.8f),
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
                cursorColor = Color.White
            ),
            modifier = Modifier
                .menuAnchor()
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
            modifier = Modifier.background(Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)))
        ) {
                Box(
                    modifier = Modifier
                        .background(Color(android.graphics.Color.parseColor(flowEnv.listItemsUnSelectedHexColor)))
                ) {
                    Column {
                        countryList.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country.name, color = Color.White) },
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

