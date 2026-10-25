# Lost & Found: Tài liệu API (hợp đồng giữa Backend và Android / Admin web)

> **Người viết:** A (Backend). **Người đọc:** B, C.
> **Quy tắc:** chốt vào ngày 4. Muốn đổi API thì **báo cả nhóm trước khi sửa**, rồi cập nhật file này trong cùng Pull Request.

- Địa chỉ backend: `https://lost-found-api-ph8v.onrender.com`
- Trang thử API (Swagger): `https://lost-found-api-ph8v.onrender.com/docs`
- ⚠ Gói miễn phí của Render tự tắt sau 15 phút không có truy cập. **Lần gọi đầu tiên có thể mất 30–50 giây.** App cần có thời gian chờ (timeout) đủ dài ở lần gọi đầu và hiện vòng quay tải.

---

## 1. Hai đường lấy dữ liệu

| Việc | Đi đường nào |
|---|---|
| Đọc dữ liệu (bảng tin, chi tiết bài, tin nhắn, thông báo...) | **App → Supabase** trực tiếp (phần 4) |
| Gửi tin nhắn, đánh dấu đã đọc, upload ảnh, sửa tên/ảnh/giới thiệu | **App → Supabase** trực tiếp (phần 4) |
| Mọi thay đổi trạng thái nghiệp vụ (đăng ký, đăng bài, xác minh, DONE, báo cáo, admin) | **App → FastAPI** (phần 3) |

## 2. Quy ước chung

**Xác thực.** Mọi endpoint (trừ `/health` và `/auth/register`) cần header:
```
Authorization: Bearer <access_token>
```
`access_token` lấy được khi đăng nhập bằng Supabase Auth (thư viện `supabase-kt` tự giữ phiên và tự làm mới token).

**Về tuổi thọ của token:** token sống **1 giờ**. Backend kiểm tra chữ ký và hạn của token chứ không hỏi lại Supabase mỗi lần, nên token đã cấp vẫn dùng được đến khi hết hạn dù người dùng đã bị xóa. Vì vậy, từ các endpoint cần quyền (từ `PUT /me/phone` trở đi), backend **tra thêm bảng `profiles`**: hồ sơ không tồn tại hoặc tài khoản bị khóa thì trả `403` ngay.

**Định dạng.** JSON, mã hóa UTF-8. ID là UUID dạng chuỗi. Thời gian theo **ISO 8601 có múi giờ**, ví dụ `2026-10-23T08:30:00+07:00`.

**Mã trạng thái HTTP:**

| Mã | Ý nghĩa | App nên làm |
|---|---|---|
| 200 / 201 | Thành công | |
| 400 | Yêu cầu sai luật nghiệp vụ | Hiện `detail` cho người dùng |
| 401 | Thiếu hoặc sai token | Đưa về màn đăng nhập |
| 403 | Không có quyền (hoặc tài khoản bị khóa) | Hiện thông báo |
| 404 | Không tìm thấy | Hiện thông báo |
| 409 | Xung đột trạng thái (đã gửi rồi, bài không còn hoạt động...) | Hiện `detail`, tải lại dữ liệu |
| 422 | Dữ liệu gửi lên không hợp lệ | Hiện lỗi cạnh ô nhập |
| 429 | Gọi quá nhiều | Báo "Thử lại sau ít phút" |
| 5xx | Lỗi máy chủ | Báo "Có lỗi, thử lại sau" |

**Định dạng lỗi:**
```json
{ "detail": "Email này đã được đăng ký" }
```
Riêng lỗi 422 do FastAPI sinh ra có dạng danh sách:
```json
{ "detail": [ { "loc": ["body", "password"], "msg": "Mật khẩu phải có cả chữ và số", "type": "value_error" } ] }
```

**Trạng thái làm việc của từng endpoint:** ✅ đã chạy trên Render · 🚧 đang làm (ngày dự kiến ghi bên cạnh).

---

## 3. Endpoint do FastAPI cung cấp

