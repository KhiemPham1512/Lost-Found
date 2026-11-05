# Lost & Found: Tài liệu API (hợp đồng giữa Backend và Android / Admin web)

> 🧪 = đã triển khai và kiểm thử cục bộ; chưa xác nhận phiên bản trên Render.
> Các mục ✅ đã có trước đợt bổ sung này giữ nguyên trạng thái được ghi nhận.

Admin-web có dashboard, bộ lọc danh sách, chi tiết và xử lý báo cáo, gỡ/khôi phục
bài, khóa/mở khóa tài khoản và xem/khóa hội thoại bị báo cáo trực tiếp. Các luồng
này đã được kiểm tra cục bộ. Tin nhắn ảnh chỉ
hiển thị loại tin nhắn, chưa mở tệp đính kèm. Với báo cáo hội thoại, khóa tài khoản
thành viên thực hiện từ danh sách người dùng.

**Bộ lọc loại theo Figma (local, 05/11/2026):** admin-web có nhóm chọn
Tất cả/Lost/Found cho bài đăng và Tất cả/Bài đăng/Tài khoản/Hội thoại cho báo cáo.
Các lựa chọn dùng `type` của `GET /admin/posts` và `GET /admin/reports` đã có,
kết hợp với `q` và `status`. Đổi loại đưa về trang đầu; chọn Tất cả bỏ `type`,
Xóa bộ lọc xóa cả loại/từ khóa/trạng thái và đặt lại phân trang. Users và Actions
không có nhóm loại này. Khi tải lại dữ liệu, vùng catalog có trạng thái bận;
lỗi/thử lại không làm mất loại đang chọn. Không đổi hợp đồng hoặc quyền API.
Hồi quy local đạt 28 test và build TypeScript/Vite (một probe chẩn đoán cũ
được bỏ qua), gồm kiểm tra bàn phím, nút ít nhất 44px và bố trí
375/768/1024/1440px. Auth/API được kiểm soát; bộ lọc mới chưa kiểm chứng trên live.

**Bổ sung catalog theo Figma (đã kiểm chứng live, 05/11/2026):** danh sách báo cáo hiển thị
thêm loại đối tượng, handle người báo cáo và thời gian gửi; bài đăng có danh mục
và ngày đăng; người dùng có tháng tham gia. Dùng các trường API hiện có, số điện
thoại vẫn được che. Ở viewport không quá 1180px, catalog dùng bản ghi dọc có
nhãn để tránh khuất cột; desktop lớn giữ bảng. Hồi quy UI cục bộ dùng Auth/API được
kiểm soát; bản triển khai đã được kiểm chứng riêng bên dưới. Không đổi endpoint,
tham số, quyền hoặc nghiệp vụ; đây chỉ là một phần đối chiếu chức năng Figma.
Hồi quy frontend ngày 05/11 đạt 23 test và build TypeScript/Vite; một probe
chẩn đoán cũ được bỏ qua. Nội dung dài ở cả ba catalog không tràn viewport hoặc
bị khuất cột trong bảng tại 375/768/1024/1440px; không lỗi console/page. Không
chạy lại backend vì vòng này không thay đổi code/hợp đồng phía server.

Sau khi PR #26 merge, Render báo frontend `a5fdb02` ở trạng thái `live`
(`dep-dblv6im7bikc73ec7rq0`, hoàn tất 03:09:16 UTC ngày 05/11). Smoke chỉ đọc
lúc 03:16:33–03:16:40 UTC dùng Auth/CORS thật: đăng nhập, tìm dữ liệu QA trên
ba catalog, kiểm tra cột mới và đối chiếu handle người báo cáo, danh mục,
tháng tham gia với response API, rồi đăng xuất. Giá trị thời gian gửi/ngày đăng
và loại đối tượng hiển thị được kiểm tra cục bộ; smoke live chưa tự động đối
chiếu các giá trị này. Tất cả
375/768/1024/1440px không tràn viewport hoặc bảng, không lỗi console/page;
request catalog đều `200`. Chặn toàn bộ API ghi nghiệp vụ, không thay đổi dữ liệu.
Chưa xác nhận toàn bộ chức năng Figma hoặc Android từ lần kiểm tra này.

