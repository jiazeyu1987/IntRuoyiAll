# 任务：eDHR Phase 2 批次统一状态机（后端）

- Task ID: `20260701-edhr-phase2-stage-unification`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

把当前 Phase 1 中的最小阶段解析器提升为正式批次统一状态机与阶段摘要模型，使列表页和详情页都能复用同一套主阶段解释。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\doc\tasks\20260701-edhr-phase1-workbench\task.md`
- 状态：`completed`
- 处理说明：Phase 1 已提供 workbench 聚合接口，Phase 2 在其基础上继续统一状态模型。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。统一阶段解释层，避免前后端页面各自推断批次阶段。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 列表页和详情页共享同一主阶段解释 -> Given 同一批次在列表页与详情页展示 / When 后端计算阶段摘要 / Then 两处看到的阶段标签、责任角色和阻塞项一致。`
- `BDD: 放行状态优先覆盖批次关闭后的主阶段 -> Given 批次已关闭并存在放行事务 / When 后端计算主阶段 / Then 主阶段优先体现放行子流程而不是停留在 CLOSED。`

## Milestones

1. M1：建立 Phase 2 后端任务台账并确认当前 resolver 缺口。`completed`
2. M2：补 service 级聚合测试，锁定统一阶段输出。`completed`
3. M3：增强状态机与摘要模型并跑 GREEN。`completed`
4. M4：补 backend evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionServiceTest" test`

## Current Blockers

- 暂无。

## Final Verification Result

- 已完成批次统一阶段字段下沉到基础响应 DTO。
- 已让 workbench resolver 与 `toResp(...)` 共用同一阶段解释。
- `MesProEdhrBatchExecutionControllerTest` 与 `MesProEdhrBatchExecutionServiceTest` 均已通过。
