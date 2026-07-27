# 20260727 Batch Record Attachment Owner Config

## Task Goal

Fix the confirm-button failure that reports `batchRecordAttachmentOwners` as an invalid batch-record attachment owner configuration when opening or creating an eDHR batch execution.

## Milestones

- [x] Reproduce and isolate the failing validation path.
- [x] Add a regression test that fails before the fix.
- [x] Implement the minimal backend fix without fallback or silent downgrade.
- [x] Run targeted verification and record evidence.
- [ ] Resolve real route data/configuration for route `922119` if user authorizes changing business configuration.

## Expected Verification

- Targeted backend regression test proves valid attachment owner configuration is accepted.
- Relevant MES backend compile or targeted Maven verification is run, or the exact blocker is recorded.
- Evidence files satisfy bug-regression and backend-api evidence contracts.

## Current Status

blocked

## Blocker

- Code fix and backend verification passed, but the actual route shown in the screenshot (`route_id=922119`) still has no `configSnapshots.batchRecordAttachmentOwners` in ACTIVE `V14` or DRAFT `V15`; changing that route requires explicit authorization because it is real business configuration, not task-owned test data.

## Experience Gates

- Backend eDHR task configuration source gate: check current BATCH process configuration, binding ownership, and route snapshot boundaries before changing runtime opening behavior.
- No fallback gate: do not convert missing or invalid attachment owner configuration into default success, empty owners, or inferred users.
- Dirty worktree gate: many concurrent changes exist in `int_main`; only task-owned files may be changed or staged.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复附件负责人配置解析/校验链路。
- `是否存在临时补丁或绕过`：否。
