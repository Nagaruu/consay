package com.consay.app.ui.screens.intervention

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.consay.app.ConSayApplication
import com.consay.app.data.MessageRepository
import com.consay.app.domain.SessionCalculator
import com.consay.app.ui.theme.BorderSubtle
import com.consay.app.ui.theme.ConSayTheme
import com.consay.app.ui.theme.PurpleAccent
import com.consay.app.ui.theme.PurpleBackground
import com.consay.app.ui.theme.PurplePrimary
import com.consay.app.ui.theme.PurpleSurface
import com.consay.app.ui.theme.PurpleSurfaceVariant
import com.consay.app.ui.theme.TextMuted
import com.consay.app.ui.theme.TextPrimary
import com.consay.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class InterventionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_END_TIME = "extra_end_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val endTimeMillis = intent.getLongExtra(EXTRA_END_TIME, 0L)

        val targetAppLabel = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(targetPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            targetPackage.ifEmpty { "Ứng dụng này" }
        }

        // Khi người dùng bấm nút Back vật lý, đưa về Home của điện thoại
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToHomeScreen()
            }
        })

        setContent {
            ConSayTheme {
                InterventionScreen(
                    targetAppLabel = targetAppLabel,
                    endTimeMillis = endTimeMillis,
                    onGoBackHome = { navigateBackToHomeScreen() },
                    onProceedAnyway = {
                        lifecycleScope.launch {
                            // Tạm hoãn can thiệp 60 giây để họ dùng nếu thực sự cần
                            ConSayApplication.instance.sessionRepository.recordInterventionCooldown(60_000L)
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun navigateBackToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun InterventionScreen(
    targetAppLabel: String,
    endTimeMillis: Long,
    onGoBackHome: () -> Unit,
    onProceedAnyway: () -> Unit
) {
    val randomMessage = remember { MessageRepository.getRandomMessage() }
    val remainingText = remember(endTimeMillis) {
        SessionCalculator.formatRemainingTime(endTimeMillis)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Warning Icon Glow
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(PurpleAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = PurpleAccent,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ê, đã hứa với bản thân rồi mà!",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bạn đang cố mở $targetAppLabel",
            fontSize = 15.sp,
            color = PurpleAccent,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Message Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = PurpleSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "“${randomMessage.text}”",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PurpleSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Phiên Cơn Say còn: $remainingText",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Action Buttons
        Button(
            onClick = onGoBackHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Quay lại nào (Khuyên dùng)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onProceedAnyway,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Tôi vẫn muốn tiếp tục (Tạm dừng 1 phút)",
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }
}
