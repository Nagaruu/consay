# BÁO CÁO KIỂM CHỨNG TÍNH KHẢ THI KỸ THUẬT (TECHNICAL FEASIBILITY)
## DỰ ÁN: CƠN SAY — ANDROID MVP
**Phiên bản:** 1.0.0  
**Tác giả:** Senior Android Engineer & Product Engineer  
**Ngày lập:** 09/10/2026  

---

## 1. TỔNG QUAN VÀ MỤC TIÊU KIỂM CHỨNG
Ứng dụng **Cơn Say** giải quyết bài toán hành vi bốc đồng khi uống rượu bia bằng cách tạo ra "khoảng dừng nhận thức" (cognitive friction / intervention pause). Khi người dùng còn tỉnh táo, họ chủ động đặt thời gian bảo vệ và chọn các ứng dụng dễ phát sinh hành động bốc đồng (Facebook, Messenger, Zalo, Instagram,...). Khi ứng dụng bị hạn chế được mở trong phiên, Cơn Say phát hiện và hiển thị màn hình nhắc nhở.

Tài liệu này xác định tính khả thi kỹ thuật, các giới hạn nền tảng Android, quyền cần thiết, rủi ro tương thích và chính sách Google Play.

---

## 2. BẢNG ĐÁNH GIÁ TÍNH NĂNG & TÍNH KHẢ THI

| Tính năng | Trạng thái khả thi | Phân tích kỹ thuật & Giới hạn |
|---|---|---|
| **1. Liệt kê ứng dụng đã cài đặt** | **Khả thi có điều kiện** | Từ Android 11 (API 30), cần khai báo `<queries>` cụ thể các package mạng xã hội hoặc quyền `QUERY_ALL_PACKAGES`. Trên Google Play, `QUERY_ALL_PACKAGES` bị kiểm duyệt gắt gao. Trong MVP, Cơn Say kết hợp `<queries>` cho các ứng dụng phổ biến (Zalo, Messenger, Facebook, Instagram, TikTok, Telegram,...) và `Intent.ACTION_MAIN` với `CATEGORY_LAUNCHER` để quét các app có launcher icon hợp lệ. |
| **2. Phát hiện ứng dụng chạy foreground qua AccessibilityService** | **Khả thi** | `AccessibilityService` lắng nghe sự kiện `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`. Khi `event.packageName` khớp với danh sách bị hạn chế, service phát hiện gần như tức thì (<100ms) mà không tốn pin polling. |
| **3. Hiển thị màn hình can thiệp (Intervention Screen)** | **Khả thi** | Khởi chạy `InterventionActivity` với flag `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NO_ANIMATION`. Android 10+ (API 29+) giới hạn khởi chạy Activity từ nền, nhưng AccessibilityService có đặc quyền tương tác trực tiếp với UI khi người dùng tương tác, cho phép đẩy Activity lên đỉnh ngăn xếp. Khi người dùng bấm "Quay lại nào", gọi `performGlobalAction(GLOBAL_ACTION_HOME)` để đưa người dùng về Home an toàn. |
| **4. Quản lý phiên qua nửa đêm (e.g. 20:00 -> 02:00)** | **Khả thi** | Tính toán timestamp Epoch chuẩn: Nếu `endTimeMillis <= startTimeMillis`, tự động cộng thêm 1 ngày (24 giờ) vào mốc kết thúc. Độc lập với định dạng hiển thị giờ cục bộ. |
| **5. Lập lịch kết thúc phiên & Khôi phục sau Restart** | **Khả thi** | Sử dụng `AlarmManager.setExactAndAllowWhileIdle()` để đánh thức thiết bị và kết thúc phiên đúng giờ kể cả khi máy vào chế độ Doze. Sử dụng `BroadcastReceiver` lắng nghe `BOOT_COMPLETED` để nạp lại trạng thái phiên từ `DataStore` sau khi máy khởi động lại. |
| **6. Chặn tuyệt đối không cho mở app** | **KHÔNG khả thi / Không cam kết** | Android không cung cấp cơ chế hệ thống cho phép app bên thứ ba chặn hoàn toàn việc mở app khác mà không root. Người dùng luôn có thể mở app rồi bị Cơn Say can thiệp. Cơn Say là "khoảng dừng nhắc nhở", không phải MDM (Mobile Device Management) hay Kiosk Mode. |
| **7. Tự động bật lại Accessibility khi bị tắt** | **KHÔNG khả thi (Bị Android cấm)** | Android bảo vệ quyền riêng tư nghiêm ngặt. Chỉ người dùng mới có thể bật quyền trong Cài đặt hệ thống. |
| **8. Tương thích với ứng dụng Ngân hàng** | **Giới hạn tương thích** | Các ứng dụng ngân hàng tại Việt Nam (Vietcombank, MB, Techcombank, VPBank,...) áp dụng giải pháp bảo mật phát hiện Accessibility Service để chống mã độc. Một số app ngân hàng sẽ từ chối mở nếu Accessibility bật. Đây là giới hạn bất khả kháng của hệ sinh thái, cần thông báo minh bạch cho người dùng. |

---

## 3. CÁC QUYỀN ANDROID CẦN THIẾT VÀ GIẢI TRÌNH

