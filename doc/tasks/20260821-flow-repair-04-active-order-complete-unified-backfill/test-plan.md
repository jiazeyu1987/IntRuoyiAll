# 流程修复 4 测试计划

## 范围

本计划用于后续严格 TDD 实施。当前任务不运行构建、服务或写入型 E2E；下面的 RED/GREEN/REGRESSION 是实施门槛，不是已经取得的测试结果。

## 测试数据前置

- 任务自有活跃订单，已绑定正式生产工单和唯一审核领料单。
- 冻结发布路线、逐工序 BATCH 批记录绑定和正式批记录模板完整。
- 一线生产签名提交和生产组长复核事实完整；一线 PQC 签名提交、PQC 组长确认和汇集设备快照完整。
- 准备有损耗、无损耗和 BLOCKED 三个互不共用的订单样本；有损耗样本含 hasActualLoss=true、lossQuantity>0、lossRecordId，无损耗样本含正式零损耗确认快照。
- 准备可诱发三类回填失败的 Tx-A 隔离配置，以及流程6 Tx-B 建批失败/重试配置，确保事务不会污染非任务数据。
- 流程7提供建批后的 pre-release Origin/TraceLink 映射、来源 hash/version 校验和放行后的 post-release 追溯样本；完整的来料检报告、灭菌报告、成品检报告、成品检记录样本由流程8提供；管理者代表权限和放行审计由流程10提供；总门禁由流程11提供。

缺任一正式前置即 BLOCKED，不能以 mock、SQL、API-only 或其它订单资料替代。

## BDD 场景

1. `BDD: 双100完成统一回填 -> Given` 当前组长负责的活跃订单生产和检验进度均由正式来源计算为 100%，三类来源完整；`When` 组长点击完成；`Then` Tx-A 原子回填三类资料并一次写入不可变 `completionBackfillReceipt`，返回 `provisionHandoff=PENDING_FLOW6`；receipt 不含 `batchExecutionId`，由流程6后继 Tx-B 消费。
2. `BDD: 完成前禁止物化 -> Given` 任一进度不足 100%；`When` 提交完成或工序/组长复核；`Then` 无最终批记录、过程检验单、损耗单和批次执行被写入。
3. `BDD: 正式批记录来源 -> Given` 某冻结工序缺逐工序 BATCH 绑定或仅有 `formBindings`；`When` 提交完成；`Then` 返回正式来源 blocker，零写入。
4. `BDD: 正式PQC来源 -> Given` PQC 明细未被组长确认或缺结构化设备快照；`When` 提交完成；`Then` 返回 blocker，零写入。
5. `BDD: 领料来源唯一 -> Given` 领料单与工单/路线/物料不正式对应、未审核或多单不唯一；`When` 提交完成；`Then` 回填前失败，零写入。
6. `BDD: 有损耗 -> Given` 流程5给出逐工序 `REQUIRED`，正式来源确认 `hasActualLoss=true`、`lossQuantity>0` 且存在 `lossRecordId`；`When` 完成；`Then` Tx-A 生成一次正式损耗单，receipt 保存 `lossReportStatus=SUCCESS` 及来源事实。
7. `BDD: 无损耗 -> Given` 流程5给出逐工序 `NO_LOSS` 且存在正式零损耗确认快照；`When` 完成；`Then` receipt 保存 `hasActualLoss=false`、`lossQuantity=0`、`lossReportStatus=NOT_REQUIRED`，不生成损耗单或零损耗报告。
8. `BDD: 损耗阻塞禁止完成 -> Given` 任一工序为 `BLOCKED`，或缺失 `lossRecordId` 但未取得正式零损耗快照；`When` 提交完成；`Then` 不生成成功 receipt、不驱动流程6，Tx-A 回滚并返回 blocker。
9. `BDD: Tx-A原子回滚 -> Given` 双100和来源完整，但批记录、过程检验或实际损耗任一步被隔离配置为失败；`When` 提交完成；`Then` 三类回填、receipt 和订单状态全部回滚，流程6不会被触发。
10. `BDD: Tx-B失败保留receipt -> Given` Tx-A 已成功并生成不可变 receipt，但流程6创建/复用批次执行失败；`When` 完成编排或重试调用 Tx-B；`Then` 流程6独占 `BATCH_PROVISIONING_RETRYABLE` 或 `BATCH_PROVISIONING_BLOCKED`，receipt 不更新、不写入批次状态和 `batchExecutionId`，不重复三类回填。
11. `BDD: 幂等重试 -> Given` 一次完成已成功；`When` 用相同规范请求和来源哈希重试；`Then` 返回原 `completionBackfillReceipt`，不新增资料或版本，流程6建批由其自身幂等规则处理。
12. `BDD: 幂等冲突 -> Given` 已使用幂等键完成或执行中；`When` 同键携带不同版本/载荷/来源哈希；`Then` 返回冲突，不重放。
13. `BDD: 乐观锁 -> Given` 客户端持有旧订单版本；`When` 完成；`Then` 返回版本冲突并不写入。
14. `BDD: 历史批次执行 -> Given` 存在完成前创建但未与有效完成 receipt/证据关联的历史执行；`When` 提交完成；`Then` 返回迁移 blocker，不认领、不复用、不删除。
15. `BDD: pre-release来源映射 -> Given` 流程6已创建或复用批次执行；`When` 流程7执行 pre-release；`Then` 将生产工单、正式领料单、批记录、过程检验、适用损耗或 `NO_LOSS` 事实映射到 batchExecution，并校验来源 hash/version；校验失败不得进入材料门禁。
16. `BDD: 四份材料放行门禁 -> Given` 流程7 pre-release 映射成功但流程8缺任一来料检报告、灭菌报告、成品检报告、成品检记录；`When` 流程10请求最终放行；`Then` 不返回 `MATERIALS_READY`，最终放行被硬门禁拒绝；四份齐全后才可返回 `MATERIALS_READY`。
17. `BDD: post-release追溯 -> Given` 完成、pre-release 映射、四份材料和流程10最终放行均成功；`When` 查询流程7 post-release 追溯；`Then` 可看到活跃订单、工单、领料、生产/PQC签名及复核、损耗或 `NO_LOSS`、三类资料、四份材料和放行事实的稳定引用。

