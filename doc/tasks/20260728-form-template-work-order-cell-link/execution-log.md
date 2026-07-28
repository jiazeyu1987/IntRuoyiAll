# Execution Log

## Intent

用户要求在表单中心模板预览红框位置增加“链接”按钮，功能与批记录表单下“链接”一致，可以把生产工单列值链接到表单。

## Baseline

- `713ef9fb`：baseline existing dirty workspace。
- `06fabade`：baseline residual dirty workspace。
- `d2b09536`：baseline form template fill config edits。
- `288b3f83`：baseline additional residual workspace edits。
- `47e1d2a0`：baseline residual task docs。
- Baseline 后当前工作区可用于本任务实现；当前分支 `int_main` 已 ahead origin。

## BDD

- BDD: 表单模板链接入口 -> Given 已选中可交互表单模板 When 用户点击预览工具栏“链接” Then 进入批记录单元格链接工作台并携带 `templateId + versionNo`。
- BDD: 生产工单字段链接到模板单元格 -> Given 工作台选择生产工单字段和模板目标单元格 When 保存链接规则 Then 规则以 `FORM_TEMPLATE_VERSION` 作用域保存并可重新加载。
- BDD: 动态表单实例预填 -> Given 模板版本存在启用的生产工单字段链接规则 When MES 创建该模板的表单中心实例 Then 对应单元格写入生产工单字段值。
- BDD: 缺失正式前置 fail fast -> Given 模板版本不存在、模板无可链接单元格或生产工单字段不存在 When 加载/保存/预填 Then 返回明确错误且不返回默认成功。

## Evidence

- GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md` 并摘录表单模板、单元格链接、E2E 与 Git/PowerShell 适用门禁。
- RED: `node tests/e2e/form-center-static.spec.js` -> FAIL, expected `openSelectedTemplateCellLinks` is missing.
- RED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> FAIL, expected `templateId?: number` API parameter is missing.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkControllerTest" test` -> FAIL, reactor dependency modules have no matching tests and require quoted `-Dsurefire.failIfNoSpecifiedTests=false`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `getWorkbenchContext` does not accept `templateId/versionNo`.

## Implementation

- 前端：在 `src/views/form-center/template/index.vue` 的模板预览工具栏增加“链接”按钮，新增 `openSelectedTemplateCellLinks` 跳转 `/mes/pro/batch-record-cell-link` 并传递 `templateId`、`versionNo`、`returnTo`、`returnLabel`。
- 前端：在 `src/api/mes/pro/batchrecordcelllink/index.ts` 与 `src/views/mes/pro/batchrecordcelllink/index.vue` 支持模板版本 query 参数和返回原页面。
- 前端：补齐 `src/router/modules/remaining.ts` 中表单中心 `policy` 子路由，修正 `form-center-static` 对当前仓库 SQL 路径的读取。
- 后端：扩展 `MesProBatchRecordCellLinkController` / `MesProBatchRecordCellLinkService`，新增 `templateId`、`versionNo` 参数和 `buildFormTemplateVersionPrefillData`。
- 后端：`MesProBatchRecordCellLinkServiceImpl` 新增 `FORM_TEMPLATE_VERSION` scope、`FORMTPL:<templateVersionId>` 虚拟目标 report、模板 `jimuSchemaJson` 单元格解析、规则保存校验和生产工单字段预填。
- 后端：`MesProEdhrBatchExecutionServiceImpl` 创建动态表单实例时调用单元格链接服务，把生产工单字段规则合并到正式 `FormInstanceCreateReqVO.formData`。

## GREEN

- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkControllerTest,MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 155 tests, 0 failures, 0 errors。

## Current Status

- 任务实现与定向验证完成，状态进入 `ready_for_closeout`。
- 当前工作区仍存在大量并行任务脏改动；提交前必须选择性暂存本任务文件，不得使用宽泛 `git add -A`。
