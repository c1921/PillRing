package io.github.c1921.pillring.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ReminderPlanCoordinatorTest {
    @Test
    fun addPlan_success_persistsPlanAndSchedules() {
        val store = FakePlanStore()
        val alarms = FakeAlarmService(scheduleResult = true)
        val coordinator = newCoordinator(
            store = store,
            alarms = alarms,
            notifications = FakeNotificationService()
        )

        val result = coordinator.addPlan(
            name = "Morning",
            hour = 8,
            minute = 30,
            repeatMode = ReminderRepeatMode.DAILY,
            intervalDays = 1,
            startDateEpochDay = null,
            scheduleReason = "test"
        )

        assertTrue(result is PlanMutationResult.Success)
        assertEquals(1, store.getPlans().size)
        assertEquals(1, alarms.scheduleCalls)
    }

    @Test
    fun addPlan_scheduleFailure_rollsBackAddedPlan() {
        val store = FakePlanStore()
        val alarms = FakeAlarmService(scheduleResult = false)
        val coordinator = newCoordinator(
            store = store,
            alarms = alarms,
            notifications = FakeNotificationService()
        )

        val result = coordinator.addPlan(
            name = "Morning",
            hour = 8,
            minute = 30,
            repeatMode = ReminderRepeatMode.DAILY,
            intervalDays = 1,
            startDateEpochDay = null,
            scheduleReason = "test"
        )

        assertEquals(
            PlanMutationResult.Failure(PlanMutationFailureReason.SCHEDULE_FAILED),
            result
        )
        assertTrue(store.getPlans().isEmpty())
        assertEquals(1, alarms.scheduleCalls)
    }

    @Test
    fun editPlan_enabledPlan_scheduleFailure_doesNotPersistChanges() {
        val existingPlan = basePlan(
            id = "p1",
            name = "Morning",
            hour = 8,
            minute = 0,
            enabled = true
        )
        val store = FakePlanStore(initialPlans = listOf(existingPlan))
        val alarms = FakeAlarmService(scheduleResult = false)
        val coordinator = newCoordinator(
            store = store,
            alarms = alarms,
            notifications = FakeNotificationService()
        )

        val result = coordinator.editPlan(
            planId = "p1",
            name = "Morning Updated",
            hour = 9,
            minute = 15,
            repeatMode = ReminderRepeatMode.DAILY,
            intervalDays = 1,
            startDateEpochDay = null,
            scheduleReason = "test"
        )

        assertEquals(
            PlanMutationResult.Failure(PlanMutationFailureReason.SCHEDULE_FAILED),
            result
        )
        val latest = store.getPlan("p1")
        assertEquals("Morning", latest?.name)
        assertEquals(8, latest?.hour)
        assertEquals(0, latest?.minute)
    }

    @Test
    fun setPlanEnabled_false_cancelsRemindersAndNotification() {
        val existingPlan = basePlan(
            id = "p1",
            name = "Evening",
            hour = 21,
            minute = 0,
            enabled = true,
            isReminderActive = true
        )
        val store = FakePlanStore(initialPlans = listOf(existingPlan))
        val alarms = FakeAlarmService(scheduleResult = true)
        val notifications = FakeNotificationService()
        val coordinator = newCoordinator(
            store = store,
            alarms = alarms,
            notifications = notifications
        )

        val result = coordinator.setPlanEnabled(
            planId = "p1",
            enabled = false,
            scheduleReason = "test"
        )

        assertTrue(result is PlanMutationResult.Success)
        val latest = store.getPlan("p1")
        assertFalse(latest?.enabled ?: true)
        assertFalse(latest?.isReminderActive ?: true)
        assertEquals(listOf("p1"), alarms.cancelPlanRemindersForPlanIds)
        assertEquals(listOf("p1"), notifications.cancelForPlanIds)
    }

    private fun newCoordinator(
        store: ReminderPlanStore,
        alarms: ReminderAlarmService,
        notifications: ReminderNotificationService
    ): ReminderPlanCoordinator {
        return ReminderPlanCoordinator(
            nowProvider = { 1_000L },
            zoneIdProvider = { ZoneId.of("UTC") },
            store = store,
            alarmService = alarms,
            notificationService = notifications
        )
    }

    private fun basePlan(
        id: String,
        name: String,
        hour: Int,
        minute: Int,
        enabled: Boolean,
        isReminderActive: Boolean = false
    ): ReminderPlan {
        return ReminderPlan(
            id = id,
            name = name,
            hour = hour,
            minute = minute,
            repeatMode = ReminderRepeatMode.DAILY,
            intervalDays = 1,
            startDateEpochDay = null,
            enabled = enabled,
            notificationId = 1001,
            isReminderActive = isReminderActive,
            suppressNextDeleteFallback = false
        )
    }

    private class FakePlanStore(
        initialPlans: List<ReminderPlan> = emptyList()
    ) : ReminderPlanStore {
        private val plans = initialPlans.toMutableList()
        private var nextNotificationId = 2000

        override val maxPlanCount: Int = ReminderSessionStore.MAX_PLAN_COUNT

        override fun getPlans(): List<ReminderPlan> = plans.toList()

        override fun getPlan(planId: String): ReminderPlan? {
            return plans.firstOrNull { it.id == planId }
        }

        override fun addPlan(
            name: String,
            hour: Int,
            minute: Int,
            enabled: Boolean,
            repeatMode: ReminderRepeatMode,
            intervalDays: Int,
            startDateEpochDay: Long?
        ): ReminderPlan {
            val plan = ReminderPlan(
                id = "p${plans.size + 1}",
                name = name,
                hour = hour,
                minute = minute,
                repeatMode = repeatMode,
                intervalDays = intervalDays,
                startDateEpochDay = startDateEpochDay,
                enabled = enabled,
                notificationId = nextNotificationId++,
                isReminderActive = false,
                suppressNextDeleteFallback = false
            )
            plans += plan
            return plan
        }

        override fun updatePlan(plan: ReminderPlan) {
            val index = plans.indexOfFirst { it.id == plan.id }
            if (index >= 0) {
                plans[index] = plan
            }
        }

        override fun deletePlan(planId: String) {
            plans.removeAll { it.id == planId }
        }

        override fun movePlanUp(planId: String) = Unit

        override fun movePlanDown(planId: String) = Unit

        override fun markReminderConfirmed(planId: String) {
            val index = plans.indexOfFirst { it.id == planId }
            if (index >= 0) {
                plans[index] = plans[index].copy(
                    isReminderActive = false,
                    suppressNextDeleteFallback = true
                )
            }
        }
    }

    private class FakeAlarmService(
        private val scheduleResult: Boolean
    ) : ReminderAlarmService {
        var scheduleCalls: Int = 0
        val cancelPlanRemindersForPlanIds = mutableListOf<String>()
        val cancelFallbackForPlanIds = mutableListOf<String>()

        override fun scheduleDailyAt(
            plan: ReminderPlan,
            triggerAtMs: Long,
            reason: String
        ): Boolean {
            scheduleCalls += 1
            return scheduleResult
        }

        override fun cancelPlanReminders(plan: ReminderPlan) {
            cancelPlanRemindersForPlanIds += plan.id
        }

        override fun cancelPlanFallbackReminder(plan: ReminderPlan) {
            cancelFallbackForPlanIds += plan.id
        }
    }

    private class FakeNotificationService : ReminderNotificationService {
        val shownForPlanIds = mutableListOf<String>()
        val cancelForPlanIds = mutableListOf<String>()

        override fun showNotification(
            plan: ReminderPlan,
            reason: String
        ) {
            shownForPlanIds += plan.id
        }

        override fun cancelReminderNotification(plan: ReminderPlan) {
            cancelForPlanIds += plan.id
        }
    }
}
