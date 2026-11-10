import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { labels } from "./DataTable";
import { ModerationDialog, type ModerationAction } from "./ModerationDialog";
import { catalogModeration } from "./catalogModeration";
import type { AdminPostDetail, AdminUserDetail } from "./types";

export function CatalogDetail({
  kind,
  id,
  back,
  backLabel = "Quay lại danh sách",
}: {
  kind: "posts" | "users";
  id: string;
  back: () => void;
  backLabel?: string;
}) {
  const heading = useRef<HTMLHeadingElement>(null);
  const [author, setAuthor] = useState<string>();
  const [moderation, setModeration] = useState<ModerationAction>();
  const result = useQuery({
    queryKey: ["catalog-detail", kind, id],
    enabled: !author,
    queryFn: ({ signal }) =>
      api<AdminPostDetail | AdminUserDetail>(`/admin/${kind}/${id}`, signal),
  });
  useEffect(() => {
    if (result.isSuccess && !author) heading.current?.focus();
  }, [result.isSuccess, author]);
  if (author)
    return (
      <CatalogDetail
        key={author}
        kind="users"
        id={author}
        back={() => setAuthor(undefined)}
        backLabel="Quay lại bài đăng"
      />
    );
  const post =
    kind === "posts" ? (result.data as AdminPostDetail | undefined) : undefined;
  const user =
    kind === "users" ? (result.data as AdminUserDetail | undefined) : undefined;
  const action = result.data && catalogModeration(kind, result.data);
  const date = (value: string | null) =>
    value ? new Date(value).toLocaleString("vi-VN") : "—";
  const fields = post
    ? [
        ["Loại bài", labels[post.type]],
        [
          "Trạng thái",
          post.status === "RESOLVED" ? "Đã trao trả" : labels[post.status],
        ],
        ["Danh mục", post.category_name],
        ["Địa điểm", post.location_text || "—"],
        ["Thời gian mất/nhặt", date(post.event_time)],
        ["Ngày đăng", date(post.created_at)],
        ["Ngày hết hạn", date(post.expires_at)],
        ["Số báo cáo", post.report_count],
      ]
    : user
      ? [
          ["Tên hiển thị", user.display_name || "—"],
          ["Vai trò", user.role === "ADMIN" ? "Quản trị viên" : "Người dùng"],
          ["Trạng thái", user.is_banned ? "Bị khóa" : "Hoạt động"],
          ["Tham gia", user.joined_month],
          ["Số điện thoại", user.phone_masked || "—"],
          ["Số bài đăng", user.post_count],
          ["Số báo cáo", user.report_count],
        ]
      : [];
  return (
    <div className="detail-stack catalog-detail">
      <div>
        <Button variant="outline" onClick={back}>
          <ArrowLeft aria-hidden="true" />
          {backLabel}
        </Button>
      </div>
      {result.isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : result.isError ? (
        <Alert variant="destructive">
          <AlertDescription>
            {result.error.message}
            <Button variant="outline" onClick={() => void result.refetch()}>
              Thử lại
            </Button>
          </AlertDescription>
        </Alert>
      ) : (
        <>
          <div className="section-heading">
            <h2 ref={heading} tabIndex={-1}>
              {post?.title ?? user?.username}
            </h2>
            <Badge variant="secondary">
              {post ? "Chi tiết bài đăng" : "Chi tiết người dùng"}
            </Badge>
          </div>
          <section className="panel">
            <h3>{post ? "Thông tin bài đăng" : "Hồ sơ công khai"}</h3>
            {user?.avatar_url && (
              <img
                className="h-20 w-20 rounded-full object-cover"
                src={user.avatar_url}
                referrerPolicy="no-referrer"
                alt={`Ảnh đại diện ${user.username}`}
              />
            )}
            <dl className="detail-meta">
              {fields.map(([label, value]) => (
                <div key={label}>
                  <dt>{label}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
            <p className="detail-copy">
              {post
                ? post.description || "Không có mô tả."
                : user?.bio || "Chưa có giới thiệu."}
            </p>
            {post && (
              <>
                {!!post.tags.length && (
                  <ul
                    aria-label="Tag bài đăng"
                    className="my-4 flex flex-wrap gap-2"
                  >
                    {post.tags.map((tag) => (
                      <li key={tag}>
                        <Badge variant="secondary">{tag}</Badge>
                      </li>
                    ))}
                  </ul>
                )}
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
                        alt={`Ảnh bài ${post.title}`}
                        loading="lazy"
                      />
                    </a>
                  ))}
                </div>
                <Button
                  className="mt-4"
                  variant="outline"
                  onClick={() => setAuthor(post.author_id)}
                  aria-label={`Xem người dùng ${post.author_username}`}
                >
                  Người đăng: {post.author_username}
                </Button>
              </>
            )}
          </section>
          {action && (
            <div>
              <Button variant="outline" onClick={() => setModeration(action)}>
                {action.label}
              </Button>
            </div>
          )}
        </>
      )}
      {moderation && (
        <ModerationDialog
          action={moderation}
          close={() => setModeration(undefined)}
        />
      )}
    </div>
  );
}
