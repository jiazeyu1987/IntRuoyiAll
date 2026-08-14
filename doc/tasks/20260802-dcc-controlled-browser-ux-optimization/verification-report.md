# Verification Report

## Scope

本报告记录 DCC 文控“受控浏览”前端体验优化的静态合同、类型检查、后端编译和真实 Playwright E2E 结果。验证范围仅限本场景：不同非 admin 角色在受控浏览中只能看到有权限的当前有效文件、打开正确发布/盖章预览、不误看草稿/历史失效版或无权限分类文件。

## Target Test Data

- Target file number: `CODX-DCC-ORIG-20260802101521`
- Target file title: `Codex DCC 原版上传链路 20260802101521`
- Target controlled file ID: `2054545668044070287`
- Expected version/status: `V1.0` / `ACTIVE`
- Controlled-browser directory path: `4.Ohter`
- Viewer directory path observed: `质量管理/4.Ohter`
- Category: `过程检验规程`
- Published file ID: `9198354916366`
- Stamped file ID: `9198354916366`
- Authorized account label: `wangsiyu`
- Lower-permission account label: `pengyunfeng`
- Permission difference under test: `wangsiyu` can see and preview the target current ACTIVE file in controlled browsing; `pengyunfeng` cannot see the same target file and receives a clear no-access/no-current-match state.

## Results

- Static contract: PASS.
  - `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js`
  - `node tests\e2e\dcc-browser-version-summary-static.spec.js`
  - `node tests\e2e\dcc-controlled-browser-viewer-linkage-static.spec.js`
  - `node tests\e2e\dcc-upload-governance-ux-static.spec.js`
- Type check: PASS.
  - `pnpm ts:check`
- Backend verification: PASS.
  - `mvn -pl yudao-module-dcc -am -DskipTests compile`
  - `mvn -pl yudao-server -am -DskipTests package`
- Read-only API/DB verification: PASS.
  - Target file state: `ACTIVE`
  - Current version: `V1.0`
  - Master current active controlled file ID: `2054545668044070287`
  - Category: `过程检验规程`
  - Published/stamped file IDs: `9198354916366` / `9198354916366`
  - View matrix rule count: `8`
- Real Playwright E2E: PASS.
  - Result file: `doc\tasks\20260802-dcc-controlled-browser-ux-optimization\dcc-controlled-browser-ux-real-e2e-result.json`
  - Authorized path: `wangsiyu` opened `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-ORIG-20260802101521&pageNo=1&pageSize=20`; browser total `1`; visible version/status `V1.0` / `ACTIVE`; row directory path `4.Ohter`.
  - Preview result: viewer route `/dcc/controlled-file/detail/2054545668044070287?viewer=1&from=browser...`; preview loaded `true`; preview kind `PDF`; preview file name `stamped-approval-sample.pdf`; published/stamped IDs `9198354916366` / `9198354916366`.
  - Viewer metadata result: page asserted current-effective row summary, distinct preview/trace/signature actions, business-readable published/stamped linkage, advanced published/stamped IDs, and final directory path.
  - Lower-permission path: `pengyunfeng` opened the same controlled-browser search path; browser total `0`; visible target count `0`; page feedback `无权限或无匹配当前有效文件`; target row not rendered.
  - Draft/history isolation: target chain in this run is the task-owned original `V1.0` ACTIVE file; no task-owned historical or draft sibling was present in the final read-only result set, and the browser/viewer asserted the current ACTIVE published/stamped file instead of a non-current file.
- Target link error count: `0`.
- Target DCC mutation request count: `0`.
- Network/page safety: `targetNetworkFailures=[]`, `nonTargetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`, `dccMutationRequests=[]`.

## PASS/BLOCKED Summary

- PASS: implemented the requested DCC controlled-browser UX improvements: current-effective row summary, version and directory visibility, readable published/stamped state, explicit no-access/no-current-match empty state, stable filter/path summary, distinct current preview/trace/signature entries, publish completion visibility, upload preflight permission visibility, and draft/history isolation copy.
- PASS: real Playwright operated the frontend with two non-admin accounts and verified both the authorized positive path and lower-permission negative path.
- PASS: final read-only API/DB核验只用于确认状态、当前版本、权限范围和 preview file IDs；未用 API-only 替代页面权限验证。
- No bypass used: no admin account, no SQL/API state mutation, no API-only permission validation, no password written to report/logs, and no old jar fallback was used to claim success.
- Closeout blocker: repository remains a shared dirty workspace with unrelated modified/untracked files; this task is ready for closeout, but commit/push must remain blocked unless the user authorizes a safe dirty-worktree baseline or an isolated submission path.
