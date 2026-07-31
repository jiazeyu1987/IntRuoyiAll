# Backend API Evidence

## Scope

- Endpoint: `GET /mes/pro/batch-record-cell-link/workbench-context`
- Service: `MesProBatchRecordCellLinkService`
- Runtime: `MesProEdhrBatchExecutionServiceImpl#createFormCenterInstanceForTask`

## API Contract

- `workbench-context` 新增可选参数 `templateId`、`versionNo`。
- 当 `templateId + versionNo` 存在时，服务解析为 `FORM_TEMPLATE_VERSION` scope。
- 模板目标 report 使用虚拟 ID `FORMTPL:<formTemplateVersionId>`，不写入批记录 report 表。

## Data Contract

- 规则继续保存到 `mes_pro_batch_record_cell_link_rule`。
- `scopeType = FORM_TEMPLATE_VERSION`，`scopeId = bpm_form_template_version.id`。
- 生产工单来源仍使用 `sourceType = PRODUCTION_WORK_ORDER` 和既有字段清单。
- 动态表单实例创建时把启用规则合并进 `FormInstanceCreateReqVO.formData`。

## Validation And Error Behavior

- 模板版本不存在、模板 schema 缺失、布局缺失、无可链接单元格、生产工单缺失、来源字段不存在或来源值为空时 fail-fast。
- 不使用 `formBindings` 推断批记录表单，不把表单中心模板降级为批记录 report。
- 对已有非空 formData 目标值不覆盖，保持 `ONLY_WHEN_EMPTY` 策略。

## BDD

- BDD: 生产工单字段链接到模板单元格 -> Given 工作台选择生产工单字段和模板目标单元格 When 保存链接规则 Then 规则以 `FORM_TEMPLATE_VERSION` 作用域保存并可重新加载。
- BDD: 动态表单实例预填 -> Given 模板版本存在启用的生产工单字段链接规则 When MES 创建该模板的表单中心实例 Then 对应单元格写入生产工单字段值。
- BDD: 缺失正式前置 fail fast -> Given 模板版本不存在、模板无可链接单元格或生产工单字段不存在 When 加载/保存/预填 Then 返回明确错误且不返回默认成功。

## RED / GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`getWorkbenchContext` 不接受 `templateId/versionNo`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkControllerTest,MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，155 tests, 0 failures, 0 errors.

## Verification

- `MesProBatchRecordCellLinkControllerTest` 覆盖 controller 参数透传。
- `MesProBatchRecordCellLinkServiceImplTest` 覆盖模板版本 scope、模板单元格解析和生产工单字段预填。
- `MesProEdhrBatchExecutionServiceTest` 覆盖 MES 动态表单实例创建时使用链接预填后的 `formData`。
- Maven 定向命令 PASS：155 tests, 0 failures, 0 errors.

## Observability

- 保留现有 `ServiceException` 错误码路径；未新增吞异常或默认成功路径。

## Blockers

- 无当前任务阻塞。
