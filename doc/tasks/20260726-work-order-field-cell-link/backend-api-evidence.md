# Backend API Evidence

## Scope

- Service: `MesProBatchRecordCellLinkServiceImpl`
- Contract: 批记录单元格链接 workbench context、rules save、prefill 三段链路支持 `PRODUCTION_WORK_ORDER` 来源。
- Schema: `mes_pro_batch_record_cell_link_rule` 增加 `source_type`、`source_field_code`、`source_field_name`。

## API And Data Contract

- `BatchRecordCellLinkWorkbenchContextRespVO.sourceFields` 返回生产工单字段白名单。
- `BatchRecordCellLinkRuleSaveItemReqVO` 接收来源类型和来源字段。
- `MesProBatchRecordCellLinkRuleDO` 持久化来源类型和字段快照。
- `BatchRecordCellLinkPrefillItemVO` 返回来源类型和字段快照给执行页。

## Validation And Failure Behavior

- Unsupported `sourceType` or `sourceFieldCode`: fail-fast `PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED`。
- Missing target execution work order: fail-fast `PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING`。
- Existing target duplicate and pair duplicate rules remain enforced.
- No fallback, no mock work order, no default-success value.

## Migrations

- Fresh schema: `IntRuoyiBackend/sql/mysql/20260711_mes_batch_record_cell_link_rule.sql`
- Forward migration: `IntRuoyiBackend/sql/mysql/20260726_mes_batch_record_cell_link_work_order_source.sql`
- Test schema: `IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/create_tables.sql`

## BDD Scenarios

- BDD: Workbench source fields -> Given workbench context requested When the route/version scope is valid Then response includes selectable production work order source fields.
- BDD: Work order source rule save -> Given a save request uses `PRODUCTION_WORK_ORDER` and a whitelisted field When saving rules Then the rule persists field metadata without requiring a source execution.
- BDD: Work order source prefill -> Given a draft target execution has a work order When prefill is requested Then the target cell receives the configured work order field value.

## RED And GREEN

- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" test` -> FAIL，生产工单字段来源测试/契约未实现且隔离分支存在并行基线编译阻塞。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。

## Verification

- `MesProBatchRecordCellLinkSchemaTest` verifies DO and schema columns.
- `MesProBatchRecordCellLinkServiceImplTest#getPrefill_resolvesProductionWorkOrderFieldWithoutSourceExecution` verifies runtime work order field prefill without source execution.

## Blockers

- None remaining for targeted backend verification.
