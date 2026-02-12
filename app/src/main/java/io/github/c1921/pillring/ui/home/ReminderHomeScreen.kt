package io.github.c1921.pillring.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderPlan
import io.github.c1921.pillring.notification.ReminderRepeatMode
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.theme.PillRingTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReminderHomeScreen(
    plans: List<ReminderPlan>,
    maxPlans: Int,
    onAddPlanClick: () -> Unit,
    onEditPlanClick: (ReminderPlan) -> Unit,
    onDeletePlanClick: (ReminderPlan) -> Unit,
    onMoveUpClick: (ReminderPlan) -> Unit,
    onMoveDownClick: (ReminderPlan) -> Unit,
    onPlanEnabledChange: (ReminderPlan, Boolean) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenReminderConfirmFromPlanCard: (ReminderPlan) -> Unit
) {
    val canAddPlan = plans.size < maxPlans

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.test_page_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettingsClick,
                        modifier = Modifier.testTag(UiTestTags.HOME_SETTINGS_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (canAddPlan) {
                        onAddPlanClick()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(text = stringResource(R.string.btn_add_plan))
                },
                containerColor = if (canAddPlan) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (canAddPlan) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag(UiTestTags.HOME_ADD_PLAN_FAB)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plans.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.empty_plan_list),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.btn_add_plan),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = plans,
                    key = { _, plan -> plan.id }
                ) { index, plan ->
                    PlanCard(
                        plan = plan,
                        index = index,
                        totalCount = plans.size,
                        onCardClick = if (plan.isReminderActive) {
                            {
                                onOpenReminderConfirmFromPlanCard(plan)
                            }
                        } else {
                            null
                        },
                        onEditClick = { onEditPlanClick(plan) },
                        onDeleteClick = { onDeletePlanClick(plan) },
                        onMoveUpClick = { onMoveUpClick(plan) },
                        onMoveDownClick = { onMoveDownClick(plan) },
                        onPlanEnabledChange = { enabled -> onPlanEnabledChange(plan, enabled) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderHomeScreenPreview() {
    PillRingTheme {
        ReminderHomeScreen(
            plans = listOf(
                ReminderPlan(
                    id = "1",
                    name = "Morning pills",
                    hour = 8,
                    minute = 30,
                    enabled = true,
                    notificationId = 1001,
                    isReminderActive = false,
                    suppressNextDeleteFallback = false
                ),
                ReminderPlan(
                    id = "2",
                    name = "After lunch",
                    hour = 13,
                    minute = 0,
                    enabled = false,
                    repeatMode = ReminderRepeatMode.DAILY,
                    intervalDays = 1,
                    startDateEpochDay = null,
                    notificationId = 1002,
                    isReminderActive = true,
                    suppressNextDeleteFallback = false
                )
            ),
            maxPlans = ReminderSessionStore.MAX_PLAN_COUNT,
            onAddPlanClick = {},
            onEditPlanClick = {},
            onDeletePlanClick = {},
            onMoveUpClick = {},
            onMoveDownClick = {},
            onPlanEnabledChange = { _, _ -> },
            onOpenSettingsClick = {},
            onOpenReminderConfirmFromPlanCard = {}
        )
    }
}
