-- =====================================================================
--  DỌN DỮ LIỆU THỬ NGHIỆM (CHỈ DÙNG KHI ĐANG PHÁT TRIỂN)
--  Xóa: bài đăng, ảnh, tag, câu hỏi, yêu cầu xác minh, chat, thông báo, báo cáo, nhật ký admin, device token.
--  GIỮ NGUYÊN: tài khoản (auth.users), profiles, số điện thoại, danh mục (categories), vai trò ADMIN.
--  Chạy được vô số lần. Dán toàn bộ vào SQL Editor rồi bấm Run.
-- =====================================================================

-- 0) Chốt an toàn: từ chối chạy nếu hệ thống có hơn 50 tài khoản (nghi là dữ liệu thật)
do $$
begin
  if (select count(*) from auth.users) > 50 then
    raise exception 'Có hơn 50 tài khoản: có vẻ đây là dữ liệu thật, từ chối xóa. Sửa số 50 nếu bạn chắc chắn.';
  end if;
end $$;

-- 1) Xóa toàn bộ dữ liệu nghiệp vụ và đặt lại bộ đếm (mã báo cáo quay về R-1001, id tin nhắn về 1...)
truncate table
  public.notifications,
  public.admin_actions,
  public.reports,
  public.messages,
  public.conversations,
  public.claim_answers,
  public.claims,
  public.verification_questions,
  public.post_tags,
  public.post_images,
  public.posts,
  public.tags,
  public.device_tokens
restart identity;

-- 2) Mở khóa mọi tài khoản đã bị khóa khi thử (vai trò ADMIN giữ nguyên)
update public.profiles set is_banned = false, banned_at = null where is_banned or banned_at is not null;

-- 3) (Tùy chọn) Gỡ lệnh cấm đăng nhập ở Supabase Auth, nếu bạn đã thử khóa tài khoản qua admin.
--    Bỏ dấu -- ở dòng dưới rồi chạy. Nếu báo lỗi quyền thì mở Authentication > Users và bấm "Unban" bằng tay.
-- update auth.users set banned_until = null where banned_until is not null;
