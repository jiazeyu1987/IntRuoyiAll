# Execution Log：eDHR 模板说明缺少布局回归修复

BDD: 模板说明页显示已有布局 -> Given 批记录报表的 Jimu JSON 中存在有效模板 rows / When 用户打开 eDHR 批次模板说明页 / Then 页面能收到 sheetLayoutJson 并渲染模板说明，不显示缺少布局错误。
BDD: 真正缺少布局继续 fail-fast -> Given 报表 JSON 确实没有可识别模板 rows / When 用户打开模板说明页 / Then 页面显示明确布局缺失错误，不展示默认模板。

READONLY: 已读取 `docs/experience-index.md`，命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本轮尚未执行真实登录写入 E2E、服务器操作、数据库写入或发布动作。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test` -> FAIL，执行详情 `sheetLayoutJson` 为 `{}`，会触发前端“缺少电子批记录模板布局”校验。

GREEN: `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS。

GREEN: `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordExecutionServiceImplTest" test` -> PASS，后端执行详情和报表布局接口返回真实可渲染 rows。

FIX: 前端保持现有 fail-fast 逻辑；后端修复旧 `sheetLayoutJson` 字段与 `executionSnapshotJson.layout` 的同源返回，避免页面误报缺少模板布局。
