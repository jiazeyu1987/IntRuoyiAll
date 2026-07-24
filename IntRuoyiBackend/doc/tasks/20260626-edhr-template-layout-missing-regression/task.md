# 任务：eDHR 模板布局接口缺失回归修复

## 任务目标

- 修复批次模板说明页依赖的 `getCellRules/getSignatureCellMarkers` 未返回 `sheetLayoutJson` 导致“缺少电子批记录模板布局”的问题。
- 后端必须从真实 Jimu 报表 JSON 解析已有模板布局，不返回 mock、默认表格或空成功。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-signature-cell-electronic-signature\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成并提交；本次只处理 MES 批记录报表布局接口回归。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无后端本机单元测试专属经验文档。
- 适用强制门禁：
  - 本轮不操作服务器、不改真实数据库 schema、不执行发布或远端联调。
  - 若后续进入真实 E2E 或登录后写入验证，必须先补登录预检记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。无真实 rows 时继续抛明确错误。
- `是否从根因和长期维护角度解决`：是。修正报表 JSON 布局读取合同，让接口稳定返回已有布局。
- `是否存在临时补丁或绕过`：否。不新增 mock 布局、不绕过 Jimu 报表源。

## BDD 场景

- `BDD: cell-rules 返回已有模板布局 -> Given Jimu 报表 JSON 中存在有效模板 rows / When 调用 cell-rules 接口 / Then 响应包含 sheetLayoutJson.rows。`
- `BDD: signature-cell-markers 返回同一模板布局 -> Given Jimu 报表 JSON 中存在有效模板 rows / When 调用 signature-cell-markers 接口 / Then 响应包含 sheetLayoutJson.rows。`
- `BDD: 无模板布局仍明确失败 -> Given Jimu 报表 JSON 没有可识别 rows / When 调用布局相关接口 / Then 后端返回明确 JSON 无效错误，不返回空成功。`

## 里程碑

1. M1：创建任务文档和 RED 单测。`COMPLETED`
2. M2：实现布局解析修复。`COMPLETED`
3. M3：运行后端目标验证。`COMPLETED`
4. M4：收尾预览并按验证结果提交。`COMPLETED`

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test`

## 最终验证结果

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test` -> PASS，97 tests, 0 failures。
- 根因：按上下文创建 eDHR 执行记录时只把真实 Jimu 布局放入 `executionSnapshotJson.layout`，旧响应字段 `sheetLayoutJson` 持久化为 `{}`，执行/只读页前置校验会误判缺少模板布局。
- 修复：运行态快照构建一次性生成 `sheetLayoutJson`、`metaJson`、`executionSnapshotJson` 三个同源字段；报表布局接口拒绝只有 `rows.len` 但无真实数字行单元格的空布局。
