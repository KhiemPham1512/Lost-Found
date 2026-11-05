import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Alert, AlertDescription } from "@/components/ui/alert";

export function Login({
  onLogin,
  error,
}: {
  onLogin: (email: string, password: string) => Promise<void>;
  error?: string;
}) {
  const [pending, setPending] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setPending(true);
    try {
      await onLogin(String(data.get("email")), String(data.get("password")));
    } finally {
      setPending(false);
    }
  }
  return (
    <main className="login-screen">
      <section className="login-form" aria-labelledby="login-heading">
        <div className="brand">Lost &amp; Found</div>
        <p className="text-muted-foreground">Admin</p>
        <h1 id="login-heading">Đăng nhập quản trị</h1>
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <form onSubmit={submit}>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                required
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="password">Mật khẩu</FieldLabel>
              <Input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
              />
            </Field>
            <Button type="submit" disabled={pending} size="lg">
              {pending ? "Đang đăng nhập…" : "Đăng nhập"}
            </Button>
          </FieldGroup>
        </form>
      </section>
    </main>
  );
}
