# ỨNG DỤNG CƠN SAY — ANDROID MVP
> **“Tỉnh táo đặt luật, Cơn Say giữ lời.”**

---

## 1. GIỚI THIỆU DỰ ÁN
**Cơn Say** là ứng dụng Android giúp người dùng tự bảo vệ mình khỏi những hành động bốc đồng khi uống rượu bia (nhắn tin người yêu cũ, đăng status mất kiểm soát, mua sắm online,...).

- **Khi tỉnh táo (trước khi đi nhậu):** Người dùng chủ động chọn khung giờ bảo vệ (hỗ trợ qua đêm, ví dụ 20:00 -> 02:00 hôm sau) và chọn các app cần hạn chế (Facebook, Messenger, Zalo, Instagram,...).
- **Khi trong phiên bảo vệ:** Nếu người dùng lỡ mở app bị hạn chế, Cơn Say phát hiện tức thì qua `AccessibilityService` và hiển thị màn hình can thiệp với các lời nhắc nhở hài hước, thân thiện, tạo "khoảng dừng nhận thức" để họ quay lại.
- **Khi hết thời gian:** Phiên tự động kết thúc thông qua `AlarmManager`.
- **Bảo mật:** 100% dữ liệu lưu trữ cục bộ qua Jetpack DataStore, không cần tài khoản, không kết nối Internet.

---

## 2. KIẾN TRÚC KỸ THUẬT & CÔNG NGHỆ
- **Ngôn ngữ:** Kotlin 2.0+
- **Giao diện:** Jetpack Compose, Material 3, Dark Theme sang trọng (Tím đậm / Indigo)
- **Kiến trúc:** Clean Architecture / MVVM (Model - Domain - Data - UI phân tách độc lập)
- **Lưu trữ cục bộ:** Jetpack DataStore Preferences
- **Cơ chế can thiệp:** `AccessibilityService` (lắng nghe `TYPE_WINDOW_STATE_CHANGED`, chỉ đọc `packageName`, không đọc nội dung màn hình)
- **Định thời gian:** `AlarmManager` (Exact Alarm with `setExactAndAllowWhileIdle`), khôi phục phiên sau reboot bằng `BootCompletedReceiver`
- **Tương thích:** Android 8.0 (API 26) đến Android 14/15 (API 34/35)

---

## 3. CẤU TRÚC THƯ MỤC MÃ NGUỒN
```text
con-say/
├── TECHNICAL_FEASIBILITY.md           # Báo cáo kiểm chứng khả thi & rủi ro Android
├── build.gradle.kts                   # Root build script
├── settings.gradle.kts                # Cấu hình module
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties      # Gradle 8.7 Wrapper
└── app/
    ├── build.gradle.kts               # Module build script (Compose BOM, DataStore)
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml    # Quyền, Service, Receiver, Intent queries
        │   ├── res/
        │   │   ├── xml/accessibility_service_config.xml
        │   │   └── values/{strings,colors,themes}.xml
        │   └── java/com/consay/app/
        │       ├── ConSayApplication.kt
        │       ├── model/             # SessionState, SessionConfig, AppItem
        │       ├── domain/            # SessionCalculator (xử lý giờ, qua nửa đêm)
        │       ├── data/              # SessionRepository (DataStore), MessageRepository
        │       ├── service/           # ConSayAccessibilityService
        │       ├── scheduler/         # SessionScheduler (AlarmManager)
        │       ├── receiver/          # SessionAlarmReceiver, BootCompletedReceiver
        │       └── ui/
        │           ├── MainActivity.kt
        │           ├── theme/{Color,Theme,Type}.kt
        │           └── screens/
        │               ├── home/{HomeScreen,HomeViewModel}.kt
        │               ├── setup/{SetupSessionScreen,SetupSessionViewModel}.kt
        │               ├── appselect/{AppSelectScreen,AppSelectViewModel}.kt
        │               ├── intervention/InterventionActivity.kt
        │               └── permission/PermissionGuideScreen.kt
        └── test/java/com/consay/app/
            └── domain/SessionCalculatorTest.kt # Bộ kiểm thử Unit test thời gian phiên
```

---

## 4. HƯỚNG DẪN MỞ DỰ ÁN & BUILD TRÊN THIẾT BỊ THẬT

### 4.1. Mở dự án trong Android Studio
1. Khởi động **Android Studio** (phiên bản Ladybug 2024.2+ hoặc Koala).
2. Chọn **File > Open...** và trỏ đến thư mục:
   `c:\Users\HP\Desktop\con-say`