### 3.1 Hệ thống

| Endpoint | Auth | Mô tả | Trạng thái |
|---|---|---|---|
| `GET /health` | Không | Kiểm tra server sống. Trả `{"status":"ok"}` | ✅ |
| `GET /me` | Có | Trả `{"user_id": "<uuid>"}` của người đang đăng nhập. Dùng để thử token | ✅ |

### 3.2 Tài khoản

#### `POST /auth/register` ✅
Không cần token.
```json
// Request
{ "email": "abc@gmail.com", "password": "Passw0rdOK", "display_name": "Thu Lan" }
```
- `email`: đúng định dạng email.
- `password`: 8–72 ký tự, **có cả chữ và số**.
- `display_name`: tùy chọn, 1–50 ký tự.

```json
// 201 Created
{ "user_id": "374f3d39-8821-43a2-82df-9df558b072bf", "email": "abc@gmail.com", "username": "abc" }
```
`username` là **handle công khai** do hệ thống tự sinh từ email (ví dụ `abc`, hoặc `abc4821` nếu trùng). Người khác thấy handle, **không thấy email**.

Lỗi: `409` email đã đăng ký · `400` mật khẩu quá yếu · `422` dữ liệu sai.

Sau khi đăng ký thành công, app **tự đăng nhập** bằng email + mật khẩu qua Supabase Auth.

#### `PUT /me/phone` ✅
Lưu số điện thoại (không công khai, chỉ chính chủ và admin xem được).
```json
// Request
{ "phone": "0901234567" }
```
Định dạng số Việt Nam: bắt đầu bằng `0` hoặc `+84`, theo sau 9–10 chữ số. Server tự bỏ khoảng trắng, dấu chấm, dấu gạch ngang (`0901 234 567` được chấp nhận). Gọi lại để **đổi số** thì số cũ bị thay thế.
```json
// 200 OK
{ "phone_saved": true }
```
Lỗi: `422` sai định dạng · `403` tài khoản bị khóa hoặc chưa có hồ sơ · `503` backend chưa cấu hình database.

### 3.3 Bài đăng

#### `POST /posts` ✅
Tạo bài Lost hoặc Found. **App upload ảnh lên Supabase Storage trước** (phần 4.5), rồi gửi danh sách đường dẫn ảnh tại đây.
```json
// Request
{
  "type": "FOUND",
  "title": "Nhặt được ví da màu nâu",
  "description": "Ở căn tin trường, không ghi chi tiết nhận dạng",
  "category_id": 1,
  "location_text": "Căn tin UTH, Quận Bình Thạnh",
  "latitude": 10.8030,
  "longitude": 106.7140,
  "event_time": "2026-10-23T08:30:00+07:00",
  "tags": ["màu nâu", "da"],
  "image_paths": ["374f3d39-8821-43a2-82df-9df558b072bf/6f1c...e2.jpg"],
  "questions": ["Trong ví có bao nhiêu tờ tiền?", "Mặt trong ví có họa tiết gì?"]
}
```

| Trường | Luật |
|---|---|
| `type` | `LOST` hoặc `FOUND` |
| `title` | 3–120 ký tự |
| `description` | tối đa 2000 ký tự, có thể bỏ trống |
| `category_id` | lấy từ bảng `categories` (phần 4.3) |
| `location_text` | 2–200 ký tự |
| `latitude`, `longitude` | **cùng có hoặc cùng không có**. Cần để hiển thị khoảng cách và sắp xếp Gần nhất |
| `event_time` | thời điểm mất hoặc nhặt, không ở tương lai |
| `tags` | tối đa 10 tag, mỗi tag tối đa 30 ký tự. Server tự viết thường và cắt khoảng trắng |
| `image_paths` | 0–5 đường dẫn. **Mỗi đường dẫn phải bắt đầu bằng ID của chính bạn** |
| `questions` | **1–3 câu**, mỗi câu 3–300 ký tự. Bắt buộc cho cả bài Lost và Found |

