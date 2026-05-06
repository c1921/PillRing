package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.notification.ReminderLogStore
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.UiTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityReminderLogNavigationTest {
    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        clearStores()
    }

    @Test
    fun homeLogButton_opensEmptyLogScreenAndBackReturnsHome() {
        composeRule.onNodeWithTag(UiTestTags.HOME_LOG_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.REMINDER_LOG_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.REMINDER_LOG_EMPTY).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag(UiTestTags.HOME_LOG_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.REMINDER_LOG_SCREEN).assertCountEquals(0)
    }

    @Test
    fun logScreen_displaysReminderAndConfirmationRecords() {
        seedLogEntries()

        composeRule.onNodeWithTag(UiTestTags.HOME_LOG_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.REMINDER_LOG_SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.REMINDER_LOG_ENTRY).assertCountEquals(2)
        composeRule.onNodeWithText(string(R.string.reminder_log_event_triggered))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reminder_log_event_confirmed))
            .assertIsDisplayed()
    }

    private fun seedLogEntries() {
        composeRule.runOnUiThread {
            val plan = ReminderSessionStore.addPlan(
                context = composeRule.activity,
                name = "Plan A",
                hour = 8,
                minute = 0,
                enabled = false
            )
            ReminderLogStore.recordReminderTriggered(
                context = composeRule.activity,
                plan = plan,
                occurredAtEpochMs = 1_000L
            )
            ReminderLogStore.recordManualConfirmation(
                context = composeRule.activity,
                plan = plan,
                occurredAtEpochMs = 2_000L
            )
        }
    }

    private fun clearStores() {
        composeRule.activity.getSharedPreferences(
            "reminder_session_store",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        composeRule.activity.getSharedPreferences(
            "reminder_log_store",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        composeRule.activityRule.scenario.recreate()
    }

    private fun string(@StringRes resId: Int): String {
        return composeRule.activity.getString(resId)
    }
}

