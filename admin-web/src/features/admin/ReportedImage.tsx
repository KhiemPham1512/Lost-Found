import { useEffect, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";

export function ReportedImage({
  conversationId,
  messageId,
  reportId,
  close,
}: {
  conversationId: string;
  messageId: number;
  reportId: string;
  close: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const [expired, setExpired] = useState(false);
  const cache = useQueryClient();
  const image = useQuery({
    queryKey: ["reported-image", conversationId, messageId],
    gcTime: 0,
    staleTime: 0,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    queryFn: async ({ signal }) => {
      const result = await api<{ url: string; expires_in: number }>(
        `/admin/conversations/${conversationId}/messages/${messageId}/image`,
        signal,
      );
      void cache.invalidateQueries({ queryKey: ["actions"] });
      void cache.invalidateQueries({ queryKey: ["report", reportId] });
      return result;
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
  useEffect(() => {
    if (!image.data) return;
    setExpired(false);
    const timeout = setTimeout(
      () => setExpired(true),
      image.data.expires_in * 1000,
    );
    return () => clearTimeout(timeout);
  }, [image.data, image.dataUpdatedAt]);
  function retry() {
    setLoaded(false);
    setFailed(false);
    setExpired(false);
    void image.refetch();
  }
  return (
    <dialog
      ref={dialog}
      className="moderation-dialog chat-image-dialog"
      aria-labelledby="reported-image-title"
      onCancel={(e) => {
        e.preventDefault();
        close();
      }}
    >
      <div className="section-heading">
        <h2 id="reported-image-title">Ảnh tin nhắn</h2>
        <Button
          variant="ghost"
          size="icon"
          aria-label="Đóng ảnh"
          onClick={close}
        >
          <X aria-hidden="true" />
        </Button>
      </div>
      {image.isPending || image.isFetching ? (
        <p role="status">Đang tải ảnh…</p>
      ) : image.isError || failed ? (
        <Alert variant="destructive">
          <AlertDescription>
            Không tải được ảnh hoặc phiên xem đã hết hạn.
            <Button variant="outline" onClick={retry}>
              Thử lại ảnh
            </Button>
          </AlertDescription>
        </Alert>
      ) : expired ? (
        <div>
          <p role="status">Phiên xem ảnh đã hết hạn.</p>
          <Button variant="outline" onClick={retry}>
            Tải lại ảnh
          </Button>
        </div>
      ) : (
        <div>
          {!loaded && <p role="status">Đang tải ảnh…</p>}
          <img
            key={image.dataUpdatedAt}
            src={image.data.url}
            alt={`Ảnh tin nhắn ${messageId}`}
            referrerPolicy="no-referrer"
            className={`mx-auto max-h-[65dvh] max-w-full object-contain ${loaded ? "" : "invisible"}`}
            onLoad={() => setLoaded(true)}
            onError={() => setFailed(true)}
          />
        </div>
      )}
    </dialog>
  );
}
