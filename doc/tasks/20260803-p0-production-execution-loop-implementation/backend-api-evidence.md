# Backend API Evidence - P0 M1 PQC 入池 / M2 签名快照 JSON / P0-T00A-T02 合同 / M4 质量数量闸、幂等、FIFO、批记录回填、并发确认 / P0-T10 Trace Failure / P0-T13 收口证据包

## Scope

- 本证据覆盖 M1 / P0-T03：`MesFrontlinePqcContextServiceImpl.submitPqcInspectionFromQaRegulationTaskSource` 在 PQC 正式提交后创建或绑定工序池 `PQC_INSPECTION` 事件。
- 本证据覆盖 M4 第二片：`MesTeamLeaderReportConfirmationServiceImpl.confirmSubmission` 在复核、分配、工单完成写库前，重新校验生产提交根事件、唯一正式 PQC 结构化绑定、PQC `SUCCESS` 结果和 PQC 合格样本数量覆盖本次确认数量。
- 本证据覆盖 M4 / P0-T01 主提交幂等：`MesProcessPoolEventServiceImpl.createEvent` 和 `MesProFrontlineFeedbackSubmitServiceImpl.submit` 在写报工、记录本和工序池事件前，基于 `processPoolSubmissionIdempotencyKey` 查询并返回既有提交结果。
- 本证据覆盖 P0-T04 PQC 重复提交幂等：`MesFrontlinePqcContextServiceImpl.submitPqcInspection` 和 `MesProcessPoolEventServiceImpl.createPqcInspectionEvent` 在写 PQC task、逐件明细、PQC 工序池事件和结构化 PQC 记录前，基于 `pqcSubmissionIdempotencyKey` 查询并返回既有 PQC task / event。
- 本证据覆盖 P0-T07 FIFO 消耗持久化：`MesTeamLeaderReportConfirmationServiceImpl.confirmSubmission` 在写复核、报工分配和工单完成前，锁定 `production_submit_event_id` 来源数量片段并调用 `MesProcessPoolFifoAllocationService.allocate` 写入 process-pool FIFO allocation line。
- 本证据覆盖 P0-T07 活跃工单 FIFO 数量边界：`MesTeamLeaderReportConfirmationServiceImpl.persistFifoConsumptionIfRequired` 构建 process-pool FIFO target 时，必须使用本次确认到目标工单的分配数量作为 `requiredQuantity`，不得使用目标工单计划量放大消耗需求。
- 本证据覆盖 P0-T08 批记录回填字段审计：`MesTeamLeaderBatchRecordBackfillServiceImpl.backfillCompletedProcess` 在正式逐工序批记录绑定和 `PROCESS_POOL_REPORT` 字段映射基础上生成字段审计 change，携带来源新值、当前旧值 hash、字段路径、单元格位置和稳定幂等键。
- 本证据覆盖并发/重复确认边界：`MesTeamLeaderReportConfirmationServiceImpl.confirmSubmission` 在同一事务中先锁定生产提交根事件，再通过 `MesProcessPoolReportAllocationMapper.selectListByEventIdForUpdate` 重查既有分配；命中重复时返回 `PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE`，不得触发 PQC、FIFO、复核、分配、完工或批记录回填下游写入。
- 本证据覆盖 P0-T10 trace 缺投影阻塞：`MesTeamLeaderTraceServiceImpl.getProductionExecutionTrace` 在订单工序完成和批记录字段审计存在、但完成记录缺 `lastEventId` 来源生产提交事件时，必须保持 `batchRecord.status=BLOCKED` 并返回 `BATCH_RECORD_SOURCE_MISSING`，不得只因字段审计存在就把批记录追溯标记完成。
- 本证据新增 P0-T10 trace scope 校验：分配记录和订单工序完成记录即使指向当前 `processPoolEventId`，也必须同属当前生产工单、路线工序和 MES 工序；跨工单、跨路线工序或跨 MES 工序的事实不得拼接进当前闭环。
- 本证据覆盖 P0-T13 后端收口证据包：`MesTeamLeaderTraceServiceImpl.getProductionExecutionTrace` 返回 `closureEvidence`，逐项回答九个审计问题，缺少正式来源或只读复验入口时保持 `complete=false`。
- 本证据补齐 P0-T00A / P0-T02 文档命名合同：`MesP0ProductionExecutionSchemaContractTest` 固化正式字段、索引和 backfill blocker 合同；`MesP0ProductionSubmitClosedLoopContractTest` 固化一线提交同事务写入报工、记录本和工序池生产提交事件，并验证失败传播边界。
- 本证据新增 M2 复核签名快照 fail-fast 边界：`MesTeamLeaderSubmissionReviewServiceImpl.reviewSubmission` 与 `MesTeamLeaderReportConfirmationServiceImpl.confirmSubmission` 必须在读取事件、写复核或写分配前拒绝空或非 JSON 对象 `reviewSignatureSnapshotJson`。
- 涉及后端对象：`MesFrontlinePqcSubmitReqVO`、`MesProFrontlineFeedbackSubmitReqVO`、`MesFrontlinePqcSubmitCommand`、`MesFrontlineDeviceAccountController`、`MesFrontlinePqcContextServiceImpl`、`MesProFrontlineFeedbackSubmitServiceImpl`、`MesProcessPoolEventServiceImpl`、`MesTeamLeaderReportConfirmationServiceImpl`、`MesTeamLeaderBatchRecordBackfillServiceImpl`、`MesTeamLeaderTraceServiceImpl`、`MesProductionExecutionTraceRespVO`、`MesTeamLeaderOrderProcessCompletionService`、`MesProProcessPoolPqcRecordMapper`、`MesProProcessPoolQuantityFragmentMapper`、`MesPqcInspectionTaskMapper`、`MesPqcInspectionPieceDetailMapper`、`MesProcessPoolFifoAllocationService`、`MesP0PqcQualityAllocationGateTest`、`MesP0BatchRecordBackfillClosedLoopTest`、`MesP0ProductionExecutionClosureAuditTest`、`MesP0FrontlineSubmitIdempotencyTest`、`MesProcessPoolPqcEventTest`、`MesFrontlinePqcContextServiceTest`。
- 非目标范围：真实 E2E PASS。

