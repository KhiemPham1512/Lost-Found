import re
from uuid import UUID

import psycopg
from fastapi import HTTPException

from app.db import require_row
from app.schemas.admin import ResolveRequest
from app.services.moderation import (
    ban_user,
    log_action,
    notify,
    remove_post,
    set_auth_ban,
)

_TARGET_TYPE_SQL = (
    "case when r.target_post_id is not null then 'POST' "
    "when r.target_user_id is not null then 'USER' else 'CONVERSATION' end"
)
_FROM = """
    from public.reports r
    join public.profiles rp on rp.id = r.reporter_id
    left join public.posts p on p.id = r.target_post_id
    left join public.profiles tu on tu.id = r.target_user_id
    left join public.conversations cv on cv.id = r.target_conversation_id
    left join public.profiles ca on ca.id = cv.post_author_id
    left join public.profiles cc on cc.id = cv.claimant_id
"""
# Cùng đối tượng: so sánh cả ba cột target (đúng một cột có giá trị)
_SAME_TARGET = (
    "r2.target_post_id is not distinct from r.target_post_id "
    "and r2.target_user_id is not distinct from r.target_user_id "
    "and r2.target_conversation_id is not distinct from r.target_conversation_id"
)
_LIKE = "'%%' || replace(replace(replace(lower(public.f_unaccent(%s)), '\\', '\\\\'), '%%', '\\%%'), '_', '\\_') || '%%'"


def list_reports(
    conn: psycopg.Connection,
    status: str | None,
    target_type: str | None,
    q: str | None,
    limit: int,
    offset: int,
) -> dict:
    where: list[str] = []
    params: list = []

    if status:
        where.append("r.status = %s")
        params.append(status)
    if target_type:
        where.append(f"({_TARGET_TYPE_SQL}) = %s")
        params.append(target_type)
    if q and q.strip():
        q = q.strip()
        m = re.fullmatch(r"(?i)(?:r-?)?(\d{1,12})", q)
        if m:  # tìm theo mã báo cáo: R-1042, r1042 hoặc chỉ 1042
            where.append("r.report_no = %s")
            params.append(int(m.group(1)))
        else:
            like = _LIKE
            where.append(
                "("
                f"lower(public.f_unaccent(coalesce(p.title, ''))) like {like} "
                f"or lower(coalesce(tu.username, '')) like {like} "
                f"or lower(coalesce(rp.username, '')) like {like}"
                ")"
            )
            params.extend([q, q, q])

    where_sql = ("where " + " and ".join(where)) if where else ""

    (total,) = require_row(
        conn.execute(f"select count(*) {_FROM} {where_sql}", params).fetchone()
    )

    rows = conn.execute(
        f"""
        select r.id, r.report_no, {_TARGET_TYPE_SQL} as target_type,
               coalesce(r.target_post_id, r.target_user_id, r.target_conversation_id) as target_id,
               case when r.target_post_id is not null then p.title
                    when r.target_user_id is not null then tu.username
                    else ca.username || ' ↔ ' || cc.username end as target_label,
               r.reason, r.status, rp.id, rp.username, r.created_at,
               (select count(*) from public.reports r2 where {_SAME_TARGET}) as same_target_count
        {_FROM} {where_sql}
        order by (r.status in ('PENDING', 'REVIEWING')) desc, r.created_at desc
        limit %s offset %s
        """,
        [*params, limit, offset],
    ).fetchall()

    items = [
        {
            "id": str(r[0]),
            "code": f"R-{r[1]}",
            "target_type": r[2],
            "target_id": str(r[3]),
            "target_label": r[4] or "",
            "reason": r[5],
            "status": r[6],
            "reporter": {"id": str(r[7]), "username": r[8]},
            "created_at": r[9],
            "same_target_count": r[10],
        }
        for r in rows
    ]
    return {"items": items, "total": total}


