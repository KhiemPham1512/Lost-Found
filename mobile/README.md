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

Mọi lời gọi lấy token bằng `SessionStore.requireToken()`. Token sắp hết hạn sẽ được
tự làm mới; các request cùng phiên dùng chung một lần refresh. `4xx` của phiên
hiện tại xóa phiên, còn lỗi mạng/`5xx` giữ phiên để thử lại. Refresh đang chạy hoặc
request đang chờ không được dùng hay thay đổi phiên mới sau logout/đổi tài khoản.
File chứa token được loại khỏi cloud backup và chuyển dữ liệu thiết bị; người dùng
cần đăng nhập lại khi chuyển máy. Quy tắc không xóa các bản backup đã tồn tại.

## Chi tiết bài và xác minh

Chi tiết đọc bài, ảnh, tag, danh mục và hồ sơ công khai qua Supabase/RLS. Màn xác
minh tải lại đúng bài theo `postId` cùng câu hỏi từ `GET /posts/{post_id}/questions`;
không dùng tiêu đề, ảnh, tác giả hay số bài đăng mẫu. Khi không có ảnh/mô tả, app
hiện trạng thái trống. Ảnh thật có nút trước/sau và vuốt để xem; lỗi tải ảnh có thông báo.

Chủ bài thấy **Xem yêu cầu**, người khác đi vào luồng xác minh như trước. Bài Lost
không có câu hỏi vẫn dùng **Gửi yêu cầu liên hệ**, không tự mở chat. Menu báo cáo
giữ ID bài/người đăng thực tế. Phân quyền và trạng thái hợp lệ do server kiểm tra.
Hội thoại, trao trả và thông báo của C vẫn chưa nối đủ dữ liệu thật.

Bản sửa chi tiết/xác minh đã merge qua PR #42 (`47d53f9`). Build/cài APK và kiểm
chứng Android được ghi riêng với trạng thái triển khai API/web; không có SQL hay
endpoint mới trong thay đổi này.

## Duyệt yêu cầu (nhánh feature, chưa merge)

Chủ bài mở yêu cầu từ bài cụ thể; lối tắt trang chủ dẫn tới bài của mình để chọn
bài. Danh sách dùng `GET /posts/{post_id}/claims`, phân trang 20 mục với **Xem thêm**,
hiện hồ sơ công khai, thời điểm, trạng thái và câu trả lời thật. Lost không có câu hỏi
hiện yêu cầu liên hệ. Màn hình có tải lại, trạng thái rỗng và lỗi có thử lại.

Chỉ yêu cầu `SUBMITTED` của bài `ACTIVE` có nút chấp nhận/từ chối; server vẫn quyết
định quyền và trạng thái hợp lệ. Cả hai có xác nhận, chặn thao tác khi đang gửi và
tải lại danh sách sau kết quả. Lỗi không tự gửi lại quyết định; cần kiểm tra trạng thái
trước khi thử lại vì request có thể đã được lưu. Không mở hội thoại mẫu sau khi duyệt;
giao diện chat thật là phần tiếp theo. Không thêm endpoint, SQL hay quyền mới.
