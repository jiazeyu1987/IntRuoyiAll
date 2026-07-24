# eDHR 模板内电子签名显示

## 目标

在电子批记录模板中配置签名单元格，并在历史批记录/复盘只读表格中把电子签名显示回模板内部对应单元格。

## 里程碑

1. RED：补充后端接口契约与复盘 VO 失败断言。
2. GREEN：实现签名位读取、保存、复盘 marker 返回。
3. REGRESSION：验证现有导入、预览、重命名、复盘接口不回退。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。签名位作为模板单元格元数据持久化，不靠文字猜测。
- 是否存在临时补丁或绕过：否。

## 验证

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest,MesProEdhrBatchExecutionControllerTest test`

## 当前状态

- completed。

## 完成记录

- 后端新增签名单元格 marker 读写接口，签名位持久化在 Jimu 报表 JSON 的 `edhrSignature` 元数据中。
- 复盘接口 `formViewModel` 返回 `signatureCellMarkers`，旧执行快照没有 marker 时只读读取当前模板 marker，不回写历史快照。
- 验证通过：目标 Maven 测试、后端构建、Playwright 真实页面验证。
