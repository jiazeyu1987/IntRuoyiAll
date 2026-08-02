# Verification Report

## Scope

本报告记录 DCC 文控“受控浏览”前端体验优化的静态合同、类型检查和真实 Playwright E2E 结果。

## Target Test Data

- Target file number: `CODX-DCC-ORIG-20260802101521`
- Target file title: `Codex DCC 原版上传链路 20260802101521`
- Target controlled file ID: `2054545668044070287`
- Expected version/status: `V1.0` / `ACTIVE`
- Expected directory path: `4.Ohter`
- Expected published file ID: `9198354916366`
- Expected stamped file ID: `9198354916366`
- Authorized account label: `wangsiyu`
- Lower-permission account label: `pengyunfeng`
- Permission difference under test: `wangsiyu` should see the target current ACTIVE file in controlled browsing; `pengyunfeng` should not see the same file or should receive an explicit no-access/no-current-match state.

## Results

- Static contract: PASS.
  - `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js`
  - `node tests\e2e\dcc-browser-version-summary-static.spec.js`
  - `node tests\e2e\dcc-controlled-browser-viewer-linkage-static.spec.js`
  - `node tests\e2e\dcc-upload-governance-ux-static.spec.js`
- Type check: PASS.
  - `pnpm ts:check`
- Backend package/artifact recovery: PASS.
  - `mvn -pl yudao-server -am -DskipTests package`
- Read-only target data verification: PASS.
  - Target file state: `ACTIVE`
  - Current version: `V1.0`
  - Master current active controlled file ID: `2054545668044070287`
  - Category: `过程检验规程`
  - Published/stamped file IDs: `9198354916366` / `9198354916366`
  - View matrix rule count: `8`
- Real Playwright E2E: BLOCKED.
  - Result file: `doc\tasks\20260802-dcc-controlled-browser-ux-optimization\dcc-controlled-browser-ux-real-e2e-result.json`
  - Authorized path: BLOCKED before completing controlled-browser list, preview, and published/stamped viewer verification after backend runtime failed.
  - Lower-permission path: BLOCKED, not reached after backend runtime failed.
  - Preview result: BLOCKED, not reached after backend runtime failed.
  - Final controlled-browser directory path: expected `4.Ohter`; not fully page-verified after backend runtime failed.
- Target link error count: `0` DCC target failures in the last result JSON; run failed earlier on non-target tenant lookup because backend `48081` exited.
- Target DCC write request count: `0`; the auditor recorded no DCC mutation requests.

## PASS/BLOCKED Summary

- PASS: implemented the requested UX improvements in the DCC controlled browser/detail/upload front-end scope, including current-effective metadata, version/entry labels, readable published/stamped information, permission/no-match empty state, stable filter/path summary, publish completion visibility, and upload preflight browse-permission scope.
- PASS: added a regression contract for the real E2E failure root cause by requiring current-effective metadata in the file-number visible column, not only in an optional/hidden column.
- BLOCKED: real Playwright E2E cannot complete because the rebuilt backend runtime exits with `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`; `http://127.0.0.1:48081/actuator/health` is unreachable after the failed startup.
- No bypass used: no admin account, no SQL/API mutation, no API-only permission validation, and no old jar fallback was used to claim E2E success.