```json
// 201 Created
{ "id": "9b1d...", "status": "ACTIVE", "expires_at": "2026-11-22T08:30:00+00:00" }
```
Quy tắc thêm:
- `image_paths`: mỗi đường dẫn phải đúng dạng `<id-của-bạn>/<tên>.jpg` (đuôi `jpg`, `jpeg`, `png` hoặc `webp`), **không trùng nhau**, và ảnh phải **đã được tải lên Storage trước đó**. Server kiểm tra từng ảnh có thật trong bucket.
- `event_time` phải có múi giờ và không ở tương lai (cho phép lệch 5 phút).
- Mỗi người **tối đa 10 bài trong 24 giờ**.
- Bài được tạo trong **một transaction**: có lỗi ở bất kỳ bước nào thì không có gì được lưu.

Lỗi: `400` đường dẫn ảnh sai, ảnh của người khác, hoặc ảnh chưa được tải lên · `422` sai luật ở bảng trên, hoặc `category_id` không tồn tại · `429` đăng quá 10 bài trong 24 giờ · `403` tài khoản bị khóa hoặc chưa có hồ sơ.

#### `PATCH /posts/{post_id}/status` 🚧 (ngày 6)
Chủ bài ẩn, hiện lại hoặc xóa bài của mình.
```json
{ "status": "ARCHIVED" }
```
| Giá trị | Ý nghĩa | Cho phép khi bài đang |
|---|---|---|
| `ARCHIVED` | Ẩn bài | `ACTIVE` |
| `ACTIVE` | Hiện lại | `ARCHIVED` (và chưa hết hạn) |
| `DELETED` | Xóa (xóa mềm) | `ACTIVE`, `ARCHIVED` |

```json
// 200 OK
{ "id": "9b1d...", "status": "ARCHIVED" }
```
Lỗi: `403` không phải chủ bài · `404` · `409` trạng thái hiện tại không cho phép chuyển (ví dụ bài đã `RESOLVED`).

### 3.4 Xác minh quyền sở hữu

> Người **đăng bài** luôn là người đặt câu hỏi và chấm Pass/Fail. Người còn lại (gọi là *claimant*) là người trả lời.
> Bài **Found**: claimant là người nhận mình là chủ đồ. Bài **Lost**: claimant là người nói đã nhặt được đồ.

#### `GET /posts/{post_id}/questions` ✅
Claimant lấy câu hỏi để trả lời. **Câu hỏi không đọc trực tiếp từ database** để backend đếm được số lần thử.
```json
// 200 OK
{
  "post_id": "9b1d...",
  "questions": [
    { "id": "a1...", "position": 1, "question_text": "Trong ví có bao nhiêu tờ tiền?" },
    { "id": "a2...", "position": 2, "question_text": "Mặt trong ví có họa tiết gì?" }
  ],
  "attempts_left": 2
}
```
Lỗi: `403` đây là bài của bạn · `404` không có bài · `409` bài không còn hoạt động, hoặc bạn đã có yêu cầu đang xử lý · `429` đã hết lượt.

#### `POST /posts/{post_id}/claims` ✅
Nộp câu trả lời. **Phải trả lời đủ mọi câu hỏi.**
```json
// Request
{
  "answers": [
    { "question_id": "a1...", "answer_text": "3 tờ 500 nghìn" },
    { "question_id": "a2...", "answer_text": "Họa tiết hoa văn màu vàng" }
  ]
}
```
`answer_text`: 1–500 ký tự.
```json
// 201 Created
{ "claim_id": "c1...", "status": "SUBMITTED" }
```
Quy tắc: mỗi người chỉ có **1 yêu cầu đang xử lý** trên mỗi bài; **tối đa 2 lượt** trên cùng một bài (mỗi lần bị Fail hoặc tự hủy tính là 1 lượt). Phải trả lời **đúng và đủ** mọi câu hỏi: không thiếu, không thừa, không trùng.
Lỗi: `403` bài của chính bạn · `404` không có bài · `409` bạn đã có yêu cầu đang xử lý, hoặc bài không còn hoạt động · `429` hết lượt · `422` thiếu, thừa hoặc trùng câu trả lời, hay câu trả lời rỗng.

