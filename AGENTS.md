# Repository Guidelines

## Project Structure & Module Organization

- `backend/app/` contains the FastAPI application: `routers/` handles HTTP, `schemas/` defines Pydantic models, `services/` implements business logic and SQL, and `core/` provides configuration and authentication. `db.py` manages database connections.
- `supabase/schema.sql` defines tables, triggers, row-level security (RLS), functions, and seed data; incremental SQL changes live in `supabase/patches/`.
- `docs/api.md` is the API contract; `docs/decisions.md` records architectural decisions.
- `dev/` contains token helpers and example account configuration.
- `admin-web/src/` contains the Vite/React admin application: authentication, dashboard, catalogs and moderation. `admin-web/tests/` contains local Playwright tests. `mobile/` remains a placeholder in this checkout.

## Build, Test, and Development Commands

Run backend setup from PowerShell:

```powershell
cd backend
py -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
Copy-Item .env.example .env
python -m uvicorn app.main:app --reload
```

Copy the environment template only on initial setup, fill in local values, and restart the server after configuration changes. Visit `http://127.0.0.1:8000/health` for a health check and `/docs` for interactive API checks. From the repository root, `py dev\gettoken.py admin` retrieves a configured development account token.

## Coding Style & Naming Conventions

Follow existing Python conventions: four-space indentation, `snake_case` modules and functions, and `PascalCase` Pydantic models. Keep routers thin and business logic in services. Register new routers in `app/main.py`. Use `python -m ruff format` for changed Python files; keep frontend TypeScript and JSX consistently formatted.

## Testing Guidelines

Use TDD for behavior changes: agree the public interface under test, write one failing behavioral test, run it to establish the failure, implement the smallest change, and rerun it to green before the next slice. Test through HTTP or visible browser behavior; mock external Auth/API boundaries, not internal React components. Use existing tests as regression checks. Visual styling changes also require browser inspection at desktop and mobile sizes.

From `backend/`, use local `requirements-dev.txt`, then run `python -m pytest` and `python -m mypy app`. Pytest starts an isolated embedded PostgreSQL cluster under `.test-postgres/`; tests must never use the real `.env` database. From `admin-web/`, run `npm ci` and `npm run build`. If the local Playwright tools are present, run `npx --no-install playwright test --reporter=line`; there is no shared `npm test` script. On a clean checkout, test files and test-only dependencies must be provisioned locally before those checks can run; do not silently add them to the shared manifest. No coverage percentage is mandated. Check successful requests, invalid inputs, authorization, lifecycle effects and concurrency as appropriate to the change. Record verification results in the PR and update `docs/api.md` with implementation status separately from deployment status.

Before opening a PR, review the diff from the feature's pinned `develop` base along two axes: repository standards and the requested specification. Resolve material findings and rerun affected checks. Report remaining limitations honestly; passing UI mocks does not verify the deployed API or database.

## Commit & Pull Request Guidelines

History uses concise messages such as `feat: add admin guard and report list/detail endpoints`. Follow that pattern with a clear action and scope. Backend feature branches follow `feat/a-...`; open PRs into `develop`. Describe behavior changes, link relevant issues, and include verification results and database changes. Merging into `develop` triggers backend deployment.

Use `develop` as the integration branch and keep `main` for validated demo releases. Start each feature on a separate branch from `develop`, finish and verify it there, then open a PR into `develop`. Do not mix a new feature into an unfinished feature branch or push directly to `develop` or `main`. Promote tested work from `develop` to `main` for a demo release; do not merge into `main` merely to keep it synchronized. Distinguish work present on a feature branch from work merged into `develop` and work verified on the deployed service.

Before merging any PR, explain its concrete changes, target branch, verification, remaining limitations and deployment impact to the user, then ask for their explicit approval and wait for it. Complete implementation and review before asking so the user can approve a finished result. Permission to develop or open a PR is not permission to merge. After approval, the agent may merge that reviewed PR and verify integration/deployment; if its scope changes before merging, explain the changes and obtain approval again.

Group routine documentation, QA and deployment-verification updates with the next related feature or substantial change, or accumulate a coherent, substantial documentation batch before opening a documentation-only PR. Do not open separate PRs for small documentation edits, isolated verification results or each post-merge deployment snapshot. Record fresh evidence and pending shared-document updates promptly in the local handoff while waiting to include them. Features and fixes may have their own coherent PRs; keep required API documentation with the implementation and avoid splitting one change into trivial PRs or mixing unrelated features just to reduce PR count. Explicit user instructions to open or finish a particular PR take precedence.

