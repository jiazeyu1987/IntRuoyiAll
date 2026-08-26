# Stage2.5 Simulation Metadata

## Goal

修复 Stage2.5 调用正式活跃订单模拟服务时缺少 simulationStage 和 simulationRunId 的问题，确保生产/PQC模拟事实可按本轮任务清理和追溯。

## Milestones

1. 固定 BDD 和 RED 证据。
2. 调用正式四参数模拟入口。
3. 运行 Stage2.5 静态合同、Java 合同和 MES 编译。
4. 提交并保护性提升 int_main，随后同步主工作树后端。

## Expected Verification

- Stage2.5 静态合同通过。
- Stage2.5 Java receipt handoff 合同通过。
- MES 编译通过。
- 不引入 dossier writer、fallback 或直接进度写入。

## Current Status

completed

## Verification Evidence

- Stage2.5 backend static contract: PASS。
- Stage2.5 frontend static contract: PASS。
- Stage2.5 Java receipt handoff contract: PASS。
- Stage4/Stage6 adjacent frontend static contracts: PASS。
- MES module compile: BUILD SUCCESS。
- Implementation commit: ab69fa640c7155a7dcf51b73a335b5fe3186d8e1。
- int_main has been protected-updated to the implementation commit and the main worktree backend has been synchronized path-by-path。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，复用正式四参数模拟服务。
- 是否存在临时补丁或绕过：否。