## Contract

- API/Data contract：PQC 提交必须携带 `deviceAccountId`、`deviceId`、`workstationId`、`pqcSubmissionIdempotencyKey`、`signatureId`、`signatureEmployeeId` 和 `signatureSnapshot`。
- Event contract：服务必须调用 `MesProcessPoolEventService.createPqcInspectionEvent`，事件 DTO 至少包含工单、路线、工序、实际员工、设备账号、设备、工作站、模板类型、PQC 来源类型/ID、质量结果、客户端提交时间和签名快照。
- Confirmation quality contract：班组长确认只接受 `eventType=PRODUCTION_SUBMIT` 的根事件；必须通过 `production_submit_event_id` 找到唯一 `MesProProcessPoolPqcRecordDO`，且 `inspectionResult=SUCCESS` 后才允许写复核、分配和工单完成。
- Confirmation quantity contract：PQC `SUCCESS` 还不足以放行；确认服务必须通过 PQC 事件 `feedbackSourceId -> mes_pqc_inspection_task.id` 锁定正式 PQC task，并按 `mes_pqc_inspection_piece_detail` 逐样本明细计算合格样本数，确认数量大于合格可分配数量时返回 `QUALITY_QUANTITY_MISMATCH` 且不写终态事实。
- Production submit idempotency contract：生产提交必须携带 `processPoolSubmissionIdempotencyKey`，后端用同租户、同工单、同路线工序、同工序、同人员、同设备账号、同设备、同工作站和同幂等键查找既有 `PRODUCTION_SUBMIT`，命中时返回既有 `feedbackId`、`recordbookEntryId`、`recordbookEventId`、`processPoolEventId`，不得新增报工、记录本或数量片段。
- Quantity fragment root contract：每个生产提交数量片段必须持久化 `production_submit_event_id=PRODUCTION_SUBMIT.id`，非生产提交事件不得创建数量片段。
- PQC duplicate idempotency contract：PQC 提交必须携带 `pqcSubmissionIdempotencyKey`，后端用同租户、同工单、同路线工序、同工序、同人员、同设备账号、同设备、同工作站、同 PQC task 来源和同幂等键查找既有 `PQC_INSPECTION`；命中时前线服务必须在更新 PQC task、插入逐件明细和创建新 PQC 事件前返回既有 PQC task，事件表、结构化 PQC 记录和逐件明细不得重复。
- FIFO consumption contract：FIFO 确认必须在终态写库前锁定 `MesProProcessPoolQuantityFragmentDO.productionSubmitEventId=PRODUCTION_SUBMIT.id` 的来源片段，构建 `MesProcessPoolFifoAllocationCommand` 并调用正式 process-pool FIFO 服务；返回消耗数量不足本次确认数量时必须抛出 `PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_QUANTITY_MISMATCH`，不得写复核、报工分配或工单完成。
- Active-order FIFO target contract：FIFO 自动分配必须只选择 `ACTIVE` 活跃工单并按 `joinedAt` 顺序生成目标；每个 `MesProcessPoolFifoTargetWorkOrder.requiredQuantity` 必须等于本次确认分配到该工单的数量，`alreadyAllocatedQuantity` 必须来自当前工序已分配数量。
- Batch record backfill audit contract：批记录回填只能读取正式 `BATCH + BATCH_RECORD` 逐工序绑定和 `PROCESS_POOL_REPORT` 字段映射；字段审计 change 必须携带 `fieldPath`、`fieldKey`、`rowIndex`、`columnIndex`、`newValueJson`、`newValueDisplay` 和按当前 `cellValuesJson` / 模板默认值计算的 `expectedOldValueHash`；幂等键固定为 `PROCESS_POOL_REPORT_BACKFILL:<processPoolEventId>:<workOrderId>:<routeProcessId>`。
- Concurrent confirmation contract：班组长确认必须在事务内用 `selectListByEventIdForUpdate(event.id)` 对既有分配做带锁重查；重复/并发请求命中既有分配时必须在 PQC 质量重算、来源片段消耗、复核、分配、订单工序完成和批记录回填前 fail-fast。
- Review signature snapshot contract：复核签名不仅要有签名 ID 和签名员工，还必须有非空且为 JSON 对象的签名快照；空快照或非 JSON 对象快照必须返回 `PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED` 并在读取事件、写复核、写 FIFO 分配之前终止。
- Trace batch record source contract：批记录 trace 分组必须同时具备字段审计明细、来源分配 ID 和来源生产提交事件 ID；`sourceProcessPoolEventId` 缺失时即使批记录执行、字段审计 batch/item 和 `sourceAllocationId` 存在，也必须返回 `BATCH_RECORD_SOURCE_MISSING`。
- Trace allocation/completion scope contract：allocation、completion 和 batchRecord 分组必须同时校验 `eventId/lastEventId` 与 `workOrderId + routeProcessId + processId`；跨当前生产提交根事件、生产工单、路线工序或 MES 工序时必须返回 `ALLOCATION_SOURCE_MISSING`、`ALLOCATION_SCOPE_MISMATCH`、`COMPLETION_SOURCE_MISSING`、`COMPLETION_SCOPE_MISMATCH` 或 `BATCH_RECORD_SOURCE_MISSING`，不得只在 `closureEvidence` 阶段间接失败。
- Closure evidence contract：统一 trace 必须返回 `closureEvidence`，包含 `who`、`device`、`process`、`quantity`、`quality`、`signature`、`workOrder`、`review`、`batchRecord` 九项答案；每项必须包含业务值、正式 `sourceIds`、`sameSource=true` 和只读复验入口，缺任一项时返回 `CLOSURE_EVIDENCE_MISSING_SOURCE` 或同源 blocker，并保持 trace `complete=false`。
- No-fallback contract：缺正式设备/工作站/签名/幂等上下文时 fail fast，不用默认设备、登录人备注、前端文案或旧质量表数据补齐。

