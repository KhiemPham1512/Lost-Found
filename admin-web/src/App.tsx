import { Login } from "@/features/auth/Login";
import { useAdminSession } from "@/features/auth/useAdminSession";
import { configured } from "@/lib/supabase";
import { lazy, Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";

const AdminShell = lazy(() =>
  import("@/features/admin/AdminShell").then((module) => ({
    default: module.AdminShell,
  })),
);

function Administration() {
  const auth = useAdminSession();
  if (!auth.ready)
    return (
      <main className="p-8" aria-label="Đang kiểm tra đăng nhập">
        <Skeleton className="h-8 w-60" />
      </main>
    );
  if (!auth.admin || !auth.session)
    return <Login onLogin={auth.login} error={auth.error} />;
  return (
    <Suspense
      fallback={
        <main className="p-8">
          <Skeleton className="h-64 w-full" />
        </main>
      }
    >
      <AdminShell logout={auth.logout} />
    </Suspense>
  );
}

export default function App() {
  if (!configured)
    return (
      <main className="login-screen">
        <Alert>
          <AlertDescription>
            Trang quản trị chưa được cấu hình.
          </AlertDescription>
        </Alert>
      </main>
    );
  return <Administration />;
}
