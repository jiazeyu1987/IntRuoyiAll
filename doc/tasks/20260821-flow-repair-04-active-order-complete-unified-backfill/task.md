# 流程修复 4：活跃订单完成统一回填

## Current Status

ready_for_closeout。已按总流程复核修订职责、事务边界、材料门禁和测试计划；未修改生产代码、数据库或运行环境，未执行写入型 E2E。待主线程完成最终只读一致性检查和收尾清理。

## 任务目标

将活跃订单的正式完成节点定义为唯一的三类资料回填节点：生产组长仅在活跃订单生产进度、检验进度均为 100% 后点击“完成”。流程4在 Tx-A 中重新核验正式来源，原子回填批记录、过程检验单和条件损耗资料，并生成不可变完成/回填 receipt。receipt 提交后交给流程6（含流程9合法入口）创建或复用批次执行；这是后继 Tx-B，不与 Tx-A 合并。流程6独占批次执行状态和 batchExecutionId。

批次执行创建/复用后，流程7先执行 pre-release Origin/TraceLink 映射并校验来源 hash/version；随后流程8负责上传并硬门禁四份材料：来料检报告、灭菌报告、成品检报告、成品检记录，返回 `MATERIALS_READY`。流程10是唯一最终放行者，流程7负责 post-release 追溯。

## 目标态

1. 生产组长加入活跃订单时，订单已正式绑定生产工单和对应领料单。
2. 一线生产和一线 PQC 只提交签名事实；生产组长和 PQC 组长只复核来源事实。上述动作不得物化三类最终资料。
3. 生产组长点击完成时，服务端锁定活跃订单并以正式来源重新计算生产进度、检验进度；任一不足 100% 均失败。
4. 双 100% 后，Tx-A 在同一“点击完成”业务节点原子写入批记录、过程检验单、条件损耗回填；逐工序损耗状态为 `REQUIRED|NO_LOSS|BLOCKED`。无损耗必须有正式零损耗确认快照，`hasActualLoss=false`、`lossQuantity=0`、receipt 的 `lossReportStatus=NOT_REQUIRED`，不创建损耗单或零损耗报告。
5. Tx-A 提交后生成不可变 `completionBackfillReceipt`，不写入流程6的批次状态或 `batchExecutionId`；receipt 交给流程6（流程9合法入口也只能进入流程6）。流程6在后继 Tx-B 中创建或复用批次执行，独占 `BATCH_PROVISIONING*`、`BATCH_READY` 和 `batchExecutionId`。Tx-B 失败保留成功 receipt，进入流程6可重试或 blocker，不重复三类回填。
6. 流程6建批后，流程7先完成 pre-release Origin/TraceLink 映射并校验生产工单、正式领料单、批记录、过程检验、适用损耗或 `NO_LOSS` 事实的来源 hash/version；流程8随后上传四份材料并返回 `MATERIALS_READY`，流程10作为唯一最终放行者执行放行，流程7执行 post-release 追溯。
7. 同一完成幂等键和来源哈希重试返回原 receipt；不同载荷/来源哈希冲突。流程6按 receipt 幂等创建/复用，不得重新触发 Tx-A。

## 当前代码事实

- 当前可见正式入口是 `POST /active-order/release/apply`；没有“活跃订单双 100% 完成”领域命令。`MesProcessPoolTeamLeaderController` 的完成操作为模拟路径，申请放行入口约在 396-412 行。
- `MesTeamLeaderActiveOrderReleaseGenerationService` 在申请阶段检查生产/检验完工、申请幂等和各冻结工序 `COMPLETED + BACKFILL_SUCCESS`，然后创建 `PQC_RELEASE_PENDING` 申请。
- `MesTeamLeaderOrderProcessCompletionService` 在单个工序满足条件时调用 `completeAndBackfill`；`MesTeamLeaderBatchRecordBackfillServiceImpl` 以独立事务回填批记录。这会在订单完成前物化最终资料。
- 进度读取在 `MesTeamLeaderActiveOrderServiceImpl` 中按工序完成记录汇总；未发现锁定订单、双 100% 权威校验及统一回填建批的订单级命令。
- 现有申请有请求和业务幂等，但没有涵盖“三类回填、建批/复用、状态推进”的订单完成级幂等回执。

