# Backend API Evidence

## Scope

实现 eDHR 批记录单元格链接自动落库后端行为：创建或打开传统批记录执行记录时，服务端根据启用的 `PRODUCTION_WORK_ORDER.batchCode` 等链接规则，把目标单元格值写入执行记录 `cell_values_json`，并通过字段审计链更新哈希、修订号和幂等证据。

Owned backend files include:

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/BatchRecordCellLinkAutoPersistCommand.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/BatchRecordCellLinkAutoPersistResult.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkAutoPersistService.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkAutoPersistServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionFieldAuditServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`

## Contract

- `MesProBatchRecordExecutionServiceImpl#openOrCreateByContext` 在新建执行记录后调用 `MesProBatchRecordCellLinkAutoPersistService#autoPersist`，触发点为 `EXECUTION_CREATE`。
- 已存在 DRAFT 执行记录打开时调用同一自动落库服务，触发点为 `TASK_OPEN` 或打开上下文触发点，并在响应中返回 `cellLinkAutoPersist` 摘要。
- `MesProEdhrBatchExecutionServiceImpl#openTask` 将 `openOrCreateByContext` 的 `cellLinkAutoPersist` 透传到 `EdhrBatchExecutionTaskOpenRespVO`。
- 自动落库只处理 DRAFT 执行记录；非 DRAFT 不写入。
- `SOURCE_VALUE_MISSING` 且来源为 `PRODUCTION_WORK_ORDER` 时 fail-fast，不写空值、默认值或 mock 成功。
- `TARGET_ALREADY_MANUAL` 不覆盖人工值；若已有相同系统幂等审计批次，则返回 `NO_CHANGE_ALREADY_APPLIED` 且不追加审计批次。
- 系统自动预填调用字段审计服务的系统写入边界 `saveSystemCellLinkChanges`，不直接 update 主表 `cell_values_json`。
- 字段审计 `idempotency_key` 使用稳定原始组合键的 SHA-256，固定为 64 位小写十六进制；系统写入和重复打开查询必须复用同一生成逻辑，严格满足审计表 `varchar(64)`。
- 传统批记录 active execution 查询按工单、工序、报表和批号复用执行记录，不再绑定生产任务 `taskId`，避免同一传统工序因不同排产任务重复创建执行记录。

## Validation

- BDD: Production work order batch code auto-persists on execution create/open -> Given 启用 `PRODUCTION_WORK_ORDER.batchCode` 到目标单元格的链接规则，When 创建或打开 DRAFT 执行记录，Then 后端写入 `cell_values_json` 并更新字段审计链。
- BDD: Existing manual target value is not overwritten -> Given 目标格已有人工保存值且规则为 `ONLY_WHEN_EMPTY`，When 自动落库执行，Then 返回 `TARGET_ALREADY_MANUAL`，不覆盖原值。
- BDD: Missing production batch code fails fast -> Given 生产工单批号为空但规则启用，When 创建或打开执行记录，Then 抛出明确业务错误，不写空值。
- BDD: Repeated open is idempotent -> Given 同一规则版本和来源值已经自动落库，When 重复打开，Then 返回 `NO_CHANGE_ALREADY_APPLIED`，不追加字段审计批次。
- BDD: Audit idempotency key fits schema -> Given 自动预填需要写入或查询字段审计批次，When 根据执行、规则、目标格和来源值生成幂等键，Then 保存与查询使用同一 64 位 SHA-256 键。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" test` -> FAIL expected before implementation because `MesProBatchRecordCellLinkAutoPersistServiceImpl` did not persist link values through the field-audit chain.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_autoPersistsCellLinksOnNewExecutionAndReturnsSummary" test` -> FAIL expected before wiring `openOrCreateByContext` to auto-persist and response summary.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests, 0 failures.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 138 tests, 0 failures.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext+openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures.
- RED: isolated worktree `MesProBatchRecordCellLinkAutoPersistServiceImplTest` -> FAIL, 4 tests, 2 failures; generated plaintext key length was `101`, exceeding the schema limit.
- GREEN: isolated worktree full lifecycle `MesProBatchRecordCellLinkAutoPersistServiceImplTest` -> PASS, 4 tests; save and lookup keys both match `[0-9a-f]{64}`.
- REGRESSION: isolated worktree adjacent backend suite -> PASS, 138 tests.

## Verification

- Backend service verification proves success write, missing source fail-fast, manual target non-overwrite, and repeated-open idempotency.
- Field audit verification proves system cell-link changes reuse the field-audit save path and preserve hash/revision behavior.
- Execution service verification proves `openOrCreateByContext` calls auto-persist on new executions and returns the summary.
- eDHR task-open focused verification proves task open response includes the auto-persist summary and traditional execution context remains schedule-task independent.
- Real Playwright on `8086/48086` proves `task/open` completed through the page path and returned `NO_CHANGE_ALREADY_APPLIED` for the already persisted target value.
- Database readback proves execution `1571` has `field_audit_revision=1`, target `3:3=34126020001` in `cell_values_json`, exactly one automatic-prefill audit batch, and a 64-character idempotency key.
- Full `MesProEdhrBatchExecutionServiceTest` is not used as task completion evidence because current unrelated failures pre-exist this slice: H2 test schema missing `bpm_form_template_version.batch_record_report_id`, invalid attachment owner config, and a pending-approval allowed-actions expectation mismatch.

## Observability

- Auto-persist result exposes `executionId`, `trigger`, `appliedCount`, `conflictCount`, item statuses, `fieldAuditRevisionAfter`, `fieldAuditHeadHashAfter`, and `cellValuesHashAfter`.
- Field audit batch idempotency key namespace is `CELL_LINK_AUTO_PREFILL`, enabling repeated opens to be traced without duplicate audit writes.
- The persisted key is a schema-safe digest; namespace and semantic inputs remain represented by the associated audit batch/change records and deterministic generation contract.
- Reason text records system automatic prefill intent, including production batch-code specific text when applicable.

## Blockers

- No backend blocker remains for the owned automatic prefill slice.
- Broader full-class eDHR regression remains blocked by unrelated schema/config/assertion failures listed in `verification-report.md`; this task does not broaden scope to repair those parallel failures.
