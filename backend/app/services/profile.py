import psycopg


def save_phone(conn: psycopg.Connection, user_id: str, phone: str) -> None:
    """Lưu số điện thoại vào bảng riêng tư. Có rồi thì cập nhật (upsert)."""
    conn.execute(
        """
        insert into public.user_private (user_id, phone)
        values (%s, %s)
        on conflict (user_id) do update set phone = excluded.phone
        """,
        (user_id, phone),
    )