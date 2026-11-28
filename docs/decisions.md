# Các quyết định của nhóm

Khi ai đó hỏi "sao lại làm vậy?" thì mở file này. Các dòng dưới đây là những quyết định **đang được áp dụng trong code và trong `api.md`**. Mỗi người tick vào cột cuối khi đã đồng ý. Muốn đổi thì họp rồi sửa file này cùng lúc với sửa code.

| # | Quyết định | Lý do | Nhóm đồng ý |
|---|---|---|---|
| 1 | Đăng nhập bằng **email + mật khẩu** | Figma dùng email; Supabase Auth hỗ trợ sẵn | ☐ |
| 2 | `username` là **handle công khai tự sinh** từ email, không công khai email | Bảo vệ quyền riêng tư | ☐ |
| 3 | **Bài Found bắt buộc 1–3 câu hỏi xác minh; bài Lost tùy chọn 0–3 câu**. Chủ bài **luôn** phải duyệt trước khi mở chat | Bài Lost không bắt người đang mất đồ phải nghĩ câu hỏi, nhưng không ai nhắn thẳng được cho họ. Nhóm đã chọn phương án này (24/10/2026) | ✅ |
| 4 | **Người đăng bài** luôn là người đặt câu hỏi và chấm Pass/Fail | Một luồng dùng chung cho cả Lost và Found | ☐ |
| 5 | Mỗi người **tối đa 2 lượt** trên một bài (bị Fail hoặc tự hủy đều tính là 1 lượt) | Chống dò đáp án, chống gửi yêu cầu liên tục | ☐ |
| 6 | Bài tự **hết hạn sau 30 ngày** | Theo đề bài | ☐ |
| 7 | Số điện thoại **không bắt buộc khi đăng ký**, nhập ở Chỉnh sửa hồ sơ, lưu riêng tư | Figma không có ô SĐT ở màn Đăng ký | ☐ |
| 8 | **Đọc thẳng Supabase, ghi nghiệp vụ qua FastAPI** (ngoại lệ: tin nhắn và upload ảnh) | Bảo mật + ít code | ☐ |
| 9 | Backend truy cập database bằng **`psycopg` và SQL trực tiếp**, không dùng ORM | Schema đã có sẵn bằng SQL kèm trigger; ORM sẽ phải mô tả lại lần nữa | ☐ |
| 10 | Endpoint cần quyền **tra bảng `profiles`** mỗi request (tồn tại, chưa bị khóa) | Token vẫn dùng được tối đa 1 giờ sau khi bị xóa hoặc khóa | ☐ |
| 11 | Git: `main` luôn chạy được, làm việc ở `develop`, mỗi việc một nhánh `feat/<vai>-...`, gộp bằng Pull Request | Tránh ghi đè code của nhau | ☐ |
| 12 | Render triển khai từ nhánh **`develop`** trong lúc phát triển, đổi sang `main` gần ngày demo | B và C cần bản mới nhất | ☐ |
| 13 | Họp ngắn mỗi ngày, buổi tích hợp vào ngày 7, 14, 21, 28 | Theo kế hoạch 30 ngày | ☐ |

## Phân công

### Realtime Android khi màn hình đang mở

Nhánh `feat/a-android-realtime` dùng WebSocket của OkHttp hiện có và protocol
Supabase Realtime v1, tránh thêm SDK chỉ để nhận tín hiệu thay đổi. Event không
trở thành bản ghi UI: ứng dụng đọc lại qua REST dưới RLS, bao gồm đọc bù sau khi
nối lại và định kỳ 30 giây. Subscription theo màn hình RESUMED, JWT hiện tại và
ID thật; đổi phiên hoặc rời màn thì đóng kết nối. Việc này giảm polling chat từ
5 giây, không cung cấp push khi app đóng. Heartbeat, timeout, token refresh và
reconnect là trách nhiệm client; phải giữ test giao thức và kiểm tra Storage/
Realtime thật riêng với test HTTP giả lập. Trạng thái merge/thiết bị ở handoff/PR.