Hệ thống **tự gửi thông báo** cho chủ bài.

#### `POST /claims/{claim_id}/decision` ✅
**Chỉ chủ bài** được gọi. Yêu cầu phải đang ở trạng thái `SUBMITTED`.
```json
// Request
{ "decision": "PASS", "note": null }
```
`decision`: `PASS` hoặc `FAIL`. `note`: tùy chọn, tối đa 500 ký tự.
```json
// 200 OK
{ "claim_id": "c1...", "status": "PASSED", "conversation_id": "d1..." }
```
Khi `FAIL` thì `status` là `FAILED`, `conversation_id` là `null`, và claimant nhận thông báo. Lượt thử của claimant giảm 1.
Khi `PASS`, **database tự tạo cuộc trò chuyện** kèm một tin nhắn hệ thống, và tự gửi thông báo cho claimant.
Lỗi: `403` bạn không phải chủ bài · `404` không có yêu cầu này · `409` yêu cầu đã được xử lý rồi, hoặc bài không còn hoạt động · `422` `decision` không phải `PASS` hoặc `FAIL`.

#### `POST /claims/{claim_id}/cancel` ✅
Claimant hủy yêu cầu của mình (khi `SUBMITTED` hoặc `PASSED`). Không cần body.
```json
// 200 OK
{ "claim_id": "c1...", "status": "CANCELLED" }
```
Nếu yêu cầu đã `PASSED` thì cuộc trò chuyện bị **khóa** (chỉ còn đọc). Lần hủy **tính là 1 lượt thử** của claimant trên bài đó.
Lỗi: `403` bạn không phải người gửi yêu cầu · `404` · `409` yêu cầu đã kết thúc (`FAILED`, `CANCELLED`, `COMPLETED`, `CLOSED`).

### 3.5 Trao trả và DONE

#### `POST /claims/{claim_id}/confirm` 🚧 (ngày 10)
Mỗi bên bấm xác nhận của mình. Không cần body. Backend tự biết bạn là chủ bài hay claimant qua token. **Gọi nhiều lần không gây lỗi.** Yêu cầu phải đang ở trạng thái `PASSED`.
```json
// 200 OK
{
  "claim_id": "c1...",
  "status": "PASSED",
  "author_confirmed_at": "2026-10-23T09:00:00+00:00",
  "claimant_confirmed_at": null
}
```
Khi **cả hai bên đã xác nhận**, `status` trả về là `COMPLETED`. Lúc đó database tự: chuyển bài sang `RESOLVED`, đóng các yêu cầu khác của bài, khóa cuộc trò chuyện, và gửi thông báo cho cả hai. Gọi lại sau khi đã `COMPLETED` vẫn trả `200` với trạng thái hiện tại.
Lỗi: `403` bạn không thuộc yêu cầu này · `404` · `409` yêu cầu chưa ở bước trao trả (còn `SUBMITTED`) hoặc đã kết thúc (`FAILED`, `CANCELLED`, `CLOSED`), hoặc bài không còn hoạt động.

### 3.6 Báo cáo

#### `POST /reports` 🚧 (ngày 11)
```json
// Request
{
  "target_type": "POST",
  "target_id": "9b1d...",
  "reason": "SCAM",
  "description": "Người này đòi chuyển tiền trước"
}
```
| Trường | Giá trị |
|---|---|
| `target_type` | `POST`, `USER` hoặc `CONVERSATION` |
| `reason` | `SPAM`, `SCAM`, `FALSE_INFO`, `MONEY_REQUEST`, `VIOLATION`, `OTHER` |
| `description` | tối đa 1000 ký tự, có thể bỏ trống |

