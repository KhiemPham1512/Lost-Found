import { useEffect, useRef, useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  House,
  Flag,
  Layers,
  Users,
  ScrollText,
  LogOut,
  CalendarDays,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Catalog } from "./Catalog";
import { Dashboard } from "./Dashboard";
import type { Tab } from "./types";

const links = [
  { tab: "stats", label: "Tổng quan", icon: House },
  { tab: "reports", label: "Báo cáo", icon: Flag },
  { tab: "posts", label: "Bài đăng", icon: Layers },
  { tab: "users", label: "Người dùng", icon: Users },
  { tab: "actions", label: "Nhật ký", icon: ScrollText },
] as const;

const descriptions: Record<Tab, string> = {
  stats: "Theo dõi hoạt động và các nội dung cần xử lý.",
  reports: "Xem xét báo cáo và xử lý nội dung vi phạm.",
  posts: "Theo dõi bài tìm đồ, nhặt được và trạng thái bài đăng.",
  users: "Quản lý tài khoản và thông tin liên hệ đã được che.",
  actions: "Tra cứu lịch sử thao tác của quản trị viên.",
};

export function AdminShell({ logout }: { logout: () => Promise<void> }) {
  const heading = useRef<HTMLHeadingElement>(null);
  const [tab, setTab] = useState<Tab>("stats");
  const [reportId, setReportId] = useState<string>();
  // The cache belongs to this authenticated shell and is discarded on logout.
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: { queries: { retry: false, staleTime: 15000 } },
      }),
  );
  const active = links.find((l) => l.tab === tab)!;
  useEffect(() => {
    heading.current?.focus();
  }, [tab]);
  return (
    <QueryClientProvider client={queryClient}>
      <div className="admin-layout">
        <a className="skip-link" href="#main-content">
          Đến nội dung chính
        </a>
        <aside className="sidebar">
          <div className="sidebar-brand">
            Lost &amp; Found<span>Quản trị hệ thống</span>
          </div>
          <nav aria-label="Quản trị">
            {links.map((link) => (
              <button
                key={link.tab}
                className={cn("nav-link", tab === link.tab && "nav-active")}
                aria-current={tab === link.tab ? "page" : undefined}
                onClick={() => {
                  setReportId(undefined);
                  setTab(link.tab);
                }}
              >
                <link.icon aria-hidden="true" />
                {link.label}
              </button>
            ))}
          </nav>
          <button className="nav-link logout" onClick={() => void logout()}>
            <LogOut aria-hidden="true" />
            Đăng xuất
          </button>
        </aside>
        <main id="main-content" className="workspace" tabIndex={-1}>
          <header className="page-header">
            <div>
              <h1 ref={heading} tabIndex={-1}>
                {active.label}
              </h1>
              <p>{descriptions[tab]}</p>
            </div>
            <p className="page-date">
              <CalendarDays aria-hidden="true" />
              {new Date().toLocaleDateString("vi-VN", {
                weekday: "long",
                day: "numeric",
                month: "long",
                year: "numeric",
              })}
            </p>
          </header>
          {tab === "stats" ? (
            <Dashboard
              openReports={(id) => {
                setReportId(id);
                setTab("reports");
              }}
            />
          ) : (
            <Catalog
              key={`${tab}-${reportId ?? "list"}`}
              tab={tab}
              initialReportId={reportId}
            />
          )}
        </main>
      </div>
    </QueryClientProvider>
  );
}
