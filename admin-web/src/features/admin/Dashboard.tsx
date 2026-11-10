import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { DataTable } from "./DataTable";
import type { Stats } from "./types";
import { ArrowRight } from "lucide-react";

const number = new Intl.NumberFormat("vi-VN");

export function Dashboard({
  openReports,
}: {
  openReports: (id?: string) => void;
}) {
  const result = useQuery({
    queryKey: ["stats"],
    queryFn: ({ signal }) => api<Stats>("/admin/stats", signal),
  });
  if (result.isPending) return <Skeleton className="h-64 w-full" />;
  if (result.isError)
    return (
      <Alert variant="destructive">
        <AlertDescription>
          {result.error.message}
          <Button variant="outline" onClick={() => void result.refetch()}>
            Thử lại
          </Button>
        </AlertDescription>
      </Alert>
    );
  const data = result.data;
  const metrics = [
    {
      label: "Báo cáo chờ xử lý",
      value: data.pending_reports,
      detail: `${data.pending_delta > 0 ? "+" : ""}${number.format(data.pending_delta)} so với đầu ngày`,
    },
    {
      label: "Bài đang hoạt động",
      value: data.active_posts.LOST + data.active_posts.FOUND,
      detail: `${number.format(data.active_posts.LOST)} tìm đồ · ${number.format(data.active_posts.FOUND)} nhặt được`,
    },
    {
      label: "Người dùng",
      value: data.users.total,
      detail: `${number.format(data.users.new_this_week)} mới trong 7 ngày`,
    },
    {
      label: "Trao trả trong tuần",
      value: data.completed_this_week,
      detail: "Đã xác nhận bởi cả hai bên",
    },
  ];
  const max = Math.max(
    1,
    ...data.posts_by_day.flatMap((d) => [d.LOST, d.FOUND]),
  );
  const ceiling = Math.ceil(max / 4) * 4;
  return (
    <div className="dashboard">
      <dl className="metrics">
        {metrics.map(({ label, value, detail }) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>{number.format(value)}</dd>
            <dd className="metric-detail">{detail}</dd>
          </div>
        ))}
      </dl>
      <div className="analytics-row">
        <section className="panel chart-panel" aria-labelledby="chart-heading">
          <div className="section-heading">
            <h2 id="chart-heading">Bài đăng trong 7 ngày qua</h2>
            <div className="chart-legend">
              <span>Tìm đồ</span>
              <span>Nhặt được</span>
            </div>
          </div>
          <div className="chart-area">
            <div className="chart-axis" aria-hidden="true">
              {[4, 3, 2, 1, 0].map((tick) => (
                <span key={tick}>{number.format((ceiling * tick) / 4)}</span>
              ))}
            </div>
            <div
              className="bar-chart"
              aria-label="Số bài tìm đồ và nhặt được theo ngày"
            >
              {data.posts_by_day.map((d) => (
                <div className="bar-column" key={d.date}>
                  <div className="bar-pair">
                    {(["LOST", "FOUND"] as const).map((type) => (
                      <div
                        key={type}
                        className={`bar-track ${type.toLowerCase()}`}
                      >
                        <span>{number.format(d[type])}</span>
                        <div
                          className="bar"
                          style={{ height: `${(d[type] / ceiling) * 180}px` }}
                        />
                      </div>
                    ))}
                  </div>
                  <small>
                    {new Date(d.date + "T00:00:00").toLocaleDateString(
                      "vi-VN",
                      {
                        day: "2-digit",
                        month: "2-digit",
                      },
                    )}
                  </small>
                </div>
              ))}
            </div>
          </div>
          <details className="chart-data">
            <summary>Xem số liệu theo ngày</summary>
            <table>
              <caption>Số bài đăng theo ngày (Asia/Ho_Chi_Minh)</caption>
              <thead>
                <tr>
                  <th>Ngày</th>
                  <th>Tìm đồ</th>
                  <th>Nhặt được</th>
                </tr>
              </thead>
              <tbody>
                {data.posts_by_day.map((d) => (
                  <tr key={d.date}>
                    <th scope="row">{d.date}</th>
                    <td>{d.LOST}</td>
                    <td>{d.FOUND}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </details>
        </section>
        <section
          className="panel return-panel"
          aria-labelledby="return-heading"
        >
          <h2 id="return-heading">Hiệu quả trao trả</h2>
          <p className="return-rate">
            {number.format(data.completion_rate)}
            <span>%</span>
          </p>
          <p className="muted">
            Tỷ lệ bài đã trao trả trên tổng số bài chưa xóa.
          </p>
          <dl className="breakdown">
            <div>
              <dt>Tìm đồ đang hoạt động</dt>
              <dd>{number.format(data.active_posts.LOST)}</dd>
            </div>
            <div>
              <dt>Nhặt được đang hoạt động</dt>
              <dd>{number.format(data.active_posts.FOUND)}</dd>
            </div>
          </dl>
        </section>
      </div>
      <section className="panel recent-panel catalog-surface">
        <div className="section-heading">
          <h2>Báo cáo mới nhất</h2>
          <Button variant="ghost" onClick={() => openReports()}>
            Xem tất cả <ArrowRight aria-hidden="true" />
          </Button>
        </div>
        <DataTable
          rows={data.recent_reports}
          action={(row) => ({
            label: "Xem",
            run: () => openReports(String(row.id)),
          })}
          columns={[
            { key: "code", label: "Mã" },
            { key: "target_type", label: "Loại" },
            { key: "target_label", label: "Đối tượng" },
            { key: "reason", label: "Lý do" },
            { key: "created_at", label: "Thời gian" },
            { key: "status", label: "Trạng thái" },
          ]}
        />
      </section>
    </div>
  );
}
