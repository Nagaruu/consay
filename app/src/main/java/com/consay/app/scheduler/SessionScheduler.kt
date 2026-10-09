package com.consay.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.consay.app.model.SessionConfig
import com.consay.app.receiver.SessionAlarmReceiver

object SessionScheduler {

    private const val REQUEST_CODE_START = 1001
    private const val REQUEST_CODE_END = 1002

    fun scheduleSessionAlarms(context: Context, config: SessionConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Lập lịch cho thời điểm bắt đầu nếu phiên đang ở trạng thái READY
        if (config.startTimeMillis > System.currentTimeMillis()) {
            val startIntent = Intent(context, SessionAlarmReceiver::class.java).apply {
                action = SessionAlarmReceiver.ACTION_SESSION_START
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_START,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExact(alarmManager, config.startTimeMillis, startPendingIntent)
        }

        // 2. Lập lịch cho thời điểm kết thúc phiên
        if (config.endTimeMillis > System.currentTimeMillis()) {
            val endIntent = Intent(context, SessionAlarmReceiver::class.java).apply {
                action = SessionAlarmReceiver.ACTION_SESSION_END
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_END,
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExact(alarmManager, config.endTimeMillis, endPendingIntent)
        }
    }

    fun cancelSessionAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val startIntent = Intent(context, SessionAlarmReceiver::class.java)
        val startPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_START,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        startPending?.let { alarmManager.cancel(it) }

        val endIntent = Intent(context, SessionAlarmReceiver::class.java)
        val endPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        endPending?.let { alarmManager.cancel(it) }
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
