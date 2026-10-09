package com.consay.app.domain

import com.consay.app.model.SessionConfig
import com.consay.app.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SessionCalculatorTest {

    @Test
    fun testSameDaySession_CalculatesEndAfterStart() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 18, 0, 0)
        }
        val refNow = cal.timeInMillis

        // 19:00 -> 23:00 cùng ngày
        val (startMillis, endMillis) = SessionCalculator.calculateTimestamps(
            startHour = 19,
            startMinute = 0,
            endHour = 23,
            endMinute = 0,
            referenceNowMillis = refNow
        )

        assertTrue("End time must be after start time", endMillis > startMillis)
        val diffHours = (endMillis - startMillis) / (1000 * 60 * 60)
        assertEquals("Session duration should be 4 hours", 4L, diffHours)
    }

    @Test
    fun testCrossMidnightSession_CalculatesCorrectOvernightTimestamp() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 18, 30, 0)
        }
        val refNow = cal.timeInMillis

        // 20:00 -> 02:00 sáng hôm sau (qua nửa đêm)
        val (startMillis, endMillis) = SessionCalculator.calculateTimestamps(
            startHour = 20,
            startMinute = 0,
            endHour = 2,
            endMinute = 0,
            referenceNowMillis = refNow
        )

        assertTrue("Overnight end time must be greater than start time", endMillis > startMillis)
        val diffHours = (endMillis - startMillis) / (1000 * 60 * 60)
        assertEquals("Session duration overnight should be 6 hours", 6L, diffHours)
    }

    @Test
    fun testResolveCurrentState_InactiveWhenConfigInactive() {
        val config = SessionConfig(state = SessionState.INACTIVE)
        val state = SessionCalculator.resolveCurrentState(config, System.currentTimeMillis())
        assertEquals(SessionState.INACTIVE, state)
    }

    @Test
    fun testResolveCurrentState_ReadyBeforeStartTime() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 20, 0, 0)
        }
        val startMillis = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 4)
        val endMillis = cal.timeInMillis

        val config = SessionConfig(
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            state = SessionState.READY
        )

        // Kiểm tra lúc 19:00 (trước giờ bắt đầu)
        val checkCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 19, 0, 0)
        }
        val state = SessionCalculator.resolveCurrentState(config, checkCal.timeInMillis)
        assertEquals(SessionState.READY, state)
    }

    @Test
    fun testResolveCurrentState_ActiveDuringSession() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 20, 0, 0)
        }
        val startMillis = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 4)
        val endMillis = cal.timeInMillis

        val config = SessionConfig(
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            state = SessionState.ACTIVE
        )

        // Kiểm tra lúc 21:30 (trong giờ phiên)
        val checkCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 21, 30, 0)
        }
        val state = SessionCalculator.resolveCurrentState(config, checkCal.timeInMillis)
        assertEquals(SessionState.ACTIVE, state)
    }

    @Test
    fun testResolveCurrentState_CompletedAfterEndTime() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 9, 20, 0, 0)
        }
        val startMillis = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 4) // Hết lúc 00:00 ngày 10/10
        val endMillis = cal.timeInMillis

        val config = SessionConfig(
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            state = SessionState.ACTIVE
        )

        // Kiểm tra lúc 01:00 ngày 10/10 (đã quá hạn)
        val checkCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 1, 0, 0)
        }
        val state = SessionCalculator.resolveCurrentState(config, checkCal.timeInMillis)
        assertEquals(SessionState.COMPLETED, state)
    }

    @Test
    fun testFormatRemainingTime() {
        val now = 1000000000L
        val oneHourThirtyMinutesLater = now + (90 * 60 * 1000L)
        val result = SessionCalculator.formatRemainingTime(oneHourThirtyMinutesLater, now)
        assertEquals("1 giờ 30 phút", result)

        val fortyFiveMinutesLater = now + (45 * 60 * 1000L)
        assertEquals("45 phút", SessionCalculator.formatRemainingTime(fortyFiveMinutesLater, now))

        val expired = now - 5000L
        assertEquals("0 phút", SessionCalculator.formatRemainingTime(expired, now))
    }

    @Test
    fun testFormatTimeDisplay() {
        assertEquals("19:05", SessionCalculator.formatTimeDisplay(19, 5))
        assertEquals("01:00", SessionCalculator.formatTimeDisplay(1, 0))
    }
}
