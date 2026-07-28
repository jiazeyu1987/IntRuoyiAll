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
- `48081` 当前运行 Jar 仍是旧的 `backend-switch-filler-20260728-131920.jar`，未加载本次 class；已生成补丁 Jar `output/runtime/int_main/backend-formtpl32-20260728-1328.jar`，但自动停止/启动旧后端进程被环境安全策略拦截，页面需后端重启加载新 Jar 后才会看到修复效果。
