package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.UiTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivityPlanTimePickerTest {
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
    fun selectTimeButton_opensComposeTimePickerDialog() {
        openAddPlanDialog()
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER).assertIsDisplayed()
    }

    @Test
    fun cancelFromTimePicker_keepsSelectedTimeUnchanged() {
        openAddPlanDialog()
        val before = selectedTimeLabel()

        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER_CANCEL).performClick()

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_TIME_PICKER).assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECTED_TIME).assertTextEquals(before)
    }

    @Test
    fun confirmFromTimePicker_updatesSelectedTimeText() {
        seedSinglePlanForEdit(hour = 9, minute = 0)
        composeRule.onNodeWithText(string(R.string.btn_edit_plan)).performClick()

        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER).assertIsDisplayed()
        val twelveLabel = NumberFormat.getIntegerInstance(Locale.getDefault()).format(12)
        composeRule.onAllNodesWithText(twelveLabel).onFirst().performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER_CONFIRM).performClick()

        val targetHour = if (DateFormat.is24HourFormat(composeRule.activity)) 12 else 0
        val expectedTime = formatReminderTime(targetHour, 0)
        val expectedLabel = composeRule.activity.getString(
            R.string.label_selected_time,
            expectedTime
        )
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECTED_TIME).assertTextEquals(expectedLabel)
    }

    private fun openAddPlanDialog() {
        composeRule.onNodeWithText(string(R.string.btn_add_plan)).performClick()
    }

    private fun clearPlanStore() {
        composeRule.activity.getSharedPreferences(
            "reminder_session_store",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        composeRule.activityRule.scenario.recreate()
    }

    private fun seedSinglePlanForEdit(
        hour: Int,
        minute: Int
    ) {
        clearPlanStore()
        composeRule.runOnUiThread {
            ReminderSessionStore.addPlan(
                context = composeRule.activity,
                name = "Plan 1",
                hour = hour,
                minute = minute,
                enabled = true
            )
        }
        composeRule.activityRule.scenario.recreate()
    }

    private fun selectedTimeLabel(): String {
        val semanticsNode = composeRule
            .onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECTED_TIME)
            .fetchSemanticsNode()
        return semanticsNode.config[SemanticsProperties.Text]
            .joinToString(separator = "") { annotatedString -> annotatedString.text }
    }

    private fun formatReminderTime(
        hour: Int,
        minute: Int
    ): String {
        val pattern = if (DateFormat.is24HourFormat(composeRule.activity)) "HH:mm" else "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return LocalTime.of(hour, minute).format(formatter)
    }

    private fun string(@StringRes resId: Int): String {
        return composeRule.activity.getString(resId)
    }
}
