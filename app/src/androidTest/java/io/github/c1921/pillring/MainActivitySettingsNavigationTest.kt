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
    fun openSettingsButton_navigatesToSettingsOverview() {
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertCountEquals(0)
    }

    @Test
    fun settingsBackIcon_onOverview_navigatesToHomeScreen() {
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertCountEquals(0)
    }

    @Test
    fun systemBack_onOverview_navigatesToHomeScreen() {
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        pressBack()

        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertCountEquals(0)
    }

    @Test
    fun tapLanguageEntry_navigatesToLanguageSubpage() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM).assertIsDisplayed()
    }

    @Test
    fun tapPermissionEntry_navigatesToPermissionSubpage() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_PAGE).assertIsDisplayed()
    }

    @Test
    fun tapAboutEntry_navigatesToAboutSubpage() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_PAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_VERSION_VALUE).assertIsDisplayed()
    }

    @Test
    fun backFromLanguageSubpage_returnsToSettingsOverview() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
    }

    @Test
    fun systemBackFromPermissionSubpage_returnsToSettingsOverview() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).performClick()
        pressBack()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.SETTINGS_PERMISSION_PAGE).assertCountEquals(0)
    }

    @Test
    fun backFromAboutSubpage_returnsToSettingsOverview() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
    }

    @Test
    fun systemBackFromAboutSubpage_returnsToSettingsOverview() {
        openSettingsOverview()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).performClick()
        pressBack()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.SETTINGS_ABOUT_PAGE).assertCountEquals(0)
    }

    private fun openSettingsOverview() {
        composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ITEM).assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.SETTINGS_PERMISSION_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
