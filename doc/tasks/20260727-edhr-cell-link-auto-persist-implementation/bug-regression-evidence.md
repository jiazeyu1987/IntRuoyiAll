# Bug Regression Evidence

## Bug

用户配置了“生产批号”到“粗洗工序生产记录”目标单元格的链接，但创建或打开批次执行记录后目标单元格为空。已分析确认生产工单批号和链接规则存在，根因不是“批号本来没有”，而是链接计算结果只在前端草稿态临时 hydrate，没有在创建/打开执行记录时落库到正式 `cell_values_json`。

## Expected

- 创建或打开 DRAFT 执行记录时，后端自动解析启用链接规则。
- 来源为 `PRODUCTION_WORK_ORDER.batchCode` 且值存在时，目标单元格写入执行记录 `cell_values_json`。
- 自动写入必须经过字段审计链，更新 hash/head revision/idempotency evidence。
- 来源批号缺失时 fail-fast，不写空字符串或默认值。
- 目标格已有人工值时不覆盖。
- 重复打开时幂等，不重复追加审计批次。
- 前端只显示已保存详情，不用 `/prefill` 结果冒充保存。

## Reproduction

- Reproduction command/path: 设计复盘中通过截图和当前链路定位到批号来源存在、规则启用、目标 execution `cell_values_json=[]`；前端 `ExecutionPage.vue` 曾在 DRAFT 页调用 `/prefill` 并写入本地 draft，但没有字段审计保存。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" test` -> FAIL expected before fix because no backend auto-persist service wrote applicable prefill through `saveSystemCellLinkChanges`。
- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL expected before frontend fix because execution page still contained `/prefill` draft injection.

## Root Cause

`MesProBatchRecordCellLinkServiceImpl#getPrefill` 已能计算可用预填项，但它只是预览/诊断结果。`MesProBatchRecordExecutionServiceImpl#openOrCreateByContext` 新建执行记录时初始化 `cellValuesJson("[]")`，没有把链接值写入数据库；前端再把 `/prefill` 结果写入本地草稿，导致执行页可能看到临时值，但只读预览、批次详情和字段审计链仍读取不到正式保存值。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 138 tests。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext+openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests。
- GREEN: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> PASS。

## Verification

- Regression tests cover auto-persist success, missing source fail-fast, manual target conflict, idempotent repeated open, execution-create response summary, task-open response summary, field audit save behavior, and frontend removal of draft prefill injection.
- `git diff --check -- <task-owned implementation files and implementation task docs>` -> PASS with LF-to-CRLF warnings only.
- Full `MesProEdhrBatchExecutionServiceTest` was run as an exploratory broad regression and failed on unrelated existing blockers: missing H2 column `bpm_form_template_version.batch_record_report_id`, invalid batch record attachment owner config, and a pending-approval action expectation mismatch.

## Risk

- The implementation deliberately avoids direct SQL backfill, GET-detail side effects, frontend fallback display, and silent default values.
- Existing historical DRAFT records are repaired only through explicit create/open write boundaries when still DRAFT and rule context applies.
- Real Playwright verification is still needed in an environment with running frontend/backend, login, tenant, and task-owned writable eDHR data before claiming browser-path completion.

## Blockers

- No blocker remains for targeted code-level regression.
- Real E2E is blocked until local runtime/login/test-tenant prerequisites are explicitly available; API-only verification is not accepted as a substitute.
