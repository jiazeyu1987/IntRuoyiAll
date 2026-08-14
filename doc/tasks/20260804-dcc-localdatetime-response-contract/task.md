# DCC LocalDateTime Response Contract Sweep

## Task Goal

Normalize DCC frontend API response contracts so backend `LocalDateTime` response fields are not typed as pure `string` when the backend serializer emits epoch-millisecond numeric timestamps.

## Milestones

- [x] Create task record and capture preflight state.
- [x] Add a focused static contract that fails on remaining DCC `LocalDateTime` response fields typed as pure `string` or string-compatible response unions.
- [x] Update the minimal DCC frontend API response types to match numeric timestamp serialization.
- [x] Run targeted static contract and frontend type verification.
- [x] Record cleanup readiness and Git blocker status according to project policy.

## Expected Verification

- RED/GREEN static contract for DCC `LocalDateTime` response typing.
- `pnpm ts:check` from `IntRuoyiFronted`.
- `git diff --check` on task-owned files.

## Current Status

ready_for_closeout

Implementation and required verification passed. Final completion is blocked by the shared dirty/ahead Git state: the workspace contains unrelated concurrent source/task changes and the branch is ahead of `origin`; this task is not marked completed until commit/push can be completed without mixing unrelated work.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按后端全局 `LocalDateTime` 数字时间戳序列化口径修正前端 API 契约。
- `是否存在临时补丁或绕过`：否。

## Preflight Notes

- Trigger rules read: `docs/frontend-development.md`, `docs/e2e-rules.md`, `docs/task-closeout-rules.md`, `docs/powershell-memory.md`, and `docs/powershell-encoding.md`.
- Skill read: `bug-regression-fix-loop` and `references/bug-contract.md`.
- Experience gates applied: `docs/frontend-development.md#前端-localdatetime-响应契约门禁`, `docs/frontend-development.md#前端静态契约隔离门禁`, `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`, and `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`.
- Git preflight: branch `int_main`, remote `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`.
- Branch divergence at start: `int_main...origin/int_main [ahead 5, behind 2]`; remote PQC documentation conflict from the previous cleanup-time task remains unresolved.
- Existing untracked concurrent task docs under `doc/tasks/20260803-dcc-upload-onlyoffice-document-url/` and `doc/tasks/20260804-qa-regulation-tab/` are not task-owned and must remain unstaged.
- Additional sweep found and fixed frontend response contracts outside `src/api/dcc/controlledFile`: `SignatureGovernanceRecordRespVO.signedAt` and `DccNasControlAuditFileRespVO.modifiedAt`.
- `DccControlledFileSignatureExportSummaryRespVO.SignatureItem.signedAt` exists on a backend summary endpoint, but no active frontend API response contract for `/signature-export-summary` was found in this task scope.