Nâng cấp giao diện danh sách đã được kiểm tra trên bản live: xóa bộ lọc, hiển thị khoảng
kết quả, hàng dữ liệu dạng dọc trên điện thoại, bỏ qua điều hướng bằng bàn phím
và khôi phục focus sau khi hủy kiểm duyệt. Các cải tiến này giữ nguyên hợp đồng
API bên dưới. Frontend Render đã xác nhận commit `a5fdb02`, gồm PR #20–#26.

**Triển khai admin-web (05/11/2026):** https://lost-found-admin.onrender.com,
Render Static Site từ `develop`, commit `a5fdb02`. Backend đang chạy `37a0405`;
code backend và các SQL database không đổi từ commit đó đến `a5fdb02`
(các cập nhật tài liệu không yêu cầu triển khai lại backend).
Kiểm tra trực tiếp đã đạt: `/health`, chặn request chưa đăng nhập (`401`), CORS
preflight từ domain admin-web, đăng nhập admin, dashboard (`GET /admin/stats`),
danh sách báo cáo/bài đăng/người dùng/nhật ký, chi tiết báo cáo và đăng xuất.
Đã kiểm tra tìm kiếm không có kết quả, khoảng `0–0`, reset tìm kiếm/trạng thái,
skip link, focus tiêu đề và trả focus khi hủy dialog kiểm duyệt. Dashboard và
bốn catalog ở 375/768/1024/1440px không tràn ngang; không có lỗi console hoặc
JavaScript chưa xử lý. Dữ liệu live chưa đủ mở trang hai; nút Tiếp bị khóa đúng
ở trang cuối, còn chuyển trang được kiểm tra bằng UI mock cục bộ.

Hồi quy cục bộ gần nhất (04/11/2026): 77 test backend trên PostgreSQL test cô lập
và mypy đạt; 19 test Playwright với Auth/API được kiểm soát và build TypeScript/Vite đạt.
Đã bổ sung kiểm tra HTTP luồng Found → xác minh → hai bên xác nhận, xác nhận
lặp lại và giới hạn hai lượt Fail/hủy trên bài Lost không có câu hỏi. Đây là
kiểm chứng hành vi hiện có, không thay đổi code nghiệp vụ. Một test tích hợp
Playwright → FastAPI thật → PostgreSQL test cô lập kiểm tra gỡ/khôi phục bài,
khóa/mở khóa tài khoản, nhận/xử lý báo cáo và nhật ký. Chỉ Supabase Auth và danh
tính test được giả lập; không mock dữ liệu trả về của endpoint nghiệp vụ. Test
dùng origin loopback riêng, không thay thế kiểm chứng CORS live.

**QA với Auth thật (05/11/2026):** theo lựa chọn của người dùng, đã tạo hai tài
khoản QA mới và hai bài đánh dấu riêng trên project hiện tại. Kiểm tra HTTP/SDK
đạt đăng ký/đăng nhập, tạo Found, lấy câu hỏi qua API (đọc trực tiếp từ claimant
bị RLS ẩn), gửi/duyệt claim, nhận sự kiện claim và tin nhắn qua Realtime, thông
báo `CLAIM_RECEIVED` và `POST_DONE`, hai bên xác nhận và xác nhận lặp lại.
Sau DONE, bài `RESOLVED`, chat bị khóa và gửi tin trả đúng lỗi RLS `42501`.

Trên bài QA thứ hai, đã tạo báo cáo, nhận xem xét, xử lý `REMOVE_POST`, khôi
phục bài và kiểm tra nhật ký. Khóa tài khoản QA chặn đăng nhập Auth thật
(`user_banned`) và chặn JWT đã cấp qua API; mở khóa cho đăng nhập lại thành công.
Đọc lại xác nhận có `POST_REMOVED`/`REPORT_UPDATED`; người khác không đọc được
thông báo riêng. Admin không đọc thẳng được hội thoại qua RLS; API trả `403`
với chat chưa bị báo cáo. Chỉ các endpoint đã có bằng chứng thành công được
cập nhật ✅; dấu này không có nghĩa mọi biến thể đều đã kiểm tra trên live.

Hai tài khoản QA và dữ liệu được giữ để truy vết: một bài `RESOLVED`, một bài
`REMOVED`; tài khoản QA đã mở khóa. Không sửa tài khoản/bài có sẵn, không chạy
seed/reset hoặc đổi schema. Đây là dữ liệu tách biệt trên project hiện tại,
không phải một môi trường staging riêng.

