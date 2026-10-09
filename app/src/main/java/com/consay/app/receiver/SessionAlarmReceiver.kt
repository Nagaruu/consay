package com.consay.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.consay.app.ConSayApplication
import com.consay.app.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SESSION_START = "com.consay.app.ACTION_SESSION_START"
        const val ACTION_SESSION_END = "com.consay.app.ACTION_SESSION_END"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val repository = ConSayApplication.instance.sessionRepository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = repository.getCurrentConfig()
                when (intent?.action) {
                    ACTION_SESSION_START -> {
                        if (current.state == SessionState.READY) {
                            // Chuyển sang trạng thái ACTIVE khi đến giờ bắt đầu
                            repository.activateSession(
                                current.startHour,
                                current.startMinute,
                                current.endHour,
                                current.endMinute,
                                current.restrictedPackages
                            )
                        }
                    }
                    ACTION_SESSION_END -> {
                        // Kết thúc phiên khi hết hạn
                        repository.endSession()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
