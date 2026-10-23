# Các quyết định của nhóm

Khi ai đó hỏi "sao lại làm vậy?" thì mở file này. Các dòng dưới đây là những quyết định **đang được áp dụng trong code và trong `api.md`**. Mỗi người tick vào cột cuối khi đã đồng ý. Muốn đổi thì họp rồi sửa file này cùng lúc với sửa code.

| # | Quyết định | Lý do | Nhóm đồng ý |
|---|---|---|---|
| 1 | Đăng nhập bằng **email + mật khẩu** | Figma dùng email; Supabase Auth hỗ trợ sẵn | ☐ |
| 2 | `username` là **handle công khai tự sinh** từ email, không công khai email | Bảo vệ quyền riêng tư | ☐ |
| 3 | **Cả bài Lost và Found** đều có 1–3 câu hỏi xác minh | Figma mới có câu hỏi ở bài Found, nhưng bài Lost cũng cần cơ chế tương tự | ☐ |
| 4 | **Người đăng bài** luôn là người đặt câu hỏi và chấm Pass/Fail | Một luồng dùng chung cho cả Lost và Found | ☐ |
| 5 | Mỗi người **tối đa 2 lần bị Fail** trên một bài | Chống dò đáp án | ☐ |
| 6 | Bài tự **hết hạn sau 30 ngày** | Theo đề bài | ☐ |
| 7 | Số điện thoại **không bắt buộc khi đăng ký**, nhập ở Chỉnh sửa hồ sơ, lưu riêng tư | Figma không có ô SĐT ở màn Đăng ký | ☐ |
| 8 | **Đọc thẳng Supabase, ghi nghiệp vụ qua FastAPI** (ngoại lệ: tin nhắn và upload ảnh) | Bảo mật + ít code | ☐ |
| 9 | Backend truy cập database bằng **`psycopg` và SQL trực tiếp**, không dùng ORM | Schema đã có sẵn bằng SQL kèm trigger; ORM sẽ phải mô tả lại lần nữa | ☐ |
| 10 | Endpoint cần quyền **tra bảng `profiles`** mỗi request (tồn tại, chưa bị khóa) | Token vẫn dùng được tối đa 1 giờ sau khi bị xóa hoặc khóa | ☐ |
| 11 | Git: `main` luôn chạy được, làm việc ở `develop`, mỗi việc một nhánh `feat/<vai>-...`, gộp bằng Pull Request | Tránh ghi đè code của nhau | ☐ |
| 12 | Render triển khai từ nhánh **`develop`** trong lúc phát triển, đổi sang `main` gần ngày demo | B và C cần bản mới nhất | ☐ |
| 13 | Họp ngắn mỗi ngày, buổi tích hợp vào ngày 7, 14, 21, 28 | Theo kế hoạch 30 ngày | ☐ |

## Phân công
- A (Backend + Database): 
- B (Android lõi): 
- C (Android luồng chính + admin web): 
