# 任务：用户管理公司树删除入口

## 目标

在 `系统管理 / 用户管理` 页面补一个可用的组织删除入口：

- 用户在左侧树选中公司或部门后，可以直接在当前页面发起删除。
- 删除公司时，依赖后端执行“空公司树级联删除”；删除后刷新左侧树和右侧用户列表。
- 未选中组织时按钮必须禁用；若后端因仍有员工而拒绝删除，前端不得伪造成功。

## 非目标

- 不重做用户管理页整体布局。
- 不新增测试专用控件、隐藏入口或绕过真实删除接口。
- 不修改部门管理页现有树表结构。

## 前置任务检查

- 最近前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-browser-latest-version-default\task.md`
- 启动前状态：已完成。
- 关联后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-system-user-company-cascade-delete\task.md`

## 范围

- `src/views/system/user/index.vue`
- `scripts/system-user-company-delete.test.mjs`
- `doc/tasks/20260519-system-user-company-cascade-delete/**`

## 里程碑

- [x] M1：完成任务建档并锁定“选中组织后删除、删除后刷新、无选中则禁用”的页面行为。
- [x] M2：补前端 RED 测试，证明当前用户管理页没有组织删除入口。
- [x] M3：实现用户管理页组织删除按钮、选中状态和刷新链路。
- [x] M4：运行前端 GREEN 验证并补齐证据。
- [x] M5：执行收尾预览，准备仅包含本任务改动的提交。

## 预期验证

- `node --test scripts/system-user-company-delete.test.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-system-user-company-cascade-delete\frontend-feature-evidence.md`
- 若本地入口可用：从 `http://localhost:8081` 登录进入 `系统管理 / 用户管理`，验证删除按钮默认禁用、选中组织后启用并弹出确认框。

## Current Status

Completed. The user-management delete entry, selection binding, and refresh flow are all in place, and both the targeted test and real frontend smoke passed.

## Final Verification Result

- PASS: `node --test scripts/system-user-company-delete.test.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session system-user-company-delete run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-system-user-company-cascade-delete\scripts\verify-system-user-company-delete-smoke.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-system-user-company-cascade-delete\frontend-feature-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-system-user-company-cascade-delete --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-system-user-company-cascade-delete --mode apply`