Commit this root `AGENTS.md` so every team member's coding agent can follow the same rules. Keep `.agents/`, `GLOSSARY.md`, tests, test-only configuration, QA screenshots, temporary plans/reports and `.local-artifacts/` local unless the user changes that policy. Stage explicit source/documentation paths; do not use a blanket force-add to include ignored files.

## Documentation and UI Work

Read `README.md`, `docs/api.md` and `docs/decisions.md` before implementation. The API contract and recorded current decisions govern the existing implementation; the 30-day plan supplies project scope and milestones. If older planning text differs from the current contract, preserve the current contract and explain any proposed change. Do not invent API parameters, business states or dashboard metrics.

For admin UI work, apply the local `ui-ux-pro-max` skill when available. Use its guidance with product judgment: preserve the established teal identity, Geist typography and semantic tokens in `admin-web/src/index.css`; reuse existing shadcn/ui components and Lucide icons. Keep visible form labels, keyboard focus, meaningful loading/error/empty states, reduced-motion support and usable touch targets. Verify 375px, 768px, 1024px and 1440px layouts without viewport overflow. Do not require local skill files to exist on another member's checkout to follow these baseline rules.

The user has taken over C's web development scope (`admin-web/`); C's Android work remains separate. Always use `ui-ux-pro-max` for web design when available and preserve the functions defined in the [team Figma](https://www.figma.com/design/oq27gMLy9XfXWTeerrGQqy/LTTBDD-L-F?node-id=0-1). Inspect the relevant frame before changing a web flow. The current API contract and recorded decisions still govern behavior; document design/contract gaps instead of inventing fields, permissions or states. Figma is the functional reference; the established web identity and tokens above remain in force.

Admin web: `https://lost-found-admin.onrender.com`; API: `https://lost-found-api-ph8v.onrender.com`. Render deploys the integration branch `develop`. Vite variables are baked into the build; backend CORS must allow the frontend origin. Never merge or deploy solely to make a local UI check pass.

After completion and review, consolidate a fresh local handoff with branch/PR, verification and outstanding work. Follow the workflow and cleanup scope below; preserve project-wide context before removing obsolete feature artifacts.

## Quy trình bắt buộc cho mỗi vòng công việc

Áp dụng tuần tự `domain-modeling` → `tdd` → `implement` kết hợp `codebase-design` → `code-review` → sửa lỗi bằng `diagnosing-bugs` khi cần → PR → cập nhật handoff và dọn artifact. Đọc `SKILL.md` của skill tương ứng khi sử dụng; nếu không có skill trên máy, vẫn thực hiện các bước dưới đây và ghi rõ giới hạn, không tự cài hoặc giả nhận đã dùng skill. Hướng dẫn của người dùng và hợp đồng hiện hành của repo được ưu tiên.

1. **Đọc lại ngữ cảnh và xác định việc tiếp theo.** Đọc `.local-artifacts/handoff.md`, `AGENTS.md`, `README.md`, `docs/api.md`, `docs/decisions.md`, kế hoạch 30 ngày và issue/tài liệu liên quan; đối chiếu với code, trạng thái Git/PR và bằng chứng kiểm chứng. Đọc `GLOSSARY.md` nếu có. Dùng `domain-modeling` để làm rõ thuật ngữ, thực thể, quan hệ, trạng thái và quy tắc nghiệp vụ khi chúng thay đổi hoặc còn mơ hồ; cập nhật glossary ngay khi chốt thuật ngữ, ghi quyết định kiến trúc khi có đánh đổi đáng lưu lại. Không dùng glossary thay cho đặc tả hay ghi chú triển khai. Chọn việc chưa hoàn tất có ưu tiên cao nhất trong phạm vi đã được giao, ghi mục tiêu và tiêu chí nghiệm thu ngắn gọn trong handoff local. Không suy ra đã deploy từ việc merge PR.

2. **Chuẩn bị nhánh và viết test trước code.** Bắt đầu mỗi tính năng trên nhánh riêng từ `develop` đã cập nhật; ghi SHA base cố định để review. Dùng `tdd` cho thay đổi hành vi: xác định public interface cần kiểm thử và kết quả người dùng quan sát được. Tái sử dụng phạm vi HTTP/browser đã được người dùng chấp thuận trong repo hoặc handoff; chỉ hỏi khi thật sự cần bổ sung một interface chưa được chấp thuận. Với mỗi lát hành vi, viết một test, chạy để thấy lỗi đúng do hành vi còn thiếu, rồi mới sửa production code. Test lỗi setup/môi trường chưa phải bằng chứng red. Không viết hàng loạt test rồi mới triển khai tất cả. Thay đổi chỉ có tài liệu không cần tạo test hành vi; kiểm tra diff, tính nhất quán và liên kết/tham chiếu liên quan.

3. **Triển khai bằng `implement` và `codebase-design`.** Viết thay đổi nhỏ nhất để test hiện tại đạt, chạy lại đến green rồi mới chuyển sang lát tiếp theo. Thiết kế module có interface nhỏ, che giấu logic phức tạp, tập trung quy tắc nghiệp vụ tại nơi chịu trách nhiệm; giữ router mỏng, không thêm abstraction hoặc tính năng suy đoán. Test qua cùng public interface mà bên gọi sử dụng; mock ở ranh giới Auth/API bên ngoài, không mock nội bộ để làm test xanh. Chạy test tập trung và typecheck trong quá trình làm; chạy regression và các kiểm tra bắt buộc phù hợp khi hoàn tất. Với UI, áp dụng hướng dẫn và kiểm tra trình duyệt/kích thước màn hình đã quy định ở trên.

4. **Review và sửa lỗi cho đến khi đạt.** Dùng `code-review` trên toàn bộ diff so với SHA base đã ghim, kiểm tra độc lập hai trục: chuẩn repo và yêu cầu/tiêu chí nghiệm thu. Dùng sub-agent review song song nếu công cụ hỗ trợ; nếu không, review từng trục và ghi rõ giới hạn. Nếu phát hiện bug, dùng `diagnosing-bugs`: tạo vòng kiểm tra tái hiện đúng lỗi, thu nhỏ tình huống, nêu giả thuyết có thể kiểm chứng, kiểm tra từng giả thuyết rồi sửa nguyên nhân. Bổ sung test hồi quy thất bại trước bản sửa tại interface phù hợp, chạy lại tình huống ban đầu và kiểm tra liên quan. Với lỗi câu chữ/tài liệu, sửa rồi rà soát tính nhất quán. Chỉ refactor sau khi các test đã green; review lại phần thay đổi và chạy lại kiểm tra bị ảnh hưởng. Giải quyết mọi phát hiện quan trọng trước PR; ghi trung thực giới hạn còn lại.

5. **Hoàn tất PR và bàn giao.** Cập nhật hợp đồng/trạng thái triển khai khi có thay đổi và bằng chứng tương ứng. Với tính năng hoặc bản sửa có phạm vi rõ ràng, commit đúng các source/docs được phép chia sẻ và mở PR vào `develop` kèm mục tiêu, thay đổi, kết quả kiểm tra, review và phần còn chờ. Với cập nhật nhỏ chỉ có tài liệu/QA/kiểm chứng, ghi bằng chứng và phần tài liệu chung còn chờ vào handoff ngay; gom cùng PR tính năng/thay đổi đáng kể liên quan hoặc đợi một đợt tài liệu đủ lớn mới mở PR riêng. Không mở PR lắt nhắt cho từng kết quả kiểm tra hoặc deployment snapshot. Không tự merge chỉ để chuyển sang vòng tiếp theo; phải giải thích và chờ người dùng duyệt từng PR theo quy tắc trên. Cập nhật handoff local với nhánh, SHA base/HEAD, URL và trạng thái PR (hoặc ghi rõ đang gom, chưa mở PR), bằng chứng local/merged/deployed riêng biệt, quyết định còn hiệu lực, phụ thuộc, giới hạn và việc tiếp theo. Giữ test hồi quy local theo chính sách repo.

6. **Dọn artifact rồi lặp lại.** Sau khi PR đã tạo thành công và handoff mới đã được kiểm tra, áp dụng đúng phạm vi dọn dẹp dưới đây. Đọc lại handoff/tài liệu trước vòng kế tiếp; tiếp tục các việc thuộc phạm vi đã được người dùng giao cho đến khi đạt mục tiêu. Khi việc tiếp theo phụ thuộc PR chưa merge, giữ trạng thái chờ trong handoff hoặc chọn việc độc lập trong phạm vi đã giao; không trộn tính năng mới vào nhánh còn dang dở. Nếu thiếu quyền hoặc thông tin thiết yếu, ghi rõ điểm chặn và tiếp tục phần độc lập có thể làm.

Sau mỗi vòng hoàn tất, giải thích bằng tiếng Việt dễ hiểu: đã làm hoặc sửa gì, thay đổi đó giúp người dùng/nhóm giải quyết vấn đề nào, đã kiểm tra bằng cách nào và còn giới hạn gì. Kèm nhánh/PR và trạng thái merge/deploy thực tế, rồi nêu rõ nhiệm vụ kế tiếp, lý do ưu tiên và phụ thuộc cần có. Không chỉ liệt kê tên file, thuật ngữ kỹ thuật hoặc số test mà thiếu ý nghĩa của kết quả. Lưu cùng thông tin trong handoff để vòng sau tiếp tục đúng việc.

### Phạm vi dọn dẹp đã được người dùng chấp thuận

- Được tự dọn file/context và thư mục tạm do agent tạo cho một tính năng đã xong PR, khi đã đọc nội dung, xác nhận hết tác dụng và chuyển mọi thông tin còn cần sang handoff. Ví dụ: bản nháp PR, helper tạo PR đã hoàn tất, kế hoạch/context trùng lặp, log debug và output thử nghiệm có thể tạo lại. Ưu tiên đặt artifact mới trong `.local-artifacts/<tên-tính-năng>/`; với artifact cũ ở `.local-artifacts/`, xét từng đường dẫn cụ thể.
- Trước khi xóa, ghi danh sách đường dẫn và lý do trong handoff; dùng đường dẫn tuyệt đối đã resolve, kiểm tra chúng nằm trong workspace và đúng thư mục artifact của tính năng. Không đi qua symlink/junction ra ngoài phạm vi. Với thư mục, kiểm tra toàn bộ nội dung trước khi xóa đệ quy; không xóa theo tuổi file, wildcard rộng hoặc `git clean` toàn repo.
- **Tuyệt đối giữ ngữ cảnh toàn dự án:** `.local-artifacts/handoff.md` hiện hành, mục tiêu/phạm vi MVP, quyết định còn hiệu lực, hợp đồng API, trạng thái tích hợp/deployment đã kiểm chứng, phần chưa xong và phụ thuộc của nhóm. Không xóa một context chỉ vì tên/ngày đã cũ; hợp nhất thông tin còn giá trị rồi mới dọn bản thừa. Giữ bằng chứng kiểm tra duy nhất nếu vẫn cần cho review hoặc việc còn chờ.
- Quyền dọn artifact không cho phép xóa source, `docs/`, `AGENTS.md`, `GLOSSARY.md`, tests/test config, `.agents/` và skills, credentials/`.env`/account config, dữ liệu database, môi trường đã cài, hoặc tài sản thiết kế còn được tham chiếu. File ngoài phạm vi trên hoặc chưa rõ còn giá trị phải giữ lại và hỏi trước nếu cần xóa. Quyền dọn dẹp này thay thế yêu cầu xin phép lại cho từng artifact đủ điều kiện; không mở rộng quyền sửa lịch sử Git hay dữ liệu thật.

## Security & Architecture

Client reads use Supabase with RLS; business state changes go through FastAPI. Keep secret keys server-side. Never commit `.env`, `*.local.json`, passwords, or tokens. Treat `supabase/reset.sql` as destructive development-only tooling.

## Quyền hạn & Chế độ Tự chủ (Autonomous Execution)

1. **Nguyên tắc hành động:**
   - Luôn chủ động: Tự động phân tích, tự tạo file, sửa code và chạy test liên tục cho đến khi đạt được mục tiêu đã đề ra.
   - Tuyệt đối KHÔNG dừng lại giữa chừng để hỏi những câu mang tính xin phép hiển nhiên.

2. **Chỉ được phép dừng lại xin ý kiến khi:**
   - Trước mỗi lần merge PR: giải thích nội dung, kiểm chứng và ảnh hưởng triển khai, rồi chờ người dùng đồng ý rõ ràng theo quy trình phê duyệt ở trên.
   - Xóa file hoặc thư mục ngoài phạm vi artifact đã được chấp thuận ở trên, hoặc chưa chứng minh được là hết tác dụng (`rm`, `Remove-Item`, `git clean`).
   - Can thiệp vào lịch sử Git có nguy cơ mất dữ liệu (`git reset --hard`, `git push --force`).
   - Thao tác liên quan đến xóa bảng, drop cột hoặc ghi đè dữ liệu trên cơ sở dữ liệu Supabase/PostgreSQL.
   - Xuất hiện xung đột kiến trúc nghiêm trọng ngoài phạm vi quy định của `docs/`.
