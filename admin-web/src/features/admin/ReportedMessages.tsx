import { useState } from "react";
import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
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
}: {
  conversationId: string;
  reportId: string;
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
          <ul className="audit-list mt-4">
            {messages.data.pages
              .flatMap((page) => page.messages)
              .map((message) => (
                <li key={message.id}>
                  <small>
                    {message.sender_id ? "Thành viên hội thoại" : "Hệ thống"} ·{" "}
                    {new Date(message.created_at).toLocaleString("vi-VN")}
                  </small>
                  <p className="detail-copy">
                    {message.type === "IMAGE"
                      ? "Tin nhắn ảnh"
                      : message.content || "—"}
                  </p>
                </li>
              ))}
          </ul>
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
