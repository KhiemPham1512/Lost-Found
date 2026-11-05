"""Provision five dedicated demo accounts and ten posts, without resetting existing data.

Run with backend/.venv/Scripts/python.exe dev/seed_demo.py from the repo root.
Passwords are generated once and saved only to the gitignored local account file.
"""

import json
import secrets
from pathlib import Path

import httpx
import psycopg
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "dev/demo-accounts.local.json"


def main():
    env = dotenv_values(ROOT / "backend/.env")
    if CONFIG.exists():
        config = json.loads(CONFIG.read_text(encoding="utf-8"))
    else:
        source = ROOT / "dev/accounts.local.json"
        local = (
            json.loads(source.read_text(encoding="utf-8-sig"))
            if source.exists()
            else {}
        )
        suffix = secrets.token_hex(5)
        config = {
            "supabase_url": env["SUPABASE_URL"],
            "publishable_key": local.get("publishable_key", ""),
            "accounts": {
                name: {
                    "email": f"lf-demo-{suffix}-{i}@example.com",
                    "password": secrets.token_urlsafe(24),
                }
                for i, name in enumerate(
                    ("demo_admin", "demo_a", "demo_b", "demo_c", "demo_d")
                )
            },
        }
        CONFIG.write_text(json.dumps(config, indent=2), encoding="utf-8")
    if config["supabase_url"].rstrip("/") != env["SUPABASE_URL"].rstrip("/"):
        raise RuntimeError("Demo configuration belongs to a different project")
    ids = []
    with (
        psycopg.connect(env["DATABASE_URL"], connect_timeout=10) as conn,
        httpx.Client(
            base_url=env["SUPABASE_URL"],
            headers={"apikey": env["SUPABASE_SECRET_KEY"]},
            timeout=20,
        ) as auth,
    ):
        for i, account in enumerate(config["accounts"].values()):
            row = conn.execute(
                "select id from auth.users where email=%s", (account["email"],)
            ).fetchone()
            if row:
                uid = str(row[0])
            else:
                result = auth.post(
                    "/auth/v1/admin/users", json={**account, "email_confirm": True}
                )
                if result.status_code not in (200, 201):
                    raise RuntimeError(
                        f"Auth provisioning failed ({result.status_code}); no secrets logged"
                    )
                uid = result.json()["id"]
            # Stable short handles; never modify an existing user's role or password.
            conn.execute(
                """insert into public.profiles(id,username,display_name,role)
                values(%s,%s,%s,%s) on conflict(id) do nothing""",
                (
                    uid,
                    "lf_demo_" + str(i) + "_" + uid.replace("-", "")[:8],
                    f"Demo {i + 1}",
                    "ADMIN" if i == 0 else "USER",
                ),
            )
            ids.append(uid)
        conn.execute(
            "select set_config('app.demo_user_ids',%s,true)", (json.dumps(ids),)
        )
        conn.execute((ROOT / "supabase/seed_demo.sql").read_text(encoding="utf-8"))
        print(
            "Demo seed ready: 5 accounts; 10 posts. Credentials: dev/demo-accounts.local.json (gitignored)."
        )


if __name__ == "__main__":
    main()
