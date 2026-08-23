# 流程修复 4 验证报告

## 验证范围

本报告只验证本任务的文档设计和只读审计结论。未修改生产代码、数据库或运行环境；未启动服务，未运行构建和写入型 E2E。

## 代码符合性结论

**不符合目标态，需实施修复。**

现有实现将批记录回填放在工序完成阶段，并在申请放行阶段要求每工序回填成功；它没有以活跃订单完成为唯一状态所有者。目标边界已修订为 Tx-A 在双100完成节点原子执行三类回填并一次写入不可变 completionBackfillReceipt，返回 provisionHandoff=PENDING_FLOW6；receipt 不持有 batchExecutionId 或流程6批次状态。流程6（含流程9合法入口）在后继 Tx-B 创建/复用批次，独占 BATCH_PROVISIONING、BATCH_PROVISIONING_RETRYABLE、BATCH_PROVISIONING_BLOCKED、BATCH_READY 和 batchExecutionId；Tx-A 与 Tx-B 不宣称同一数据库事务。现有申请级幂等不足以覆盖完成 receipt 和后继建批重试。

## 已验证证据

- 运营规则要求：生产和 PQC 提交/复核只形成正式来源；仅双100点击完成才统一回填三类资料，成功后建批。
- 后端正式来源门禁要求：逐工序 BATCH 绑定、已确认 PQC 汇集明细及设备快照、工单和唯一审核领料单、损耗事实、同节点事务/幂等/版本、四份材料门禁和历史执行迁移阻断。损耗合同固定为逐工序 REQUIRED|NO_LOSS|BLOCKED，订单/工序级 hasActualLoss、lossQuantity，receipt lossReportStatus=SUCCESS|NOT_REQUIRED；正损耗必须有 lossRecordId， 无损耗必须有正式零损耗确认快照，BLOCKED 或缺失来源不得生成成功 receipt。
- 源码审计要求：当前放行申请服务检查工序 `COMPLETED + BACKFILL_SUCCESS`；工序完成服务提前调用批记录回填。
- 跨流程结论：修复 4 负责完成节点、Tx-A 三类回填和不可变 receipt；修复 2、3 提供生产/PQC来源事实；修复 5 负责条件损耗；修复 6（含修复9合法入口）消费 receipt 在 Tx-B 创建/复用批次并持有 BATCH_* 状态；建批后由修复 7 执行 pre-release Origin/TraceLink 映射并校验工单、正式领料、批记录、过程检验、适用损耗或 NO_LOSS 的 hash/version；修复 8 负责四份材料上传、校验并返回 MATERIALS_READY；修复 10 负责唯一最终放行状态、角色和审计；修复 7 负责 post-release 完整追溯；修复 11 负责 BDD/TDD、回归和迁移总门禁。

## 文档结构验证

已完成 UTF-8 读取及必需文件存在性检查：DOCUMENT_CHECK=PASS。五份必需文档均存在，且覆盖目标态、当前代码事实、根因、修改边界、API/数据/状态、BDD、TDD 计划、blocker、迁移/回滚和跨流程契约。

## 未运行验证

| 项目 | 状态 | 原因 |
| --- | --- | --- |
| 后端测试 | NOT_RUN | 本任务仅文档设计，未改代码 |
| 前端测试 | NOT_RUN | 本任务仅文档设计，未改前端 |
| 构建 | NOT_RUN | 用户明确不启动服务或运行实现验证 |
| Playwright 写入型 E2E | NOT_RUN | 用户明确禁止；且真实前置尚未齐备 |

## 实施 blocker（合同已冻结）

1. 流程5、6/9、7、8、10、11 的职责、字段、状态和接口合同已在任务文档中冻结；生产代码尚未实现，尚无实现级测试、真实数据或迁移证据。
2. 流程6 Tx-B receipt 消费、createOrReuse 幂等、BATCH_* 状态、provision relation 和 batchExecutionId 尚未实现或取得测试证据；流程4不得写入这些状态。
3. 流程5损耗字段和零损耗快照、流程7 pre-release hash/version 校验、流程8四材料硬门禁/MATERIALS_READY、流程10唯一放行、流程7 post-release 追溯尚无实现证据。
4. 流程11 BDD/TDD、回归和迁移总门禁尚未运行；真实任务自有数据、签名账号、四份材料及共享 schema 迁移证据缺失，本任务不启动服务或执行写入型验证。
5. 传统过程检验报表绑定和正式 E2E 任务自有数据仍需实施阶段按冻结合同提供，不得用 mock 或历史未关联批次替代。

## 结论

当前设计已按复核意见明确职责、Tx-A/Tx-B 边界、正式目标、实施边界和验收方法；合同已冻结，但生产实现、测试、真实数据和迁移证据尚未完成，不应据此放行生产代码。任务文档状态为 ready_for_closeout。



## 文档验证补记

已完成 UTF-8 读取及必需文件存在性检查：DOCUMENT_CHECK=PASS。实现级测试仍为 NOT_RUN；文档结构验证已通过，尚未取得生产实现、真实数据或迁移证据。任务文档状态为 ready_for_closeout。
