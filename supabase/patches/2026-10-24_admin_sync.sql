-- =====================================================================
--  PATCH: đồng bộ database với thiết kế Admin web trong Figma
--  Dùng cho database ĐÃ chạy schema.sql trước ngày 24/10/2026.
--  (Database dựng mới từ schema.sql hiện tại thì KHÔNG cần chạy file này.)
--  Chạy được nhiều lần mà không lỗi. Không mất dữ liệu.
-- =====================================================================

-- 1) Hai loại hành động admin mới ghi vào nhật ký
alter type public.admin_action_type add value if not exists 'REVIEW_REPORT';      -- đánh dấu "đang xem xét"
alter type public.admin_action_type add value if not exists 'LOCK_CONVERSATION';  -- khóa hội thoại

-- 2) Mã báo cáo dễ đọc (R-1001, R-1002...) hiển thị trong admin web
alter table public.reports
  add column if not exists report_no bigint generated always as identity (start with 1001);
create unique index if not exists reports_report_no_key on public.reports (report_no);
