package com.consay.app.ui.screens.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.consay.app.ConSayApplication
import com.consay.app.domain.SessionCalculator
import com.consay.app.model.SessionConfig
import com.consay.app.scheduler.SessionScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class SetupUiState(
    val startHour: Int = 19,
    val startMinute: Int = 0,
    val endHour: Int = 1,
    val endMinute: Int = 0,
    val selectedPackagesCount: Int = 0,
    val isOvernight: Boolean = true,
    val durationText: String = "",
    val error: String? = null
)

class SetupSessionViewModel : ViewModel() {
    private val repository = ConSayApplication.instance.sessionRepository

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        loadCurrentOrPreset()
    }

    private fun loadCurrentOrPreset() {
        viewModelScope.launch {
            val config = repository.sessionFlow.first()
            val nowCal = Calendar.getInstance()
            val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)

            val sHour = if (config.startTimeMillis > 0) config.startHour else currentHour
            val sMin = if (config.startTimeMillis > 0) config.startMinute else 0
            val eHour = if (config.endTimeMillis > 0) config.endHour else (currentHour + 5) % 24
            val eMin = if (config.endTimeMillis > 0) config.endMinute else 0

            recalculate(sHour, sMin, eHour, eMin, config.restrictedPackages.size)
        }
    }

    fun setStartTime(hour: Int, minute: Int) {
        recalculate(hour, minute, _uiState.value.endHour, _uiState.value.endMinute, _uiState.value.selectedPackagesCount)
    }

    fun setEndTime(hour: Int, minute: Int) {
        recalculate(_uiState.value.startHour, _uiState.value.startMinute, hour, minute, _uiState.value.selectedPackagesCount)
    }

    fun refreshSelectedCount() {
        viewModelScope.launch {
            val config = repository.sessionFlow.first()
            recalculate(
                _uiState.value.startHour,
                _uiState.value.startMinute,
                _uiState.value.endHour,
                _uiState.value.endMinute,
                config.restrictedPackages.size
            )
        }
    }

    private fun recalculate(sH: Int, sM: Int, eH: Int, eM: Int, appCount: Int) {
        val (startMillis, endMillis) = SessionCalculator.calculateTimestamps(sH, sM, eH, eM)
        val isCrossMidnight = (eH < sH) || (eH == sH && eM <= sM)
        val durationFormatted = SessionCalculator.formatRemainingTime(endMillis, startMillis)

        _uiState.value = SetupUiState(
            startHour = sH,
            startMinute = sM,
            endHour = eH,
            endMinute = eM,
            selectedPackagesCount = appCount,
            isOvernight = isCrossMidnight,
            durationText = durationFormatted
        )
    }

    fun activate(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentConfig = repository.getCurrentConfig()
            if (currentConfig.restrictedPackages.isEmpty()) {
                _uiState.value = _uiState.value.copy(error = "Vui lòng chọn ít nhất 1 ứng dụng cần hạn chế.")
                return@launch
            }

            val newConfig = repository.activateSession(
                startHour = _uiState.value.startHour,
                startMinute = _uiState.value.startMinute,
                endHour = _uiState.value.endHour,
                endMinute = _uiState.value.endMinute,
                restrictedPackages = currentConfig.restrictedPackages
            )

            // Lập lịch AlarmManager
            SessionScheduler.scheduleSessionAlarms(context, newConfig)
            onSuccess()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
