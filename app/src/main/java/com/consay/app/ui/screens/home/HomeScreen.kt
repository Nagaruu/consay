package com.consay.app.ui.screens.home

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.consay.app.BuildConfig
import com.consay.app.core.AppUpdater
import com.consay.app.model.SessionState
import com.consay.app.ui.theme.BorderSubtle
import com.consay.app.ui.theme.PurpleAccent
import com.consay.app.ui.theme.PurpleBackground
import com.consay.app.ui.theme.PurplePrimary
import com.consay.app.ui.theme.PurpleSurface
import com.consay.app.ui.theme.PurpleSurfaceVariant
import com.consay.app.ui.theme.SuccessGreen
import com.consay.app.ui.theme.TextMuted
import com.consay.app.ui.theme.TextPrimary
import com.consay.app.ui.theme.TextSecondary
import com.consay.app.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var availableUpdate by remember { mutableStateOf<AppUpdater.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // Tự động kiểm tra bản cập nhật mới khi mở ứng dụng
    LaunchedEffect(Unit) {
        val res = AppUpdater.checkForUpdates()
        if (res is AppUpdater.CheckResult.UpdateAvailable) {
            availableUpdate = res.info
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == LifecycleEvent.Event.ON_RESUME) {
                viewModel.checkPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleBackground)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cơn Say",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Tỉnh táo đặt luật, Cơn Say giữ lời.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onNavigateToPermissions,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PurpleSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt & Quyền",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Banner Cập nhật tự động (nếu có bản mới)
        availableUpdate?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Có bản cập nhật mới (${info.tag})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tải về tự động và cài đặt chỉ với 1 chạm.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "Đang tải bản cập nhật...", Toast.LENGTH_SHORT).show()
                            AppUpdater.startUpdateDownload(context, info)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Cập nhật", color = PurpleBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Banner Cảnh báo Quyền Accessibility nếu chưa bật
        if (!uiState.isAccessibilityEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chưa bật Hỗ trợ tiếp cận",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cần bật quyền để Cơn Say phát hiện ứng dụng được bảo vệ.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = onNavigateToPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Bật ngay", color = PurpleBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Status Card chính
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = PurpleSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trạng thái badge
                val (badgeColor, badgeText) = when (uiState.config.state) {
                    SessionState.ACTIVE -> Pair(SuccessGreen, "ĐANG BẢO VỆ")
                    SessionState.READY -> Pair(WarningAmber, "ĐANG CHỜ BẮT ĐẦU")
                    SessionState.COMPLETED -> Pair(TextMuted, "ĐÃ HOÀN THÀNH")
                    SessionState.INACTIVE -> Pair(TextMuted, "CHƯA KÍCH HOẠT")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Icon trung tâm & Thời gian
                when (uiState.config.state) {
                    SessionState.ACTIVE -> {
                        Text(
                            text = "Thời gian bảo vệ còn lại",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.remainingTimeString,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tự động kết thúc lúc ${uiState.readableEndTime}",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                    SessionState.READY -> {
                        Text(
                            text = "Sẽ kích hoạt vào phiên uống rượu",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kết thúc: ${uiState.readableEndTime}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PurpleSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bạn chưa kích hoạt phiên nào",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chọn ứng dụng và đặt giờ trước khi đi uống để Cơn Say giữ lời hứa giúp bạn.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Danh sách ứng dụng bảo vệ rút gọn
                if (uiState.protectedAppCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PurpleSurfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ứng dụng được bảo vệ:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "${uiState.protectedAppCount} ứng dụng",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        if (uiState.config.state == SessionState.ACTIVE || uiState.config.state == SessionState.READY) {
            OutlinedButton(
                onClick = { viewModel.stopSession(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
            ) {
                Text(
                    text = "Dừng bảo vệ phiên này",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Button(
                onClick = onNavigateToSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Bắt đầu thiết lập Cơn Say",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nút kiểm tra cập nhật thủ công & version info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Phiên bản: v${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})",
                fontSize = 12.sp,
                color = TextMuted
            )

            OutlinedButton(
                onClick = {
                    if (isCheckingUpdate) return@OutlinedButton
                    isCheckingUpdate = true
                    coroutineScope.launch {
                        val res = AppUpdater.checkForUpdates()
                        isCheckingUpdate = false
                        when (res) {
                            is AppUpdater.CheckResult.UpdateAvailable -> {
                                availableUpdate = res.info
                                Toast.makeText(context, "Đã tìm thấy bản cập nhật ${res.info.tag}!", Toast.LENGTH_SHORT).show()
                            }
                            is AppUpdater.CheckResult.UpToDate -> {
                                Toast.makeText(context, "Bạn đang dùng bản mới nhất.", Toast.LENGTH_SHORT).show()
                            }
                            is AppUpdater.CheckResult.Error -> {
                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isCheckingUpdate) "Đang kiểm tra..." else "Kiểm tra cập nhật",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
