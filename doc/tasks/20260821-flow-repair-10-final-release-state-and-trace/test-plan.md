# 流程修复 10 测试方案

## 范围与前置条件

本文件保留 BDD 和严格 TDD 设计；专项实现后的可运行验证证据记录在 execution-log.md。已启动本地 server 做只读健康验证，未运行数据库迁移、业务写入或写入型 E2E；流程4/6/8权威适配器、迁移/outbox 和全链路样本仍需外部 owner 提供。

## BDD 场景

- BDD: active-order 来源统一放行 -> Given 流程 6 已返回 batchExecutionId、流程 1 pickListBindingId/sourceSnapshotHash、流程 4 completionBackfillReceipt 为 BACKFILL_SUCCEEDED、双进度 100% 且四材料均为当前有效版本；When 任一 active-order 放行入口调用 finalizeRelease；Then 只有一个 RELEASED 决策，后续订单/工单状态仅通过各自 owner 受控接口收敛，审计可反查全部来源。
- BDD: 独立来源统一放行 -> Given MANUAL、SCHEDULED 或 PQC_INDEPENDENT 来源提供流程 6 签发的 IndependentBatchPrerequisiteReceipt、正式 source relation、来源快照/hash、自身业务前置和 batchExecutionId；When 放行入口调用 finalizeRelease；Then 不要求伪造 activeOrderId、pickListId 或 completionBackfillReceipt，只有一个 RELEASED 决策。
- BDD: 材料缺失阻断 -> Given 四材料任一缺失、过期、hash 不一致或类型未知；When 任一入口尝试放行；Then 返回明确门禁错误，不改变上游终态，不创建假成功待办。
- BDD: 并发审批唯一胜者 -> Given 两个不同入口使用同一申请和期望版本并发批准；When 同时提交；Then 仅一个 CAS 成功，另一请求返回重复或版本冲突，并读取同一正式决策。
- BDD: 幂等键冲突 -> Given 同一幂等键已有正式 payload；When 再次提交不同 payload；Then 返回冲突，不写第二个申请、批次或决策。
- BDD: 驳回与撤回 -> Given 申请处于可驳回或可撤回窗口；When 执行相应动作；Then 仅申请状态和待办按冻结规则变化，已完成生产来源不被回滚，审计记录原因和操作者。
- BDD: 终态追溯出口 -> Given 放行已成功或终态已驳回/撤回；When 查询订单、工单、批次或领料来源；Then 返回不可变 source manifest、材料 manifest、状态版本和决策事件，终态对象没有可办理入口。

## RED 计划

RED: NOT RUN -> 严格 RED 阶段未执行，不能补写为 PASS；缺口以审计事实记录。

## GREEN 计划

GREEN: NOT RUN -> 原计划未执行；实现后实际验证为 compile BUILD SUCCESS、流程10 45/45 PASS、流程6/8/9/审批中心 29/29 PASS，不能冒充原计划 GREEN PASS。

## REGRESSION 计划

REGRESSION: NOT RUN -> 全链路真实回归、迁移和写入型 E2E 未运行；定向合同回归已运行并记录为独立证据，不能替代全链路验证。

## 迁移与回滚验证

迁移前清单必须能识别缺完成事件、缺回填、缺四材料 manifest、缺上游引用或版本/hash 的历史记录并阻断自动放行。回滚只允许在新业务事实未写入前切换入口；已写入决策后仅允许反向业务命令，不能用代码覆盖事实。

## 当前 blocker

流程4/6/8权威凭证适配器、审批中心权威上下文、生产迁移/历史回填、outbox 投递、全链路真实 E2E 尚未完成；流程11任务文档交付已完成。本文件不是全链路放行通过证明。

## 启动 Bean 回归

- BDD: Given 流程4/6/8权威适配器尚未接入；When Spring context 启动；Then MesReleaseAuthoritativeContextPort 恰好一个 Bean，放行请求仍返回结构化 blocker。
- RED: MesReleaseAuthoritativeContextConfigurationTest 在修复前因无端口 Bean 启动失败（历史日志 MesReleaseAuthoritativeContextPort missing）。
- GREEN: mvn -pl yudao-module-mes -Dtest=MesReleaseAuthoritativeContextConfigurationTest test -> PASS；该测试 1/1。
- REGRESSION: mvn -pl yudao-module-mes -Dtest=MesReleaseFinalizationValidatorTest,MesProEdhrReleaseServiceImplTest,MesProductionReleaseManagerApprovalServiceTest,MesReleaseAuthoritativeContextConfigurationTest test -> PASS，47/47；mvn -pl yudao-server -am -DskipTests package -> BUILD SUCCESS。
- Runtime smoke: 48081 LISTEN，GET /actuator/health -> status=UP；运行时 JAR 类 SHA-256 与构建产物一致。