## 严格 TDD 顺序

| 阶段 | RED（先失败） | GREEN（最小实现） | 回归 |
| --- | --- | --- |
| 1 | 完成前禁止回填/建批、双100与组长范围 | 订单完成命令及权威读取 | 现有生产/PQC来源提交不回填 |
| 2 | BATCH 绑定、PQC汇集、领料关系、损耗规则 | 三类资料 writer 接入统一事务 | 批记录和领料来源历史测试 |
| 3 | Tx-A 任一 writer 失败回滚；Tx-B 建批失败保留不可变 receipt | Tx-A receipt 与流程6后继 `createOrReuse`，流程6独占 BATCH_* 状态 | 事务边界、异常传播、重复回填禁止测试 |
| 4 | 同键重试、冲突、版本冲突、历史执行 | 幂等回执、唯一约束、迁移预检 | schema 与 API 合同测试 |
| 5 | 按钮、确认、刷新、blocker | 前端完成操作 | 前端静态合同与真实页面路径 |
| 6 | 流程7 pre-release 映射、流程8四材料/MATERIALS_READY、流程10最终放行、流程7 post-release 追溯串联失败 | 下游合同适配 | 流程11统筹真实用户路径全链路回归 |

## 命令计划

具体命令在实现前按实际模块、测试类和运行态确定，并写入 `execution-log.md`。下列记录格式必须真实，不能预填为通过：

```text
RED: <精确测试命令> -> FAIL, <预期失败原因>
GREEN: <精确测试命令> -> PASS
REGRESSION: <精确测试命令> -> PASS
```

## 验收证据

- 后端单元/集成测试输出、回滚数据库断言和 schema 合同。
- 前端静态合同：双100完成按钮、一次确认、重复保护、刷新和 blocker。
- Playwright 真实用户路径：生产组长完成、四份文件上传、管理者代表放行、追溯查询；每个写入均为任务自有数据并按同一页面路径清理/恢复。
- E2E 证据记录订单、批次执行和资料 ID，避免记录密码、token 或签名口令。

## 当前 blocker

当前仅完成只读设计；流程5、6/9、7、8、10、11 合同已冻结，但生产实现、测试、真实任务自有数据、正式签名账号、四份材料、迁移证据和下游实现端口尚未完成。因此所有可执行测试均处于 `NOT_RUN`，不得宣称通过。