```json
// 201 Created
{ "report_id": "r1...", "code": "R-1042", "status": "PENDING" }
```
`code` là mã dễ đọc để hiển thị cho người dùng và admin.

Quy tắc:
- Không báo cáo **bài của chính mình** hoặc **chính mình**.
- Báo cáo `CONVERSATION` chỉ làm được với cuộc trò chuyện **bạn là một trong hai người**.
- Báo cáo mới về cùng đối tượng bị từ chối khi báo cáo cũ của bạn còn `PENDING` hoặc `REVIEWING`.
- Mỗi người **tối đa 10 báo cáo trong 24 giờ**.

Lỗi: `400` báo cáo chính mình hoặc bài của chính mình · `404` không tìm thấy đối tượng (hoặc cuộc trò chuyện không thuộc bạn) · `409` đã có báo cáo đang chờ về đối tượng này · `422` `reason` hoặc `target_type` không hợp lệ · `429` quá 10 báo cáo trong 24 giờ.

### 3.7 Quản trị (chỉ tài khoản `ADMIN`, dùng bởi Admin web)

Người không phải admin gọi các endpoint này nhận `403`. **Mọi hành động thay đổi đều được ghi vào nhật ký** `admin_actions`. Danh sách bên dưới bám theo 9 màn hình admin trong Figma (cập nhật 24/10/2026).

**Đọc dữ liệu (cần tính toán gộp nên đi qua backend):**

| Endpoint | Màn Figma | Trả về | Trạng thái |
|---|---|---|---|
| `GET /admin/stats` | Tổng quan | Số báo cáo chờ xử lý (và chênh so với hôm qua), số bài đang hiển thị (tách Lost, Found), số người dùng (và số mới trong tuần), số bài DONE trong tuần và tỷ lệ trao trả, số bài mỗi ngày trong 7 ngày (Lost, Found), 5 báo cáo mới nhất | 🚧 |
| `GET /admin/reports?status=&type=&q=&limit=&offset=` | Báo cáo | Danh sách: mã `R-<report_no>`, loại (bài, tài khoản, hội thoại), đối tượng, lý do, handle người báo cáo, thời gian, trạng thái. `status`: `PENDING`, `REVIEWING`, `RESOLVED`, `DISMISSED` | 🚧 |
| `GET /admin/reports/{id}` | Chi tiết báo cáo | Báo cáo, bài hoặc tài khoản bị báo cáo (kèm số bài và số lần bị báo cáo của người đăng), **mọi báo cáo khác về cùng đối tượng**, tóm tắt hội thoại liên quan (số tin, tin cuối), nhật ký xử lý | 🚧 |
| `GET /admin/posts?type=&status=&q=&limit=&offset=` | Bài đăng | Danh sách kèm **số báo cáo** của từng bài | 🚧 |
| `GET /admin/users?status=&q=&limit=&offset=` | Người dùng | Handle, **số điện thoại đã che** (`09•• ••• 321`), vai trò, tháng tham gia, số bài, số lần bị báo cáo, trạng thái. `status`: `ACTIVE`, `BANNED`, `REPORTED` (bị báo cáo nhiều) | 🚧 |
| `GET /admin/actions?target_type=&range=&q=` | Nhật ký hoạt động | Thời gian, admin, hành động, đối tượng, mã báo cáo, ghi chú. `range`: `today`, `7d`, `30d` | 🚧 |
| `GET /admin/conversations/{id}/messages?limit=100&before_id=` | Hội thoại bị báo cáo | Tin nhắn của cuộc chat **bị báo cáo**. Mỗi lần gọi **luôn ghi log** `VIEW_CONVERSATION` | 🚧 |

Số điện thoại **đầy đủ không bao giờ được trả về** cho admin web; backend chỉ gửi bản đã che.

**Thay đổi (đều ghi nhật ký):**

