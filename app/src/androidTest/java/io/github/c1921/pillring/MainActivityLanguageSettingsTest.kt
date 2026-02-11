package io.github.c1921.pillring

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import io.github.c1921.pillring.locale.AppLanguage
import io.github.c1921.pillring.locale.AppLanguageManager
import io.github.c1921.pillring.ui.UiTestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityLanguageSettingsTest {
    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        clearPlanStore()
        resetLanguageToSystem()
        composeRule.activityRule.scenario.recreate()
    }

    @After
    fun tearDown() {
        resetLanguageToSystem()
    }

    @Test
    fun settings_containsLanguageItem() {
        openSettingsOverview()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
    }

    @Test
    fun selectChinese_updatesUiText() {
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE)

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM).assertIsDisplayed()
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, selectedLanguage())
    }

    @Test
    fun selectEnglish_updatesUiText() {
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE)
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_ENGLISH)

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM).assertIsDisplayed()
        assertEquals(AppLanguage.ENGLISH, selectedLanguage())
    }

    @Test
    fun languageSelection_persistsAfterRecreate() {
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE)

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        openLanguageSettings()
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, selectedLanguage())
    }

    @Test
    fun followSystem_setsEmptyApplicationLocales() {
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_ENGLISH)
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM)

        val localeManager = composeRule.activity.getSystemService(LocaleManager::class.java)
        assertTrue(localeManager.applicationLocales.isEmpty)
        assertEquals(AppLanguage.SYSTEM, selectedLanguage())
    }

    private fun openSettingsOverview() {
        val alreadyInOverview = composeRule
            .onAllNodesWithTag(UiTestTags.SETTINGS_BACK_BUTTON)
            .fetchSemanticsNodes()
            .isNotEmpty()
            && composeRule
                .onAllNodesWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM)
                .fetchSemanticsNodes()
                .isNotEmpty()
        if (!alreadyInOverview) {
            composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        }
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ABOUT_ITEM).assertIsDisplayed()
    }

    private fun openLanguageSettings() {
        val alreadyInLanguageSettings = composeRule
            .onAllNodesWithTag(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM)
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (!alreadyInLanguageSettings) {
            openSettingsOverview()
            composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).performClick()
        }
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM).assertIsDisplayed()
    }

    private fun selectLanguage(optionTag: String) {
        openLanguageSettings()
        composeRule.onNodeWithTag(optionTag).performClick()
        composeRule.waitForIdle()
        openLanguageSettings()
    }

    private fun selectedLanguage(): AppLanguage {
        return AppLanguageManager.getSelectedLanguage(composeRule.activity)
    }

    private fun clearPlanStore() {
        composeRule.activity.getSharedPreferences(
            "reminder_session_store",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
    }

    private fun resetLanguageToSystem() {
        composeRule.runOnUiThread {
            val localeManager = composeRule.activity.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
        }
        composeRule.waitForIdle()
    }
}
