# Test Plan

## BDD

1. Given 一线提交携带正式工单、领料单、签名和损耗来源，When 提交成功，Then 只新增生产事实事件和初始分配事实，本次请求不得新增或修改三类回填和批次执行。
2. Given 复核输入为当前 `submissionVersion`，When 组长通过或驳回，Then 只写复核事实，驳回后旧提交不可修改。
3. Given REVIEWED 分配数量达到工序目标，When 确认分配，Then 只更新分配事实和进度投影，不调用 `completeAndBackfill`。
4. Given 同幂等键相同载荷并发，When 重放，Then 返回同一正式事件；不同载荷返回 blocker。
5. Given 超量或版本竞态，When 请求执行，Then 按结构化 blocker 或显式可调超量策略处理，不静默截断。
6. Given 流程4完成命令，When 双100%、PQC正式确认、工单和领料绑定均成立，Then 才原子回填三类单据；批次执行、四份材料和最终放行由流程6/8/10负责。

## TDD

- RED: 证明旧 `applyConfirmedAllocations(..., true, ...)` 会调用回填。
- GREEN: 移除流程2完成回填路径，并证明目标达到时 `backfillService` 不被调用。
- REGRESSION: 复核、分配、驳回、重提、超量、并发、幂等及来源追溯测试。

生产实现测试状态以 `execution-log.md` 为准。ERP/MES reactor 编译门禁已通过；流程2及相邻 QA 回归共 108 项全部通过。完整 reactor test 已进入执行，但被无关 infra 运行时测试失败中止；服务、数据库和写入型 E2E 仍不在本任务范围。
