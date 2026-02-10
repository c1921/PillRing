package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.notification.ReminderContract
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.UiTestTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityReminderConfirmNavigationTest {
    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        clearPlanStore()
    }

    @Test
    fun activeReminder_planCardDoesNotShowConfirmButton() {
        seedPlans(PlanSeed(name = "Plan A", hour = 8, minute = 0, active = true))

        composeRule.onAllNodesWithText(string(R.string.btn_confirm_stop_plan_reminder))
            .assertCountEquals(0)
    }

    @Test
    fun notificationIntent_opensReminderConfirmScreen() {
        val (planAId) = seedPlans(PlanSeed(name = "Plan A", hour = 8, minute = 0, active = true))

        openReminderConfirmFromNotification(planAId)

        composeRule.onNodeWithTag(UiTestTags.REMINDER_CONFIRM_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.REMINDER_CONFIRM_SECONDARY_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.REMINDER_CONFIRM_PRIMARY_BUTTON).assertIsDisplayed()
    }

    @Test
    fun confirmButton_stopsOnlyTargetPlanReminder() {
        val (planAId, planBId) = seedPlans(
            PlanSeed(name = "Plan A", hour = 8, minute = 0, active = true),
            PlanSeed(name = "Plan B", hour = 9, minute = 0, active = true)
        )

        openReminderConfirmFromNotification(planAId)
        composeRule.onNodeWithTag(UiTestTags.REMINDER_CONFIRM_PRIMARY_BUTTON).performClick()

        var planAActive = true
        var planBActive = false
        composeRule.runOnUiThread {
            planAActive = ReminderSessionStore.getPlan(composeRule.activity, planAId)
                ?.isReminderActive
                ?: true
            planBActive = ReminderSessionStore.getPlan(composeRule.activity, planBId)
                ?.isReminderActive
                ?: false
        }

        assertFalse(planAActive)
        assertTrue(planBActive)
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.REMINDER_CONFIRM_SCREEN).assertCountEquals(0)
    }

    @Test
    fun invalidNotificationIntent_returnsToHomeScreen() {
        seedPlans(PlanSeed(name = "Plan A", hour = 8, minute = 0, active = false))

        openReminderConfirmFromNotification("missing-plan-id")

        composeRule.onAllNodesWithTag(UiTestTags.REMINDER_CONFIRM_SCREEN).assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun secondNotificationClickInForeground_switchesConfirmTarget() {
        val (planAId, planBId) = seedPlans(
            PlanSeed(name = "Plan A", hour = 8, minute = 0, active = true),
            PlanSeed(name = "Plan B", hour = 9, minute = 0, active = true)
        )

        openReminderConfirmFromNotification(planAId)
        composeRule.onNodeWithText(
            string(R.string.reminder_confirm_plan_label, "Plan A")
        ).assertIsDisplayed()

        openReminderConfirmFromNotification(planBId)
        composeRule.onNodeWithText(
            string(R.string.reminder_confirm_plan_label, "Plan B")
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            string(R.string.reminder_confirm_plan_label, "Plan A")
        ).assertCountEquals(0)
    }

    private fun openReminderConfirmFromNotification(planId: String) {
        composeRule.runOnUiThread {
            composeRule.activity.startActivity(
                Intent(composeRule.activity, MainActivity::class.java).apply {
                    action = ReminderContract.ACTION_OPEN_REMINDER_CONFIRM
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ReminderContract.EXTRA_PLAN_ID, planId)
                }
            )
        }
        composeRule.waitForIdle()
    }

    private fun seedPlans(vararg seeds: PlanSeed): List<String> {
        clearPlanStore()
        val planIds = mutableListOf<String>()
        composeRule.runOnUiThread {
            seeds.forEach { seed ->
                val plan = ReminderSessionStore.addPlan(
                    context = composeRule.activity,
                    name = seed.name,
                    hour = seed.hour,
                    minute = seed.minute,
                    enabled = true
                )
                if (seed.active) {
                    ReminderSessionStore.markReminderTriggered(composeRule.activity, plan.id)
                }
                planIds += plan.id
            }
        }
        composeRule.activityRule.scenario.recreate()
        return planIds
    }

    private fun clearPlanStore() {
        composeRule.activity.getSharedPreferences(
            "reminder_session_store",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        composeRule.activityRule.scenario.recreate()
    }

    private fun string(
        @StringRes resId: Int,
        vararg formatArgs: Any
    ): String {
        return composeRule.activity.getString(resId, *formatArgs)
    }

    private data class PlanSeed(
        val name: String,
        val hour: Int,
        val minute: Int,
        val active: Boolean
    )
}
