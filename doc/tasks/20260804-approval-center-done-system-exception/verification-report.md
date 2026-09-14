# Verification Report: 修复审批中心已办页系统异常

## Summary

- Root cause fixed: BPM native DONE provider no longer throws when a legacy `HistoricTaskInstance` has no local `TASK_STATUS`; the row remains visible with empty approval result fields.
- DCC legacy DONE rows are also covered: historical controlled-file snapshots missing version/category display metadata now render `版本：-` / `分类：-` for DONE history while current TODO display metadata remains fail-fast.
- Scope preserved: non-null unknown BPM task statuses still fail through `ApprovalTaskResultSupport.fromBpmTaskStatus(...)`; no default “通过/驳回”、空列表成功、前端隐藏错误或 catch 吞异常 was introduced.

## Verification

- RED: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `APPROVAL_RESULT_UNSUPPORTED: BPM done task-done-legacy status=null`.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- REGRESSION: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 29 tests.
- FRONTEND STATIC: `node tests/e2e/approval-center-done-standard-list-static.spec.js` -> PASS.
- FRONTEND STATIC: `node tests/e2e/approval-center-done-result-remark-static.spec.js` -> PASS.
- FRONTEND STATIC: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS.
- DCC GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest#pageDoneKeepsLegacyHistoricalSnapshotWhenVersionNoOrCategoryIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS.
- DCC REGRESSION: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest,DccApprovalTaskTimelineAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, 13 tests.
- RUNTIME: patched `output\runtime\int_main\backend-runtime-control-20260804-approval-done-e2e-category.jar`; nested DCC/BPM module jars stayed stored (`compress_type=0`) and patched class hashes matched target compiled classes.
- RUNTIME: stopped task-owned old backend PID `49940`; started PID `49968` on `48081`; backend health `UP`; frontend `8081` returned HTTP 200.
- REAL E2E: `node --check doc\tasks\20260804-approval-center-done-system-exception\approval-center-done-real.e2e.js` -> PASS.
- REAL E2E: `node doc\tasks\20260804-approval-center-done-system-exception\approval-center-done-real.e2e.js` -> PASS, exit code 0; `/approval-center/done` route rendered 20 rows, DONE API `status=200, code=0, total=3222`, no visible `系统异常`, `pageErrors=[]`, `consoleErrors=[]`, `targetNetworkFailures=[]`, and `targetWriteRequestCount=0`.
- INDEX: `rg -n "审批中心已办系统异常|APPROVAL_RESULT_UNSUPPORTED status=null|统一审批中心 BPM 已办历史状态门禁" docs\experience-index.md docs\backend-development.md` -> PASS.
- WHITESPACE: `git diff --check -- <task-owned paths>` -> PASS with line-ending warnings only.

## Remaining Constraints

- Real browser E2E is complete for the current local `int_main` runtime (`8081/48081`) and preserved under `doc\tasks\20260804-approval-center-done-system-exception\e2e-artifacts\`.
- Repository closeout commit/push is blocked: commit `6f9ed0e83 chore: baseline existing workspace changes` already contains this task's implementation together with unrelated files, and current workspace status has unrelated unmerged paths. No history rewrite, force push, or mixed push was attempted.
