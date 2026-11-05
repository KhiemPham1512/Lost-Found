-- =====================================================================
--  LOST & FOUND - RESET (CHỈ DÙNG TRONG GIAI ĐOẠN PHÁT TRIỂN)
--  Xóa TOÀN BỘ bảng, hàm, kiểu dữ liệu của ứng dụng để chạy lại schema.sql từ đầu.
--  ⚠ Mất hết dữ liệu ứng dụng (bài đăng, tin nhắn, report...).
--  Không xóa người dùng trong Authentication: sau khi reset hãy vào
--  Dashboard > Authentication > Users xóa các user thử nghiệm cũ.
--  KHÔNG chạy file này khi app đã có người dùng thật.
-- =====================================================================

drop policy if exists "post-images: read"                 on storage.objects;
drop policy if exists "post-images: upload to own folder" on storage.objects;
drop policy if exists "post-images: delete own files"     on storage.objects;

drop table if exists
  public.device_tokens, public.notifications, public.admin_actions, public.reports,
  public.messages, public.conversations, public.claim_answers, public.claims,
  public.verification_questions, public.post_tags, public.post_images, public.posts,
  public.tags, public.categories, public.user_private, public.profiles
cascade;

do $$
declare
  r record;
begin
  for r in
    select p.oid::regprocedure as sig
    from pg_proc p
    where p.pronamespace = 'public'::regnamespace
      and p.proname in ('f_unaccent', 'set_updated_at', 'claims_before_insert', 'claims_before_update',
                        'claims_after_insert', 'claims_after_status_change', 'messages_after_insert',
                        'notify', 'is_admin', 'distance_km', 'search_posts', 'my_todo_counts', 'expire_posts')
  loop
    execute format('drop function if exists %s cascade', r.sig);
  end loop;
end $$;

drop type if exists
  public.user_role, public.post_type, public.post_status, public.claim_status,
  public.message_type, public.report_reason, public.report_status,
  public.notification_type, public.admin_action_type
cascade;
