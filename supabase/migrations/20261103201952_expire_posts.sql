-- Expiration entry point. Invoker privileges; never callable by application users.
create or replace function public.expire_posts()
returns integer language plpgsql security invoker set search_path = '' as $$
declare
  v_post record;
  v_count integer := 0;
begin
  for v_post in
    select id, author_id, title from public.posts
    where status = 'ACTIVE' and expires_at <= now()
    order by id for update skip locked
  loop
    update public.posts set status = 'EXPIRED' where id = v_post.id;
    update public.claims set status = 'CLOSED'
      where post_id = v_post.id and status in ('SUBMITTED', 'PASSED');
    perform public.notify(v_post.author_id, 'POST_EXPIRED', 'Bài đăng đã hết hạn',
      'Bài đăng đã kết thúc thời gian hoạt động 30 ngày.', v_post.id, null);
    v_count := v_count + 1;
  end loop;
  return v_count;
end $$;
revoke all on function public.expire_posts() from public, anon, authenticated;
