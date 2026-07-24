# 执行日志：电子批记录按批记录名称删除

## 2026-06-26

- 初始化任务：用户要求“蓝色的按钮放在红色的位置，每个批记录单独可以删除”，确认现有前端只有单报表删除和全量删除，没有按批记录名称删除的正式 API。
- BDD: 按批记录名称删除未绑定模板 -> Given 批记录名称下存在多份未绑定电子批记录报表 / When 调用按名称删除接口 / Then 删除对应 Jimu 报表与元数据，并返回删除数量。
- BDD: 已绑定模板保留 -> Given 批记录名称下同时存在已绑定和未绑定报表 / When 调用按名称删除接口 / Then 未绑定报表删除，已绑定报表保留并返回保留数量。
- BDD: 缺少批记录名称失败 -> Given 删除请求缺少批记录名称 / When 调用接口 / Then 后端失败并提示批记录名称不能为空。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译失败，`MesProBatchRecordReportService` / `MesProBatchRecordReportController` 缺少 `deleteGeneratedReportsByBatchRecordName` 方法。
- GREEN: `apply_patch` -> PASS，新增后端 `DELETE /mes/pro/batch-record-report/delete-by-batch-record-name`、服务方法、前端 API 和左侧批记录名称行内删除按钮。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增绑定保护用例插入 `mes_pro_route_process` 时使用了不存在的 `process_code/process_name` 字段。
- GREEN: `apply_patch` -> PASS，按 H2 测试 schema 和既有用例字段修正绑定关系测试夹具。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，43 tests。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