1. **`android.permission.BIND_ACCESSIBILITY_SERVICE`**
   - *Mục đích:* Cho phép `ConSayAccessibilityService` nhận sự kiện chuyển đổi cửa sổ (`TYPE_WINDOW_STATE_CHANGED`) để biết khi nào ứng dụng bị hạn chế được mở.
   - *Tính riêng tư:* Chỉ đọc `packageName` của cửa sổ đang kích hoạt. **Tuyệt đối không đọc text, không ghi nhận keystroke, không theo dõi nội dung màn hình.**
2. **`android.permission.SCHEDULE_EXACT_ALARM`**
   - *Mục đích:* Đặt báo thức chính xác lúc phiên bảo vệ kết thúc để tự động hủy chế độ Cơn Say.
3. **`android.permission.RECEIVE_BOOT_COMPLETED`**
   - *Mục đích:* Khôi phục lại trạng thái phiên bảo vệ nếu người dùng khởi động lại máy khi phiên chưa hết hạn.
4. **`android.permission.POST_NOTIFICATIONS`** (Android 13+)
   - *Mục đích:* Hiển thị thông báo trạng thái "Cơn Say đang bảo vệ bạn" và thông báo khi phiên kết thúc.

---

## 4. RỦI RO GOOGLE PLAY & KHẢ NĂNG TƯƠNG THÍCH THIẾT BỊ

### 4.1. Chính sách Google Play về AccessibilityService
- **Chính sách hiện hành:** Google Play chỉ chấp nhận AccessibilityService cho người khuyết tật HOẶC công cụ Digital Wellbeing / Hạn chế sử dụng thiết bị nếu có bản công bố nổi bật (Prominent Disclosure) và sự đồng thuận minh bạch từ người dùng trước khi yêu cầu quyền.
- **Giải pháp cho Cơn Say:**
  - Thiết kế màn hình **Hướng dẫn quyền** (`PermissionGuideScreen`) cực kỳ rõ ràng, nêu bật: Mục đích duy nhất là phát hiện tên gói ứng dụng (package name) để can thiệp; Không thu thập dữ liệu; Người dùng có thể tắt bất kỳ lúc nào trong Cài đặt.

### 4.2. Android 13+ "Restricted Settings" (Cài đặt bị hạn chế)
- Khi cài đặt file APK từ bên ngoài (sideload via adb/trình duyệt), Android 13+ sẽ làm mờ công tắc Accessibility và báo "Restricted setting".
- **Hướng dẫn xử lý cho người dùng:** Phải vào `Cài đặt > Ứng dụng > Cơn Say > Nút 3 chấm góc phải > Cho phép cài đặt bị hạn chế (Allow restricted settings)` trước khi bật Accessibility. Cơn Say cần hướng dẫn rõ bước này trong UI.

### 4.3. Quản lý pin OEM (Xiaomi MIUI/HyperOS, Samsung One UI, Oppo, Vivo)
- Các hãng Android tùy biến thường chủ động "kill" service chạy nền hoặc tắt quyền Accessibility sau một thời gian không dùng.
- **Giải pháp:** Hướng dẫn người dùng thiết lập "Không hạn chế pin" (Battery Optimization -> Unrestricted) và "Khóa ứng dụng trong Đa nhiệm" để dịch vụ duy trì liên tục.

---

## 5. CHUỖI HOẠT ĐỘNG KIỂM CHỨNG (PROTOTYPE FLOW)

```text
[Bật quyền Accessibility]
           ↓
[Chọn ứng dụng cần hạn chế: VD Facebook, Zalo]
           ↓
[Kích hoạt phiên Cơn Say (VD: 20:00 -> 02:00)]
           ↓
[Người dùng mở Facebook/Zalo]
           ↓
[AccessibilityService bắt TYPE_WINDOW_STATE_CHANGED]
           ↓
[Kiểm tra: Phiên ACTIVE? && Package khớp? && Ngoài cooldown?]
           ↓
  → Đúng: Khởi chạy InterventionActivity ("Ê, đã hứa rồi mà!")
  → Bấm "Quay lại nào": performGlobalAction(GLOBAL_ACTION_HOME)
  → Bấm "Vẫn muốn tiếp tục": Cooldown can thiệp trong 60 giây
           ↓
[Đến giờ kết thúc phiên -> AlarmManager trigger -> Chuyển INACTIVE]
```

---

## 6. KẾ HOẠCH THỬ NGHIỆM TRÊN THIẾT BỊ THẬT & MÔI TRƯỜNG HIỆN TẠI

1. **Môi trường cục bộ hiện tại:** Máy tính Windows chưa cài đặt sẵn Android SDK / JDK trong biến môi trường PATH. Do đó, các logic nghiệp vụ (quản lý thời gian, tính toán qua nửa đêm, trạng thái phiên, cooldown, bộ lọc package) sẽ được thiết kế với chuẩn Unit Test để chạy độc lập và code Android được cấu trúc chuẩn Gradle Wrapper 8.7+ tương thích 100% Android Studio.
2. **Kế hoạch thử nghiệm thiết bị thật:**
   - Cài đặt APK lên thiết bị Android 10, 12, 13, 14.
   - Thử nghiệm chọn app, bật chế độ 5 phút.
   - Mở Facebook/Zalo -> xác nhận màn hình can thiệp xuất hiện trong vòng 150ms.
   - Bấm nút "Quay lại nào" -> kiểm tra thoát về Home.
   - Đổi giờ hệ thống qua mốc kết thúc -> xác nhận phiên tự kết thúc.
   - Mở app ngân hàng (VCB/Techcombank) -> ghi nhận cảnh báo nếu có.