| Endpoint | Body | Tác dụng | Trạng thái |
|---|---|---|---|
| `POST /admin/reports/{id}/review` | không | Đánh dấu báo cáo **đang xem xét** (`REVIEWING`). Log `REVIEW_REPORT` | 🚧 |
| `POST /admin/reports/{id}/resolve` | `{"action": "...", "note": "..."}` | Xử lý báo cáo. `action`: `REMOVE_POST` (gỡ bài), `BAN_USER` (khóa tài khoản người bị báo cáo), `KEEP` (đã xử lý, giữ nguyên), `DISMISS` (không vi phạm, trạng thái `DISMISSED`). **`note` bắt buộc**. Người báo cáo nhận thông báo `REPORT_UPDATED` | 🚧 |
| `POST /admin/posts/{id}/remove` | `{"reason": "..."}` | Gỡ bài (`REMOVED`), đóng các yêu cầu đang mở, **thông báo cho chủ bài kèm lý do** (`POST_REMOVED`) | 🚧 |
| `POST /admin/posts/{id}/restore` | không | Khôi phục bài đã gỡ | 🚧 |
| `POST /admin/users/{id}/ban` | `{"reason": "..."}` | Khóa tài khoản: đặt `is_banned`, chặn đăng nhập, gỡ các bài đang hoạt động của họ | 🚧 |
| `POST /admin/users/{id}/unban` | `{"reason": "..."}` | Mở khóa | 🚧 |
| `POST /admin/conversations/{id}/lock` | `{"reason": "..."}` | Khóa hội thoại: hai bên chỉ còn đọc. Log `LOCK_CONVERSATION` | 🚧 |

Ví dụ phản hồi của `GET /admin/conversations/{id}/messages`:
```json
{
  "conversation_id": "d1...",
  "messages": [
    { "id": 101, "sender_id": "374f...", "type": "TEXT", "content": "Chào bạn", "created_at": "2026-10-23T09:05:00+00:00" }
  ]
}
```

---

## 4. Những gì app lấy trực tiếp từ Supabase (không qua FastAPI)

Dùng thư viện `supabase-kt` (Android) hoặc `supabase-js` (Admin web) với **publishable key**. Quyền truy cập do RLS trong database quyết định nên người dùng chỉ thấy được dữ liệu của mình.

### 4.1 Bảng tin và tìm kiếm: hàm `search_posts`
Gọi bằng RPC. Trả về danh sách bài đang `ACTIVE`.

| Tham số | Kiểu | Ý nghĩa |
|---|---|---|
| `p_type` | `LOST` / `FOUND` / bỏ trống | Tab Lost, Found, Tất cả |
| `p_category` | số | `categories.id` |
| `p_query` | chuỗi | Từ khóa. **Tìm được không dấu**: gõ `vi` ra `ví` |
| `p_posted_after` | thời gian | Bộ lọc thời gian đăng. "Hôm nay" = từ 00:00 hôm nay, "7 ngày" = từ 7 ngày trước |
| `p_tag_ids` | mảng số | Bài phải có **đủ** các tag đã chọn |
| `p_sort` | `NEWEST` / `NEAREST` | Mới nhất hoặc Gần nhất |
| `p_lat`, `p_lng` | số | Vị trí người dùng (bắt buộc khi `NEAREST`) |
| `p_limit` | số | Tối đa 50, mặc định 20 |
| `p_offset` | số | Vị trí bắt đầu (phân trang) |

Bài không có tọa độ tự xếp cuối khi sắp xếp `NEAREST`. Khoảng cách "1.2 km" trên thẻ do app **tự tính** từ GPS của máy tới tọa độ của bài.

### 4.2 Khối "Cần xử lý" trên Home: hàm `my_todo_counts()`
```json
{ "pending_verifications": 3, "unread_messages": 3, "awaiting_confirmation": 3 }
```

