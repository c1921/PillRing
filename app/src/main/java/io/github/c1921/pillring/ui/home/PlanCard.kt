package io.github.c1921.pillring.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderPlan
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.common.formatReminderTime
import io.github.c1921.pillring.ui.common.formatRepeatSummary

@Composable
internal fun PlanCard(
    plan: ReminderPlan,
    index: Int,
    totalCount: Int,
    onCardClick: (() -> Unit)?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    onPlanEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val timeText = remember(plan.hour, plan.minute, context) {
        formatReminderTime(
            context = context,
            hour = plan.hour,
            minute = plan.minute
        )
    }
    val repeatSummaryText = remember(
        plan.repeatMode,
        plan.intervalDays,
        plan.startDateEpochDay,
        context
    ) {
        formatRepeatSummary(
            context = context,
            repeatMode = plan.repeatMode,
            intervalDays = plan.intervalDays,
            startDateEpochDay = plan.startDateEpochDay
        )
    }
    var showMenu by rememberSaveable(plan.id) {
        mutableStateOf(false)
    }
    val planStatusContainerColor = when {
        plan.isReminderActive -> MaterialTheme.colorScheme.errorContainer
        plan.enabled -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val planStatusContentColor = when {
        plan.isReminderActive -> MaterialTheme.colorScheme.onErrorContainer
        plan.enabled -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val planStatusResId = when {
        plan.isReminderActive -> R.string.status_reminder_active
        plan.enabled -> R.string.status_plan_enabled
        else -> R.string.status_plan_disabled
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .testTag(UiTestTags.planCard(plan.id))
        .then(
            if (onCardClick != null) {
                Modifier.clickable(onClick = onCardClick)
            } else {
                Modifier
            }
        )

    ElevatedCard(
        modifier = cardModifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.label_selected_time, timeText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = repeatSummaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = planStatusContainerColor
                    ) {
                        Text(
                            text = stringResource(planStatusResId),
                            style = MaterialTheme.typography.labelLarge,
                            color = planStatusContentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = plan.enabled,
                        onCheckedChange = onPlanEnabledChange
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag(UiTestTags.PLAN_CARD_OVERFLOW_BUTTON)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_plan_actions)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.btn_move_up))
                                },
                                enabled = index > 0,
                                onClick = {
                                    showMenu = false
                                    onMoveUpClick()
                                },
                                modifier = Modifier.testTag(UiTestTags.PLAN_CARD_ACTION_MOVE_UP)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.btn_move_down))
                                },
                                enabled = index < totalCount - 1,
                                onClick = {
                                    showMenu = false
                                    onMoveDownClick()
                                },
                                modifier = Modifier.testTag(UiTestTags.PLAN_CARD_ACTION_MOVE_DOWN)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.btn_delete_plan),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                modifier = Modifier.testTag(UiTestTags.PLAN_CARD_ACTION_DELETE)
                            )
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.btn_edit_plan),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
