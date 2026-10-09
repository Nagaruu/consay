package com.consay.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.consay.app.ConSayApplication
import com.consay.app.domain.SessionCalculator
import com.consay.app.model.SessionState
import com.consay.app.scheduler.SessionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val repository = ConSayApplication.instance.sessionRepository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = repository.getCurrentConfig()
                val now = System.currentTimeMillis()
                val resolvedState = SessionCalculator.resolveCurrentState(config, now)

                when (resolvedState) {
                    SessionState.READY, SessionState.ACTIVE -> {
                        // Khôi phục lại báo thức AlarmManager nếu phiên vẫn còn hạn
                        SessionScheduler.scheduleSessionAlarms(context, config)
                    }
                    SessionState.COMPLETED, SessionState.INACTIVE -> {
                        // Nếu trong thời gian tắt máy mà phiên đã quá hạn, dọn dẹp phiên
                        repository.endSession()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
