# 第三方报工导入列表与排产进度修复执行日志

## User Intent

- 本地服务器，芋道源码租户，使用 admin 登录。
- 在报工页签选择第三方导入，导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx`。
- 导入后确认弹框，当前报工列表为空，排产工单进度未更新。
- 先分析原因，再修改；修改后测试到报工列表更新、排产工单更新为止；如果目标工单已满量不更新也算正确。

## Rule Bootstrap

- Read `bug-regression-fix-loop` skill and `references/bug-contract.md`.
- Read `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/powershell-memory.md`.
- Read backend/frontend/E2E/local runtime/login/database/worktree trigger rules before implementation and verification.
- Read `docs/experience-index.md`; matched the third-party feedback direct-report formal-chain gate routed through `docs/backend-development.md`.

## BDD Scenarios

- `BDD: Third-party import creates formal feedback -> Given an Excel direct work report row matching an unfinished schedule task, When admin imports the workbook and confirms the result dialog, Then a formal MesProFeedback record exists, the feedback list shows it, and the schedule order progress includes it.`
- `BDD: Third-party import skip reasons are explicit -> Given an import row missing required formal-chain prerequisites such as reporter or remaining quantity, When admin imports the workbook, Then the row is skipped with a structured reason and no fake progress is written.`
- `BDD: Completed schedule order remains full -> Given the matched schedule order has no remaining quantity, When admin imports a direct work report row, Then the list/progress result is either a skipped formal reason or a formal full-quantity no-op, not a default success.`

## Baseline Evidence

- `git status --short --branch` before baseline: branch `int_main...origin/int_main [ahead 1]` with pre-existing DCC, role-matrix, test-management docs and E2E artifacts dirty.
- Sensitive scan: path-level keyword scan found only keyword-bearing files; no `admin123` literal in dirty files.
- Large file scan: no dirty/untracked file over 10 MiB.
- Baseline commit: `b99246f58 chore: baseline dirty workspace before feedback import fix`.
- Post-baseline status: `int_main...origin/int_main [ahead 2]` and clean worktree.

## Milestone Status

- M1 task setup and baseline: completed.
- M2 reproduce and isolate root cause: completed.
- M3 RED regression: completed.
- M4 implementation: completed.
- M5 verification: completed.
- M6 closeout: in progress.

## RED/GREEN Log

- RED: `mvn -pl yudao-module-mes -Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldUseUniqueProcessWorkstationWhenTaskWorkstationMissing test` -> FAIL before fix, expected imported count `1`, actual `0`; root cause was matched active task with `workstation_id = NULL` while the process had exactly one formal active workstation binding.
- GREEN: `mvn -pl yudao-module-mes -Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldUseUniqueProcessWorkstationWhenTaskWorkstationMissing test` -> PASS after resolving the unique formal process-workstation binding.
- RED: `mvn -pl yudao-module-mes -Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_whenSameSourceRowImportedTwice_shouldAccumulateFormalFeedbackOnly test` -> FAIL before second fix, expected `213.000000`, actual `333.000000`; root cause was direct import private recompute adding stale `DIRECT_WORK_REPORT` import-record progress on top of newly created formal feedback.
- GREEN: `mvn -pl yudao-module-mes -Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_whenSameSourceRowImportedTwice_shouldAccumulateFormalFeedbackOnly+importDirectWorkReportWorkbook_shouldUseUniqueProcessWorkstationWhenTaskWorkstationMissing test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -Dtest=ThirdPartyFeedbackImportServiceImplTest test` -> PASS, 26 tests, 17 skipped.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderProgressServiceTest test` -> PASS, 17 tests.
- GREEN: `mvn -pl yudao-module-mes -DskipTests package` -> PASS, built `yudao-module-mes-2026.04-SNAPSHOT.jar`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackImportServiceImplTest,MesProScheduleOrderProgressServiceTest" test` in `D:\IntRuoyiWorktree\third-party-feedback-import-20260802` -> PASS, 43 tests, 17 skipped.
- GREEN: `mvn -pl yudao-server -am -DskipTests package` in `D:\IntRuoyiWorktree\third-party-feedback-import-20260802` -> PASS, built `yudao-server-exec.jar`.
- GREEN: `node doc\tasks\20260802-third-party-feedback-import-list-progress\verify-direct-work-report-import-real.e2e.js` against `http://127.0.0.1:8090` / `48090` -> PASS, formal feedback and schedule progress verified.

