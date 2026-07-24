# 任务：eDHR V1 FeedbackForm 首入口 RED 测试

## Goal

按当前已放行约束，为 eDHR V1 执行节点补第一批前端 failing tests，并明确首个入口必须落在 `FeedbackForm`，不是 `WorkOrderForm2`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\scripts\edhr-v1-feedback-entry.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\doc\tasks\20260523-edhr-v1-feedback-entry-red\**`

## Non-Scope

- 不改任何生产代码
- 不补后端接口实现
- 不把入口放到 `WorkOrderForm2`
- 不做 GREEN 修复

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\doc\tasks\20260523-nas-transfer-preflight-confirm-frontend\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact: 上一任务已完成，不阻塞本次仅补前端 RED 测试

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3`
- Current state: 启动时 `git status --short` 无当前仓内未提交输出
- Impact: 本任务只新增测试与任务文档，避免影响其他并行改动

## Milestones

- [x] M1: 确认上一任务已结项并记录本次放行约束
- [x] M2: 记录 BDD / RED，锁定 FeedbackForm 首入口、独立执行页路由、执行快照契约
- [x] M3: 新增源码级静态 RED 测试
- [x] M4: 运行定向 RED 命令并记录失败原因为功能缺失

## Expected Verification

- `node --test scripts\\edhr-v1-feedback-entry.test.mjs`

## Current Status

Completed on 2026-05-23 for RED scope. 已新增首批 eDHR V1 前端静态 failing tests，并确认定向命令失败原因为 eDHR 入口 API、`打开 eDHR` 入口、独立执行页路由和 `executionSnapshotJson` 契约尚未实现。

## Blockers And Impact

- Blocker: 当前前端仓内尚未发现 eDHR 入口 API、`FeedbackForm` 的 `打开 eDHR` 入口、独立执行页隐藏路由和 `executionSnapshotJson` 契约实现
- Impact: 本轮验证预期为 RED，失败原因应体现为入口/路由/契约缺失，而不是测试脚本异常
