# 任务：eDHR Phase 1 批次总控页收口（后端）

- Task ID: `20260701-edhr-phase1-workbench`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

为 eDHR Phase 1 提供批次总控页所需的后端聚合能力，新增 workbench 聚合接口，输出统一阶段、阻塞项、任务摘要、放行摘要与审计摘要。

## Previous Task Check

- 当前 worktree 最近后端任务文档与 eDHR Phase 目标无直接关联，本轮在隔离 worktree 内建立新的正式任务台账。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 当前开发必须留在 `edhr_phase` worktree，不得回主工作区实现。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过新增正式聚合接口和阶段解析层收口批次详情，不做页面本地拼接事实。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 批次详情获得统一阶段摘要 -> Given 批次下存在任务、放行事务与审计证据 / When 请求 workbench 聚合接口 / Then 返回统一阶段、阻塞项、任务摘要、放行摘要和审计摘要。`
- `BDD: 无放行或审计数据时仍返回结构化摘要 -> Given 批次尚未进入放行或部分审计数据为空 / When 请求 workbench 聚合接口 / Then 返回结构完整但状态明确的摘要对象，不让前端自行猜测。`

## Milestones

1. M1：建立后端任务台账并确认聚合数据源。`completed`
2. M2：补 RED 测试，锁定 workbench 聚合合同。`completed`
3. M3：实现 workbench DTO、resolver、controller/service 并跑 GREEN。`completed`
4. M4：补 backend evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionWorkbenchServiceTest,MesProEdhrBatchExecutionControllerTest" test`

## Current Blockers

- 暂无。Phase 1 后端 controller/service 级验证均已通过。

## Final Verification Result

- 已新增 workbench 聚合接口与最小阶段解析能力。
- 已完成 controller/service 级定向 Maven 验证。
- 当前可为前端批次详情页提供统一阶段、阻塞、放行、审计摘要数据；真实运行态已验证 `/workbench` 可用。
