package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.UiTestTags
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityPlanCardOverflowMenuTest {
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
    fun overflowMenu_opensAndShowsActions() {
        seedPlans("Plan A", "Plan B")

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_CARD_OVERFLOW_BUTTON).onFirst().performClick()

        composeRule.onNodeWithTag(UiTestTags.PLAN_CARD_ACTION_MOVE_UP).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.PLAN_CARD_ACTION_MOVE_DOWN).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.PLAN_CARD_ACTION_DELETE).assertIsDisplayed()
    }

    @Test
    fun moveDownAction_updatesPlanOrder() {
        seedPlans("Plan A", "Plan B")

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_CARD_OVERFLOW_BUTTON).onFirst().performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_CARD_ACTION_MOVE_DOWN).performClick()

        var names: List<String> = emptyList()
        composeRule.runOnUiThread {
            names = ReminderSessionStore.getPlans(composeRule.activity).map { it.name }
        }

        assertEquals(listOf("Plan B", "Plan A"), names)
    }

    @Test
    fun deleteAction_removesPlanCard() {
        seedPlans("Plan A")

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_CARD_OVERFLOW_BUTTON).onFirst().performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_CARD_ACTION_DELETE).performClick()

        var size = -1
        composeRule.runOnUiThread {
            size = ReminderSessionStore.getPlans(composeRule.activity).size
        }

        assertEquals(0, size)
        composeRule.onAllNodesWithTag(UiTestTags.PLAN_CARD_OVERFLOW_BUTTON).assertCountEquals(0)
    }

    private fun clearPlanStore() {
        composeRule.activity.getSharedPreferences(
            "reminder_session_store",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        composeRule.activityRule.scenario.recreate()
    }

    private fun seedPlans(vararg names: String) {
        clearPlanStore()
        composeRule.runOnUiThread {
            names.forEachIndexed { index, name ->
                ReminderSessionStore.addPlan(
                    context = composeRule.activity,
                    name = name,
                    hour = 8 + index,
                    minute = 0,
                    enabled = true
                )
            }
        }
        composeRule.activityRule.scenario.recreate()
    }
}