Trên web admin thật, Playwright đã kiểm tra gỡ/khôi phục đúng bài QA, khóa/mở
khóa đúng tài khoản QA và đăng xuất qua Auth/CORS thật, không mock response.
Chỉ các URL ghi tới hai đối tượng QA đó được cho phép trong kịch bản. Các request
ghi trả `200`, đồng bộ Auth thành công; không lỗi console/page và danh sách user
375px không tràn ngang. Upload ảnh Storage đã được kiểm chứng riêng ở mục 4.5.
Chưa kiểm tra Android/hai điện thoại hoặc mọi nhánh xử lý báo cáo từ UI live;
tích hợp B/C đang chờ và không chặn phần việc A độc lập.

**QA hội thoại bị báo cáo (05/11/2026):** dùng lại hai tài khoản QA, tạo một bài
Found, claim/PASS và hội thoại mới còn mở. Cả hai gửi tin thành công trước khóa.
Admin đọc qua API bị `403` khi chưa có báo cáo trực tiếp, kể cả sau báo cáo về
bài hoặc tài khoản liên quan; đọc thẳng tin nhắn bằng Supabase vẫn bị RLS ẩn.
Sau báo cáo `CONVERSATION`, API trả đúng nội dung/thứ tự mới nhất trước qua
`limit=2`/`before_id`, không trùng hoặc thiếu tin, kể cả trang rỗng cuối cùng.
Năm lượt đọc HTTP và hai lượt đọc từ UI đều có `VIEW_CONVERSATION` riêng;
phân trang sai bị `422` và không tạo log xem thành công.

Trên web thật với Auth/CORS thật, đã mở đúng báo cáo QA, xem tin, hủy khóa và
kiểm tra focus trở về nút mở, rồi khóa qua dialog với lý do. Trạng thái khóa
được đọc lại từ cả hai participant; cả hai gửi payload hợp lệ bị RLS `42501`,
lịch sử vẫn đọc được và không có tin mới. Có đúng một `LOCK_CONVERSATION`
với lý do đã nhập; khóa lặp trả `409`. Report detail ở 375/768/1024/1440px
không tràn ngang, không lỗi console/page; đã đăng xuất. UI chỉ được phép ghi
tới đúng hội thoại và báo cáo QA mới, không mock response nghiệp vụ.
Ba báo cáo của lần QA được xử lý `KEEP`, bài mới được ẩn, dữ liệu/audit giữ lại.
Không sửa schema, seed/reset hoặc dữ liệu ngoài phạm vi QA. Kiểm chứng này
không xác nhận Android hay độ khớp toàn bộ giao diện với Figma.

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

**Trạng thái làm việc của từng endpoint:** ✅ đã chạy trên Render · 🧪 đã triển khai và kiểm thử cục bộ, chưa xác nhận trên Render · 🚧 đang làm (ngày dự kiến ghi bên cạnh).

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
| `questions` | Mỗi câu 3–300 ký tự. **Bài `FOUND`: bắt buộc 1–3 câu. Bài `LOST`: tùy chọn, 0–3 câu** (không có câu hỏi vẫn đăng được; chủ bài vẫn phải duyệt trước khi mở chat) |

```json
// 201 Created
{ "id": "9b1d...", "status": "ACTIVE", "expires_at": "2026-11-22T08:30:00+00:00" }
```
Quy tắc thêm:
- `image_paths`: mỗi đường dẫn phải đúng dạng `<id-của-bạn>/<tên>.jpg` (đuôi `jpg`, `jpeg`, `png` hoặc `webp`), **không trùng nhau**, và ảnh phải **đã được tải lên Storage trước đó**. Server kiểm tra từng ảnh có thật trong bucket.
- `event_time` phải có múi giờ và không ở tương lai (cho phép lệch 5 phút).
- Mỗi người **tối đa 10 bài trong 24 giờ**.
- Bài được tạo trong **một transaction**: có lỗi ở bất kỳ bước nào thì không có gì được lưu.

Lỗi: `400` đường dẫn ảnh sai, ảnh của người khác, hoặc ảnh chưa được tải lên · `422` sai luật ở bảng trên (kể cả bài `FOUND` không có câu hỏi), hoặc `category_id` không tồn tại · `429` đăng quá 10 bài trong 24 giờ · `403` tài khoản bị khóa hoặc chưa có hồ sơ.

