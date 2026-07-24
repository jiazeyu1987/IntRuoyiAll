# 任务：角色管理三分改名与导航重组前端改造

## 任务目标

- 将前端三类角色管理入口统一收敛到 `系统管理 / 角色管理` 目录下，对应 `权限角色`、`组织角色`、`审批角色` 3 个子菜单。
- 完成 `system/role`、`system/post`、`dcc/controlled-file/positions` 三页所有用户可见文案改名。
- 通过 `remaining.ts` 保持旧地址 `/system/post`、`/dcc/controlled-file/positions` 可访问，并让高亮与面包屑落到新菜单。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 前端上一相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-bu-select-restriction\task.md`
- 当前状态：`COMPLETED`
- 处理说明：上一任务已完成，本次只隔离修改角色管理页、岗位页、DCC 审批角色页与兼容路由相关文件。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 页面继续沿用 IntPP 紧凑运维台风格，不新增营销式卡片或无关重排。
  - 本轮优先做静态契约、源码与类型验证；若追加真实 Playwright 验收，必须先跑 `login-preflight.mjs` 并在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。
  - 旧地址兼容只能通过显式前端隐藏路由或 redirect 完成，不得继续保留重复侧边栏入口作为“兼容”。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。旧地址直接跳到新路径，不保留双入口或静默回退到旧菜单。
- `是否从根因和长期维护角度解决`：是。通过新目录结构、统一命名和兼容路由收敛导航入口与页面语义。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 权限角色页面统一新文案 -> Given 用户打开权限角色页面 / When 查看搜索区、表格、弹窗和导出文案 / Then 所有用户可见词统一使用权限角色语义。`
- `BDD: 组织角色页面统一新文案 -> Given 用户打开组织角色页面 / When 查看搜索区、表格、弹窗和导出文案 / Then 所有用户可见词统一使用组织角色语义。`
- `BDD: 审批角色页面统一新文案 -> Given 用户打开审批角色页面 / When 查看搜索区、表格、弹窗和帮助提示 / Then 所有用户可见词统一使用审批角色语义。`
- `BDD: 旧地址兼容跳转到新菜单 -> Given 用户访问 /system/post 或 /dcc/controlled-file/positions / When 路由命中隐藏兼容入口 / Then 页面跳到新角色管理子菜单并保持正确高亮。`

## 里程碑

1. M1：创建前端任务包并补静态 RED 契约。已完成。
2. M2：实现页面文案改名与顶部说明。已完成。
3. M3：实现旧地址兼容路由与高亮。已完成。
4. M4：运行 GREEN 静态验证、类型检查与证据校验。已完成。

## Cleanup Keep

- `doc/tasks/20260626-role-management-split-rename-navigation/frontend-feature-evidence.md`

## 预期验证

- `node tests/e2e/role-management-split-static.spec.js`
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-role-management-split-rename-navigation\frontend-feature-evidence.md`
