-- =====================================================================
--  LOST & FOUND - DATABASE SCHEMA (Supabase / PostgreSQL)
--  Chạy trong Supabase Dashboard > SQL Editor, hoặc đặt vào
--  supabase/migrations/0001_init.sql rồi `supabase db push`.
--
--  CÁCH CHẠY:
--    * Project Supabase mới/trống : chạy file này một lần.
--    * Đã chạy dở hoặc bị lỗi     : chạy supabase/reset.sql trước, rồi chạy lại file này.
--
--  Quy ước:
--    * Đọc dữ liệu: app Android / web admin đọc trực tiếp qua RLS.
--    * Ghi dữ liệu nghiệp vụ (tạo bài, xác minh, DONE, report, admin):
--      đi qua FastAPI (dùng secret key / service_role, bỏ qua RLS).
--    * Ngoại lệ ghi trực tiếp từ client: gửi tin nhắn, đánh dấu đã đọc,
--      cập nhật display_name/avatar, upload ảnh lên Storage.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. EXTENSIONS
-- ---------------------------------------------------------------------
create extension if not exists pg_trgm  with schema extensions;
create extension if not exists unaccent with schema extensions;

-- unaccent() mặc định không IMMUTABLE nên không dùng được trong generated column.
-- Bọc lại để tìm kiếm tiếng Việt không dấu: "vi" khớp "ví".
create or replace function public.f_unaccent(text)
returns text
language sql immutable parallel safe strict
set search_path = ''
as $$ select extensions.unaccent('extensions.unaccent'::regdictionary, $1) $$;

-- ---------------------------------------------------------------------
-- 1. ENUM TYPES
-- ---------------------------------------------------------------------
create type public.user_role     as enum ('USER', 'ADMIN');
create type public.post_type     as enum ('LOST', 'FOUND');
-- ARCHIVED = người dùng tự ẩn (có nút "Hiện lại"), REMOVED = admin gỡ, DELETED = người dùng xóa (xóa mềm)
create type public.post_status   as enum ('ACTIVE', 'RESOLVED', 'EXPIRED', 'ARCHIVED', 'REMOVED', 'DELETED');
create type public.claim_status  as enum ('SUBMITTED', 'PASSED', 'FAILED', 'CANCELLED', 'COMPLETED', 'CLOSED');
create type public.message_type  as enum ('TEXT', 'IMAGE', 'SYSTEM');
create type public.report_reason as enum ('SPAM', 'SCAM', 'FALSE_INFO', 'MONEY_REQUEST', 'VIOLATION', 'OTHER');
create type public.report_status as enum ('PENDING', 'REVIEWING', 'RESOLVED', 'DISMISSED');
create type public.notification_type as enum (
  'CLAIM_RECEIVED', 'CLAIM_PASSED', 'CLAIM_FAILED', 'HANDOVER_WAITING',
  'POST_DONE', 'REPORT_UPDATED', 'POST_REMOVED', 'POST_EXPIRED', 'SYSTEM'
);
create type public.admin_action_type as enum (
  'REMOVE_POST', 'RESTORE_POST', 'BAN_USER', 'UNBAN_USER',
  'RESOLVE_REPORT', 'DISMISS_REPORT', 'VIEW_CONVERSATION'
);

-- ---------------------------------------------------------------------
-- 2. USERS
--    auth.users (Supabase Auth) giữ email-alias + password đã hash (bcrypt).
--    profiles = thông tin công khai.  user_private = thông tin KHÔNG công khai.
-- ---------------------------------------------------------------------
create table public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  username     text not null unique
               check (username ~ '^[a-z0-9_]{4,20}$'),      -- luôn lưu chữ thường
  display_name text check (char_length(display_name) between 1 and 50),
  avatar_url   text,
  bio          text check (char_length(bio) <= 160),            -- "Giới thiệu ngắn" ở màn Chỉnh sửa hồ sơ
  role         public.user_role not null default 'USER',
  is_banned    boolean not null default false,
  banned_at    timestamptz,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table public.user_private (
  user_id    uuid primary key references public.profiles (id) on delete cascade,
  phone      text not null check (phone ~ '^(0|\+84)[0-9]{9,10}$'),
  created_at timestamptz not null default now()
);

