# Task: Test server NAS principal automatic mapping

## Task Goal

Help map unmapped NAS ACL principals from the test server NAS transfer task to the closest DCC users, departments, roles, or posts without granting permissions to low-confidence or ambiguous targets.

## Milestones

- [x] M1: Record BDD scenario and identify the target transfer task and tenant.
- [x] M2: Read unmapped NAS principals and DCC subject candidates from the test server.
- [x] M3: Generate a dry-run mapping plan with confidence and ambiguity checks.
- [x] M4: Apply only high-confidence mappings and record skipped ambiguous cases.
- [x] M5: Verify the permission snapshot summary and restore preview after mapping.

## Expected Verification

- Dry-run report lists total unmapped principals, mapped candidates, skipped candidates, and reasons.
- Applied mappings are written through the existing mapping contract or equivalent persisted table with explicit change reasons.
- Snapshot summary shows reduced unmapped principal and blocker counts.
- Restore preview is checked after mapping; any remaining blockers are reported clearly.

## Current Status

completed

## Completed Work

- Targeted test server transfer task 5 in tenant 1. The permission snapshot is captured and ready; the earlier "snapshot is not ready" condition is not present for the correct tenant.
- Confirmed there were no existing active identity mappings for tenant 1 before this operation.
- Resolved NAS ACL SIDs to readable NAS principals from the live NAS ACL output.
- Applied 130 exact, unique mappings with method `AUTO_EXACT`.
- Applied 5 conservative, unique account mappings with method `AUTO_CONSERVATIVE`:
  - `panxin` -> `zhangpanxin`
  - `meishiqi1` -> `meishiqi`
  - `zhaozheng1` -> `zhaozheng`
  - `wangmin1` -> `wangmin`
  - `wangyue1` -> `wangyue`
- Did not auto-map low-confidence or ambiguous principals, including examples such as `administrators`, `wangyi`, `wujiaqin`, and `lixiaoqi`.

## Final Verification

- Mapping table active mapped principals for tenant 1: 135.
- Permission snapshot summary API for task 5 returned `code=0`, `snapshotStatus=CAPTURED`, `directorySnapshotCount=51`, `aceCount=2314`, `unmappedPrincipalCount=39`, `unsupportedAceCount=0`, `blockerCount=1265`, `restoreSupported=false`.
- Permission restore preview API for task 5 returned `code=0`, `canRestore=false`, `directoryCount=51`, `ruleCount=5779`, `runtimeEnforcementReady=true`, `runtimeEnforcementBlocker=null`, blocker count 1265.

## Remaining Blockers

Restore is still blocked because 39 NAS principals remain unmapped. They were intentionally skipped because no unique high-confidence DCC user, department, role, or post candidate was available. These require manual confirmation or creation/renaming of the corresponding DCC subjects before restore can become supported.
