package com.consay.app.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.consay.app.ConSayApplication
import com.consay.app.domain.SessionCalculator
import com.consay.app.model.SessionConfig
import com.consay.app.model.SessionState
import com.consay.app.scheduler.SessionScheduler
import com.consay.app.service.ConSayAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val config: SessionConfig = SessionConfig(),
    val isAccessibilityEnabled: Boolean = false,
    val remainingTimeString: String = "",
    val readableEndTime: String = "",
    val protectedAppCount: Int = 0
)

class HomeViewModel : ViewModel() {
    private val repository = ConSayApplication.instance.sessionRepository
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _ticker = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.sessionFlow,
        _isAccessibilityEnabled,
        _ticker
    ) { config, isA11y, now ->
        val resolvedState = SessionCalculator.resolveCurrentState(config, now)
        val activeConfig = if (resolvedState != config.state) config.copy(state = resolvedState) else config

        val remainingStr = if (activeConfig.state == SessionState.ACTIVE) {
            SessionCalculator.formatRemainingTime(activeConfig.endTimeMillis, now)
        } else ""

        val endFormatted = if (activeConfig.state == SessionState.ACTIVE || activeConfig.state == SessionState.READY) {
            SessionCalculator.formatTimeDisplay(activeConfig.endHour, activeConfig.endMinute)
        } else ""

        HomeUiState(
            config = activeConfig,
            isAccessibilityEnabled = isA11y,
            remainingTimeString = remainingStr,
            readableEndTime = endFormatted,
            protectedAppCount = activeConfig.restrictedPackages.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        // Cập nhật bộ đếm thời gian mỗi giây khi phiên đang chạy
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _ticker.value = System.currentTimeMillis()
            }
        }
    }

    fun checkPermission(context: Context) {
        _isAccessibilityEnabled.value = ConSayAccessibilityService.isAccessibilityEnabled(context)
    }

    fun stopSession(context: Context) {
        viewModelScope.launch {
            repository.endSession()
            SessionScheduler.cancelSessionAlarms(context)
        }
    }
}