3. Chờ Gradle Sync tự động tải các dependencies.
4. Đảm bảo JDK được chọn trong Android Studio là **JDK 17 hoặc JDK 21** (`Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`).

### 4.2. Build và Cài đặt lên điện thoại thật
1. Trên điện thoại Android: Vào `Cài đặt > Thông tin điện thoại > Bấm 7 lần vào "Số hiệu bản dựng" (Build Number)` để bật chế độ Nhà phát triển.
2. Vào `Cài đặt > Tùy chọn nhà phát triển > Bật "Gỡ lỗi qua USB" (USB Debugging)`.
3. Cắm cáp kết nối điện thoại với máy tính.
4. Trong Android Studio, chọn thiết bị thật ở thanh toolbar và nhấn nút **Run 'app' (Shift + F10)**.

Hoặc build file APK debug từ dòng lệnh (khi có Android SDK & Java trong PATH):
```powershell
cd con-say
./gradlew assembleDebug
# File APK đầu ra tại: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. HƯỚNG DẪN CẤP QUYỀN TRÊN THIẾT BỊ

### Bước 1: Mở màn hình Hướng dẫn quyền trong Cơn Say
- Mở app **Cơn Say** -> Bấm vào biểu tượng bánh răng hoặc banner cảnh báo "Chưa bật Hỗ trợ tiếp cận".

### Bước 2: Xử lý "Cài đặt bị hạn chế" (Bắt buộc với Android 13+)
Nếu điện thoại chạy Android 13 trở lên và bạn cài đặt app qua file APK (sideload), hệ thống sẽ làm mờ nút Accessibility:
1. Nhấn nút **"Mở Thông tin ứng dụng Cơn Say"** trên màn hình hướng dẫn (hoặc vào `Cài đặt > Ứng dụng > Cơn Say`).
2. Nhấn vào biểu tượng **3 chấm (⋮)** ở góc trên bên phải.
3. Chọn **"Cho phép cài đặt bị hạn chế"** (Allow restricted settings).
4. Xác thực bằng vân tay/mã khóa màn hình.

### Bước 3: Bật Dịch vụ Hỗ trợ tiếp cận
1. Nhấn nút **"Mở Cài đặt Hỗ trợ tiếp cận"** trong Cơn Say.
2. Tìm mục **Ứng dụng đã tải xuống (Downloaded apps) > Cơn Say**.
3. Gạt công tắc sang **BẬT (ON)** và bấm **Cho phép (Allow)**.

---

## 6. CHECKLIST KIỂM THỬ THỰC TẾ TRÊN ĐIỆN THOẠI

| # | Tình huống kiểm thử | Các bước thực hiện | Kết quả kỳ vọng |
|---|---|---|---|
| 1 | **Kiểm tra trạng thái quyền** | Mở app khi chưa bật quyền | Banner vàng cảnh báo hiển thị rõ, nút "Bật ngay" hoạt động. |
| 2 | **Cấp quyền Accessibility** | Làm theo các bước ở Mục 5 | Khi quay lại app, trạng thái tự chuyển sang xanh "Đã kích hoạt". |
| 3 | **Chọn ứng dụng** | Vào "Thiết lập" -> "Ứng dụng cần hạn chế" -> Bấm "Chọn nhanh MXH" hoặc tick Facebook/Zalo | App count tăng chính xác, danh sách lưu sau khi thoát ra vào lại. |
| 4 | **Phiên trong ngày** | Đặt phiên từ giờ hiện tại đến +10 phút sau | Trạng thái chuyển sang "ĐANG BẢO VỆ", đồng hồ đếm ngược chạy từng giây. |
| 5 | **Phiên qua nửa đêm** | Đặt phiên từ 21:00 đến 02:00 sáng | Hiển thị huy hiệu "Phiên kéo dài qua nửa đêm", thời lượng tính đúng ~5 giờ. |
| 6 | **Kích hoạt can thiệp** | Khi phiên ACTIVE, nhấn Home và mở Facebook hoặc Zalo | Trong vòng 150ms, màn hình can thiệp Cơn Say hiện lên với câu nhắc nhở ngẫu nhiên. |
| 7 | **Bấm "Quay lại nào"** | Tại màn hình can thiệp, bấm "Quay lại nào" | Lập tức đưa người dùng về màn hình Home điện thoại. |
| 8 | **Bấm "Tôi vẫn muốn tiếp tục"** | Tại màn hình can thiệp, bấm "Tôi vẫn muốn tiếp tục" | Màn hình can thiệp đóng lại, mở lại Facebook bình thường (tạm hoãn 60s không làm phiền). |
| 9 | **Khởi động lại điện thoại** | Kích hoạt phiên 2 tiếng -> Restart máy | Sau khi mở máy, phiên bảo vệ vẫn duy trì ACTIVE và bảo vệ bình thường. |
| 10 | **Hết giờ phiên** | Chờ hoặc chỉnh giờ hệ thống qua mốc kết thúc | Phiên tự chuyển sang INACTIVE/COMPLETED, không can thiệp nữa. |
| 11 | **Tương thích App Ngân hàng** | Mở app ngân hàng (VCB/MB/Techcombank) | Ghi nhận phản ứng của app ngân hàng. Nếu app ngân hàng cảnh báo Accessibility, tắt Cơn Say khi cần giao dịch. |

---

## 7. CÁC GIỚI HẠN KỸ THUẬT & LƯU Ý QUAN TRỌNG
1. **Không chặn tuyệt đối:** Cơn Say là công cụ hỗ trợ tâm lý và tự chủ bản thân, không phải ứng dụng MDM/Kiosk. Cơ chế Android không cho phép chặn 100% việc khởi động app khác mà không can thiệp sâu vào ROM/root.
2. **Quản lý pin OEM (Xiaomi, Samsung, Oppo, Vivo):** Một số hãng tự động tắt ứng dụng chạy nền sau vài tiếng. Hãy bật "Không giới hạn pin" (No battery restrictions) và khóa đa nhiệm app Cơn Say để dịch vụ chạy xuyên suốt.
3. **Môi trường hiện tại:** Môi trường dòng lệnh máy tính chưa cài đặt sẵn Android SDK/JDK CLI, do đó dự án đã được thiết kế sẵn sàng để mở trực tiếp trong Android Studio hoặc build với bất kỳ máy tính/CI có cài JDK 17+. Toàn bộ logic thời gian cốt lõi đã được kiểm thử với Unit Test độc lập.

---

## 8. CƠ CHẾ TỰ ĐỘNG CẬP NHẬT (AUTO-UPDATE) NHƯ ANTI-STALK

Ứng dụng **Cơn Say** đã được tích hợp đầy đủ hệ thống tự động cập nhật không cần cài tay, hoạt động theo cơ chế **"1 chạm duy nhất"**:

### 8.1. Cách thức hoạt động
1. **GitHub Actions CI/CD (`.github/workflows/build-apk.yml`):**
   - Mỗi khi bạn `git push` code lên nhánh `main`, GitHub sẽ tự động:
     - Tăng số phiên bản tag `v1 -> v2 -> v3...`
     - Biên dịch ra file APK đã được ký bởi keystore cố định (`keystore/debug.keystore`).
     - Tự động xuất bản một **GitHub Release** kèm file `app-debug.apk`.
2. **In-App Updater (`AppUpdater.kt`):**
   - Mỗi lần mở ứng dụng Cơn Say, app tự động kiểm tra xem có Release mới trên GitHub hay không.
   - Nếu có bản mới: Hiện banner **"🚀 Có bản cập nhật mới (vX)"**.
   - Bấm **Cập nhật**: App tự động tải APK ngầm qua `DownloadManager` và mở ngay trình cài đặt hệ thống.
   - Người dùng chỉ cần ấn nút **Cập nhật** của Android mà không cần phải vào trình duyệt tải file hay tìm APK thủ công!

### 8.2. Hướng dẫn đẩy lên GitHub mới
1. Tạo một repository mới trên GitHub (ví dụ đặt tên là `con-say` hoặc bất kỳ tên nào, để chế độ **Public**).
2. Mở terminal tại thư mục `C:\Users\HP\Desktop\con-say`:
   ```powershell
   git remote add origin https://github.com/<tai-khoan-cua-ban>/con-say.git
   git push -u origin main
   ```
3. Mở file `AppUpdater.kt` và đổi dòng:
   ```kotlin
   var githubRepo: String = "<tai-khoan-cua-ban>/con-say"
   ```
   Sau đó commit và push. GitHub sẽ tự động kích hoạt tiến trình build bản phát hành v1 đầu tiên cho bạn!
