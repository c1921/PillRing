package io.github.c1921.pillring.ui.reminder

import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderPlan
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.common.formatReminderTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REMINDER_CONFIRM_HOLD_DURATION_MS = 1200
private const val REMINDER_CONFIRM_RELEASE_ANIMATION_MS = 180
private const val REMINDER_CONFIRM_COMPLETION_SETTLE_MS = 500L
private const val REMINDER_CONFIRM_EXIT_ANIMATION_MS = 260
private const val REMINDER_CONFIRM_EXIT_FADE_MS = 180
private const val REMINDER_CONFIRM_HAPTIC_THRESHOLD_25 = 0.25f
private const val REMINDER_CONFIRM_HAPTIC_THRESHOLD_50 = 0.50f
private const val REMINDER_CONFIRM_HAPTIC_THRESHOLD_75 = 0.75f

private enum class HoldToConfirmState {
    IDLE,
    HOLDING,
    COMPLETED,
    EXITING
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReminderConfirmScreen(
    plan: ReminderPlan,
    onBackClick: () -> Unit,
    onConfirmCommitted: () -> Unit,
    onAutoExit: () -> Unit
) {
    val context = LocalContext.current
    val accessibilityManager = LocalAccessibilityManager.current
    val reducedMotion = remember {
        !ValueAnimator.areAnimatorsEnabled()
    }
    val currentOnConfirmCommitted by rememberUpdatedState(onConfirmCommitted)
    val currentOnAutoExit by rememberUpdatedState(onAutoExit)
    val reminderTimeText = remember(plan.hour, plan.minute, context) {
        formatReminderTime(
            context = context,
            hour = plan.hour,
            minute = plan.minute
        )
    }
    var holdState by remember {
        mutableStateOf(HoldToConfirmState.IDLE)
    }
    var hasCommitted by remember {
        mutableStateOf(false)
    }
    var isAutoExiting by remember {
        mutableStateOf(false)
    }
    val hintState = if (isAutoExiting) {
        HoldToConfirmState.EXITING
    } else {
        holdState
    }
    val exitAnimationMs = if (reducedMotion) {
        REMINDER_CONFIRM_EXIT_FADE_MS
    } else {
        REMINDER_CONFIRM_EXIT_ANIMATION_MS
    }

    LaunchedEffect(hasCommitted) {
        if (!hasCommitted) {
            return@LaunchedEffect
        }
        val recommendedSettleMs = accessibilityManager
            ?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = REMINDER_CONFIRM_COMPLETION_SETTLE_MS,
                containsText = true,
                containsControls = true
            )
            ?: REMINDER_CONFIRM_COMPLETION_SETTLE_MS
        val settleMs = maxOf(REMINDER_CONFIRM_COMPLETION_SETTLE_MS, recommendedSettleMs)
        delay(settleMs)
        isAutoExiting = true
        delay(exitAnimationMs.toLong())
        currentOnAutoExit()
    }

