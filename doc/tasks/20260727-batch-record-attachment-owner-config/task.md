# 20260727 Batch Record Attachment Owner Config

## Task Goal

Fix the confirm-button failure that reports `batchRecordAttachmentOwners` as an invalid batch-record attachment owner configuration when opening or creating an eDHR batch execution.

## Milestones

- [x] Reproduce and isolate the failing validation path.
- [x] Add a regression test that fails before the fix.
- [x] Implement the minimal backend fix without fallback or silent downgrade.
- [x] Run targeted verification and record evidence.
- [x] Run real Playwright E2E for list page and open/create confirm button.
- [x] Resolve authorized local route/batch data for route `922119` and existing batch `900000000876`.

## Expected Verification

- Targeted backend regression test proves valid attachment owner configuration is accepted.
- Relevant MES backend compile or targeted Maven verification is run, or the exact blocker is recorded.
- Real Playwright E2E verifies the list page and confirm-button user path against local `int_main`.
- Evidence files satisfy bug-regression and backend-api evidence contracts.

## Current Status

ready_for_closeout

## Result

- Code fix and backend verification passed.
- User authorized local business configuration repair for `route_id=922119` and existing batch `900000000876`.
- Route version `V15/id=361` is ACTIVE and has 4 valid `batchRecordAttachmentOwners`.
- Existing batch `900000000876` frozen `route_snapshot_json.configSnapshots.batchRecordAttachmentOwners` was repaired from ACTIVE `V15`; DB verification shows JSON type `ARRAY` and owner count `4`.
- Real Playwright E2E on `http://localhost:8081` now reaches `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000876` after clicking `打开/创建 -> 确认`, with no visible `批记录附件负责人配置无效` error.

## Remaining Closeout

- Cleanup/commit/push remain pending because the shared `int_main` working tree already contains unrelated dirty files.

## Experience Gates

- Backend eDHR task configuration source gate: check current BATCH process configuration, binding ownership, and route snapshot boundaries before changing runtime opening behavior.
- No fallback gate: do not convert missing or invalid attachment owner configuration into default success, empty owners, or inferred users.
- Dirty worktree gate: many concurrent changes exist in `int_main`; only task-owned files may be changed or staged.

## Cleanup Keep

doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/verify-edhr-page-after-restart.cjs
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/verify-open-create-confirm.cjs
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/configure-route-attachment-owners.cjs
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/configure-route-attachment-owners.json
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/edhr-batch-execution-after-restart.json
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/edhr-batch-execution-after-restart.png
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/edhr-open-create-confirm-existing.json
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/edhr-open-create-confirm-existing.png
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/batch-900000000876-route-snapshot-before-repair.json
doc/tasks/20260727-batch-record-attachment-owner-config/runtime-artifacts/batch-900000000876-route-snapshot-repair.json

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复附件负责人配置解析/校验链路。
- `是否存在临时补丁或绕过`：否。
