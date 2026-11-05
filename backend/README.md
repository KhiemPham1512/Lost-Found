# Backend (FastAPI)

Phần việc của vai **A**. Backend xử lý mọi thay đổi trạng thái nghiệp vụ (đăng ký, đăng bài, xác minh, DONE, báo cáo, admin). Việc *đọc* dữ liệu thì app đi thẳng tới Supabase.

- Địa chỉ đang chạy: **https://lost-found-api-ph8v.onrender.com** (trang thử API: `/docs`)
- Hợp đồng API cho B, C và admin web: [`../docs/api.md`](../docs/api.md)

## Chạy ở máy mình (Windows, PowerShell)

Lần đầu:
```powershell
cd backend
py -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
Copy-Item .env.example .env      # rồi mở .env và điền giá trị thật, bấm Ctrl+S
```
Các lần sau (mỗi khi mở terminal mới):
```powershell
cd backend
.venv\Scripts\Activate.ps1
python -m uvicorn app.main:app --reload
```
Mở http://127.0.0.1:8000/docs để thử API.

> Dùng `py` để **tạo** venv (trên máy này `python` ngoài venv trỏ sang bản khác). Khi venv đang bật (thấy `(.venv)` đầu dòng) thì `python` đã đúng.
> `--reload` chỉ theo dõi file `.py`. **Sửa `.env` xong phải dừng (Ctrl+C) và chạy lại server.**

## Biến môi trường

| Biến | Ý nghĩa | Lấy ở đâu |
|---|---|---|
| `SUPABASE_URL` | Địa chỉ project, dạng `https://<mã>.supabase.co` | Project Settings → Integrations → Data API |
| `SUPABASE_SECRET_KEY` | Khóa secret (chỉ backend) | Project Settings → Configuration → API Keys |
| `DATABASE_URL` | Chuỗi kết nối **Session pooler** | Nút **Connect** → Session pooler |
| `ALLOWED_ORIGINS` | Địa chỉ web được phép gọi (CORS) | Tự đặt |

Trên Render, các biến này đặt ở trang **Environment** của dịch vụ (không có file `.env` trên GitHub).

## Cấu trúc thư mục

```
backend/
├── .env.example          # bản mẫu (file .env thật KHÔNG commit)
├── requirements.txt
└── app/
    ├── main.py           # tạo app, gắn router, mở/đóng nhóm kết nối database
    ├── db.py             # nhóm kết nối psycopg, hàm get_conn
    ├── core/
    │   ├── config.py     # đọc biến môi trường
    │   ├── security.py   # kiểm tra token JWT của Supabase (JWKS)
    │   └── deps.py       # get_active_user_id: token hợp lệ + hồ sơ tồn tại + chưa bị khóa
    ├── routers/          # nhận request, trả response (mỏng)
    ├── schemas/          # hình dạng dữ liệu và luật kiểm tra (Pydantic)
    └── services/         # logic nghiệp vụ và truy vấn
```
Mỗi lớp một việc: **router** nhận và trả HTTP, **schema** nói dữ liệu trông thế nào, **service** làm việc thật.

## Thêm một endpoint mới (quy trình)

1. Đọc mục tương ứng trong `docs/api.md` (đó là hợp đồng, không tự ý đổi).
2. Viết **schema** (dữ liệu vào và ra) trong `app/schemas/`.
3. Viết **service** trong `app/services/` (nhận `conn` hoặc dùng `httpx`).
4. Viết **router** trong `app/routers/`, dùng `Depends(get_active_user_id)` nếu cần đăng nhập.
5. Gắn router vào `app/main.py` bằng `app.include_router(...)`.
6. Thử trên `/docs`, rồi cập nhật cột *Trạng thái* trong `docs/api.md`.
7. Tạo nhánh `feat/a-...`, commit, push, mở Pull Request vào `develop`.

## Hai cách backend nói chuyện với Supabase

| Cách | Dùng cho | Ghi chú |
|---|---|---|
| `httpx` gọi HTTP | Tạo tài khoản (Auth Admin API), ghi hồ sơ một dòng | Khóa secret chỉ gửi qua header `apikey`, không gửi qua `Authorization: Bearer` |
| `psycopg` kết nối database | Mọi việc cần **transaction** nhiều bảng (tạo bài, xác minh...) | Mỗi request một kết nối: thoát bình thường thì COMMIT, có lỗi thì ROLLBACK |

## Triển khai (Render)

| Mục | Giá trị |
|---|---|
| Language | **Python 3** (không chọn Docker) |
| Branch | `develop` |
| Root Directory | `backend` |
| Build Command | `pip install -r requirements.txt` |
| Start Command | `uvicorn app.main:app --host 0.0.0.0 --port $PORT` |
| Health Check Path | `/health` |
| Instance | Free |

Tự triển khai lại mỗi khi gộp code vào `develop`. Gói miễn phí **tự tắt sau 15 phút** không có truy cập: mở `/health` trước buổi demo vài phút để đánh thức.

## Gặp lỗi thì xem đây

| Triệu chứng | Nguyên nhân hay gặp | Cách xử lý |
|---|---|---|
| `ValidationError ... Field required` lúc khởi động | `.env` chưa lưu, không có, sai tên file (`.env.txt`), hoặc lưu UTF-16 | Kiểm tra file, lưu bằng UTF-8, bấm Ctrl+S, chạy lại server |
| `/me` trả 401 dù token đúng | `SUPABASE_URL` sai (thừa chữ, thừa `/rest/v1/`) nên không tải được khóa công khai | Sửa `.env`, **khởi động lại server** |
| Render báo Timed Out, log đầy `GET /health 404` | Commit được build chưa có `/health` | Gộp bản đúng vào `develop`, deploy lại |
| Mở `https://…onrender.com/` thấy `Not Found` | Bình thường: app không có trang ở đường dẫn gốc | Mở `/health` hoặc `/docs` |
| Xóa user rồi token cũ vẫn gọi `/me` được | Token chỉ bị kiểm tra chữ ký và hạn (sống 1 giờ) | Bình thường. Endpoint cần quyền kiểm tra thêm bảng `profiles` |
| Lần gọi đầu rất chậm trên Render | Dịch vụ miễn phí vừa tỉnh dậy | Chờ 30–50 giây |
| `503 Chưa cấu hình database` | Chưa đặt `DATABASE_URL` | Đặt trong `.env` hoặc trang Environment của Render |

## Quy tắc an toàn

- **Không commit** `.env`. Trước mỗi lần commit chạy `git status` và kiểm tra.
- **Khóa secret chỉ ở backend.** Lỡ lộ thì đổi khóa ngay trong Supabase rồi cập nhật `.env` và Render.
- Không in mật khẩu hoặc token ra log.
