# Verification Report

## Summary

已修复表单模板“链接”入口的两类阻塞。前端现在会先把默认来源归一化为 `defaultSourceReportId`，再在加载源区域前同步 `sourceType`，因此表单模板入口会直接渲染生产工单字段矩阵。后端现在会按 Form Center 模板的正式保存形态解析布局，支持字符串型 `sheetLayoutJson`、对象型 `sheetLayoutJson`、字符串/对象型 `layout`、根对象 `rows`，以及 `jimu_schema_json` 为空但 `recognized_schema_json` 为字段数组的识别模板形态；无布局或非法布局仍按原规则 fail-fast。

## Commands

- `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js` -> PASS
- `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> PASS
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS
- `node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS
- `node tests/e2e/form-center-static.spec.js` -> PASS
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-form-template-cell-link-work-order-init/bug-regression-evidence.md` -> PASS
- `git -C E:\IntRuoyi diff --check -- <task-owned-files>` -> PASS, only Git LF-to-CRLF working-copy warnings
- `mvn -pl yudao-server -am "-DskipTests" package` -> PASS, generated full executable backend Jar
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS
- `48081` listener check -> PID `56272`, Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-142124.jar`, SHA256 `073AFE1D63B0D1C8F99847F68AB7E2916FCB090CA1DF720C63B58952D0B68903`
- `GET /admin-api/mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL%3A32` without login -> HTTP 200 body code `401`, target route is loaded behind security

## Root Cause

表单模板链接入口的后端上下文默认来源是 `PRODUCTION_WORK_ORDER`。前端旧逻辑先把 `sourceType` 固定为 `BATCH_RECORD_CELL`，再设置 `sourceReportId=PRODUCTION_WORK_ORDER` 并立即加载源单元格，导致请求 `/form-cells?reportId=PRODUCTION_WORK_ORDER`。后端按正式表单查询失败后，正确 fail-fast 返回“批记录单元格链接引用的表单不存在：PRODUCTION_WORK_ORDER”。

`FORMTPL:32` 表示 Form Center 模板版本 ID 32 的虚拟报表 ID。第一次后端修复覆盖了 `jimuSchemaJson` 中的 `sheetLayoutJson/layout/rows` 形态；本次继续核对真实库发现版本 32 的 `jimu_schema_json` 为 `NULL`，`recognized_schema_json` 是正式识别字段数组。旧实现没有把该字段数组转换成链接目标布局，因此页面仍报“批记录表单布局 JSON 无效：FORMTPL:32”。

## Fix

- 在 `loadWorkbenchContext` 中新增 `defaultSourceReportId`。
- 在调用 `loadSourceCells()` 前，根据 `defaultSourceReportId === PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID` 同步 `sourceType`。
- 新增静态回归测试，禁止初始化阶段再把 `PRODUCTION_WORK_ORDER` 作为普通表单报表 ID 查询。
- 在 `getFormTemplateCells()` 中通过 `resolveTemplateSheetLayoutJson(...)` 解析 Form Center 模板布局，纳入根布局 JSON、对象型布局和字符串型布局几种正式保存形态。
- 新增后端回归测试，覆盖 `FORMTPL:32` 根布局 JSON 可解析出“生产批号”目标单元格。
- 在 `getFormTemplateCells()` 中补充 `recognized_schema_json` 字段数组解析：当 `jimu_schema_json` 缺失且识别字段存在时，按字段数组生成只用于链接目标选择的表格布局和 `cellRules`。
- 新增后端回归测试，覆盖 `FORMTPL:32` 的 `jimu_schema_json=NULL + recognized_schema_json=字段数组` 可解析出“生产批号”目标单元格。

## Blockers

- 当前工作区存在大量非本任务脏改动；未提交、未推送本任务改动，避免混入并行任务文件。
- `48081` 已加载新后端 Jar；旧“未重启加载修复”阻塞已解除。
- 登录态目标接口复验仍被本地依赖超时阻塞：登录请求超时，`/actuator/health` 连续 3 次 20 秒超时，后端日志显示 JDBC/Redis 连接获取超时，Docker 只读检查也超时。未对无关 DB/Redis/Docker 进程执行重启或终止。
