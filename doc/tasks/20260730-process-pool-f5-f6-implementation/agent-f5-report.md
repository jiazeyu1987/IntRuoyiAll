# F5 Agent Report: Process Pool Review Copy

## Scope

- Worktree: `D:\IntRuoyiWorktree\20260730-process-pool-f5-review-copy`
- Branch: `codex/20260730-process-pool-f5-review-copy`
- Module: F5 工序池审核副本上下限修正模块
- Completion evidence source: only this F5 worktree. Main workspace overflow files are not used as evidence.

## BDD

- BDD: 审核副本保留原始提交 -> Given 工序池提交事件存在原始 payload、报工来源、记录本来源和审核签名 / When 审核人生成并提交审核副本 / Then 原始事件 raw payload 不被改写，审核副本保存 raw payload 快照和来源追溯字段。
- BDD: 上下限修正 -> Given 字段映射提供正式字段编码、字段名称、上下限元数据 / When 原始值高于上限、低于下限或处于范围内 / Then correctedValue 分别 clamp 到上限、clamp 到下限或保持原值，并保存 rawValue/correctedValue/ruleType 三元组。
- BDD: 缺失前置条件阻塞 -> Given 缺 raw payload、缺字段映射、缺上下限元数据、缺审核签名、签名人不是审核人、签名重复或字段已被 FIFO 分配锁定 / When 生成审核副本 / Then fail fast 拒绝提交，不写入审核副本。
- BDD: 时间轴只读摘要 -> Given 工序池时间轴查询提交事件 / When 存在或不存在审核副本 / Then 只读展示审核副本状态和字段数量摘要，不暴露生成、提交或 FIFO 写操作。

## RED Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest#shouldCreateReviewCopyTables" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL. Method: temporarily reversed only F5 production implementation patch in this worktree while keeping F5 tests. Expected reason: missing F5 production classes and schema. Observed reason: testCompile failed because `MesProcessPoolReviewCopyDO`, `MesProcessPoolReviewCopyFieldDO`, review copy mappers, service and DTOs were not found.

## GREEN Evidence

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest#shouldCreateReviewCopyTables" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldPreserveRawEventPayloadWhenGenerateReviewCopy" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldClampValueToMaxWhenRawValueExceedsMax,MesProcessPoolReviewCopyServiceTest#shouldClampValueToMinWhenRawValueBelowMin,MesProcessPoolReviewCopyServiceTest#shouldKeepValueWhenRawValueWithinRange" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldBlockWhenLimitMetadataMissing,MesProcessPoolReviewCopyServiceTest#shouldBlockWhenFieldMappingMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldRequireReviewerSignatureWhenSubmitReviewCopy,MesProcessPoolReviewCopyServiceTest#shouldRejectReviewCorrectionForAllocatedQuantityFragment" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest,MesProcessPoolReviewCopyServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineTraceabilityTest,ProcessPoolTimelineContentSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-frontend-static.spec.cjs` -> PASS.

## Implementation

- Added formal migration `20260730_mes_process_pool_review_copy.sql` with review copy header and field detail tables.
- Added DO and mapper layer for `mes_pro_process_pool_review_copy` and `mes_pro_process_pool_review_copy_field`.
- Added review copy service and DTOs. The service reads the source event raw payload, validates explicit field mappings and explicit limit metadata, clamps numeric values, saves raw/corrected/rule fields, keeps source feedback/recordbook traceability, and preserves the original event payload.
- Added fail-fast blocks for missing raw payload, missing field mapping, missing limit metadata, missing reviewer signature, signer/reviewer mismatch, duplicate signature and FIFO allocated quantity fragment mutation.
- Extended process pool timeline SQL to expose read-only audit copy status and summary. Timeline static checks confirm no write action was added.

## Requirement Gate

- R14/R15/R16: original event raw payload is not rewritten; review copy stores raw payload snapshot and field-level raw/corrected values.
- R18: no default limits, no field-name guessing, no fallback success; missing metadata blocks.
- R19: FIFO allocated fragments use `MesProcessPoolFifoAllocationService#validateOriginalFieldMutationAllowed` and block quantity/allocation-impacting corrections.
- R21: timeline remains read-only and only displays status/summary.
- No surplus pool reuse; schema test asserts no `mes_pro_feedback_surplus_pool` dependency.
- No front-end write path or timeline write request was introduced.

## Changed Paths

- `IntRuoyiBackend/sql/mysql/20260730_mes_process_pool_review_copy.sql`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProcessPoolReviewCopyDO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProcessPoolReviewCopyFieldDO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProcessPoolReviewCopyMapper.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProcessPoolReviewCopyFieldMapper.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolReviewCopyService.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolReviewCopyServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/dto/MesProcessPoolReviewCopyGenerateReqDTO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/dto/MesProcessPoolReviewCopyFieldMappingDTO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProcessPoolReviewCopySchemaTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolReviewCopyServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/create_tables.sql`
- `IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/clean.sql`
- `docs/worktree-memory.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/task.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/execution-log.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/agent-f5-report.md`

## Merge-Time Reverification

- Frontend real E2E was not run in this F5 worktree; main agent should run the F5/F6 combined real path after merging with the paired frontend/backend runtime.
- Main agent should verify migration ordering with the final F5/F6 merged schema set.
- Main agent should confirm menu/API exposure strategy for a formal review-copy entry if product scope requires a write API beyond service-level integration.
