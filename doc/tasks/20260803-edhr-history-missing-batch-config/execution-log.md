# Execution Log

## User Intent

用户先要求处理缺失 BATCH 配置导致历史批记录页签报错；随后指出“所有的历史信息都报这个错误”，要求按代码判断逻辑是否有问题。最终口径调整为：历史只读页不应为了展示已归档历史内容而触发当前 BATCH 流转门禁。

## Baseline

- Branch: `int_main`
- Dirty-worktree baseline commit: `125d640fa`
- Baseline scope: existing DCC source/test/task documentation changes saved before this eDHR task.

## BDD

- BDD: 缺失 BATCH 门禁配置的历史批记录仍展示持久化内容 -> Given 一个已归档历史批次的路线快照和当前路线都无法解析 BATCH 流转门禁 When 用户打开历史批记录页签 Then 系统仍返回已持久化的批记录执行快照且任务事件标记为只读。
- BDD: 正常 BATCH 配置历史批记录仍展示 -> Given 一个历史批次存在有效 BATCH 批记录配置 When 用户打开历史批记录页签 Then 系统仍展示正式历史批记录内容。

## RED / GREEN

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsEmptyHistoryContentWhenArchivedBatchConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，未到达 MES，先被无关 DCC 测试编译错误阻塞：`FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS` / `getDefaultUnclassified` 缺失。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsEmptyHistoryContentWhenArchivedBatchConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，符合预期，旧逻辑在 `buildTaskGateMap -> buildTaskPredecessorRouteProcessIdMap` 抛出 `ServiceException` code `1040750403`。
- RED: `node yudao-module-mes\src\test\js\edhr-history-missing-batch-config-static.spec.cjs` -> FAIL，新契约要求 `getReviewTimeline` 使用历史专用门禁摘要，当前实现仍直接调用 `buildTaskGateMap`。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，修正门禁后继续暴露历史执行预览还会反查当前 Jimu 报表，报 `PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING`。
- GREEN: `node yudao-module-mes\src\test\js\edhr-history-missing-batch-config-static.spec.cjs` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS。
- REGRESSION: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsBatchTasksSignaturesAndArchives" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，正常历史批记录展示路径仍通过。
- RECHECK: `node yudao-module-mes\src\test\js\edhr-history-missing-batch-config-static.spec.cjs` -> PASS，收尾前复核静态契约仍通过。

## Implementation

- `MesProEdhrBatchExecutionServiceImpl#getReviewTimeline` 对终态历史批次使用 `buildReviewTimelineTaskGateMap`，不再调用活动任务的 `buildTaskGateMap` / `buildTaskPredecessorRouteProcessIdMap`。
- 历史任务事件统一标记 `available=false`、`gateMessage=历史批次只读`，避免误导为可切换或可填写。
- `toExecutionReview` 增加终态只读上下文；历史执行预览解析签名单元格时只看 `executionSnapshotJson` 和已落库 `sheetLayoutJson`，不再反查当前 Jimu 报表阻断历史。
- 活动批次仍使用正式 BATCH 配置门禁，缺失配置继续 fail-fast，不改变新建、填写、关闭、归档生成等运行态校验。
- 新增 Java 回归：`getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing`。
- 新增静态契约：`edhr-history-missing-batch-config-static.spec.cjs`。
- `docs/backend-development.md` 已追加长期门禁：历史批记录只读页不得复用活动流转门禁，不得因当前 Jimu 报表缺失阻断已持久化历史执行快照。

## Notes

- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- 已读取 `bug-regression-fix-loop` 技能和 `references/bug-contract.md`。
- 基线后出现多项并行 DCC/前端/doc 改动；本任务后续暂存必须仅限 MES 服务、MES 测试、MES 静态契约和本任务文档。
- 收尾阻塞：`git status --short --branch` 显示 `int_main...origin/int_main [behind 2]`，且存在大量并行任务脏改动；按任务所有权要求，当前未暂存、未提交、未推送。
