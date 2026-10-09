package com.consay.app.domain

import com.consay.app.model.SessionConfig
import com.consay.app.model.SessionState
import java.util.Calendar

object SessionCalculator {

    /**
     * Tính toán thời điểm bắt đầu và kết thúc dạng epoch millis dựa vào giờ/phút người dùng chọn.
     * Xử lý trường hợp phiên đi qua nửa đêm (ví dụ: 20:00 -> 02:00 sáng hôm sau).
     */
    fun calculateTimestamps(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        referenceNowMillis: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = referenceNowMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            timeInMillis = referenceNowMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Nếu giờ kết thúc nhỏ hơn hoặc bằng giờ bắt đầu, phiên kéo dài qua nửa đêm
        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Nếu tại thời điểm bấm kích hoạt mà giờ kết thúc đã trôi qua,
        // đẩy cả phiên sang chu kỳ ngày tiếp theo
        if (endCal.timeInMillis < referenceNowMillis) {
            startCal.add(Calendar.DAY_OF_YEAR, 1)
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Xác định trạng thái phiên hiện tại dựa trên mốc thời gian thực tế.
     */
    fun resolveCurrentState(
        config: SessionConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): SessionState {
        if (config.state == SessionState.INACTIVE) {
            return SessionState.INACTIVE
        }

        if (config.startTimeMillis == 0L || config.endTimeMillis == 0L) {
            return SessionState.INACTIVE
        }

        return when {
            nowMillis < config.startTimeMillis -> SessionState.READY
            nowMillis in config.startTimeMillis..config.endTimeMillis -> SessionState.ACTIVE
            else -> SessionState.COMPLETED
        }
    }

    /**
     * Định dạng thời gian còn lại sang chuỗi tiếng Việt dễ hiểu.
     */
    fun formatRemainingTime(endTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = endTimeMillis - nowMillis
        if (diff <= 0) return "0 phút"

        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

        return when {
            hours > 0 && minutes > 0 -> "${hours} giờ ${minutes} phút"
            hours > 0 -> "${hours} giờ"
            else -> "${minutes.coerceAtLeast(1)} phút"
        }
    }

    /**
     * Định dạng giờ:phút hiển thị (VD: 20:00).
     */
    fun formatTimeDisplay(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }
}
