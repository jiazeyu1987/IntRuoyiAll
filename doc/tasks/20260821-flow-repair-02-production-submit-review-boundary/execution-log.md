# Execution Log

## Scope

独立 worktree：`D:/IntRuoyiWorktree/20260822-flow-repair-02-design-development`。不启动服务、不改数据库、不运行写入型 E2E。

## BDD

BDD: 复核/分配达到工序目标 -> Given 来源事实有效且数量达到目标 When 复核或分配请求成功 Then 只写本阶段事实和进度投影，不得新增或修改本次三类回填和批次执行。

## Implementation Evidence

- `MesTeamLeaderOrderProcessCompletionService` no longer owns or injects `MesTeamLeaderBatchRecordBackfillService`; target reach only writes the process completion projection and source trace.
- Allocation reconciliation retains the existing explicit overage policy: confirmed allocation may preserve adjustable overage, while correction reconciliation rejects overage before progress or completion writes.
- `MesProFrontlineFeedbackSubmitServiceImpl` no longer depends on or calls `createOriginalEntry`; the splitter records `recordbookSourceSnapshot` in the process-pool raw payload for the later flow4 completion command.
- Frontline initial allocation now persists only the allocation fact from the submitted event context; it no longer resolves an order-process target or calls the completion projection service before leader confirmation.
- The process-pool raw payload now retains both `recordbookSourceSnapshot` and `activeOrderProcess` formal source snapshots.

## 编译阻断修复（追加范围）

BDD: 完整 reactor 编译 -> Given ERP 合同测试和 MES QA 正式 DTO/测试必须与当前生产代码一致 When 执行 reactor `test-compile` Then 所有 MES 主源码和测试源码应编译成功，且不以排除源码或临时 POM 绕过。

- 根因一：`ErpKingdeeTableAutoSyncContractTest` 在 `assertContains` 仅接受两个参数时传入三个参数；修正为断言金蝶 job 的正式 `executeAutoForCurrentTenant()` 入口。
- 根因二：`MesQaInspectionRegulationPublishedVersionRespVO` 和 `MesQaInspectionRegulationSaveReqVO.InspectionItem` 缺少当前服务/测试依赖的 `equipmentRequired`、`equipmentOptions` 及 `EquipmentOption`；恢复正式字段与校验类型。
- 根因三：`MesQaInspectionRegulationWordImportServiceTest.existingConfiguration` 删除了本地 `process` 构造却仍引用该变量；恢复完整来源快照 fixture。
- RED: `mvn -pl yudao-module-erp -am '-DskipTests' test-compile` -> FAIL，ERP 合同测试第 144 行参数数量不匹配。
- RED: `mvn -pl yudao-module-mes -am '-Dmaven.test.skip=true' install` -> FAIL，MES `MesFrontlinePqcContextServiceImpl:736` 找不到 `EquipmentOption`。
- RED: `mvn -pl yudao-module-mes -am '-DskipTests' test-compile` -> FAIL，QA 保存请求缺少 `setEquipmentRequired`，Word 导入测试缺少 `process`。
- GREEN: 同两条编译命令修复后均 PASS；MES 主源码编译 2786 个、测试源码编译 488 个。
- REGRESSION: 流程2及相邻 QA 测试命令 PASS，108 项测试，Failures=0、Errors=0、Skipped=0。

## RED/GREEN/REGRESSION

- Historical RED: the initial reactor run was blocked by the three compile issues recorded in “编译阻断修复（追加范围）”; all three are now resolved.
- GREEN: temporary isolated verification POM (excluding only unrelated baseline `MesFrontlinePqcContextServiceImpl.java`) compiled 2785 MES main sources and ran the flow2 target suite -> PASS, 37 tests, 0 failures/errors.
- REGRESSION: the same isolated harness expanded to submission review/rejection, report confirmation, allocation version/idempotency/concurrency/overage, initial-allocation boundary and progress projection -> PASS, 74 tests, 0 failures/errors/skips.
- Full reactor `test`: reached runtime tests but stopped in unrelated `yudao-module-infra` with 3 failures and 1 error (`RuntimeControlLocalConfigContractTest`, `RuntimeIncidentServiceImplTest`, `RuntimeOpsGuideServiceImplTest`, `RuntimeOpsResponsibilityServiceImplTest`); no MES test failure was observed in that run.
- 文档/结构检查：PASS（task/development-plan/test-plan/execution-log/verification-report 结构和跨线程关键词已静态核对；backend-api-evidence 校验 PASS）。
- 生产边界静态检查：PASS（提交服务无正式记录簿创建调用；工序完成服务无 completeAndBackfill、批记录回填服务或回填服务调用）。

## 跨线程事实

流程2只发布四类生产事实事件；流程4消费流程2正式事件和流程3 PQC 正式合同后完成回填；流程6建批；流程7映射追溯；流程8上传来料检报告、灭菌报告、成品检报告、成品检记录；流程9前置合同；流程10最终放行与管理者代表签名；流程11验证/迁移/回归。

必须按正式 ID 消费字段：productionFactEventId、reviewEventId、allocationEventId、submissionVersion、reviewVersion、allocationVersion、payloadHash、signatureSnapshot、activeOrderId、workOrderId、pickListBindingId、routeVersionId。

## 来源快照职责修复（本轮追加）

- BDD: 来源快照与正式批记录隔离 -> Given 一线提交携带记录簿输入 When 流程2拆分生产事实 Then 只生成 `MesProFrontlineRecordbookSourceSnapshot`，不得暴露正式记录簿写入 service。
- RED: 历史代码存在 `MesProFrontlineRecordbookEntryService`/`createOriginalEntry` 及 entry payload/result 类型，职责无法从类型层阻止误用。
- GREEN: 删除未使用的正式记录簿写入 service 与 entry 类型，split payload 改为显式 `recordbookSourceSnapshot`；静态引用检查无残留。
- REGRESSION: `mvn -pl :yudao-module-mes '-Dtest=...' '-Dsurefire.failIfNoSpecifiedTests=false' test` PASS，108 项通过；`mvn -pl :yudao-module-mes -am '-DskipTests' test-compile` PASS。

## Blockers

- 流程2目标与相邻单元回归已验证；运行态、数据库迁移和写入型 E2E 未运行，符合本任务限制。
- Maven compile gates are clear. The full reactor test remains red only on unrelated infra runtime tests listed above; the 108-item flow2/adjacent target suite passes independently.
- 剩余职责修复：删除流程2未使用的正式记录簿写入 service 与 entry payload/result 类型，新增显式 `MesProFrontlineRecordbookSourceSnapshot`；来源快照不可被误用为正式批记录。
- 跨线程事件字段和流程3 PQC 合同仍需由邻接线程冻结。
- 提交/融合门禁未通过：`scripts\preflight\branch-runtime-port-guard.ps1` 要求当前 worktree 先注册端口；官方注册脚本因既有并行登记 `D:\IntRuoyiWorktree\20260820-pqc-inspection-equipment-selection` 使用越界活动 `slot=31` 而拒绝执行。不得删除、改写或绕过该其它任务登记；因此本轮未提交、未融合、未生成 commit hash。
