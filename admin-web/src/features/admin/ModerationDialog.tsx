import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { api } from "@/lib/api";

export type ModerationAction = {
  label: string;
  target: string;
  path: string;
  needsReason: boolean;
};

export function ModerationDialog({
  action,
  close,
}: {
  action: ModerationAction;
  close: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [reason, setReason] = useState("");
  const cache = useQueryClient();
  const mutation = useMutation({
    mutationFn: () =>
      api<{ login_blocked?: boolean; is_banned?: boolean }>(
        action.path,
        undefined,
        action.needsReason ? { reason: reason.trim() } : {},
      ),
    onSuccess: async () => {
      await cache.invalidateQueries();
    },
  });
  useEffect(() => {
    const element = dialog.current;
    const opener = document.activeElement;
    element?.showModal();
    return () => {
      element?.close();
      if (opener instanceof HTMLElement && opener.isConnected) opener.focus();
    };
  }, []);
  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }
  return (
    <dialog
      ref={dialog}
      className="moderation-dialog"
      aria-labelledby="moderation-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!mutation.isPending) close();
      }}
    >
      <h2 id="moderation-title">{action.label}</h2>
      <p>{action.target}</p>
      {mutation.isSuccess ? (
        <div className="action-form">
          <p role="status" className="feedback">
            Đã thực hiện thành công.
            {mutation.data.is_banned === true &&
              mutation.data.login_blocked === false &&
              " Thay đổi đã lưu trong hệ thống; đồng bộ khóa đăng nhập với Supabase Auth chưa thành công."}
          </p>
          <Button onClick={close}>Đóng</Button>
        </div>
      ) : (
        <form className="action-form" onSubmit={submit}>
          {action.needsReason && (
            <label>
              Lý do
              <textarea
                autoFocus
                required
                minLength={3}
                maxLength={500}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                disabled={mutation.isPending}
              />
            </label>
          )}
          {mutation.isError && (
            <Alert variant="destructive">
              <AlertDescription>{mutation.error.message}</AlertDescription>
            </Alert>
          )}
          <div className="form-actions">
            <Button
              type="button"
              variant="outline"
              disabled={mutation.isPending}
              onClick={close}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={
                mutation.isPending ||
                (action.needsReason && reason.trim().length < 3)
              }
            >
              {mutation.isPending ? "Đang lưu…" : action.label}
            </Button>
          </div>
        </form>
      )}
    </dialog>
  );
}
