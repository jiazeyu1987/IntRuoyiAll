# Development Plan

## 1. 目标状态机

SUBMITTED -> REVIEW_PENDING -> REVIEWED 或 REJECTED；REVIEWED -> ALLOCATED；达到工序目标只更新 PROCESS_TARGET_REACHED 投影。流程2不得调用 completeAndBackfill、发布完成事件、回填 receipt、调用流程6、创建批次执行、上传四份材料或最终放行。驳回后的重提必须使用新 submissionVersion，旧事实不可修改。

## 2. 当前代码根因

基线中 `MesTeamLeaderOrderProcessCompletionService.applyConfirmedAllocations` 通过布尔参数允许达到目标时进入 `completeAndBackfill`；`MesProFrontlineFeedbackSubmitServiceImpl.submit` 通过 `recordbookEntryService.createOriginalEntry` 直接创建正式记录簿来源。当前 worktree 已移除这两条流程2越界路径：提交只写显式 `MesProFrontlineRecordbookSourceSnapshot` 与 `activeOrderProcess` 来源快照；正式记录簿写入 service 和 entry 类型已从流程2移除；初始分配只写分配事实，不读取工序目标或推进完成投影；组长确认后的分配才更新来源事实和进度投影。

## 3. 实现边界

提交、复核、分配各自在本阶段事务内原子写事实；失败不得伪造成功。达到目标时只更新来源追溯和进度投影；活跃订单完成命令由流程4单独实现，并消费流程2正式事件和流程3 PQC 正式确认/汇集合同，重新校验双100%、工单和领料绑定后才三类回填。流程6仅消费流程4回填成功 receipt，在独立事务中创建或复用批次。

## 4. 跨线程事件合同

流程2只发布 `ProductionSubmissionFactRecorded`、`ProductionSubmissionReviewed`、`ProductionSubmissionRejected`、`ProductionAllocationConfirmed`。事件必须携带正式 ID：`productionFactEventId`、`reviewEventId`、`allocationEventId`、`submissionVersion`、`reviewVersion`、`allocationVersion`、`payloadHash`、`signatureSnapshot`、`activeOrderId`、`workOrderId`、`pickListBindingId`、`routeVersionId`，以及 tenantId、routeProcessId、processId、occurredAt、idempotencyKey、sourceEventId、statusOwner。流程4/6/7 按正式事件 ID 消费，不反查当前投影或最新提交。

流程4只在完成命令中消费流程2事件与流程3 PQC 合同并原子回填批记录、过程检验单、按需损耗单；流程6建批；流程7做生产/工单/领料/PQC/损耗映射和放行追溯；流程8在建批后上传四份正式材料，齐套后才允许流程10最终放行；流程9负责多入口前置合同；流程10负责最终放行状态与管理者代表签名；流程11负责总体验证、迁移和回归。

## 5. 失败、并发、幂等

同幂等键同 payload 返回原回执，不同 payload 返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`；复核/分配版本冲突返回结构化 blocker。超量只能按显式策略拒绝或保留差异。流程4回填失败整批回滚且不发布成功 receipt；流程6建批失败不得伪造回填状态，由 receipt/失败重试合同承接。

## 6. BDD/TDD 计划

- BDD: 达到目标的组长复核 -> Given 已复核来源数量达到目标 When 提交复核确认 Then 只写复核事实和进度投影且不得新增或修改本次三类回填和批次执行。
- BDD: 达到目标的分配 -> Given REVIEWED 来源 When 确认分配 Then 写分配事实并标记工序目标投影，不等于活跃订单完成。
- BDD: 驳回重提、超量、并发、幂等 -> Given 版本/载荷冲突 When 重复请求 Then 返回结构化 blocker，不覆盖旧事实。
- RED/GREEN/REGRESSION: 历史隔离验证曾编译 2785 个主源码并通过 74 项流程2回归；追加修复后完整 reactor 编译通过（2783 个主源码、488 个测试源码），流程2及相邻 QA 测试共 108 项通过。完整 reactor test 的无关 infra 运行时失败见 `execution-log.md`。

## 7. 迁移与回滚

本任务不改数据库。若现存正式批记录由流程2提前写入，须由流程4/11另行盘点和批准修复；代码回滚只能回到不触发提前回填的已验证版本，不恢复越界行为。
