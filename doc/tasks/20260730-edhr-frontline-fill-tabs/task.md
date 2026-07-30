# 20260730 eDHR 一线填写页签

## Task Goal

在 eDHR 批记录页面级页签中新增 `生产填写` 和 `PQC填写` 两个独立页签，并把现有一线简化填写 UI 接入真实 Vue 前端页面。

## Milestones

- [ ] 建立任务记录与 BDD/TDD 验收合同。
- [ ] 新增 eDHR 批记录共享页签组件和两个填写路由。
- [ ] 将一线简化填写组件拆成固定 `production` / `pqc` 模式，防止员工切换时跨模板自动换 UI。
- [ ] 完成静态合同、类型检查和可用验证记录。

## Expected Verification

- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`
- `pnpm ts:check`

## Current Status

in_progress

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，采用共享页签组件和固定模式的一线填写组件，避免静态 HTML 或重复实现。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端功能按 BDD + strict TDD 执行，先新增最小静态合同，再实现。
- 页面复刻只允许改前端展示和路由，不修改后端 API、DTO、数据库或业务数据。
- 缺少正式提交上下文时必须显式阻塞，不 mock、不默认成功、不静默切换模板。
- PowerShell 写中文文档必须使用 UTF-8 路径；本任务文档通过 `apply_patch` 写入。

## Dirty Workspace Baseline

- Baseline commit: `4158334f chore: baseline dirty workspace before edhr frontline tabs`
- Purpose: 隔离本任务开始前已存在的未提交/未跟踪改动。