    AnimatedVisibility(
        visible = !isAutoExiting,
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight / 5 },
            animationSpec = tween(
                durationMillis = exitAnimationMs,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = REMINDER_CONFIRM_EXIT_FADE_MS)
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag(UiTestTags.REMINDER_CONFIRM_SCREEN),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.reminder_confirm_page_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.REMINDER_CONFIRM_SECONDARY_CONTENT),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_confirm_secondary_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.reminder_confirm_plan_label, plan.name),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.reminder_confirm_time_label, reminderTimeText),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.reminder_confirm_long_press_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    HoldToConfirmButton(
                        text = stringResource(R.string.btn_reminder_hold_to_confirm),
                        isAutoExiting = isAutoExiting,
                        onConfirm = {
                            if (hasCommitted) {
                                return@HoldToConfirmButton
                            }
                            hasCommitted = true
                            currentOnConfirmCommitted()
                        },
                        onStateChanged = { state ->
                            holdState = state
                        }
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    AnimatedContent(
                        targetState = hintState,
                        label = "reminder_confirm_hold_hint"
                    ) { state ->
                        val textResId = when (state) {
                            HoldToConfirmState.IDLE -> R.string.reminder_confirm_hold_hint_idle
                            HoldToConfirmState.HOLDING -> R.string.reminder_confirm_hold_hint_holding
                            HoldToConfirmState.COMPLETED,
                            HoldToConfirmState.EXITING -> R.string.reminder_confirm_hold_hint_completed
                        }
                        Text(
                            text = stringResource(textResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(UiTestTags.REMINDER_CONFIRM_HOLD_HINT)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldToConfirmButton(
    text: String,
    isAutoExiting: Boolean,
    onConfirm: () -> Unit,
    onStateChanged: (HoldToConfirmState) -> Unit
) {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val reducedMotion = remember {
        !ValueAnimator.areAnimatorsEnabled()
    }

    val progress = remember {
        Animatable(0f)
    }
    var holdState by remember {
        mutableStateOf(HoldToConfirmState.IDLE)
    }
    var isHolding by remember {
        mutableStateOf(false)
    }
    var hasConfirmed by remember {
        mutableStateOf(false)
    }
    var progressJob by remember {
        mutableStateOf<Job?>(null)
    }
    val visualState = when {
        isAutoExiting && hasConfirmed -> HoldToConfirmState.EXITING
        else -> holdState
    }

    LaunchedEffect(visualState) {
        onStateChanged(visualState)
    }

    DisposableEffect(Unit) {
        onDispose {
            progressJob?.cancel()
        }
    }

    fun performSemanticHaptic(
        semanticConstant: Int,
        fallbackType: HapticFeedbackType
    ) {
        val usedSemanticHaptic = view.performHapticFeedback(semanticConstant)
        if (!usedSemanticHaptic) {
            hapticFeedback.performHapticFeedback(fallbackType)
        }
    }

    fun startHold() {
        if (hasConfirmed || isAutoExiting) {
            return
        }
        progressJob?.cancel()
        isHolding = true
        hasConfirmed = false
        holdState = HoldToConfirmState.HOLDING
        performSemanticHaptic(
            semanticConstant = HapticFeedbackConstants.GESTURE_START,
            fallbackType = HapticFeedbackType.LongPress
        )

        progressJob = coroutineScope.launch {
            progress.snapTo(0f)
            val thresholds = if (reducedMotion) {
                emptyList()
            } else {
                listOf(
                    REMINDER_CONFIRM_HAPTIC_THRESHOLD_25,
                    REMINDER_CONFIRM_HAPTIC_THRESHOLD_50,
                    REMINDER_CONFIRM_HAPTIC_THRESHOLD_75
                )
            }
            var thresholdIndex = 0
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REMINDER_CONFIRM_HOLD_DURATION_MS,
                    easing = LinearEasing
                )
            ) {
                if (thresholdIndex < thresholds.size && value >= thresholds[thresholdIndex]) {
                    performSemanticHaptic(
                        semanticConstant = HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,
                        fallbackType = HapticFeedbackType.TextHandleMove
                    )
                    thresholdIndex += 1
                }
            }
            if (isHolding && !hasConfirmed) {
                hasConfirmed = true
                holdState = HoldToConfirmState.COMPLETED
                performSemanticHaptic(
                    semanticConstant = HapticFeedbackConstants.CONFIRM,
                    fallbackType = HapticFeedbackType.LongPress
                )
                currentOnConfirm()
            }
        }
    }

    fun stopHold() {
        if (!isHolding) {
            return
        }
        isHolding = false
        if (!hasConfirmed) {
            if (holdState == HoldToConfirmState.HOLDING) {
                performSemanticHaptic(
                    semanticConstant = HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE,
                    fallbackType = HapticFeedbackType.TextHandleMove
                )
            }
            progressJob?.cancel()
            progressJob = null
            coroutineScope.launch {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = REMINDER_CONFIRM_RELEASE_ANIMATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
                holdState = HoldToConfirmState.IDLE
            }
        }
    }

    val stateTransition = updateTransition(
        targetState = visualState,
        label = "reminder_confirm_button_transition"
    )
    val buttonScale by stateTransition.animateFloat(
        transitionSpec = {
            if (reducedMotion) {
                tween(durationMillis = 120, easing = LinearOutSlowInEasing)
            } else {
                androidx.compose.animation.core.spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            }
        },
        label = "reminder_confirm_button_scale"
    ) { state ->
        when (state) {
            HoldToConfirmState.IDLE -> 1f
            HoldToConfirmState.HOLDING -> 0.94f
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> 1.02f
        }
    }
    val buttonContainerColor by stateTransition.animateColor(
        transitionSpec = { tween(durationMillis = 180) },
        label = "reminder_confirm_button_container_color"
    ) { state ->
        when (state) {
            HoldToConfirmState.IDLE -> MaterialTheme.colorScheme.primary
            HoldToConfirmState.HOLDING -> MaterialTheme.colorScheme.primaryContainer
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> MaterialTheme.colorScheme.secondaryContainer
        }
    }
    val buttonContentColor by stateTransition.animateColor(
        transitionSpec = { tween(durationMillis = 180) },
        label = "reminder_confirm_button_content_color"
    ) { state ->
        when (state) {
            HoldToConfirmState.IDLE -> MaterialTheme.colorScheme.onPrimary
            HoldToConfirmState.HOLDING -> MaterialTheme.colorScheme.onPrimaryContainer
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    }
    val progressColor by stateTransition.animateColor(
        transitionSpec = { tween(durationMillis = 180) },
        label = "reminder_confirm_progress_color"
    ) { state ->
        when (state) {
            HoldToConfirmState.IDLE,
            HoldToConfirmState.HOLDING -> MaterialTheme.colorScheme.primary
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> MaterialTheme.colorScheme.secondary
        }
    }
    val progressTrackColor by stateTransition.animateColor(
        transitionSpec = { tween(durationMillis = 180) },
        label = "reminder_confirm_progress_track_color"
    ) { state ->
        when (state) {
            HoldToConfirmState.HOLDING -> MaterialTheme.colorScheme.surfaceContainerHigh
            HoldToConfirmState.IDLE,
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
    }
    val buttonShadowElevation by stateTransition.animateDp(
        transitionSpec = { tween(durationMillis = 180) },
        label = "reminder_confirm_button_shadow_elevation"
    ) { state ->
        when (state) {
            HoldToConfirmState.IDLE -> 8.dp
            HoldToConfirmState.HOLDING -> 2.dp
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> 4.dp
        }
    }
    val buttonTonalElevation by stateTransition.animateDp(
        transitionSpec = { tween(durationMillis = 180) },
        label = "reminder_confirm_button_tonal_elevation"
    ) { state ->
        when (state) {
            HoldToConfirmState.IDLE -> 8.dp
            HoldToConfirmState.HOLDING -> 2.dp
            HoldToConfirmState.COMPLETED,
            HoldToConfirmState.EXITING -> 4.dp
        }
    }

    val haloTransition = rememberInfiniteTransition(label = "reminder_confirm_halo")
    val haloScale by haloTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reminder_confirm_halo_scale"
    )
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reminder_confirm_halo_alpha"
    )
    val showHalo = visualState == HoldToConfirmState.HOLDING && !reducedMotion
    val canReceivePress = !hasConfirmed && !isAutoExiting
    val pressModifier = if (canReceivePress) {
        Modifier.pointerInput(isAutoExiting) {
            detectTapGestures(
                onPress = {
                    startHold()
                    tryAwaitRelease()
                    stopHold()
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .scale(if (showHalo) haloScale else 1f)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (showHalo) haloAlpha else 0f
                    ),
                    shape = CircleShape
                )
        )
        CircularProgressIndicator(
            progress = {
                progress.value
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag(UiTestTags.REMINDER_CONFIRM_HOLD_PROGRESS),
            color = progressColor,
            trackColor = progressTrackColor,
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round
        )
        Surface(
            modifier = Modifier
                .size(112.dp)
                .scale(buttonScale)
                .testTag(UiTestTags.REMINDER_CONFIRM_PRIMARY_BUTTON)
                .then(pressModifier),
            shape = CircleShape,
            color = buttonContainerColor,
            shadowElevation = buttonShadowElevation,
            tonalElevation = buttonTonalElevation
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = visualState,
                    label = "reminder_confirm_button_content"
                ) { state ->
                    if (state == HoldToConfirmState.COMPLETED || state == HoldToConfirmState.EXITING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = buttonContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge,
                                color = buttonContentColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge,
                            color = buttonContentColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