def _author_info(conn: psycopg.Connection, user_id, exclude_report_id) -> dict | None:
    row = conn.execute(
        "select id, username, display_name, is_banned, created_at from public.profiles where id = %s",
        (user_id,),
    ).fetchone()
    if row is None:
        return None
    (post_count,) = require_row(
        conn.execute(
            "select count(*) from public.posts where author_id = %s and status <> 'DELETED'",
            (user_id,),
        ).fetchone()
    )
    (reported_before,) = require_row(
        conn.execute(
            """
        select count(*) from public.reports r2
        where r2.id <> %s and (
              r2.target_user_id = %s
           or r2.target_post_id in (select id from public.posts where author_id = %s))
        """,
            (exclude_report_id, user_id, user_id),
        ).fetchone()
    )
    return {
        "id": str(row[0]),
        "username": row[1],
        "display_name": row[2],
        "is_banned": row[3],
        "joined_at": row[4],
        "post_count": post_count,
        "reported_before_count": reported_before,
    }


def _conversation_summary(conn: psycopg.Connection, conv_id) -> dict | None:
    row = conn.execute(
        """
        select cv.id, ca.username, cc.username, cv.is_locked, cv.last_message_at,
               (select count(*) from public.messages m where m.conversation_id = cv.id)
        from public.conversations cv
        join public.profiles ca on ca.id = cv.post_author_id
        join public.profiles cc on cc.id = cv.claimant_id
        where cv.id = %s
        """,
        (conv_id,),
    ).fetchone()
    if row is None:
        return None
    return {
        "id": str(row[0]),
        "participants": [row[1], row[2]],
        "is_locked": row[3],
        "last_message_at": row[4],
        "message_count": row[5],
    }


