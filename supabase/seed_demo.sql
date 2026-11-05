-- Supply exactly five existing Auth user IDs using app.demo_user_ids (JSON array).
-- No passwords, Auth table writes, truncation, or reset of existing workflow state.
do $$
declare
  users jsonb := current_setting('app.demo_user_ids')::jsonb;
  uid uuid;
  pid uuid;
  i integer;
  category smallint;
begin
  if jsonb_array_length(users) <> 5 or
     (select count(distinct value) from jsonb_array_elements_text(users)) <> 5 then
    raise exception 'Exactly five distinct demo user IDs are required';
  end if;
  select id into strict category from public.categories where code='WALLET';
  for i in 0..4 loop
    uid := (users->>i)::uuid;
    if not exists(select from auth.users where id=uid) then
      raise exception 'Demo Auth user does not exist';
    end if;
    insert into public.profiles(id,username,display_name,role)
      values(uid,'lf_demo_' || right(replace(uid::text,'-',''),12),'Demo ' || (i+1),
             case when i=0 then 'ADMIN'::public.user_role else 'USER'::public.user_role end)
      on conflict(id) do nothing;
  end loop;
  for i in 1..10 loop
    uid := (users->>((i-1)%5))::uuid;
    pid := ('9f000000-0000-4000-8000-' || lpad(i::text,12,'0'))::uuid;
    if exists(select from public.posts where id=pid and author_id<>uid) then
      raise exception 'Demo post ID collision: no data was overwritten';
    end if;
    insert into public.posts(id,author_id,type,title,description,category_id,location_text,
                            latitude,longitude,event_time)
      values(pid,uid,case when i%2=0 then 'LOST'::public.post_type else 'FOUND'::public.post_type end,
        case when i%2=0 then '[Demo] Tìm ví số ' else '[Demo] Nhặt được ví số ' end || i,
        'Dữ liệu minh họa luồng tìm và trao trả đồ, không phải thông tin thật.',category,
        'Căn tin UTH',10.803,106.714,now()-i*interval '1 hour')
      on conflict(id) do nothing;
    if i%2=1 then
      insert into public.verification_questions(post_id,question_text,position)
        values(pid,'Bên trong ví có đặc điểm gì?',1) on conflict(post_id,position) do nothing;
    end if;
  end loop;
end $$;