### 4.3 Các bảng đọc được
| Bảng | Ghi chú |
|---|---|
| `posts` | Bài `ACTIVE` của mọi người; bài của mình (mọi trạng thái trừ `DELETED`) |
| `post_images`, `post_tags`, `tags` | Ảnh và tag của bài |
| `categories` | Danh mục (Ví, Điện thoại, Giấy tờ...) |
| `profiles` | Handle, tên hiển thị, ảnh, giới thiệu của mọi người. **Không có email, không có số điện thoại** |
| `claims`, `claim_answers` | Chỉ chủ bài và claimant |
| `conversations`, `messages` | Chỉ hai người trong cuộc |
| `notifications` | Chỉ của chính mình |
| `reports` | Chỉ báo cáo mình đã gửi (admin xem hết) |

Bảng `verification_questions` chỉ **chủ bài** đọc trực tiếp được (để xem lại câu hỏi của chính bài mình). Người muốn trả lời xác minh phải dùng `GET /posts/{id}/questions`.

### 4.4 Những việc được ghi trực tiếp
| Việc | Cách làm |
|---|---|
| Gửi tin nhắn | Chèn vào `messages`: `conversation_id`, `sender_id` = ID của bạn, `type` = `TEXT` hoặc `IMAGE`, `content` (1–2000 ký tự). Bị chặn nếu cuộc trò chuyện đã khóa |
| Đánh dấu tin đã đọc | Cập nhật `messages.read_at` của tin do **người kia** gửi |
| Đánh dấu thông báo đã đọc ("Đọc hết") | Cập nhật `notifications.read_at` |
| Sửa hồ sơ | Cập nhật `profiles`: chỉ các cột `display_name`, `avatar_url`, `bio` (tối đa 160 ký tự) |

### 4.5 Upload ảnh
Bucket `post-images`. Đường dẫn bắt buộc: **`<user_id>/<uuid>.jpg`**. Thư mục đầu phải là ID của chính bạn, nếu không bị từ chối.
Khuyến nghị: nén ảnh, cạnh dài khoảng 1280 px, JPEG chất lượng khoảng 80%.

Địa chỉ hiển thị ảnh:
```
<SUPABASE_URL>/storage/v1/object/public/post-images/<đường dẫn>
```

### 4.6 Realtime
Đăng ký lắng nghe sự kiện thêm dòng (INSERT):
- `messages` lọc theo `conversation_id` (màn chat).
- `notifications` lọc theo `user_id` (chuông thông báo).
- `claims` (người trả lời biết ngay khi được Pass hoặc bị Fail).

---

## 5. Bảng giá trị (enum)

| Tên | Giá trị |
|---|---|
| `post.type` | `LOST`, `FOUND` |
| `post.status` | `ACTIVE`, `RESOLVED`, `EXPIRED`, `ARCHIVED`, `REMOVED`, `DELETED` |
| `claim.status` | `SUBMITTED`, `PASSED`, `FAILED`, `CANCELLED`, `COMPLETED`, `CLOSED` |
| `message.type` | `TEXT`, `IMAGE`, `SYSTEM` (tin hệ thống không có `sender_id`) |
| `report.reason` | `SPAM`, `SCAM`, `FALSE_INFO`, `MONEY_REQUEST`, `VIOLATION`, `OTHER` |
| `report.status` | `PENDING`, `REVIEWING`, `RESOLVED`, `DISMISSED` (mã hiển thị: `R-<report_no>`, ví dụ R-1042) |
| `notification.type` | `CLAIM_RECEIVED`, `CLAIM_PASSED`, `CLAIM_FAILED`, `HANDOVER_WAITING`, `POST_DONE`, `REPORT_UPDATED`, `POST_REMOVED`, `POST_EXPIRED`, `SYSTEM` |

## 6. Luồng chính theo thứ tự gọi

```
Người nhặt:   upload ảnh (Storage) → POST /posts
Người mất:    GET /posts/{id}/questions → POST /posts/{id}/claims
Người nhặt:   POST /claims/{id}/decision {PASS}   → chat tự được tạo
Hai bên:      chat (Supabase) → POST /claims/{id}/confirm (cả hai) → DONE
```
