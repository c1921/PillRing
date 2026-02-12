package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.notification.ReminderRepeatMode
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.UiTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    fun confirmFromTimePicker_closesDialogAndKeepsCurrentSelection() {
        seedSinglePlanForEdit(hour = 9, minute = 0)
        composeRule.onNodeWithText(string(R.string.btn_edit_plan)).performClick()
        val before = selectedTimeLabel()

        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.PLAN_TIME_PICKER_CONFIRM).performClick()

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_TIME_PICKER).assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECTED_TIME).assertTextEquals(before)
    }

    @Test
    fun defaultMode_timeSelection_hasSinglePrimaryControl() {
        openAddPlanDialog()

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.PLAN_EDITOR_SELECTED_TIME).assertCountEquals(1)
    }

    @Test
    fun intervalMode_selectStartDateButton_opensDatePickerDialog() {
        openAddPlanDialog()

        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_REPEAT_MODE_INTERVAL_DAYS).performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_SELECT_START_DATE_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.PLAN_START_DATE_PICKER).assertIsDisplayed()
    }

    @Test
    fun intervalMode_emptyIntervalDays_disablesSaveButton() {
        openAddPlanDialog()

        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_REPEAT_MODE_INTERVAL_DAYS).performClick()
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_INTERVAL_DAYS_INPUT).performTextClearance()

        composeRule.onNodeWithText(string(R.string.dialog_btn_save)).assertIsNotEnabled()
    }

    @Test
    fun intervalMode_startDateSelection_hasSinglePrimaryControl() {
        openAddPlanDialog()
        composeRule.onNodeWithTag(UiTestTags.PLAN_EDITOR_REPEAT_MODE_INTERVAL_DAYS).performClick()

        composeRule.onAllNodesWithTag(UiTestTags.PLAN_EDITOR_SELECT_START_DATE_BUTTON)
            .assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.PLAN_EDITOR_SELECTED_START_DATE)
            .assertCountEquals(1)
    }

    @Test
    fun intervalMode_savedPlan_showsRepeatSummaryOnCard() {
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        seedSingleIntervalPlan(startDateEpochDay = todayEpochDay)
        val expected = string(
            R.string.label_repeat_summary_interval,
            1,
            formatReminderDate(todayEpochDay)
        )

        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    private fun openAddPlanDialog() {
        composeRule.onNodeWithTag(UiTestTags.HOME_ADD_PLAN_FAB).performClick()
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

    private fun seedSingleIntervalPlan(startDateEpochDay: Long) {
        clearPlanStore()
        composeRule.runOnUiThread {
            ReminderSessionStore.addPlan(
                context = composeRule.activity,
                name = "Plan 1",
                hour = 9,
                minute = 0,
                enabled = false,
                repeatMode = ReminderRepeatMode.INTERVAL_DAYS,
                intervalDays = 1,
                startDateEpochDay = startDateEpochDay
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

    private fun string(@StringRes resId: Int): String {
        return composeRule.activity.getString(resId)
    }

    private fun string(
        @StringRes resId: Int,
        vararg formatArgs: Any
    ): String {
        return composeRule.activity.getString(resId, *formatArgs)
    }

    private fun formatReminderDate(epochDay: Long): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        return LocalDate.ofEpochDay(epochDay).format(formatter)
    }
}
