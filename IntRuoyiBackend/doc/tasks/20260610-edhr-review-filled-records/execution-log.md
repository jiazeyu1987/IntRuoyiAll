# eDHR 批次复盘已填写批记录视图执行日志

- BDD: 已完成批次复盘 -> Given 一个已完成 eDHR 批次 When 打开批次复盘接口 Then 响应应包含按工序排序的 `executionReviews`，每项包含执行编号、最终表单快照、填写值、签名摘要、审批摘要和追溯摘要。
- BDD: 审批记录显式返回 -> Given 单表执行已审批 When 获取批次复盘 Then `approvalRecords` 应包含对应执行编号、工序、审批人、审批意见和签名时间。
- RED: 旧接口行为 -> FAIL, expected reason: 原接口只返回批次事件、任务事件、批次签名和归档版本，缺少 `executionReviews` 与显式 `approvalRecords`。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsBatchTasksSignaturesAndArchives -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- REGRESSION: 复盘接口保留 `batchEvents`、`taskEvents`、`signatureRecords`、`archiveVersions`，新增字段不改变既有字段语义。
