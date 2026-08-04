# Verification Report: 修复审批中心已办页系统异常

## Summary

- Root cause fixed: BPM native DONE provider no longer throws when a legacy `HistoricTaskInstance` has no local `TASK_STATUS`; the row remains visible with empty approval result fields.
- Scope preserved: non-null unknown BPM task statuses still fail through `ApprovalTaskResultSupport.fromBpmTaskStatus(...)`; no default “通过/驳回”、空列表成功、前端隐藏错误或 catch 吞异常 was introduced.

## Verification

- RED: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `APPROVAL_RESULT_UNSUPPORTED: BPM done task-done-legacy status=null`.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- REGRESSION: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 29 tests.
- FRONTEND STATIC: `node tests/e2e/approval-center-done-standard-list-static.spec.js` -> PASS.
- FRONTEND STATIC: `node tests/e2e/approval-center-done-result-remark-static.spec.js` -> PASS.
- FRONTEND STATIC: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS.
- INDEX: `rg -n "审批中心已办系统异常|APPROVAL_RESULT_UNSUPPORTED status=null|统一审批中心 BPM 已办历史状态门禁" docs\experience-index.md docs\backend-development.md` -> PASS.
- WHITESPACE: `git diff --check -- <task-owned paths>` -> PASS with line-ending warnings only.

## Remaining Constraints

- Real browser E2E was not run in this turn; the fix is covered by backend RED/GREEN and approval-center static contracts.
- Repository closeout commit/push is blocked by existing unrelated dirty worktree state and branch ahead status unless a separate baseline/ownership decision is made.
