import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { labels } from "./DataTable";
import type { ReportDetailData } from "./types";
import { ReportedMessages } from "./ReportedMessages";
import { ModerationDialog, type ModerationAction } from "./ModerationDialog";

export function ReportDetail({ id, back }: { id: string; back: () => void }) {
  const cache = useQueryClient();
  const heading = useRef<HTMLHeadingElement>(null);
  const [action, setAction] = useState("KEEP");
  const [note, setNote] = useState("");
  const [success, setSuccess] = useState("");
  const [moderation, setModeration] = useState<ModerationAction>();
  const result = useQuery({
    queryKey: ["report", id],
    queryFn: ({ signal }) =>
      api<ReportDetailData>(`/admin/reports/${id}`, signal),
  });
  useEffect(() => {
    if (result.isSuccess) heading.current?.focus();
  }, [result.isSuccess]);
  const mutation = useMutation({
    mutationFn: ({ kind }: { kind: "review" | "resolve" }) =>
      api<{ login_blocked?: boolean; affected_reports: number }>(
        `/admin/reports/${id}/${kind}`,
        undefined,
        kind === "review" ? {} : { action, note: note.trim() },
      ),
    onMutate: () => setSuccess(""),
    onSuccess: async (data) => {
      setSuccess(
        `Đã cập nhật ${data.affected_reports} báo cáo cùng đối tượng.${data.login_blocked === false ? " Thay đổi đã lưu trong hệ thống; đồng bộ khóa đăng nhập với Supabase Auth chưa thành công." : ""}`,
      );
      setNote("");
      await cache.invalidateQueries();
    },
    onError: async () => {
      await cache.invalidateQueries({ queryKey: ["report", id] });
    },
  });
  function resolve(event: FormEvent) {
    event.preventDefault();
    mutation.mutate({ kind: "resolve" });
  }
  if (result.isPending) return <Skeleton className="h-64 w-full" />;
  if (result.isError)
    return (
      <div className="detail-stack">
        <Button variant="outline" onClick={back}>
          Quay lại danh sách
        </Button>
        <Alert variant="destructive">
          <AlertDescription>
            {result.error.message}
            <Button variant="outline" onClick={() => void result.refetch()}>
              Thử lại
            </Button>
          </AlertDescription>
        </Alert>
      </div>
    );
  const report = result.data;
  const post = report.target.post;
  const user =
    report.target.type === "USER" ? report.target.user : post?.author;
  const conversation = report.target.conversation;
  const open = ["PENDING", "REVIEWING"].includes(report.status);
  return (
    <div className="detail-stack">
      <div>
        <Button variant="outline" onClick={back}>
          <ArrowLeft aria-hidden="true" />
          Quay lại danh sách
        </Button>
      </div>
      <div className="section-heading">
        <h2 ref={heading} tabIndex={-1}>
          Báo cáo {report.code}
        </h2>
        <Badge variant="secondary" data-status={report.status}>
          {labels[report.status] ?? report.status}
        </Badge>
      </div>
      {success && (
        <p className="feedback" role="status">
          {success}
        </p>
      )}
      <div className="detail-grid">
        <div className="detail-stack">
          <section className="panel">
            <h2>Nội dung báo cáo</h2>
            <dl className="detail-meta">
              <div>
                <dt>Người báo cáo</dt>
                <dd>{report.reporter.username}</dd>
              </div>
              <div>
                <dt>Thời gian</dt>
                <dd>{new Date(report.created_at).toLocaleString("vi-VN")}</dd>
              </div>
              <div>
                <dt>Lý do</dt>
                <dd>{labels[report.reason] ?? report.reason}</dd>
              </div>
            </dl>
            <p className="detail-copy">
              {report.description || "Không có mô tả bổ sung."}
            </p>
          </section>
          <section className="panel">
            <h2>{labels[report.target.type]} bị báo cáo</h2>
            {post && (
              <>
                <h3>{post.title}</h3>
                <dl className="detail-meta">
                  <div>
                    <dt>Trạng thái</dt>
                    <dd>{labels[post.status] ?? post.status}</dd>
                  </div>
                  <div>
                    <dt>Địa điểm</dt>
                    <dd>{post.location_text || "—"}</dd>
                  </div>
                </dl>
                <p className="detail-copy">{post.description}</p>
                <div className="detail-images">
                  {post.image_paths.map((path) => (
                    <a
                      key={path}
                      href={`${import.meta.env.VITE_SUPABASE_URL}/storage/v1/object/public/post-images/${path}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      <img
                        src={`${import.meta.env.VITE_SUPABASE_URL}/storage/v1/object/public/post-images/${path}`}
                        alt="Ảnh của bài bị báo cáo"
                        loading="lazy"
                      />
                    </a>
                  ))}
                </div>
              </>
            )}
            {user && (
              <dl className="detail-meta">
                <div>
                  <dt>{post ? "Người đăng" : "Định danh"}</dt>
                  <dd>{user.username}</dd>
                </div>
                <div>
                  <dt>Tài khoản</dt>
                  <dd>{user.is_banned ? "Bị khóa" : "Hoạt động"}</dd>
                </div>
                <div>
                  <dt>Bài đã đăng</dt>
                  <dd>{user.post_count}</dd>
                </div>
                <div>
                  <dt>Báo cáo trước đây</dt>
                  <dd>{user.reported_before_count}</dd>
                </div>
              </dl>
            )}
            {report.target.type === "CONVERSATION" && conversation && (
              <dl className="detail-meta">
                <div>
                  <dt>Thành viên</dt>
                  <dd>{conversation.participants.join(" ↔ ")}</dd>
                </div>
                <div>
                  <dt>Hội thoại</dt>
                  <dd>{conversation.is_locked ? "Đã khóa" : "Đang mở"}</dd>
                </div>
                <div>
                  <dt>Số tin nhắn</dt>
                  <dd>{conversation.message_count}</dd>
                </div>
              </dl>
            )}
            {!post && !user && report.target.type !== "CONVERSATION" && (
              <p className="muted">Đối tượng không còn tồn tại.</p>
            )}
          </section>
          {report.target.type === "CONVERSATION" && conversation && (
            <>
              {!conversation.is_locked && (
                <div>
                  <Button
                    variant="outline"
                    onClick={() =>
                      setModeration({
                        label: "Khóa hội thoại",
                        target: conversation.participants.join(" ↔ "),
                        path: `/admin/conversations/${conversation.id}/lock`,
                        needsReason: true,
                      })
                    }
                  >
                    Khóa hội thoại
                  </Button>
                </div>
              )}
              <ReportedMessages
                conversationId={conversation.id}
                reportId={id}
                reporterId={report.reporter.id}
              />
            </>
          )}
          <section className="panel">
            <h2>Báo cáo cùng đối tượng ({report.reports_on_target.length})</h2>
            <ul className="audit-list">
              {report.reports_on_target.map((item) => (
                <li key={item.id}>
                  <strong>
                    {item.code} · {labels[item.reason] ?? item.reason}
                  </strong>
                  <p className="detail-copy">
                    {item.description || "Không có mô tả bổ sung."}
                  </p>
                  <small>{item.reporter_username}</small>
                </li>
              ))}
            </ul>
          </section>
        </div>
        <div className="detail-stack">
          <section className="panel">
            <h2>Xử lý báo cáo</h2>
            {mutation.isError && (
              <Alert variant="destructive">
                <AlertDescription>{mutation.error.message}</AlertDescription>
              </Alert>
            )}
            {open ? (
              <form className="action-form" onSubmit={resolve}>
                {report.status === "PENDING" && (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={mutation.isPending}
                    onClick={() => mutation.mutate({ kind: "review" })}
                  >
                    Nhận xem xét
                  </Button>
                )}
                <label>
                  <span id="report-action-label">Hành động</span>
                  <select
                    aria-labelledby="report-action-label"
                    value={action}
                    onChange={(e) => setAction(e.target.value)}
                    disabled={mutation.isPending}
                  >
                    <option value="KEEP">Giữ nguyên</option>
                    <option value="DISMISS">Không vi phạm</option>
                    {post && !["REMOVED", "DELETED"].includes(post.status) && (
                      <option value="REMOVE_POST">Gỡ bài</option>
                    )}
                    {user && !user.is_banned && (
                      <option value="BAN_USER">
                        Khóa tài khoản {user.username}
                      </option>
                    )}
                  </select>
                </label>
                <label>
                  Ghi chú xử lý
                  <textarea
                    required
                    minLength={3}
                    maxLength={500}
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    disabled={mutation.isPending}
                  />
                </label>
                <p className="muted">
                  Áp dụng cho các báo cáo đang mở về cùng đối tượng. Lý do gỡ
                  bài được gửi cho người đăng.
                </p>
                <Button
                  type="submit"
                  disabled={mutation.isPending || note.trim().length < 3}
                >
                  {mutation.isPending ? "Đang lưu…" : "Lưu kết quả xử lý"}
                </Button>
              </form>
            ) : (
              <>
                <p>
                  {labels[report.resolution_action ?? ""] ?? "Báo cáo đã đóng"}
                </p>
                <p className="detail-copy muted">{report.resolution_note}</p>
              </>
            )}
          </section>
          <section className="panel">
            <h2>Lịch sử xử lý</h2>
            {report.admin_actions.length ? (
              <ul className="audit-list">
                {report.admin_actions.map((item) => (
                  <li key={item.id}>
                    <strong>{labels[item.action] ?? item.action}</strong>
                    <p className="detail-copy">{item.note}</p>
                    <small>
                      {item.admin_username} ·{" "}
                      {new Date(item.created_at).toLocaleString("vi-VN")}
                    </small>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="muted">Chưa có thao tác xử lý.</p>
            )}
          </section>
        </div>
      </div>
      {moderation && (
        <ModerationDialog
          action={moderation}
          close={() => setModeration(undefined)}
        />
      )}
    </div>
  );
}
