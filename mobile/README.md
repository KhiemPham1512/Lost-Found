# Lost & Found: ứng dụng Android

Kotlin + Jetpack Compose, minSdk 26. Mở thư mục `mobile/` bằng Android Studio.

## Cấu hình lần đầu

Tạo hoặc sửa file `mobile/local.properties` (file này **không** được commit):

```properties
sdk.dir=...                      # Android Studio tự điền
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
# Không bắt buộc. Mặc định dùng backend trên Render.
# Chạy backend trên máy tính và dùng máy ảo Android: http://10.0.2.2:8000
# API_BASE_URL=https://lost-found-api-ph8v.onrender.com
```

Chỉ đặt **publishable key** trong app. Secret key chỉ nằm ở backend.

Lần gọi API đầu tiên có thể mất tới khoảng 50 giây vì Render gói miễn phí "ngủ" khi không có truy cập.

## Quên mật khẩu bằng mã

Mục Quên mật khẩu gửi một **mã số** qua email. Muốn mục này chạy, A cần làm hai việc trong Supabase Dashboard:

1. **Authentication → Email Templates → Reset Password**: thêm `{{ .Token }}` vào nội dung email để người dùng nhận được mã.
2. Cấu hình **SMTP riêng** (Resend hoặc Gmail). Dịch vụ email mặc định của Supabase giới hạn số email gửi đi rất thấp.

## Cấu trúc mã (phần B: Android lõi)

```
com.lostfound.app
├── LostFoundApp.kt        # nạp phiên đăng nhập khi mở app
├── MainActivity.kt        # chỉ gọi AppNavHost
├── core/                  # B sở hữu
│   ├── AppConfig.kt       # đọc khóa từ BuildConfig
│   ├── session/           # lưu phiên + tự làm mới token
│   ├── network/           # OkHttp, dịch lỗi sang tiếng Việt
│   ├── supabase/          # Auth, PostgREST (đọc), Storage (upload ảnh)
│   ├── api/               # gọi FastAPI (docs/api.md)
│   ├── location/          # GPS (Fused Location)
│   ├── image/             # nén ảnh 1280 px, JPEG 80%
│   └── util/              # định dạng ngày giờ, khoảng cách, kiểm tra form
├── data/
│   ├── model/             # Post, Profile, Category, báo cáo...
│   └── repository/        # AuthRepository, PostRepository, ProfileRepository, ReportRepository
└── ui/
    ├── navigation/        # Routes + AppNavHost (CHỈ B sửa)
    ├── components/        # thẻ bài, trạng thái rỗng/đang tải/lỗi, ô nhập...
    ├── feed/              # phân trang dùng chung cho Home và Tìm kiếm
    ├── auth/              # Splash, Đăng nhập, Đăng ký, Quên/Đổi mật khẩu
    ├── home/  search/  post/  myposts/  profile/  report/
    └── theme/             # màu và chữ theo Figma
```

Các màn của C (Chi tiết, Xác minh, Duyệt, Chat, Trao trả, Thông báo) vẫn nằm ở gói gốc và được gắn vào `AppNavHost`.

**C muốn thêm màn hình:** gửi tên route cho B để B thêm vào `Routes.kt` và `AppNavHost.kt`.

## Quy ước gọi dữ liệu

| Việc | Đi đường nào |
|---|---|
| Bảng tin, tìm kiếm (`search_posts`), Cần xử lý (`my_todo_counts`), danh mục, bài của tôi, hồ sơ, báo cáo đã gửi | Supabase (RLS) qua `SupabaseRest` |
| Upload ảnh | Supabase Storage, đường dẫn `<user_id>/<uuid>.jpg` |
| Sửa tên hiển thị, giới thiệu, ảnh đại diện | Supabase (chỉ các cột được phép ghi) |
| Đăng ký, tạo bài, ẩn/hiện/xóa bài, số điện thoại, báo cáo | FastAPI qua `BackendApi` |
| Đăng nhập, làm mới token, quên/đổi mật khẩu | Supabase Auth qua `SupabaseAuth` |

Mọi lời gọi lấy token bằng `SessionStore.requireToken()`. Token sắp hết hạn sẽ được tự làm mới. Nếu không làm mới được, app tự quay về màn Đăng nhập.
