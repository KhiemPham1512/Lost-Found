import type { CatalogRow } from "./types";
import type { ModerationAction } from "./ModerationDialog";

export function catalogModeration(
  kind: "posts" | "users",
  row: CatalogRow,
): ModerationAction | undefined {
  if (kind === "posts" && ["DELETED", "EXPIRED"].includes(row.status ?? ""))
    return;
  if (kind === "users" && row.role === "ADMIN") return;
  const verb =
    kind === "posts"
      ? row.status === "REMOVED"
        ? "restore"
        : "remove"
      : row.is_banned
        ? "unban"
        : "ban";
  return {
    label: {
      restore: "Khôi phục",
      remove: "Gỡ bài",
      ban: "Khóa",
      unban: "Mở khóa",
    }[verb],
    target: row.title ?? row.username ?? String(row.id),
    path: `/admin/${kind}/${row.id}/${verb}`,
    needsReason: verb !== "restore",
  };
}