#### `PATCH /posts/{post_id}/status` ✅
Chủ bài ẩn, hiện lại hoặc xóa bài của mình.
Đã kiểm tra live chuyển `ACTIVE` → `ARCHIVED` trên bài QA có ảnh; nhánh hiện lại
và xóa vẫn dựa trên kiểm chứng cục bộ, chưa chạy trong đợt Storage QA.
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
Nếu bài **không có câu hỏi** (bài `LOST` tùy chọn), `questions` là danh sách rỗng `[]`. Khi đó app **không hiện màn trả lời** mà hiện màn "Gửi yêu cầu liên hệ".
Lỗi: `403` đây là bài của bạn · `404` không có bài · `409` bài không còn hoạt động, hoặc bạn đã có yêu cầu đang xử lý · `429` đã hết lượt.

#### `POST /posts/{post_id}/claims` ✅
Nộp câu trả lời. **Phải trả lời đủ mọi câu hỏi của bài.** Bài không có câu hỏi thì gửi `{"answers": []}`: chỉ là yêu cầu liên hệ, vẫn tạo claim `SUBMITTED` và chờ chủ bài duyệt.
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

#### `POST /claims/{claim_id}/confirm` ✅
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

#### `POST /reports` ✅
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
| `GET /admin/stats` | Tổng quan | Số báo cáo chờ xử lý (và chênh so với hôm qua), số bài đang hiển thị (tách Lost, Found), số người dùng (và số mới trong tuần), số bài DONE trong tuần và tỷ lệ trao trả, số bài mỗi ngày trong 7 ngày (Lost, Found), 5 báo cáo mới nhất | ✅ |
| `GET /admin/reports?status=&type=&q=&limit=&offset=` | Báo cáo | Danh sách: mã `R-<report_no>`, loại (bài, tài khoản, hội thoại), đối tượng, lý do, handle người báo cáo, thời gian, trạng thái. `status`: `PENDING`, `REVIEWING`, `RESOLVED`, `DISMISSED` | ✅ |
| `GET /admin/reports/{id}` | Chi tiết báo cáo | Báo cáo, bài hoặc tài khoản bị báo cáo (kèm số bài và số lần bị báo cáo của người đăng), **mọi báo cáo khác về cùng đối tượng**, tóm tắt hội thoại liên quan (số tin, tin cuối), nhật ký xử lý | ✅ |
| `GET /admin/posts?type=&status=&q=&limit=&offset=` | Bài đăng | Danh sách kèm **số báo cáo** của từng bài | ✅ |
| `GET /admin/users?status=&q=&limit=&offset=` | Người dùng | Handle, **số điện thoại đã che** (`09•• ••• 321`), vai trò, tháng tham gia, số bài, số lần bị báo cáo, trạng thái. `status`: `ACTIVE`, `BANNED`, `REPORTED` (bị báo cáo nhiều) | ✅ |
| `GET /admin/actions?target_type=&range=&q=` | Nhật ký hoạt động | Thời gian, admin, hành động, đối tượng, mã báo cáo, ghi chú. `range`: `today`, `7d`, `30d` | ✅ |
| `GET /admin/conversations/{id}/messages?limit=100&before_id=` | Hội thoại bị báo cáo | Tin nhắn của cuộc chat **bị báo cáo**. Mỗi lần gọi **luôn ghi log** `VIEW_CONVERSATION` | ✅ |

Số điện thoại **đầy đủ không bao giờ được trả về** cho admin web; backend chỉ gửi bản đã che.

#### `GET /admin/reports` và `GET /admin/reports/{id}`: dữ liệu trả về

Tham số của danh sách: `status` (`PENDING`, `REVIEWING`, `RESOLVED`, `DISMISSED`), `type` (`POST`, `USER`, `CONVERSATION`), `q` (tìm theo mã báo cáo như `R-1042` hoặc `1042`, tiêu đề bài, handle người bị báo cáo, handle người báo cáo; gõ **không dấu** vẫn tìm được), `limit` (1–100, mặc định 20), `offset`. Báo cáo chưa xử lý (`PENDING`, `REVIEWING`) luôn nằm trên, trong mỗi nhóm mới nhất trước.

