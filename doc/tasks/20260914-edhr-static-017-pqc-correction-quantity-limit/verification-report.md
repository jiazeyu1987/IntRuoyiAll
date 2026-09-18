# Verification Report

## Summary

EDHR-STATIC-017 is fixed and locally fused into `int_main` for the scoped PQC correction path. The correction request rejects impossible quantity relationships before signature/revision/formal PQC writes, and the frontend blocks the same invalid payload before submission.

## Changed Paths

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionServiceTest.java`
- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- `IntRuoyiFronted/scripts/pqc-correction-quantity-limit-static.spec.cjs`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionServiceTest.java` (integration compile unblocker for the current `int_main` constructor signature)
- `doc/tasks/20260914-edhr-static-017-pqc-correction-quantity-limit/task.md`
- `doc/tasks/20260914-edhr-static-017-pqc-correction-quantity-limit/execution-log.md`
- `doc/tasks/20260914-edhr-static-017-pqc-correction-quantity-limit/verification-report.md`
- `doc/tasks/20260914-edhr-static-017-pqc-correction-quantity-limit/bug-regression-evidence.md`

## RED

- `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> FAIL, frontend correction scrap input/request builder did not cap scrap quantity at actual inspection quantity.
- `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` from `IntRuoyiBackend\yudao-module-mes` -> FAIL as expected, over-limit and below-piece-floor correction tests expected `ServiceException` but no exception was thrown.

## GREEN

- `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> PASS.
- `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` from `IntRuoyiBackend\yudao-module-mes` -> PASS, 7 tests.
- `node --check IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> PASS.
- `git diff --check -- <task-owned paths>` -> PASS.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-017-pqc-correction-quantity-limit\bug-regression-evidence.md` -> PASS.

## Verification Notes

- No Playwright/E2E was run.
- No database writes were performed.
- No services were started, stopped, or restarted.
- No remote server operation was performed.
- Local `int_main` fusion is present through commit `2722c7ac1`; the combined backend PQC correction test/service state was preserved through commit `3d774fe75` during the adjacent EDHR-STATIC-015/016 cherry-pick.
- `task-closeout-cleanup` preview/apply kept `task.md`, `execution-log.md`, `verification-report.md`, and `bug-regression-evidence.md`; it deleted nothing and reported no blockers.
- No git push was performed because this turn did not explicitly authorize push.

## Int Main Re-Verification

- GREEN: `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` from `E:\IntRuoyi` -> PASS.
- GREEN: `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolProductionReportCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest" test` from `E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes` -> PASS, 22 tests including 8 PQC correction tests.
- GREEN: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` from `E:\IntRuoyi` -> PASS for `int_main/int_main`, frontend 8081, backend 48081.
- GREEN: `git diff --cached --check` during integration conflict resolution -> PASS.

## Blockers

- Completion cannot be marked `completed` under `docs/task-closeout-rules.md` because the project requires git push before completion, and this turn did not explicitly authorize `git push`.
- Supplemental existing SFC static check `node tests\e2e\team-leader-workbench-sfc-style-compile-static.spec.cjs` is blocked by missing frontend dependencies: `node_modules`, `vue-tsc`, and `postcss` are absent. This task did not install dependencies.
- Source worktree cleanup remains intentionally untouched: `C:\Users\BJB110\.codex\worktrees\cdb9\IntRuoyi` is detached and contains mixed DCC/runtime dirty paths outside EDHR-STATIC-017, so only the task allow-list was fused into `int_main`.

## Risk

- Static frontend failed-sample counting mirrors current boolean and numeric result semantics. Backend remains the authoritative gate and validates rebuilt piece details before any signature or formal write.
- Existing unrelated dirty worktree changes were present before this task and were not touched.

## Suggested Shared Defect Table Update

- `EDHR-STATIC-017 | P2 | PQC更正允许损耗数量大于实际检验数量 | FIXED_STATIC_VERIFIED_SOURCE_ONLY`
- Suggested note: `2026-09-14 targeted backend unit + frontend static contract PASS; no E2E/DB/service/git operations by user instruction; closeout blocked by no commit/push authorization and detached HEAD.`
