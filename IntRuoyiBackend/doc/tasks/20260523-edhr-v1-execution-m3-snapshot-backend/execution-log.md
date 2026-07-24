# Execution Log: eDHR V1 执行节点 M3 标准化快照后端

BDD: executionSnapshotJson 必须产出标准化顶层结构 -> Given route-process 绑定到一条 JMReport 批记录报表 / When 后端按上下文新建执行记录 / Then `executionSnapshotJson` 至少包含 `snapshotVersion`、`source`、`layout`、`meta`、`fields`。

BDD: 可编辑字段必须可按表格坐标驱动前端渲染 -> Given 报表 JSON 中存在 `fillForm.field` 配置 / When 后端标准化快照 / Then `fields` 至少包含 `fieldKey`、`label`、`rowIndex`、`columnIndex`、`component`、`required`。

BDD: 保存/提交回显仍以 cellValues 为准 -> Given 执行记录已保存草稿 / When 获取执行详情 / Then 前端可以同时拿到标准化快照与 `cellValues` 进行只读/编辑切换。

RED: `MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_shouldNormalizeExecutionSnapshotJsonStructure` -> FAIL before implementation, `executionSnapshotJson` 仍直接返回原始 `reportJson`，缺少 `snapshotVersion/source/layout/meta/fields` 标准化顶层结构。

GREEN: `MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_shouldNormalizeExecutionSnapshotJsonStructure` -> PASS, `executionSnapshotJson` 已输出标准化结构，并从 `fillForm.field` 生成可编辑字段定义。

GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
