export type Tab = "stats" | "reports" | "posts" | "users" | "actions";
export type CatalogRow = {
  id: string | number;
  code?: string;
  title?: string;
  target_label?: string;
  target_type?: string;
  reporter?: { id: string; username: string };
  status?: string;
  reason?: string;
  type?: string;
  username?: string;
  role?: string;
  phone_masked?: string | null;
  joined_month?: string;
  is_banned?: boolean;
  post_count?: number;
  report_count?: number;
  action?: string;
  admin_username?: string;
  report_code?: string | null;
  note?: string | null;
  author_username?: string;
  author_id?: string;
  category_name?: string;
  created_at?: string;
};

export type ReportUser = {
  id: string;
  username: string;
  is_banned: boolean;
  post_count: number;
  reported_before_count: number;
};
export type ReportDetailData = {
  id: string;
  code: string;
  reason: string;
  description: string | null;
  status: string;
  created_at: string;
  resolution_action: string | null;
  resolution_note: string | null;
  reporter: { id: string; username: string };
  target: {
    type: "POST" | "USER" | "CONVERSATION";
    post: {
      id: string;
      title: string;
      description: string;
      status: string;
      location_text: string;
      image_paths: string[];
      author: ReportUser | null;
    } | null;
    user: ReportUser | null;
    conversation: {
      id: string;
      participants: string[];
      is_locked: boolean;
      message_count: number;
    } | null;
  };
  reports_on_target: {
    id: string;
    code: string;
    reason: string;
    description: string | null;
    reporter_username: string;
  }[];
  admin_actions: {
    id: number;
    action: string;
    note: string | null;
    created_at: string;
    admin_username: string;
  }[];
};
export type PageData = { total: number; items: CatalogRow[] };
export type Stats = {
  pending_reports: number;
  pending_delta: number;
  active_posts: { LOST: number; FOUND: number };
  users: { total: number; new_this_week: number };
  completed_this_week: number;
  completion_rate: number;
  posts_by_day: { date: string; LOST: number; FOUND: number }[];
  recent_reports: CatalogRow[];
};
