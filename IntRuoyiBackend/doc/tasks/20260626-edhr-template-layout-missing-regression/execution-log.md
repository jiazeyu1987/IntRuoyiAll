# Execution Log：eDHR 模板布局接口缺失回归修复

BDD: cell-rules 返回已有模板布局 -> Given Jimu 报表 JSON 中存在有效模板 rows / When 调用 cell-rules 接口 / Then 响应包含 sheetLayoutJson.rows。
BDD: signature-cell-markers 返回同一模板布局 -> Given Jimu 报表 JSON 中存在有效模板 rows / When 调用 signature-cell-markers 接口 / Then 响应包含 sheetLayoutJson.rows。
BDD: 无模板布局仍明确失败 -> Given Jimu 报表 JSON 没有可识别 rows / When 调用布局相关接口 / Then 后端返回明确 JSON 无效错误，不返回空成功。
BDD: 执行详情返回真实模板布局 -> Given 按上下文从 Jimu 报表创建 eDHR 执行记录 / When 前端读取执行详情 / Then `sheetLayoutJson` 与 `executionSnapshotJson.layout` 同源包含可渲染 rows，不返回 `{}`。

READONLY: 已读取 `docs/experience-index.md`；本轮尚未执行服务器、真实库 schema、真实登录写入或发布动作。

READONLY: `docker exec int-ruoyi-mysql mysql ... SELECT ... mes_pro_edhr_batch_execution_task/jimu_report` -> PASS，只读确认测试租户最近任务绑定 Jimu 报表 JSON 合法且顶层存在 `rows`；同时存在少量历史无效 JSON，修复不得 mock 或静默降级。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test` -> FAIL, `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_shouldNormalizeExecutionSnapshotJsonStructure` 断言 `detail.sheetLayoutJson` 可渲染失败，实际为 `{}`。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test` -> PASS，97 tests, 0 failures。

ROOT_CAUSE: `openOrCreateByContext` 原先将运行态执行记录的 `sheetLayoutJson/metaJson` 写成 `{}`，真实布局只存在于 `executionSnapshotJson.layout`，导致仍依赖旧字段的页面校验报“缺少电子批记录模板布局”。

FIX: `openOrCreateByContext` 改为从 Jimu JSON 构建同源 `RuntimeSnapshot`，同时持久化真实 `sheetLayoutJson`、`metaJson` 与 `executionSnapshotJson`；`parseReportJson` 增加可渲染 rows 校验，只有 `rows.len` 的空布局继续 fail-fast。