```json
// GET /admin/reports?status=PENDING&limit=20
{
  "total": 12,
  "items": [
    {
      "id": "4288d74f-...",
      "code": "R-1042",
      "target_type": "POST",
      "target_id": "9b1d...",
      "target_label": "Nhặt được balo đen có laptop",
      "reason": "MONEY_REQUEST",
      "status": "PENDING",
      "reporter": { "id": "374f...", "username": "thulan_k" },
      "created_at": "2026-10-23T09:00:00+00:00",
      "same_target_count": 3
    }
  ]
}
```
`target_label` là tiêu đề bài, handle người dùng, hoặc `a ↔ b` với hội thoại. `same_target_count` là tổng số báo cáo về cùng đối tượng (kể cả báo cáo này), dùng để hiện chấm cảnh báo.

```json
// GET /admin/reports/{id}
{
  "id": "4288d74f-...", "code": "R-1042", "reason": "MONEY_REQUEST", "status": "PENDING",
  "description": "Bạn này đòi tiền chuộc", "created_at": "...",
  "handled_by": null, "handled_at": null, "resolution_action": null, "resolution_note": null,
  "reporter": { "id": "...", "username": "thulan_k", "display_name": "Thu Lan" },
  "target": {
    "type": "POST",
    "post": {
      "id": "...", "type": "FOUND", "title": "Nhặt được balo đen có laptop", "description": "...",
      "status": "ACTIVE", "location_text": "Thư viện Tổng hợp", "event_time": "...", "created_at": "...",
      "category": { "id": 5, "name": "Túi / Balo" },
      "tags": ["balo", "laptop"], "image_paths": ["<user_id>/<uuid>.jpg"],
      "author": {
        "id": "...", "username": "hoang_nam", "display_name": null, "is_banned": false,
        "joined_at": "...", "post_count": 12, "reported_before_count": 1
      }
    },
    "user": null,
    "conversation": {
      "id": "...", "participants": ["hoang_nam", "thulan_k"], "is_locked": false,
      "last_message_at": "...", "message_count": 6
    }
  },
  "reports_on_target": [
    { "id": "...", "code": "R-1042", "reason": "MONEY_REQUEST", "description": "...",
      "status": "PENDING", "created_at": "...", "reporter_username": "thulan_k", "is_current": true }
  ],
  "admin_actions": []
}
```
Ghi chú:
- Đúng một trong `target.post` hoặc `target.user` có dữ liệu (tùy `target.type`); với báo cáo hội thoại thì dùng `target.conversation`.
- `target.conversation` còn xuất hiện ở báo cáo bài hoặc người dùng khi **người báo cáo và đối tượng từng có hội thoại**. Nội dung tin nhắn **không** nằm trong phản hồi này; muốn xem phải gọi `GET /admin/conversations/{id}/messages` (có ghi nhật ký).
- `image_paths` ghép với địa chỉ ảnh công khai như mục 4.5.
- `reported_before_count`: số báo cáo **khác** về người này hoặc các bài của họ.
- Không có email hay số điện thoại trong phản hồi.
- `resolution_action` (`REMOVE_POST`, `BAN_USER`, `KEEP`, `DISMISS`) và `resolution_note` chỉ có giá trị sau khi báo cáo đã được xử lý. `resolution_note` là **ghi chú nội bộ của admin**, không bao giờ hiển thị cho người báo cáo.
- `admin_actions` gồm mọi hành động đã thực hiện trên **các báo cáo cùng đối tượng** (đang xem xét, gỡ bài, khóa tài khoản...), mới nhất trước.

**Thay đổi (đều ghi nhật ký):**

