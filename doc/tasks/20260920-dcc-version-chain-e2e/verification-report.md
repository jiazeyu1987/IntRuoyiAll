# 20260920 DCC 版本链 E2E Verification Report

## Result

PASS（业务验收范围）。

真实 Playwright 页面完成了任务自有 DCC 文件的完整版本链：

`A/1 ACTIVE -> A/2 ACTIVE（小版本） -> B/1 ACTIVE（从指定 A/2 创建的大版本）`

最终 B/1 替代旧正式版本，当前详情页显示 `ACTIVE / B/1`。

## Runtime

- Branch: `int_qms`
- Frontend: `http://127.0.0.1:8061`
- Backend: `http://127.0.0.1:48061`
- Tenant/user: `芋道源码/admin`
- Project: ID `262`, code `CODEX-DCC-PROD-20260920012155`
- File number: `DCC-VCHAIN-202609200151`

## Real Page Evidence

1. A/1 creation used the upload page and returned HTTP 200/business code 0 for preview upload and controlled-file submit.
2. A/1 to A/2 used the browser page checkout, checkin type `小版本`, source upload, and checkin. The checkin response returned A/2 with predecessor A/1 and matching source hashes.
3. The browser page explicitly selected historical A/2, then submitted approval. Four approval/signature stages completed in the real approval pages; A/2 became ACTIVE.
4. The browser page checked out A/2, selected `大版本`, uploaded a new source, and generated B/1. B/1 was submitted and approved through the real approval center and signature page.
5. Final detail and controlled-browser pages show B/1 ACTIVE.

## History Assertions

The final version-history table visibly contains:

- B/1 direct source `#2054545668044084002` (A/2), and A/2 direct source `#2054545668044084001` (A/1).
- Previous/current content hashes, including `4f75621c9396 -> 4f75621c9396`.
- Operator and time, displayed as `瑛泰管理员 (admin)` with timestamps.
- Approval/publish results: B/1 `已生效` and `当前有效`; A/1 and A/2 `已替代` with successor versions.

## Evidence Files

- `artifacts/initial-version-chain-real.json`
- `artifacts/minor-version-real.json`
- `artifacts/submit-minor-approval-real.json`
- `artifacts/a2-approval-real.json`
- `artifacts/major-version-real.json`
- `artifacts/final-detail.png`

## Test Boundary

All business writes, checkouts, checkins, approval submissions, approvals, signature actions, and uploads were performed by Playwright through the real frontend. API responses were captured passively from Playwright response events. No direct API, database write, mock, or fallback was used for business actions.

The existing runtime emitted incidental `ERR_CONNECTION_REFUSED`/`hm.gif` and OnlyOffice static-resource noise during page rendering; these did not affect the accepted DCC lifecycle actions or final page assertions.

## RED/GREEN

- RED: initial locator timing and remote project-option matching needed correction; no business submission was accepted by those failed attempts.
- RED: after A/2 checkin, the browser default remained on active A/1 until the requested historical A/2 was explicitly selected.
- GREEN: corrected real-page flows and final detail assertions passed with B/1 ACTIVE and complete history metadata.

## Current Status

ready_for_closeout

Cleanup preview/apply both passed. The task remains `ready_for_closeout` because this run was authorized for runtime/E2E execution, not Git commit/push; the shared worktree also contains unrelated changes.

## Cleanup Keep

- doc/tasks/20260920-dcc-version-chain-e2e/task.md
- doc/tasks/20260920-dcc-version-chain-e2e/execution-log.md
- doc/tasks/20260920-dcc-version-chain-e2e/verification-report.md
- doc/tasks/20260920-dcc-version-chain-e2e/create-initial-version-real.e2e.cjs
- doc/tasks/20260920-dcc-version-chain-e2e/minor-version-real.e2e.cjs
- doc/tasks/20260920-dcc-version-chain-e2e/submit-minor-approval-real.e2e.cjs
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/initial-version-chain-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/minor-version-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/submit-minor-approval-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/a2-approval-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/major-version-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/final-detail.png
