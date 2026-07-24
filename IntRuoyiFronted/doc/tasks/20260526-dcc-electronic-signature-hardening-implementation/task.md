# Task: DCC 电子签名强化前端实现

## Goal

按后端实现 worktree 中已放行的 DCC 电子签名强化文档实现前端相关功能，并与同名后端分支联调。主实现台账位于后端仓库 `doc/tasks/20260526-dcc-electronic-signature-hardening-implementation/`。

## Scope

- 更新 DCC 签名 API 类型和调用。
- 更新受控文件详情签名弹窗、签名留痕、签名管理页和授权管理页。
- 增加证据校验、授权审计、锁定/解锁、导出证据入口和错误展示。
- 不增加测试专用 UI，不使用 mock 数据，不隐藏后端错误。

## Milestones

- [x] M0: Create frontend task record.
- [x] M1: Add RED frontend type/static checks for new contracts.
- [x] M2: Implement API and UI changes.
- [x] M3: Run lint/type checks and support reviewer E2E.

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-implementation\yudao-ui-admin-vue3 exec eslint src/api/dcc src/views/dcc/controlled-file`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-implementation\yudao-ui-admin-vue3 ts:check`
- Playwright real frontend path from `http://localhost:8081` after backend and test data are ready.

## Current Status

Completed.

Reviewer status: `GO / COMPLETED` on 2026-05-27.

Frontend API/UI changes, direct ESLint verification, full frontend typecheck, and the reviewer-owned real browser E2E are present:

- PASS: `pnpm ts:check`.
- PASS: `node node_modules\eslint\bin\eslint.js src/api/dcc src/views/dcc/controlled-file`.
- PASS: reviewer full real browser E2E from frontend `http://localhost:8095` against backend `http://127.0.0.1:48095` and fresh clone DB `ruoyi_vue_pro_dcc_sign_e2e_20260527_1900`, final output `GREEN: DCC electronic signature hardening real frontend E2E PASS`.

Round 5 fixed the remaining typecheck OOM by restoring the required AutoImport type declaration and upgrading frontend `vue-tsc` from `1.8.27` to `2.2.12` while keeping TypeScript `5.3.3`, DCC business source coverage, and Vue SFC type checking intact.

Round 26 fixed the real signer filter usability gap exposed by full E2E: DCC simple-user labels now include `username`, so the `签名人` Element Plus filterable select can match accounts such as `aoteman` even when the nickname is `芋道1` and department is empty. Targeted label test, ESLint, `pnpm ts:check`, and `git diff --check` passed.

Rebase closeout preserved `int_main` DCC approval-print, fourth-node file collection, electronic distribution, return/transfer/add-sign UI, and wrong-password field handling while retaining the electronic-signature response validation and evidence labels. Targeted node tests, targeted ESLint, `pnpm ts:check`, and `git diff --check` passed after conflict resolution.

## Historical Blockers And Impact

- RESOLVED: real test tenant users, real DCC files, assigned approval tasks and completed export file were prepared in fresh clone DB `ruoyi_vue_pro_dcc_sign_e2e_20260527_1900`.
- RESOLVED: the consumed clone DB issue was avoided by creating a fresh/reset clone dataset for final E2E.
- RESOLVED FOR VERIFICATION: direct `node node_modules\eslint\bin\eslint.js ...` and `pnpm ts:check` passed.
- CLEANUP PREVIEW: task-closeout-cleanup preview is recorded from the reviewer closeout pass; no frontend deliverable depends on cleanup apply.

## Final Closeout Status

- STATUS: COMPLETED / GO on 2026-05-27.
- REVIEW: backend review-fix-loop round 40 passed with no blocking issues.
- FINAL REGRESSION: `node --test` frontend label/export tests, targeted ESLint, `pnpm ts:check`, real browser E2E evidence, and `git diff --check` passed.
- REBASE REGRESSION: `node --test scripts\dcc-controlled-file-simple-user-label.test.mjs`, `node --test scripts\dcc-signature-evidence-export.test.mjs`, targeted `pnpm exec eslint`, `pnpm ts:check`, and `git diff --check` passed after rebasing onto `int_main`.
- CLEANUP PREVIEW: task-closeout-cleanup preview was run and blocked safely because no checked-out worktree for main branch `master` was found. No cleanup apply or deletion was performed.
- NO SIDE EFFECTS: No test-only UI, mock data, hidden backend errors, fallback export path, or API-only signing shortcut was introduced.
