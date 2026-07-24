# 任务：展厅公司菜单可见即编辑直存（前端）

## Goal

把公司页前端从 `showroom_publicity` 角色门控切换为“进入该菜单页即可操作”的真实菜单路径模型；将公司文字主动作文案从 `直接发布` 调整为 `保存`，保留现有接口与无审批直存行为。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\company\CompanyWorkbench.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\company\contracts.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-company-dashboard-history.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-company-menu-direct-save\**`

## Non-Scope

- 不重做页面布局
- 不改产品、展厅、审批中心页面
- 不新增按钮权限码
- 不引入 mock、fallback 或兼容分支

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-frontend-current-code-commit\task.md`
- Status before this task: completed
- Impact: 无，可继续处理新前端任务

## Milestones

- [x] M1: 创建前端任务文档并确认上一同仓库任务状态。
- [x] M2: 先补 RED 测试，锁定“去角色门控”和“保存文案”。
- [x] M3: 完成公司页前端最小改动。
- [x] M4: 执行前端测试与 lint，记录 GREEN。
- [x] M5: 更新前端证据并准备真实路径验证。

## Expected Verification

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue src/views/showroom-admin/company/contracts.ts scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-company-menu-direct-save\frontend-feature-evidence.md`

## Current Status

Completed on 2026-05-20.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue src/views/showroom-admin/company/contracts.ts scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs`
- NOT RUN: 真实浏览器路径依赖当前后端运行时先切到本次代码；由于同仓后端集成文件存在 unrelated 展示接口断言漂移，本次未继续重启运行时做前端 E2E
