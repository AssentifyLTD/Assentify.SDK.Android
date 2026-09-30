package com.assentify.sdk.Flow.QuestionnaireStep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.assentify.sdk.AssentifySdkObject
import com.assentify.sdk.Core.Constants.StepperType
import com.assentify.sdk.Core.Constants.getCurrentDateTimeForTracking
import com.assentify.sdk.Core.Constants.toBrush
import com.assentify.sdk.Flow.BlockLoader.BaseTheme
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.Flow.FlowController.InterFont
import com.assentify.sdk.Flow.FlowController.flowStrings
import com.assentify.sdk.Flow.ReusableComposable.BaseBackgroundContainer
import com.assentify.sdk.Flow.ReusableComposable.BaseClick
import com.assentify.sdk.Flow.ReusableComposable.LogoSvgUrl
import com.assentify.sdk.Flow.ReusableComposable.ProgressStepper.ProgressStepper
// ⚠️ Adjust these to wherever Questionnaire / its callback / models live in your SDK.
import com.assentify.sdk.Questionnaire.AnswerModel
import com.assentify.sdk.Questionnaire.QuestionModel
import com.assentify.sdk.Questionnaire.Questionnaire
import com.assentify.sdk.Questionnaire.QuestionnaireCallback
import com.assentify.sdk.Questionnaire.QuestionnaireModel
import kotlinx.coroutines.launch

object QuestionnaireStepEventTypes {
    const val onSend = "onSend"
    const val onError = "onError"
    const val onComplete = "onComplete"
}

class QuestionnaireActivity : FragmentActivity(), QuestionnaireCallback {

    private val eventType = mutableStateOf(QuestionnaireStepEventTypes.onSend)
    private val questionnaireObject = mutableStateOf<QuestionnaireModel?>(null)
    private lateinit var questionnaire: Questionnaire

    private var timeStarted = getCurrentDateTimeForTracking()

    val assentifySdk = AssentifySdkObject.getAssentifySdkObject()
    private var isNavigating = false

    /** question index -> selected answers, kept in the order the user picked them. */
    private val selections = mutableStateMapOf<Int, List<AnswerModel>>()

    /** Pages where the user tried to continue without answering (to show the inline error). */
    private val errorPages = mutableStateListOf<Int>()

    /** Output property that receives the total weight, e.g. "<uuid>_OnBoardMe_Questionnaire_TotalWeight". */
    private var totalWeightKey: String? = null

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentStep = FlowController.getCurrentStep()!!
        val outputs = FlowController.outputPropertiesToMap(currentStep.stepDefinition!!.outputProperties)

        FlowController.trackProgress(
            currentStep = currentStep,
            response = null,
            inputData = outputs,
            status = "InProgress"
        )

        totalWeightKey = outputs.keys.firstOrNull { it.endsWith(TOTAL_WEIGHT_SUFFIX) }

