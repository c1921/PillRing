package io.github.c1921.pillring

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.ui.UiTestTags
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
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertCountEquals(0)
    }

    @Test
    fun settingsBackIcon_navigatesToHomeScreen() {
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertCountEquals(0)
    }

    @Test
    fun systemBack_navigatesToHomeScreen() {
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        pressBack()

        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertCountEquals(0)
    }
}
