# PRD：一线 PQC 提交与组长复核来源边界

## Problem

一线 PQC 提交和 PQC 组长复核必须形成可追溯、不可变的过程检验来源事实，但不能提前写正式过程检验单、创建批次执行或改变放行状态。现有代码已有任务、逐件明细、设备快照和聚合模型，仍需把终态复核幂等、版本冲突、退回修订和唯一有效来源固化为可执行行为。

## Users And Roles

- 一线 PQC：按正式任务提交逐件/逐项检验事实、设备快照和签名。
- PQC 组长：读取同一来源版本，确认或退回；不得改写检验事实。
- 流程 4：在活跃订单双进度 100% 完成节点消费确认来源并回填正式过程检验单。
- 流程 6/7/8/9/10/11：分别负责建批、映射追溯、四份材料、入口前置、最终放行和总体验证；不由流程 3 越权。

## Scope

1. 实现 PQC 来源提交、组长确认/退回、结构化 aggregate 生成和只读确认来源查询。
2. 固化 `sourceRevision`、`payloadHash`、`aggregateVersionId`、签名/设备快照、`expectedVersion` 和幂等键。
3. 同键同内容返回同一结果；同键不同内容返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`；终态普通复核返回幂等或 `STATE_CONFLICT`，修订必须使用新 revision。
4. 禁止 raw payload、旧 IPQC、当前 QA/设备配置、formBindings、生产提交事实或页面缓存替代正式来源。

## Out Of Scope

流程 4 的三类回填、流程 6 建批、流程 7 批次映射/追溯、流程 8 四份材料、流程 9 独立入口凭证、流程 10 最终放行、流程 11 总体验证和数据库迁移不在本实现范围内；本任务只提供其所需来源合同。

## Acceptance Criteria

- Given 正式 PQC 任务、完整逐件明细、设备快照和签名，When 一线 PQC 提交，Then 生成唯一 `SUBMITTED` 来源版本和 `payloadHash`，不创建正式过程检验单。
- Given 同一 `sourceRevision` 的 `SUBMITTED` 来源，When PQC 组长确认，Then 原子生成唯一 `CONFIRMED` aggregate；`CONFIRMED` 不代表正式单、批次、材料或放行。
- Given 组长退回，When 一线 PQC 修订，Then 保留旧版本审计并仅允许新 revision 提交；旧 revision 不得被下游消费。
- Given 已确认来源，When 普通复核重复提交，Then 同键同内容幂等返回或稳定 `STATE_CONFLICT`，不得新增相反终态。
- Given 任何 hash、租户、路线版本、逐件明细或设备快照不一致，When 提交/确认/查询，Then 返回结构化 blocker 并回滚事务。

## Verification

采用 BDD + 严格 TDD：先为提交、确认、退回、幂等冲突、CAS 冲突、完整性阻断和只读查询编写 RED；实现后运行定向后端测试和回归测试；真实跨角色 E2E 在具备任务自有租户、用户、权限和正式来源数据后执行。