| Endpoint | Body | Tác dụng | Trạng thái |
|---|---|---|---|
| `POST /admin/reports/{id}/review` | không | Đánh dấu báo cáo **đang xem xét** (`REVIEWING`). Log `REVIEW_REPORT` | ✅ |
| `POST /admin/reports/{id}/resolve` | `{"action": "...", "note": "..."}` | Xử lý báo cáo. `action`: `REMOVE_POST` (gỡ bài), `BAN_USER` (khóa tài khoản người bị báo cáo), `KEEP` (đã xử lý, giữ nguyên), `DISMISS` (không vi phạm, trạng thái `DISMISSED`). **`note` bắt buộc**. Người báo cáo nhận thông báo `REPORT_UPDATED` | ✅ |
| `POST /admin/posts/{id}/remove` | `{"reason": "..."}` | Gỡ bài (`REMOVED`), đóng các yêu cầu đang mở, **thông báo cho chủ bài kèm lý do** (`POST_REMOVED`) | ✅ |
| `POST /admin/posts/{id}/restore` | không | Khôi phục bài đã gỡ | ✅ |
| `POST /admin/users/{id}/ban` | `{"reason": "..."}` | Khóa tài khoản: đặt `is_banned`, chặn đăng nhập, gỡ các bài đang hoạt động của họ | ✅ |
| `POST /admin/users/{id}/unban` | `{"reason": "..."}` | Mở khóa | ✅ |
| `POST /admin/conversations/{id}/lock` | `{"reason": "..."}` | Khóa hội thoại: hai bên chỉ còn đọc. Log `LOCK_CONVERSATION` | ✅ |

#### `POST /admin/reports/{id}/review` và `POST /admin/reports/{id}/resolve`: chi tiết

**Quy tắc chung:** quyết định áp dụng cho **mọi báo cáo đang mở về cùng đối tượng**, không chỉ báo cáo đang xem. Ví dụ bài bị 3 người báo cáo, admin xử lý một lần thì cả 3 cùng được đóng và cả 3 người báo cáo cùng nhận thông báo. `affected_reports` cho biết số báo cáo bị tác động.

**`review`** (không cần body): chuyển các báo cáo `PENDING` về cùng đối tượng sang `REVIEWING`, ghi tên admin đang xem xét. Gọi lại khi đã `REVIEWING` vẫn trả `200` với `affected_reports: 0`.
```json
// 200 OK
{ "report_id": "...", "code": "R-1042", "status": "REVIEWING", "affected_reports": 3 }
```
Lỗi: `403` · `404` · `409` báo cáo đã được xử lý.

**`resolve`:**
```json
// Request
{ "action": "REMOVE_POST", "note": "Bài có yêu cầu chuyển khoản trước", "user_id": null }
```
| `action` | Tác dụng | Điều kiện |
|---|---|---|
| `REMOVE_POST` | Bài thành `REMOVED` và lưu `note` làm lý do; đóng các yêu cầu xác minh đang mở của bài (chat bị khóa); **chủ bài nhận thông báo `POST_REMOVED` kèm lý do** | Báo cáo phải nhắm vào một bài đăng |
| `BAN_USER` | Khóa người bị báo cáo: đặt `is_banned`, gỡ các bài đang hoạt động của họ, đóng mọi yêu cầu xác minh đang mở của họ, chặn đăng nhập ở Supabase Auth | Báo cáo bài: khóa **chủ bài**. Báo cáo người dùng: khóa người đó. **Báo cáo hội thoại: bắt buộc gửi `user_id`** là một trong hai người trong cuộc trò chuyện |
| `KEEP` | Đánh dấu đã xử lý, giữ nguyên nội dung | |
| `DISMISS` | Đóng báo cáo với trạng thái `DISMISSED` (không vi phạm) | |

`note` **bắt buộc**, 3–500 ký tự. Với `REMOVE_POST`, `note` được gửi cho chủ bài làm lý do gỡ nên cần viết lịch sự; với các hành động khác `note` chỉ lưu nội bộ vào nhật ký.

Người báo cáo nhận thông báo `REPORT_UPDATED` với nội dung theo kết quả: "Bài đăng bị báo cáo đã được gỡ", "Tài khoản vi phạm đã bị khóa", "Đã xem xét, nội dung được giữ nguyên", "Không phát hiện vi phạm".
```json
// 200 OK
{
  "report_id": "...", "code": "R-1042", "status": "RESOLVED", "resolution_action": "REMOVE_POST",
  "affected_reports": 3, "banned_user_id": null, "login_blocked": null
}
```
Với `BAN_USER`: `banned_user_id` là ID người bị khóa, `login_blocked` cho biết đã chặn được đăng nhập ở Supabase Auth chưa. Nếu là `false`, tài khoản **vẫn bị khóa trong hệ thống** (mọi API đã từ chối họ) nhưng cần kiểm tra thủ công trong Supabase Authentication.