-- Ghi chú: Figma đăng nhập bằng EMAIL. Email nằm ở auth.users (KHÔNG công khai).
-- profiles.username là "handle" công khai (vd: thulan_k) do backend tự sinh khi đăng ký.
-- user_private chỉ có dòng khi người dùng đã nhập số điện thoại (màn Chỉnh sửa hồ sơ).

-- ---------------------------------------------------------------------
-- 3. CATEGORIES & TAGS
-- ---------------------------------------------------------------------
create table public.categories (
  id         smallint generated always as identity primary key,
  code       text not null unique,
  name       text not null,
  icon       text,
  sort_order smallint not null default 0
);

create table public.tags (
  id         integer generated always as identity primary key,
  name       text not null unique check (name = lower(btrim(name)) and char_length(name) between 1 and 30),
  created_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 4. POSTS
-- ---------------------------------------------------------------------
create table public.posts (
  id            uuid primary key default gen_random_uuid(),
  author_id     uuid not null references public.profiles (id) on delete cascade,
  type          public.post_type not null,
  title         text not null check (char_length(title) between 3 and 120),
  description   text check (char_length(description) <= 2000),
  category_id   smallint not null references public.categories (id),
  location_text text not null check (char_length(location_text) between 2 and 200),
  latitude      numeric(9, 6) check (latitude  between -90  and 90),
  longitude     numeric(9, 6) check (longitude between -180 and 180),
  event_time    timestamptz not null,                          -- lúc mất / lúc nhặt được
  status        public.post_status not null default 'ACTIVE',
  expires_at    timestamptz not null default (now() + interval '30 days'),
  resolved_at   timestamptz,
  removed_reason text,                                         -- lý do admin gỡ bài
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  -- cột phục vụ tìm kiếm: không dấu, chữ thường
  search_text   text generated always as (
                  lower(public.f_unaccent(title || ' ' || coalesce(description, '') || ' ' || location_text))
                ) stored,
  check ((latitude is null) = (longitude is null))
);

create table public.post_images (
  id           uuid primary key default gen_random_uuid(),
  post_id      uuid not null references public.posts (id) on delete cascade,
  storage_path text not null,                                  -- vd: <user_id>/<post_id>/1.jpg
  sort_order   smallint not null default 0,
  created_at   timestamptz not null default now(),
  unique (post_id, sort_order)
);

create table public.post_tags (
  post_id uuid    not null references public.posts (id) on delete cascade,
  tag_id  integer not null references public.tags (id)  on delete cascade,
  primary key (post_id, tag_id)
);

-- ---------------------------------------------------------------------
-- 5. XÁC MINH QUYỀN SỞ HỮU
--    Người đăng bài = "gatekeeper": đặt câu hỏi và chấm Pass/Fail.
--    Người còn lại = "claimant": trả lời để được mở chat.
--      Bài FOUND: claimant là người nhận là chủ đồ.
--      Bài LOST : claimant là người nói đã nhặt được đồ.
-- ---------------------------------------------------------------------
create table public.verification_questions (
  id            uuid primary key default gen_random_uuid(),
  post_id       uuid not null references public.posts (id) on delete cascade,
  question_text text not null check (char_length(question_text) between 3 and 300),
  position      smallint not null check (position between 1 and 3),   -- tối đa 3 câu
  created_at    timestamptz not null default now(),
  unique (post_id, position)
);

create table public.claims (
  id                    uuid primary key default gen_random_uuid(),
  post_id               uuid not null references public.posts (id) on delete cascade,
  post_author_id        uuid not null references public.profiles (id) on delete cascade, -- denormalize cho RLS nhanh
  claimant_id           uuid not null references public.profiles (id) on delete cascade,
  status                public.claim_status not null default 'SUBMITTED',
  decision_note         text check (char_length(decision_note) <= 500),
  decided_at            timestamptz,
  author_confirmed_at   timestamptz,       -- người đăng bài xác nhận đã trao/nhận
  claimant_confirmed_at timestamptz,       -- người kia xác nhận
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now()
);

-- Mỗi người chỉ có 1 claim "đang sống" trên 1 bài (FAILED/CANCELLED thì được thử lại,
-- số lần thử tối đa do FastAPI kiểm soát).
create unique index claims_one_active_per_user_post
  on public.claims (post_id, claimant_id)
  where status in ('SUBMITTED', 'PASSED');

create table public.claim_answers (
  claim_id    uuid not null references public.claims (id) on delete cascade,
  question_id uuid not null references public.verification_questions (id) on delete cascade,
  answer_text text not null check (char_length(answer_text) between 1 and 500),
  primary key (claim_id, question_id)
);

-- ---------------------------------------------------------------------
-- 6. CHAT RIÊNG
--    conversation được tạo TỰ ĐỘNG bởi trigger khi claim chuyển sang PASSED.
-- ---------------------------------------------------------------------
create table public.conversations (
  id              uuid primary key default gen_random_uuid(),
  claim_id        uuid not null unique references public.claims (id) on delete cascade,
  post_author_id  uuid not null references public.profiles (id) on delete cascade,
  claimant_id     uuid not null references public.profiles (id) on delete cascade,
  is_locked       boolean not null default false,   -- true khi claim kết thúc: chỉ đọc
  last_message_at timestamptz,
  created_at      timestamptz not null default now()
);

create table public.messages (
  id              bigint generated always as identity primary key,
  conversation_id uuid not null references public.conversations (id) on delete cascade,
  sender_id       uuid references public.profiles (id) on delete set null,   -- NULL = tin hệ thống
  type            public.message_type not null default 'TEXT',
  content         text,
  image_path      text,
  created_at      timestamptz not null default now(),
  read_at         timestamptz,
  check (type <> 'TEXT'   or (content is not null and char_length(btrim(content)) between 1 and 2000)),
  check (type <> 'IMAGE'  or image_path is not null),
  check (type <> 'SYSTEM' or sender_id is null)
);

-- ---------------------------------------------------------------------
-- 7. REPORT & ADMIN
-- ---------------------------------------------------------------------
create table public.reports (
  id                     uuid primary key default gen_random_uuid(),
  reporter_id            uuid not null references public.profiles (id) on delete cascade,
  target_post_id         uuid references public.posts (id)         on delete cascade,
  target_user_id         uuid references public.profiles (id)      on delete cascade,
  target_conversation_id uuid references public.conversations (id) on delete cascade,
  reason                 public.report_reason not null,
  description            text check (char_length(description) <= 1000),
  status                 public.report_status not null default 'PENDING',
  handled_by             uuid references public.profiles (id),
  handled_at             timestamptz,
  resolution_note        text,
  created_at             timestamptz not null default now(),
  -- đúng 1 đối tượng bị báo cáo
  check (num_nonnulls(target_post_id, target_user_id, target_conversation_id) = 1)
);

-- Nhật ký hành động của admin (bắt buộc ghi, kể cả khi admin xem chat bị report)
create table public.admin_actions (
  id          bigint generated always as identity primary key,
  admin_id    uuid not null references public.profiles (id),
  action      public.admin_action_type not null,
  target_type text not null check (target_type in ('POST', 'USER', 'REPORT', 'CONVERSATION')),
  target_id   uuid not null,
  report_id   uuid references public.reports (id) on delete set null,
  note        text,
  created_at  timestamptz not null default now()
);

-- Màn "Thông báo" (chuông ở Home): mục Mới / Trước đó, nút "Đọc hết", chấm xanh chưa đọc
create table public.notifications (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references public.profiles (id) on delete cascade,
  type            public.notification_type not null,
  title           text not null,
  body            text,
  post_id         uuid references public.posts (id)  on delete cascade,
  claim_id        uuid references public.claims (id) on delete cascade,
  read_at         timestamptz,
  created_at      timestamptz not null default now()
);

-- (Tuỳ chọn / mở rộng) token FCM để đẩy thông báo
create table public.device_tokens (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references public.profiles (id) on delete cascade,
  fcm_token  text not null unique,
  platform   text not null default 'android',
  updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 8. INDEXES
-- ---------------------------------------------------------------------
create index posts_feed_idx        on public.posts (status, type, created_at desc);
create index posts_author_idx      on public.posts (author_id, created_at desc);
create index posts_category_idx    on public.posts (category_id) where status = 'ACTIVE';
create index posts_event_time_idx  on public.posts (event_time desc) where status = 'ACTIVE';
create index posts_expiry_idx      on public.posts (expires_at) where status = 'ACTIVE';
create index posts_search_trgm_idx on public.posts using gin (search_text extensions.gin_trgm_ops);

create index post_images_post_idx  on public.post_images (post_id);
create index post_tags_tag_idx     on public.post_tags (tag_id);
create index vq_post_idx           on public.verification_questions (post_id);

create index claims_post_idx       on public.claims (post_id);
create index claims_claimant_idx   on public.claims (claimant_id, created_at desc);
create index claims_author_idx     on public.claims (post_author_id, created_at desc);

create index conv_author_idx       on public.conversations (post_author_id);
create index conv_claimant_idx     on public.conversations (claimant_id);
create index messages_conv_idx     on public.messages (conversation_id, id desc);

create index notifications_user_idx   on public.notifications (user_id, created_at desc);
create index notifications_unread_idx on public.notifications (user_id) where read_at is null;
create index reports_queue_idx     on public.reports (status, created_at);
create index admin_actions_idx     on public.admin_actions (created_at desc);

-- ---------------------------------------------------------------------
-- 9. FUNCTIONS & TRIGGERS  (bảo vệ các quy tắc nghiệp vụ ngay trong DB)
-- ---------------------------------------------------------------------

-- 9.1 updated_at tự cập nhật
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

create trigger trg_profiles_updated before update on public.profiles
  for each row execute function public.set_updated_at();
create trigger trg_posts_updated before update on public.posts
  for each row execute function public.set_updated_at();
create trigger trg_claims_updated before update on public.claims
  for each row execute function public.set_updated_at();

-- 9.2 Kiểm tra khi tạo claim: không tự claim bài của mình, bài phải ACTIVE
create or replace function public.claims_before_insert()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_author uuid;
  v_status public.post_status;
begin
  select author_id, status into v_author, v_status from public.posts where id = new.post_id;
  if not found then
    raise exception 'Post % does not exist', new.post_id;
  end if;
  if v_author = new.claimant_id then
    raise exception 'You cannot claim your own post';
  end if;
  if v_status <> 'ACTIVE' then
    raise exception 'Post is not active (status=%)', v_status;
  end if;
  new.post_author_id := v_author;
  return new;
end $$;

create trigger trg_claims_before_insert before insert on public.claims
  for each row execute function public.claims_before_insert();

-- 9.3 Khi cả hai bên đã xác nhận => COMPLETED (chặn lỗi logic từ phía backend)
create or replace function public.claims_before_update()
returns trigger language plpgsql as $$
begin
  if new.status = 'PASSED'
     and new.author_confirmed_at is not null
     and new.claimant_confirmed_at is not null then
    new.status := 'COMPLETED';
  end if;
  if new.status in ('PASSED', 'FAILED') and old.status = 'SUBMITTED' then
    new.decided_at := coalesce(new.decided_at, now());
  end if;
  return new;
end $$;

create trigger trg_claims_before_update before update on public.claims
  for each row execute function public.claims_before_update();

-- 9.4a Hàm tạo thông báo. KHÔNG cho client gọi (nếu không ai cũng spam thông báo được).
create or replace function public.notify(
  p_user uuid, p_type public.notification_type, p_title text, p_body text,
  p_post uuid default null, p_claim uuid default null
) returns void language sql security definer set search_path = public as $$
  insert into public.notifications (user_id, type, title, body, post_id, claim_id)
  values (p_user, p_type, p_title, p_body, p_post, p_claim)
$$;
revoke all on function public.notify(uuid, public.notification_type, text, text, uuid, uuid)
  from public, anon, authenticated;

-- 9.4b Có claim mới => báo cho người đăng bài ("Có yêu cầu xác minh mới")
create or replace function public.claims_after_insert()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_name  text;
  v_title text;
begin
  select coalesce(display_name, username) into v_name  from public.profiles where id = new.claimant_id;
  select title                            into v_title from public.posts    where id = new.post_id;
  perform public.notify(new.post_author_id, 'CLAIM_RECEIVED', 'Có yêu cầu xác minh mới',
                        v_name || ' đã gửi câu trả lời cho bài "' || v_title || '"', new.post_id, new.id);
  return new;
end $$;

create trigger trg_claims_after_insert after insert on public.claims
  for each row execute function public.claims_after_insert();

-- 9.4c Phản ứng khi claim thay đổi:
--     PASSED     -> tự tạo conversation + tin nhắn hệ thống + thông báo cho claimant
--     FAILED     -> thông báo cho claimant
--     1 bên xác nhận trao trả -> thông báo "Chờ bạn xác nhận" cho bên còn lại
--     COMPLETED  -> bài RESOLVED, các claim khác CLOSED, thông báo DONE cho cả hai
--     kết thúc   -> khóa conversation (chỉ đọc)
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

-- Lưu ý: KHÔNG dùng "after update of status": status có thể bị đổi bên trong trigger BEFORE
-- (khi cả hai xác nhận) mà câu UPDATE gốc không có cột status => trigger kiểu "of status" sẽ không chạy.
create trigger trg_claims_after_status after update on public.claims
  for each row execute function public.claims_after_status_change();

-- 9.5 Cập nhật last_message_at
create or replace function public.messages_after_insert()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  update public.conversations set last_message_at = new.created_at where id = new.conversation_id;
  return new;
end $$;

create trigger trg_messages_after_insert after insert on public.messages
  for each row execute function public.messages_after_insert();

-- 9.6 is_admin(): dùng trong RLS (SECURITY DEFINER để không bị RLS của profiles chặn vòng lặp)
create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and role = 'ADMIN' and not is_banned
  )
$$;

-- 9.7 Khoảng cách (km) theo công thức Haversine, không cần PostGIS.
--     App hiển thị "1.2 km" bằng cách tự tính từ GPS của máy; DB dùng hàm này để SẮP XẾP "Gần nhất".
create or replace function public.distance_km(
  lat1 double precision, lng1 double precision, lat2 double precision, lng2 double precision
) returns double precision language sql immutable parallel safe as $$
  select 2 * 6371 * asin(sqrt(
    power(sin(radians(lat2 - lat1) / 2), 2) +
    cos(radians(lat1)) * cos(radians(lat2)) * power(sin(radians(lng2 - lng1) / 2), 2)))
$$;

-- 9.8 Tìm kiếm + lọc (SECURITY INVOKER => vẫn áp dụng RLS của người gọi)
--     Khớp các bộ lọc ở màn "Tìm kiếm" và "Home" trong Figma:
--       Loại bài (Tất cả/Lost/Found), Danh mục, Thời gian đăng (Hôm nay / 7 ngày), Mới nhất / Gần nhất.
--     Ví dụ "Hôm nay": p_posted_after => date_trunc('day', now())   "7 ngày": now() - interval '7 days'
--     Tag lọc theo kiểu AND. Bài không có tọa độ xếp cuối khi sắp xếp "Gần nhất".
create or replace function public.search_posts(
  p_type         public.post_type default null,
  p_category     smallint         default null,
  p_query        text             default null,
  p_posted_after timestamptz      default null,
  p_tag_ids      integer[]        default null,
  p_sort         text             default 'NEWEST',      -- 'NEWEST' | 'NEAREST'
  p_lat          double precision default null,
  p_lng          double precision default null,
  p_limit        integer          default 20,
  p_offset       integer          default 0
)
returns setof public.posts
language sql stable security invoker
set search_path = public
as $$
  select p.*
  from public.posts p
  where p.status = 'ACTIVE'
    and (p_type         is null or p.type = p_type)
    and (p_category     is null or p.category_id = p_category)
    and (p_posted_after is null or p.created_at >= p_posted_after)
    and (p_query        is null or btrim(p_query) = '' or
         p.search_text like '%' ||
           replace(replace(replace(lower(public.f_unaccent(btrim(p_query))), '\', '\\'), '%', '\%'), '_', '\_')
         || '%')
    and (p_tag_ids      is null or cardinality(p_tag_ids) = 0 or
         (select count(*) from public.post_tags pt
           where pt.post_id = p.id and pt.tag_id = any (p_tag_ids)) = cardinality(p_tag_ids))
  order by
    case when p_sort = 'NEAREST' and p_lat is not null and p_lng is not null
         then public.distance_km(p_lat, p_lng, p.latitude::double precision, p.longitude::double precision)
    end asc nulls last,
    p.created_at desc
  limit least(greatest(p_limit, 1), 50)
  offset greatest(p_offset, 0)
$$;

-- 9.9 Ba con số ở khối "Cần xử lý" trên Home
create or replace function public.my_todo_counts()
returns table (pending_verifications bigint, unread_messages bigint, awaiting_confirmation bigint)
language sql stable security invoker
set search_path = public
as $$
  select
    -- "Yêu cầu xác minh chờ duyệt"
    (select count(*) from public.claims c
      where c.post_author_id = auth.uid() and c.status = 'SUBMITTED'),
    -- "Tin nhắn chưa đọc"
    (select count(*) from public.messages m
       join public.conversations cv on cv.id = m.conversation_id
      where m.read_at is null and m.type <> 'SYSTEM'
        and m.sender_id is distinct from auth.uid()
        and auth.uid() in (cv.post_author_id, cv.claimant_id)),
    -- "Chờ xác nhận trao trả": bên kia đã xác nhận, mình chưa
    (select count(*) from public.claims c
      where c.status = 'PASSED'
        and ((c.post_author_id = auth.uid() and c.author_confirmed_at   is null and c.claimant_confirmed_at is not null)
          or (c.claimant_id    = auth.uid() and c.claimant_confirmed_at is null and c.author_confirmed_at   is not null)))
$$;

-- ---------------------------------------------------------------------
-- 10. ROW LEVEL SECURITY
--     Bật RLS cho MỌI bảng. Không có policy INSERT/UPDATE/DELETE = client
--     không ghi được; FastAPI dùng secret/service_role nên bỏ qua RLS.
-- ---------------------------------------------------------------------
alter table public.profiles               enable row level security;
alter table public.user_private           enable row level security;
alter table public.categories             enable row level security;
alter table public.tags                   enable row level security;
alter table public.posts                  enable row level security;
alter table public.post_images            enable row level security;
alter table public.post_tags              enable row level security;
alter table public.verification_questions enable row level security;
alter table public.claims                 enable row level security;
alter table public.claim_answers          enable row level security;
alter table public.conversations          enable row level security;
alter table public.messages               enable row level security;
alter table public.reports                enable row level security;
alter table public.admin_actions          enable row level security;
alter table public.notifications          enable row level security;
alter table public.device_tokens          enable row level security;

-- Quyền cột: client chỉ được sửa vài cột an toàn
revoke update on public.profiles from authenticated, anon;
grant  update (display_name, avatar_url, bio) on public.profiles to authenticated;

revoke update on public.notifications from authenticated, anon;
grant  update (read_at) on public.notifications to authenticated;   -- nút "Đọc hết"

revoke update on public.messages from authenticated, anon;
grant  update (read_at) on public.messages to authenticated;

-- profiles: ai đã đăng nhập cũng xem được thông tin công khai; chỉ sửa hồ sơ của mình
create policy profiles_select on public.profiles
  for select to authenticated using (true);
create policy profiles_update_own on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- user_private: chỉ chủ tài khoản hoặc admin; KHÔNG ai khác thấy số điện thoại
create policy user_private_select on public.user_private
  for select to authenticated using (user_id = auth.uid() or public.is_admin());

-- danh mục & tag: đọc tự do khi đã đăng nhập
create policy categories_select on public.categories for select to authenticated using (true);
create policy tags_select       on public.tags       for select to authenticated using (true);

-- posts: thấy bài ACTIVE, bài của mình, bài mình đã claim, hoặc là admin
-- Bài DELETED (xóa mềm) chỉ admin thấy.
create policy posts_select on public.posts
  for select to authenticated using (
    status = 'ACTIVE'
    or public.is_admin()
    or (status <> 'DELETED' and author_id = auth.uid())
    or (status <> 'DELETED' and exists (select 1 from public.claims c
                                        where c.post_id = posts.id and c.claimant_id = auth.uid()))
  );

create policy post_images_select on public.post_images
  for select to authenticated using (exists (select 1 from public.posts p where p.id = post_id));
create policy post_tags_select on public.post_tags
  for select to authenticated using (exists (select 1 from public.posts p where p.id = post_id));

-- câu hỏi xác minh: CHỈ người đăng bài (và admin) đọc trực tiếp.
-- Claimant lấy câu hỏi qua FastAPI để có thể giới hạn số lần thử, tránh dò đáp án.
create policy vq_select on public.verification_questions
  for select to authenticated using (
    public.is_admin()
    or exists (select 1 from public.posts p where p.id = post_id and p.author_id = auth.uid())
  );

-- claims / claim_answers: người đăng bài + claimant + admin
create policy claims_select on public.claims
  for select to authenticated using (
    claimant_id = auth.uid() or post_author_id = auth.uid() or public.is_admin()
  );
create policy claim_answers_select on public.claim_answers
  for select to authenticated using (exists (select 1 from public.claims c where c.id = claim_id));

-- conversations / messages: CHỈ 2 người tham gia (admin đọc qua FastAPI và bị ghi log)
create policy conversations_select on public.conversations
  for select to authenticated using (auth.uid() in (post_author_id, claimant_id));

create policy messages_select on public.messages
  for select to authenticated using (
    exists (select 1 from public.conversations c
            where c.id = conversation_id and auth.uid() in (c.post_author_id, c.claimant_id))
  );

create policy messages_insert on public.messages
  for insert to authenticated with check (
    sender_id = auth.uid()
    and type in ('TEXT', 'IMAGE')
    and exists (select 1 from public.conversations c
                where c.id = conversation_id
                  and not c.is_locked
                  and auth.uid() in (c.post_author_id, c.claimant_id))
    and not exists (select 1 from public.profiles pr where pr.id = auth.uid() and pr.is_banned)
  );

-- chỉ người NHẬN mới được đánh dấu đã đọc
create policy messages_mark_read on public.messages
  for update to authenticated
  using (
    sender_id is distinct from auth.uid()
    and exists (select 1 from public.conversations c
                where c.id = conversation_id and auth.uid() in (c.post_author_id, c.claimant_id))
  )
  with check (true);

-- reports: người báo cáo xem report của mình; admin xem tất cả
create policy reports_select on public.reports
  for select to authenticated using (reporter_id = auth.uid() or public.is_admin());

create policy admin_actions_select on public.admin_actions
  for select to authenticated using (public.is_admin());

create policy notifications_select on public.notifications
  for select to authenticated using (user_id = auth.uid());
create policy notifications_mark_read on public.notifications
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy device_tokens_own on public.device_tokens
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- 11. REALTIME
--     Bật Realtime cho tin nhắn (và claims để app nhận biết Pass/Fail ngay).
-- ---------------------------------------------------------------------
do $$
declare
  t text;
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    foreach t in array array['messages', 'claims', 'notifications'] loop
      begin
        execute format('alter publication supabase_realtime add table public.%I', t);
      exception when duplicate_object then
        null;   -- đã có trong publication
      end;
    end loop;
  end if;
end $$;

-- ---------------------------------------------------------------------
-- 12. STORAGE (ảnh bài đăng)
--     Bucket public để hiển thị nhanh qua CDN; đường dẫn chứa UUID khó đoán.
--     Chỉ chủ tài khoản được upload/xóa trong thư mục <user_id>/...
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('post-images', 'post-images', true)
on conflict (id) do nothing;

-- Bucket public: ảnh xem được qua địa chỉ công khai mà KHÔNG cần policy SELECT.
-- Cố ý không tạo policy SELECT, để client không liệt kê được toàn bộ file trong bucket.
-- (Nếu trước đó đã có policy này: drop policy if exists "post-images: read" on storage.objects;)
drop policy if exists "post-images: read" on storage.objects;

drop policy if exists "post-images: upload to own folder" on storage.objects;
create policy "post-images: upload to own folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'post-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "post-images: delete own files" on storage.objects;
create policy "post-images: delete own files" on storage.objects
  for delete to authenticated
  using (bucket_id = 'post-images' and (storage.foldername(name))[1] = auth.uid()::text);

-- ---------------------------------------------------------------------
-- 13. SEED DATA
-- ---------------------------------------------------------------------
insert into public.categories (code, name, icon, sort_order) values
  ('WALLET',    'Ví / Bóp',                  'wallet',    1),
  ('PHONE',     'Điện thoại',                'phone',     2),
  ('DOCUMENT',  'Giấy tờ (CCCD, thẻ, bằng)', 'id-card',   3),
  ('KEYS',      'Chìa khóa',                 'key',       4),
  ('BAG',       'Túi / Balo',                'backpack',  5),
  ('ELECTRONIC','Thiết bị điện tử',          'laptop',    6),
  ('CLOTHING',  'Quần áo / Phụ kiện',        'shirt',     7),
  ('PET',       'Thú cưng',                  'paw',       8),
  ('OTHER',     'Khác',                      'package',   9)
on conflict (code) do nothing;

-- ---------------------------------------------------------------------
-- 14. (TUỲ CHỌN) TỰ ĐỘNG HẾT HẠN BÀI ĐĂNG - cần bật extension pg_cron
--     Dashboard > Database > Extensions > pg_cron, rồi chạy:
-- ---------------------------------------------------------------------
-- select cron.schedule(
--   'expire-posts', '0 * * * *',
--   $$ update public.posts set status = 'EXPIRED'
--      where status = 'ACTIVE' and expires_at < now() $$
-- );

-- ---------------------------------------------------------------------
-- 15. TẠO TÀI KHOẢN ADMIN ĐẦU TIÊN
--     1) Đăng ký một tài khoản bình thường qua app (hoặc POST /auth/register).
--     2) Chạy lệnh sau trong SQL Editor, thay email bằng email của admin:
-- ---------------------------------------------------------------------
-- update public.profiles set role = 'ADMIN'
--  where id = (select id from auth.users where email = 'admin@example.com');
