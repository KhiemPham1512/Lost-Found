-- =====================================================================
--  XEM NHANH AI LÀ ADMIN (CHỈ DÙNG KHI PHÁT TRIỂN)
--  Vai trò nằm ở bảng public.profiles, còn email nằm ở auth.users,
--  nên trang Authentication > Users không hiện vai trò. Câu lệnh dưới ghép hai bảng lại.
--  Mẹo: bấm "Save query" trong SQL Editor để lần sau mở ra là chạy luôn.
-- =====================================================================

-- A) Danh sách mọi tài khoản: admin xếp trên cùng
select
  u.email,
  p.username,
  p.role,
  p.is_banned                         as bi_khoa,
  (p.id is not null)                  as co_ho_so,      -- false = tài khoản tạo từ dashboard, chưa có hồ sơ
  u.created_at::date                  as ngay_tao
from auth.users u
left join public.profiles p on p.id = u.id
order by (p.role = 'ADMIN') desc nulls last, u.created_at;

-- B) Nâng một tài khoản lên ADMIN (thay email) và xem kết quả ngay
-- update public.profiles set role = 'ADMIN'
--  where id = (select id from auth.users where email = 'email-cua-ban@example.com')
-- returning username, role;

-- C) Hạ một admin về người dùng thường
-- update public.profiles set role = 'USER'
--  where id = (select id from auth.users where email = 'email-cua-ban@example.com')
-- returning username, role;
