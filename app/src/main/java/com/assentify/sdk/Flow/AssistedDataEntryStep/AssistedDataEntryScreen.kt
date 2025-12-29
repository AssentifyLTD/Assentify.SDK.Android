package com.assentify.sdk.Flow.AssistedDataEntryStep

import AssistedFormHelper
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryModel
import com.assentify.sdk.Flow.ReusableComposable.Events.EventTypes
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper
import com.assentify.sdk.FlowEnvironmentalConditionsObject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AssistedDataEntryScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    assistedDataEntryModel: AssistedDataEntryModel?,
    eventTypes: String,
    modifier: Modifier = Modifier
) {

    val flowEnv = FlowEnvironmentalConditionsObject.getFlowEnvironmentalConditions()
    val logoBitmap: ImageBitmap? = remember(flowEnv.appLogo) {
        flowEnv.appLogo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }





    @OptIn(ExperimentalFoundationApi::class)
    var pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 0}
    )

    val scope = rememberCoroutineScope()


    var currentPage by remember { mutableIntStateOf(0) }
    var changeTick by remember { mutableIntStateOf(0) }
    val enabled = remember(pagerState.currentPage, changeTick,eventTypes) {
        if(eventTypes == EventTypes.onComplete){
            AssistedFormHelper.validatePage(currentPage)
        }else{
            false
        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(android.graphics.Color.parseColor(flowEnv.backgroundHexColor)))
    ) {


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                logoBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(10.dp))

            ProgressStepper(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )

        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 25.dp, bottom = 0.dp)
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(24.dp))

                when (eventTypes) {
                    EventTypes.onSend -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(60.dp)
                                .align(Alignment.CenterHorizontally),
                            color =Color(android.graphics.Color.parseColor(flowEnv.textHexColor)),
                            strokeWidth = 6.dp
                        )
                    }

                    EventTypes.onError -> {
                        Text(
                            text = "Something went wrong",
                            color = Color.Red,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }

                    EventTypes.onComplete -> {

                        pagerState = rememberPagerState(
                            pageCount = { assistedDataEntryModel!!.assistedDataEntryPages.size }
                        )

                        val cfg = LocalConfiguration.current
                        val screenH = cfg.screenHeightDp.dp

                        val headerReserve = 150.dp + 60.dp
                        val bottomReserve = 20.dp
                        val pagerHeight = (screenH - headerReserve - bottomReserve)
                            .coerceAtLeast(260.dp)

                        AssistedDataEntryPager(
                            assistedDataEntryModel = assistedDataEntryModel,
                            modifier = Modifier.height(pagerHeight),
                            pagerState = pagerState,
                            onFieldChanged = { changeTick++ }
                        )
                    }
                }
            }

            if(eventTypes == EventTypes.onComplete){
                val lastIndex = pagerState.pageCount - 1
                Button(
                    onClick = {

                        scope.launch {
                            if (enabled && pagerState.currentPage < lastIndex) {
                                currentPage++;
                                changeTick++
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                if(enabled){
                                    onNext()
                                }
                            }

                        }
                    },
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(android.graphics.Color.parseColor(flowEnv.clicksHexColor)),
                        contentColor = Color(android.graphics.Color.parseColor(flowEnv.textHexColor))
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Text(
                        assistedDataEntryModel!!.assistedDataEntryPages[pagerState.currentPage].nextButtonTitle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

        }

    }

}