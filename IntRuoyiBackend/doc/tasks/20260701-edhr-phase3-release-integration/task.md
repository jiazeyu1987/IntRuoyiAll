# 任务：eDHR Phase 3 放行衔接（后端）

- Task ID: `20260701-edhr-phase3-release-integration`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

让批次详情页中的放行摘要从“只读信息”升级为“正式流程阶段入口”，提供面向批次的放行预检/事务查看支撑，减少用户先跳去独立放行后台的必要性。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\doc\tasks\20260701-edhr-phase2-stage-unification\task.md`
- 状态：`completed`
- 处理说明：Phase 2 已完成统一阶段模型，Phase 3 在其基础上把放行链路进一步收口到批次上下文。

## BDD 场景

- `BDD: 批次详情可直接触发放行预检 -> Given 用户位于批次详情且批次存在有效 ID / When 点击放行预检 / Then 在当前批次上下文中完成预检并刷新放行摘要。`
- `BDD: 批次详情可直接查看放行事务 -> Given 批次已有放行事务 / When 点击事务事件或检查项 / Then 用户围绕该批次直接进入放行事务明细，而不是先回放行列表筛选。`

## Milestones

1. M1：建立 Phase 3 后端任务台账并确认放行接口最小复用方案。`completed`
2. M2：补必要接口/contract 支撑批次详情放行入口。`completed`
3. M3：补 backend evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionControllerTest" test`

## Current Blockers

- 暂无。

## Final Verification Result

- 批次详情页放行衔接复用既有 `MesProEdhrReleaseController` / `MesProEdhrReleaseServiceImpl` 合同完成，无需新增平行后端放行接口。
- `MesProEdhrBatchExecutionControllerTest` 与 `MesProEdhrBatchExecutionServiceTest` 已通过，确认批次详情页收口放行入口不会破坏既有批次执行合同。
