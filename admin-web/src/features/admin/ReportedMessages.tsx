import { useState } from "react";
import {
  useInfiniteQuery,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "@/lib/api";
import { supabase } from "@/lib/supabase";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";

type MessagePage = {
  messages: {
    id: number;
    sender_id: string | null;
    type: string;
    content: string | null;
    created_at: string;
  }[];
};

export function ReportedMessages({
  conversationId,
  reportId,
  reporterId,
}: {
  conversationId: string;
  reportId: string;
  reporterId: string;
}) {
  const [inspecting, setInspecting] = useState(false);
  const cache = useQueryClient();
  const messages = useInfiniteQuery({
    queryKey: ["reported-messages", conversationId],
    enabled: inspecting,
    initialPageParam: undefined as number | undefined,
    queryFn: async ({ pageParam, signal }) => {
      const result = await api<MessagePage>(
        `/admin/conversations/${conversationId}/messages?limit=100${pageParam === undefined ? "" : `&before_id=${pageParam}`}`,
        signal,
      );
      void cache.invalidateQueries({ queryKey: ["report", reportId] });
      void cache.invalidateQueries({ queryKey: ["actions"] });
      return result;
    },
    getNextPageParam: (page) =>
      page.messages.length === 100
        ? Math.min(...page.messages.map((message) => message.id))
        : undefined,
  });
  const items = messages.data?.pages.flatMap((page) => page.messages) ?? [];
  const senderIds = [
    ...new Set(
      items.flatMap((message) =>
        message.sender_id ? [message.sender_id] : [],
      ),
    ),
  ].sort();
  const senders = useQuery({
    queryKey: ["message-senders", conversationId, senderIds],
    enabled: inspecting && senderIds.length > 0,
    queryFn: async ({ signal }) => {
      const { data, error } = await supabase
        .from("profiles")
        .select("id,username")
        .in("id", senderIds)
        .abortSignal(AbortSignal.any([signal, AbortSignal.timeout(15000)]));
      if (error) throw error;
      return data;
    },
  });
  return (
    <section className="panel">
      <h2>Tin nhắn hội thoại bị báo cáo</h2>
      <p className="muted">Mỗi lần xem được ghi vào nhật ký quản trị.</p>
      {!inspecting ? (
        <Button
          className="mt-4"
          variant="outline"
          onClick={() => setInspecting(true)}
        >
          Xem tin nhắn
        </Button>
      ) : messages.isError ? (
        <Alert variant="destructive">
          <AlertDescription>
            {messages.error.message}
            <Button variant="outline" onClick={() => void messages.refetch()}>
              Thử lại
            </Button>
          </AlertDescription>
        </Alert>
      ) : messages.isPending ? (
        <p role="status">Đang tải tin nhắn…</p>
      ) : (
        <>
          {senderIds.length > 0 && senders.isPending && (
            <p className="muted mt-4" role="status">
              Đang tải tên người gửi…
            </p>
          )}
          {senders.isError && (
            <p className="muted mt-4" role="status">
              Chưa tải được tên người gửi. Đang hiển thị ID.
              <Button variant="outline" onClick={() => void senders.refetch()}>
                Thử lại tên người gửi
              </Button>
            </p>
          )}
          <ol
            className="reported-messages mt-4"
            aria-label="Tin nhắn từ cũ đến mới"
          >
            {items
              .sort((a, b) => a.id - b.id)
              .map((message) => (
                <li
                  key={message.id}
                  className={
                    !message.sender_id
                      ? "message-system"
                      : message.sender_id === reporterId
                        ? "message-reporter"
                        : ""
                  }
                >
                  <p className="detail-copy message-bubble">
                    {message.type === "IMAGE"
                      ? "Tin nhắn ảnh"
                      : message.content || "—"}
                  </p>
                  <small>
                    {message.sender_id
                      ? (senders.data?.find(
                          (sender) => sender.id === message.sender_id,
                        )?.username ?? `ID: ${message.sender_id}`)
                      : "Hệ thống"}{" "}
                    ·{" "}
                    <time dateTime={message.created_at}>
                      {new Date(message.created_at).toLocaleString("vi-VN")}
                    </time>
                  </small>
                </li>
              ))}
          </ol>
          {!messages.data.pages[0].messages.length && (
            <p className="muted mt-4">Chưa có tin nhắn.</p>
          )}
          {messages.hasNextPage && (
            <Button
              className="mt-4"
              variant="outline"
              disabled={messages.isFetchingNextPage}
              onClick={() => void messages.fetchNextPage()}
            >
              {messages.isFetchingNextPage ? "Đang tải…" : "Tin nhắn cũ hơn"}
            </Button>
          )}
        </>
      )}
    </section>
  );
}
