import { supabase } from "./supabase";

export async function api<T>(
  path: string,
  signal?: AbortSignal,
  body?: object,
): Promise<T> {
  const { data, error } = await supabase.auth.getSession();
  if (error || !data.session) throw new Error("Phiên đăng nhập đã kết thúc");
  const response = await fetch(
    `${import.meta.env.VITE_API_BASE_URL.replace(/\/$/, "")}${path}`,
    {
      method: body === undefined ? "GET" : "POST",
      headers: {
        Authorization: `Bearer ${data.session.access_token}`,
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(60000)])
        : AbortSignal.timeout(60000),
    },
  );
  if (response.status === 401 || response.status === 403) {
    await supabase.auth.signOut({ scope: "local" });
    throw new Error("Phiên đăng nhập hoặc quyền quản trị không còn hợp lệ");
  }
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(
      body !== undefined && typeof error?.detail === "string"
        ? error.detail
        : "Không tải được dữ liệu. Vui lòng thử lại.",
    );
  }
  return response.json() as Promise<T>;
}
