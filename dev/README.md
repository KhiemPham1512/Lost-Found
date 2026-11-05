# Công cụ hỗ trợ phát triển (dev/)

Chỉ dùng khi phát triển. Không liên quan tới ứng dụng chạy thật.

## Lấy token nhanh để thử API

1. Sao chép `accounts.example.json` thành `accounts.local.json` rồi điền: địa chỉ project, publishable key, email và mật khẩu của các tài khoản thử (admin, a, b, c...).
   File `*.local.json` đã bị `.gitignore` chặn nên **không bị đưa lên GitHub**. Kiểm tra bằng `git status`.
2. Ở thư mục gốc của repo:

```
py dev\gettoken.py            liệt kê các tài khoản
py dev\gettoken.py admin      lấy token của admin và copy vào clipboard
py dev\gettoken.py b --show   in token ra màn hình
py dev\gettoken.py b --force  bỏ qua token đã lưu, đăng nhập lại
```
3. Mở `/docs` > **Authorize** > dán (Ctrl+V) > Authorize.

Token sống 1 giờ. Script lưu lại và dùng lại tới khi sắp hết hạn; hết hạn thì tự làm mới.
Không đặt tên file là `token.py`: tên đó trùng một module có sẵn của Python và làm script lỗi.
