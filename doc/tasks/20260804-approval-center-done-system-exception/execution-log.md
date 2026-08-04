# Execution Log: 修复审批中心已办页系统异常

## User Intent

- 用户截图显示进入“审批中心 > 已办”后，列表区域出现“系统异常”，且当前页面无审批任务数据。

## BDD

- BDD: 已办审批列表正常加载 -> Given 用户进入审批中心“已办”页 / When 前端以 `viewType=DONE` 请求统一审批任务分页 / Then 系统必须返回正式 DONE 视图结果或空态，不显示“系统异常”。

## TDD Evidence

- RED: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `APPROVAL_RESULT_UNSUPPORTED: BPM done task-done-legacy status=null`.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 29 tests.

## Work Log

- 创建任务目录和初始任务文档。
- 已确认工作树已有大量未提交改动，且 `IntRuoyiFronted/src/views/approval-center/index.vue` 已有未提交修改；后续只做最小差异并避免覆盖无关改动。
- Root cause: `BpmNativeApprovalTaskProvider.toDoneSummary(...)` 直接把 `FlowableUtils.getTaskStatus(task)` 交给 `ApprovalTaskResultSupport.fromBpmTaskStatus(...)`；legacy historic DONE task 缺少 `TASK_STATUS` 时抛异常，导致 `/approval-center/tasks/page?viewType=DONE` 整页失败。
- Fix: 新增 `resolveDoneApprovalResult(...)`，仅当 `TASK_STATUS` 为空时返回空 `approvalResult`；非空未知状态仍沿用 `fromBpmTaskStatus(...)` fail-fast。
- Frontend contracts: `node tests/e2e/approval-center-done-standard-list-static.spec.js` -> PASS；`node tests/e2e/approval-center-done-result-remark-static.spec.js` -> PASS；`node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- Experience consolidation: 已新增 `docs/backend-development.md#统一审批中心 BPM 已办历史状态门禁`，并在 `docs/experience-index.md` 增加可检索关键词；`rg` 索引验证通过。

## Blockers

- 当前分支 `int_main` 已领先 `origin/int_main` 且工作区存在大量既有未提交改动；按项目 Git policy，最终提交/推送需要先处理既有脏工作区基线或由用户确认边界。