        questionnaire = assentifySdk.startQuestionnaire(
            this,
            currentStep.stepDefinition!!.stepId
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FlowController.backClick(this@QuestionnaireActivity)
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuestionnaireScreen(
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        eventTypes = eventType.value,
                        model = questionnaireObject.value
                    )
                }
            }
        }
    }

    companion object {
        private const val TOTAL_WEIGHT_SUFFIX = "_TotalWeight"

        fun start(context: Context) {
            context.startActivity(Intent(context, QuestionnaireActivity::class.java))
        }
    }

    // ───────────── SDK callbacks ─────────────

    override fun onQuestionnaireCallbackSuccess(questionnaireModel: QuestionnaireModel) {
        selections.clear()
        errorPages.clear()
        questionnaireObject.value = questionnaireModel
        eventType.value = QuestionnaireStepEventTypes.onComplete
    }

    override fun onQuestionnaireCallbackError(message: String) {
        eventType.value = QuestionnaireStepEventTypes.onError
    }

    // ───────────── Answers & result ─────────────

    private fun isAnswered(index: Int): Boolean = !selections[index].isNullOrEmpty()

    /**
     * Single choice: the new answer replaces the old one.
     * Multiple choice: toggles the answer, preserving pick order ("Answer 3;Answer 2").
     * Because the result is always rebuilt from [selections], changing an answer changes the result.
     */
    private fun toggleAnswer(index: Int, question: QuestionModel, answer: AnswerModel) {
        val current = selections[index].orEmpty()
        val isSelected = current.any { it.key == answer.key }

        val updated = when {
            question.allowMultipleAnswers && isSelected -> current.filterNot { it.key == answer.key }
            question.allowMultipleAnswers -> current + answer
            else -> listOf(answer)
        }

        if (updated.isEmpty()) selections.remove(index) else selections[index] = updated
        if (updated.isNotEmpty()) errorPages.remove(index)
    }

    /** Weight contributed by one selected answer. Change here if question.weight should factor in. */
    private fun answerWeight(question: QuestionModel, answer: AnswerModel): Int = answer.weight

    private fun totalWeight(questions: List<QuestionModel>): Int =
        questions.indices.sumOf { i ->
            selections[i].orEmpty().sumOf { answerWeight(questions[i], it) }
        }

    /** Accepts answer keys that are either already full ("<keyProperty>_Answer3") or short ("Answer3"). */
    private fun answerKeyFor(question: QuestionModel, answer: AnswerModel): String =
        if (answer.key.startsWith(question.keyProperty)) answer.key
        else "${question.keyProperty}_${answer.key}"

    private fun buildResult(questions: List<QuestionModel>): Map<String, String> {
        val result = linkedMapOf<String, String>()

        questions.forEachIndexed { i, q ->
            val picked = selections[i].orEmpty()
            if (picked.isEmpty()) return@forEachIndexed

            result[q.keyProperty] = picked.joinToString(";") { answerKeyFor(q, it) }
            result[q.valueProperty] = picked.joinToString(";") { it.value }
        }

        totalWeightKey?.let { result[it] = totalWeight(questions).toString() }
        return result
    }

    private fun submit(questions: List<QuestionModel>) {
        if (isNavigating) return
        isNavigating = true
        FlowController.makeCurrentStepDone(buildResult(questions), timeStarted)
        FlowController.naveToNextStep(this)
    }

    private fun markError(index: Int) {
        if (index !in errorPages) errorPages.add(index)
    }

    // ───────────── UI ─────────────

    @Composable
    fun QuestionnaireScreen(
        onBack: () -> Unit,
        eventTypes: String,
        model: QuestionnaireModel?,
    ) {
        val s = flowStrings()

        BaseBackgroundContainer(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                TopBar(onBack)

                val questions = model?.questions.orEmpty()

                when {
                    eventTypes == QuestionnaireStepEventTypes.onError -> CenteredMessage(
                        text = s.qLoadFailed,
                        modifier = Modifier.weight(1f)
                    )

                    eventTypes != QuestionnaireStepEventTypes.onComplete || model == null -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BaseTheme.BaseTextColor)
                    }

                    questions.isEmpty() -> CenteredMessage(
                        text = s.qNoQuestions,
                        modifier = Modifier.weight(1f)
                    )

                    else -> QuestionnaireContent(
                        model = model,
                        questions = questions,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun TopBar(onBack: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    Spacer(Modifier.weight(1f))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(BaseTheme.BaseLogo)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp)) // balances the back button
                }
            }

            Spacer(Modifier.height(10.dp))

            ProgressStepper(
                onBack = { onBack() },
                normalModifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                percentageBased = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )
        }
    }

    @Composable
    private fun QuestionnaireContent(
        model: QuestionnaireModel,
        questions: List<QuestionModel>,
        modifier: Modifier,
    ) {
        val s = flowStrings()
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { questions.size })
        val page = pagerState.currentPage
        val isLast = page == questions.lastIndex

        // System back goes to the previous question first, then leaves the step.
        BackHandler(enabled = page > 0) {
            scope.launch { pagerState.animateScrollToPage(page - 1) }
        }

        Column(modifier = modifier.fillMaxWidth()) {

            Header(model)

            PageIndicator(count = questions.size, current = page)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top
            ) { index ->
                QuestionPage(
                    index = index,
                    question = questions[index],
                    total = questions.size
                )
            }

            // ───────────── BOTTOM (fixed) ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 25.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                    }) {
                        Text(
                            text = s.qPrevious,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Medium,
                            color = BaseTheme.BaseTextColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                val active = isAnswered(page)

                // key(page) resets BaseClick's internal (slider) state for every page.
                key(page) {
                    BaseClick(
                        isNormalClick = if (isLast) model.isNormalClick ?: true else true,
                        label = if (isLast) model.nextButtonTitle ?: s.qFinish else s.next,
                        isActive = active,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                brush = if (active) BaseTheme.BaseClickColor!!.toBrush()
                                else SolidColor(BaseTheme.FieldColor),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        sliderModifier = Modifier.weight(1f),
                        onNext = {
                            if (!isAnswered(page)) {
                                markError(page)
                                return@BaseClick
                            }
                            if (!isLast) {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                                return@BaseClick
                            }
                            val firstMissing = questions.indices.firstOrNull { !isAnswered(it) }
                            if (firstMissing != null) {
                                markError(firstMissing)
                                scope.launch { pagerState.animateScrollToPage(firstMissing) }
                            } else {
                                submit(questions)
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun Header(model: QuestionnaireModel) {
        val hasLogoHeader = !model.svgLogoUrl.isNullOrEmpty() &&
                !model.header.isNullOrEmpty() &&
                !model.subHeader.isNullOrEmpty()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasLogoHeader) {
                LogoSvgUrl(
                    url = model.svgLogoUrl!!,
                    modifier = Modifier.size(width = 70.dp, height = 70.dp)
                )
                Text(
                    text = model.header!!,
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
                    text = model.subHeader!!,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Medium,
                    color = BaseTheme.BaseTextColor.copy(0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp, start = 20.dp, end = 20.dp)
                )
            } else if (!model.header.isNullOrEmpty()) {
                Text(
                    text = model.header,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    color = BaseTheme.BaseTextColor,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }

    @Composable
    private fun PageIndicator(count: Int, current: Int) {
        val accent = BaseTheme.BaseClickColor!!.toBrush()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(count) { i ->
                val width by animateDpAsState(
                    targetValue = if (i == current) 22.dp else 8.dp,
                    label = "dotWidth"
                )
                val brush = when {
                    i == current -> accent
                    isAnswered(i) -> SolidColor(BaseTheme.BaseTextColor.copy(alpha = 0.6f))
                    else -> SolidColor(BaseTheme.BaseTextColor.copy(alpha = 0.2f))
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }

    @Composable
    private fun QuestionPage(index: Int, question: QuestionModel, total: Int) {
        val s = flowStrings()
        val picked = selections[index].orEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = s.qQuestionOf(index + 1, total),
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                color = BaseTheme.BaseTextColor.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = question.title,
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                color = BaseTheme.BaseTextColor,
                fontSize = 20.sp
            )

            if (!question.subTitle.isNullOrEmpty()) {
                Text(
                    text = question.subTitle,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Normal,
                    color = BaseTheme.BaseTextColor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!question.image.isNullOrEmpty()) {
                Spacer(Modifier.height(14.dp))
                QuestionImage(question.image)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (question.allowMultipleAnswers) s.qSelectMultiple else s.qSelectOne,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                color = BaseTheme.BaseTextColor.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            question.answers.forEach { answer ->
                AnswerOption(
                    label = answer.value,
                    selected = picked.any { it.key == answer.key },
                    multiple = question.allowMultipleAnswers,
                    onClick = { toggleAnswer(index, question, answer) }
                )
                Spacer(Modifier.height(10.dp))
            }

            if (index in errorPages && picked.isEmpty()) {
                Text(
                    text = s.qAnswerRequired,
                    fontFamily = InterFont,
                    color = Color(0xFFE53935),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    @Composable
    private fun QuestionImage(url: String) {
        if (url.endsWith(".svg", ignoreCase = true)) {
            LogoSvgUrl(
                url = url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }

    @Composable
    private fun AnswerOption(
        label: String,
        selected: Boolean,
        multiple: Boolean,
        onClick: () -> Unit,
    ) {
        val shape = RoundedCornerShape(14.dp)
        val accent = BaseTheme.BaseClickColor!!.toBrush()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(BaseTheme.FieldColor)
                .then(
                    if (selected) Modifier.border(2.dp, accent, shape)
                    else Modifier.border(1.dp, BaseTheme.BaseTextColor.copy(alpha = 0.15f), shape)
                )
                .selectable(
                    selected = selected,
                    role = if (multiple) Role.Checkbox else Role.RadioButton,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionIndicator(selected = selected, multiple = multiple, accent = accent)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontFamily = InterFont,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = BaseTheme.BaseTextColor,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun SelectionIndicator(selected: Boolean, multiple: Boolean, accent: Brush) {
        val shape = if (multiple) RoundedCornerShape(6.dp) else CircleShape

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(shape)
                .then(
                    if (selected) Modifier.background(accent)
                    else Modifier.border(2.dp, BaseTheme.BaseTextColor.copy(alpha = 0.4f), shape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                if (multiple) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }

    @Composable
    private fun CenteredMessage(text: String, modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                color = BaseTheme.BaseTextColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}