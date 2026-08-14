# Verification Report

## Scope

- MES 一线 PQC task overlay。
- MES 一线生产提交候选 active-order process snapshot 归属。

## RED

- Command: `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL。
- Expected reason: `MesFrontlinePqcTaskOverlay` missing while new tests require overlay identity behavior.

## GREEN

- Command: `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS。
- Evidence: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`.

## Supervisor Revision

- Finding: original DF09 evidence did not cover stable business sorting required by TC-DF09-TASK-OVERLAY.
- RED: same Maven command -> FAIL, sorting scenario produced [1004, 1003, 1001, 1002] instead of [1001, 1002, 1003, 1004].
- Fix: overlay list now sorts by `businessDate`, FIRST/PATROL_AM/PATROL_PM/FINAL rule order, `roundNo`, and task id; unknown rule keys fail fast.

## Static And Evidence Checks

- `git diff --check`: PASS。
- 禁止项扫描：PASS，未发现越界文件、formBindings、产品/物料/路线推算 QA、QA 工序/检验项目过滤、QA 与 MES 路线工序存在性校验、fallback/兼容/默认成功。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df09/backend-api-evidence.md`: PASS，`Backend API evidence is valid.`

## Current Status

ready_for_closeout
