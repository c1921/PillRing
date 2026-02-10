package io.github.c1921.pillring

import android.Manifest
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

class MainActivitySettingsNavigationTest {
    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openSettingsButton_navigatesToSettingsScreen() {
        composeRule.onNodeWithText(string(R.string.btn_open_settings_page)).performClick()

        composeRule.onNodeWithText(string(R.string.permission_health_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.test_page_title)).assertCountEquals(0)
    }

    @Test
    fun backButton_navigatesToHomeScreen() {
        composeRule.onNodeWithText(string(R.string.btn_open_settings_page)).performClick()
        composeRule.onNodeWithText(string(R.string.btn_back_to_test_page)).performClick()

        composeRule.onNodeWithText(string(R.string.test_page_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.permission_health_title)).assertCountEquals(0)
    }

    @Test
    fun systemBack_navigatesToHomeScreen() {
        composeRule.onNodeWithText(string(R.string.btn_open_settings_page)).performClick()
        pressBack()

        composeRule.onNodeWithText(string(R.string.test_page_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.permission_health_title)).assertCountEquals(0)
    }

    private fun string(@StringRes resId: Int): String {
        return composeRule.activity.getString(resId)
    }
}
