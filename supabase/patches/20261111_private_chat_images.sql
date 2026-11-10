-- Private chat attachments; does not change the public post-images bucket.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('chat-images', 'chat-images', false, 5242880, array['image/jpeg','image/png','image/webp'])
on conflict (id) do nothing;

drop policy if exists "chat-images: member read" on storage.objects;
create policy "chat-images: member read" on storage.objects
  for select to authenticated using (
    bucket_id = 'chat-images'
    and exists (select 1 from public.profiles me where me.id=auth.uid() and not me.is_banned)
    and exists (select 1 from public.conversations c
      where c.id::text = (storage.foldername(name))[1]
        and auth.uid() in (c.post_author_id,c.claimant_id))
  );

drop policy if exists "chat-images: member upload" on storage.objects;
create policy "chat-images: member upload" on storage.objects
  for insert to authenticated with check (
    bucket_id = 'chat-images'
    and char_length(name) <= 200
    and name ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}/[A-Za-z0-9_-]{1,80}[.](jpg|jpeg|png|webp)$'
    and (storage.foldername(name))[2] = auth.uid()::text
    and exists (select 1 from public.profiles me where me.id=auth.uid() and not me.is_banned)
    and exists (select 1 from public.conversations c
      where c.id::text = (storage.foldername(name))[1]
        and not c.is_locked and auth.uid() in (c.post_author_id,c.claimant_id))
  );
