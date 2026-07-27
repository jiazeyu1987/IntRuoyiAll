# 20260727 Batch Record Attachment Owner Config

## Task Goal

Fix the confirm-button failure that reports `batchRecordAttachmentOwners` as an invalid batch-record attachment owner configuration when opening or creating an eDHR batch execution.

## Milestones

- [x] Reproduce and isolate the failing validation path.
- [x] Add a regression test that fails before the fix.
- [x] Implement the minimal backend fix without fallback or silent downgrade.
- [x] Run targeted verification and record evidence.
- [x] Run real Playwright E2E for list page and open/create confirm button.
- [ ] Resolve real route data/configuration for route `922119` if user authorizes changing business configuration.

## Expected Verification

- Targeted backend regression test proves valid attachment owner configuration is accepted.
- Relevant MES backend compile or targeted Maven verification is run, or the exact blocker is recorded.
- Real Playwright E2E verifies the list page and confirm-button user path against local `int_main`.
- Evidence files satisfy bug-regression and backend-api evidence contracts.

## Current Status

blocked

## Blocker

- Code fix and backend verification passed. Real E2E on `http://localhost:8081` shows the list page no longer displays the global red error, but the open/create confirm button still returns `1040271050 / 批记录附件负责人配置无效：batchRecordAttachmentOwners` for existing batch `900000000876`.
- The exact route and existing batch shown in the screenshot (`route_id=922119`, active `V14`, draft `V15`, batch `900000000876`) still have no frozen `configSnapshots.batchRecordAttachmentOwners`; changing route/batch business configuration requires explicit authorization because it is real business data, not task-owned test data.

## Experience Gates

- Backend eDHR task configuration source gate: check current BATCH process configuration, binding ownership, and route snapshot boundaries before changing runtime opening behavior.
- No fallback gate: do not convert missing or invalid attachment owner configuration into default success, empty owners, or inferred users.
- Dirty worktree gate: many concurrent changes exist in `int_main`; only task-owned files may be changed or staged.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复附件负责人配置解析/校验链路。
- `是否存在临时补丁或绕过`：否。
