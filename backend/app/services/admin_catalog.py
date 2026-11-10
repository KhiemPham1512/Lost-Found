"""Admin read interface: bounded lists, private-field projection and statistics."""

import psycopg
from psycopg.rows import dict_row

LOCAL_DAY = "date_trunc('day', now() at time zone 'Asia/Ho_Chi_Minh') at time zone 'Asia/Ho_Chi_Minh'"


def _page(conn, query, where, params, limit, offset):
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            f"select count(*) as total from ({query}) catalog where {where}", params
        )
        total = cur.fetchone()["total"]
        cur.execute(
            f"select * from ({query}) catalog where {where} order by created_at desc, id desc limit %s offset %s",
            [*params, limit, offset],
        )
        return {"total": total, "items": cur.fetchall()}


def list_posts(conn: psycopg.Connection, type, status, q, limit, offset):
    clauses, params = ["true"], []
    for name, value in (("type", type), ("status", status)):
        if value:
            clauses.append(f"{name} = %s")
            params.append(value)
    if q and q.strip():
        clauses.append("position(lower(public.f_unaccent(%s)) in search_text) > 0")
        params.append(q.strip())
    return _page(
        conn,
        """select p.id,p.type,p.title,p.status,p.created_at,p.expires_at,
        p.author_id,u.username as author_username,p.search_text,
        c.name as category_name,
        (select count(*) from public.reports r where r.target_post_id=p.id) as report_count
        from public.posts p join public.profiles u on u.id=p.author_id
        join public.categories c on c.id=p.category_id""",
        " and ".join(clauses),
        params,
        limit,
        offset,
    )


def list_users(conn: psycopg.Connection, status, q, limit, offset):
    clauses, params = ["true"], []
    if status == "ACTIVE":
        clauses.append("not is_banned")
    elif status == "BANNED":
        clauses.append("is_banned")
    elif status == "REPORTED":
        clauses.append("report_count >= 3")
    if q and q.strip():
        clauses.append(
            "position(lower(public.f_unaccent(%s)) in lower(public.f_unaccent(username || ' ' || coalesce(display_name,'')))) > 0"
        )
        params.append(q.strip())
    return _page(
        conn,
        """select u.id,u.username,u.display_name,u.role,u.is_banned,u.created_at,
        to_char(u.created_at at time zone 'Asia/Ho_Chi_Minh','MM/YYYY') as joined_month,
        case when v.phone is null then null else left(v.phone,2) || '•• ••• ' || right(v.phone,3) end as phone_masked,
        (select count(*) from public.posts p where p.author_id=u.id) as post_count,
        (select count(*) from public.reports r where r.target_user_id=u.id
          or r.target_post_id in (select id from public.posts where author_id=u.id)) as report_count
        from public.profiles u left join public.user_private v on v.user_id=u.id""",
        " and ".join(clauses),
        params,
        limit,
        offset,
    )


def list_actions(conn: psycopg.Connection, target_type, range, q, limit, offset):
    clauses, params = ["true"], []
    if target_type:
        clauses.append("target_type=%s")
        params.append(target_type)
    if range:
        days = {"today": 0, "7d": 6, "30d": 29}[range]
        clauses.append(f"created_at >= ({LOCAL_DAY}) - interval '{days} days'")
    if q and q.strip():
        clauses.append(
            "position(lower(public.f_unaccent(%s)) in lower(public.f_unaccent(concat_ws(' ',admin_username,action,target_label,report_code,note)))) > 0"
        )
        params.append(q.strip())
    return _page(
        conn,
        """select a.id,a.admin_id,u.username as admin_username,a.action,a.target_type,
        a.target_id,a.note,a.created_at,a.report_id,
        case when r.report_no is not null then 'R-' || r.report_no end as report_code,
        coalesce(p.title,t.username,a.target_id::text) as target_label
        from public.admin_actions a join public.profiles u on u.id=a.admin_id
        left join public.reports r on r.id=a.report_id
        left join public.posts p on a.target_type='POST' and p.id=a.target_id
        left join public.profiles t on a.target_type='USER' and t.id=a.target_id""",
        " and ".join(clauses),
        params,
        limit,
        offset,
    )


def get_stats(conn: psycopg.Connection):
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(f"""select
          count(*) filter(where status in ('PENDING','REVIEWING')) as pending,
          count(*) filter(where created_at < ({LOCAL_DAY})
            and (status in ('PENDING','REVIEWING') or handled_at >= ({LOCAL_DAY}))) as yesterday
          from public.reports""")
        reports = cur.fetchone()
        assert reports is not None
        cur.execute(f"""select count(*) as total,
          count(*) filter(where created_at >= ({LOCAL_DAY}) - interval '6 days') as new_this_week
          from public.profiles""")
        users = cur.fetchone()
        assert users is not None
        cur.execute(f"""select count(*) filter(where status='ACTIVE' and type='LOST') as lost,
          count(*) filter(where status='ACTIVE' and type='FOUND') as found,
          count(*) filter(where status='RESOLVED' and resolved_at >= ({LOCAL_DAY}) - interval '6 days') as completed,
          count(*) filter(where status='RESOLVED') as resolved,
          count(*) filter(where status <> 'DELETED') as total from public.posts""")
        posts = cur.fetchone()
        assert posts is not None
        cur.execute("""select d.day::date as date,
          count(p.id) filter(where p.type='LOST') as "LOST",
          count(p.id) filter(where p.type='FOUND') as "FOUND"
          from generate_series((now() at time zone 'Asia/Ho_Chi_Minh')::date - 6,
                               (now() at time zone 'Asia/Ho_Chi_Minh')::date, interval '1 day') d(day)
          left join public.posts p on (p.created_at at time zone 'Asia/Ho_Chi_Minh')::date=d.day::date
          group by d.day order by d.day""")
        days = cur.fetchall()
        cur.execute("""select r.id,'R-' || r.report_no as code,r.reason,r.status,r.created_at,
            case when r.target_post_id is not null then 'POST' when r.target_user_id is not null then 'USER' else 'CONVERSATION' end as target_type,
            case when r.target_post_id is not null then p.title
                 when r.target_user_id is not null then u.username
                 else ca.username || ' ↔ ' || cc.username end as target_label
            from public.reports r
            left join public.posts p on p.id=r.target_post_id
            left join public.profiles u on u.id=r.target_user_id
            left join public.conversations cv on cv.id=r.target_conversation_id
            left join public.profiles ca on ca.id=cv.post_author_id
            left join public.profiles cc on cc.id=cv.claimant_id
            order by r.created_at desc,r.id desc limit 5""")
        recent = cur.fetchall()
    return {
        "pending_reports": reports["pending"],
        "pending_delta": reports["pending"] - reports["yesterday"],
        "active_posts": {"LOST": posts["lost"], "FOUND": posts["found"]},
        "users": users,
        "completed_this_week": posts["completed"],
        "completion_rate": round(100 * posts["resolved"] / posts["total"], 2)
        if posts["total"]
        else 0.0,
        "posts_by_day": days,
        "recent_reports": recent,
    }