## Validation

- 校验正数 ID：`deviceAccountId`、`deviceId`、`workstationId` 必须为正式正数。
- 校验非空幂等键：`pqcSubmissionIdempotencyKey` 缺失时不得继续提交。
- 质量结果归一：`DETECTION_SUCCESS` 进入事件 DTO 时映射为 `SUCCESS`，不得把未知质量默认当合格。
- FIFO 写前质量/数量闸：根事件类型错误、缺唯一结构化 PQC 绑定、PQC 非 `SUCCESS`、PQC 合格样本数不足覆盖确认数量都必须抛出明确错误码，且不得调用 `reviewMapper.insert`、`allocationMapper.insertBatch` 或 `orderProcessCompletionService.applyConfirmedAllocations`。
- FIFO 消耗持久化：FIFO 模式必须调用 `quantityFragmentMapper.selectListByProductionSubmitEventIdForUpdate(event.id)` 和 `processPoolFifoAllocationService.allocate(...)`；返回总消耗量必须等于本次提交确认数量。
- FIFO 目标数量边界：自动 FIFO 模式必须断言传入 process-pool FIFO 服务的目标工单顺序、目标工单编码、`requiredQuantity` 和 `alreadyAllocatedQuantity`；手动分配剩余量不足或总量不匹配必须在复核、分配、完工写入前阻塞。
- 批记录字段审计：正式绑定、执行记录、字段映射、来源值或旧值校验缺失时不得写无来源字段审计；重复完工已成功回填的订单工序不得再次触发 backfill。
- 重复/并发确认：确认服务必须调用 `allocationMapper.selectListByEventIdForUpdate(event.id)`；发现既有分配时不得读取 PQC、不得消耗 FIFO 来源片段、不得写复核/分配/完工/批记录回填。
- 复核签名快照：`reviewSubmission` 和 `confirmSubmission` 必须覆盖空或非 JSON 对象 `reviewSignatureSnapshotJson`，并验证失败路径不调用 `eventMapper`、`reviewMapper.insert` 或 `allocationMapper.insertBatch`。
- Trace 批记录来源：订单工序完成记录缺 `lastEventId` 时，`batchRecord` 分组必须 `BLOCKED`，并返回 `BATCH_RECORD_SOURCE_MISSING`；不得让字段审计明细掩盖来源事件断链。
- 收口证据：完整闭环时 `closureEvidence.answers` 必须恰好覆盖九个审计字段；下游分组已阻塞时顶层 blockers 保持原分组语义，分组完整但收口证据缺正式来源时才将 closure blocker 升到顶层。

