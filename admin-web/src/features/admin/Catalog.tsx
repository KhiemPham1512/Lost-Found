import { useEffect, useRef, useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { DataTable, type Column } from "./DataTable";
import type { PageData, Tab } from "./types";
import { ReportDetail } from "./ReportDetail";
import { CatalogDetail } from "./CatalogDetail";
import { catalogModeration } from "./catalogModeration";
import { ModerationDialog, type ModerationAction } from "./ModerationDialog";
import { ChevronLeft, ChevronRight } from "lucide-react";

const titles = {
  reports: "Danh sách báo cáo",
  posts: "Danh sách bài đăng",
  users: "Danh sách người dùng",
  actions: "Lịch sử kiểm duyệt",
};
const placeholders = {
  reports: "Nhập mã, tiêu đề hoặc định danh…",
  posts: "Nhập tiêu đề hoặc mô tả bài đăng…",
  users: "Nhập định danh công khai…",
  actions: "Nhập mã báo cáo hoặc ghi chú…",
};

const filters: Record<Exclude<Tab, "stats">, [string, string][]> = {
  reports: [
    ["PENDING", "Chờ xử lý"],
    ["REVIEWING", "Đang xem xét"],
    ["RESOLVED", "Đã xử lý"],
    ["DISMISSED", "Không vi phạm"],
  ],
  posts: [
    ["ACTIVE", "Đang hoạt động"],
    ["RESOLVED", "Đã trao trả"],
    ["ARCHIVED", "Đã ẩn"],
    ["EXPIRED", "Hết hạn"],
    ["REMOVED", "Đã gỡ"],
    ["DELETED", "Đã xóa"],
  ],
  users: [
    ["ACTIVE", "Hoạt động"],
    ["BANNED", "Bị khóa"],
    ["REPORTED", "Có từ 3 báo cáo"],
  ],
  actions: [
    ["today", "Hôm nay"],
    ["7d", "7 ngày qua"],
    ["30d", "30 ngày qua"],
  ],
};

const typeFilters: Partial<Record<Exclude<Tab, "stats">, [string, string][]>> =
  {
    reports: [
      ["POST", "Bài đăng"],
      ["USER", "Tài khoản"],
      ["CONVERSATION", "Hội thoại"],
    ],
    posts: [
      ["LOST", "Lost"],
      ["FOUND", "Found"],
    ],
    actions: [
      ["POST", "Bài đăng"],
      ["USER", "Tài khoản"],
      ["CONVERSATION", "Hội thoại"],
    ],
  };

const columns: Record<Exclude<Tab, "stats">, Column[]> = {
  reports: [
    { key: "code", label: "Mã" },
    { key: "target_type", label: "Loại" },
    { key: "target_label", label: "Đối tượng" },
    { key: "reason", label: "Lý do" },
    { key: "reporter", label: "Người báo cáo" },
    { key: "created_at", label: "Thời gian" },
    { key: "status", label: "Trạng thái" },
  ],
  posts: [
    { key: "title", label: "Bài đăng" },
    { key: "type", label: "Loại" },
    { key: "category_name", label: "Danh mục" },
    { key: "author_username", label: "Người đăng" },
    { key: "created_at", label: "Ngày đăng" },
    { key: "report_count", label: "Báo cáo" },
    { key: "status", label: "Trạng thái" },
  ],
  users: [
    { key: "username", label: "Định danh" },
    { key: "phone_masked", label: "Số điện thoại" },
    { key: "role", label: "Vai trò" },
    { key: "joined_month", label: "Tham gia" },
    { key: "post_count", label: "Số bài" },
    { key: "report_count", label: "Báo cáo" },
    { key: "is_banned", label: "Trạng thái" },
  ],
  actions: [
    { key: "created_at", label: "Thời gian" },
    { key: "admin_username", label: "Quản trị viên" },
    { key: "action", label: "Hành động" },
    { key: "target_label", label: "Đối tượng" },
    { key: "report_code", label: "Mã báo cáo" },
    { key: "note", label: "Ghi chú" },
  ],
};

export function Catalog({
  tab,
  initialReportId,
}: {
  tab: Exclude<Tab, "stats">;
  initialReportId?: string;
}) {
  const [query, setQuery] = useState("");
  const [draft, setDraft] = useState("");
  const [offset, setOffset] = useState(0);
  const [status, setStatus] = useState("");
  const [type, setType] = useState("");
  const typeOptions = typeFilters[tab];
  const [reportId, setReportId] = useState(initialReportId);
  const [detailId, setDetailId] = useState<string>();
  const returnFocus = useRef<string | undefined>(undefined);
  const catalogHeading = useRef<HTMLHeadingElement>(null);
  const [moderation, setModeration] = useState<ModerationAction>();
  const result = useQuery({
    queryKey: [tab, query, offset, status, type],
    enabled: !reportId && !detailId,
    queryFn: ({ signal }) =>
      api<PageData>(
        `/admin/${tab}?${new URLSearchParams({ q: query, limit: "20", offset: String(offset), ...(status ? { [tab === "actions" ? "range" : "status"]: status } : {}), ...(typeOptions && type ? { [tab === "actions" ? "target_type" : "type"]: type } : {}) })}`,
        signal,
      ),
  });
  useEffect(() => {
    if (!detailId && !result.isFetching && returnFocus.current) {
      (
        document.getElementById(returnFocus.current) ?? catalogHeading.current
      )?.focus();
      returnFocus.current = undefined;
    }
  }, [detailId, result.isFetching]);
  function search(event: FormEvent) {
    event.preventDefault();
    setQuery(draft.trim());
    setOffset(0);
  }
  if (reportId)
    return (
      <ReportDetail
        key={reportId}
        id={reportId}
        back={() => setReportId(undefined)}
      />
    );
  if (detailId && (tab === "posts" || tab === "users"))
    return (
      <CatalogDetail
        kind={tab}
        id={detailId}
        back={() => setDetailId(undefined)}
      />
    );
  return (
    <section
      className="catalog-surface"
      aria-label={titles[tab]}
      aria-busy={result.isFetching}
    >
      <form onSubmit={search} className="search-form">
        <FieldGroup>
          <Field>
            <FieldLabel htmlFor="search">Tìm kiếm</FieldLabel>
            <Input
              id="search"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              maxLength={100}
              placeholder={placeholders[tab]}
            />
          </Field>
        </FieldGroup>
        {typeOptions && (
          <FieldGroup className="type-filter">
            <Field>
              <FieldLabel id="catalog-type-label">
                {tab === "posts"
                  ? "Loại bài"
                  : tab === "actions"
                    ? "Loại đối tượng"
                    : "Loại báo cáo"}
              </FieldLabel>
              <ToggleGroup
                aria-labelledby="catalog-type-label"
                variant="outline"
                spacing={2}
                className="flex-wrap"
                value={[type || "ALL"]}
                onValueChange={(values) => {
                  const selected = values[0];
                  setType(!selected || selected === "ALL" ? "" : selected);
                  setOffset(0);
                }}
              >
                <ToggleGroupItem value="ALL" className="min-h-11">
                  Tất cả
                </ToggleGroupItem>
                {typeOptions.map(([value, label]) => (
                  <ToggleGroupItem
                    key={value}
                    value={value}
                    className="min-h-11"
                  >
                    {label}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
            </Field>
          </FieldGroup>
        )}
        <label className="filter-field">
          <span id="catalog-filter-label">
            {tab === "actions" ? "Khoảng thời gian" : "Trạng thái"}
          </span>
          <select
            aria-labelledby="catalog-filter-label"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setOffset(0);
            }}
          >
            <option value="">Tất cả</option>
            {filters[tab].map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <Button type="submit" variant="outline">
          Tìm kiếm
        </Button>
        <Button
          type="button"
          variant="ghost"
          disabled={!draft && !query && !status && !type && offset === 0}
          onClick={() => {
            setDraft("");
            setQuery("");
            setStatus("");
            setType("");
            setOffset(0);
          }}
        >
          Xóa bộ lọc
        </Button>
      </form>
      <div className="catalog-heading">
        <h2 ref={catalogHeading} tabIndex={-1}>
          {titles[tab]}
        </h2>
        {result.isSuccess && (
          <p className="muted">{result.data.total} kết quả</p>
        )}
      </div>
      {result.isPending ? (
        <Skeleton className="h-40 w-full" />
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
          <DataTable
            rows={result.data.items}
            columns={columns[tab]}
            action={
              tab === "actions"
                ? undefined
                : (row) => {
                    if (tab === "reports")
                      return {
                        label: "Xem",
                        run: () => setReportId(String(row.id)),
                      };
                    const id = `catalog-view-${tab}-${row.id}`;
                    const view = {
                      label: "Xem",
                      id,
                      run: () => {
                        returnFocus.current = id;
                        setDetailId(String(row.id));
                      },
                    };
                    const moderation = catalogModeration(tab, row);
                    return moderation
                      ? [
                          view,
                          {
                            label: moderation.label,
                            run: () => setModeration(moderation),
                          },
                        ]
                      : [view];
                  }
            }
          />
          <div className="pagination">
            <p role="status">
              Hiển thị {result.data.items.length ? offset + 1 : 0}–
              {result.data.items.length ? offset + result.data.items.length : 0}{" "}
              trên {result.data.total} kết quả
            </p>
            <Button
              variant="outline"
              disabled={offset === 0}
              onClick={() => setOffset(Math.max(0, offset - 20))}
            >
              <ChevronLeft aria-hidden="true" data-icon="inline-start" />
              Trước
            </Button>
            <Button
              variant="outline"
              disabled={offset + 20 >= result.data.total}
              onClick={() => setOffset(offset + 20)}
            >
              Tiếp
              <ChevronRight aria-hidden="true" data-icon="inline-end" />
            </Button>
          </div>
        </>
      )}
      {moderation && (
        <ModerationDialog
          action={moderation}
          close={() => setModeration(undefined)}
        />
      )}
    </section>
  );
}