def get_report_detail(conn: psycopg.Connection, report_id: UUID) -> dict:
    r = conn.execute(
        """
        select r.id, r.report_no, r.reason, r.description, r.status, r.created_at,
               r.handled_at, r.resolution_action, hb.id, hb.username,
               rp.id, rp.username, rp.display_name,
               r.target_post_id, r.target_user_id, r.target_conversation_id
        from public.reports r
        join public.profiles rp on rp.id = r.reporter_id
        left join public.profiles hb on hb.id = r.handled_by
        where r.id = %s
        """,
        (report_id,),
    ).fetchone()
    if r is None:
        raise HTTPException(status_code=404, detail="Không tìm thấy báo cáo")
    (
        rid,
        report_no,
        reason,
        description,
        status,
        created_at,
        handled_at,
        resolution_action,
        hb_id,
        hb_name,
        rp_id,
        rp_name,
        rp_display,
        post_id,
        user_id,
        conv_id,
    ) = r

    detail: dict = {
        "id": str(rid),
        "code": f"R-{report_no}",
        "reason": reason,
        "description": description,
        "status": status,
        "created_at": created_at,
        "handled_at": handled_at,
        "resolution_action": resolution_action,
        "resolution_note": None,
        "handled_by": {"id": str(hb_id), "username": hb_name} if hb_id else None,
        "reporter": {"id": str(rp_id), "username": rp_name, "display_name": rp_display},
        "target": {"type": None, "post": None, "user": None, "conversation": None},
    }

    related_conv_id = None
    if post_id:
        detail["target"]["type"] = "POST"
        p = conn.execute(
            """
            select p.id, p.type, p.title, p.description, p.status, p.location_text, p.event_time,
                   p.created_at, p.author_id, c.id, c.name
            from public.posts p join public.categories c on c.id = p.category_id
            where p.id = %s
            """,
            (post_id,),
        ).fetchone()
        if p:
            tags = [
                x[0]
                for x in conn.execute(
                    "select t.name from public.post_tags pt join public.tags t on t.id = pt.tag_id "
                    "where pt.post_id = %s order by t.name",
                    (post_id,),
                ).fetchall()
            ]
            images = [
                x[0]
                for x in conn.execute(
                    "select storage_path from public.post_images where post_id = %s order by sort_order",
                    (post_id,),
                ).fetchall()
            ]
            detail["target"]["post"] = {
                "id": str(p[0]),
                "type": p[1],
                "title": p[2],
                "description": p[3],
                "status": p[4],
                "location_text": p[5],
                "event_time": p[6],
                "created_at": p[7],
                "category": {"id": p[9], "name": p[10]},
                "tags": tags,
                "image_paths": images,
                "author": _author_info(conn, p[8], rid),
            }
            # Hội thoại giữa người báo cáo và chủ bài về chính bài này (nếu có)
            row = conn.execute(
                """
                select cv.id from public.conversations cv
                join public.claims cl on cl.id = cv.claim_id
                where cl.post_id = %s and (cv.claimant_id = %s or cv.post_author_id = %s)
                order by cv.created_at desc limit 1
                """,
                (post_id, rp_id, rp_id),
            ).fetchone()
            related_conv_id = row[0] if row else None
    elif user_id:
        detail["target"]["type"] = "USER"
        detail["target"]["user"] = _author_info(conn, user_id, rid)
        row = conn.execute(
            """
            select id from public.conversations
            where (post_author_id = %s and claimant_id = %s) or (post_author_id = %s and claimant_id = %s)
            order by created_at desc limit 1
            """,
            (rp_id, user_id, user_id, rp_id),
        ).fetchone()
        related_conv_id = row[0] if row else None
    else:
        detail["target"]["type"] = "CONVERSATION"
        related_conv_id = conv_id

    detail["target"]["conversation"] = (
        _conversation_summary(conn, related_conv_id) if related_conv_id else None
    )

    # Mọi báo cáo về cùng đối tượng (kể cả báo cáo này), mới nhất trước
    rows = conn.execute(
        f"""
        select r2.id, r2.report_no, r2.reason, r2.description, r2.status, r2.created_at, u.username
        from public.reports r2
        join public.profiles u on u.id = r2.reporter_id
        join public.reports r on r.id = %s
        where {_SAME_TARGET}
        order by r2.created_at desc
        """,
        (report_id,),
    ).fetchall()
    detail["reports_on_target"] = [
        {
            "id": str(x[0]),
            "code": f"R-{x[1]}",
            "reason": x[2],
            "description": x[3],
            "status": x[4],
            "created_at": x[5],
            "reporter_username": x[6],
            "is_current": x[0] == rid,
        }
        for x in rows
    ]

    # Nhật ký xử lý: mọi hành động liên quan tới các báo cáo cùng đối tượng
    rows = conn.execute(
        f"""
        select a.id, a.action, a.note, a.created_at, u.username
        from public.admin_actions a
        join public.profiles u on u.id = a.admin_id
        where a.report_id in (
            select r2.id from public.reports r2 join public.reports r on r.id = %s where {_SAME_TARGET})
        order by a.created_at desc, a.id desc
        """,
        (report_id,),
    ).fetchall()
    detail["admin_actions"] = [
        {
            "id": x[0],
            "action": x[1],
            "note": x[2],
            "created_at": x[3],
            "admin_username": x[4],
        }
        for x in rows
    ]
    # Ghi chú kết quả = ghi chú của hành động xử lý gần nhất (không tính "đang xem xét")
    for a in detail["admin_actions"]:
        if a["action"] in (
            "REMOVE_POST",
            "BAN_USER",
            "RESOLVE_REPORT",
            "DISMISS_REPORT",
        ):
            detail["resolution_note"] = a["note"]
            break
    return detail


