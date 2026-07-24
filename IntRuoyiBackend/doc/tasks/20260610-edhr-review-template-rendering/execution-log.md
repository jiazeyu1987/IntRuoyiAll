# 执行日志

BDD: 已完成批次按模板复盘 -> Given 一个已完成的 eDHR 批次且单表执行记录包含模板快照和值，When 调用批次复盘接口，Then 每个已填写执行项返回模板布局、元数据、快照和值，供前端按原模板只读渲染。

RED: 代码检查 -> FAIL，`EdhrBatchExecutionReviewTimelineRespVO.FormViewModel` 只包含 `executionSnapshotJson`、`cellValuesJson`、`remark`，没有显式返回 `sheetLayoutJson` / `metaJson`。

GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsBatchTasksSignaturesAndArchives -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，复盘响应包含 `sheetLayoutJson` / `metaJson`，原批次任务、签名、审批、归档断言仍通过。

REGRESSION: 后端未新增表结构、未改批次/任务/执行状态机；目标单测覆盖复盘接口既有批次事件、任务事件、签名、审批记录、归档版本和执行复盘列表。
