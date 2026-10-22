# DCC Permission Isolation E2E

## Goal

Use real frontend Playwright E2E on the IntRuoyi test tenant to verify the complete DCC permission-isolation flow:

- Name permission only: the user can search controlled files and view metadata, but cannot preview or download content.
- Content permission: the user can preview and download content only when content permission is granted.
- Edit, approval, and association permissions are effective independently and do not imply each other.

## Constraints

- Tenant: 芋道源码.
- Login account supplied by task instructions: admin / admin123.
- E2E business actions must be completed through the real frontend page by Playwright.
- No direct API or database writes are allowed for acceptance actions.
- API/DB may not replace frontend business actions or frontend assertions.
- The user explicitly authorized restarting services for this task.
- Do not stop or restart the int_main backend service or occupy port 48081.
- Follow branch runtime port matrix for current branch runtime.
- Do not commit, push, operate remote servers, or use sub agents without explicit current-turn authorization.

## Given / When / Then

### Name Permission

Given a task-owned controlled document and a task user/role with name-level visibility but without content permission,
When the user searches the DCC controlled-file browser and opens the document metadata,
Then the document name and metadata are visible, and preview/download/edit/approval/association actions are unavailable or blocked by the frontend authorization result.

### Content Permission

Given the same task-owned controlled document and a task user/role with content permission,
When the user opens the document detail page from the real frontend,
Then preview and download actions are available and complete through the browser UI, while unrelated edit/approval/association actions remain unavailable unless separately granted.

### Edit Permission

Given a task user/role with edit permission but without approval or association permission,
When the user opens the controlled document detail page,
Then metadata edit actions are available, and approval/association actions remain unavailable.

### Approval Permission

Given a task user/role with approval permission for the relevant workflow stage but without edit or association permission,
When the user opens an approval task or approval-capable document from the real frontend,
Then approval actions are available only in that approval context, and edit/association actions remain unavailable.

### Association Permission

Given a task user/role with association permission on the source project but without content/edit/approval permission,
When the user searches a target document and creates or maintains an association through the real frontend,
Then association actions are available, target metadata visibility follows name permission, and target preview/download remain governed by content permission.

## Milestones

- Read required docs and record runtime constraints.
- Confirm current branch and permitted runtime ports.
- Start or restart the current branch runtime without touching int_main.
- Discover existing DCC frontend pages and controls through real frontend Playwright.
- Execute permission-isolation E2E with real frontend accounts and task-owned data.
- Save screenshots, traces, and logs under this task directory.
- Mark ready_for_closeout after verification evidence is recorded.
- Mark completed only after task-closeout requirements can be satisfied.

## Current Status

ready_for_closeout

## Status

ready_for_closeout

Verified through real frontend Playwright:

- `zhaojie`: can search and view controlled-file metadata for `CODEX 文件上传流程测试 20260808`, with no preview and no download actions exposed.
- `wangsiyu`: can preview and download the same controlled file; preview metadata and preview endpoints both returned 200, and the controlled download endpoint returned 200 after the real confirmation dialog.
- `admin`: can preview/download and see richer traceability, approval, and association panels for the same controlled file.
- `admin`: has the `修改基础信息` row menu, an enabled `关联文档入口`, the real `/mdm/project-code` maintenance page, and DCC approval-center todo rows with `审核` actions.
- `wangsiyu`: has content preview/download, the edit menu, and the real `/mdm/project-code` maintenance page with association actions, but no approval-center todo/review action.
- `zhaojie`: has metadata visibility only for content, edit, association, and approval gates: preview/download/edit are absent, association is disabled, and approval center is empty.

Remaining gap:

- The source and E2E route contract was corrected from the obsolete `/mes/md/dcc-project-code` path to the registered `/mdm/project-code` path; real browser reruns now load the maintenance page for `admin` and `wangsiyu`.
- The write E2E completed metadata edit/save and restore, an approval rejection, and association assignment creation through the real frontend. The association assignment was then revoked through the real frontend cleanup action.

Write-flow evidence:

- `permission-isolation-edit-approval-write.json`: final edit and approval write evidence.
- `permission-isolation-association-write.json`: association assignment creation passed.
- `permission-isolation-association-cleanup.json`: association assignment revoke passed and final status was `REVOKED`.

## Cleanup Keep

- doc/tasks/20260920-dcc-permission-isolation-e2e/permission-isolation-browser-check.e2e.cjs
- doc/tasks/20260920-dcc-permission-isolation-e2e/permission-isolation-write-e2e.cjs
- doc/tasks/20260920-dcc-permission-isolation-e2e/task.md
- doc/tasks/20260920-dcc-permission-isolation-e2e/execution-log.md
- doc/tasks/20260920-dcc-permission-isolation-e2e/verification-report.md
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-write-e2e.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-edit-approval-write.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-association-write.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-association-cleanup.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-admin-post-write.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-wangsiyu-post-write.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-zhaojie-post-write.json
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-browser.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-preview.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-detail.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-association-management.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-approval-center.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/wangsiyu-browser.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/wangsiyu-preview.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/wangsiyu-detail.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-browser.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-detail.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-association-management.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-approval-center.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/edit-restored.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/approval-rejected.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/association-created.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/association-revoked.png
- doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-edit-trace.zip