def _lock_report(conn: psycopg.Connection, report_id: UUID) -> tuple:
    row = conn.execute(
        "select report_no, status, target_post_id, target_user_id, target_conversation_id "
        "from public.reports where id = %s for update",
        (report_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Không tìm thấy báo cáo")
    return row


def _open_reports_same_target(
    conn: psycopg.Connection, report_id: UUID, statuses: tuple
) -> list:
    """Các báo cáo cùng đối tượng đang ở một trong các trạng thái `statuses` (và khóa chúng lại)."""
    return conn.execute(
        f"""
        select r2.id, r2.reporter_id from public.reports r2
        join public.reports r on r.id = %s
        where {_SAME_TARGET} and r2.status = any(%s)
        order by r2.created_at
        for update of r2
        """,
        (report_id, list(statuses)),
    ).fetchall()


def review_report(conn: psycopg.Connection, admin_id: str, report_id: UUID) -> dict:
    report_no, status, *_ = _lock_report(conn, report_id)
    code = f"R-{report_no}"
    if status == "REVIEWING":  # gọi lại không gây lỗi
        return {
            "report_id": str(report_id),
            "code": code,
            "status": status,
            "affected_reports": 0,
        }
    if status != "PENDING":
        raise HTTPException(status_code=409, detail="Báo cáo này đã được xử lý")

    rows = _open_reports_same_target(conn, report_id, ("PENDING",))
    ids = [r[0] for r in rows]
    conn.execute(
        "update public.reports set status = 'REVIEWING', handled_by = %s where id = any(%s)",
        (admin_id, ids),
    )
    log_action(conn, admin_id, "REVIEW_REPORT", "REPORT", report_id, report_id, None)
    return {
        "report_id": str(report_id),
        "code": code,
        "status": "REVIEWING",
        "affected_reports": len(ids),
    }


_OUTCOME_TEXT = {
    "REMOVE_POST": "Bài đăng bị báo cáo đã được gỡ",
    "BAN_USER": "Tài khoản vi phạm đã bị khóa",
    "KEEP": "Đã xem xét, nội dung được giữ nguyên",
    "DISMISS": "Không phát hiện vi phạm",
}


def resolve_report(
    conn: psycopg.Connection, admin_id: str, report_id: UUID, body: ResolveRequest
) -> dict:
    report_no, status, post_id, user_id, conv_id = _lock_report(conn, report_id)
    code = f"R-{report_no}"
    if status not in ("PENDING", "REVIEWING"):
        raise HTTPException(status_code=409, detail="Báo cáo này đã được xử lý")

    banned_user_id = None
    action = body.action

    if action == "REMOVE_POST":
        if not post_id:
            raise HTTPException(
                status_code=400, detail="Báo cáo này không nhắm vào một bài đăng"
            )
        remove_post(conn, admin_id, post_id, body.note, report_id)

    elif action == "BAN_USER":
        if post_id:
            (banned_user_id,) = require_row(
                conn.execute(
                    "select author_id from public.posts where id = %s", (post_id,)
                ).fetchone()
            )
        elif user_id:
            banned_user_id = user_id
        else:  # hội thoại: admin phải chọn khóa ai trong hai người
            if body.user_id is None:
                raise HTTPException(
                    status_code=422,
                    detail="Cần chọn người bị khóa (user_id) khi xử lý báo cáo hội thoại",
                )
            row = conn.execute(
                "select 1 from public.conversations where id = %s and (post_author_id = %s or claimant_id = %s)",
                (conv_id, body.user_id, body.user_id),
            ).fetchone()
            if row is None:
                raise HTTPException(
                    status_code=400,
                    detail="Người này không thuộc cuộc trò chuyện bị báo cáo",
                )
            banned_user_id = body.user_id
        ban_user(conn, admin_id, banned_user_id, body.note, report_id)

    elif action == "KEEP":
        log_action(
            conn, admin_id, "RESOLVE_REPORT", "REPORT", report_id, report_id, body.note
        )
    else:  # DISMISS
        log_action(
            conn, admin_id, "DISMISS_REPORT", "REPORT", report_id, report_id, body.note
        )

    # Quyết định áp dụng cho MỌI báo cáo đang mở về cùng đối tượng
    new_status = "DISMISSED" if action == "DISMISS" else "RESOLVED"
    rows = _open_reports_same_target(conn, report_id, ("PENDING", "REVIEWING"))
    ids = [r[0] for r in rows]
    # KHÔNG ghi body.note vào cột resolution_note: người báo cáo đọc được dòng báo cáo của mình qua RLS,
    # còn ghi chú của admin là nội bộ (chỉ nằm trong admin_actions, mà chỉ admin đọc được)
    conn.execute(
        "update public.reports set status = %s, resolution_action = %s, "
        "handled_by = %s, handled_at = now() where id = any(%s)",
        (new_status, action, admin_id, ids),
    )
    for _rid, reporter_id in rows:
        notify(
            conn,
            reporter_id,
            "REPORT_UPDATED",
            "Báo cáo của bạn đã được xử lý",
            _OUTCOME_TEXT[action],
        )

    login_blocked = set_auth_ban(banned_user_id, True) if banned_user_id else None
    return {
        "report_id": str(report_id),
        "code": code,
        "status": new_status,
        "resolution_action": action,
        "affected_reports": len(ids),
        "banned_user_id": str(banned_user_id) if banned_user_id else None,
        "login_blocked": login_blocked,
    }