Lỗi: `400` `REMOVE_POST` cho báo cáo không phải về bài, người bị khóa không thuộc cuộc trò chuyện, hoặc định khóa một quản trị viên · `403` · `404` · `409` báo cáo đã được xử lý, bài đã bị gỡ hoặc xóa, hoặc tài khoản đã bị khóa · `422` thiếu hoặc quá ngắn `note`, hoặc thiếu `user_id` khi báo cáo hội thoại.

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
| `reports` | Chỉ báo cáo mình đã gửi. Màn "Báo cáo đã gửi" dùng `status` và `resolution_action`: `REMOVE_POST` → "Đã gỡ bài", `BAN_USER` → "Đã khóa tài khoản", `KEEP` → "Giữ nguyên", `DISMISS` → "Không vi phạm". Ghi chú nội bộ của admin không bao giờ gửi cho người báo cáo |

Bảng `verification_questions` chỉ **chủ bài** đọc trực tiếp được (để xem lại câu hỏi của chính bài mình). Người muốn trả lời xác minh phải dùng `GET /posts/{id}/questions`.

### 4.4 Những việc được ghi trực tiếp
| Việc | Cách làm |
|---|---|
| Gửi tin nhắn | Chèn vào `messages`: `conversation_id`, `sender_id` = ID của bạn, `type` = `TEXT` hoặc `IMAGE`, `content` (1–2000 ký tự). Bị chặn nếu cuộc trò chuyện đã khóa |
| Đánh dấu tin đã đọc | Cập nhật `messages.read_at` của tin do **người kia** gửi |
| Đánh dấu thông báo đã đọc ("Đọc hết") | Cập nhật `notifications.read_at` |
| Sửa hồ sơ | Cập nhật `profiles`: chỉ các cột `display_name`, `avatar_url`, `bio` (tối đa 160 ký tự) |

### 4.5 Upload ảnh
Bucket `post-images`. Thư mục đầu **bắt buộc là ID của chính bạn**, nếu không bị
từ chối. Đặt tên ảnh bằng UUID mới, ví dụ **`<user_id>/<uuid>.jpg`**; API cũng
chấp nhận các đuôi `jpeg`, `png`, `webp` như quy định ở mục 3.3.
Khuyến nghị: nén ảnh, cạnh dài khoảng 1280 px, JPEG chất lượng khoảng 80%.

Địa chỉ hiển thị ảnh:
```
<SUPABASE_URL>/storage/v1/object/public/post-images/<đường dẫn>
```

**Storage QA live (05/11/2026):** dùng lại hai tài khoản QA, upload một PNG mới
vào đúng thư mục chủ sở hữu, rồi tạo một bài Found có ảnh qua FastAPI. Đọc lại
`post_images` và chi tiết báo cáo xác nhận đúng đường dẫn/thứ tự ảnh. URL public
trả `200`, MIME `image/png`, SHA-256 khớp bytes upload. Ảnh giải mã đúng 320×180
và hiển thị trên web admin thật tại 375/768/1024/1440px, không tràn ngang hoặc
lỗi console/page. Báo cáo QA được xử lý `KEEP` qua UI; bài được ẩn `ARCHIVED`,
ảnh vẫn được giữ để truy vết.

Các kiểm tra từ chối đã đạt: upload vào thư mục người khác, không có thư mục
chủ sở hữu, không đăng nhập và ghi đè ảnh của người khác đều lỗi RLS `403`.
Client không liệt kê được ảnh trong thư mục đã biết có ảnh; URL public vẫn
đọc được theo thiết kế bucket công khai. FastAPI trả `400` với ảnh của người
khác, ảnh chưa upload hoặc đường dẫn trùng; quá 5 ảnh trả `422`. Những request
bị từ chối không tạo bài đăng dở dang. Không chạy seed/reset, không xóa object
hoặc thay đổi dữ liệu cũ. Chưa kiểm tra Photo Picker/nén ảnh trên Android hoặc
thao tác xóa Storage; không suy ra giới hạn MIME/dung lượng bucket từ các kiểm
tra này (cấu hình bucket hiện không đặt hai giới hạn đó).

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

## 7. Chi tiết bổ sung cho các API admin

Các danh sách trả `{total, items}`; `total` tính sau lọc và trước phân trang.
`limit`: 1–100, mặc định 20; `offset`: từ 0; `q`: tối đa 100 ký tự.
Thứ tự ổn định: thời gian tạo giảm dần, rồi ID giảm dần.

