package io.github.c1921.pillring.notification

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyCompatibilityRemovalTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        clearStore()
    }

    @After
    fun tearDown() {
        clearStore()
    }

    @Test
    fun getPlans_legacyOnlyData_isIgnoredAndNewSchemaKeysAreInitialized() {
        prefs.edit()
            .putBoolean(KEY_PLAN_ENABLED_LEGACY, true)
            .putInt(KEY_SELECTED_HOUR_LEGACY, 9)
            .putInt(KEY_SELECTED_MINUTE_LEGACY, 30)
            .putBoolean(KEY_REMINDER_ACTIVE_LEGACY, true)
            .putBoolean(KEY_SUPPRESS_DELETE_FALLBACK_LEGACY, true)
            .commit()

        val plans = ReminderSessionStore.getPlans(context)

        assertTrue(plans.isEmpty())
        assertTrue(prefs.contains(KEY_PLANS_JSON))
        assertTrue(prefs.contains(KEY_NEXT_NOTIFICATION_ID))
    }

    @Test
    fun onReceive_dailyAlarmWithoutPlanId_isNoOp() {
        val plan = ReminderSessionStore.addPlan(
            context = context,
            name = "Morning",
            hour = 8,
            minute = 0,
            enabled = true
        )
        val receiver = ReminderAlarmReceiver()
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderContract.ACTION_SHOW_REMINDER
            putExtra(ReminderContract.EXTRA_ALARM_KIND, ReminderContract.ALARM_KIND_DAILY)
            putExtra(ReminderContract.EXTRA_REASON, "test")
        }

        receiver.onReceive(context, intent)

        val latest = ReminderSessionStore.getPlan(context, plan.id)
        assertFalse(latest?.isReminderActive ?: true)
    }

    @Test
    fun addPlan_thenImmediateRead_planExists() {
        val plan = ReminderSessionStore.addPlan(
            context = context,
            name = "Immediate Read",
            hour = 10,
            minute = 30,
            enabled = true
        )

        val latest = ReminderSessionStore.getPlan(context, plan.id)

        assertNotNull(latest)
        assertTrue(latest?.id == plan.id)
    }

    private fun clearStore() {
        prefs.edit().clear().commit()
    }

    companion object {
        private const val PREFS_NAME = "reminder_session_store"
        private const val KEY_PLANS_JSON = "plans_json"
        private const val KEY_NEXT_NOTIFICATION_ID = "next_notification_id"

        private const val KEY_REMINDER_ACTIVE_LEGACY = "is_reminder_active"
        private const val KEY_SUPPRESS_DELETE_FALLBACK_LEGACY = "suppress_next_delete_fallback"
        private const val KEY_PLAN_ENABLED_LEGACY = "is_plan_enabled"
        private const val KEY_SELECTED_HOUR_LEGACY = "selected_hour"
        private const val KEY_SELECTED_MINUTE_LEGACY = "selected_minute"
    }
}