## Root Cause And Fix

- Root cause 1: direct import required a formal `workstationId`, but affected live schedule-linked tasks had `workstation_id = NULL`. The matched process had exactly one active formal workstation binding in `mes_md_workstation.process_id`, so the formal chain could be resolved without fallback. Fix: when task workstation is missing, use only the unique active formal process-workstation binding; if zero or multiple exist, skip with structured reason.
- Root cause 2: after formal feedback creation, `recalculateDirectProgressForScheduleOrder` still merged legacy `DIRECT_WORK_REPORT` import-record progress into the same process total. This double-counted progress in the import result and conflicted with the shared schedule progress contract that counts formal feedback statuses. Fix: direct import recompute now uses formal feedback progress only and never queries applied direct-progress records.
- E2E verifier correction: backend `MesProFeedbackStatusEnum.APPROVING` is status `2`, but the verifier was filtering the formal feedback list with status `1`. Fixed the verifier to query `APPROVING=2` and assert returned rows remain in that status.

## Runtime And Real E2E Evidence

- Existing patched runtime before this continuation: PID `35328`, port `48081`, jar `output\runtime\int_main\backend-third-party-feedback-import-20260802-0237.jar`, health `UP`.
- Real Playwright import after the workstation fix created formal feedback `FB-000641`, persisted `status=2`, `workstation_id=922739`, `schedule_order_process_id=3079`, `source_import_record_id=1751`, proving the formal feedback chain was active.
- Real Playwright rerun after verifier status fix reached schedule progress assertion and failed at `SCH-881MO093613-20260707-0001/Z2600 did not reflect imported progress`; analysis showed the assertion exposed the importer double-counting stale direct-progress records in the import result.
- Created full-module patched runtime jar `backend-third-party-feedback-import-20260802-0258.jar`, SHA256 `7CD60013CDB9E42BEB615A5AA3D330F62B3701BBDF0D50C00CB929076FA620AF`; startup failed with `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: EDHR`, so it was not used for verification.
- Created narrower class-only patched runtime jar `backend-third-party-feedback-import-20260802-0303.jar`, SHA256 `C2771325A8926DD8023273BE766FB81808ED9BC12F1BFE4F4B99F2B52A88E112`; startup also failed with `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: EDHR`, so it was not used for verification.
- After patched-start failures, `48081` was taken by another task-owned runtime: PID `29052`, command line `java.exe -jar E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081 --spring.profiles.active=local`, parent `scripts\runtime\start-branch-backend.ps1`, log `backend-dcc-upload-revision-e2e-20260802-continue.out.log`.
- The active `yudao-server-exec.jar` was last written `2026-08-02 02:06:32`; its nested `ThirdPartyFeedbackImportServiceImpl.class` SHA256 is `AF44327E08C404F564C3196A7FFD2A24173669C4A72FB0B0146E2EE03383D327`, while the fixed class SHA256 is `C87588127A3881F3A30341178596C4505901311FF6E8025B1756C74E95C845CA`, so this active runtime cannot serve as final verification evidence.
- To avoid replacing another task's shared `48081` runtime, created and verified the current task in official worktree `D:\IntRuoyiWorktree\third-party-feedback-import-20260802`, branch `codex/third-party-feedback-import-20260802`, reserved slot `9`, frontend `8090`, backend `48090`.
- Isolated runtime evidence: backend PID `44852` served `D:\IntRuoyiWorktree\third-party-feedback-import-20260802\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`; frontend PID `38528` served the same worktree's Vite frontend.
- Real Playwright import result: `submittedCount=1`, `importedCount=1`, `feedbackCodes=["FB-000643"]`, `importRecordIds=[1753]`, `scheduleOrderCodes=["SCH-881MO093613-20260707-0001"]`, `feedbackListRows=1`.
- Schedule verification result: `SCH-881MO093613-20260707-0001` returned `completedQuantity=4995`, `uncompletedQuantity=21005`, `progressPercent=19.211538`, `status=2`, `processCount=26`; therefore the formal feedback list and排产工单进度 both update after confirming the import result dialog.

## Blockers

- None for implementation or verification.
- Closeout remaining: experience consolidation, cleanup preview/apply, selective commit, push, task runtime/process cleanup, and final task status update.