- **Posts:** lọc `type`, `status`; tìm không dấu trong tiêu đề, mô tả, địa điểm.
  Mỗi phần tử có `id`, `title`, `type`, `status`, `author_id`, `author_username`,
  `category_name`, `created_at`, `expires_at`, `report_count`.
- **Users:** tìm handle hoặc tên hiển thị. `REPORTED` là từ **3 báo cáo** về tài khoản
  hoặc các bài của họ, tính mọi trạng thái; có thể bao gồm tài khoản đã bị khóa.
  Trả `phone_masked`, `joined_month`, `post_count`, `report_count`, không trả email
  hay số điện thoại đầy đủ. Các bộ lọc ACTIVE/BANNED dựa vào tình trạng khóa.
- **Actions:** tìm admin, hành động, nhãn đối tượng, mã báo cáo và ghi chú.
  Có `limit`/`offset`; hành động độc lập có thể có `report_code:null`.
- **Stats:** múi giờ Asia/Ho_Chi_Minh; “trong tuần” là bảy ngày lịch gồm hôm nay.
  `pending_reports` tính PENDING + REVIEWING; `pending_delta` so với số báo cáo
  đang mở tại đầu ngày hôm nay, suy ra từ thời điểm tạo/xử lý của dữ liệu còn lưu.
  `completed_this_week` dùng thời điểm hoàn tất; `completion_rate` là phần trăm
  bài RESOLVED trên mọi bài chưa DELETED, trả 0 khi không có bài.
  `posts_by_day` luôn có bảy ngày theo ngày tạo bài; `recent_reports` tối đa 5.
- **Messages:** chỉ hội thoại có báo cáo trực tiếp nhắm vào chính hội thoại đó.
  Báo cáo bài/người dùng liên quan không tự cấp quyền xem chat. Trả mới nhất trước;
  `before_id` là ID dương, lấy các tin có ID nhỏ hơn. Mỗi lần xem thành công ghi log.
- **Trạng thái bài:** ẩn/xóa đóng các yêu cầu SUBMITTED/PASSED, khóa chat liên quan.
  Hiện/khôi phục bài không mở lại yêu cầu hoặc chat đã đóng. Khôi phục chỉ áp dụng
  cho REMOVED còn hạn, chủ bài chưa bị khóa, giữ trạng thái trước gỡ
  (ACTIVE/ARCHIVED/RESOLVED). Với dữ liệu gỡ cũ chưa có lịch sử trạng thái, dùng
  RESOLVED nếu có thời điểm hoàn tất; trường hợp còn lại dùng ACTIVE.
- **Moderation:** `reason` bắt buộc 3–500 ký tự sau cắt khoảng trắng. Gỡ/khôi phục
  bài trả `{id,status}`; khóa hội thoại trả `{id,is_locked}`; khóa/mở khóa tài khoản
  trả `{id,is_banned,login_blocked}`. Khóa vẫn có hiệu lực trong database nếu Auth
  lỗi (`login_blocked:false`); mở khóa Auth thất bại trả 502 và giữ nguyên khóa.
  Mở khóa không tự khôi phục bài hoặc chat.

Các endpoint mới có response schema trong OpenAPI. 🧪 là kết quả kiểm thử cục bộ,
không đồng nghĩa mã đã được triển khai lên Render.

Cron gọi `public.expire_posts()` mỗi giờ: bài ACTIVE quá hạn chuyển EXPIRED,
yêu cầu đang mở bị đóng, chat bị khóa và chủ bài nhận thông báo POST_EXPIRED.
Cài bằng `supabase/install_cron.sql` sau migration; người dùng ứng dụng không
được gọi trực tiếp hàm hết hạn.

Đã kiểm tra cấu hình database live ngày 04/11/2026: job `expire-posts` đang bật,
lịch `0 * * * *`, ba lượt gần nhất lúc 14:00, 15:00 và 16:00 UTC đều `succeeded`.
Cả 16 bảng `public` bật RLS; `claims`, `messages`, `notifications` có trong
publication `supabase_realtime`. Việc có publication chưa kiểm chứng tin nhắn
được nhận realtime trên hai thiết bị Android. Lần kiểm tra này chỉ đọc metadata
và lịch sử cron, không chạy seed/reset hoặc đổi dữ liệu/schema trên live.
