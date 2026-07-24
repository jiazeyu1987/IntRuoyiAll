# Task: DCC 纸质发放确认前端闭环

## Goal

在 `DCC受控文件详情` 页为 `PAPER` 分发记录增加“确认纸质发放”动作按钮，并在动作成功后刷新分发状态。

## Scope

- 为前端 workflow API 增加纸质发放确认请求。
- 在详情页分发表格中为 `PAPER` 行增加确认按钮。
- 成功后刷新当前详情页状态。
- 不改训练页，不补更复杂的签收字段展示。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260517-dcc-distribution-medium-detail-display/task.md`
- Status before this task: completed for code delivery.
- Impact: medium detail display is already green, so this task only adds the
  first PAPER row action.

## Milestones

- [x] M1: Create this frontend task package before code edits.
- [x] M2: Record BDD scenarios and RED evidence for missing PAPER action.
- [x] M3: Implement the minimal detail-page PAPER acknowledge action.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only task-scoped files if verification fully passes.

## Expected Verification

- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-paper-distribution-ack-frontend\frontend-feature-evidence.md`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-paper-distribution-ack-frontend run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-paper-distribution-ack-frontend\scripts\verify-dcc-paper-distribution-ack-frontend.mjs`

## Current Status

Completed for frontend code delivery. The detail page now exposes a PAPER-only
acknowledge action and real detail-page verification is green against a runtime
PAPER fixture row.

## Blocker And Impact

- Blocker: a task-scoped frontend commit is not yet safe because the repository
  still contains unrelated in-progress DCC upload / training changes in other
  files and task directories.
- Impact: the PAPER action slice is implemented and verified, but a clean
  task-only commit still needs a narrower or cleaner write set.

## Final Verification Result

- RED:
  - no PAPER action existed on the detail page before this task.
- GREEN:
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-paper-distribution-ack-frontend run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-paper-distribution-ack-frontend\scripts\verify-dcc-paper-distribution-ack-frontend.mjs` -> PASS
- Real path result:
  - controlled file id: `51`
  - distribution id: `12`
  - before: `纸质发放 / 待分发 / 确认纸质发放`
  - after: `纸质发放 / 已确认`
  - backend request:
    `POST /admin-api/dcc/controlled-files/51/paper-distributions/12/acknowledge`
  - backend response code: `0`

## Cleanup Keep

- `doc/tasks/20260517-dcc-paper-distribution-ack-frontend/frontend-feature-evidence.md`
- `doc/tasks/20260517-dcc-paper-distribution-ack-frontend/scripts/verify-dcc-paper-distribution-ack-frontend.mjs`