PR #49 (đã merge, `a3b5769`) bổ sung theo dõi yêu cầu riêng của claimant và inbox. Inbox
chưa biết ID hội thoại mới nên nghe `messages INSERT`/`claims UPDATE` không có
filter ID nhưng vẫn qua JWT và RLS hiện có; không mở quyền đọc công khai. Chỉ hai
cặp này được bỏ filter, mỗi cặp một đăng ký. Client chỉ đọc lại REST, không dùng
payload làm bản ghi. Kiểm tra điều kiện thử lại yêu cầu luôn qua GET questions:
CLOSED có thể kiểm tra lại nếu bài được mở lại, không tự đồng nhất CLOSED với
Fail/Cancel hoặc tự cấp thêm lượt. Không đổi mô hình trạng thái hay ngân sách thử.

### Khôi phục bản nháp xác minh và báo cáo

PR #51 giữ câu trả lời xác minh qua saved state, tách theo tài khoản, bài đăng và
bộ câu hỏi. PR #60 dùng SavedStateHandle cho form báo cáo, với ViewModel tách theo
tài khoản, loại đối tượng và ID. Lý do đã khôi phục được ưu tiên hơn lý do gợi ý
từ route; ViewModel của đối tượng mới vẫn được khởi tạo riêng.

Khôi phục form báo cáo không tự gửi nghiệp vụ hoặc giữ trạng thái đang gửi.
Request chưa có kết quả chắc chắn hiện lời nhắc kiểm tra Báo cáo đã gửi; phản hồi
thành công đã nhận giữ đúng mã. Đây là trạng thái UI, không thêm trạng thái báo
cáo hay idempotency server. Không cam kết giữ draft sau force-stop/xóa dữ liệu.
Phân biệt kiểm tra saved state với ViewModel mới, khôi phục task sau khi dừng
tiến trình nền và áp lực bộ nhớ tự nhiên; kết quả từng loại ở [readiness](readiness.md).

### Ảnh chat riêng tư và viewer admin

Ảnh tin nhắn dùng bucket `chat-images` riêng tư, path theo conversation và người
gửi; không dùng bucket ảnh bài đăng công khai. Thành viên upload/đọc theo RLS;
admin không thuộc chat chỉ lấy link tạm qua FastAPI sau khi có report trực tiếp.
Mỗi lần cấp link ghi `VIEW_CONVERSATION`, không bổ sung quyền đọc chat từ report
POST/USER. Đây là contract bổ sung cho `messages.image_path` hiện có, không đổi
luồng xác minh hay hoàn tất. Chi tiết path, TTL và SQL patch ở `docs/api.md`.

Ngày 22/11/2026, patch Storage đã được duyệt riêng và áp dụng live; kiểm tra hai
thành viên đọc được ảnh QA, người ngoài/admin không tham gia/ẩn danh/public URL
bị chặn. Android dùng tải có JWT và tắt cache ảnh trong Coil, không cần cấp link
admin cho thành viên. Không thêm quyền DELETE để cleanup từ client; upload có
thể mồ côi khi gửi tin thất bại và cần xử lý phía server riêng. Việc Storage đã
đạt không xác nhận viewer admin hosted hoặc APK Android đã được phát hành.

### Đường đọc danh sách yêu cầu cho chủ bài (07/11/2026)

Bổ sung `GET /posts/{post_id}/claims` làm đường đọc FastAPI cho màn hình duyệt
Android: gom hồ sơ công khai, trạng thái và câu trả lời, kiểm tra chủ bài và
phân trang tại server. Đây là ngoại lệ bổ sung cho nguyên tắc đọc Supabase
ở quyết định 8; đường đọc RLS hiện có vẫn giữ nguyên. Dùng chung cho Lost và
Found theo quyết định 3–4; không thay đổi luồng PASS/FAIL, schema hay RLS.

- A (Backend + Database): 
- B (Android lõi): 
- C (Android luồng chính + admin web): 
