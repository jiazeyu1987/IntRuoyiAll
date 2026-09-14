# P4 Runtime Acceptance Preflight

## Status

Prepared only. No database migration, runtime restart, browser launch, login, business write, API probe, or remote action was executed during this code-gate pass.

The current main workspace contains unrelated concurrent changes. Runtime acceptance must start only after the reviewed P4 commit is available in a clean detached worktree and the main Agent confirms the build/runtime ownership boundary.

## Tooling

- Playwright prerequisite: `npx` is available on this machine.
- Frontend project: `E:\IntRuoyi\IntRuoyiFronted`.
- Backend project: `E:\IntRuoyi\IntRuoyiBackend`.
- Canonical local restart tool: `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1`.
- Migration policy gate: `IntRuoyiBackend\script\release\run-release-migration-policy-gate.py`.
- Main runtime ports: frontend `8081`, backend `48081`. Ownership and process identity must be checked immediately before any restart.
- Required feature migrations: platform message business key, publication follow-up, impact assessment, and publication notification, with their complete dependency closure.

## Identity And Secrets

- Tenant label: `芋道源码`.
- Document-control operator: the approved DCC admin/document-control account.
- Impact assignee: the approved `zhaojie` account from the existing secure thread context.
- Passwords and tokens must be supplied only through the runtime secret mechanism. They must not appear in commands saved to the task log, scripts, screenshots, traces, JSON evidence, or console excerpts.
- A fresh UI login is required for every actor switch. Existing tokens or direct storage injection are forbidden.

## Task Data

- Every new file name and number must include `DCC-P4-<timestamp>`.
- Use at least two new controlled files in the same DCC project and one valid formal relation created through the upload page.
- Source documents must be dedicated local test files supplied at runtime. Existing business files must not be reused or modified.
- The relation must resolve an active responsible user so an impact task and recipient are observable.
- If an open major revision already exists, the UI must use “关联现有版本”. Otherwise it must select an actual iteration such as A/1 or A/2 in the revision dialog and create the next major revision.

## Verified UI Entry Points

- Upload: `/dcc/controlled-file/upload`; visible upload form, source-file picker, `dcc-upload-related-files-select`, and submit action exist.
- Controlled browser: `/dcc/controlled-file/browser`; publish action anchor `dcc-controlled-browser-publish` exists.
- Detail: `/dcc/controlled-file/detail/:id`; publication follow-up and `dcc-detail-publication-timeline` exist behind current VIEW authorization.
- Workbench: `/dcc/controlled-file/workbench`; `dcc-workbench-impact-assessments`, start/decision actions, revision-options dialog, create/link branches, and pagination exist.
- Management: `/dcc/controlled-file/publication-followup`; notification retry and `dcc-management-publication-timeline` exist behind document-control permissions.
- Inbox: current “我的站内信” detail uses the DCC publication target and routes through the normal controlled-file detail viewer.

## Future Playwright Path

1. Fresh-login as the document-control actor through the visible login form.
2. Through the upload page create the related file, complete its visible approval/publish path, and confirm ACTIVE in the UI.
3. Through the upload page create the publishing file with the task marker and select the related file in the visible relation control.
4. Complete approval and publish only through visible DCC/approval-center actions. Confirm the new version ACTIVE, previous version SUPERSEDED, and the detail follow-up timeline visible.
5. Open “我的站内信” as an actual recipient and use the visible DCC shortcut. Confirm current VIEW behavior; the message itself must not grant access.
6. Fresh-login as the impact assignee, open “我的影响评估”, start a task, submit “无需升版” with a reason, and verify it leaves default work.
7. On another task submit “需要升版”. Confirm it remains visible. Open revision options and either select a real small version to create the next major revision or associate the one authoritative open revision.
8. Confirm the linked task remains visible until that concrete revision is published, then becomes resolved and leaves default work.
9. Fresh-login as document control, open the publication-followup management page, inspect the complete server-projected timeline, and retry only an actually PENDING/FAILED notification when a deterministic runtime failure fixture is available.
10. After all UI writes, use only read-only page-triggered network observations and approved read-only database queries to reconcile version pointers, batch uniqueness, recipients/reasons, notification message IDs/status, impact decisions/tracking, and audit order.

## Runtime Stop Conditions

- Dirty or non-reviewed build input, unknown port owner, missing migration dependency, failed health check, stale menu cache after fresh login, missing DCC project/category/taxonomy, unavailable dedicated source files, missing actor role/permission, or no valid same-project relation blocks the run.
- Missing deterministic notification failure data blocks only the real FAILED-to-SENT retry branch; it must not be replaced by API calls, database writes, mock delivery, or fabricated success.
- Any page path requiring `fetch`, Axios, Playwright request context, token injection, direct database write, or hidden route invocation instead of visible controls fails the acceptance run.

## Planned Evidence

- Record task marker, actor labels, visible page steps, resulting controlled-file IDs, screenshot names, console/page errors, and read-only reconciliation counts.
- Redact login inputs, authorization headers, cookies, tokens, and all credentials.
- Store runtime artifacts only under this task directory after the main Agent authorizes the detached-worktree runtime pass.
