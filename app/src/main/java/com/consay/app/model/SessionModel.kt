package com.consay.app.model

import android.graphics.drawable.Drawable

enum class SessionState {
    INACTIVE,   // Chưa kích hoạt phiên
    READY,      // Đã thiết lập xong, đang chờ đến giờ bắt đầu
    ACTIVE,     // Đang trong khoảng thời gian bảo vệ
    COMPLETED   // Phiên vừa kết thúc đúng hạn
}

data class SessionConfig(
    val startHour: Int = 19,
    val startMinute: Int = 0,
    val endHour: Int = 1,
    val endMinute: Int = 0,
    val restrictedPackages: Set<String> = emptySet(),
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val state: SessionState = SessionState.INACTIVE
) {
    val isProtectionActive: Boolean
        get() = state == SessionState.ACTIVE
}

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val isSelected: Boolean = false,
    val isSocialOrMessaging: Boolean = false
)

data class InterventionMessage(
    val id: Int,
    val text: String
)
