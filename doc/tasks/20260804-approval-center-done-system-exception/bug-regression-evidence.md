# Bug Regression Evidence: 审批中心已办页系统异常

## Bug Summary

进入审批中心“已办”页时，列表区域显示“系统异常”。

## Expected Behavior

`DONE` 视图应通过统一审批中心接口加载已办任务，存在数据时展示列表，无数据时展示空态，不应因视图参数、provider 支持范围或前端路由同步问题触发系统异常。

## Reproduction

- Reproduction path: `审批中心 -> 已办`。
- Reproduction evidence: 用户提供截图显示“已办”菜单高亮，列表顶部出现“系统异常”。

## Root Cause

- `BpmNativeApprovalTaskProvider.toDoneSummary(...)` 将 legacy historic task 的空 `TASK_STATUS` 直接传入 `ApprovalTaskResultSupport.fromBpmTaskStatus(...)`。标准 BPM `done-page` 会把历史任务状态作为可空字段返回，但统一审批中心将其升级为异常，导致 DONE 视图主查询失败并在页面显示“系统异常”。

## Regression Test

- `BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing` 构造缺少 `TASK_STATUS` 的 `HistoricTaskInstance`，断言统一摘要仍保留 `BPM_TASK_DONE` 行，且 `approvalResult/approvalRemark` 为空。

## RED

RED: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `APPROVAL_RESULT_UNSUPPORTED: BPM done task-done-legacy status=null`。

## GREEN

GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 29 tests.
GREEN: `node tests/e2e/approval-center-done-standard-list-static.spec.js` -> PASS.
GREEN: `node tests/e2e/approval-center-done-result-remark-static.spec.js` -> PASS.
GREEN: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS.

## Verification

- Backend provider/service regression and approval-center static contracts passed as listed in GREEN.
- `git diff --check -- <task-owned paths>` -> PASS with line-ending warnings only.

## Risk And Scope

- 影响审批中心统一列表 DONE 视图；需避免改变 TODO、MY_INITIATED、CC、模块筛选、分页和显示字段配置行为。

## Blockers And Follow-Up

- 当前工作树已有无关未提交改动和本地分支 ahead 状态，最终提交/推送可能被既有状态阻塞。
