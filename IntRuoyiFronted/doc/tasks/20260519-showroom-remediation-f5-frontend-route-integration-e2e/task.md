# 任务：展厅前端 F5 路由集成与 E2E 收口

## 目标

在前端子页面和后端契约都到位后，完成展厅前端的最终路由编排、后台壳页承接、前台子路由整合、关键 E2E 回归与设计文档同步。

## 里程碑

- [x] 检查前置任务交付状态
- [x] 完成路由与壳页集成
- [x] 运行脚本与真实路径 E2E
- [x] 更新任务记录并提交

## 范围

- 可修改：
  - `src/router/modules/showroom.ts`
  - `src/views/showroom-admin/index.vue`
  - `scripts/showroom-*.mjs`
  - 相关 task 文档与 E2E 证据

## 非范围

- 不实现新的业务接口
- 不新增独立子页面业务逻辑，除非为路由集成所必需
- 不接管 `screen/pad/mobile` 设备壳细节实现

## 写入边界

- `src/router/modules/showroom.ts`
- `src/views/showroom-admin/index.vue`
- `scripts/showroom-*.mjs`
- `doc/tasks/20260519-showroom-remediation-f5-frontend-route-integration-e2e/**`

## 依赖

- `F1/F2/F3/F4` 页面与组件已交付
- `B2/B3/B4` 契约已稳定
- 讲解工作台前端已存在，按 `PUBLIC + ZH/EN` 维护；`B5` 仍在补持久化与最终收口，因此 F5 只承接现有讲解工作台，不把 `B5` 当作已完全完成。

## 预期验证

- `node --test scripts/showroom-*.mjs`
- Playwright 真实入口回归：`http://localhost:8081`

## 完成定义

- 后台子菜单进入的不是摘要占位，而是对应真实页面。
- 前台路由、隐藏详情路由、设置页、讲解页都与最新实现一致。
- E2E 用真实路径和真实接口回归通过。

## 当前状态

已完成：后台壳页、前台子路由和真实路径回归均已通过，并已按 F5 写入边界单独提交本任务文件。

## 后续说明

- 本任务为当前仓库中的历史集成收口记录。
- 后续新的 APP 展示层集成与 E2E 改由 `D:\ProjectPackage\Website` 仓库承接。

## 验证结果

- PASS: `node --test scripts/showroom-*.mjs`
- PASS: Playwright 真实入口回归 `http://localhost:8081`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-remediation-f5-frontend-route-integration-e2e --mode preview`

## 提交说明

- 本次仅提交 F5 写入边界内文件。
- 仓库里其余未暂存的 showroom / dcc 脏改动保持原样，未纳入本任务提交。

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f5-frontend-route-integration-e2e\task.md
3. 所有前置前端整改 task 文档与后端契约 task 文档

目标：
- 完成展厅前端最终路由编排与 E2E 收口。
- 把前面独立交付的页面接进真实菜单和真实用户路径。

写入边界：
- src/router/modules/showroom.ts
- src/views/showroom-admin/index.vue
- scripts/showroom-*.mjs
- 你的 task 目录

要求：
- 只做集成，不重写其他任务已交付页面。
- 严格 TDD 与真实路径 E2E。
- 缺前置产物就失败并明确写 blocker。

完成后运行：
- node --test scripts/showroom-*.mjs
- 按 AGENTS.md 要求用 Playwright 走真实入口做回归
```
