# Verification Report

## Scope

EDHR-STATIC-013 reopened fix: active-order production process config snapshots must carry frozen `outputMaterialIds` from the formal route snapshot so conservative multi-output-material progress can run on real active orders.

## Verification Status

- Status: ready_for_closeout
- E2E: not run by explicit user instruction.
- Service startup: not run by explicit user instruction.
- Database writes: not run by explicit user instruction.
- Git commit/fusion: authorized in current follow-up; EDHR implementation commit `2618a4a3f` exists on local `int_main`, and merge commit `362929947` reconciles latest `origin/int_main`.

## Results

- PASS: RED static contract reproduced the reopened gap before the fix: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` failed because production snapshot/progress code did not yet satisfy the `outputMaterialIds` contract.
- PASS: GREEN static contract now passes: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs`.
- PASS: Java compile gate passes: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile`.
- PASS: `git diff --check` completed with no whitespace errors; Git only emitted CRLF normalization warnings for touched Java files.
- PASS: changed-path static review is limited to EDHR-STATIC-013 scope: active-order production snapshot generation, route production config validation, conservative output-material progress calculation, affected completion consumers, event mapper support, the static contract, and task evidence.
- PASS: bug regression evidence contract validates: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-edhr-static-013-reopened-output-material-snapshot\execution-log.md`.
- PASS: Earlier detached-worktree blocker was recovered by applying the task patch directly to `E:\IntRuoyi` `int_main` after existing mainline conflicts were resolved.
- PASS: Post-fusion verification passed on `E:\IntRuoyi` `int_main`: branch runtime port guard, static contract, `git diff --check`, MES compile, bug evidence validator, and cleanup preview/apply.

## Root Cause

Formal active-order production config snapshots did not freeze the route snapshot's full `outputMaterialIds`, while downstream progress could still rely on old allocation summing. Split submissions for output material A and B could therefore add up to the target without proving every frozen output material reached the target.

## Fixed Scope

- `MesTeamLeaderActiveOrderServiceImpl`: freezes and validates `outputMaterialIds` in production process config snapshots and uses conservative progress for list remaining/completed quantities.
- `MesProRouteCandidateConfigServiceImpl`: requires non-empty, positive, deduplicated `outputMaterialIds` in production process configs before formal route snapshot use.
- `MesOutputMaterialProgressCalculator`: centralizes fail-fast output-material progress from frozen snapshot IDs, current allocation event IDs, and production event material details.
- `MesTeamLeaderActiveOrderCompletionProgressPortImpl` and `MesTeamLeaderOrderProcessCompletionService`: use the shared conservative progress calculation for completion gates and process completion records.
- `MesProProcessPoolEventMapper`: adds bulk production-submit event reads by work order and route for list progress.

## Blockers / Exclusions

- No remaining code verification blocker for the scoped static/backend compile checks.
- E2E, service startup, database writes, and new Git push remain intentionally excluded. No current code/fusion blocker remains; task is ready for closeout once the new local commits are explicitly pushed.
