import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import {
  Empty,
  EmptyHeader,
  EmptyTitle,
  EmptyDescription,
} from "@/components/ui/empty";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { CatalogRow } from "./types";

export const labels: Record<string, string> = {
  PENDING: "Chờ xử lý",
  REVIEWING: "Đang xem xét",
  RESOLVED: "Đã xử lý",
  DISMISSED: "Không vi phạm",
  ACTIVE: "Đang hoạt động",
  REMOVED: "Đã gỡ",
  ARCHIVED: "Đã ẩn",
  EXPIRED: "Hết hạn",
  DELETED: "Đã xóa",
  LOST: "Tìm đồ",
  FOUND: "Nhặt được",
  POST: "Bài đăng",
  USER: "Người dùng",
  CONVERSATION: "Hội thoại",
  SPAM: "Spam",
  SCAM: "Lừa đảo",
  FALSE_INFO: "Thông tin sai",
  MONEY_REQUEST: "Đòi tiền",
  VIOLATION: "Vi phạm",
  OTHER: "Khác",
  REMOVE_POST: "Gỡ bài",
  RESTORE_POST: "Khôi phục bài",
  BAN_USER: "Khóa tài khoản",
  UNBAN_USER: "Mở khóa tài khoản",
  REVIEW_REPORT: "Nhận xem xét",
  RESOLVE_REPORT: "Xử lý báo cáo",
  DISMISS_REPORT: "Không vi phạm",
  VIEW_CONVERSATION: "Xem hội thoại",
  LOCK_CONVERSATION: "Khóa hội thoại",
  KEEP: "Giữ nguyên",
  DISMISS: "Không vi phạm",
};
export type Column = { key: keyof CatalogRow; label: string };

export function DataTable({
  rows,
  columns,
  action,
}: {
  rows: CatalogRow[];
  columns: Column[];
  action?: (row: CatalogRow) => { label: string; run: () => void } | undefined;
}) {
  if (!rows.length)
    return (
      <Empty>
        <EmptyHeader>
          <EmptyTitle>Chưa có dữ liệu</EmptyTitle>
          <EmptyDescription>
            Không có kết quả phù hợp với tìm kiếm hiện tại.
          </EmptyDescription>
        </EmptyHeader>
      </Empty>
    );
  return (
    <Table>
      <TableHeader>
        <TableRow>
          {columns.map((c) => (
            <TableHead key={c.key}>{c.label}</TableHead>
          ))}
          {action && <TableHead>Thao tác</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((row) => (
          <TableRow key={row.id}>
            {columns.map((c) => {
              const value = row[c.key];
              return (
                <TableCell key={c.key} data-label={c.label}>
                  {c.key === "status" ? (
                    <Badge variant="secondary" data-status={String(value)}>
                      {value === "RESOLVED" && row.type
                        ? "Đã trao trả"
                        : labels[String(value)] || String(value ?? "—")}
                    </Badge>
                  ) : c.key === "reporter" ? (
                    (row.reporter?.username ?? "—")
                  ) : c.key === "created_at" && value ? (
                    new Date(String(value)).toLocaleString("vi-VN")
                  ) : c.key === "is_banned" ? (
                    <Badge
                      variant="secondary"
                      data-status={value ? "BANNED" : "ACTIVE"}
                    >
                      {value ? "Bị khóa" : "Hoạt động"}
                    </Badge>
                  ) : ["type", "target_type", "reason", "action"].includes(
                      c.key,
                    ) ? (
                    labels[String(value)] || String(value ?? "—")
                  ) : (
                    String(value ?? "—")
                  )}
                </TableCell>
              );
            })}
            {action && (
              <TableCell data-label="Thao tác">
                {(() => {
                  const item = action(row);
                  return item ? (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={item.run}
                      aria-label={`${item.label} ${row.code ?? row.title ?? row.username ?? ""}`.trim()}
                    >
                      {item.label}
                    </Button>
                  ) : null;
                })()}
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
