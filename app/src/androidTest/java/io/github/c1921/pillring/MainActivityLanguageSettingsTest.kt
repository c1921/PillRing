package io.github.c1921.pillring

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
        openSettings()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).assertIsDisplayed()
    }

    @Test
    fun selectChinese_updatesUiText() {
        openSettings()
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE)

        composeRule.onNodeWithText(string(R.string.settings_page_title)).assertIsDisplayed()
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, selectedLanguage())
    }

    @Test
    fun selectEnglish_updatesUiText() {
        openSettings()
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE)
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_ENGLISH)

        composeRule.onNodeWithText(string(R.string.settings_page_title)).assertIsDisplayed()
        assertEquals(AppLanguage.ENGLISH, selectedLanguage())
    }

    @Test
    fun languageSelection_persistsAfterRecreate() {
        openSettings()
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE)

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, selectedLanguage())
    }

    @Test
    fun followSystem_setsEmptyApplicationLocales() {
        openSettings()
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_ENGLISH)
        selectLanguage(UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM)

        val localeManager = composeRule.activity.getSystemService(LocaleManager::class.java)
        assertTrue(localeManager.applicationLocales.isEmpty)
        assertEquals(AppLanguage.SYSTEM, selectedLanguage())
    }

    private fun openSettings() {
        val alreadyInSettings = composeRule
            .onAllNodesWithTag(UiTestTags.SETTINGS_BACK_BUTTON)
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (!alreadyInSettings) {
            composeRule.onNodeWithTag(UiTestTags.HOME_SETTINGS_BUTTON).performClick()
        }
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_BACK_BUTTON).assertIsDisplayed()
    }

    private fun selectLanguage(optionTag: String) {
        openSettings()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_LANGUAGE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(optionTag).performClick()
        composeRule.waitForIdle()
        openSettings()
    }

    private fun selectedLanguage(): AppLanguage {
        return AppLanguageManager.getSelectedLanguage(composeRule.activity)
    }

    private fun string(resId: Int): String {
        return composeRule.activity.getString(resId)
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
