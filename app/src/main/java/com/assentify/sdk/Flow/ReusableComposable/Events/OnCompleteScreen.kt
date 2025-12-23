package com.assentify.sdk.Flow.ReusableComposable.Events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.Flow.ReusableComposable.SecureImage
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import com.assentify.sdk.NfcPassportResponseModelObject
import com.assentify.sdk.OnCompleteScreenData

@Composable
fun OnCompleteScreen(
    imageUrl: String,
    onNext: () -> Unit = {},
) {
    val extractedMap = OnCompleteScreenData.getData()
    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()

    // ---------- helpers ----------
    fun Any?.asCleanString(): String? {
        val s = this?.toString()?.trim()
        if (s.isNullOrBlank()) return null
        if (s.equals("null", ignoreCase = true)) return null
        return s
    }

    fun keyLabel(key: String): String {
        // title is the last word after "_" (as requested)
        return key.substringAfterLast("_").replaceFirstChar { it.uppercase() }
    }

    // ✅ allowed keys by "contains" (ignore UUID prefixes)
    val allowedKeyParts = remember {
        listOf(
            "OnBoardMe_IdentificationDocumentCapture_Document_Number",
            "OnBoardMe_IdentificationDocumentCapture_Birth_Date",
            "OnBoardMe_IdentificationDocumentCapture_name",
            "OnBoardMe_IdentificationDocumentCapture_surname",
            "OnBoardMe_IdentificationDocumentCapture_ID_FathersName",
            "OnBoardMe_IdentificationDocumentCapture_ID_MothersName",
            "OnBoardMe_IdentificationDocumentCapture_ID_PlaceOfBirth",
            "OnBoardMe_IdentificationDocumentCapture_Document_Type",
            "OnBoardMe_IdentificationDocumentCapture_IDType",
            "OnBoardMe_IdentificationDocumentCapture_Country",
            "OnBoardMe_IdentificationDocumentCapture_Nationality",
            "OnBoardMe_IdentificationDocumentCapture_Image",
            "OnBoardMe_IdentificationDocumentCapture_ID_CivilRegisterNumber",
            "OnBoardMe_IdentificationDocumentCapture_ID_DateOfIssuance",
            "OnBoardMe_IdentificationDocumentCapture_Sex",
            "OnBoardMe_IdentificationDocumentCapture_ID_MaritalStatus",
            "OnBoardMe_IdentificationDocumentCapture_ID_PlaceOfResidence",
            "OnBoardMe_IdentificationDocumentCapture_ID_Province",
            "OnBoardMe_IdentificationDocumentCapture_ID_Governorate",
            "OnBoardMe_IdentificationDocumentCapture_FaceCapture",
        )
    }

    fun isAllowedKey(key: String): Boolean {
        return allowedKeyParts.any { part ->
            key.contains(part, ignoreCase = true)
        }
    }

    // Use your colors (no colors from screenshot)
    val bgColor = Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor))
    val textColor = Color(android.graphics.Color.parseColor(flowEnv.textHexColor))
    val primary = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor))

    // ---------- data rows ----------
    val dataRows = remember(extractedMap) {
        extractedMap?.entries
            ?.filter { isAllowedKey(it.key) }
            ?.mapNotNull { (k, v) ->
                // 🔥 IGNORE BY ORIGINAL KEY (not label)
                if (
                    k.contains("OnBoardMe_IdentificationDocumentCapture_Image", ignoreCase = true) ||
                    k.contains("OnBoardMe_IdentificationDocumentCapture_FaceCapture", ignoreCase = true)
                ) return@mapNotNull null

                val value = v.asCleanString() ?: return@mapNotNull null
                val label = keyLabel(k)
                label to value
            }
            ?.sortedBy { it.first.lowercase() }
            ?: emptyList()
    }

    // ---------- images ----------
    val imageUrls = remember(extractedMap) {
        val list = mutableListOf<String>()

        // Front image
        extractedMap?.entries
            ?.firstOrNull { it.key.contains("OnBoardMe_IdentificationDocumentCapture_Image", ignoreCase = true) }
            ?.value
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let { list.add(it) }

        // Face (passport NFC first, else FaceCapture)
        if (NfcPassportResponseModelObject.getPassportResponseModelObject() != null) {
            val face = NfcPassportResponseModelObject
                .getPassportResponseModelObject()
                ?.passportExtractedModel
                ?.faces
                ?.firstOrNull()

            if (!face.isNullOrBlank()) list.add(face)
        } else {
            extractedMap?.entries
                ?.firstOrNull { it.key.contains("OnBoardMe_IdentificationDocumentCapture_FaceCapture", ignoreCase = true) }
                ?.value
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { list.add(it) }
        }

        list
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.height(130.dp))
        // ---- Images header (2 thumbnail cards) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(
                    color = primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(14.dp)
        ) {
            ImagesHeader(
                imageUrls = imageUrls
            )
        }


        Spacer(modifier = Modifier.height(14.dp))

        // ---- List container ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight / 2.2f)
                .background(
                    color = primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dataRows) { (label, value) ->
                    // ✅ status logic: ok if value not blank, error otherwise
                    val isOk = value.isNotBlank()

                    PrettyListRow(
                        label = label,
                        value = value,
                        primary = primary,
                        textColor = textColor,
                        isOk = isOk
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Button (keep as is) ----
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = textColor
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {
            Text(
                "Next",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}

/* ---------------------------------------------
   Images header (2 thumbnail cards)
---------------------------------------------- */
@Composable
private fun ImagesHeader(
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    if (imageUrls.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = minOf(imageUrls.size, 2)

        repeat(count) { index ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        SecureImage(imageUrl = imageUrls[index])
                    }
                }
            }
        }

        // Optional symmetry if only 1 image
        if (imageUrls.size == 1) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) { /* empty */ }
        }
    }
}

/* ---------------------------------------------
   Pretty row like your 2nd screenshot
---------------------------------------------- */
@Composable
private fun PrettyListRow(
    label: String,
    value: String,
    primary: Color,
    textColor: Color,
    isOk: Boolean = true,
    modifier: Modifier = Modifier
) {
    val icon = if (isOk) Icons.Filled.CheckCircle else Icons.Filled.Error
    val iconTint = primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(primary)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatLabel(label),
                    color = textColor.copy(alpha = 0.80f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(10.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

/* ---------------------------------------------
   Label formatting
---------------------------------------------- */
fun formatLabel(raw: String): String {
    return raw
        .replace("_", " ")
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}