## BDD:

- Given 一线 PQC 从 QA 规程任务提交逐件检验、签名和正式设备上下文。
- When 后端保存 PQC 任务状态与逐件明细。
- Then 系统必须创建绑定该 PQC 任务的工序池 PQC 事件，并保留签名、质量结果和正式来源 ID。
- Given 班组长确认同一 `processPoolEventId` 的 FIFO 分配。
- When 根事件不是 `PRODUCTION_SUBMIT`、缺唯一结构化 PQC 绑定或 PQC 结果非 `SUCCESS`。
- Then 后端必须 fail-fast，且不得写复核、分配、工单完成或批记录终态。
- Given 绑定 PQC 记录为 `SUCCESS`，但 PQC task 的逐样本明细中合格样本数小于生产提交确认数量。
- When 班组长确认 FIFO 分配。
- Then 后端必须返回 `PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_QUANTITY_MISMATCH`，且不得写复核、分配、工单完成或批记录终态。
- Given 一线员工已用同一 `processPoolSubmissionIdempotencyKey` 完成一次生产提交。
- When 浏览器重复点击或网络重试再次发送相同上下文。
- Then 后端必须返回既有 `feedbackId`、`recordbookEntryId`、`recordbookEventId` 和 `processPoolEventId`，且事件表和数量片段表均只保留一条同源事实。
- Given PQC 员工已用同一 `pqcSubmissionIdempotencyKey` 完成一次过程检验提交。
- When 浏览器重复点击或网络重试再次发送相同 PQC 上下文。
- Then 后端必须返回既有 PQC task，且不得再次更新 PQC task、插入逐件明细、创建 `PQC_INSPECTION` 事件或结构化 PQC 记录。
- Given 生产提交已有正式 `production_submit_event_id` 数量片段且 PQC 合格数量覆盖本次确认数量。
- When 班组长确认 FIFO 分配。
- Then 后端必须先写 process-pool FIFO allocation line，再允许复核、报工分配和工单完成进入终态；若 FIFO 消耗量不足确认数量，则不得写任何终态事实。
- Given 班组长复核或确认携带了签名 ID 和签名员工，但缺少正式签名快照。
- When 后端处理 `reviewSubmission` 或 `confirmSubmission`。
- Then 必须在读取事件、写复核或写分配前 fail-fast，不能保存缺快照的审计签名。
- Given 班组长复核或确认携带了签名 ID、签名员工和非 JSON 文本签名快照。
- When 后端处理 `reviewSubmission` 或 `confirmSubmission`。
- Then 必须在读取事件、写复核或写分配前 fail-fast，不能把普通字符串保存成审计签名快照。
- Given 班组长 FIFO 分配跨多个活跃生产工单且存在已移除工单。
- When 系统构建 process-pool FIFO 消耗命令。
- Then 后端必须只按 `ACTIVE` 工单的 `joinedAt` 顺序传入目标，并以本次确认量作为每个目标的 `requiredQuantity`；若手动分配超过当前工序剩余量或总量不等于提交量，则不得写任何终态事实。
- Given FIFO 分配使订单工序完成且存在正式逐工序批记录绑定和 `PROCESS_POOL_REPORT` 字段映射。
- When 系统自动回填批记录字段。
- Then 字段审计命令必须包含来源值、新值、当前旧值 hash、字段路径、单元格位置和稳定幂等键；重复完工不得再次写字段审计。
- Given 同一生产提交根事件已存在确认分配记录。
- When 班组长重复点击或并发请求再次确认同一 `processPoolEventId`。
- Then 后端必须通过带锁分配重查返回重复确认错误，并且不得触发 PQC、FIFO、复核、分配、订单工序完成或批记录字段审计下游写入。
- Given `processPoolEventId` 的提交、质量、复核、分配、完工和批记录分组都由正式 ID 聚合。
- When 后端生成 `closureEvidence` 收口证据包。
- Then 九个审计问题都必须有业务值、正式 `sourceIds`、同源校验和只读复验入口；缺任一项时 trace 不得 `complete=true`。
- Given 订单工序完成和批记录字段审计存在但完成记录缺 `lastEventId`。
- When 用户按生产提交根事件查询统一闭环 trace。
- Then batchRecord 分组必须返回 `BATCH_RECORD_SOURCE_MISSING`，不得只凭批记录执行和字段审计明细标记完成。
- Given 分配记录或订单工序完成记录指向其它 `processPoolEventId`。
- When 用户按当前生产提交根事件查询统一闭环 trace。
- Then allocation、completion 和 batchRecord 分组必须直接 `BLOCKED`，不得只在最终收口证据里间接失败。
- Given 分配记录或订单工序完成记录属于其它生产工单、路线工序或 MES 工序。
- When 用户按当前生产提交根事件查询统一闭环 trace。
- Then allocation、completion 和 batchRecord 分组必须直接 `BLOCKED`，并返回 scope/source blocker，不得把跨工单或跨工序事实拼成当前闭环。
- Given P0 TDD 计划引用 schema 合同和生产提交闭环合同命名测试类。
- When 验收人员按文档执行 Maven 测试命令。
- Then 引用测试类必须真实存在并可执行，缺类不得被 `surefire.failIfNoSpecifiedTests=false` 假 PASS 掩盖。
- Given 一线员工一次提交要同时形成报工、记录本和工序池事件。
- When 记录本或事件链路在提交过程中失败。
- Then 后端必须在同一事务边界内传播异常并阻止后续工序池事件写入，不能由前端串联接口模拟闭环。

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，替换旧 `never()` 断言后，构造函数缺 `MesProcessPoolEventService` 注入，PQC command builder 缺正式设备/工作站/幂等字段。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，testCompile 失败于缺少 `MesProProcessPoolPqcRecordMapper` 构造器依赖、`selectListByProductionSubmitEventId` 查询方法和 P0 FIFO 质量闸错误码。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 PQC 合格数量覆盖测试后 testCompile 失败于确认服务缺 PQC task/detail mapper 构造器依赖、`selectByIdForUpdate`、`selectListByTaskId` 和 `QUALITY_QUANTITY_MISMATCH` 错误码。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`MesP0FrontlineSubmitIdempotencyTest` 两个用例因 H2 `production_submit_event_id` 为 `NOT NULL` 且 insert 未写该列报 `NULL not allowed for column "production_submit_event_id"`，证明数量片段缺正式生产提交根事件字段落库。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcEventTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 PQC 重复提交幂等用例后 testCompile 失败于缺少 `MesProcessPoolEventService.findExistingPqcInspectionTaskId(MesProcessPoolCreatePqcInspectionReqDTO)`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 P0-T07 FIFO 消耗测试后 testCompile 失败于确认服务缺 `MesProProcessPoolQuantityFragmentMapper` / `MesProcessPoolFifoAllocationService` 构造器依赖，以及 `selectListByProductionSubmitEventIdForUpdate(Long)` 查询方法。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ActiveOrderFifoClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 3, Failures: 1`；新增活跃工单 FIFO 闭环测试后，自动 FIFO target `requiredQuantity` 使用目标计划量而非本次确认量，导致跨工单消费需求被放大。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0BatchRecordBackfillClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，目标测试失败于 `expectedOldValueHash` 为空，证明批记录回填字段审计缺当前旧值校验。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增并发/重复确认测试后 testCompile 失败于缺少 `MesProcessPoolReportAllocationMapper.selectListByEventIdForUpdate(Long)`，证明确认服务尚未用带锁分配状态重查来阻止并发终态重复写入。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionClosureAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，testCompile 失败于缺少 `MesProductionExecutionTraceRespVO.ClosureEvidence`、`EvidenceAnswer`、`SameSourceCheck` 和 `getClosureEvidence()`，证明 trace 尚不能输出九个审计问题的后端收口证据包。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest,MesP0ProductionExecutionTraceBatchRecordSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 3, Failures: 1`；`batchRecordSectionStaysBlockedWhenCompletionLacksSourceEvent` 失败为 expected `BLOCKED` but was `COMPLETE`，证明批记录 trace 会被字段审计明细假完成。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 2, Failures: 1`；`traceSectionsStayBlockedWhenAllocationAndCompletionPointToOtherEvent` 失败为 expected `BLOCKED` but was `COMPLETE`，证明分配和完工来源事件漂移时 trace 分组仍可能被标记完成。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 3, Failures: 1`；`traceSectionsStayBlockedWhenAllocationOrCompletionBelongsToOtherWorkOrderOrProcess` 失败为 expected `BLOCKED` but allocation was `COMPLETE`，证明分配或完工跨工单/跨路线工序/MES 工序时 trace 分组仍可能被标记完成。
- `python -X utf8 -c "<doc Java test reference existence check>"` -> FAIL，`MesP0ProductionExecutionSchemaContractTest` 和 `MesP0ProductionSubmitClosedLoopContractTest` 被 `docs/acceptance/production-execution-main-loop/tdd-plan.md` 引用但真实测试类不存在。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 8, Failures: 2`；空 `reviewSignatureSnapshotJson` 场景返回 `PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS` 而非入口上下文错误，证明服务会继续读取事件且未在签名前置校验处 fail-fast。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 10, Failures: 2`；非 JSON `reviewSignatureSnapshotJson` 场景返回 `PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS` 而非入口上下文错误，证明服务只判空、不判 JSON 结构且会继续读取事件。

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcEventTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，P0-T07 后 `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ActiveOrderFifoClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0BatchRecordBackfillClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，并发/重复确认边界 GREEN 后 `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionClosureAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest,MesP0ProductionExecutionTraceBatchRecordSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，scope mismatch GREEN 后 `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolSchemaTest,MesP0ProductionExecutionSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionSubmitClosedLoopContractTest#shouldCreateFeedbackRecordbookAndProcessPoolEventInOneTransaction" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `python -X utf8 -c "<doc Java test reference existence check>"` -> PASS，`ALL_REFERENCED_TESTS_EXIST`，28 个文档 Java 测试引用均已落地。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

## Verification

- 定向单测捕获 `MesProcessPoolCreatePqcInspectionReqDTO`，验证 `workOrderId`、`routeId`、`routeProcessId`、`processId`、`actualEmployeeId`、`deviceAccountId`、`deviceId`、`workstationId`、`templateType`、`feedbackSourceType/sourceId`、`recordbookSourceType/sourceId`、`inspectionResult`、`clientSubmitTime`、`signatureId`、`signatureUserId`、`signatureSnapshot`。
- M4 定向单测覆盖 PQC 子事件不能作为确认根事件、缺结构化绑定、PQC `FAILURE` 结果阻塞、PQC `SUCCESS` 但合格数量不足阻塞，以及 `SUCCESS` 且合格数量覆盖时放行，并断言失败时不写复核、分配、工单完成。
- 邻接回归 `MesTeamLeaderReportConfirmationServiceTest,MesP0TeamLeaderReviewSignatureServiceTest,MesP0PqcQualityAllocationGateTest,MesP0ProductionExecutionTraceQualityBindingTest,MesTeamLeaderTraceServiceTest` -> PASS，`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。
- 主提交幂等定向单测覆盖重复同幂等键只返回同一 `processPoolEventId`、只保留一条事件和一条数量片段、查重方法在前置写报工前返回既有 `feedbackId` / `recordbookEntryId` / `recordbookEventId` / `processPoolEventId`，以及缺幂等键 fail-fast。
- P0 相邻回归 `MesProcessPoolEventServiceTest,MesProcessPoolTimeSignatureTest,MesProcessPoolPqcEventTest,MesFrontlinePqcContextServiceTest,MesP0PqcQualityAllocationGateTest,MesP0ProductionExecutionTraceServiceTest,MesTeamLeaderTraceServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesP0FrontlineSubmitIdempotencyTest` -> PASS，`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`。
- PQC 重复提交幂等定向单测覆盖重复同 `pqcSubmissionIdempotencyKey` 只返回同一 `PQC_INSPECTION` 事件和既有 PQC task，事件表、结构化 PQC 记录、PQC task update 与逐件明细 insert 均不重复。
- P0-T04 相邻回归 `MesProcessPoolPqcEventTest,MesFrontlinePqcContextServiceTest,MesP0PqcQualityAllocationGateTest,MesP0FrontlineSubmitIdempotencyTest,MesProcessPoolEventServiceTest,MesProcessPoolTimeSignatureTest,MesTeamLeaderTraceServiceTest` -> PASS，`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。
- P0-T07 定向单测覆盖 FIFO 确认从 `production_submit_event_id` 来源数量片段构建 `MesProcessPoolFifoAllocationCommand`，校验 `sourceQuantityFragmentId`、`sourceEventId`、目标工单、目标工单编码和确认数量，并覆盖 FIFO 消耗返回数量不足时不写复核、报工分配或工单完成。
- P0-T07 活跃工单 FIFO 定向单测覆盖自动 FIFO 剔除非 `ACTIVE` 工单、按 `joinedAt` 顺序选择工单、传给 process-pool FIFO target 的 `requiredQuantity` 等于本次确认量，以及手动分配剩余量不足和总量不匹配在终态写入前 fail-fast。
- P0-T07 相邻回归 `MesP0ActiveOrderFifoClosedLoopTest,MesP0PqcQualityAllocationGateTest,MesTeamLeaderReportConfirmationServiceTest,MesP0TeamLeaderReviewSignatureServiceTest,MesProcessPoolFifoAllocationServiceTest,MesProcessPoolPqcEventTest,MesFrontlinePqcContextServiceTest,MesP0FrontlineSubmitIdempotencyTest,MesProcessPoolEventServiceTest,MesProcessPoolTimeSignatureTest,MesTeamLeaderTraceServiceTest` -> PASS，`Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`。
- P0-T08 定向单测覆盖批记录回填字段审计 change 的 `expectedOldValueHash`、来源新值、字段路径、单元格行列和 `PROCESS_POOL_REPORT_BACKFILL` 幂等键。
- P0-T08 相邻回归 `MesP0BatchRecordBackfillClosedLoopTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesP0ProductionExecutionTraceBatchRecordSourceTest,MesP0ProductionExecutionTraceServiceTest,MesTeamLeaderTraceServiceTest,MesP0PqcQualityAllocationGateTest` -> PASS，`Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`。
- 并发/重复确认定向单测覆盖既有分配命中时必须通过 `selectListByEventIdForUpdate` 返回重复确认错误，且不得调用 `pqcRecordMapper.selectListByProductionSubmitEventId`、`quantityFragmentMapper.selectListByProductionSubmitEventIdForUpdate`、`processPoolFifoAllocationService.allocate`、`reviewMapper.insert`、`allocationMapper.insertBatch` 或 `orderProcessCompletionService.applyConfirmedAllocations`。
- 并发确认相邻回归 `MesP0PqcQualityAllocationGateTest,MesTeamLeaderReportConfirmationServiceTest,MesP0TeamLeaderReviewSignatureServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesP0BatchRecordBackfillClosedLoopTest,MesTeamLeaderBatchRecordBackfillServiceTest` -> PASS，`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。
- P0-T13 定向单测覆盖 `closureEvidence` 九项答案：`who/device/process/quantity/quality/signature/workOrder/review/batchRecord` 均有业务值、正式 `sourceIds`、`sameSource=true` 和只读复验入口；缺少数量正式来源时返回 `CLOSURE_EVIDENCE_MISSING_SOURCE` 且 trace `complete=false`。
- P0-T10 定向单测覆盖订单工序完成缺 `lastEventId` 时，批记录分组即使有正式执行和字段审计明细也保持 `BLOCKED`，blocker code 为 `BATCH_RECORD_SOURCE_MISSING`。
- P0-T10 同源校验定向单测覆盖分配记录 `eventId` 和完工记录 `lastEventId` 指向其它事件时，allocation、completion 和 batchRecord 三个分组都保持 `BLOCKED`，避免把跨事件事实拼成当前生产提交闭环。
- P0-T10 scope 校验定向单测覆盖分配记录和订单工序完成记录属于其它生产工单、路线工序或 MES 工序时，allocation 返回 `ALLOCATION_SCOPE_MISMATCH`、completion 返回 `COMPLETION_SCOPE_MISMATCH`，batchRecord 保持 `BATCH_RECORD_SOURCE_MISSING`。
- P0-T10 / P0-T13 trace 相邻回归 `MesP0ProductionExecutionTraceFailureTest,MesP0ProductionExecutionTraceBatchRecordSourceTest,MesP0ProductionExecutionTraceServiceTest,MesP0ProductionExecutionTraceQualityBindingTest,MesP0ProductionExecutionTraceReviewGateTest,MesP0ProductionExecutionClosureAuditTest,MesTeamLeaderTraceServiceTest,MesProcessPoolTeamLeaderControllerTest` -> PASS，`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。
- P0 后端综合回归 `MesP0ProductionExecutionSchemaContractTest,MesP0ProductionSubmitClosedLoopContractTest,MesP0FrontlineSubmitIdempotencyTest,MesP0TeamLeaderReviewSignatureSchemaTest,MesP0TeamLeaderReviewSignatureServiceTest,MesP0PqcQualityAllocationGateTest,MesP0ActiveOrderFifoClosedLoopTest,MesP0BatchRecordBackfillClosedLoopTest,MesP0ProductionExecutionClosureAuditTest,MesP0ProductionExecutionTraceFailureTest,MesP0ProductionExecutionTraceBatchRecordSourceTest,MesP0ProductionExecutionTraceQualityBindingTest,MesP0ProductionExecutionTraceReviewGateTest,MesP0ProductionExecutionTraceServiceTest,MesFrontlinePqcContextServiceTest,MesProcessPoolPqcEventTest` -> PASS，`Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`。
- P0-T00A schema 合同单测覆盖生产提交、PQC、复核、数量片段、工序池事件、提交响应和迁移索引/backfill blocker 的正式字段，不允许把 VO/DTO 或 rawPayload-only 当作持久化闭环证据。
- P0-T02 生产提交闭环合同单测覆盖同一次提交返回 `feedbackId`、`recordbookEntryId`、`recordbookEventId`、`processPoolEventId`，并通过 `@Transactional(rollbackFor = Exception.class)` 和记录本失败传播用例证明后端事务边界存在。
- P0-T02 相邻回归 `MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackSubmitServiceTest,MesP0FrontlineSubmitIdempotencyTest` -> PASS，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- M2 复核签名快照边界定向单测覆盖 `reviewSubmission` 和 `confirmSubmission` 的空签名快照 fail-fast，以及非 JSON 文本签名快照 fail-fast；相邻回归 `MesP0TeamLeaderReviewSignatureSchemaTest,MesP0TeamLeaderReviewSignatureServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesProcessPoolTeamLeaderControllerTest` -> PASS，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- 新增数据库 schema：`20260803_mes_process_pool_quantity_fragment_submit_root.sql` 补齐数量片段正式生产提交根事件字段和索引，全量 release migration policy gate PASS，`migrationCount=419`。

## Blockers

- 真实 E2E PASS 仍待后续里程碑实现；后端 P0-T13 收口证据包已 GREEN。
- 当前证据已证明 PQC 重复提交唯一性、P0-T07 FIFO 消耗持久化、P0-T08 批记录回填字段审计、并发/重复确认边界和 P0-T13 后端收口证据包；真实 E2E 仍需后续专门覆盖。
