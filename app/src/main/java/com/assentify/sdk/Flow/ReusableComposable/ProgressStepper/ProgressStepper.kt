package com.assentify.sdk.Flow.ReusableComposable.ProgressStepper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Flow.BlockLoader.BaseTheme


enum class StepVisualState { Done, Active, Upcoming }
@Composable
fun ProgressStepper(
    normalModifier: Modifier = Modifier,
    percentageBased: Modifier = Modifier,
    onBack: () -> Unit = {},
    ){
    if(BaseTheme.StepperType == StepperType.Normal){
        NormalProgressStepper(modifier = normalModifier)
    }else{
        PercentageBasedProgressStepper(modifier = percentageBased, onBack = { onBack() })
    }
}

