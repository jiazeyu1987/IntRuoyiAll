# DCC cleanupTime Response Type Fix

## Task Goal

Fix the DCC response contract so `cleanupTime` is emitted with the valid API field type and no response validator reports `DCC response field has invalid type: cleanupTime`.

## Milestones

- [x] Create task record and capture required preflight state.
- [x] Reproduce the invalid `cleanupTime` response type with a deterministic regression test.
- [x] Fix the DCC response field type at the root contract/source.
- [x] Run targeted DCC regression verification.
- [ ] Complete cleanup, commit, and push according to project policy.

## Expected Verification

- Targeted regression test fails before the fix and passes after the fix.
- Relevant DCC backend/frontend static contract checks pass for the changed response model.
- Bug evidence validator passes for this task evidence.

## Current Status

ready_for_closeout

## Verification Evidence

- RED: `node tests/e2e/dcc-upload-temporary-status-timestamp-static.spec.js` failed before the fix because `expireTime` was still typed as a string in the frontend temporary status contract.
- GREEN: `node tests/e2e/dcc-upload-temporary-status-timestamp-static.spec.js` passed after the frontend contract changed `expireTime` and `cleanupTime` to numeric timestamps and the parser switched to `readOptionalTimestamp`.
- REGRESSION: `pnpm ts:check` passed in `IntRuoyiFronted`.
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-dcc-cleanup-time-response-type/bug-regression-evidence.md` passed.
- DIFF: `git diff --check -- IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts IntRuoyiFronted/tests/e2e/dcc-upload-temporary-status-timestamp-static.spec.js doc/tasks/20260803-dcc-cleanup-time-response-type` passed.

## Experience Gates

- Applied `docs/frontend-development.md#前端静态契约隔离门禁`: used a task-specific static contract for RED/GREEN and then ran `pnpm ts:check`.
- Applied `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`: `workflow.ts` contains unrelated OnlyOffice hunks, so this task requires selective staging of only cleanup-time hunks.
- Applied `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`: bug evidence validator PASS was copied into kept task records before cleanup removed the temporary evidence file.
- Consolidated new reusable experience into `docs/frontend-development.md#前端-localdatetime-响应契约门禁` and routed it from `docs/experience-index.md`.

## Cleanup Evidence

- PREVIEW: `task_closeout.py --task-id 20260803-dcc-cleanup-time-response-type --mode preview` kept `task.md`, `execution-log.md`, and `verification-report.md`, and selected only `bug-regression-evidence.md` for deletion.
- APPLY: `task_closeout.py --task-id 20260803-dcc-cleanup-time-response-type --mode apply` deleted only `bug-regression-evidence.md` with no blocked paths or warnings.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正 DCC 响应字段类型定义，而不是在调用方绕过校验。
- `是否存在临时补丁或绕过`：否。

## Preflight Notes

- Required backend, task closeout, PowerShell encoding, PowerShell/Git, and bug-regression workflow rules were read before implementation.
- Existing dirty worktree was captured in baseline commit `dfc84a443` before this task's files were created.
- After the baseline, unrelated concurrent changes appeared in `IntRuoyiFronted/tests/e2e/dcc-nas-uncontrolled-local-import-real.e2e.js` and `doc/tasks/20260803-dcc-upload-onlyoffice-document-url/`; they are not task-owned and will remain unstaged unless they directly conflict.
