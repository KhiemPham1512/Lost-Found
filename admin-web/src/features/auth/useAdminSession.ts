import { useEffect, useState } from "react";
import type { Session } from "@supabase/supabase-js";
import { supabase } from "@/lib/supabase";

export function useAdminSession() {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);
  const [admin, setAdmin] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const { data } = supabase.auth.onAuthStateChange((_event, next) => {
      setAdmin(false);
      setReady(!next);
      setSession(next);
    });
    return () => data.subscription.unsubscribe();
  }, []);
  useEffect(() => {
    if (!session) return;
    let current = true;
    // Outside the Auth callback: avoid waiting on the Auth SDK's own lock.
    void supabase
      .from("profiles")
      .select("role,is_banned")
      .eq("id", session.user.id)
      .single()
      .then(async (result) => {
        if (!current) return;
        if (
          result.error ||
          result.data?.role !== "ADMIN" ||
          result.data.is_banned
        ) {
          setError("Không có quyền quản trị hoặc tài khoản đã bị khóa");
          await supabase.auth.signOut({ scope: "local" });
          if (!current) return;
          setSession(null);
          setAdmin(false);
        } else {
          setAdmin(true);
        }
        setReady(true);
      });
    return () => {
      current = false;
    };
  }, [session]);
  async function login(email: string, password: string) {
    setError("");
    try {
      const result = await supabase.auth.signInWithPassword({
        email,
        password,
      });
      if (result.error)
        setError("Email hoặc mật khẩu không đúng. Vui lòng thử lại.");
    } catch {
      setError("Không thể kết nối. Vui lòng thử lại.");
    }
  }
  async function logout() {
    await supabase.auth.signOut({ scope: "local" });
  }
  return { session, ready, admin, error, login, logout };
}
