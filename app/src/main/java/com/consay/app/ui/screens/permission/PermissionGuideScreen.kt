package com.consay.app.ui.screens.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.consay.app.service.ConSayAccessibilityService
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isEnabled by remember { mutableStateOf(ConSayAccessibilityService.isAccessibilityEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == LifecycleEvent.Event.ON_RESUME) {
                isEnabled = ConSayAccessibilityService.isAccessibilityEnabled(context)
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
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Quyền & Minh bạch dữ liệu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PurpleBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Trạng thái hiện tại
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isEnabled) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = PurpleSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isEnabled) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnabled) "Dịch vụ đã được kích hoạt" else "Dịch vụ chưa được bật",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isEnabled) "Cơn Say đã sẵn sàng phát hiện và bảo vệ bạn." else "Vui lòng làm theo hướng dẫn bên dưới để bật.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Minh bạch quyền Accessibility
            Text(
                text = "1. Tại sao Cơn Say cần quyền Hỗ trợ tiếp cận?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android phân tách các ứng dụng độc lập. Để biết khi nào bạn vừa mở Facebook hay Zalo lúc đang say để hiện màn hình can thiệp, Cơn Say cần quyền AccessibilityService để lắng nghe sự kiện đổi cửa sổ.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cam kết quyền riêng tư
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Cam kết bảo mật & Quyền riêng tư", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "✓ CHỈ đọc tên gói ứng dụng (Package Name) đang mở.", fontSize = 12.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✗ KHÔNG đọc nội dung màn hình hay tin nhắn riêng tư.", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✗ KHÔNG ghi phím bấm (keystroke), mật khẩu.", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✗ KHÔNG gửi bất kỳ dữ liệu nào qua Internet (hoàn toàn offline).", fontSize = 12.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cảnh báo app ngân hàng
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PurpleSurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Lưu ý về Ứng dụng Ngân hàng", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Một số app ngân hàng tại Việt Nam sẽ cảnh báo hoặc từ chối mở khi có dịch vụ Accessibility đang bật trên máy. Đây là cơ chế bảo mật của ngân hàng, bạn có thể tắt tạm Cơn Say khi cần chuyển khoản.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hướng dẫn Android 13+ Sideload Restricted Settings
            Text(
                text = "2. Nếu bị báo \"Cài đặt bị hạn chế\" (Android 13+)?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Khi cài app từ file APK trực tiếp, Android 13+ sẽ khóa nút Accessibility. Cách mở:\n1. Vào Cài đặt ứng dụng > Cơn Say\n2. Nhấn vào dấu 3 chấm (⋮) ở góc trên bên phải\n3. Chọn \"Cho phép cài đặt bị hạn chế\" (Allow restricted settings)\n4. Quay lại đây và bật Hỗ trợ tiếp cận.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mở Thông tin ứng dụng Cơn Say", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nút mở Accessibility Settings
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mở Cài đặt Hỗ trợ tiếp cận",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
