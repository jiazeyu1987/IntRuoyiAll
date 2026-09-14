# Bug Regression Evidence

## Bug Summary

表单模板页签点击“链接”后，批记录单元格链接工作台先提示“批记录单元格链接引用的表单不存在：PRODUCTION_WORK_ORDER”。修复入口初始化后，继续加载目标模板时提示“批记录表单布局 JSON 无效：FORMTPL:32”；第一次后端修复后，真实模板版本 32 仍因 `jimu_schema_json=NULL` 且布局只在 `recognized_schema_json` 字段数组中而继续报同一错误。

## Expected Behavior

表单模板链接入口默认来源为生产工单时，工作台应直接显示生产工单字段列表，并允许把“生产批号”等字段链接到当前表单模板单元格。目标模板为 `FORMTPL:<versionId>` 时，后端应按 Form Center 模板正式布局 JSON 或正式识别字段数组解析可链接目标单元格；无布局、无识别字段或非法 JSON 仍应 fail-fast。

## Reproduction

- Path: 表单模板页签选择已发布模板，点击“链接”。
- Data: 本地 `bpm_form_template_version.id=32` 的 `jimu_schema_json` 为 `NULL`，`recognized_schema_json` 为 JSON array。
- RED command: `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js`
- RED command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- RED command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`loadWorkbenchContext` 旧逻辑先固定 `sourceType = BATCH_RECORD_CELL`，再把 `sourceReportId` 设置为后端返回的 `PRODUCTION_WORK_ORDER` 并立即调用 `loadSourceCells()`。因此前端走普通表单单元格接口查询 `reportId=PRODUCTION_WORK_ORDER`，后端按正式报表查不到后 fail-fast 返回该错误。

`getFormTemplateCells()` 旧逻辑只读取 `schema.getString("sheetLayoutJson")`。部分 Form Center 模板把布局直接保存在根对象 `rows`，或保存在对象型 `sheetLayoutJson` / `layout` 中；因此 `FORMTPL:32` 这类模板解析到空布局并触发“批记录表单布局 JSON 无效”。

继续排查真实库后确认，版本 32 并没有 `jimu_schema_json`，只有 `recognized_schema_json` 字段数组。旧实现未把该正式识别字段形态转换为链接工作台可用的目标单元格布局，因此仍在模板 schema 解析阶段 fail-fast。

## Regression Test

新增 `IntRuoyiFronted/tests/e2e/form-template-cell-link-work-order-init-static.spec.js`，断言初始化必须先归一化 `defaultSourceReportId`，并在加载源单元格前同步生产工单来源类型。

新增 `MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema`，断言根布局 JSON 中的 `rows + cellRules` 可以解析出 `FORMTPL:32` 的可链接目标单元格。

新增 `MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing`，断言 `jimu_schema_json` 缺失但 `recognized_schema_json` 为字段数组时，可以生成可链接目标单元格。

## RED

- RED: `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js` -> FAIL, 工作台初始化没有归一化默认来源，仍会先固定为普通批记录来源。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 修复前根布局 JSON 模板会触发 `批记录表单布局 JSON 无效：FORMTPL:32`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 修复前 `jimu_schema_json=NULL + recognized_schema_json=字段数组` 会触发 `批记录表单布局 JSON 无效：FORMTPL:32`。

## GREEN

- GREEN: `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js` -> PASS
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> PASS
- GREEN: `node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS
- GREEN: `node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS
- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS

## Verification

- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-form-template-cell-link-work-order-init/bug-regression-evidence.md`

## Risk And Regression Scope

风险集中在单元格链接工作台初始化和 Form Center 模板单元格解析。修复只改变默认来源对应的 `sourceType` 同步顺序，并把已有正式模板布局形态和识别字段形态纳入解析契约；不改变保存规则、表单模板作用域或批记录表单链路，也不吞掉真正无效的布局 JSON。

## Blockers

当前工作区存在大量非本任务脏改动；未提交、未推送本任务改动，避免混入并行任务文件。

`48081` 当前运行 Jar 仍是旧的 `backend-switch-filler-20260728-131920.jar`，未加载本次 class；已生成补丁 Jar `output/runtime/int_main/backend-formtpl32-20260728-1328.jar`，但自动停止/启动旧后端进程被环境安全策略拦截，页面需后端重启加载新 Jar 后才会看到修复效果。
