# 测试计划：多入口正式前置与放行分离

## 范围与前置条件

本任务只设计 BDD/TDD 验证，不运行生产代码、服务、数据库迁移或写入型 E2E。实现前必须读取流程修复 6 的 canonical `IndependentBatchPrerequisiteReceipt`、流程修复 8 四材料 gate、流程修复 10 最终放行合同和流程修复 11 总门禁。

## BDD 场景

### BDD-01 活跃订单完成入口消费 receipt

Given 流程修复 1 领料绑定、双进度 100%、流程修复 4 完成版本和 `BACKFILL_SUCCEEDED` completion receipt 齐全；When 活跃订单完成入口请求建批；Then 流程 9 只分流并调用流程 6，返回 batchExecutionId，不产生新的 receipt。

### BDD-02 活跃排产/PQC/手工不得产生 receipt

Given 活跃订单排产、PQC 或手工重试入口；When 请求建批；Then 只能消费流程 4 receipt，不能创建、修改或重新回填 receipt，缺 receipt 阻断。

### BDD-03 有效独立 MANUAL 建批

Given 没有 activeOrderId，但有后端签发、未过期未撤销的 `IndependentBatchPrerequisiteReceipt(entryType=MANUAL)` 和正式 source relation；When 调用流程 6；Then 创建或复用独立 batchExecutionId，来源可追溯且不伪造活跃订单。

### BDD-04 有效独立 SCHEDULED/PQC_INDEPENDENT 建批

Given `entryType=SCHEDULED` 或 `PQC_INDEPENDENT`、canonical receipt 和正式 source relation 均有效；When 入口调用流程 6；Then 允许建批。PQC_INDEPENDENT 不得用 PQC 批准替代 receipt。

### BDD-05 无凭证独立入口阻断

Given 独立入口只有工单号、批号或路线号；When 请求建批；Then 返回 `INDEPENDENT_CREDENTIAL_REQUIRED/SOURCE_RELATION_REQUIRED`，不猜来源、不创建批次。

### BDD-06 场景混用阻断

Given ACTIVE_ORDER 组合独立 receipt，或 MANUAL/SCHEDULED/PQC_INDEPENDENT 组合活跃 receipt；When 请求建批；Then 返回 `ENTRY_SCENARIO_MISMATCH`，无副作用。

### BDD-07 同源跨入口复用

Given 两个合法入口拥有同一 receipt、source relation、sourceContextHash、租户和路线版本；When 先后建批；Then 流程 6 返回同一 batchExecutionId 且 created/reused 正确。

### BDD-08 不同来源不得复用

Given receipt 或 sourceContextHash/source relation 不同；When 入口重试或跨入口请求；Then 返回冲突或创建独立批次，不覆盖既有来源。

### BDD-09 receipt 生命周期阻断

Given canonical receipt 已过期、撤销、签名无效或 payload hash/version 不一致；When 调用流程 6；Then fail fast，不能通过页面或 PQC 旁路。

### BDD-10 四材料缺件阻断

Given batchExecutionId 已创建/复用但四份材料任一缺失、过期或 hash/version 不一致；When 任一放行入口提交；Then 流程 8 gate 拒绝，流程 10 不写 RELEASED。

### BDD-11 四材料齐套后流程 10 最终放行

Given batchExecutionId 已存在，四份固定材料均由流程 8 判定有效；When 管理者代表调用流程 10 最终放行；Then 流程 10 唯一写入 RELEASED，流程 9 不参与建批/终态写入。

### BDD-12 独立放行后的真实来源追溯

Given 独立批次已通过流程 8 并由流程 10 放行；When 从 release、batch 或 source object 查询；Then 返回 `batchExecutionSourceRelation -> IndependentBatchPrerequisiteReceipt -> source IDs`，不能显示成活跃订单来源。

### BDD-13 历史无凭证阻断

Given 历史批次只有工单/批号/路线或旧附件；When 迁移、复用或放行；Then 保持 `BLOCKED_LEGACY`，不自动认领、不补默认凭证。

## RED 计划（NOT RUN）

- `mvn -pl <backend-module> -Dtest=IndependentBatchPrerequisiteReceiptContractTest test` -> 预期 FAIL：canonical 字段、后端签发/有效期/撤销校验尚未实现。
- `mvn -pl <backend-module> -Dtest=MultiEntryProvisionerRoutingTest test` -> 预期 FAIL：entryType 分流、活跃 receipt 消费和场景混用阻断尚未实现。
- `mvn -pl <backend-module> -Dtest=BatchExecutionMaterialReleaseBoundaryTest test` -> 预期 FAIL：流程 8 gate 和流程 10 只消费既有 batchExecutionId 的统一边界尚未实现。
- 流程 11 负责的真实 Playwright 和迁移 RED 仅作后续计划，本任务不运行。

## GREEN 计划（NOT RUN）

实现双 receipt 合同、后端受控签发、entryType 分流、流程 6 建批、流程 8 四材料 gate 和流程 10 最终放行后，逐项复跑同一 RED 命令至 PASS。不能用默认凭证、前端隐藏字段、工单号猜测或文档结构 PASS 代替 GREEN。

## REGRESSION 计划（NOT RUN）

覆盖完成/排产/PQC/手工/独立五类入口、PQC_INDEPENDENT、过期/撤销/签名冲突、同源复用、异源不复用、批次创建与放行分离、四材料重传版本、流程 10 唯一 RELEASED、独立来源追溯、历史 BLOCKED_LEGACY、权限和并发幂等。

## 迁移/回滚验证（NOT RUN）

迁移仅接受可验签、未过期未撤销且 hash/version 可复算的 canonical receipt；无正式凭证/source relation 的历史数据保持 BLOCKED_LEGACY。已写入 release decision 的事实只能通过有权限反向业务命令回滚，不能代码覆盖。

## 发布阻断

任一入口可无正式 receipt 建批、PQC 可替代活跃完成/回填、独立来源无法追溯、流程 8 缺材料仍放行、非流程 10 写 RELEASED 或流程 9直接修改状态，均不得发布。
