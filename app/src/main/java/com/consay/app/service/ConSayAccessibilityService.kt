package com.consay.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.consay.app.ConSayApplication
import com.consay.app.domain.SessionCalculator
import com.consay.app.model.SessionState
import com.consay.app.ui.screens.intervention.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConSayAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptedTimestamp: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val targetPackage = event.packageName?.toString() ?: return

        // Bỏ qua nếu là chính ứng dụng Cơn Say hoặc System UI
        if (targetPackage == packageName || targetPackage == "com.android.systemui") {
            return
        }

        serviceScope.launch {
            checkAndTriggerIntervention(targetPackage)
        }
    }

    private suspend fun checkAndTriggerIntervention(targetPackage: String) {
        val repository = ConSayApplication.instance.sessionRepository
        val config = repository.getCurrentConfig()
        val now = System.currentTimeMillis()

        // 1. Kiểm tra trạng thái phiên thời gian thực
        val currentState = SessionCalculator.resolveCurrentState(config, now)
        if (currentState != SessionState.ACTIVE) {
            return
        }

        // 2. Kiểm tra xem ứng dụng mở có nằm trong danh sách hạn chế không
        if (!config.restrictedPackages.contains(targetPackage)) {
            return
        }

        // 3. Kiểm tra xem người dùng có đang trong thời gian tạm hoãn (cooldown sau khi chọn "Tôi vẫn muốn tiếp tục") không
        if (repository.isInterventionInCooldown()) {
            return
        }

        // 4. Tránh lặp lại liên tục trong khoảng 1.5 giây nếu cùng 1 package liên tục gửi event
        if (targetPackage == lastInterceptedPackage && (now - lastInterceptedTimestamp) < 1500L) {
            return
        }

        lastInterceptedPackage = targetPackage
        lastInterceptedTimestamp = now

        // 5. Khởi chạy màn hình can thiệp
        val intent = Intent(this, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, targetPackage)
            putExtra(InterventionActivity.EXTRA_END_TIME, config.endTimeMillis)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Accessibility bị gián đoạn bởi hệ thống
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        /**
         * Kiểm tra xem dịch vụ Accessibility của Cơn Say đã được người dùng bật trong Cài đặt hay chưa.
         */
        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedComponentName = "${context.packageName}/${ConSayAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}