## 根因

当前实现将“工序完成”“资料回填”“申请放行”混为多个阶段：工序完成服务提前回填，申请服务把回填结果作为前置条件。该分段无法保证订单双 100% 后的一次性资料物化、全链路回滚与批次执行创建，也与正式运营规则中“完成节点统一回填、回填后建批”的状态所有者要求冲突。

## 修改边界

### 纳入后续实现

- 新增或替换为正式的订单完成命令、状态机、版本校验、幂等回执和结构化 blocker。
- 将逐工序提前最终回填改为只形成完成候选/来源事实；最终资料只允许订单完成事务物化。
- Tx-A 只负责三类回填和不可变完成/回填 receipt；receipt 成功后交给流程修复 6，在后继 Tx-B 创建或复用批次执行并建立来源映射。
- 不实现批次执行四份材料上传、最终放行、流程7 pre-release/post-release 映射和追溯；分别由流程修复 8、10、7 负责。
- 适配活跃订单页面的双 100% 完成按钮、确认、刷新和 blocker 展示。
- 冻结 Tx-A 单元/集成合同测试和数据完整性约束；迁移预检、回归及真实路径 E2E 由流程修复 11 统筹。

### 明确不纳入本任务的实现

- 不重写一线生产、一线 PQC 或两个组长复核的提交业务；它们的职责由流程修复 2、3 的上游来源契约维护。
- 不实现批次执行四份材料上传和硬门禁；由流程修复 8 负责。
- 不实现最终放行状态、放行角色和放行审计；由流程修复 10 负责。
- 不实现批次建成后的 Origin/TraceLink pre-release 映射、完整批次映射和放行后追溯；由流程修复 7 负责。
- 不承担流程修复 11 的 BDD/TDD、回归和迁移总门禁。
- 不自动修复、删除或认领历史不完整批次执行；此类数据必须先通过迁移预检并取得已批准的数据修复方案。

## 里程碑

1. 完成现有入口、回填、申请和建批链路审计，并冻结术语与状态所有者。
2. 冻结完成命令、三类正式来源、事务、幂等、数据模型和跨流程接口。
3. 按严格 TDD 实现后端与前端，不允许完成前回填或建批。
4. 完成迁移预检、合同回归和任务自有真实路径验证。

## 预期验证

- 文档阶段：UTF-8 读取、五份必需文档存在、设计覆盖目标态、当前事实、根因、接口/数据/状态、BDD、RED/GREEN/REGRESSION、blocker、迁移/回滚及跨流程契约。
- 实现阶段：双 100% 成功、进度不足拒绝、正式来源缺失拒绝、Tx-A 任一回填失败整体回滚、Tx-B 失败保留不可变 receipt 且由流程6重试、同键重试稳定返回、完成前无最终资料/批次执行、有损耗与无损耗分支、BLOCKED 不生成 receipt/不驱动流程6、流程7 pre-release 映射、流程8 MATERIALS_READY、流程10唯一放行、流程7 post-release追溯及流程11总门禁。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。缺正式来源、版本、权限、关系、持久化或下游建批能力均 fail fast。
- 是否从根因和长期维护角度解决：是。由订单完成命令成为唯一状态所有者，移除“逐工序提前回填 + 申请阶段拼装”的隐式分段。
- 是否存在临时补丁或绕过：否。历史数据不通过临时关联或默认值绕过迁移门禁。

## 适用经验门禁

已读取 `docs/experience-index.md`。匹配 `活跃订单申请放行资料必须只使用正式来源` 门禁：只能使用冻结路线、逐工序 BATCH 绑定、正式生产/PQC/领料/损耗事实；禁用 `formBindings`、默认 `MAIN`、工序开始配置、前端拼装、空资料和 API-only 替代。

## Cleanup Keep

- `doc/tasks/20260821-flow-repair-04-active-order-complete-unified-backfill/development-plan.md`
- `doc/tasks/20260821-flow-repair-04-active-order-complete-unified-backfill/test-plan.md`
