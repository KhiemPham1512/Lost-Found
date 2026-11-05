-- =====================================================================
--  PATCH: thông báo "cần hành động" tự chuyển sang ĐÃ ĐỌC khi không còn cần thiết
--  - CLAIM_RECEIVED ("Có yêu cầu xác minh mới"): hết cần thiết khi chủ bài đã duyệt/từ chối, hoặc yêu cầu bị hủy/đóng
--  - HANDOVER_WAITING ("Chờ bạn xác nhận"): hết cần thiết khi bài DONE, hoặc yêu cầu bị hủy/đóng
--  Thông báo KHÔNG bị xóa: vẫn nằm trong mục "Trước đó" làm lịch sử.
--  Chạy được nhiều lần. Không mất dữ liệu. (Database dựng mới từ schema.sql hiện tại không cần chạy file này.)
-- =====================================================================

create or replace function public.claims_after_status_change()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_conv   uuid;
  v_title  text;
  v_author text;
  v_claimant text;
begin
  select title into v_title from public.posts where id = new.post_id;
  select coalesce(display_name, username) into v_author   from public.profiles where id = new.post_author_id;
  select coalesce(display_name, username) into v_claimant from public.profiles where id = new.claimant_id;

  -- một bên vừa xác nhận trao trả, bên kia chưa
  if new.status = 'PASSED' then
    if old.author_confirmed_at is null and new.author_confirmed_at is not null then
      perform public.notify(new.claimant_id, 'HANDOVER_WAITING', 'Chờ bạn xác nhận đã trao trả',
                            v_author || ' đã xác nhận trao trả bài "' || v_title || '"', new.post_id, new.id);
    end if;
    if old.claimant_confirmed_at is null and new.claimant_confirmed_at is not null then
      perform public.notify(new.post_author_id, 'HANDOVER_WAITING', 'Chờ bạn xác nhận đã trao trả',
                            v_claimant || ' đã xác nhận trao trả bài "' || v_title || '"', new.post_id, new.id);
    end if;
  end if;

  if new.status = old.status then
    return new;
  end if;

  -- Thông báo "cần hành động" mà hết cần thiết thì đánh dấu ĐÃ ĐỌC (vẫn giữ lại làm lịch sử, không xóa)
  if old.status = 'SUBMITTED' then
    update public.notifications
       set read_at = coalesce(read_at, now())
     where claim_id = new.id and type = 'CLAIM_RECEIVED';
  end if;
  if new.status in ('COMPLETED', 'CANCELLED', 'CLOSED') then
    update public.notifications
       set read_at = coalesce(read_at, now())
     where claim_id = new.id and type = 'HANDOVER_WAITING';
  end if;

  if new.status = 'PASSED' then
    insert into public.conversations (claim_id, post_author_id, claimant_id)
    values (new.id, new.post_author_id, new.claimant_id)
    on conflict (claim_id) do nothing
    returning id into v_conv;

    if v_conv is not null then
      insert into public.messages (conversation_id, sender_id, type, content)
      values (v_conv, null, 'SYSTEM',
              'Xác minh thành công. Hãy trao đổi để thống nhất thời gian và địa điểm trao trả. Không chuyển tiền trước.');
    end if;
    perform public.notify(new.claimant_id, 'CLAIM_PASSED', 'Bạn đã được cho phép nhắn tin',
                          v_author || ' đã duyệt câu trả lời của bạn cho bài "' || v_title || '"', new.post_id, new.id);
  end if;

  if new.status = 'FAILED' then
    perform public.notify(new.claimant_id, 'CLAIM_FAILED', 'Yêu cầu xác minh bị từ chối',
                          'Câu trả lời của bạn cho bài "' || v_title || '" chưa được chấp nhận', new.post_id, new.id);
  end if;

  if new.status = 'COMPLETED' then
    update public.posts
       set status = 'RESOLVED', resolved_at = now()
     where id = new.post_id and status = 'ACTIVE';

    update public.claims
       set status = 'CLOSED'
     where post_id = new.post_id and id <> new.id and status in ('SUBMITTED', 'PASSED');

    perform public.notify(new.post_author_id, 'POST_DONE', 'Bài "' || v_title || '" đã DONE',
                          'Cả hai bên đã xác nhận trao trả', new.post_id, new.id);
    perform public.notify(new.claimant_id,   'POST_DONE', 'Bài "' || v_title || '" đã DONE',
                          'Cả hai bên đã xác nhận trao trả', new.post_id, new.id);
  end if;

  if new.status in ('COMPLETED', 'CLOSED', 'CANCELLED') then
    update public.conversations set is_locked = true where claim_id = new.id;
  end if;

  return new;
end $$;

-- Dọn các thông báo đã cũ trước khi có patch này
update public.notifications n
   set read_at = coalesce(n.read_at, now())
  from public.claims c
 where n.claim_id = c.id
   and ((n.type = 'CLAIM_RECEIVED'   and c.status <> 'SUBMITTED')
     or (n.type = 'HANDOVER_WAITING' and c.status in ('COMPLETED', 'CANCELLED', 'CLOSED')));
