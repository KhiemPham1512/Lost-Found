r"""Lấy access token Supabase nhanh để thử API (CHỈ DÙNG KHI PHÁT TRIỂN).

Cách dùng (chạy ở thư mục gốc của repo):
    py dev\gettoken.py            liệt kê các tài khoản đã khai báo
    py dev\gettoken.py admin      lấy token của "admin" và COPY vào clipboard (rồi dán vào nút Authorize)
    py dev\gettoken.py b --show   in token ra màn hình thay vì copy
    py dev\gettoken.py b --force  bỏ qua token đã lưu, đăng nhập lại

Token sống 1 giờ. Script lưu lại và dùng lại tới khi sắp hết hạn, nên gọi nhiều lần không tốn thêm yêu cầu.
Chỉ dùng thư viện có sẵn của Python, không cần cài gì.
"""
import argparse
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONFIG = HERE / "accounts.local.json"
CACHE = HERE / ".token-cache.local.json"
MIN_LEFT = 120  # còn dưới 120 giây thì lấy token mới


class TokenError(Exception):
    pass


def fail(message: str) -> None:
    print(f"LỖI: {message}", file=sys.stderr)
    sys.exit(1)


def load_config() -> dict:
    if not CONFIG.exists():
        fail(
            "chưa có dev/accounts.local.json.\n"
            "      Sao chép dev/accounts.example.json thành dev/accounts.local.json rồi điền thông tin thật."
        )
    try:
        cfg = json.loads(CONFIG.read_text(encoding="utf-8-sig"))
    except json.JSONDecodeError as e:
        fail(f"dev/accounts.local.json không phải JSON hợp lệ ({e}). Kiểm tra dấu phẩy và dấu nháy.")
    for key in ("supabase_url", "publishable_key", "accounts"):
        if not cfg.get(key):
            fail(f"dev/accounts.local.json thiếu mục '{key}'.")
    cfg["supabase_url"] = cfg["supabase_url"].rstrip("/")
    if "/rest/v1" in cfg["supabase_url"] or not cfg["supabase_url"].startswith(("https://", "http://")):
        fail("supabase_url phải có dạng https://<mã-dự-án>.supabase.co (không có /rest/v1 ở cuối).")
    return cfg


def load_cache() -> dict:
    try:
        return json.loads(CACHE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}


def save_cache(cache: dict) -> None:
    CACHE.write_text(json.dumps(cache, indent=2), encoding="utf-8")


def token_request(cfg: dict, grant_type: str, payload: dict) -> dict:
    req = urllib.request.Request(
        f"{cfg['supabase_url']}/auth/v1/token?grant_type={grant_type}",
        data=json.dumps(payload).encode("utf-8"),
        headers={"apikey": cfg["publishable_key"], "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            body = json.loads(e.read().decode("utf-8"))
            message = body.get("error_description") or body.get("msg") or body.get("message") or str(body)
        except Exception:
            message = f"HTTP {e.code}"
        raise TokenError(message) from None
    except urllib.error.URLError as e:
        raise TokenError(f"không kết nối được tới Supabase ({e.reason})") from None


def get_token(cfg: dict, name: str, force: bool) -> tuple[dict, str]:
    account = cfg["accounts"][name]
    cache = load_cache()
    entry = cache.get(name)
    if entry and entry.get("email") != account["email"]:
        entry = None  # đổi email trong file cấu hình thì bỏ token cũ
    now = time.time()

    if entry and not force and entry["expires_at"] - now > MIN_LEFT:
        return entry, "dùng lại token đã lưu"

    data, source = None, ""
    if entry and entry.get("refresh_token") and not force:
        try:
            data = token_request(cfg, "refresh_token", {"refresh_token": entry["refresh_token"]})
            source = "làm mới bằng refresh token"
        except TokenError:
            data = None  # refresh hỏng thì đăng nhập lại bằng mật khẩu
    if data is None:
        try:
            data = token_request(cfg, "password", {"email": account["email"], "password": account["password"]})
        except TokenError as e:
            fail(f"đăng nhập '{name}' ({account['email']}) thất bại: {e}")
        source = "đăng nhập mới"

    entry = {
        "email": account["email"],
        "access_token": data["access_token"],
        "refresh_token": data.get("refresh_token"),
        "expires_at": data.get("expires_at") or (now + data.get("expires_in", 3600)),
    }
    cache[name] = entry
    save_cache(cache)
    return entry, source


def copy_to_clipboard(text: str) -> bool:
    for cmd in (["clip"], ["pbcopy"], ["xclip", "-selection", "clipboard"], ["wl-copy"]):
        try:
            subprocess.run(cmd, input=text.encode("ascii"), check=True, capture_output=True)
            return True
        except (OSError, subprocess.CalledProcessError):
            continue
    return False


def main() -> None:
    for stream in (sys.stdout, sys.stderr):  # tránh lỗi hiển thị tiếng Việt khi terminal không dùng UTF-8
        try:
            stream.reconfigure(encoding="utf-8")
        except (AttributeError, OSError):
            pass
    parser = argparse.ArgumentParser(description="Lấy token Supabase nhanh để thử API")
    parser.add_argument("who", nargs="?", help="tên tài khoản trong accounts.local.json (vd: admin, a, b, c)")
    parser.add_argument("--show", action="store_true", help="in token ra màn hình thay vì copy")
    parser.add_argument("--force", action="store_true", help="bỏ qua token đã lưu và đăng nhập lại")
    args = parser.parse_args()

    cfg = load_config()
    accounts = cfg["accounts"]
    if not args.who:
        print("Các tài khoản đã khai báo:")
        for name, acc in accounts.items():
            print(f"  {name:<10} {acc['email']}")
        print("\nVí dụ: py dev\\gettoken.py " + next(iter(accounts)))
        return
    if args.who not in accounts:
        fail(f"không có tài khoản '{args.who}'. Có: {', '.join(accounts)}")

    entry, source = get_token(cfg, args.who, args.force)
    minutes = max(0, int((entry["expires_at"] - time.time()) // 60))
    print(f"{args.who} ({entry['email']}): {source}, còn khoảng {minutes} phút.")
    if args.show:
        print(entry["access_token"])
    elif copy_to_clipboard(entry["access_token"]):
        print("Đã copy token vào clipboard. Mở /docs > Authorize > dán (Ctrl+V).")
    else:
        print("Không copy được vào clipboard, token ở dòng dưới:")
        print(entry["access_token"])


if __name__ == "__main__":
    main()
